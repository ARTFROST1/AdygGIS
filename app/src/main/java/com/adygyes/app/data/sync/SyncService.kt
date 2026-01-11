package com.adygyes.app.data.sync

import android.content.Context
import com.adygyes.app.data.local.dao.AttractionDao
import com.adygyes.app.data.local.preferences.PreferencesManager
import com.adygyes.app.data.mapper.AttractionMapper.toEntity
import com.adygyes.app.data.remote.NetworkResult
import com.adygyes.app.data.remote.SupabaseRemoteDataSource
import com.adygyes.app.domain.usecase.NetworkUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service responsible for syncing data between Room and Supabase
 * 
 * Implements delta sync strategy:
 * 1. On first run: Full sync (fetch all attractions + all reviews)
 * 2. On subsequent runs: Delta sync (only changes since last sync)
 * 3. Tombstones: Track deleted/unpublished records for removal
 * 
 * Important: Local favorites are preserved during sync.
 * 
 * Enhanced with:
 * - Network connectivity check before sync
 * - Bulk reviews sync during main sync
 * - Better error handling and recovery
 * - Optimized batch processing
 */
@Singleton
class SyncService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val remoteDataSource: SupabaseRemoteDataSource,
    private val attractionDao: AttractionDao,
    private val preferencesManager: PreferencesManager,
    private val networkUseCase: NetworkUseCase,
    private val reviewSyncService: ReviewSyncService
) {
    
    /**
     * Perform delta sync with Supabase
     * 
     * Steps:
     * 0. Check network connectivity
     * 1. Get last sync timestamp
     * 2. Fetch updated attractions since last sync
     * 3. Fetch deleted attractions (tombstones) since last sync
     * 4. Apply changes to Room DB (preserving favorites)
     * 5. Update last sync timestamp
     */
    suspend fun performSync(): SyncResult = withContext(Dispatchers.IO) {
        try {
            // 0. Check network connectivity
            if (!networkUseCase.isOnline()) {
                val connectionType = networkUseCase.getConnectionType()
                Timber.w("⚠️ No internet connection (type: $connectionType)")
                return@withContext SyncResult(
                    success = false,
                    errorMessage = "Нет подключения к интернету. Проверьте настройки сети."
                )
            }
            
            val connectionType = networkUseCase.getConnectionType()
            Timber.d("🔄 Starting sync with Supabase... (connection: $connectionType)")
            
            // 1. Get last sync timestamp
            val lastSyncTimestamp = preferencesManager.getLastSyncTimestamp()
            val syncSince = lastSyncTimestamp ?: DEFAULT_SYNC_TIMESTAMP
            val isFirstSync = lastSyncTimestamp == null
            
            Timber.d("📅 Last sync: $syncSince (first sync: $isFirstSync)")
            
            // 2. Fetch updated attractions
            val updatedResult = if (isFirstSync) {
                // First sync: get all attractions
                Timber.d("📥 Performing FULL sync (first time)")
                remoteDataSource.getAllAttractions()
            } else {
                // Delta sync: only changes since last sync
                // With fallback to full sync if delta fails
                Timber.d("📥 Attempting DELTA sync since $syncSince")
                val deltaResult = remoteDataSource.getUpdatedAttractions(syncSince)
                
                // If delta sync failed (network issue), try full sync as fallback
                if (deltaResult is NetworkResult.Error) {
                    Timber.w("⚠️ Delta sync failed: ${deltaResult.message}, falling back to full sync")
                    remoteDataSource.getAllAttractions()
                } else {
                    deltaResult
                }
            }
            

            
            // Handle errors
            if (updatedResult is NetworkResult.Error) {
                val errorMsg = getHumanReadableError(updatedResult.message, updatedResult.code)
                Timber.e("❌ Failed to fetch updated attractions: $errorMsg")
                return@withContext SyncResult(
                    success = false,
                    errorMessage = errorMsg
                )
            }
            
            val updatedAttractions = (updatedResult as NetworkResult.Success).data
            
            // 3. Fetch deleted attractions (tombstones)
            // CRITICAL FIX: Re-enabled for full synchronization
            val deletedResult = if (isFirstSync) {
                // First sync - no tombstones needed
                NetworkResult.Success(emptyList<String>())
            } else {
                // Delta sync - fetch tombstones
                Timber.d("📥 Fetching tombstones since $syncSince")
                remoteDataSource.getDeletedAttractions(syncSince)
            }
            
            val deletedIds = when (deletedResult) {
                is NetworkResult.Success -> deletedResult.data
                is NetworkResult.Error -> {
                    Timber.w("⚠️ Could not fetch tombstones: ${deletedResult.message}, continuing without deletions")
                    emptyList()
                }
            }
            
            Timber.d("📊 Sync data: ${updatedAttractions.size} updated/new, ${deletedIds.size} deleted")
            
            // 4. Apply changes to Room DB in batches
            var added = 0
            var updated = 0
            
            // Get current favorites to preserve them
            val favoriteIds = attractionDao.getFavoriteIds().toSet()
            
            // Process in batches for better performance
            val batchSize = 50
            updatedAttractions.chunked(batchSize).forEach { batch ->
                batch.forEach { dto ->
                    val existingEntity = attractionDao.getAttractionById(dto.id)
                    val newEntity = dto.toEntity()
                    
                    if (existingEntity != null) {
                        // Update existing - preserve local favorite status
                        attractionDao.updateAttraction(
                            newEntity.copy(isFavorite = existingEntity.isFavorite)
                        )
                        updated++
                    } else {
                        // Insert new - check if it was a favorite before (edge case)
                        attractionDao.insertAttraction(
                            newEntity.copy(isFavorite = favoriteIds.contains(dto.id))
                        )
                        added++
                    }
                }
            }
            
            // Delete removed attractions
            deletedIds.forEach { id ->
                attractionDao.deleteAttractionById(id)
            }
            
            // 5. Update last sync timestamp
            // Use server timestamp from the latest record
            // If no records received, keep the current sync timestamp (no new changes)
            val newTimestamp = if (updatedAttractions.isNotEmpty()) {
                calculateNewSyncTimestamp(updatedAttractions)
            } else {
                // No new data - use current server time for next delta sync
                Instant.now().toString()
            }
            
            Timber.d("📝 Updating sync timestamp: $syncSince → $newTimestamp")
            preferencesManager.updateLastSyncTimestamp(newTimestamp)
            
            // 6. Bulk sync reviews (runs in parallel-ish, uses same connection pool)
            Timber.d("📥 Syncing reviews...")
            val reviewsCount = reviewSyncService.performBulkSync()
            if (reviewsCount >= 0) {
                Timber.d("✅ Reviews sync complete: $reviewsCount reviews")
            } else {
                Timber.w("⚠️ Reviews sync failed, but attractions sync succeeded")
            }
            
            Timber.d("✅ Sync complete: +$added updated=$updated deleted=${deletedIds.size} reviews=$reviewsCount")
            
            SyncResult(
                success = true,
                added = added,
                updated = updated,
                deleted = deletedIds.size
            )
            
        } catch (e: UnknownHostException) {
            val errorMsg = "Не удалось подключиться к серверу. Проверьте DNS настройки."
            Timber.e(e, "❌ DNS error: $errorMsg")
            SyncResult(success = false, errorMessage = errorMsg)
            
        } catch (e: SocketTimeoutException) {
            val errorMsg = "Превышено время ожидания. Проверьте качество интернет-соединения."
            Timber.e(e, "❌ Timeout: $errorMsg")
            SyncResult(success = false, errorMessage = errorMsg)
            
        } catch (e: IOException) {
            val errorMsg = "Ошибка сети: ${e.message ?: "неизвестная ошибка"}"
            Timber.e(e, "❌ Network error: $errorMsg")
            SyncResult(success = false, errorMessage = errorMsg)
            
        } catch (e: Exception) {
            val errorMsg = "Ошибка синхронизации: ${e.message ?: "неизвестная ошибка"}"
            Timber.e(e, "❌ Sync failed: $errorMsg")
            SyncResult(success = false, errorMessage = errorMsg)
        }
    }
    
    /**
     * Force full reload from Supabase (ignores delta sync)
     * 
     * Use this when:
     * - User manually requests refresh
     * - Data corruption detected
     * - After app update with schema changes
     */
    suspend fun forceFullSync(): SyncResult = withContext(Dispatchers.IO) {
        try {
            // Check network connectivity
            if (!networkUseCase.isOnline()) {
                Timber.w("⚠️ No internet connection for full sync")
                return@withContext SyncResult(
                    success = false,
                    errorMessage = "Нет подключения к интернету"
                )
            }
            
            Timber.d("🔄 Starting FULL sync with Supabase...")
            
            val result = remoteDataSource.getAllAttractions()
            
            when (result) {
                is NetworkResult.Success -> {
                    val attractions = result.data
                    
                    // Save favorite IDs before clearing
                    val favoriteIds = attractionDao.getFavoriteIds().toSet()
                    
                    // Clear existing data
                    attractionDao.deleteAll()
                    
                    // Insert new data in batches, preserving favorites
                    val batchSize = 50
                    attractions.chunked(batchSize).forEach { batch ->
                        batch.forEach { dto ->
                            val entity = dto.toEntity().copy(
                                isFavorite = favoriteIds.contains(dto.id)
                            )
                            attractionDao.insertAttraction(entity)
                        }
                    }
                    
                    // Update sync timestamp
                    val newTimestamp = calculateNewSyncTimestamp(attractions)
                    Timber.d("📝 Full sync - updating timestamp to: $newTimestamp")
                    preferencesManager.updateLastSyncTimestamp(newTimestamp)
                    
                    // Also sync reviews
                    Timber.d("📥 Full syncing reviews...")
                    val reviewsCount = reviewSyncService.performBulkSync()
                    Timber.d("✅ Full sync complete: ${attractions.size} attractions, $reviewsCount reviews")
                    
                    SyncResult(
                        success = true,
                        added = attractions.size
                    )
                }
                is NetworkResult.Error -> {
                    val errorMsg = getHumanReadableError(result.message, result.code)
                    Timber.e("❌ Full sync failed: $errorMsg")
                    SyncResult(
                        success = false,
                        errorMessage = errorMsg
                    )
                }
            }
        } catch (e: UnknownHostException) {
            val errorMsg = "Не удалось подключиться к серверу"
            Timber.e(e, "❌ DNS error")
            SyncResult(success = false, errorMessage = errorMsg)
            
        } catch (e: SocketTimeoutException) {
            val errorMsg = "Превышено время ожидания"
            Timber.e(e, "❌ Timeout")
            SyncResult(success = false, errorMessage = errorMsg)
            
        } catch (e: IOException) {
            val errorMsg = "Ошибка сети: ${e.message}"
            Timber.e(e, "❌ Network error")
            SyncResult(success = false, errorMessage = errorMsg)
            
        } catch (e: Exception) {
            val errorMsg = "Ошибка: ${e.message}"
            Timber.e(e, "❌ Full sync failed")
            SyncResult(success = false, errorMessage = errorMsg)
        }
    }
    
    /**
     * Convert technical error messages to user-friendly Russian text
     */
    private fun getHumanReadableError(message: String?, code: Int?): String {
        return when {
            code == 429 -> "Слишком много запросов. Подождите немного."
            code in 500..599 -> "Сервер временно недоступен. Попробуйте позже."
            code == 401 || code == 403 -> "Ошибка авторизации. Обновите приложение."
            message?.contains("timeout", ignoreCase = true) == true -> 
                "Превышено время ожидания. Проверьте качество связи."
            message?.contains("host", ignoreCase = true) == true -> 
                "Не удалось подключиться к серверу."
            message?.contains("ssl", ignoreCase = true) == true -> 
                "Ошибка безопасного соединения."
            else -> message ?: "Неизвестная ошибка сети"
        }
    }
    
    /**
     * Calculate new sync timestamp from fetched data
     * 
     * Uses the maximum updated_at from server data to avoid clock skew issues.
     * Falls back to current time if no data or all timestamps are null.
     * 
     * IMPORTANT: Normalizes timestamp to Z format to avoid URL encoding issues on cellular networks.
     */
    private fun calculateNewSyncTimestamp(
        attractions: List<com.adygyes.app.data.remote.dto.AttractionDto>
    ): String {
        if (attractions.isEmpty()) {
            val timestamp = Instant.now().toString()
            Timber.d("⏱️ No attractions, using current time: $timestamp")
            return timestamp
        }
        
        val maxUpdatedAt = attractions
            .mapNotNull { it.updatedAt }
            .maxOrNull()
        
        val rawTimestamp = maxUpdatedAt ?: Instant.now().toString()
        
        // Normalize to Z format: +00:00 → Z, +0000 → Z
        // This prevents URL encoding issues on cellular networks
        val normalizedTimestamp = rawTimestamp
            .replace("+00:00", "Z")
            .replace("+0000", "Z")
        
        Timber.d("⏱️ Calculated new timestamp from ${attractions.size} attractions: $rawTimestamp → $normalizedTimestamp")
        
        return normalizedTimestamp
    }
    
    companion object {
        // Far past timestamp to get all data on first sync
        private const val DEFAULT_SYNC_TIMESTAMP = "1970-01-01T00:00:00Z"
    }
}
