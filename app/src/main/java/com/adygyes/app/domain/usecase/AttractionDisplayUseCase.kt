package com.adygyes.app.domain.usecase

import com.adygyes.app.domain.model.Attraction
import com.adygyes.app.domain.model.AttractionCategory
import com.adygyes.app.domain.model.distanceTo
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for business rules related to attraction display and filtering
 */
@Singleton
class AttractionDisplayUseCase @Inject constructor() {
    
    /**
     * Apply business rules to filter attractions for display
     */
    fun filterAttractionsForDisplay(
        attractions: List<Attraction>,
        userLatitude: Double? = null,
        userLongitude: Double? = null,
        maxDistance: Float? = null,
        showOnlyOpen: Boolean = false,
        minRating: Float? = null
    ): List<Attraction> {
        return attractions.filter { attraction ->
            // Distance filter
            val withinDistance = if (userLatitude != null && userLongitude != null && maxDistance != null) {
                val distance = attraction.distanceTo(userLatitude, userLongitude)
                distance <= maxDistance * 1000 // Convert km to meters
            } else {
                true
            }
            
            // Rating filter
            val meetsRatingRequirement = if (minRating != null) {
                (attraction.averageRating ?: 0f) >= minRating
            } else {
                true
            }
            
            // Working hours filter (simplified - in real app would parse actual hours)
            val isCurrentlyOpen = if (showOnlyOpen) {
                attraction.workingHours?.let { hours ->
                    !hours.contains("Закрыто", ignoreCase = true)
                } ?: true
            } else {
                true
            }
            
            withinDistance && meetsRatingRequirement && isCurrentlyOpen
        }
    }
    
    /**
     * Sort attractions by relevance based on user context
     */
    fun sortAttractionsByRelevance(
        attractions: List<Attraction>,
        userLatitude: Double? = null,
        userLongitude: Double? = null,
        userPreferences: Set<AttractionCategory> = emptySet(),
        sortBy: SortCriteria = SortCriteria.RELEVANCE
    ): List<Attraction> {
        return when (sortBy) {
            SortCriteria.DISTANCE -> {
                if (userLatitude != null && userLongitude != null) {
                    attractions.sortedBy { it.distanceTo(userLatitude, userLongitude) }
                } else {
                    attractions
                }
            }
            SortCriteria.RATING -> {
                attractions.sortedByDescending { it.averageRating ?: 0f }
            }
            SortCriteria.NAME -> {
                attractions.sortedBy { it.name }
            }
            SortCriteria.CATEGORY -> {
                attractions.sortedBy { it.category.displayName }
            }
            SortCriteria.RELEVANCE -> {
                calculateRelevanceScore(attractions, userLatitude, userLongitude, userPreferences)
                    .sortedByDescending { it.second }
                    .map { it.first }
            }
        }
    }
    
    /**
     * Calculate relevance score for attractions
     */
    private fun calculateRelevanceScore(
        attractions: List<Attraction>,
        userLatitude: Double?,
        userLongitude: Double?,
        userPreferences: Set<AttractionCategory>
    ): List<Pair<Attraction, Float>> {
        return attractions.map { attraction ->
            var score = 0f
            
            // Base score from rating
            score += (attraction.averageRating ?: 3f) * 20f
            
            // Bonus for user preferred categories
            if (userPreferences.contains(attraction.category)) {
                score += 30f
            }
            
            // Distance penalty (closer is better)
            if (userLatitude != null && userLongitude != null) {
                val distance = attraction.distanceTo(userLatitude, userLongitude)
                val distanceKm = distance / 1000f
                score += when {
                    distanceKm <= 5f -> 25f
                    distanceKm <= 15f -> 15f
                    distanceKm <= 50f -> 5f
                    else -> 0f
                }
            }
            
            // Bonus for attractions with complete information
            if (attraction.images.isNotEmpty()) score += 5f
            if (attraction.workingHours != null) score += 3f
            if (attraction.priceInfo != null) score += 2f
            if (attraction.tags.isNotEmpty()) score += 2f
            
            // Bonus for popular attractions (high rating + many tags suggests popularity)
            if ((attraction.averageRating ?: 0f) >= 4.5f && attraction.tags.size >= 3) {
                score += 10f
            }
            
            Pair(attraction, score)
        }
    }
    
