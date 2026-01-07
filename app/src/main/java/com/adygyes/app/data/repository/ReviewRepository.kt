package com.adygyes.app.data.repository

import com.adygyes.app.data.remote.NetworkResult
import com.adygyes.app.data.remote.ReviewsRemoteDataSource
import com.adygyes.app.data.remote.api.SupabaseApiService
import com.adygyes.app.data.remote.dto.CreateReviewRequest
import com.adygyes.app.domain.model.AuthState
import com.adygyes.app.domain.model.Review
import com.adygyes.app.domain.model.ReviewReaction
import com.adygyes.app.domain.model.ReviewSortOption
import com.adygyes.app.domain.model.ReviewSubmission
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing reviews.
 *
 * Features:
 * - Reads: public approved reviews via Supabase RLS
 * - Writes: authenticated users can submit reviews (status='pending')
 * - User's own reviews: visible regardless of status when authenticated
 */
@Singleton
class ReviewRepository @Inject constructor(
    private val reviewsRemoteDataSource: ReviewsRemoteDataSource,
    private val supabaseApi: SupabaseApiService,
    private val authRepository: AuthRepository
) {

    private val _reviews = MutableStateFlow<Map<String, List<Review>>>(emptyMap())
    
    // User's own reviews (including pending/rejected)
    private val _userOwnReviews = MutableStateFlow<List<Review>>(emptyList())
    val userOwnReviews: StateFlow<List<Review>> = _userOwnReviews.asStateFlow()
    
    // Submission state
    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

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
            likesCount = 12,
            dislikesCount = 1
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
            likesCount = 8,
            dislikesCount = 0
        ),
        Review(
            id = "r3",
            attractionId = "",
            authorId = "u3",
            authorName = "Алексей Краснов",
            rating = 5,
            text = "Были всей семьёй, дети в восторге! Обязательно вернёмся ещё.",
            createdAt = Instant.now().minus(17, ChronoUnit.DAYS),
            likesCount = 15,
            dislikesCount = 2
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
     * @param excludeUserId - ID пользователя, чьи отзывы нужно исключить из списка (чтобы избежать дублирования)
     */
    suspend fun refreshApprovedReviews(attractionId: String, excludeUserId: String? = null) {
        when (val result = reviewsRemoteDataSource.getApprovedReviews(attractionId)) {
            is NetworkResult.Success -> {
                val currentUserId = getCurrentUserId()
                val base = result.data
                    // КРИТИЧНО: исключаем отзывы текущего пользователя из публичного списка
                    .filter { dto -> excludeUserId == null || dto.userId != excludeUserId }
                    .map { dto ->
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
                        likesCount = dto.likesCount ?: 0,
                        dislikesCount = dto.dislikesCount ?: 0,
                        isOwn = dto.userId == currentUserId,
                        userReaction = ReviewReaction.NONE
                    )
                }

                // If authenticated: enrich reviews with current user's reactions (LIKE/DISLIKE)
                val mapped = enrichWithUserReactions(base)

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
    
    /**
     * Check if current user has already reviewed this attraction.
     */
    suspend fun hasUserReviewed(attractionId: String): Boolean {
        val authState = authRepository.authState.value
        if (authState !is AuthState.Authenticated) return false
        
        return try {
            val response = supabaseApi.checkUserReviewExists(
                authorization = "Bearer ${authState.accessToken}",
                attractionId = "eq.$attractionId",
                userId = "eq.${authState.user.id}"
            )
            
            response.isSuccessful && (response.body()?.isNotEmpty() == true)
        } catch (e: Exception) {
            Timber.w(e, "Error checking if user reviewed attraction")
            false
        }
    }

    /**
     * Submit a new review (requires authentication).
     * Review is created with status='pending' and goes to moderation.
     */
    suspend fun submitReview(submission: ReviewSubmission): Result<Review> {
        val authState = authRepository.authState.value
        
        if (authState !is AuthState.Authenticated) {
            return Result.failure(
                ReviewException("Чтобы оставить отзыв, необходимо войти в аккаунт")
            )
        }
        
        // Check for duplicate
        if (hasUserReviewed(submission.attractionId)) {
            return Result.failure(
                ReviewException("Вы уже оставили отзыв для этого места")
            )
        }
        
        return try {
            _isSubmitting.value = true
            
            val request = CreateReviewRequest(
                attractionId = submission.attractionId,
                userId = authState.user.id, // Required for RLS: auth.uid() = user_id
                rating = submission.rating,
                title = null, // We don't use title in current UI
                body = submission.text?.takeIf { it.isNotBlank() } ?: "" // body is NOT NULL in DB
            )
            
            val response = supabaseApi.createReview(
                authorization = "Bearer ${authState.accessToken}",
                request = request
            )
            
            if (response.isSuccessful && response.body()?.isNotEmpty() == true) {
                val createdReview = response.body()!!.first()
                
                val review = Review(
                    id = createdReview.id,
                    attractionId = createdReview.attractionId,
                    authorId = createdReview.userId,
                    authorName = authState.user.displayName ?: "Пользователь",
                    authorAvatar = authState.user.avatarUrl,
                    authorBadge = null,
                    rating = createdReview.rating,
                    text = listOfNotNull(createdReview.title, createdReview.body)
                        .joinToString("\n")
                        .ifBlank { null },
                    createdAt = parseInstant(createdReview.createdAt) ?: Instant.now(),
                    updatedAt = parseInstant(createdReview.updatedAt),
                    likesCount = 0,
                    dislikesCount = 0,
                    isOwn = true,
                    status = createdReview.status
                )
                
                // Add to user's own reviews cache
                _userOwnReviews.value = listOf(review) + _userOwnReviews.value
                
                Timber.d("Review submitted successfully: ${review.id}")
                Result.success(review)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = parseReviewError(errorBody, response.code())
                Result.failure(ReviewException(errorMessage))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to submit review")
            val message = when {
                e.message?.contains("unique", ignoreCase = true) == true ||
                e.message?.contains("duplicate", ignoreCase = true) == true ->
                    "Вы уже оставили отзыв для этого места"
                e.message?.contains("timeout", ignoreCase = true) == true ->
                    "Превышено время ожидания. Проверьте подключение."
                else -> "Не удалось отправить отзыв. Попробуйте позже."
            }
            Result.failure(ReviewException(message))
        } finally {
            _isSubmitting.value = false
        }
    }
    
    /**
     * Load user's own reviews (all statuses).
     */
    suspend fun loadUserOwnReviews(): Result<List<Review>> {
        val authState = authRepository.authState.value
        
        if (authState !is AuthState.Authenticated) {
            Timber.w("loadUserOwnReviews: User not authenticated")
            _userOwnReviews.value = emptyList()
            return Result.success(emptyList())
        }
        
        Timber.d("loadUserOwnReviews: userId=${authState.user.id}")
        
        return try {
            val response = supabaseApi.getUserOwnReviews(
                authorization = "Bearer ${authState.accessToken}",
                userId = "eq.${authState.user.id}"
            )
            
            Timber.d("loadUserOwnReviews: response code=${response.code()}, isSuccessful=${response.isSuccessful}")
            
            if (response.isSuccessful) {
                val reviews = response.body()?.map { dto ->
                    Review(
                        id = dto.id,
                        attractionId = dto.attractionId,
                        authorId = authState.user.id,
                        authorName = authState.user.displayName ?: "Пользователь",
                        authorAvatar = authState.user.avatarUrl,
                        authorBadge = null,
                        rating = dto.rating,
                        text = listOfNotNull(dto.title, dto.body)
                            .joinToString("\n")
                            .ifBlank { null },
                        createdAt = parseInstant(dto.createdAt) ?: Instant.now(),
                        updatedAt = parseInstant(dto.updatedAt),
                        likesCount = 0, // User can't see own review reactions
                        dislikesCount = 0,
                        isOwn = true,
                        status = dto.status,
                        rejectionReason = dto.rejectionReason
                    )
                } ?: emptyList()
                
                Timber.d("loadUserOwnReviews: loaded ${reviews.size} reviews")
                _userOwnReviews.value = reviews
                Result.success(reviews)
            } else {
                val errorBody = response.errorBody()?.string()
                Timber.e("loadUserOwnReviews: API error ${response.code()}: $errorBody")
                Result.failure(ReviewException("Не удалось загрузить ваши отзывы"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load user's own reviews")
            Result.failure(ReviewException("Не удалось загрузить ваши отзывы"))
        }
    }
    
    /**
     * Delete user's own review.
     */
    suspend fun deleteOwnReview(reviewId: String): Result<Unit> {
        val authState = authRepository.authState.value
        
        if (authState !is AuthState.Authenticated) {
            return Result.failure(ReviewException("Необходимо войти в аккаунт"))
        }
        
        return try {
            val response = supabaseApi.deleteUserReview(
                authorization = "Bearer ${authState.accessToken}",
                reviewId = "eq.$reviewId",
                userId = "eq.${authState.user.id}"
            )
            
            if (response.isSuccessful) {
                // Remove from local cache
                _userOwnReviews.value = _userOwnReviews.value.filter { it.id != reviewId }
                Result.success(Unit)
            } else {
                Result.failure(ReviewException("Не удалось удалить отзыв"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete review")
            Result.failure(ReviewException("Не удалось удалить отзыв"))
        }
    }

    /**
     * React to a review (like/dislike).
     * Toggle logic: same reaction → remove, different reaction → switch, no reaction → add
     */
    suspend fun reactToReview(reviewId: String, isLike: Boolean): NetworkResult<Unit> {
        val authState = authRepository.authState.value
        
        if (authState !is AuthState.Authenticated) {
            return NetworkResult.Error("Требуется авторизация", 401)
        }
        
        return try {
            val desired = if (isLike) "like" else "dislike"
            Timber.d("reactToReview: reviewId=$reviewId, desired=$desired")

            val currentResp = supabaseApi.getUserReaction(
                authorization = "Bearer ${authState.accessToken}",
                reviewId = "eq.$reviewId",
                userId = "eq.${authState.user.id}"
            )

            if (!currentResp.isSuccessful) {
                return NetworkResult.Error("Не удалось получить текущую реакцию", currentResp.code())
            }

            val existing = currentResp.body()?.firstOrNull()?.reaction?.takeIf { it.isNotBlank() }

            if (existing == desired) {
                val deleteResp = supabaseApi.deleteReviewReaction(
                    authorization = "Bearer ${authState.accessToken}",
                    reviewId = "eq.$reviewId",
                    userId = "eq.${authState.user.id}"
                )
                if (!deleteResp.isSuccessful) {
                    return NetworkResult.Error("Не удалось убрать реакцию", deleteResp.code())
                }
            } else {
                val request = com.adygyes.app.data.remote.dto.ReviewReactionRequest(
                    reviewId = reviewId,
                    userId = authState.user.id,
                    reaction = desired
                )
                val upsertResp = supabaseApi.upsertReviewReaction(
                    authorization = "Bearer ${authState.accessToken}",
                    // keep default Prefer (merge-duplicates + return=representation)
                    request = request
                )
                if (!upsertResp.isSuccessful) {
                    return NetworkResult.Error("Не удалось сохранить реакцию", upsertResp.code())
                }
            }

            NetworkResult.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to react to review")
            NetworkResult.Error(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Refresh user's own reviews for a specific attraction (all statuses).
     */
    suspend fun refreshUserOwnReviews(attractionId: String) {
        val authState = authRepository.authState.value
        
        if (authState !is AuthState.Authenticated) {
            Timber.w("refreshUserOwnReviews: User not authenticated")
            _userOwnReviews.value = emptyList()
            return
        }
        
        Timber.d("refreshUserOwnReviews: attractionId=$attractionId, userId=${authState.user.id}")
        
        try {
            val response = supabaseApi.getUserOwnReviews(
                authorization = "Bearer ${authState.accessToken}",
                userId = "eq.${authState.user.id}",
                attractionId = "eq.$attractionId"
            )
            
            Timber.d("refreshUserOwnReviews: response code=${response.code()}, isSuccessful=${response.isSuccessful}")
            
            if (response.isSuccessful) {
                val reviews = response.body()?.map { dto ->
                    Review(
                        id = dto.id,
                        attractionId = dto.attractionId,
                        authorId = authState.user.id,
                        authorName = authState.user.displayName ?: "Пользователь",
                        authorAvatar = authState.user.avatarUrl,
                        authorBadge = null,
                        rating = dto.rating,
                        text = listOfNotNull(dto.title, dto.body)
                            .joinToString("\n")
                            .ifBlank { null },
                        createdAt = parseInstant(dto.createdAt) ?: Instant.now(),
                        updatedAt = parseInstant(dto.updatedAt),
                        likesCount = 0,
                        dislikesCount = 0,
                        isOwn = true,
                        status = dto.status,
                        rejectionReason = dto.rejectionReason
                    )
                } ?: emptyList()
                
                Timber.d("refreshUserOwnReviews: loaded ${reviews.size} reviews, statuses=${reviews.map { it.status }}")
                _userOwnReviews.value = reviews
            } else {
                val errorBody = response.errorBody()?.string()
                Timber.e("refreshUserOwnReviews: API error ${response.code()}: $errorBody")
                // НЕ очищаем _userOwnReviews, сохраняем предыдущее состояние
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to refresh user's own reviews for attraction $attractionId")
            // НЕ очищаем _userOwnReviews при ошибке сети
        }
    }

    private suspend fun enrichWithUserReactions(reviews: List<Review>): List<Review> {
        val authState = authRepository.authState.value
        if (authState !is AuthState.Authenticated) return reviews
        if (reviews.isEmpty()) return reviews

        return try {
            val ids = reviews.map { it.id }.distinct()
            val filter = "in.(${ids.joinToString(",")})"
            val resp = supabaseApi.getUserReactionsForReviews(
                authorization = "Bearer ${authState.accessToken}",
                userId = "eq.${authState.user.id}",
                reviewIdFilter = filter
            )

            if (!resp.isSuccessful) return reviews

            val map = (resp.body() ?: emptyList())
                .mapNotNull { dto ->
                    val reviewId = dto.reviewId
                    val reaction = dto.reaction
                    if (reviewId.isNullOrBlank() || reaction.isBlank()) null else reviewId to reaction
                }
                .toMap()

            reviews.map { review ->
                when (map[review.id]) {
                    "like" -> review.copy(userReaction = ReviewReaction.LIKE)
                    "dislike" -> review.copy(userReaction = ReviewReaction.DISLIKE)
                    else -> review
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to enrich reviews with user reactions")
            reviews
        }
    }
    
    fun getCurrentUserId(): String? {
        val authState = authRepository.authState.value
        return (authState as? AuthState.Authenticated)?.user?.id
    }
    
    private fun parseReviewError(errorBody: String?, code: Int): String {
        return when {
            code == 409 || errorBody?.contains("duplicate", ignoreCase = true) == true ||
            errorBody?.contains("unique", ignoreCase = true) == true ->
                "Вы уже оставили отзыв для этого места"
            code == 401 || code == 403 ->
                "Ошибка авторизации. Попробуйте войти снова."
            code == 400 ->
                "Некорректные данные отзыва"
            else -> "Не удалось отправить отзыв. Попробуйте позже."
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
                    compareByDescending<Review> { it.likesCount - it.dislikesCount }
                        .thenByDescending { it.createdAt }
                )
            }
        }
    }
}

/**
 * Exception for review-related errors
 */
class ReviewException(message: String) : Exception(message)
