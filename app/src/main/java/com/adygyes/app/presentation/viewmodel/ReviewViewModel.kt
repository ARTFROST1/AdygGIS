package com.adygyes.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adygyes.app.data.repository.ReviewRepository
import com.adygyes.app.domain.model.Review
import com.adygyes.app.domain.model.ReviewSortOption
import com.adygyes.app.domain.model.ReviewSubmission
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for managing reviews
 * Unified with RN useReviewStore behavior
 */
@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val reviewRepository: ReviewRepository
) : ViewModel() {
    
    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews: StateFlow<List<Review>> = _reviews.asStateFlow()
    
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
    
    private var currentAttractionId: String? = null
    
    /**
     * Load reviews for an attraction
     */
    fun loadReviews(attractionId: String, forceRefresh: Boolean = false) {
        if (!forceRefresh && currentAttractionId == attractionId && _reviews.value.isNotEmpty()) {
            // Already loaded
            return
        }
        
        currentAttractionId = attractionId
        _loading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                // Always refresh from remote first (approved-only)
                reviewRepository.refreshApprovedReviews(attractionId)
                reviewRepository.getReviewsForAttraction(attractionId, _sortBy.value)
                    .collect { reviewList ->
                        _reviews.value = reviewList
                        _loading.value = false
                    }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load reviews for attraction $attractionId")
                _error.value = e.message
                _loading.value = false
            }
        }
    }
    
    /**
     * Change sort order
     */
    fun setSortBy(newSortBy: ReviewSortOption) {
        _sortBy.value = newSortBy
        currentAttractionId?.let { loadReviews(it) }
    }
    
    /**
     * Like a review
     */
    fun likeReview(reviewId: String) {
        viewModelScope.launch {
            try {
                reviewRepository.updateReaction(reviewId, isLike = true)
                    .onSuccess {
                        Timber.d("Successfully liked review $reviewId")
                    }
                    .onFailure { e ->
                        Timber.e(e, "Failed to like review $reviewId")
                    }
            } catch (e: Exception) {
                Timber.e(e, "Error liking review $reviewId")
            }
        }
    }
    
    /**
     * Dislike a review
     */
    fun dislikeReview(reviewId: String) {
        viewModelScope.launch {
            try {
                reviewRepository.updateReaction(reviewId, isLike = false)
                    .onSuccess {
                        Timber.d("Successfully disliked review $reviewId")
                    }
                    .onFailure { e ->
                        Timber.e(e, "Failed to dislike review $reviewId")
                    }
            } catch (e: Exception) {
                Timber.e(e, "Error disliking review $reviewId")
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
                        Timber.d("Successfully submitted review for attraction $attractionId")
                        _submitSuccess.value = true
                        // Refresh reviews list
                        loadReviews(attractionId, forceRefresh = true)
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
     * Clear reviews (e.g., when navigating away)
     */
    fun clearReviews() {
        _reviews.value = emptyList()
        currentAttractionId = null
    }
}
