package com.adygyes.app.data.remote.api

import com.adygyes.app.data.remote.dto.AttractionDto
import com.adygyes.app.data.remote.dto.CreateReviewRequest
import com.adygyes.app.data.remote.dto.CreateReviewResponse
import com.adygyes.app.data.remote.dto.ReviewDto
import com.adygyes.app.data.remote.dto.ReviewReactionDto
import com.adygyes.app.data.remote.dto.ReviewReactionRequest
import com.adygyes.app.data.remote.dto.SyncMetadataDto
import com.adygyes.app.data.remote.dto.UpdateReviewRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Supabase REST API service for attractions
 * 
 * Uses PostgREST query syntax:
 * - eq.value: equals
 * - gt.value: greater than
 * - lt.value: less than
 * - gte.value: greater than or equal
 * - lte.value: less than or equal
 * 
 * RLS (Row Level Security) policies on Supabase ensure:
 * - Only is_published=true attractions are returned to anon users
 * - Sync metadata is readable for delta sync operations
 */
interface SupabaseApiService {
    
    /**
     * Get all published attractions
     * RLS policy ensures only is_published=true are returned
     */
    @GET("rest/v1/attractions")
    suspend fun getAllAttractions(
        @Query("select") select: String = "*",
        @Query("order") order: String = "name.asc"
    ): Response<List<AttractionDto>>
    
    /**
     * Get attractions updated after specific timestamp (delta sync)
     * 
     * @param updatedAt PostgREST filter (e.g., "gt.2025-01-01T00:00:00Z")
     */
    @GET("rest/v1/attractions")
    suspend fun getUpdatedAttractions(
        @Query("select") select: String = "*",
        @Query("updated_at") updatedAt: String, // e.g., "gt.2025-01-01T00:00:00Z"
        @Query("order") order: String = "updated_at.asc"
    ): Response<List<AttractionDto>>
    
    /**
     * Get single attraction by ID
     */
    @GET("rest/v1/attractions")
    suspend fun getAttractionById(
        @Query("id") id: String, // e.g., "eq.uuid-here"
        @Query("select") select: String = "*"
    ): Response<List<AttractionDto>>
    
    /**
     * Get tombstones (deleted/unpublished records) for sync
     * 
     * @param entityType Filter by entity type (e.g., "eq.attraction")
     * @param deletedAt PostgREST filter for deleted_at timestamp
     */
    @GET("rest/v1/sync_metadata")
    suspend fun getDeletedAttractions(
        @Query("entity_type") entityType: String = "eq.attraction",
        @Query("deleted_at") deletedAt: String, // e.g., "gt.2025-01-01T00:00:00Z"
        @Query("select") select: String = "entity_id"
    ): Response<List<SyncMetadataDto>>
    
    /**
     * Get all tombstones for initial sync or recovery
     */
    @GET("rest/v1/sync_metadata")
    suspend fun getAllTombstones(
        @Query("entity_type") entityType: String = "eq.attraction",
        @Query("select") select: String = "entity_id,action,deleted_at",
        @Query("order") order: String = "deleted_at.desc"
    ): Response<List<SyncMetadataDto>>

    /**
     * Get approved reviews for a specific attraction.
     *
     * Note: With current RLS, anon users can only see status='approved' anyway,
     * but we keep explicit filter for clarity.
     */
    @GET("rest/v1/reviews")
    suspend fun getApprovedReviewsForAttraction(
        @Query("select") select: String = "id,attraction_id,user_id,rating,title,body,status,created_at,updated_at,likes_count,dislikes_count,profiles(display_name,avatar_url)",
        @Query("attraction_id") attractionId: String, // e.g., "eq.uuid"
        @Query("status") status: String = "eq.approved",
        @Query("order") order: String = "created_at.desc"
    ): Response<List<ReviewDto>>
    
    // ========== Authenticated Endpoints (require Bearer token) ==========
    
