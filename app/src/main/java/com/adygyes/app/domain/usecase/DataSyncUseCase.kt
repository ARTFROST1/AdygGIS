package com.adygyes.app.domain.usecase

import com.adygyes.app.data.local.preferences.PreferencesManager
import com.adygyes.app.data.sync.SyncService
import com.adygyes.app.domain.repository.AttractionRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for data synchronization with Supabase.
 * 
 * OFFLINE-FIRST approach:
 * - Room DB is always read first (instant)
 * - Supabase sync happens in background
 * - Delta sync only fetches changes
 */
@Singleton
class DataSyncUseCase @Inject constructor(
    private val attractionRepository: AttractionRepository,
    private val preferencesManager: PreferencesManager,
    private val networkUseCase: NetworkUseCase,
    private val syncService: SyncService
) {
    
    /**
     * Check if initial sync is needed (Room is empty)
     */
    suspend fun shouldCheckForUpdates(): Boolean {
        return !attractionRepository.isDataLoaded()
    }
    
    /**
     * Check for data updates from Supabase
     */
    suspend fun checkForUpdates(): UpdateCheckResult {
        return try {
            if (!networkUseCase.isOnline()) {
                return UpdateCheckResult.NoConnection
            }
            
            // Check last sync timestamp
            val lastSync = preferencesManager.getLastSyncTimestamp()
            
            if (lastSync == null) {
                // Never synced - update available
                Timber.d("First sync needed")
                UpdateCheckResult.UpdateAvailable("initial")
            } else {
                // Already synced - check for delta
                Timber.d("Last sync: $lastSync")
                UpdateCheckResult.UpToDate
            }
        } catch (e: Exception) {
            Timber.e(e, "Error checking for updates")
            UpdateCheckResult.Error(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Perform data synchronization with Supabase
     */
    fun syncData(): Flow<SyncProgress> = flow {
        try {
            emit(SyncProgress.Started)
            
            if (!networkUseCase.isOnline()) {
                // Offline mode - data is already in Room, just report success
                emit(SyncProgress.Completed("Offline mode - using cached data"))
                return@flow
            }
            
            emit(SyncProgress.CheckingForUpdates)
            
            // Perform Supabase delta sync
            emit(SyncProgress.DownloadingData(20))
            
            val result = syncService.performSync()
            
            emit(SyncProgress.DownloadingData(80))
            
            if (result.success) {
                val message = when {
                    result.added > 0 || result.updated > 0 || result.deleted > 0 ->
                        "Synced: +${result.added} ~${result.updated} -${result.deleted}"
                    else -> "Data is up to date"
                }
                emit(SyncProgress.Completed(message))
            } else {
                emit(SyncProgress.Error(result.errorMessage ?: "Sync failed"))
            }
            
        } catch (e: Exception) {
            Timber.e(e, "Error during data sync")
            emit(SyncProgress.Error(e.message ?: "Sync failed"))
        }
    }
    
    /**
     * Force full data refresh from Supabase (ignores delta)
     */
    suspend fun forceRefresh(): Boolean {
        return try {
            if (!networkUseCase.isOnline()) {
                Timber.w("Cannot force refresh - offline")
                return false
            }
            
            val result = syncService.forceFullSync()
            
            if (result.success) {
                Timber.d("Force refresh completed: ${result.added} attractions")
                true
            } else {
                Timber.e("Force refresh failed: ${result.errorMessage}")
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Force refresh failed")
            false
        }
    }
    
    /**
     * Get last sync information
     */
    suspend fun getLastSyncInfo(): SyncInfo {
        val lastSync = preferencesManager.getLastSyncTimestamp()
        return SyncInfo(
            lastSyncTime = lastSync?.let { parseTimestamp(it) },
            dataVersion = "supabase", // Version is managed by Supabase
            lastUpdateCheck = lastSync?.let { parseTimestamp(it) },
            isDataLoaded = attractionRepository.isDataLoaded()
        )
    }
    
    /**
     * Check if force update is needed
     */
    suspend fun needsForceUpdate(): Boolean {
        // With Supabase, force update is rarely needed
        // Delta sync handles most cases
        return false
    }
    
    private fun parseTimestamp(isoString: String): Long? {
        return try {
            java.time.Instant.parse(isoString).toEpochMilli()
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Result of update check
 */
sealed class UpdateCheckResult {
    object UpToDate : UpdateCheckResult()
    object NoConnection : UpdateCheckResult()
    data class UpdateAvailable(val version: String) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

/**
 * Sync progress states
 */
sealed class SyncProgress {
    object Started : SyncProgress()
    object CheckingForUpdates : SyncProgress()
    data class DownloadingData(val progress: Int) : SyncProgress()
    object ProcessingData : SyncProgress()
    data class Completed(val message: String) : SyncProgress()
    data class Error(val message: String) : SyncProgress()
}

/**
 * Information about last sync
 */
data class SyncInfo(
    val lastSyncTime: Long?,
    val dataVersion: String,
    val lastUpdateCheck: Long?,
    val isDataLoaded: Boolean
) {
    fun getLastSyncFormatted(): String {
        return lastSyncTime?.let { 
            java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(it))
        } ?: "Никогда"
    }
    
    fun getLastCheckFormatted(): String {
        return lastUpdateCheck?.let { 
            java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(it))
        } ?: "Никогда"
    }
}
