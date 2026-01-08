package com.adygyes.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adygyes.app.data.repository.AuthRepository
import com.adygyes.app.data.repository.ReviewRepository
import com.adygyes.app.domain.model.AuthState
import com.adygyes.app.domain.model.Review
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
    
    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews: StateFlow<List<Review>> = _reviews.asStateFlow()
    
    // User's own reviews (all statuses: pending, approved, rejected)
    private val _userOwnReviews = MutableStateFlow<List<Review>>(emptyList())
    val userOwnReviews: StateFlow<List<Review>> = _userOwnReviews.asStateFlow()
    
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    
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
    
    /**
     * Load reviews for an attraction.
     * 
     * Strategy:
     * 1. INSTANT: Load from Room cache immediately (no loading spinner)
     * 2. BACKGROUND: Delta sync to check for updates
     * 3. UPDATE: If new data found, update UI seamlessly
     */
    fun loadReviews(attractionId: String, forceRefresh: Boolean = false) {
        // Skip reload if same attraction and already have data (unless forceRefresh)
        if (!forceRefresh && currentAttractionId == attractionId && _reviews.value.isNotEmpty()) {
            // Still refresh user's own reviews in background
            if (isAuthenticated.value) {
                viewModelScope.launch {
                    reviewRepository.refreshUserOwnReviews(attractionId)
                    _userOwnReviews.value = reviewRepository.userOwnReviews.value
                    Timber.d("Refreshed user own reviews (cached path), count=${_userOwnReviews.value.size}")
                }
            }
            return
        }
        
        currentAttractionId = attractionId
        _error.value = null
        
        viewModelScope.launch {
            try {
                // Check if user already reviewed this attraction
                checkUserReviewed(attractionId)
                
                // CRITICAL: Load user's own reviews FIRST before public reviews
                // This ensures proper filtering to avoid duplicates
                if (isAuthenticated.value) {
                    reviewRepository.refreshUserOwnReviews(attractionId)
                    _userOwnReviews.value = reviewRepository.userOwnReviews.value
                    Timber.d("Loaded user own reviews, count=${_userOwnReviews.value.size}, statuses=${_userOwnReviews.value.map { it.status }}")
                } else {
                    _userOwnReviews.value = emptyList()
                }
                
                // Get current user ID for filtering
                val currentUserId = if (isAuthenticated.value) {
                    reviewRepository.getCurrentUserId()
                } else null
                
                // INSTANT: Load from cache immediately (no loading indicator)
                val cachedReviews = reviewRepository.getReviewsOfflineFirst(attractionId)
                if (cachedReviews.isNotEmpty()) {
                    val userReviewIds = _userOwnReviews.value.map { it.id }.toSet()
                    _reviews.value = cachedReviews.filter { it.id !in userReviewIds }
                    Timber.d("📦 INSTANT: Displayed ${_reviews.value.size} cached reviews")
                } else {
                    // No cache - show loading only when fetching fresh data
                    _loading.value = true
                }
                
                // BACKGROUND: Delta sync to check for updates
                reviewRepository.backgroundSyncReviews(attractionId, excludeUserId = currentUserId)
                
                // Observe updated reviews from repository
                reviewRepository.getReviewsForAttraction(attractionId, _sortBy.value)
                    .collect { allReviews ->
                        val userReviewIds = _userOwnReviews.value.map { it.id }.toSet()
                        _reviews.value = allReviews.filter { it.id !in userReviewIds }
                        _loading.value = false
                    }
            } catch (e: kotlinx.coroutines.CancellationException) {
                Timber.d("Review loading cancelled for attraction $attractionId")
                _loading.value = false
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to load reviews for attraction $attractionId")
                _error.value = e.message
                _loading.value = false
            }
        }
    }
    
    /**
     * Check if user already reviewed this attraction
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
     * Change sort order
     */
    fun setSortBy(newSortBy: ReviewSortOption) {
        _sortBy.value = newSortBy
        currentAttractionId?.let { loadReviews(it) }
    }
    
    /**
     * React to a review (like/dislike with toggle logic)
     */
    fun reactToReview(reviewId: String, isLike: Boolean) {
        if (!authRepository.isCurrentlyAuthenticated()) {
            _error.value = "Требуется авторизация"
            return
        }
        
        viewModelScope.launch {
            try {
                val result = reviewRepository.reactToReview(reviewId, isLike)
                when (result) {
                    is com.adygyes.app.data.remote.NetworkResult.Success -> {
                        Timber.d("Successfully reacted to review $reviewId")
                        // Refresh both public reviews and user's own reviews to get updated counts
                        // forceRefresh=true ensures proper re-filtering
                        currentAttractionId?.let { attractionId ->
                            loadReviews(attractionId, forceRefresh = true)
                        }
                    }
                    is com.adygyes.app.data.remote.NetworkResult.Error -> {
                        Timber.e("Failed to react to review: ${result.message}")
                        _error.value = result.message
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error reacting to review $reviewId")
                _error.value = e.message
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
                        _userOwnReviews.value = reviewRepository.userOwnReviews.value
                        
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
     * Clear reviews (e.g., when navigating away)
     */
    fun clearReviews() {
        _reviews.value = emptyList()
        _hasUserReviewed.value = false
        currentAttractionId = null
    }
}
