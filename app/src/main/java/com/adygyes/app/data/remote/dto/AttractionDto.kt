package com.adygyes.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for attractions from Supabase
 * 
 * Uses snake_case for @SerialName to match Supabase PostgreSQL column names.
 * Note: isFavorite is NOT included here - it's a local-only field stored in Room.
 */
@Serializable
data class AttractionDto(
    @SerialName("id")
    val id: String,
    
    @SerialName("name")
    val name: String,
    
    @SerialName("description")
    val description: String,
    
    @SerialName("category")
    val category: String,
    
    @SerialName("latitude")
    val latitude: Double,
    
    @SerialName("longitude")
    val longitude: Double,
    
    @SerialName("address")
    val address: String? = null,
    
    @SerialName("directions")
    val directions: String? = null,
    
    @SerialName("images")
    val images: List<String> = emptyList(),
    
    @SerialName("rating")
    val rating: Float? = null,
    
    // Reviews aggregate fields (computed by Supabase trigger)
    @SerialName("reviews_count")
    val reviewsCount: Int? = null,
    
    @SerialName("average_rating")
    val averageRating: Float? = null,
    
    // Contact info - snake_case to match Supabase
    @SerialName("working_hours")
    val workingHours: String? = null,
    
    @SerialName("phone_number")
    val phoneNumber: String? = null,
    
    @SerialName("email")
    val email: String? = null,
    
    @SerialName("website")
    val website: String? = null,
    
    @SerialName("tags")
    val tags: List<String> = emptyList(),
    
    @SerialName("price_info")
    val priceInfo: String? = null,
    
    @SerialName("amenities")
    val amenities: List<String> = emptyList(),
    
    // Extended fields from RN (now unified across platforms)
    @SerialName("operating_season")
    val operatingSeason: String? = null,
    
    @SerialName("duration")
    val duration: String? = null,
    
    @SerialName("best_time_to_visit")
    val bestTimeToVisit: String? = null,
    
    // Metadata fields from Supabase
    @SerialName("is_published")
    val isPublished: Boolean = true,
    
    @SerialName("created_at")
    val createdAt: String? = null,
    
    @SerialName("updated_at")
    val updatedAt: String? = null
)

/**
 * Root object for legacy JSON parsing (attractions.json file)
 * Kept for backward compatibility with local JSON data
 */
@Serializable
data class AttractionsResponse(
    @SerialName("version")
    val version: String,
    
    @SerialName("lastUpdated")
    val lastUpdated: String,
    
    @SerialName("attractions")
    val attractions: List<AttractionDto>
)