    /**
     * Create a new review (requires authentication).
     * RLS policy requires auth.uid() to match user_id.
     * Status is automatically set to 'pending' on the server.
     * 
     * @param authorization Bearer token: "Bearer <access_token>"
     * @param prefer "return=representation" to get created record back
     */
    @POST("rest/v1/reviews")
    suspend fun createReview(
        @Header("Authorization") authorization: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body request: CreateReviewRequest
    ): Response<List<CreateReviewResponse>>
    
    /**
     * Get user's own reviews (including pending/rejected).
     * RLS policy allows users to see their own reviews regardless of status.
     */
    @GET("rest/v1/reviews")
    suspend fun getUserOwnReviews(
        @Header("Authorization") authorization: String,
        @Query("select") select: String = "id,attraction_id,rating,title,body,status,rejection_reason,created_at,updated_at,likes_count,dislikes_count",
        @Query("user_id") userId: String, // e.g., "eq.user-uuid"
        @Query("attraction_id") attractionId: String? = null, // e.g., "eq.attraction-uuid"
        @Query("order") order: String = "created_at.desc"
    ): Response<List<ReviewDto>>
    
    /**
     * Check if user already has a review for specific attraction.
     */
    @GET("rest/v1/reviews")
    suspend fun checkUserReviewExists(
        @Header("Authorization") authorization: String,
        @Query("select") select: String = "id",
        @Query("attraction_id") attractionId: String, // e.g., "eq.attraction-uuid"
        @Query("user_id") userId: String // e.g., "eq.user-uuid"
    ): Response<List<ReviewDto>>
    
    /**
     * Update user's own pending review.
     * RLS policy restricts to own reviews with status='pending'.
     */
    @PATCH("rest/v1/reviews")
    suspend fun updateUserReview(
        @Header("Authorization") authorization: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Query("id") reviewId: String, // e.g., "eq.review-uuid"
        @Query("user_id") userId: String, // e.g., "eq.user-uuid"
        @Body request: UpdateReviewRequest
    ): Response<List<ReviewDto>>
    
    /**
     * Delete user's own review.
     * RLS policy restricts to own reviews.
     */
    @DELETE("rest/v1/reviews")
    suspend fun deleteUserReview(
        @Header("Authorization") authorization: String,
        @Query("id") reviewId: String, // e.g., "eq.review-uuid"
        @Query("user_id") userId: String // e.g., "eq.user-uuid"
    ): Response<Unit>
    
    // ========== REVIEW REACTIONS ==========
    
    /**
     * Upsert user's reaction to a review (like/dislike).
     * Uses Prefer: resolution=merge-duplicates to handle unique constraint.
     */
    @POST("rest/v1/review_reactions")
    suspend fun upsertReviewReaction(
        @Header("Authorization") authorization: String,
        @Header("Prefer") prefer: String = "resolution=merge-duplicates,return=representation",
        @Body request: ReviewReactionRequest
    ): Response<List<ReviewReactionDto>>
    
    /**
     * Delete user's reaction from a review (toggle off).
     * RLS policy restricts to own reactions.
     */
    @DELETE("rest/v1/review_reactions")
    suspend fun deleteReviewReaction(
        @Header("Authorization") authorization: String,
        @Query("review_id") reviewId: String, // e.g., "eq.review-uuid"
        @Query("user_id") userId: String      // e.g., "eq.user-uuid"
    ): Response<Unit>
    
    /**
     * Get user's reaction for a specific review.
     * Used to check current reaction state before toggling.
     */
    @GET("rest/v1/review_reactions")
    suspend fun getUserReaction(
        @Header("Authorization") authorization: String,
        @Query("review_id") reviewId: String,
        @Query("user_id") userId: String,
        @Query("select") select: String = "review_id,reaction"
    ): Response<List<ReviewReactionDto>>

    /**
     * Get user's reactions for a set of reviews (batch).
     * Example: reviewIdFilter = "in.(uuid1,uuid2,uuid3)"
     */
    @GET("rest/v1/review_reactions")
    suspend fun getUserReactionsForReviews(
        @Header("Authorization") authorization: String,
        @Query("user_id") userId: String,
        @Query("review_id") reviewIdFilter: String,
        @Query("select") select: String = "review_id,reaction"
    ): Response<List<ReviewReactionDto>>
}
