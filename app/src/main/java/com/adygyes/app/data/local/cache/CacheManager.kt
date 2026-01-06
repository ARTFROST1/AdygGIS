package com.adygyes.app.data.local.cache

import com.adygyes.app.data.local.preferences.PreferencesManager
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for handling data caching strategies
 */
@Singleton
class CacheManager @Inject constructor(
    private val preferencesManager: PreferencesManager
) {
    
    companion object {
        // Cache expiration times
        private val ATTRACTIONS_CACHE_DURATION = TimeUnit.DAYS.toMillis(7) // 1 week
        private val IMAGES_CACHE_DURATION = TimeUnit.DAYS.toMillis(30) // 30 days
        private val TEMP_CACHE_DURATION = TimeUnit.HOURS.toMillis(1) // 1 hour
        
        // Cache size limits
        const val MAX_MEMORY_CACHE_SIZE = 50 * 1024 * 1024 // 50 MB
        const val MAX_DISK_CACHE_SIZE = 200 * 1024 * 1024 // 200 MB
    }
    
    /**
     * Check if attractions data needs refresh
     */
    suspend fun shouldRefreshAttractions(): Boolean {
        val preferences = preferencesManager.userPreferencesFlow.first()
        val lastSyncTime = preferences.lastSyncTime
        val currentTime = System.currentTimeMillis()
        
        // Check if cache has expired
        val isExpired = (currentTime - lastSyncTime) > ATTRACTIONS_CACHE_DURATION
        
        if (isExpired) {
            Timber.d("Attractions cache expired, refresh needed")
        }
        
        return isExpired
    }
    
    /**
     * Check if image cache is valid
     */
    fun isImageCacheValid(imageUrl: String, lastModified: Long): Boolean {
        val currentTime = System.currentTimeMillis()
        val age = currentTime - lastModified
        return age < IMAGES_CACHE_DURATION
    }
    
    /**
     * Mark attractions data as synced
     */
    suspend fun markAttractionsSynced() {
        val currentTime = System.currentTimeMillis()
        preferencesManager.updateLastSyncTime(currentTime)
        Timber.d("Attractions marked as synced at $currentTime")
    }
    
    /**
     * Check if offline mode is enabled
     */
    suspend fun isOfflineModeEnabled(): Boolean {
        val preferences = preferencesManager.userPreferencesFlow.first()
        return preferences.offlineMode
    }
    
    /**
     * Get cache policy based on current settings
     */
    suspend fun getCachePolicy(): CachePolicy {
        val isOffline = isOfflineModeEnabled()
        val shouldRefresh = shouldRefreshAttractions()
        
        return when {
            isOffline -> CachePolicy.OFFLINE_FIRST
            shouldRefresh -> CachePolicy.NETWORK_FIRST
            else -> CachePolicy.CACHE_FIRST
        }
    }
    
    /**
     * Clear all cache
     */
    suspend fun clearAllCache() {
        // Reset legacy last sync time
        preferencesManager.updateLastSyncTime(0L)

        // Reset Supabase delta sync timestamp
        preferencesManager.clearLastSyncTimestamp()
        Timber.d("All cache cleared")
    }
    
    /**
     * Cache policy enumeration
     */
    enum class CachePolicy {
        CACHE_FIRST,    // Try cache first, fallback to network
        NETWORK_FIRST,  // Try network first, fallback to cache
        OFFLINE_FIRST,  // Use cache only, no network
        NO_CACHE        // Always fetch from network
    }
}
