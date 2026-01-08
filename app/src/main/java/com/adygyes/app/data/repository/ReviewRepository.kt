package com.adygyes.app.data.repository

import com.adygyes.app.data.local.dao.ReviewDao
import com.adygyes.app.data.local.entities.ReviewEntity
import com.adygyes.app.data.remote.NetworkResult
import com.adygyes.app.data.remote.ReviewsRemoteDataSource
import com.adygyes.app.data.remote.api.SupabaseApiService
import com.adygyes.app.data.remote.dto.CreateReviewRequest
import com.adygyes.app.data.remote.dto.ReviewDto
import com.adygyes.app.domain.model.AuthState
import com.adygyes.app.domain.model.Review
import com.adygyes.app.domain.model.ReviewReaction
import com.adygyes.app.domain.model.ReviewSortOption
import com.adygyes.app.data.sync.ReviewSyncService
import com.adygyes.app.domain.model.ReviewSubmission
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing reviews.
 *
 * Features:
 * - Offline-first: reads from Room cache, syncs with Supabase
 * - Reads: public approved reviews via Supabase RLS
 * - Writes: authenticated users can submit reviews (status='pending')
 * - User's own reviews: visible regardless of status when authenticated
 */
@Singleton
class ReviewRepository @Inject constructor(
    private val reviewsRemoteDataSource: ReviewsRemoteDataSource,
    private val reviewDao: ReviewDao,
    private val supabaseApi: SupabaseApiService,
    private val authRepository: AuthRepository,
    private val reviewSyncService: ReviewSyncService
) {

    private val _reviews = MutableStateFlow<Map<String, List<Review>>>(emptyMap())
    
    // User's own reviews (including pending/rejected)
    private val _userOwnReviews = MutableStateFlow<List<Review>>(emptyList())
    val userOwnReviews: StateFlow<List<Review>> = _userOwnReviews.asStateFlow()
    
    // Submission state
    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

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
     * Get reviews from local cache INSTANTLY (no network wait).
     * This is the primary method for displaying reviews when opening a card.
     * Reviews are pre-synced during main attractions sync.
     */
    suspend fun getReviewsOfflineFirst(attractionId: String): List<Review> {
        // Load from Room cache immediately
        val cached = reviewDao.getApprovedReviews(attractionId)
        if (cached.isNotEmpty()) {
            Timber.d("📦 INSTANT: Loaded ${cached.size} reviews from cache for $attractionId")
            val currentUserId = getCurrentUserId()
            val reviews = cached.map { it.toDomain() }
            
            // Enrich with user reactions if authenticated
            val enrichedReviews = enrichWithCachedReactions(reviews)
            
            _reviews.value = _reviews.value.toMutableMap().apply {
                put(attractionId, enrichedReviews)
            }
            return enrichedReviews
        }
        
        Timber.d("📦 No cached reviews for $attractionId")
        return emptyList()
    }
    
    /**
     * Background delta sync for an attraction.
     * Called after showing cached data to check for updates.
     * Does NOT block UI - reviews are already displayed from cache.
     */
    suspend fun backgroundSyncReviews(attractionId: String, excludeUserId: String? = null) {
        // Delta sync in background (returns quickly if cache is fresh)
        val didSync = reviewSyncService.syncReviewsForAttraction(attractionId)
        
        if (didSync) {
            // Reload from cache after sync
            val updated = reviewDao.getApprovedReviews(attractionId)
            if (updated.isNotEmpty()) {
                val currentUserId = getCurrentUserId()
                val reviews = updated
                    .filter { excludeUserId == null || it.userId != excludeUserId }
                    .map { it.toDomain() }
                
                // Enrich with user reactions
                val enrichedReviews = enrichWithUserReactions(reviews)
                
                _reviews.value = _reviews.value.toMutableMap().apply {
                    put(attractionId, enrichedReviews)
                }
                Timber.d("✅ Updated ${enrichedReviews.size} reviews after background sync")
            }
        }
    }

    /**
     * Refresh approved reviews from Supabase and cache to Room.
     * Offline-first: shows cached data immediately, then updates from network.
     * @param excludeUserId - ID пользователя, чьи отзывы нужно исключить из списка (чтобы избежать дублирования)
     */
    suspend fun refreshApprovedReviews(attractionId: String, excludeUserId: String? = null) {
        // First, load from cache for immediate display
        val cached = reviewDao.getApprovedReviews(attractionId)
        if (cached.isNotEmpty() && (_reviews.value[attractionId]?.isEmpty() != false)) {
            val cachedReviews = cached.map { it.toDomain() }
            _reviews.value = _reviews.value.toMutableMap().apply {
                put(attractionId, cachedReviews)
            }
            Timber.d("📦 Showing ${cached.size} cached reviews")
        }
        
        // Background delta sync
        backgroundSyncReviews(attractionId, excludeUserId)
    }
    
    /**
     * Enrich reviews with cached user reactions (from Room).
     * Fast - no network call.
     */
    private fun enrichWithCachedReactions(reviews: List<Review>): List<Review> {
        // User reactions are stored in ReviewEntity.userReaction
        // They're already loaded from cache, so just pass through
        return reviews
    }

    suspend fun fetchReviews(attractionId: String): Result<List<Review>> {
        return try {
            // First show cached data instantly
            getReviewsOfflineFirst(attractionId)
            // Then sync in background
            backgroundSyncReviews(attractionId)
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

        // Fast-path: if we already have an own-review cached locally, trust it.
        if (reviewDao.hasUserReviewed(attractionId, authState.user.id)) return true

        return try {
            val response = supabaseApi.checkUserReviewExists(
                authorization = "Bearer ${authState.accessToken}",
                attractionId = "eq.$attractionId",
                userId = "eq.${authState.user.id}"
            )

            response.isSuccessful && (response.body()?.isNotEmpty() == true)
        } catch (e: Exception) {
            Timber.w(e, "Error checking if user reviewed attraction")
            // If network fails, fall back to what we know locally.
            reviewDao.hasUserReviewed(attractionId, authState.user.id)
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

                // Persist to Room for offline access
                reviewDao.insertReview(review.toEntity())
                
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

        // Show cached own reviews immediately (offline-first)
        val cached = reviewDao.getAllUserReviews(authState.user.id)
        if (cached.isNotEmpty()) {
            _userOwnReviews.value = cached.map { it.toDomain() }
            Timber.d("📦 Loaded ${cached.size} own reviews from cache")
        }

        return when (
            val result = reviewsRemoteDataSource.getUserReviews(
                authorization = "Bearer ${authState.accessToken}",
                userId = authState.user.id,
                attractionId = null
            )
        ) {
            is NetworkResult.Success -> {
                val reviews = result.data.map { dto ->
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
                }

                _userOwnReviews.value = reviews
                reviewDao.replaceAllUserOwnReviews(authState.user.id, reviews.map { it.toEntity() })
                Result.success(reviews)
            }

            is NetworkResult.Error -> {
                Timber.w("⚠️ loadUserOwnReviews failed: ${result.code}: ${result.message}")
                // If we have cached data, keep it and report success to avoid breaking UI.
                if (cached.isNotEmpty()) Result.success(_userOwnReviews.value)
                else Result.failure(ReviewException("Не удалось загрузить ваши отзывы"))
            }
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
                reviewDao.deleteReview(reviewId)
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

        // Show cached data first if available
        val cached = reviewDao.getUserReviews(attractionId, authState.user.id)
        if (cached.isNotEmpty() && _userOwnReviews.value.isEmpty()) {
            _userOwnReviews.value = cached.map { it.toDomain() }
            Timber.d("📦 Showing cached own reviews while fetching from network")
        }

        when (
            val result = reviewsRemoteDataSource.getUserReviews(
                authorization = "Bearer ${authState.accessToken}",
                userId = authState.user.id,
                attractionId = attractionId
            )
        ) {
            is NetworkResult.Success -> {
                val reviews = result.data.map { dto ->
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
                }

                _userOwnReviews.value = reviews
                reviewDao.replaceUserOwnReviewsForAttraction(
                    attractionId = attractionId,
                    userId = authState.user.id,
                    reviews = reviews.map { it.toEntity() }
                )
            }

            is NetworkResult.Error -> {
                Timber.e("refreshUserOwnReviews: API error ${result.code}: ${result.message}")
                // keep cached / previous state
            }
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
    
    // ========== Mappers ==========
    
    private fun ReviewDto.toDomain(currentUserId: String? = null): Review {
        return Review(
            id = id,
            attractionId = attractionId,
            authorId = userId ?: "",
            authorName = profile?.displayName ?: "Пользователь",
            authorAvatar = profile?.avatarUrl,
            authorBadge = null,
            rating = rating,
            text = listOfNotNull(title, body)
                .joinToString("\n")
                .ifBlank { null },
            createdAt = parseInstant(createdAt) ?: Instant.now(),
            updatedAt = parseInstant(updatedAt),
            likesCount = likesCount ?: 0,
            dislikesCount = dislikesCount ?: 0,
            isOwn = userId == currentUserId,
            userReaction = ReviewReaction.NONE,
            status = status
        )
    }
    
    private fun ReviewEntity.toDomain(): Review {
        return Review(
            id = id,
            attractionId = attractionId,
            authorId = userId ?: "",
            authorName = authorName ?: "Пользователь",
            authorAvatar = authorAvatar,
            authorBadge = null,
            rating = rating,
            text = listOfNotNull(title, body)
                .joinToString("\n")
                .ifBlank { null },
            createdAt = parseInstant(createdAt) ?: Instant.now(),
            updatedAt = parseInstant(updatedAt),
            likesCount = likesCount,
            dislikesCount = dislikesCount,
            isOwn = isOwnReview,
            userReaction = when (userReaction) {
                "like" -> ReviewReaction.LIKE
                "dislike" -> ReviewReaction.DISLIKE
                else -> ReviewReaction.NONE
            },
            status = status,
            rejectionReason = rejectionReason
        )
    }
    
    private fun Review.toEntity(): ReviewEntity {
        return ReviewEntity(
            id = id,
            attractionId = attractionId,
            userId = authorId.takeIf { it.isNotBlank() },
            rating = rating,
            title = null, // We store full text in body
            body = text,
            status = status ?: "approved",
            rejectionReason = rejectionReason,
            authorName = authorName,
            authorAvatar = authorAvatar,
            likesCount = likesCount,
            dislikesCount = dislikesCount,
            userReaction = when (userReaction) {
                ReviewReaction.LIKE -> "like"
                ReviewReaction.DISLIKE -> "dislike"
                ReviewReaction.NONE -> null
            },
            createdAt = createdAt.toString(),
            updatedAt = updatedAt?.toString(),
            isOwnReview = isOwn,
            lastSyncedAt = System.currentTimeMillis()
        )
    }
}

/**
 * Exception for review-related errors
 */
class ReviewException(message: String) : Exception(message)
