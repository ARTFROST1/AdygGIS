package com.adygyes.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for reviews from Supabase.
 * Mirrors `reviews` table (see IMPLEMENTATION_MASTER_PLAN.md).
 */
@Serializable
data class ReviewDto(
    @SerialName("id")
    val id: String,

    @SerialName("attraction_id")
    val attractionId: String,

    @SerialName("user_id")
    val userId: String? = null,

    @SerialName("rating")
    val rating: Int,

    @SerialName("title")
    val title: String? = null,

    @SerialName("body")
    val body: String,

    @SerialName("status")
    val status: String? = null,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("updated_at")
    val updatedAt: String? = null,

    // Joined profile (public read allowed by RLS)
    @SerialName("profiles")
    val profile: ProfileDto? = null
)

@Serializable
data class ProfileDto(
    @SerialName("display_name")
    val displayName: String? = null,

    @SerialName("avatar_url")
    val avatarUrl: String? = null
)
