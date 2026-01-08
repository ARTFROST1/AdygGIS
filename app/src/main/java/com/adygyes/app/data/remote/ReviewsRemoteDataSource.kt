package com.adygyes.app.data.remote

import com.adygyes.app.data.remote.api.SupabaseApiService
import com.adygyes.app.data.remote.dto.ReviewDto
import kotlinx.coroutines.delay
import timber.log.Timber
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remote data source for reviews with retry logic
 * 
 * Features:
 * - Bulk sync: fetch ALL reviews during main sync
 * - Delta sync: fetch only updated reviews for specific attraction
 * - Automatic retry on transient failures
 * - Detailed logging for debugging
 */
@Singleton
class ReviewsRemoteDataSource @Inject constructor(
    private val apiService: SupabaseApiService
) {
    companion object {
        private const val MAX_RETRIES = 2
        private const val INITIAL_DELAY_MS = 1000L
    }
    
    /**
     * Get ALL approved reviews (bulk sync).
     * Called during main attractions sync to pre-populate Room cache.
     */
    suspend fun getAllApprovedReviews(): NetworkResult<List<ReviewDto>> {
        return executeWithRetry("getAllApprovedReviews") {
            val response = apiService.getAllApprovedReviews()
            
            if (response.isSuccessful) {
                val data = response.body() ?: emptyList()
                Timber.d("✅ Bulk fetched ${data.size} approved reviews")
                NetworkResult.Success(data)
            } else {
                val errorMsg = "Bulk reviews API error: ${response.code()} - ${response.message()}"
                Timber.e("❌ $errorMsg")
                NetworkResult.Error(response.message(), response.code())
            }
        }
    }
    
    /**
     * Get reviews updated since a timestamp (global delta sync).
     */
    suspend fun getUpdatedReviews(since: String): NetworkResult<List<ReviewDto>> {
        return executeWithRetry("getUpdatedReviews(since=$since)") {
            val response = apiService.getUpdatedReviews(updatedAt = "gt.$since")
            
            if (response.isSuccessful) {
                val data = response.body() ?: emptyList()
                Timber.d("✅ Delta fetched ${data.size} updated reviews since $since")
                NetworkResult.Success(data)
            } else {
                val errorMsg = "Delta reviews API error: ${response.code()} - ${response.message()}"
                Timber.e("❌ $errorMsg")
                NetworkResult.Error(response.message(), response.code())
            }
        }
    }
    
    /**
     * Get reviews updated for a specific attraction since last sync.
     * Used when reopening a card to check for updates.
     */
    suspend fun getUpdatedReviewsForAttraction(
        attractionId: String,
        since: String
    ): NetworkResult<List<ReviewDto>> {
        return executeWithRetry("getUpdatedReviewsForAttraction($attractionId, since=$since)") {
            val response = apiService.getUpdatedReviewsForAttraction(
                attractionId = "eq.$attractionId",
                updatedAt = "gt.$since"
            )
            
            if (response.isSuccessful) {
                val data = response.body() ?: emptyList()
                Timber.d("✅ Delta fetched ${data.size} updated reviews for attraction $attractionId")
                NetworkResult.Success(data)
            } else {
                val errorMsg = "Delta reviews for attraction API error: ${response.code()} - ${response.message()}"
                Timber.e("❌ $errorMsg")
                NetworkResult.Error(response.message(), response.code())
            }
        }
    }
    
    /**
     * Get approved reviews for an attraction with retry logic (full fetch for one attraction)
     */
    suspend fun getApprovedReviews(attractionId: String): NetworkResult<List<ReviewDto>> {
        return executeWithRetry("getApprovedReviews($attractionId)") {
            val response = apiService.getApprovedReviewsForAttraction(
                attractionId = "eq.$attractionId"
            )

            if (response.isSuccessful) {
                val data = response.body() ?: emptyList()
                Timber.d("✅ Fetched ${data.size} approved reviews for attraction $attractionId")
                NetworkResult.Success(data)
            } else {
                val errorMsg = "Reviews API error: ${response.code()} - ${response.message()}"
                Timber.e("❌ $errorMsg")
                NetworkResult.Error(response.message(), response.code())
            }
        }
    }
    
    /**
     * Get user's own reviews for an attraction
     */
    suspend fun getUserReviews(
        authorization: String,
        userId: String,
        attractionId: String? = null
    ): NetworkResult<List<ReviewDto>> {
        return executeWithRetry("getUserReviews($userId, $attractionId)") {
            val response = apiService.getUserOwnReviews(
                authorization = authorization,
                userId = "eq.$userId",
                attractionId = attractionId?.let { "eq.$it" }
            )

            if (response.isSuccessful) {
                val data = response.body() ?: emptyList()
                Timber.d("✅ Fetched ${data.size} user reviews")
                NetworkResult.Success(data)
            } else {
                val errorMsg = "User reviews API error: ${response.code()} - ${response.message()}"
                Timber.e("❌ $errorMsg")
                NetworkResult.Error(response.message(), response.code())
            }
        }
    }
    
    /**
     * Execute an API call with automatic retry on transient failures
     */
    private suspend fun <T> executeWithRetry(
        operation: String,
        block: suspend () -> NetworkResult<T>
    ): NetworkResult<T> {
        var lastException: Exception? = null
        var lastResult: NetworkResult<T>? = null
        
        repeat(MAX_RETRIES + 1) { attempt ->
            try {
                val result = block()
                
                // If success, return immediately
                if (result is NetworkResult.Success) {
                    return result
                }
                
                // If error but not retryable (e.g., 4xx), return immediately
                if (result is NetworkResult.Error && !isRetryableError(result.code)) {
                    return result
                }
                
                lastResult = result
                
                // Retry on retryable errors
                if (attempt < MAX_RETRIES) {
                    val delayMs = INITIAL_DELAY_MS * (attempt + 1)
                    Timber.w("⚠️ $operation failed (attempt ${attempt + 1}/${MAX_RETRIES + 1}), retrying in ${delayMs}ms...")
                    delay(delayMs)
                }
                
            } catch (e: SocketTimeoutException) {
                lastException = e
                Timber.w("⏱️ $operation timeout (attempt ${attempt + 1}/${MAX_RETRIES + 1})")
                if (attempt < MAX_RETRIES) {
                    delay(INITIAL_DELAY_MS * (attempt + 1))
                }
            } catch (e: UnknownHostException) {
                lastException = e
                Timber.w("🌐 $operation DNS error (attempt ${attempt + 1}/${MAX_RETRIES + 1})")
                if (attempt < MAX_RETRIES) {
                    delay(INITIAL_DELAY_MS * (attempt + 1))
                }
            } catch (e: IOException) {
                lastException = e
                Timber.w("📡 $operation IO error (attempt ${attempt + 1}/${MAX_RETRIES + 1}): ${e.message}")
                if (attempt < MAX_RETRIES) {
                    delay(INITIAL_DELAY_MS * (attempt + 1))
                }
            } catch (e: Exception) {
                // Non-retryable exception
                Timber.e(e, "❌ $operation failed with non-retryable error")
                return NetworkResult.Error(e.message ?: "Unknown error")
            }
        }
        
        // All retries exhausted
        return lastResult ?: NetworkResult.Error(
            lastException?.message ?: "All retry attempts failed",
            null
        )
    }
    
    private fun isRetryableError(code: Int?): Boolean {
        return when (code) {
            408, 429, 500, 502, 503, 504 -> true
            else -> false
        }
    }
}
