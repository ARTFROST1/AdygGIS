package com.adygyes.app.data.remote

import com.adygyes.app.data.remote.api.SupabaseApiService
import com.adygyes.app.data.remote.dto.AttractionDto
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result wrapper for network operations
 */
sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(val message: String, val code: Int? = null) : NetworkResult<Nothing>()
}

/**
 * Remote data source for Supabase API
 * 
 * Handles all network communication with Supabase REST API.
 * Returns NetworkResult to handle success/error states uniformly.
 */
@Singleton
class SupabaseRemoteDataSource @Inject constructor(
    private val apiService: SupabaseApiService
) {
    
    /**
     * Fetch all published attractions
     */
    suspend fun getAllAttractions(): NetworkResult<List<AttractionDto>> {
        return try {
            val response = apiService.getAllAttractions()
            
            if (response.isSuccessful) {
                val data = response.body() ?: emptyList()
                Timber.d("✅ Fetched ${data.size} attractions from Supabase")
                NetworkResult.Success(data)
            } else {
                Timber.e("❌ API error: ${response.code()} - ${response.message()}")
                NetworkResult.Error(response.message(), response.code())
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Network error fetching attractions")
            NetworkResult.Error(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Fetch attractions updated since timestamp (delta sync)
     * 
     * @param since ISO 8601 timestamp (e.g., "2025-01-01T00:00:00Z")
     */
    suspend fun getUpdatedAttractions(since: String): NetworkResult<List<AttractionDto>> {
        return try {
            // Supabase PostgREST filter format: gt.timestamp
            val response = apiService.getUpdatedAttractions(
                updatedAt = "gt.$since"
            )
            
            if (response.isSuccessful) {
                val data = response.body() ?: emptyList()
                Timber.d("✅ Fetched ${data.size} updated attractions since $since")
                NetworkResult.Success(data)
            } else {
                Timber.e("❌ API error: ${response.code()}")
                NetworkResult.Error(response.message(), response.code())
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Network error fetching updated attractions")
            NetworkResult.Error(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Fetch single attraction by ID
     * 
     * @param id UUID of the attraction
     */
    suspend fun getAttractionById(id: String): NetworkResult<AttractionDto?> {
        return try {
            val response = apiService.getAttractionById(id = "eq.$id")
            
            if (response.isSuccessful) {
                val data = response.body()?.firstOrNull()
                if (data != null) {
                    Timber.d("✅ Fetched attraction: ${data.name}")
                    NetworkResult.Success(data)
                } else {
                    Timber.w("⚠️ Attraction not found: $id")
                    NetworkResult.Success(null)
                }
            } else {
                Timber.e("❌ API error: ${response.code()}")
                NetworkResult.Error(response.message(), response.code())
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Network error fetching attraction $id")
            NetworkResult.Error(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Fetch tombstones (deleted/unpublished attraction IDs)
     * 
     * @param since ISO 8601 timestamp
     */
    suspend fun getDeletedAttractions(since: String): NetworkResult<List<String>> {
        return try {
            val response = apiService.getDeletedAttractions(
                deletedAt = "gt.$since"
            )
            
            if (response.isSuccessful) {
                val data = response.body()?.map { it.entityId } ?: emptyList()
                Timber.d("✅ Fetched ${data.size} deleted attraction IDs since $since")
                NetworkResult.Success(data)
            } else {
                Timber.e("❌ API error: ${response.code()}")
                NetworkResult.Error(response.message(), response.code())
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Network error fetching deleted attractions")
            NetworkResult.Error(e.message ?: "Unknown error")
        }
    }
}
