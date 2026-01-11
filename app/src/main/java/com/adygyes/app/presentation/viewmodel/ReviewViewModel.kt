package com.adygyes.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adygyes.app.data.repository.AuthRepository
import com.adygyes.app.data.repository.ReviewRepository
import com.adygyes.app.domain.model.AuthState
import com.adygyes.app.domain.model.Review
import com.adygyes.app.domain.model.ReviewReaction
import com.adygyes.app.domain.model.ReviewSortOption
import com.adygyes.app.domain.model.ReviewSubmission
import com.adygyes.app.domain.model.isAuthenticated
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for managing reviews
 * Unified with RN useReviewStore behavior
 */
@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val reviewRepository: ReviewRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // IN-MEMORY CACHE (like Zustand in RN) - persists between screen visits
    // ═══════════════════════════════════════════════════════════════════════════════
    // This is the KEY difference from before: we keep reviews in memory by attractionId
    // so when user switches between cards, reviews appear INSTANTLY without any loading
    private val _reviewsCache = mutableMapOf<String, List<Review>>()
    private val _userOwnReviewsCache = mutableMapOf<String, List<Review>>()
    
    // Current attraction's reviews (for UI consumption)
    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews: StateFlow<List<Review>> = _reviews.asStateFlow()
    
    // User's own reviews (all statuses: pending, approved, rejected)
    private val _userOwnReviews = MutableStateFlow<List<Review>>(emptyList())
    val userOwnReviews: StateFlow<List<Review>> = _userOwnReviews.asStateFlow()
    
    // Loading state: true ONLY when no cached data and fetching from network
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    
    // Background sync indicator: shows subtle indicator that sync is happening
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _sortBy = MutableStateFlow(ReviewSortOption.DEFAULT)
    val sortBy: StateFlow<ReviewSortOption> = _sortBy.asStateFlow()
    
    private val _submitting = MutableStateFlow(false)
    val submitting: StateFlow<Boolean> = _submitting.asStateFlow()
    
    private val _submitSuccess = MutableStateFlow(false)
    val submitSuccess: StateFlow<Boolean> = _submitSuccess.asStateFlow()
    
    // Auth state for checking if user can submit reviews
    val isAuthenticated: StateFlow<Boolean> = authRepository.authState
        .map { it.isAuthenticated }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    // Check if user needs to login to submit review
    private val _showAuthRequired = MutableStateFlow(false)
    val showAuthRequired: StateFlow<Boolean> = _showAuthRequired.asStateFlow()
    
    // Check if user already reviewed this attraction
    private val _hasUserReviewed = MutableStateFlow(false)
    val hasUserReviewed: StateFlow<Boolean> = _hasUserReviewed.asStateFlow()
    
    private var currentAttractionId: String? = null
    
    // Job for background sync to allow cancellation
    private var syncJob: kotlinx.coroutines.Job? = null
    
    /**
     * Load reviews for an attraction.
     * 
     * OFFLINE-FIRST STRATEGY (like RN Zustand store):
     * 1. INSTANT FROM MEMORY: If we have in-memory cache, show it IMMEDIATELY (no async, no suspend)
     * 2. INSTANT FROM ROOM: Otherwise load from Room cache (fast local DB)
     * 3. BACKGROUND SYNC: Fetch updates from server in background (doesn't block UI)
     * 4. SEAMLESS UPDATE: If new data arrives, update UI smoothly without flickering
     * 
     * Loading spinner is shown ONLY when:
     * - NO in-memory cache AND NO Room cache AND we're fetching from network
     */
    fun loadReviews(attractionId: String, forceRefresh: Boolean = false) {
        // Cancel previous sync job if switching attractions
        if (currentAttractionId != attractionId) {
            syncJob?.cancel()
        }
        
        // ═══════════════════════════════════════════════════════════════════════════════
        // PHASE 0: INSTANT MEMORY CACHE (like Zustand - no async, no waiting!)
        // This is what makes RN feel instant - data persists in memory between views
        // ═══════════════════════════════════════════════════════════════════════════════
        val memoryReviews = _reviewsCache[attractionId]
        val memoryOwnReviews = _userOwnReviewsCache[attractionId]
        
        if (!forceRefresh && memoryReviews != null) {
            // INSTANT: Show from memory cache (same as Zustand behavior)
            _reviews.value = memoryReviews
            _userOwnReviews.value = memoryOwnReviews ?: emptyList()
            _loading.value = false
            _hasUserReviewed.value = memoryOwnReviews?.isNotEmpty() == true
            currentAttractionId = attractionId
            Timber.d("⚡ INSTANT MEMORY: ${memoryReviews.size} reviews, ${memoryOwnReviews?.size ?: 0} own")
            
            // Still do background sync to catch updates
            launchBackgroundSync(attractionId, forceRefresh = false)
            return
        }
        
        currentAttractionId = attractionId
        _error.value = null
        
        viewModelScope.launch {
            try {
                // ═══════════════════════════════════════════════════════════
                // PHASE 1: INSTANT ROOM CACHE (no network, no waiting)
                // ═══════════════════════════════════════════════════════════
                
                var userReviewIds: Set<String> = emptySet()
                var hasAnyCachedContent = false
                
                // Load user's own reviews from Room cache FIRST
                if (isAuthenticated.value) {
                    val cachedOwnReviews = reviewRepository.getUserOwnReviewsFromCache(attractionId)
                    if (cachedOwnReviews.isNotEmpty()) {
                        _userOwnReviews.value = cachedOwnReviews
                        _userOwnReviewsCache[attractionId] = cachedOwnReviews // Save to memory
                        userReviewIds = cachedOwnReviews.map { it.id }.toSet()
                        hasAnyCachedContent = true
                        Timber.d("📦 ROOM CACHE: ${cachedOwnReviews.size} own reviews")
                    }
                }
                
                // Load public reviews from Room cache
                val cachedReviews = reviewRepository.getReviewsFromCacheOnly(attractionId)
                val filteredCachedReviews = cachedReviews.filter { it.id !in userReviewIds }
                
                if (filteredCachedReviews.isNotEmpty()) {
                    val sortedReviews = sortReviewsLocally(filteredCachedReviews, _sortBy.value)
                    _reviews.value = sortedReviews
                    _reviewsCache[attractionId] = sortedReviews // Save to memory
                    hasAnyCachedContent = true
                    Timber.d("📦 ROOM CACHE: ${sortedReviews.size} public reviews")
                }

                // Set loading state based on cache availability
                _loading.value = !hasAnyCachedContent
                if (!hasAnyCachedContent) {
                    Timber.d("⏳ No cache, showing loading spinner")
                }
                
                // Check hasUserReviewed from cache
                _hasUserReviewed.value = _userOwnReviews.value.isNotEmpty() || 
                    reviewRepository.hasUserReviewedFromCache(attractionId)
                
                // ═══════════════════════════════════════════════════════════
                // PHASE 2: BACKGROUND SYNC (non-blocking)
                // ═══════════════════════════════════════════════════════════
                launchBackgroundSync(attractionId, forceRefresh)
                
            } catch (e: kotlinx.coroutines.CancellationException) {
                Timber.d("Review loading cancelled for $attractionId")
                _loading.value = false
                _isSyncing.value = false
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to load reviews for $attractionId")
                _error.value = e.message
                _loading.value = false
                _isSyncing.value = false
            }
        }
    }
    
    /**
     * Launch background sync without blocking UI.
     * Updates in-memory cache and UI when new data arrives.
     */
    private fun launchBackgroundSync(attractionId: String, forceRefresh: Boolean) {
        syncJob = viewModelScope.launch {
            _isSyncing.value = true
            
            try {
                val currentUserId = if (isAuthenticated.value) {
                    reviewRepository.getCurrentUserId()
                } else null
                
                // Sync user's own reviews in background
                if (isAuthenticated.value) {
                    reviewRepository.refreshUserOwnReviews(attractionId)
                    val updatedOwnReviews = reviewRepository.userOwnReviews.value
                        .filter { it.attractionId == attractionId }
                    if (updatedOwnReviews != _userOwnReviews.value) {
                        _userOwnReviews.value = updatedOwnReviews
                        _userOwnReviewsCache[attractionId] = updatedOwnReviews
                        Timber.d("✅ SYNC: Updated ${updatedOwnReviews.size} own reviews")
                    }
                }
                
                // Build current own-review IDs for filtering public reviews
                val userReviewIds = _userOwnReviews.value.map { it.id }.toSet()
                
                // Background delta sync for public reviews
                val didSync = reviewRepository.performBackgroundSync(attractionId, excludeUserId = currentUserId)
                
                if (didSync || forceRefresh) {
                    // Reload from Room after sync
                    val updatedReviews = reviewRepository.getReviewsFromCacheOnly(attractionId)
                    val filteredUpdatedReviews = updatedReviews.filter { it.id !in userReviewIds }
                    val sortedReviews = sortReviewsLocally(filteredUpdatedReviews, _sortBy.value)
                    
                    if (sortedReviews != _reviews.value) {
                        _reviews.value = sortedReviews
                        _reviewsCache[attractionId] = sortedReviews
                        Timber.d("✅ SYNC: UI updated with ${sortedReviews.size} reviews")
                    }
                }
                
                // Update hasUserReviewed
                _hasUserReviewed.value = _userOwnReviews.value.isNotEmpty() ||
                    reviewRepository.hasUserReviewedFromCache(attractionId)
                
            } catch (e: kotlinx.coroutines.CancellationException) {
                Timber.d("🚫 Background sync cancelled")
                throw e
            } catch (e: Exception) {
                // Network error during background sync - don't show error if we have cached data
                if (_reviews.value.isEmpty() && _reviewsCache[attractionId].isNullOrEmpty()) {
                    _error.value = "Не удалось загрузить отзывы"
                }
                Timber.w(e, "⚠️ Background sync failed, keeping cached data")
            } finally {
                _isSyncing.value = false
                _loading.value = false
            }
        }
    }
    
    /**
     * Sort reviews locally without network call
     */
    private fun sortReviewsLocally(reviews: List<Review>, sortBy: ReviewSortOption): List<Review> {
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
    
    /**
     * Check if user already reviewed from cache (fast, no network)
     */
    private suspend fun checkUserReviewedFromCache(attractionId: String) {
        _hasUserReviewed.value = reviewRepository.hasUserReviewedFromCache(attractionId)
    }
    
    /**
     * Check if user already reviewed this attraction (with network fallback)
     */
    private suspend fun checkUserReviewed(attractionId: String) {
        _hasUserReviewed.value = reviewRepository.hasUserReviewed(attractionId)
    }
    
    /**
     * Check if user can write a review (authenticated and hasn't reviewed yet)
     * Uses synchronous check from AuthRepository for immediate validation
     */
    fun canWriteReview(): Boolean {
        // Use synchronous check instead of StateFlow to avoid timing issues
        if (!authRepository.isCurrentlyAuthenticated()) {
            _showAuthRequired.value = true
            return false
        }
        if (_hasUserReviewed.value) {
            _error.value = "Вы уже оставили отзыв для этого места"
            return false
        }
        return true
    }
    
    /**
     * Dismiss auth required prompt
     */
    fun dismissAuthRequired() {
        _showAuthRequired.value = false
    }
    
    /**
     * Change sort order - applies locally without network call
     */
    fun setSortBy(newSortBy: ReviewSortOption) {
        _sortBy.value = newSortBy
        // Sort existing data locally (instant, no network) - create NEW list for StateFlow
        if (_reviews.value.isNotEmpty()) {
            _reviews.value = sortReviewsLocally(_reviews.value, newSortBy).toList()
        }
    }
    
    /**
     * React to a review (like/dislike with toggle logic).
     * REQUIRES AUTHENTICATION - shows auth modal if not logged in.
     */
    fun reactToReview(reviewId: String, isLike: Boolean) {
        android.util.Log.d("ReviewViewModel", "🎯 reactToReview called: $reviewId, isLike=$isLike")
        
        // Check authentication first
        if (!authRepository.isCurrentlyAuthenticated()) {
            android.util.Log.w("ReviewViewModel", "⚠️ User not authenticated - showing auth modal")
            _showAuthRequired.value = true
            return
        }
        
        // Find review
        val currentReview = _reviews.value.find { it.id == reviewId }
        if (currentReview == null) {
            android.util.Log.e("ReviewViewModel", "❌ Review not found: $reviewId")
            return
        }
        
        val currentReaction = currentReview.userReaction
        val desiredReaction = if (isLike) ReviewReaction.LIKE else ReviewReaction.DISLIKE
        
        // Calculate new state (toggle logic)
        val newReaction: ReviewReaction
        val newLikes: Int
        val newDislikes: Int
        
        when {
            currentReaction == desiredReaction -> {
                // Same button → remove reaction
                newReaction = ReviewReaction.NONE
                newLikes = if (isLike) (currentReview.likesCount - 1).coerceAtLeast(0) else currentReview.likesCount
                newDislikes = if (!isLike) (currentReview.dislikesCount - 1).coerceAtLeast(0) else currentReview.dislikesCount
            }
            currentReaction == ReviewReaction.NONE -> {
                // No reaction → add new
                newReaction = desiredReaction
                newLikes = if (isLike) currentReview.likesCount + 1 else currentReview.likesCount
                newDislikes = if (!isLike) currentReview.dislikesCount + 1 else currentReview.dislikesCount
            }
            else -> {
                // Switch reaction
                newReaction = desiredReaction
                newLikes = if (isLike) currentReview.likesCount + 1 else (currentReview.likesCount - 1).coerceAtLeast(0)
                newDislikes = if (!isLike) currentReview.dislikesCount + 1 else (currentReview.dislikesCount - 1).coerceAtLeast(0)
            }
        }
        
        // Update UI immediately
        val updatedReview = currentReview.copy(
            userReaction = newReaction,
            likesCount = newLikes,
            dislikesCount = newDislikes
        )
        
        val updatedReviews = _reviews.value.map { if (it.id == reviewId) updatedReview else it }.toList()
        _reviews.value = updatedReviews
        
        // Also update in-memory cache
        currentAttractionId?.let { attractionId ->
            _reviewsCache[attractionId] = updatedReviews
        }
        
        android.util.Log.d("ReviewViewModel", "⚡ UI UPDATED: $newReaction (likes=$newLikes, dislikes=$newDislikes)")
        
        // Background sync
        viewModelScope.launch {
            try {
                reviewRepository.reactToReviewOptimistic(
                    reviewId = reviewId,
                    newReaction = newReaction,
                    newLikesCount = newLikes,
                    newDislikesCount = newDislikes,
                    isLike = isLike,
                    wasToggleOff = currentReaction == desiredReaction
                )
                android.util.Log.d("ReviewViewModel", "✅ Server synced")
            } catch (e: Exception) {
                android.util.Log.e("ReviewViewModel", "⚠️ Sync failed: ${e.message}")
                // Keep UI state - no rollback
            }
        }
    }
    
    /**
     * Submit a new review
     */
    fun submitReview(attractionId: String, rating: Int, text: String?) {
        viewModelScope.launch {
            _submitting.value = true
            _submitSuccess.value = false
            _error.value = null
            
            try {
                val submission = ReviewSubmission(
                    attractionId = attractionId,
                    rating = rating,
                    text = text
                )
                
                reviewRepository.submitReview(submission)
                    .onSuccess { newReview ->
                        Timber.d("Successfully submitted review for attraction $attractionId, status=${newReview.status}")
                        
                        // КРИТИЧНО: обновить локальный стейт сразу с новым отзывом
                        // Репозиторий уже добавил отзыв в свой кэш, копируем его
                        val updatedOwnReviews = reviewRepository.userOwnReviews.value
                            .filter { it.attractionId == attractionId }
                        _userOwnReviews.value = updatedOwnReviews
                        _userOwnReviewsCache[attractionId] = updatedOwnReviews // Update memory cache
                        
                        // Обновить hasUserReviewed флаг
                        _hasUserReviewed.value = true
                        
                        // Обновить публичные отзывы (может понадобиться для рейтинга)
                        val currentUserId = reviewRepository.getCurrentUserId()
                        reviewRepository.refreshApprovedReviews(attractionId, excludeUserId = currentUserId)
                        
                        _submitSuccess.value = true
                        
                        Timber.d("User own reviews count after submit: ${_userOwnReviews.value.size}")
                    }
                    .onFailure { e ->
                        Timber.e(e, "Failed to submit review for attraction $attractionId")
                        _error.value = e.message ?: "Ошибка при отправке отзыва"
                    }
            } catch (e: Exception) {
                Timber.e(e, "Error submitting review for attraction $attractionId")
                _error.value = e.message ?: "Ошибка при отправке отзыва"
            } finally {
                _submitting.value = false
            }
        }
    }
    
    /**
     * Reset submit success state
     */
    fun resetSubmitSuccess() {
        _submitSuccess.value = false
    }
    
    /**
     * Clear error state
     */
    fun clearError() {
        _error.value = null
    }
    
    /**
     * Clear current attraction's reviews state.
     * NOTE: In-memory cache (_reviewsCache) is NOT cleared - this is intentional!
     * Like RN's Zustand store, we keep reviews in memory by attractionId
     * so when user returns to a card, reviews appear INSTANTLY.
     */
    fun clearReviews() {
        syncJob?.cancel()
        // Don't clear _reviewsCache and _userOwnReviewsCache - they persist between screens
        _reviews.value = emptyList()
        _userOwnReviews.value = emptyList()
        _hasUserReviewed.value = false
        _loading.value = false
        _isSyncing.value = false
        currentAttractionId = null
    }
    
    /**
     * Clear all cached reviews (e.g., on logout or full app reset).
     */
    fun clearAllCaches() {
        clearReviews()
        _reviewsCache.clear()
        _userOwnReviewsCache.clear()
    }
}