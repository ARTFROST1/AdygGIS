package com.adygyes.app.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for caching reviews locally
 * 
 * Features:
 * - Caches approved reviews for offline access
 * - Stores user's own reviews (including pending/rejected)
 * - Index on attractionId for fast queries
 * - Foreign key to attractions (with CASCADE delete)
 */
@Entity(
    tableName = "reviews",
    indices = [
        Index(value = ["attractionId"]),
        Index(value = ["userId"]),
        Index(value = ["attractionId", "userId"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = AttractionEntity::class,
            parentColumns = ["id"],
            childColumns = ["attractionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ReviewEntity(
    @PrimaryKey
    val id: String,
    
    val attractionId: String,
    val userId: String?,
    
    // Review content
    val rating: Int,
    val title: String?,
    val body: String?,
    
    // Status for user's own reviews: pending, approved, rejected
    val status: String? = "approved",
    val rejectionReason: String? = null,
    
    // Author info (denormalized for offline display)
    val authorName: String?,
    val authorAvatar: String?,
    
    // Reaction counts
    val likesCount: Int = 0,
    val dislikesCount: Int = 0,
    
    // User's own reaction to this review (LIKE, DISLIKE, NONE)
    val userReaction: String? = null,
    
    // Timestamps
    val createdAt: String?,
    val updatedAt: String?,
    
    // Local-only fields
    val isOwnReview: Boolean = false,
    val lastSyncedAt: Long = System.currentTimeMillis()
)
