package com.adygyes.app.data.repository

import com.adygyes.app.data.remote.NetworkResult
import com.adygyes.app.data.remote.ReviewsRemoteDataSource
import com.adygyes.app.domain.model.Review
import com.adygyes.app.domain.model.ReviewSortOption
import com.adygyes.app.domain.model.ReviewSubmission
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing reviews.
 *
 * IMPORTANT:
 * - Reads: uses Supabase approved reviews (public via RLS).
 * - Writes: requires Supabase Auth (auth.uid) per schema/RLS; Kotlin app has no Auth yet,
 *   so submitReview() must fail (to avoid fake "moderation").
 */
@Singleton
class ReviewRepository @Inject constructor(
    private val reviewsRemoteDataSource: ReviewsRemoteDataSource
) {

    private val _reviews = MutableStateFlow<Map<String, List<Review>>>(emptyMap())

    // Mock data (offline/demo fallback only)
    private val mockReviewsTemplate = listOf(
        Review(
            id = "r1",
            attractionId = "",
            authorId = "u1",
            authorName = "Иван Петров",
            authorBadge = "Знаток города 5 уровня",
            rating = 5,
            text = "Потрясающее место! Виды невероятные, особенно на рассвете. Тропа хорошо размечена, но требует хорошей физической подготовки.",
            createdAt = Instant.now().minus(9, ChronoUnit.DAYS),
            likes = 12,
            dislikes = 1
        ),
        Review(
            id = "r2",
            attractionId = "",
            authorId = "u2",
            authorName = "Мария Сидорова",
            authorBadge = "Путешественник",
            rating = 4,
            text = "Красивые виды, но подъём действительно сложный. Советую брать с собой много воды.",
            createdAt = Instant.now().minus(12, ChronoUnit.DAYS),
            likes = 8,
            dislikes = 0
        ),
        Review(
            id = "r3",
            attractionId = "",
            authorId = "u3",
            authorName = "Алексей Краснов",
            rating = 5,
            text = "Были всей семьёй, дети в восторге! Обязательно вернёмся ещё.",
            createdAt = Instant.now().minus(17, ChronoUnit.DAYS),
            likes = 15,
            dislikes = 2
        )
    )

    private fun getMockReviewsForAttraction(attractionId: String): List<Review> {
        return mockReviewsTemplate.mapIndexed { index, review ->
            review.copy(
                id = "r${attractionId}_$index",
                attractionId = attractionId
            )
        }
    }

    fun getReviewsForAttraction(
        attractionId: String,
        sortBy: ReviewSortOption = ReviewSortOption.DEFAULT
    ): Flow<List<Review>> {
        return _reviews.map { reviewsMap ->
            val reviews = reviewsMap[attractionId] ?: emptyList()
            sortReviews(reviews, sortBy)
        }
    }

    /**
     * Refresh approved reviews from Supabase into local in-memory cache.
     * Keeps UI reactive via the Flow from getReviewsForAttraction().
     */
    suspend fun refreshApprovedReviews(attractionId: String) {
        when (val result = reviewsRemoteDataSource.getApprovedReviews(attractionId)) {
            is NetworkResult.Success -> {
                val mapped = result.data.map { dto ->
                    Review(
                        id = dto.id,
                        attractionId = dto.attractionId,
                        authorId = dto.userId ?: "",
                        authorName = dto.profile?.displayName ?: "Пользователь",
                        authorAvatar = dto.profile?.avatarUrl,
                        authorBadge = null,
                        rating = dto.rating,
                        text = listOfNotNull(dto.title, dto.body)
                            .joinToString("\n")
                            .ifBlank { null },
                        createdAt = parseInstant(dto.createdAt) ?: Instant.now(),
                        updatedAt = parseInstant(dto.updatedAt),
                        likes = 0,
                        dislikes = 0,
                        isOwn = false
                    )
                }

                _reviews.value = _reviews.value.toMutableMap().apply {
                    put(attractionId, mapped)
                }
            }

            is NetworkResult.Error -> {
                Timber.w(
                    "⚠️ Reviews fetch failed, fallback to mock (if empty). ${result.code}: ${result.message}"
                )
                if ((_reviews.value[attractionId] ?: emptyList()).isEmpty()) {
                    _reviews.value = _reviews.value.toMutableMap().apply {
                        put(attractionId, getMockReviewsForAttraction(attractionId))
                    }
                }
            }
        }
    }

    suspend fun fetchReviews(attractionId: String): Result<List<Review>> {
        return try {
            refreshApprovedReviews(attractionId)
            Result.success(_reviews.value[attractionId] ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitReview(submission: ReviewSubmission): Result<Review> {
        return Result.failure(
            IllegalStateException(
                "Отправка отзыва в Supabase требует авторизации (Supabase Auth). Сейчас доступно только чтение одобренных отзывов."
            )
        )
    }

    suspend fun updateReaction(reviewId: String, isLike: Boolean): Result<Unit> {
        return try {
            // Reactions are local-only for now (schema doesn't store them).
            delay(200)

            _reviews.value = _reviews.value.mapValues { (_, reviews) ->
                reviews.map { review ->
                    if (review.id == reviewId) {
                        if (isLike) review.copy(likes = review.likes + 1)
                        else review.copy(dislikes = review.dislikes + 1)
                    } else {
                        review
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseInstant(value: String?): Instant? {
        return try {
            if (value.isNullOrBlank()) null else Instant.parse(value)
        } catch (_: Exception) {
            null
        }
    }

    private fun sortReviews(reviews: List<Review>, sortBy: ReviewSortOption): List<Review> {
        return when (sortBy) {
            ReviewSortOption.NEWEST -> reviews.sortedByDescending { it.createdAt }
            ReviewSortOption.OLDEST -> reviews.sortedBy { it.createdAt }
            ReviewSortOption.HIGHEST -> reviews.sortedByDescending { it.rating }
            ReviewSortOption.LOWEST -> reviews.sortedBy { it.rating }
            ReviewSortOption.DEFAULT -> {
                reviews.sortedWith(
                    compareByDescending<Review> { it.likes - it.dislikes }
                        .thenByDescending { it.createdAt }
                )
            }
        }
    }
}
