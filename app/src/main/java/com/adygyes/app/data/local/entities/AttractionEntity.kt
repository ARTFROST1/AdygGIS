package com.adygyes.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

/**
 * Room entity for storing attraction data
 * 
 * This entity includes:
 * - All fields from Supabase (for sync)
 * - Local-only fields like isFavorite (not synced)
 * - Sync metadata like lastSyncedAt
 */
@Entity(tableName = "attractions")
@TypeConverters(Converters::class)
data class AttractionEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val address: String?,
    val directions: String?,
    val images: List<String>,
    
    // Reviews aggregate fields (from Supabase trigger)
    val reviewsCount: Int? = null,
    val averageRating: Float? = null,
    
    // Contact info
    val workingHours: String?,
    val phoneNumber: String?,
    val email: String?,
    val website: String?,
    val tags: List<String>,
    val priceInfo: String?,
    val amenities: List<String>,
    
    // Extended fields (unified with RN)
    val operatingSeason: String? = null,
    val duration: String? = null,
    val bestTimeToVisit: String? = null,
    
    // Supabase metadata
    val isPublished: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    
    // Local-only fields (NOT synced with Supabase)
    val isFavorite: Boolean = false,
    val lastSyncedAt: Long = System.currentTimeMillis()
)

/**
 * Type converters for Room database
 */
class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return value?.joinToString(separator = "|||") ?: ""
    }
    
    @TypeConverter
    fun toStringList(value: String): List<String> {
        return if (value.isEmpty()) emptyList() else value.split("|||")
    }
}
