package com.adygyes.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.adygyes.app.data.local.entities.ReviewEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for review operations
 * 
 * Features:
 * - Caches approved public reviews
 * - Stores user's own reviews (pending, approved, rejected)
 * - Supports offline-first access
 */
@Dao
interface ReviewDao {
    
    // ========== Queries ==========
    
    /**
     * Get all approved reviews for an attraction (excluding user's own)
     * Used for public reviews display
     */
    @Query("""
        SELECT * FROM reviews 
        WHERE attractionId = :attractionId 
        AND status = 'approved' 
        AND isOwnReview = 0
        ORDER BY createdAt DESC
    """)
    fun getApprovedReviewsFlow(attractionId: String): Flow<List<ReviewEntity>>
    
    @Query("""
        SELECT * FROM reviews 
        WHERE attractionId = :attractionId 
        AND status = 'approved' 
        AND isOwnReview = 0
        ORDER BY createdAt DESC
    """)
    suspend fun getApprovedReviews(attractionId: String): List<ReviewEntity>
    
    /**
     * Get user's own reviews for an attraction (all statuses)
     */
    @Query("""
        SELECT * FROM reviews 
        WHERE attractionId = :attractionId 
        AND userId = :userId
        AND isOwnReview = 1
        ORDER BY createdAt DESC
    """)
    fun getUserReviewsFlow(attractionId: String, userId: String): Flow<List<ReviewEntity>>
    
    @Query("""
        SELECT * FROM reviews 
        WHERE attractionId = :attractionId 
        AND userId = :userId
        AND isOwnReview = 1
        ORDER BY createdAt DESC
    """)
    suspend fun getUserReviews(attractionId: String, userId: String): List<ReviewEntity>
    
    /**
     * Get all user's own reviews across all attractions
     */
    @Query("""
        SELECT * FROM reviews 
        WHERE userId = :userId
        AND isOwnReview = 1
        ORDER BY createdAt DESC
    """)
    suspend fun getAllUserReviews(userId: String): List<ReviewEntity>
    
    /**
     * Check if user already has a review for this attraction
     */
    @Query("""
        SELECT COUNT(*) > 0 FROM reviews 
        WHERE attractionId = :attractionId 
        AND userId = :userId
        AND isOwnReview = 1
    """)
    suspend fun hasUserReviewed(attractionId: String, userId: String): Boolean
    
    /**
     * Get single review by ID
     */
    @Query("SELECT * FROM reviews WHERE id = :reviewId")
    suspend fun getReviewById(reviewId: String): ReviewEntity?
    
    /**
     * Get reviews count for attraction
     */
    @Query("SELECT COUNT(*) FROM reviews WHERE attractionId = :attractionId AND status = 'approved'")
    suspend fun getReviewsCount(attractionId: String): Int
    
    /**
     * Get average rating for attraction
     */
    @Query("SELECT AVG(CAST(rating AS FLOAT)) FROM reviews WHERE attractionId = :attractionId AND status = 'approved'")
    suspend fun getAverageRating(attractionId: String): Float?
    
    // ========== Inserts & Updates ==========
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: ReviewEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviews(reviews: List<ReviewEntity>)
    
    @Update
    suspend fun updateReview(review: ReviewEntity)
    
    /**
     * Update user's reaction to a review
     */
    @Query("""
        UPDATE reviews 
        SET userReaction = :reaction, likesCount = :likesCount, dislikesCount = :dislikesCount
        WHERE id = :reviewId
    """)
    suspend fun updateReaction(reviewId: String, reaction: String?, likesCount: Int, dislikesCount: Int)

    /**
     * Update only the user's reaction without touching counts.
     * Useful when syncing per-user reactions from server.
     */
    @Query("UPDATE reviews SET userReaction = :reaction WHERE id = :reviewId")
    suspend fun updateUserReactionOnly(reviewId: String, reaction: String?)
    
    // ========== Deletes ==========
    
    @Query("DELETE FROM reviews WHERE id = :reviewId")
    suspend fun deleteReview(reviewId: String)
    
    @Query("DELETE FROM reviews WHERE attractionId = :attractionId AND isOwnReview = 0")
    suspend fun deleteApprovedReviewsForAttraction(attractionId: String)

    @Query("DELETE FROM reviews WHERE attractionId = :attractionId AND userId = :userId AND isOwnReview = 1")
    suspend fun deleteUserOwnReviewsForAttraction(attractionId: String, userId: String)

    @Query("DELETE FROM reviews WHERE userId = :userId AND isOwnReview = 1")
    suspend fun deleteAllUserOwnReviews(userId: String)
    
