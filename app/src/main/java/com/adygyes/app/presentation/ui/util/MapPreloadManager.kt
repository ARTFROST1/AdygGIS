package com.adygyes.app.presentation.ui.util

import com.adygyes.app.data.local.cache.ImageCacheManager
import com.adygyes.app.data.sync.SyncService
import com.adygyes.app.domain.model.Attraction
import com.adygyes.app.domain.repository.AttractionRepository
import com.adygyes.app.presentation.ui.map.markers.VisualMarkerProvider
import com.adygyes.app.presentation.ui.map.markers.VisualMarkerRegistry
import com.yandex.mapkit.mapview.MapView
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages preloading of map data and markers in background while splash screen is shown.
 * 
 * OFFLINE-FIRST SUPABASE APPROACH:
 * 1. Immediately read cached data from Room (instant)
 * 2. Display UI with cached data
 * 3. Background sync with Supabase (non-blocking)
 * 4. Update UI when sync completes (if new data)
 * 
 * This ensures the app works offline and starts fast.
 */
@Singleton
class MapPreloadManager @Inject constructor(
    private val repository: AttractionRepository,
    private val imageCacheManager: ImageCacheManager,
    private val syncService: SyncService,
    private val preferencesManager: com.adygyes.app.data.local.preferences.PreferencesManager
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private val _preloadState = MutableStateFlow(PreloadState())
    val preloadState: StateFlow<PreloadState> = _preloadState.asStateFlow()
    
    private val _attractions = MutableStateFlow<List<Attraction>>(emptyList())
    val attractions: StateFlow<List<Attraction>> = _attractions.asStateFlow()
    
    private var preloadJob: Job? = null
    private var visualMarkerProvider: VisualMarkerProvider? = null
    
    data class PreloadState(
        val isLoading: Boolean = false,
        val dataLoaded: Boolean = false,
        val markersCreated: Boolean = false,
        val imagesPreloaded: Boolean = false,
        val allMarkersReady: Boolean = false,
        val dataUpdating: Boolean = false,
        val syncInProgress: Boolean = false,
        val error: String? = null,
        val progress: Float = 0f
    )
    
    /**
     * Start preloading data and creating markers.
     * 
     * OFFLINE-FIRST FLOW:
     * 1. Read cached data from Room immediately (fast, works offline)
     * 2. Create markers and show UI
     * 3. Trigger background Supabase sync (non-blocking)
     * 4. Update markers if sync brings new data
     */
    fun startPreload(mapView: MapView) {
        // Prevent multiple preload attempts
        if (preloadJob?.isActive == true) {
            Timber.d("Preload already in progress")
            return
        }
        
        // Ensure MapView is properly initialized
        if (mapView.mapWindow == null) {
            Timber.w("MapView not ready yet, retrying in 100ms")
            scope.launch {
                delay(100)
                startPreload(mapView)
            }
            return
        }
        
        Timber.d("🚀 Starting OFFLINE-FIRST preload with MapView: ${mapView.hashCode()}")
        
        preloadJob = scope.launch {
            try {
                _preloadState.value = PreloadState(isLoading = true, progress = 0.1f)
                
                // ═══════════════════════════════════════════════════════════
                // STEP 1: Load cached data from Room (INSTANT, works offline)
                // ═══════════════════════════════════════════════════════════
                val cachedAttractions = withContext(Dispatchers.IO) {
                    repository.getAllAttractions().first()
                }
                
                Timber.d("📦 Loaded ${cachedAttractions.size} cached attractions from Room")
                
                // If we have cached data, proceed immediately
                if (cachedAttractions.isNotEmpty()) {
                    _attractions.value = cachedAttractions
                    _preloadState.value = _preloadState.value.copy(
                        dataLoaded = true,
                        progress = 0.3f
                    )
                    
                    // Create markers with cached data
                    createMarkersForAttractions(mapView, cachedAttractions)
                    
                    // Mark as ready - user can proceed
                    _preloadState.value = _preloadState.value.copy(
                        allMarkersReady = true,
                        isLoading = false,
                        progress = 1.0f
                    )
                    Timber.d("✅ Preload complete with cached data")
                    
                    // Background sync (non-blocking)
                    launchBackgroundSync(mapView)
                    
                } else {
                    // ═══════════════════════════════════════════════════════════
                    // NO CACHED DATA: Need to sync from Supabase first
                    // ═══════════════════════════════════════════════════════════
                    Timber.d("📡 No cached data, syncing from Supabase...")
                    _preloadState.value = _preloadState.value.copy(
                        syncInProgress = true,
                        progress = 0.2f
                    )
                    
                    // Perform sync
                    val syncResult = withContext(Dispatchers.IO) {
                        syncService.performSync()
                    }
                    
                    _preloadState.value = _preloadState.value.copy(
                        syncInProgress = false,
                        progress = 0.5f
                    )
                    
                    if (syncResult.success) {
                        // Reload from Room after sync
                        val freshAttractions = withContext(Dispatchers.IO) {
                            repository.getAllAttractions().first()
                        }
                        
                        Timber.d("📦 Synced and loaded ${freshAttractions.size} attractions")
                        
                        if (freshAttractions.isNotEmpty()) {
                            _attractions.value = freshAttractions
                            _preloadState.value = _preloadState.value.copy(
                                dataLoaded = true,
                                progress = 0.7f
                            )
                            
                            createMarkersForAttractions(mapView, freshAttractions)
                            
                            _preloadState.value = _preloadState.value.copy(
                                allMarkersReady = true,
                                isLoading = false,
                                progress = 1.0f
                            )
                            Timber.d("✅ Preload complete after Supabase sync")
                        } else {
                            // Sync succeeded but no data - show empty state
                            _preloadState.value = _preloadState.value.copy(
                                dataLoaded = true,
                                allMarkersReady = true,
                                isLoading = false,
                                progress = 1.0f
                            )
                            Timber.w("⚠️ Sync successful but no attractions found")
                        }
                    } else {
                        // Sync failed - allow user to proceed anyway
                        Timber.e("❌ Supabase sync failed: ${syncResult.errorMessage}")
                        _preloadState.value = _preloadState.value.copy(
                            allMarkersReady = true, // Allow proceed
                            isLoading = false,
                            error = syncResult.errorMessage,
                            progress = 1.0f
                        )
                    }
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Error during map preload")
                // On any error, allow user to proceed
                _preloadState.value = PreloadState(
                    isLoading = false,
                    allMarkersReady = true, // Don't block user
                    error = e.message
                )
            }
        }
    }
    
    /**
     * Create visual markers for attractions (runs on Main thread)
     */
    private suspend fun createMarkersForAttractions(mapView: MapView, attractions: List<Attraction>) {
        withContext(Dispatchers.Main) {
            visualMarkerProvider = VisualMarkerRegistry.getOrCreate(mapView, imageCacheManager)
            
            visualMarkerProvider?.let { provider ->
                provider.preloadMarkers(attractions)
                VisualMarkerRegistry.setLastIds(mapView, attractions.map { it.id }.toSet())
                Timber.d("✅ Created ${attractions.size} markers on MapView: ${mapView.hashCode()}")
            }
        }
        
        _preloadState.value = _preloadState.value.copy(
            markersCreated = true,
            progress = 0.6f
        )
        
        // Preload images in background
        val imageUrls = attractions.mapNotNull { it.images.firstOrNull() }
        if (imageUrls.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                imageCacheManager.preloadImages(imageUrls)
            }
            Timber.d("📷 Preloaded ${imageUrls.size} images")
        }
        
        _preloadState.value = _preloadState.value.copy(
            imagesPreloaded = true,
            progress = 0.9f
        )
    }
    
    /**
     * Launch background sync with Supabase (non-blocking)
     * Uses incremental marker updates to avoid full reload and flickering
     */
    private fun launchBackgroundSync(mapView: MapView) {
        scope.launch {
            try {
                Timber.d("🔄 Starting background Supabase sync...")
                _preloadState.value = _preloadState.value.copy(syncInProgress = true)
                
                val syncResult = withContext(Dispatchers.IO) {
                    syncService.performSync()
                }
                
                _preloadState.value = _preloadState.value.copy(syncInProgress = false)
                
                if (syncResult.success && (syncResult.added > 0 || syncResult.updated > 0 || syncResult.deleted > 0)) {
                    Timber.d("📡 Background sync found changes: +${syncResult.added} ~${syncResult.updated} -${syncResult.deleted}")
                    
                    // Reload fresh data from Room
                    val freshAttractions = withContext(Dispatchers.IO) {
                        repository.getAllAttractions().first()
                    }
                    
                    if (freshAttractions != _attractions.value) {
                        val previousAttractions = _attractions.value
                        val previousById = previousAttractions.associateBy { it.id }

                        // Preload images for NEW/UPDATED attractions first, so marker icon updates hit cache.
                        val changedOrNewImageUrls = freshAttractions
                            .asSequence()
                            .filter { attraction ->
                                val old = previousById[attraction.id]
                                old == null || old.images != attraction.images
                            }
                            .mapNotNull { it.images.firstOrNull() }
                            .distinct()
                            .toList()

                        if (changedOrNewImageUrls.isNotEmpty()) {
                            withContext(Dispatchers.IO) {
                                imageCacheManager.preloadImages(changedOrNewImageUrls)
                            }
                            Timber.d("📷 Preloaded ${changedOrNewImageUrls.size} new/updated images")
                        }

                        // Apply marker updates only after data + relevant images are ready.
                        withContext(Dispatchers.Main) {
                            visualMarkerProvider?.updateVisualMarkers(freshAttractions)
                        }

                        _attractions.value = freshAttractions
                        Timber.d("✅ Markers updated after background sync (incremental, no flickering)")
                    }
                } else {
                    Timber.d("📡 Background sync: no changes")
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Background sync failed")
                _preloadState.value = _preloadState.value.copy(syncInProgress = false)
            }
        }
    }
    
    /**
     * Clear preload data when no longer needed
     */
    fun clearPreloadData() {
        preloadJob?.cancel()
        _preloadState.value = PreloadState()
        _attractions.value = emptyList()
        visualMarkerProvider = null
        Timber.d("Preload data cleared")
    }
    
    /**
     * Force reset and reload data (e.g., after manual refresh)
     */
    fun forceReset() {
        Timber.d("🔄 Force resetting MapPreloadManager")
        preloadJob?.cancel()
        _preloadState.value = PreloadState()
        _attractions.value = emptyList()
        visualMarkerProvider = null
        VisualMarkerRegistry.forceResetAll()
        Timber.d("✅ MapPreloadManager force reset completed")
    }
    
    /**
     * Trigger manual sync with Supabase
     */
    fun triggerSync(mapView: MapView? = null) {
        val targetMapView = mapView ?: VisualMarkerRegistry.getCurrentMapView()
        if (targetMapView != null) {
            launchBackgroundSync(targetMapView)
        } else {
            Timber.w("⚠️ No MapView available for sync")
        }
    }
    
    /**
     * Check if preload is complete
     */
    fun isPreloadComplete(): Boolean {
        val state = _preloadState.value
        return state.dataLoaded && state.markersCreated && !state.isLoading
    }
    
    /**
     * Animate preloaded markers when user navigates to map
     */
    fun animatePreloadedMarkers() {
        visualMarkerProvider?.animatePreloadedMarkers()
        Timber.d("🎬 Triggered animation for preloaded markers")
    }
    
    /**
     * Show preloaded markers immediately (fallback)
     */
    fun showPreloadedMarkers() {
        visualMarkerProvider?.showPreloadedMarkers()
        Timber.d("👁️ Showed preloaded markers immediately")
    }
    
    /**
     * Check if markers are preloaded and ready
     */
    fun hasPreloadedMarkers(): Boolean {
        return visualMarkerProvider?.hasPreloadedMarkers() == true
    }
    
    fun onDestroy() {
        scope.cancel()
    }
}
