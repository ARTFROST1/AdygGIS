package com.adygyes.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for upserting a review reaction (like/dislike)
 * Used with Supabase's upsert feature via Prefer: resolution=merge-duplicates
 */
@Serializable
data class ReviewReactionRequest(
    @SerialName("review_id")
    val reviewId: String,
    
    @SerialName("user_id")
    val userId: String,
    
    val reaction: String // "like" or "dislike"
)

/**
 * Response from Supabase review_reactions table
 */
@Serializable
data class ReviewReactionDto(
    val id: String? = null,
    
    @SerialName("review_id")
    val reviewId: String? = null,
    
    @SerialName("user_id")
    val userId: String? = null,
    
    val reaction: String = "", // "like" or "dislike"
    
    @SerialName("created_at")
    val createdAt: String? = null,
    
    @SerialName("updated_at")
    val updatedAt: String? = null
)