    /**
     * Get recommended attractions based on user behavior
     */
    fun getRecommendedAttractions(
        allAttractions: List<Attraction>,
        favoriteAttractions: List<Attraction>,
        recentlyViewed: List<Attraction>,
        limit: Int = 5
    ): List<Attraction> {
        // Analyze user preferences from favorites and recently viewed
        val preferredCategories = (favoriteAttractions + recentlyViewed)
            .groupingBy { it.category }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .map { it.first }
            .take(3)
            .toSet()
        
        val favoriteIds = favoriteAttractions.map { it.id }.toSet()
        val viewedIds = recentlyViewed.map { it.id }.toSet()
        
        // Get attractions user hasn't seen yet
        val unseenAttractions = allAttractions.filter { 
            it.id !in favoriteIds && it.id !in viewedIds 
        }
        
        // Sort by relevance and return top recommendations
        return sortAttractionsByRelevance(
            attractions = unseenAttractions,
            userPreferences = preferredCategories,
            sortBy = SortCriteria.RELEVANCE
        ).take(limit)
    }
    
    /**
     * Check if attraction should be highlighted (featured)
     */
    fun shouldHighlightAttraction(attraction: Attraction): Boolean {
        return when {
            // High-rated attractions
            (attraction.averageRating ?: 0f) >= 4.7f -> true
            
            // Popular nature attractions
            attraction.category == AttractionCategory.NATURE && 
            attraction.tags.any { it.contains("водопад", ignoreCase = true) || 
                               it.contains("гора", ignoreCase = true) ||
                               it.contains("ущелье", ignoreCase = true) } -> true
            
            // Cultural landmarks
            attraction.category == AttractionCategory.CULTURE &&
            attraction.tags.any { it.contains("музей", ignoreCase = true) ||
                               it.contains("памятник", ignoreCase = true) } -> true
            
            // Has many positive indicators
            attraction.images.size >= 3 && 
            attraction.tags.size >= 4 && 
            attraction.workingHours != null -> true
            
            else -> false
        }
    }
    
    /**
     * Get attraction display priority (for map markers)
     */
    fun getDisplayPriority(attraction: Attraction): DisplayPriority {
        return when {
            shouldHighlightAttraction(attraction) -> DisplayPriority.HIGH
            (attraction.averageRating ?: 0f) >= 4.0f -> DisplayPriority.MEDIUM
            attraction.isFavorite -> DisplayPriority.MEDIUM
            else -> DisplayPriority.LOW
        }
    }
    
    /**
     * Validate attraction data completeness
     */
    fun validateAttractionData(attraction: Attraction): ValidationResult {
        val issues = mutableListOf<String>()
        
        if (attraction.name.isBlank()) {
            issues.add("Отсутствует название")
        }
        
        if (attraction.description.isBlank()) {
            issues.add("Отсутствует описание")
        }
        
        if (attraction.images.isEmpty()) {
            issues.add("Отсутствуют фотографии")
        }
        
        if (attraction.location.address.isNullOrBlank()) {
            issues.add("Отсутствует адрес")
        }
        
        if (attraction.averageRating == null) {
            issues.add("Отсутствует рейтинг")
        }
        
        if (attraction.tags.isEmpty()) {
            issues.add("Отсутствуют теги")
        }
        
        val completeness = ((6 - issues.size) / 6f * 100).toInt()
        
        return ValidationResult(
            isValid = issues.isEmpty(),
            issues = issues,
            completeness = completeness
        )
    }
}

/**
 * Sort criteria for attractions
 */
enum class SortCriteria {
    RELEVANCE,
    DISTANCE,
    RATING,
    NAME,
    CATEGORY
}

/**
 * Display priority levels
 */
enum class DisplayPriority {
    HIGH,    // Featured attractions, always visible
    MEDIUM,  // Important attractions, visible at medium zoom
    LOW      // Regular attractions, visible at high zoom
}

/**
 * Validation result for attraction data
 */
data class ValidationResult(
    val isValid: Boolean,
    val issues: List<String>,
    val completeness: Int // Percentage 0-100
)
