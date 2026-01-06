package com.adygyes.app.domain.model

import java.time.Instant

/**
 * Review model representing user's rating and feedback for an attraction
 * Unified with RN Review type from src/types/review.ts
 */
data class Review(
    val id: String,
    val attractionId: String,
    val authorId: String,
    val authorName: String,
    val authorAvatar: String? = null,
    val authorBadge: String? = null, // e.g., "Знаток города 5 уровня"
    val rating: Int, // 1-5
    val text: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant? = null,
    val likes: Int = 0,
    val dislikes: Int = 0,
    val isOwn: Boolean = false // Whether this is current user's review
)

/**
 * Data for submitting a new review
 */
data class ReviewSubmission(
    val attractionId: String,
    val rating: Int, // 1-5
    val text: String? = null
)

/**
 * Sort options for reviews list
 */
enum class ReviewSortOption(val displayName: String) {
    DEFAULT("По популярности"),
    NEWEST("Сначала новые"),
    OLDEST("Сначала старые"),
    HIGHEST("Высокая оценка"),
    LOWEST("Низкая оценка")
}

/**
 * User reaction to a review
 */
enum class ReviewReaction {
    LIKE,
    DISLIKE,
    NONE
}
