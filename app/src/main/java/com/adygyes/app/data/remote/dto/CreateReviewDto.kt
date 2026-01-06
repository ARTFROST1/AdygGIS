package com.adygyes.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for creating a new review.
 * 
 * RLS policy requires:
 * - auth.uid() = user_id (user must match JWT)
 * - status = 'pending' (automatically set by DEFAULT)
 * - moderated_at IS NULL (automatically NULL for new records)
 * 
 * Note: user_id MUST be provided as the column doesn't have a DEFAULT.
 * The body field is NOT NULL in DB but can be empty string.
 */
@Serializable
data class CreateReviewRequest(
    @SerialName("attraction_id")
    val attractionId: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("rating")
    val rating: Int,
    @SerialName("title")
    val title: String? = null,
    @SerialName("body")
    val body: String  // NOT NULL in DB, empty string if no text
)

/**
 * Response from creating a review (Supabase returns the created row)
 */
@Serializable
data class CreateReviewResponse(
    @SerialName("id")
    val id: String,
    @SerialName("attraction_id")
    val attractionId: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("rating")
    val rating: Int,
    @SerialName("title")
    val title: String? = null,
    @SerialName("body")
    val body: String? = null,
    @SerialName("status")
    val status: String,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String
)

/**
 * Request body for updating user review
 */
@Serializable
data class UpdateReviewRequest(
    @SerialName("rating")
    val rating: Int,
    @SerialName("title")
    val title: String? = null,
    @SerialName("body")
    val body: String? = null
)
