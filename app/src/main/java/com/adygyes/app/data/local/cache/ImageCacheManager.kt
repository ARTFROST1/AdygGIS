package com.adygyes.app.data.local.cache

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache as CoilDiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages image caching for the application using Coil
 * Integrates with JSON versioning to clear cache when data updates
 */
@Singleton
class ImageCacheManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val MEMORY_CACHE_MAX_SIZE_PERCENT = 0.25 // 25% of app memory
        private const val DISK_CACHE_MAX_SIZE_MB = 250L // 250MB disk cache
        private const val CACHE_DIRECTORY = "image_cache"
        
        // Cache keys for preferences
        private const val PREFS_NAME = "image_cache_prefs"
        private const val KEY_CACHE_VERSION = "cache_version"
    }
    
    val imageLoader: ImageLoader by lazy {
        // Custom OkHttpClient with headers for external images
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("User-Agent", "AdygGIS Android App")
                    .addHeader("Accept", "image/webp,image/png,image/jpeg,image/*,*/*")
                    .build()
                chain.proceed(request)
            }
            .build()

        ImageLoader.Builder(context)
            .okHttpClient(okHttpClient)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(MEMORY_CACHE_MAX_SIZE_PERCENT)
                    .build()
            }
            .diskCache {
                CoilDiskCache.Builder()
                    .directory(getCacheDirectory())
                    .maxSizeBytes(DISK_CACHE_MAX_SIZE_MB * 1024 * 1024)
                    .build()
            }
            .respectCacheHeaders(false) // Ignore server cache headers
            .crossfade(true) // Enable crossfade animation
            .build()
    }
    
    /**
     * Get cache directory for images
     */
    private fun getCacheDirectory(): File {
        return File(context.cacheDir, CACHE_DIRECTORY).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }
    
    /**
     * Check if image is cached
     */
    suspend fun isImageCached(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = ImageRequest.Builder(context)
                .data(url)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()
            
            val memoryCache = imageLoader.memoryCache
            val diskCache = imageLoader.diskCache
            
            // Check memory cache
            val memoryCacheKey = MemoryCache.Key(url)
            if (memoryCache?.get(memoryCacheKey) != null) {
                Timber.d("✅ Image found in memory cache: ${url.substringAfterLast('/')}")
                return@withContext true
            }
            
            // Check disk cache
            val diskCacheKey = url
            val snapshot = diskCache?.openSnapshot(diskCacheKey)
            if (snapshot != null) {
                snapshot.close()
                Timber.d("✅ Image found in disk cache: ${url.substringAfterLast('/')}")
                return@withContext true
            }
            
            false
        } catch (e: Exception) {
            Timber.e(e, "Error checking cache for image: $url")
            false
        }
    }
    
    /**
     * Preload image into cache
     */
    suspend fun preloadImage(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Timber.d("🔄 Preloading image: ${url.substringAfterLast('/')}")
            
            val request = ImageRequest.Builder(context)
                .data(url)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()
            
            val result = imageLoader.execute(request)
            
            if (result.drawable != null) {
                Timber.d("✅ Successfully preloaded: ${url.substringAfterLast('/')}")
                true
            } else {
                Timber.w("⚠️ Failed to preload: ${url.substringAfterLast('/')}")
                false
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Coroutine cancelled - don't log as error
            Timber.d("Image preload cancelled: ${url.substringAfterLast('/')}")
            throw e // Re-throw to propagate cancellation
        } catch (e: Exception) {
            Timber.e(e, "Error preloading image: $url")
            false
        }
    }
    
    /**
     * Preload multiple images in parallel
     */
    suspend fun preloadImages(urls: List<String>) = withContext(Dispatchers.IO) {
        urls.forEach { url ->
            preloadImage(url)
        }
    }
    
    /**
     * Clear all image caches
     */
    suspend fun clearAllCache() = withContext(Dispatchers.IO) {
        try {
            Timber.d("🗑️ Clearing all image caches")
            
            // Clear memory cache
            imageLoader.memoryCache?.clear()
            
            // Clear disk cache
            imageLoader.diskCache?.clear()
            
            // Update cache version
            updateCacheVersion()
            
            Timber.d("✅ All image caches cleared")
        } catch (e: Exception) {
            Timber.e(e, "Error clearing image cache")
        }
    }
    
    /**
     * Clear cache for specific URL
     */
    suspend fun clearImageCache(url: String) = withContext(Dispatchers.IO) {
        try {
            // Remove from memory cache
            val memoryCacheKey = MemoryCache.Key(url)
            imageLoader.memoryCache?.remove(memoryCacheKey)
            
            // Remove from disk cache
            imageLoader.diskCache?.remove(url)
            
            Timber.d("✅ Cleared cache for: ${url.substringAfterLast('/')}")
        } catch (e: Exception) {
            Timber.e(e, "Error clearing cache for image: $url")
        }
    }
    
    /**
     * Get current cache size info
     */
    suspend fun getCacheInfo(): CacheInfo = withContext(Dispatchers.IO) {
        try {
            val memoryCache = imageLoader.memoryCache
            val diskCache = imageLoader.diskCache
            
            val memorySizeBytes = memoryCache?.size?.toLong() ?: 0L
            val memoryMaxSizeBytes = memoryCache?.maxSize?.toLong() ?: 0L
            
            val diskSizeBytes = getCacheDirectorySize()
            val diskMaxSizeBytes = DISK_CACHE_MAX_SIZE_MB * 1024 * 1024
            
            CacheInfo(
                memorySizeBytes = memorySizeBytes,
                diskSizeBytes = diskSizeBytes,
                memoryMaxSizeBytes = memoryMaxSizeBytes,
                diskMaxSizeBytes = diskMaxSizeBytes
            )
        } catch (e: Exception) {
            Timber.e(e, "Error getting cache info")
            CacheInfo()
        }
    }
    
    /**
     * Get cache directory size
     */
    private fun getCacheDirectorySize(): Long {
        return getCacheDirectory().walkTopDown()
            .filter { it.isFile }
            .map { it.length() }
            .sum()
    }
    
    /**
     * Check if cache needs to be cleared based on JSON version
     */
    fun shouldClearCache(jsonVersion: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cachedVersion = prefs.getString(KEY_CACHE_VERSION, null)
        
        return if (cachedVersion == null || cachedVersion != jsonVersion) {
            Timber.d("📦 Cache version mismatch. Cached: $cachedVersion, Current: $jsonVersion")
            true
        } else {
            false
        }
    }
    
    /**
     * Update cache version after clearing
     */
    fun updateCacheVersion(version: String? = null) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (version != null) {
            prefs.edit()
                .putString(KEY_CACHE_VERSION, version)
                .apply()
            Timber.d("✅ Updated cache version to: $version")
        } else {
            // Clear version on cache clear
            prefs.edit()
                .remove(KEY_CACHE_VERSION)
                .apply()
        }
    }
    
    /**
     * Data class for cache information
     */
    data class CacheInfo(
        val memorySizeBytes: Long = 0,
        val diskSizeBytes: Long = 0,
        val memoryMaxSizeBytes: Long = 0,
        val diskMaxSizeBytes: Long = 0
    ) {
        val memorySizeMB: Float get() = memorySizeBytes / (1024f * 1024f)
        val diskSizeMB: Float get() = diskSizeBytes / (1024f * 1024f)
        val memoryUsagePercent: Float get() = if (memoryMaxSizeBytes > 0) 
            (memorySizeBytes.toFloat() / memoryMaxSizeBytes) * 100f else 0f
        val diskUsagePercent: Float get() = if (diskMaxSizeBytes > 0) 
            (diskSizeBytes.toFloat() / diskMaxSizeBytes) * 100f else 0f
    }
}
