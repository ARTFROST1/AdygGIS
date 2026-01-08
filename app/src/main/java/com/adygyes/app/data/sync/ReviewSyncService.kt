package com.adygyes.app.data.sync

import com.adygyes.app.data.local.dao.ReviewDao
import com.adygyes.app.data.local.entities.ReviewEntity
import com.adygyes.app.data.remote.NetworkResult
import com.adygyes.app.data.remote.ReviewsRemoteDataSource
import com.adygyes.app.data.remote.dto.ReviewDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for syncing reviews between Room and Supabase.
 * 
 * Architecture:
 * 1. BULK SYNC: Called during main attractions sync - fetches ALL approved reviews
 * 2. DELTA SYNC: Called when reopening a card - fetches only changed reviews for that attraction
 * 3. INSTANT DISPLAY: Card opens show cached data immediately, delta sync happens in background
 * 
 * This ensures:
 * - Reviews load instantly from cache (no network wait on card open)
 * - New reviews appear after background refresh
 * - Minimal network usage via delta sync
 */
@Singleton
class ReviewSyncService @Inject constructor(
    private val reviewsRemoteDataSource: ReviewsRemoteDataSource,
    private val reviewDao: ReviewDao
) {
    
    companion object {
        // Stale threshold: if cache is older than this, do delta sync
        private const val CACHE_STALE_THRESHOLD_MS = 5 * 60 * 1000L // 5 minutes
    }
    
    /**
     * Perform bulk sync of ALL approved reviews.
     * Called during main attractions sync.
     * 
     * @return Number of reviews synced, or -1 on error
     */
    suspend fun performBulkSync(): Int = withContext(Dispatchers.IO) {
        Timber.d("📥 Starting BULK reviews sync...")
        
        val existingCount = reviewDao.countAllApprovedReviews()
        val lastUpdatedAt = reviewDao.getGlobalLatestUpdatedAt()
        
        // If we have cached reviews, do delta sync instead
        if (existingCount > 0 && lastUpdatedAt != null) {
            Timber.d("📊 Found $existingCount cached reviews, performing delta sync instead")
            return@withContext performGlobalDeltaSync(lastUpdatedAt)
        }
        
        // First time: fetch all
        when (val result = reviewsRemoteDataSource.getAllApprovedReviews()) {
            is NetworkResult.Success -> {
                val reviews = result.data

                val localStateById = loadLocalStateById(reviews.map { it.id })
                val entities = reviews.map { dto -> dto.toEntity(localStateById[dto.id]) }
                
                reviewDao.replaceAllApprovedReviews(entities)
                
                Timber.d("✅ Bulk synced ${reviews.size} reviews to Room cache")
                reviews.size
            }
            is NetworkResult.Error -> {
                Timber.e("❌ Bulk reviews sync failed: ${result.code} - ${result.message}")
                -1
            }
        }
    }
    
    /**
     * Delta sync: fetch only reviews updated since last sync.
     */
    private suspend fun performGlobalDeltaSync(since: String): Int {
        Timber.d("📥 Performing global delta reviews sync since $since")
        
        val normalizedSince = normalizeTimestamp(since)
        
        when (val result = reviewsRemoteDataSource.getUpdatedReviews(normalizedSince)) {
            is NetworkResult.Success -> {
                val reviews = result.data
                if (reviews.isEmpty()) {
                    Timber.d("✅ No new reviews since $normalizedSince")
                    return 0
                }
                
                // Insert/update changed reviews (REPLACE strategy)
                val localStateById = loadLocalStateById(reviews.map { it.id })
                val entities = reviews.map { dto -> dto.toEntity(localStateById[dto.id]) }
                reviewDao.insertReviews(entities)
                
                Timber.d("✅ Delta synced ${reviews.size} updated reviews")
                return reviews.size
            }
            is NetworkResult.Error -> {
                Timber.w("⚠️ Delta reviews sync failed: ${result.code} - ${result.message}")
                // Don't fail bulk sync if delta fails - we still have cached data
                return 0
            }
        }
    }
    
    /**
     * Sync reviews for a specific attraction.
     * Called when opening/reopening a card.
     * 
     * Strategy:
     * - If cache is fresh (< 5 min), skip network call
     * - If cache is stale, do delta sync in background
     * - If no cache, do full fetch for this attraction
     * 
     * @return true if sync was performed, false if skipped (cache fresh)
     */
    suspend fun syncReviewsForAttraction(attractionId: String): Boolean = withContext(Dispatchers.IO) {
        val lastSyncTime = reviewDao.getLastSyncTime(attractionId)
        val now = System.currentTimeMillis()
        
        // Check if cache is fresh
        if (lastSyncTime != null && (now - lastSyncTime) < CACHE_STALE_THRESHOLD_MS) {
            Timber.d("⏭️ Reviews cache for $attractionId is fresh (${(now - lastSyncTime) / 1000}s old), skipping sync")
            return@withContext false
        }
        
        val latestUpdatedAt = reviewDao.getLatestUpdatedAt(attractionId)
        
        if (latestUpdatedAt != null) {
            // Delta sync for this attraction
            Timber.d("📥 Delta sync reviews for $attractionId since $latestUpdatedAt")
            performDeltaSyncForAttraction(attractionId, latestUpdatedAt)
        } else {
            // No cached reviews - full fetch for this attraction
            Timber.d("📥 Full fetch reviews for $attractionId (no cache)")
            performFullSyncForAttraction(attractionId)
        }
        
        true
    }
    
    /**
     * Delta sync for a specific attraction.
     */
    private suspend fun performDeltaSyncForAttraction(attractionId: String, since: String) {
        val normalizedSince = normalizeTimestamp(since)
        
        when (val result = reviewsRemoteDataSource.getUpdatedReviewsForAttraction(attractionId, normalizedSince)) {
            is NetworkResult.Success -> {
                val reviews = result.data
                if (reviews.isEmpty()) {
                    // No updates - just refresh lastSyncedAt efficiently
                    Timber.d("✅ No new reviews for $attractionId since $normalizedSince")
                    reviewDao.updateLastSyncTimeForAttraction(attractionId, System.currentTimeMillis())
                    return
                }
                
                // Insert/update changed reviews
                val localStateById = loadLocalStateById(reviews.map { it.id })
                val entities = reviews.map { dto -> dto.toEntity(localStateById[dto.id]) }
                reviewDao.insertReviews(entities)
                
                Timber.d("✅ Delta synced ${reviews.size} reviews for $attractionId")
            }
            is NetworkResult.Error -> {
                Timber.w("⚠️ Delta reviews sync failed for $attractionId: ${result.code} - ${result.message}")
            }
        }
    }
    
    /**
     * Full sync for a specific attraction (when no cache exists).
     */
    private suspend fun performFullSyncForAttraction(attractionId: String) {
        when (val result = reviewsRemoteDataSource.getApprovedReviews(attractionId)) {
            is NetworkResult.Success -> {
                val reviews = result.data

                val localStateById = loadLocalStateById(reviews.map { it.id })
                val entities = reviews.map { dto -> dto.toEntity(localStateById[dto.id]) }
                
                reviewDao.replaceApprovedReviews(attractionId, entities)
                
                Timber.d("✅ Full synced ${reviews.size} reviews for $attractionId")
            }
            is NetworkResult.Error -> {
                Timber.e("❌ Full reviews sync failed for $attractionId: ${result.code} - ${result.message}")
            }
        }
    }
    
    /**
     * Force full refresh for an attraction (ignores cache staleness).
     * Used when user manually pulls to refresh.
     */
    suspend fun forceRefreshForAttraction(attractionId: String) = withContext(Dispatchers.IO) {
        Timber.d("🔄 Force refreshing reviews for $attractionId")
        performFullSyncForAttraction(attractionId)
    }
    
    /**
     * Normalize timestamp to Z format to avoid URL encoding issues.
     */
    private fun normalizeTimestamp(timestamp: String): String {
        return timestamp
            .replace("+00:00", "Z")
            .replace("+0000", "Z")
    }

    private suspend fun loadLocalStateById(ids: List<String>): Map<String, com.adygyes.app.data.local.dao.ReviewLocalState> {
        if (ids.isEmpty()) return emptyMap()

        // Avoid SQLite bind limit by chunking.
        return ids
            .distinct()
            .chunked(500)
            .flatMap { chunk -> reviewDao.getLocalStatesForIds(chunk) }
            .associateBy { it.id }
    }
    
    /**
     * Convert ReviewDto to ReviewEntity for Room storage.
     */
    private fun ReviewDto.toEntity(local: com.adygyes.app.data.local.dao.ReviewLocalState?): ReviewEntity {
        return ReviewEntity(
            id = id,
            attractionId = attractionId,
            userId = userId,
            rating = rating,
            title = title,
            body = body,
            status = status ?: "approved",
            // Preserve local rejectionReason if it exists (important for own reviews).
            rejectionReason = local?.rejectionReason ?: rejectionReason,
            authorName = profile?.displayName ?: "Пользователь",
            authorAvatar = profile?.avatarUrl,
            likesCount = likesCount ?: 0,
            dislikesCount = dislikesCount ?: 0,
            // Preserve local user reaction (LIKE/DISLIKE) so it doesn't disappear after sync.
            userReaction = local?.userReaction,
            createdAt = createdAt,
            updatedAt = updatedAt,
            // Preserve local isOwnReview if this review is already known as user's own.
            isOwnReview = local?.isOwnReview ?: false,
            lastSyncedAt = System.currentTimeMillis()
        )
    }
}
