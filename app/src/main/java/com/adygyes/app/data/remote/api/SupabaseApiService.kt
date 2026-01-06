package com.adygyes.app.data.remote.api

import com.adygyes.app.data.remote.dto.AttractionDto
import com.adygyes.app.data.remote.dto.ReviewDto
import com.adygyes.app.data.remote.dto.SyncMetadataDto
import retrofit2.Response
import retrofit2.http.GET
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
        @Query("select") select: String = "id,attraction_id,user_id,rating,title,body,status,created_at,updated_at,profiles(display_name,avatar_url)",
        @Query("attraction_id") attractionId: String, // e.g., "eq.uuid"
        @Query("status") status: String = "eq.approved",
        @Query("order") order: String = "created_at.desc"
    ): Response<List<ReviewDto>>
}
