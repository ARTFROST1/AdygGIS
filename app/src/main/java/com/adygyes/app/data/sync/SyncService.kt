package com.adygyes.app.data.sync

import com.adygyes.app.data.local.dao.AttractionDao
import com.adygyes.app.data.local.preferences.PreferencesManager
import com.adygyes.app.data.mapper.AttractionMapper.toEntity
import com.adygyes.app.data.remote.NetworkResult
import com.adygyes.app.data.remote.SupabaseRemoteDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service responsible for syncing data between Room and Supabase
 * 
 * Implements delta sync strategy:
 * 1. On first run: Full sync (fetch all attractions)
 * 2. On subsequent runs: Delta sync (only changes since last sync)
 * 3. Tombstones: Track deleted/unpublished records for removal
 * 
 * Important: Local favorites are preserved during sync.
 */
@Singleton
class SyncService @Inject constructor(
    private val remoteDataSource: SupabaseRemoteDataSource,
    private val attractionDao: AttractionDao,
    private val preferencesManager: PreferencesManager
) {
    
    /**
     * Perform delta sync with Supabase
     * 
     * Steps:
     * 1. Get last sync timestamp
     * 2. Fetch updated attractions since last sync
     * 3. Fetch deleted attractions (tombstones) since last sync
     * 4. Apply changes to Room DB (preserving favorites)
     * 5. Update last sync timestamp
     */
    suspend fun performSync(): SyncResult = withContext(Dispatchers.IO) {
        try {
            Timber.d("🔄 Starting sync with Supabase...")
            
            // 1. Get last sync timestamp
            val lastSyncTimestamp = preferencesManager.getLastSyncTimestamp()
            val syncSince = lastSyncTimestamp ?: DEFAULT_SYNC_TIMESTAMP
            val isFirstSync = lastSyncTimestamp == null
            
            Timber.d("📅 Last sync: $syncSince (first sync: $isFirstSync)")
            
            // 2. Fetch updated attractions
            val updatedResult = if (isFirstSync) {
                // First sync: get all attractions
                remoteDataSource.getAllAttractions()
            } else {
                // Delta sync: only changes since last sync
                remoteDataSource.getUpdatedAttractions(syncSince)
            }
            
            // 3. Fetch deleted attractions (tombstones)
            val deletedResult = remoteDataSource.getDeletedAttractions(syncSince)
            
            // Handle errors
            if (updatedResult is NetworkResult.Error) {
                Timber.e("❌ Failed to fetch updated attractions: ${updatedResult.message}")
                return@withContext SyncResult(
                    success = false,
                    errorMessage = updatedResult.message
                )
            }
            
            val updatedAttractions = (updatedResult as NetworkResult.Success).data
            val deletedIds = when (deletedResult) {
                is NetworkResult.Success -> deletedResult.data
                is NetworkResult.Error -> {
                    Timber.w("⚠️ Could not fetch tombstones: ${deletedResult.message}")
                    emptyList()
                }
            }
            
            Timber.d("📊 Sync data: ${updatedAttractions.size} updated/new, ${deletedIds.size} deleted")
            
            // 4. Apply changes to Room DB
            var added = 0
            var updated = 0
            
            // Get current favorites to preserve them
            val favoriteIds = attractionDao.getFavoriteIds()
            
            updatedAttractions.forEach { dto ->
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
            
            // Delete removed attractions
            deletedIds.forEach { id ->
                attractionDao.deleteAttractionById(id)
            }
            
            // 5. Update last sync timestamp
            // Use server timestamp from the latest record, or current time
            val newTimestamp = calculateNewSyncTimestamp(updatedAttractions)
            preferencesManager.updateLastSyncTimestamp(newTimestamp)
            
            Timber.d("✅ Sync complete: +$added updated=$updated deleted=${deletedIds.size}")
            
            SyncResult(
                success = true,
                added = added,
                updated = updated,
                deleted = deletedIds.size
            )
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Sync failed")
            SyncResult(
                success = false,
                errorMessage = e.message
            )
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
            Timber.d("🔄 Starting FULL sync with Supabase...")
            
            val result = remoteDataSource.getAllAttractions()
            
            when (result) {
                is NetworkResult.Success -> {
                    val attractions = result.data
                    
                    // Save favorite IDs before clearing
                    val favoriteIds = attractionDao.getFavoriteIds().toSet()
                    
                    // Clear existing data
                    attractionDao.deleteAll()
                    
                    // Insert new data, preserving favorites
                    attractions.forEach { dto ->
                        val entity = dto.toEntity().copy(
                            isFavorite = favoriteIds.contains(dto.id)
                        )
                        attractionDao.insertAttraction(entity)
                    }
                    
                    // Update sync timestamp
                    val newTimestamp = calculateNewSyncTimestamp(attractions)
                    preferencesManager.updateLastSyncTimestamp(newTimestamp)
                    
                    Timber.d("✅ Full sync complete: ${attractions.size} attractions")
                    
                    SyncResult(
                        success = true,
                        added = attractions.size
                    )
                }
                is NetworkResult.Error -> {
                    Timber.e("❌ Full sync failed: ${result.message}")
                    SyncResult(
                        success = false,
                        errorMessage = result.message
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Full sync failed")
            SyncResult(
                success = false,
                errorMessage = e.message
            )
        }
    }
    
    /**
     * Calculate new sync timestamp from fetched data
     * 
     * Uses the maximum updated_at from server data to avoid clock skew issues.
     * Falls back to current time if no data.
     */
    private fun calculateNewSyncTimestamp(
        attractions: List<com.adygyes.app.data.remote.dto.AttractionDto>
    ): String {
        val maxUpdatedAt = attractions
            .mapNotNull { it.updatedAt }
            .maxOrNull()
        
        return maxUpdatedAt ?: Instant.now().toString()
    }
    
    companion object {
        // Far past timestamp to get all data on first sync
        private const val DEFAULT_SYNC_TIMESTAMP = "1970-01-01T00:00:00Z"
    }
}
