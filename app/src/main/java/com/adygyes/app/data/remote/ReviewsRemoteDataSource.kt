package com.adygyes.app.data.remote

import com.adygyes.app.data.remote.api.SupabaseApiService
import com.adygyes.app.data.remote.dto.ReviewDto
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewsRemoteDataSource @Inject constructor(
    private val apiService: SupabaseApiService
) {
    suspend fun getApprovedReviews(attractionId: String): NetworkResult<List<ReviewDto>> {
        return try {
            val response = apiService.getApprovedReviewsForAttraction(
                attractionId = "eq.$attractionId"
            )

            if (response.isSuccessful) {
                val data = response.body() ?: emptyList()
                Timber.d("✅ Fetched ${data.size} approved reviews for attraction $attractionId")
                NetworkResult.Success(data)
            } else {
                Timber.e("❌ Reviews API error: ${response.code()} - ${response.message()}")
                NetworkResult.Error(response.message(), response.code())
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Network error fetching reviews")
            NetworkResult.Error(e.message ?: "Unknown error")
        }
    }
}
