package com.adygyes.app.presentation.ui.map.markers

import com.adygyes.app.data.local.cache.ImageCacheManager
import com.yandex.mapkit.mapview.MapView
import kotlinx.coroutines.CoroutineScope
import timber.log.Timber

/**
 * Keeps VisualMarkerProvider instances and last rendered attraction ids per MapView
 * so that markers are not force-cleared/re-added when navigating away and back.
 */
object VisualMarkerRegistry {
    private data class Entry(
        val provider: VisualMarkerProvider,
        var lastIds: Set<String> = emptySet()
    )

    private val map = mutableMapOf<MapView, Entry>()

    fun getOrCreate(
        mapView: MapView, 
        imageCacheManager: ImageCacheManager,
        scope: CoroutineScope? = null
    ): VisualMarkerProvider {
        val entry = map[mapView]
        return if (entry != null) {
            entry.provider
        } else {
            val provider = VisualMarkerProvider(mapView, imageCacheManager, scope)
            map[mapView] = Entry(provider)
            provider
        }
    }

    fun getLastIds(mapView: MapView): Set<String> = map[mapView]?.lastIds ?: emptySet()

    fun setLastIds(mapView: MapView, ids: Set<String>) {
        val entry = map[mapView]
        if (entry != null) {
            entry.lastIds = ids
        } else {
            // Should not happen: create empty provider-less entry
            map[mapView] = Entry(provider = VisualMarkerProvider(mapView, ImageCacheManager(mapView.context), null), lastIds = ids)
        }
    }
    
    /**
     * Force reset all VisualMarkerProviders when data version changes
     * This prevents JobCancellationException and invalid MapKit objects
     */
    fun forceResetAll() {
        Timber.d("🔄 Force resetting all ${map.size} VisualMarkerProviders")
        
        map.values.forEach { entry ->
            try {
                entry.provider.forceReset()
                entry.lastIds = emptySet() // Clear cached IDs
            } catch (e: Exception) {
                Timber.w(e, "Error force resetting VisualMarkerProvider")
            }
        }
        
        Timber.d("✅ Force reset completed for all VisualMarkerProviders")
    }
    
    /**
     * Clear registry entry for a specific MapView
     */
    fun clearMapView(mapView: MapView) {
        map.remove(mapView)?.let { entry ->
            try {
                entry.provider.clearMarkers()
            } catch (e: Exception) {
                Timber.w(e, "Error clearing markers for MapView")
            }
        }
    }
    
    /**
     * Get the current active MapView (for restarting preload after version update)
     */
    fun getCurrentMapView(): MapView? {
        return map.keys.firstOrNull()
    }
}
