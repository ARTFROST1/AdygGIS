package com.adygyes.app.data.mapper

import com.adygyes.app.data.local.entities.AttractionEntity
import com.adygyes.app.data.remote.dto.AttractionDto
import com.adygyes.app.domain.model.Attraction
import com.adygyes.app.domain.model.AttractionCategory
import com.adygyes.app.domain.model.ContactInfo
import com.adygyes.app.domain.model.Location

/**
 * Mapper for converting between domain models and data entities
 * 
 * Handles conversions:
 * - AttractionDto (Supabase) ↔ AttractionEntity (Room)
 * - AttractionEntity (Room) ↔ Attraction (Domain)
 * - AttractionDto (Supabase) → Attraction (Domain)
 */
object AttractionMapper {
    
    /**
     * Convert AttractionEntity to Attraction domain model
     */
    fun AttractionEntity.toDomainModel(): Attraction {
        return Attraction(
            id = id,
            name = name,
            description = description,
            category = try {
                AttractionCategory.valueOf(category)
            } catch (e: IllegalArgumentException) {
                AttractionCategory.NATURE
            },
            location = Location(
                latitude = latitude,
                longitude = longitude,
                address = address,
                directions = directions
            ),
            images = images,
            workingHours = workingHours,
            contactInfo = if (phoneNumber != null || email != null || website != null) {
                ContactInfo(
                    phone = phoneNumber,
                    email = email,
                    website = website
                )
            } else null,
            isFavorite = isFavorite,
            tags = tags,
            priceInfo = priceInfo,
            amenities = amenities,
            // Extended fields
            reviewsCount = reviewsCount,
            averageRating = averageRating,
            operatingSeason = operatingSeason,
            duration = duration,
            bestTimeToVisit = bestTimeToVisit
        )
    }
    
    /**
     * Convert Attraction domain model to AttractionEntity
     */
    fun Attraction.toEntity(): AttractionEntity {
        return AttractionEntity(
            id = id,
            name = name,
            description = description,
            category = category.name,
            latitude = location.latitude,
            longitude = location.longitude,
            address = location.address,
            directions = location.directions,
            images = images,
            workingHours = workingHours,
            phoneNumber = contactInfo?.phone,
            email = contactInfo?.email,
            website = contactInfo?.website,
            isFavorite = isFavorite,
            tags = tags,
            priceInfo = priceInfo,
            amenities = amenities,
            // Extended fields
            reviewsCount = reviewsCount,
            averageRating = averageRating,
            operatingSeason = operatingSeason,
            duration = duration,
            bestTimeToVisit = bestTimeToVisit
        )
    }
    
    /**
     * Convert list of AttractionEntity to list of Attraction domain models
     */
    fun List<AttractionEntity>.toDomainModels(): List<Attraction> {
        return map { it.toDomainModel() }
    }
    
    /**
     * Convert list of Attraction domain models to list of AttractionEntity
     */
    fun List<Attraction>.toEntities(): List<AttractionEntity> {
        return map { it.toEntity() }
    }
    
    /**
     * Convert AttractionDto to Attraction domain model
     */
    fun AttractionDto.toDomainModel(): Attraction {
        return Attraction(
            id = id,
            name = name,
            description = description,
            category = try {
                AttractionCategory.valueOf(category)
            } catch (e: IllegalArgumentException) {
                AttractionCategory.NATURE // Default category if unknown
            },
            location = Location(
                latitude = latitude,
                longitude = longitude,
                address = address,
                directions = directions
            ),
            images = images,
            workingHours = workingHours,
            contactInfo = if (phoneNumber != null || email != null || website != null) {
                ContactInfo(
                    phone = phoneNumber,
                    email = email,
                    website = website
                )
            } else null,
            isFavorite = false, // isFavorite is local-only, not from API
            tags = tags,
            priceInfo = priceInfo,
            amenities = amenities,
            // Extended fields
            reviewsCount = reviewsCount,
            averageRating = averageRating,
            operatingSeason = operatingSeason,
            duration = duration,
            bestTimeToVisit = bestTimeToVisit
        )
    }
    
    /**
     * Convert AttractionDto to AttractionEntity
     * 
     * Note: isFavorite defaults to false since it's a local-only field.
     * The sync service should preserve existing favorite status when updating.
     */
    fun AttractionDto.toEntity(): AttractionEntity {
        return AttractionEntity(
            id = id,
            name = name,
            description = description,
            category = category,
            latitude = latitude,
            longitude = longitude,
            address = address,
            directions = directions,
            images = images,
            workingHours = workingHours,
            phoneNumber = phoneNumber,
            email = email,
            website = website,
            isFavorite = false, // Local-only, not from API
            tags = tags,
            priceInfo = priceInfo,
            amenities = amenities,
            // Extended fields
            reviewsCount = reviewsCount,
            averageRating = averageRating,
            operatingSeason = operatingSeason,
            duration = duration,
            bestTimeToVisit = bestTimeToVisit,
            // Supabase metadata
            isPublished = isPublished,
            createdAt = createdAt,
            updatedAt = updatedAt,
            lastSyncedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * Convert list of AttractionDto to list of Attraction domain models
     */
    fun List<AttractionDto>.toDomainModelsFromDto(): List<Attraction> {
        return map { it.toDomainModel() }
    }
    
    /**
     * Convert list of AttractionDto to list of AttractionEntity
     */
    fun List<AttractionDto>.toEntitiesFromDto(): List<AttractionEntity> {
        return map { it.toEntity() }
    }
}
