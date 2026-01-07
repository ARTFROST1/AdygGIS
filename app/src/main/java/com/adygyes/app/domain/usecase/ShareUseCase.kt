package com.adygyes.app.domain.usecase

import android.content.Context
import android.content.Intent
import com.adygyes.app.domain.model.Attraction
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for sharing attraction information
 */
@Singleton
class ShareUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    /**
     * Share attraction details with other apps
     */
    fun shareAttraction(attraction: Attraction) {
        try {
            val shareText = buildShareText(attraction)
            
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
                putExtra(Intent.EXTRA_SUBJECT, "Посетите ${attraction.name} в Адыгее!")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            val chooserIntent = Intent.createChooser(shareIntent, "Поделиться местом").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            context.startActivity(chooserIntent)
            Timber.d("Shared attraction: ${attraction.name}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to share attraction: ${attraction.name}")
        }
    }
    
    /**
     * Share attraction location (coordinates only)
     */
    fun shareAttractionLocation(attraction: Attraction) {
        try {
            val locationText = buildLocationShareText(attraction)
            
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, locationText)
                putExtra(Intent.EXTRA_SUBJECT, "Местоположение: ${attraction.name}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            val chooserIntent = Intent.createChooser(shareIntent, "Поделиться местоположением").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            context.startActivity(chooserIntent)
            Timber.d("Shared location for: ${attraction.name}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to share location for: ${attraction.name}")
        }
    }
    
    /**
     * Share multiple attractions (for collections or favorites)
     */
    fun shareAttractionCollection(attractions: List<Attraction>, collectionName: String = "Мои избранные места") {
        try {
            val shareText = buildCollectionShareText(attractions, collectionName)
            
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
                putExtra(Intent.EXTRA_SUBJECT, collectionName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            val chooserIntent = Intent.createChooser(shareIntent, "Поделиться коллекцией").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            context.startActivity(chooserIntent)
            Timber.d("Shared collection: $collectionName with ${attractions.size} attractions")
        } catch (e: Exception) {
            Timber.e(e, "Failed to share collection: $collectionName")
        }
    }
    
    /**
     * Build formatted text for sharing a single attraction
     */
    private fun buildShareText(attraction: Attraction): String {
        return buildString {
            appendLine("🏞️ ${attraction.name}")
            appendLine()
            appendLine("📍 ${attraction.category.displayName}")
            appendLine()
            appendLine("📝 ${attraction.description}")
            appendLine()
            
            attraction.location.address?.let { address ->
                appendLine("📍 Адрес: $address")
            }
            
            attraction.averageRating?.let { rating ->
                appendLine("⭐ Рейтинг: $rating/5")
            }
            
            attraction.workingHours?.let { hours ->
                appendLine("🕐 Время работы: $hours")
            }
            
            attraction.priceInfo?.let { price ->
                appendLine("💰 Цена: $price")
            }
            
            if (attraction.tags.isNotEmpty()) {
                appendLine("🏷️ Теги: ${attraction.tags.joinToString(", ")}")
            }
            
            appendLine()
            appendLine("📍 Координаты: ${attraction.location.latitude}, ${attraction.location.longitude}")
            appendLine("🗺️ Открыть в Яндекс.Картах:")
            append("https://yandex.ru/maps/?ll=${attraction.location.longitude},${attraction.location.latitude}&z=16&text=${attraction.name}")
            appendLine()
            appendLine()
            appendLine("Найдено в приложении AdygGIS - Путеводитель по Адыгее")
        }
    }
    
    /**
     * Build formatted text for sharing location only
     */
    private fun buildLocationShareText(attraction: Attraction): String {
        return buildString {
            appendLine("📍 ${attraction.name}")
            appendLine()
            attraction.location.address?.let { address ->
                appendLine("Адрес: $address")
            }
            appendLine("Координаты: ${attraction.location.latitude}, ${attraction.location.longitude}")
            appendLine()
            appendLine("🗺️ Открыть в Яндекс.Картах:")
            append("https://yandex.ru/maps/?ll=${attraction.location.longitude},${attraction.location.latitude}&z=16&text=${attraction.name}")
        }
    }
    
    /**
     * Build formatted text for sharing a collection of attractions
     */
    private fun buildCollectionShareText(attractions: List<Attraction>, collectionName: String): String {
        return buildString {
            appendLine("🏞️ $collectionName")
            appendLine("📍 ${attractions.size} мест в Адыгее")
            appendLine()
            
            attractions.forEachIndexed { index, attraction ->
                appendLine("${index + 1}. ${attraction.name}")
                appendLine("   📍 ${attraction.category.displayName}")
                attraction.averageRating?.let { rating ->
                    appendLine("   ⭐ $rating/5")
                }
                attraction.location.address?.let { address ->
                    appendLine("   📍 $address")
                }
                appendLine()
            }
            
            appendLine("Найдено в приложении AdygGIS - Путеводитель по Адыгее")
        }
    }
}