    @Query("DELETE FROM reviews WHERE attractionId = :attractionId")
    suspend fun deleteAllReviewsForAttraction(attractionId: String)
    
    @Query("DELETE FROM reviews")
    suspend fun deleteAll()
    
    // ========== Sync helpers ==========
    
    /**
     * Replace all approved reviews for an attraction
     * Used during sync to update cache
     */
    @Transaction
    suspend fun replaceApprovedReviews(attractionId: String, reviews: List<ReviewEntity>) {
        deleteApprovedReviewsForAttraction(attractionId)
        insertReviews(reviews)
    }

    @Transaction
    suspend fun replaceUserOwnReviewsForAttraction(
        attractionId: String,
        userId: String,
        reviews: List<ReviewEntity>
    ) {
        deleteUserOwnReviewsForAttraction(attractionId, userId)
        insertReviews(reviews)
    }

    @Transaction
    suspend fun replaceAllUserOwnReviews(userId: String, reviews: List<ReviewEntity>) {
        deleteAllUserOwnReviews(userId)
        insertReviews(reviews)
    }
    
    /**
     * Replace ALL approved reviews (bulk sync).
     * Used during main attractions sync to pre-populate cache.
     */
    @Transaction
    suspend fun replaceAllApprovedReviews(reviews: List<ReviewEntity>) {
        deleteAllApprovedReviews()
        insertReviews(reviews)
    }
    
    @Query("DELETE FROM reviews WHERE isOwnReview = 0")
    suspend fun deleteAllApprovedReviews()
    
    /**
     * Get last sync time for reviews of an attraction
     */
    @Query("""
        SELECT MAX(lastSyncedAt) FROM reviews 
        WHERE attractionId = :attractionId 
        AND isOwnReview = 0
    """)
    suspend fun getLastSyncTime(attractionId: String): Long?
    
    /**
     * Get global last sync time for all reviews
     */
    @Query("SELECT MAX(lastSyncedAt) FROM reviews WHERE isOwnReview = 0")
    suspend fun getGlobalLastSyncTime(): Long?
    
    /**
     * Get the latest updated_at timestamp from cached reviews for an attraction.
     * Used for delta sync.
     */
    @Query("""
        SELECT MAX(updatedAt) FROM reviews 
        WHERE attractionId = :attractionId 
        AND isOwnReview = 0
    """)
    suspend fun getLatestUpdatedAt(attractionId: String): String?
    
    /**
     * Get the global latest updated_at timestamp from all cached reviews.
     */
    @Query("SELECT MAX(updatedAt) FROM reviews WHERE isOwnReview = 0")
    suspend fun getGlobalLatestUpdatedAt(): String?
    
    /**
     * Count total cached approved reviews
     */
    @Query("SELECT COUNT(*) FROM reviews WHERE isOwnReview = 0")
    suspend fun countAllApprovedReviews(): Int
    
    /**
     * Update lastSyncedAt for all approved reviews of an attraction.
     * Efficient batch update without full re-insert.
     */
    @Query("UPDATE reviews SET lastSyncedAt = :syncTime WHERE attractionId = :attractionId AND isOwnReview = 0")
    suspend fun updateLastSyncTimeForAttraction(attractionId: String, syncTime: Long)
    
    /**
     * Update lastSyncedAt for all approved reviews globally.
     */
    @Query("UPDATE reviews SET lastSyncedAt = :syncTime WHERE isOwnReview = 0")
    suspend fun updateGlobalLastSyncTime(syncTime: Long)

    /**
     * Fetch local-only fields for a set of review IDs.
     * Used to avoid overwriting local state (isOwnReview/userReaction/rejectionReason)
     * when syncing public approved reviews.
     */
    @Query("""
        SELECT id, isOwnReview, userReaction, rejectionReason
        FROM reviews
        WHERE id IN (:ids)
    """)
    suspend fun getLocalStatesForIds(ids: List<String>): List<ReviewLocalState>

    /**
     * Fetch current cached userReaction for a set of review IDs.
     * Used to avoid N queries when reconciling reactions from the server.
     */
    @Query("""
        SELECT id, userReaction
        FROM reviews
        WHERE id IN (:ids)
    """)
    suspend fun getCachedReactionsForIds(ids: List<String>): List<ReviewReactionState>
}

data class ReviewLocalState(
    val id: String,
    val isOwnReview: Boolean,
    val userReaction: String?,
    val rejectionReason: String?
)

data class ReviewReactionState(
    val id: String,
    val userReaction: String?
)
