package com.adygyes.app.data.sync

import com.adygyes.app.data.local.preferences.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for handling data synchronization with Supabase.
 * 
 * OFFLINE-FIRST approach:
 * - Room DB is the source of truth for the app
 * - Supabase is the source of truth for the server
 * - SyncService handles delta sync between them
 */
@Singleton
class DataSyncManager @Inject constructor(
    private val syncService: SyncService,
    private val preferencesManager: PreferencesManager
) {
    
    private val _syncState = MutableStateFlow<SyncState>(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    
    private val _syncProgress = MutableStateFlow(0f)
    val syncProgress: StateFlow<Float> = _syncProgress.asStateFlow()
    
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    /**
     * Perform delta sync with Supabase
     */
    fun syncData() {
        coroutineScope.launch {
            try {
                _syncState.value = SyncState.SYNCING
                _syncProgress.value = 0.2f
                
                Timber.d("🔄 DataSyncManager: Starting Supabase sync...")
                
                val result = syncService.performSync()
                
                _syncProgress.value = 0.9f
                
                if (result.success) {
                    _syncProgress.value = 1f
                    _syncState.value = SyncState.SUCCESS
                    Timber.d("✅ Sync completed: +${result.added} ~${result.updated} -${result.deleted}")
                } else {
                    _syncState.value = SyncState.ERROR(result.errorMessage ?: "Sync failed")
                    Timber.e("❌ Sync failed: ${result.errorMessage}")
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Sync failed")
                _syncState.value = SyncState.ERROR(e.message ?: "Unknown error")
                _syncProgress.value = 0f
            }
        }
    }
    
    /**
     * Force full refresh from Supabase (ignores delta)
     */
    fun forceRefresh() {
        coroutineScope.launch {
            try {
                _syncState.value = SyncState.SYNCING
                _syncProgress.value = 0.1f
                
                Timber.d("🔄 DataSyncManager: Starting FULL Supabase sync...")
                
                val result = syncService.forceFullSync()
                
                _syncProgress.value = 0.9f
                
                if (result.success) {
                    _syncProgress.value = 1f
                    _syncState.value = SyncState.SUCCESS
                    Timber.d("✅ Full sync completed: ${result.added} attractions")
                } else {
                    _syncState.value = SyncState.ERROR(result.errorMessage ?: "Full sync failed")
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Force refresh failed")
                _syncState.value = SyncState.ERROR(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * Check if sync is needed (based on last sync timestamp)
     */
    suspend fun isSyncNeeded(): Boolean {
        val lastSync = preferencesManager.getLastSyncTimestamp()
        // If never synced, sync is needed
        return lastSync == null
    }
    
    /**
     * Cancel ongoing sync
     */
    fun cancelSync() {
        _syncState.value = SyncState.IDLE
        _syncProgress.value = 0f
        Timber.d("Sync cancelled")
    }
    
    /**
     * Sync state sealed class
     */
    sealed class SyncState {
        data object IDLE : SyncState()
        data object SYNCING : SyncState()
        data object SUCCESS : SyncState()
        data class ERROR(val message: String) : SyncState()
    }
}
