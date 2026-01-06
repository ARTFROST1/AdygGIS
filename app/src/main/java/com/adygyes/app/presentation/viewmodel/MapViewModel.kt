package com.adygyes.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adygyes.app.domain.model.Attraction
import com.adygyes.app.domain.model.AttractionCategory
import com.adygyes.app.domain.repository.AttractionRepository
import com.adygyes.app.domain.usecase.GetLocationUseCase
import com.adygyes.app.domain.usecase.NavigationUseCase
import com.adygyes.app.domain.usecase.ShareUseCase
import com.adygyes.app.domain.usecase.NetworkUseCase
import com.adygyes.app.domain.usecase.AttractionDisplayUseCase
import com.adygyes.app.domain.usecase.SortCriteria
import com.adygyes.app.domain.usecase.NetworkStatus
import com.adygyes.app.presentation.ui.map.markers.MarkerOverlayState
import com.adygyes.app.presentation.ui.map.markers.MarkerState
import com.adygyes.app.presentation.ui.components.ViewMode
import com.adygyes.app.data.local.preferences.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import timber.log.Timber
import com.adygyes.app.data.sync.SyncService
import javax.inject.Inject

/**
 * ViewModel for the Map screen
 * Enhanced with new marker overlay system for 100% reliable clicks
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    private val attractionRepository: AttractionRepository,
    private val getLocationUseCase: GetLocationUseCase,
    private val navigationUseCase: NavigationUseCase,
    private val shareUseCase: ShareUseCase,
    private val networkUseCase: NetworkUseCase,
    private val attractionDisplayUseCase: AttractionDisplayUseCase,
    private val preferencesManager: PreferencesManager,
    private val syncService: SyncService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()
    
    // New marker overlay state for the improved marker system
    private val _markerOverlayState = MutableStateFlow(MarkerOverlayState())
    val markerOverlayState: StateFlow<MarkerOverlayState> = _markerOverlayState.asStateFlow()
    
    private val _attractions = MutableStateFlow<List<Attraction>>(emptyList())
    val attractions: StateFlow<List<Attraction>> = _attractions.asStateFlow()
    
    // Job для управления отслеживанием местоположения
    private var locationTrackingJob: Job? = null
    
    private val _selectedAttraction = MutableStateFlow<Attraction?>(null)
    val selectedAttraction: StateFlow<Attraction?> = _selectedAttraction.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _selectedCategories = MutableStateFlow<Set<AttractionCategory>>(emptySet())
    val selectedCategories: StateFlow<Set<AttractionCategory>> = _selectedCategories.asStateFlow()
    
    // New: Selected category for carousel
    private val _selectedCategoryFilter = MutableStateFlow<CategoryFilter>(CategoryFilter.All)
    val selectedCategoryFilter: StateFlow<CategoryFilter> = _selectedCategoryFilter.asStateFlow()
    
    // New: Sort options for list view
    private val _sortBy = MutableStateFlow(SortBy.NAME)
    val sortBy: StateFlow<SortBy> = _sortBy.asStateFlow()
    
    // New: View mode for list (LIST or GRID)
    private val _listViewMode = MutableStateFlow(ListViewMode.LIST)
    val listViewMode: StateFlow<ListViewMode> = _listViewMode.asStateFlow()
    
    // Main view mode (MAP or LIST)
    private val _viewMode = MutableStateFlow(ViewMode.MAP)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()
    
    // Search panel state
    private val _showSearchPanel = MutableStateFlow(false)
    val showSearchPanel: StateFlow<Boolean> = _showSearchPanel.asStateFlow()
    
    private val _searchPanelHasKeyboard = MutableStateFlow(false)
    val searchPanelHasKeyboard: StateFlow<Boolean> = _searchPanelHasKeyboard.asStateFlow()
    
    private val _selectedFromPanel = MutableStateFlow(false)
    val selectedFromPanel: StateFlow<Boolean> = _selectedFromPanel.asStateFlow()
    
    // Navigation source tracking for DetailScreen -> Map navigation
    private val _selectedFromDetailScreen = MutableStateFlow(false)
    val selectedFromDetailScreen: StateFlow<Boolean> = _selectedFromDetailScreen.asStateFlow()
    
    private val _returnToDetailAttractionId = MutableStateFlow<String?>(null)
    val returnToDetailAttractionId: StateFlow<String?> = _returnToDetailAttractionId.asStateFlow()
    
    private val _shouldReturnToDetail = MutableStateFlow(false)
    val shouldReturnToDetail: StateFlow<Boolean> = _shouldReturnToDetail.asStateFlow()
    
    private val _attractionIdToShowOnMap = MutableStateFlow<String?>(null)
    val attractionIdToShowOnMap: StateFlow<String?> = _attractionIdToShowOnMap.asStateFlow()
    
    val filteredAttractions: StateFlow<List<Attraction>> = combine(
        _attractions,
        _searchQuery,
        _selectedCategoryFilter,
        _sortBy
    ) { attractions, query, categoryFilter, sortBy ->
        var filtered = attractions
        
        // Apply search filter
        if (query.isNotEmpty()) {
            filtered = filtered.filter { attraction ->
                attraction.name.contains(query, ignoreCase = true) ||
                attraction.description.contains(query, ignoreCase = true) ||
                attraction.tags.any { it.contains(query, ignoreCase = true) }
            }
        }
        
        // Apply category filter from carousel
        filtered = when (categoryFilter) {
            is CategoryFilter.All -> filtered
            is CategoryFilter.Favorites -> filtered.filter { it.isFavorite }
            is CategoryFilter.Category -> filtered.filter { it.category == categoryFilter.category }
        }
        
        // Apply sorting
        filtered = when (sortBy) {
            SortBy.NAME -> filtered.sortedBy { it.name }
            SortBy.CATEGORY -> filtered.sortedBy { it.category.displayName }
            SortBy.RATING -> filtered.sortedByDescending { it.rating ?: 0f }
            SortBy.DISTANCE -> {
                // TODO: Implement distance sorting when user location is available
                filtered
            }
        }
        
        filtered
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    init {
        loadAttractions()
        observeLocationSettings()
        observeDataVersionChanges()
        performInitialSupabaseSync()
    }
    
    /**
     * Perform initial sync with Supabase on app startup.
     * Uses delta sync to only fetch changes since last sync.
     */
    private fun performInitialSupabaseSync() {
        viewModelScope.launch {
            try {
                Timber.d("🔄 Starting initial Supabase sync...")
                val result = syncService.performSync()
                if (result.success) {
                    Timber.d("✅ Supabase sync complete: +${result.added} updated=${result.updated} deleted=${result.deleted}")
                    // Reload attractions if there were any changes
                    if (result.added > 0 || result.updated > 0 || result.deleted > 0) {
                        loadAttractions()
                    }
                } else {
                    Timber.w("⚠️ Supabase sync failed: ${result.errorMessage}")
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Error during Supabase sync")
            }
        }
    }
    
    // NOTE:
    // Initial data should come from Supabase via SyncService.
    // Loading from assets JSON here can overwrite Supabase truth and cause inconsistencies.
    
    /**
     * Отслеживает изменения версии данных и принудительно перезагружает attractions
     */
    private fun observeDataVersionChanges() {
        var lastKnownVersion: String? = null
        
        viewModelScope.launch {
            preferencesManager.userPreferencesFlow.collect { preferences ->
                val currentVersion = preferences.dataVersion
                
                if (lastKnownVersion != null && lastKnownVersion != currentVersion) {
                    Timber.d("🔄 Data version changed in MapViewModel: '$lastKnownVersion' → '$currentVersion', reloading attractions")
                    
                    // Force reload attractions after version change
                    delay(2000) // Wait for data update process to complete
                    loadAttractions()
                    
                    Timber.d("✅ Attractions reloaded after version change")
                }
                
                lastKnownVersion = currentVersion
            }
        }
    }
    
    /**
     * Отслеживает настройки местоположения и управляет маркером
     */
    private fun observeLocationSettings() {
        viewModelScope.launch {
            preferencesManager.userPreferencesFlow.collect { preferences ->
                val shouldShowLocation = preferences.autoCenterLocation
                val currentState = _uiState.value
                
                Timber.d("📍 Location settings changed: shouldShow=$shouldShowLocation, currentlyShowing=${currentState.showUserLocationMarker}")
                
                if (shouldShowLocation) {
                    // Включаем отслеживание местоположения если было отключено
                    if (!currentState.showUserLocationMarker && currentState.hasLocationPermission) {
                        Timber.d("📍 Location tracking enabled via settings")
                        startLocationTracking()
                    }
                } else {
                    // НЕМЕДЛЕННО отключаем отслеживание местоположения и убираем маркер
                    Timber.d("🚫 Location tracking disabled via settings")
                    stopLocationTracking()
                    _uiState.update { 
                        it.copy(
                            showUserLocationMarker = false,
                            userLocation = null,
                            isLoadingLocation = false,
                            locationError = null
                        )
                    }
                }
            }
        }
    }
    
    private fun loadAttractions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            attractionRepository.getAllAttractions()
                .catch { error ->
                    Timber.e(error, "Error loading attractions")
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = error.message
                        )
                    }
                }
                .collect { attractionList ->
                    _attractions.value = attractionList
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = null
                        )
                    }
                    Timber.d("Loaded ${attractionList.size} attractions")
                }
        }
    }
    
    fun selectAttraction(attraction: Attraction) {
        Timber.d("Selecting attraction: ${attraction.name}")
        _selectedAttraction.value = attraction
        _uiState.update { it.copy(showAttractionDetail = true) }
    }
    
    /**
     * Handle attraction selection from search panel
     */
    fun selectAttractionFromPanel(attraction: Attraction, mapView: com.yandex.mapkit.mapview.MapView?) {
        Timber.d("Selecting attraction from panel: ${attraction.name}")
        _selectedFromPanel.value = true
        // Keep panel visible in half state instead of hiding completely
        _searchPanelHasKeyboard.value = false // Ensure it's in half state, not expanded
        
        // Center map on selected attraction
        centerMapOnAttraction(attraction, mapView)
        
        // Then show bottom sheet
        selectAttraction(attraction)
    }
    
    /**
     * Handle attraction selection from DetailScreen "Show on Map" button
     * This will open map, center on attraction, and should return to DetailScreen on close
     */
    fun selectAttractionFromDetailScreen(attraction: Attraction, mapView: com.yandex.mapkit.mapview.MapView?) {
        Timber.d("Selecting attraction from DetailScreen: ${attraction.name}")
        _selectedFromDetailScreen.value = true
        _returnToDetailAttractionId.value = attraction.id
        _shouldReturnToDetail.value = false // Reset flag, will be set on dismiss
        
        // Switch to map view
        _viewMode.value = ViewMode.MAP
        
        // Center map on selected attraction
        centerMapOnAttraction(attraction, mapView)
        
        // Then show bottom sheet
        selectAttraction(attraction)
    }
    
    /**
     * Center map on specific attraction with animation
     */
    fun centerMapOnAttraction(attraction: Attraction, mapView: com.yandex.mapkit.mapview.MapView?) {
        mapView?.let { map ->
            viewModelScope.launch {
                try {
                    // Slight offset to show marker above center (shift center down a bit)
                    val latOffset = 0.001 // Move center down by ~100m to show marker above center
                    val targetPoint = com.yandex.mapkit.geometry.Point(
                        attraction.location.latitude - latOffset, // Center slightly below marker
                        attraction.location.longitude
                    )
                    val cameraPosition = com.yandex.mapkit.map.CameraPosition(
                        targetPoint,
                        15.0f, // Zoom level - more distant view with good context
                        0.0f,
                        0.0f
                    )
                    
                    // Smooth animation to the attraction
                    map.map.move(
                        cameraPosition,
                        com.yandex.mapkit.Animation(com.yandex.mapkit.Animation.Type.SMOOTH, 1.0f),
                        null
                    )
                    
                    Timber.d("🎯 Centering map on: ${attraction.name} at ${attraction.location.latitude}, ${attraction.location.longitude}")
                } catch (e: Exception) {
                    Timber.e(e, "❌ Error centering map on attraction")
                }
            }
        }
    }
    
    /**
     * Center map on search results with proper bounds for upper half visibility
     */
    fun centerMapOnSearchResults(mapView: com.yandex.mapkit.mapview.MapView?) {
        mapView?.let { map ->
            viewModelScope.launch {
                try {
                    val currentAttractions = filteredAttractions.value
                    if (currentAttractions.isEmpty()) return@launch
                    
                    if (currentAttractions.size == 1) {
                        // Single result - center on it with offset for upper half
                        val attraction = currentAttractions.first()
                        
                        // Offset the center down so the attraction appears in upper part of screen
                        val latOffset = 0.005 // Move center down by ~500m to show attraction in upper area
                        val targetPoint = com.yandex.mapkit.geometry.Point(
                            attraction.location.latitude - latOffset, // Move center DOWN
                            attraction.location.longitude
                        )
                        
                        // Position in upper third of visible area (accounting for bottom panel)
                        val cameraPosition = com.yandex.mapkit.map.CameraPosition(
                            targetPoint,
                            15.0f, // Slightly zoomed out for context
                            0.0f,
                            0.0f
                        )
                        
                        map.map.move(
                            cameraPosition,
                            com.yandex.mapkit.Animation(com.yandex.mapkit.Animation.Type.SMOOTH, 1.0f),
                            null
                        )
                    } else {
                        // Multiple results - fit all in upper half of screen
                        val points = currentAttractions.map { attraction ->
                            com.yandex.mapkit.geometry.Point(
                                attraction.location.latitude,
                                attraction.location.longitude
                            )
                        }
                        
                        // Calculate bounds
                        val minLat = points.minOf { it.latitude }
                        val maxLat = points.maxOf { it.latitude }
                        val minLon = points.minOf { it.longitude }
                        val maxLon = points.maxOf { it.longitude }
                        
                        // Add padding and offset for upper positioning
                        val latPadding = (maxLat - minLat) * 0.3 // 30% padding
                        val lonPadding = (maxLon - minLon) * 0.2 // 20% padding
                        
                        // Calculate center point and shift DOWN to show all points in upper area
                        val centerLat = (minLat + maxLat) / 2.0
                        val centerLon = (minLon + maxLon) / 2.0
                        
                        // Shift center DOWN so all attractions appear in upper half of screen
                        val latSpanWithPadding = (maxLat - minLat) + latPadding * 2
                        val downwardShift = latSpanWithPadding * 0.25 // Shift center down by 25% of total span
                        val adjustedCenterLat = centerLat - downwardShift
                        
                        // Calculate appropriate zoom level
                        val latSpan = maxLat - minLat + latPadding * 2
                        val lonSpan = maxLon - minLon + lonPadding * 2
                        val maxSpan = maxOf(latSpan, lonSpan)
                        val zoom = when {
                            maxSpan > 0.1 -> 10.0f
                            maxSpan > 0.05 -> 12.0f
                            maxSpan > 0.01 -> 14.0f
                            else -> 15.0f
                        }
                        
                        val cameraPosition = com.yandex.mapkit.map.CameraPosition(
                            com.yandex.mapkit.geometry.Point(adjustedCenterLat, centerLon),
                            zoom,
                            0.0f,
                            0.0f
                        )
                        
                        map.map.move(
                            cameraPosition,
                            com.yandex.mapkit.Animation(com.yandex.mapkit.Animation.Type.SMOOTH, 1.5f),
                            null
                        )
                    }
                    
                    Timber.d("🎯 Centered map on ${currentAttractions.size} search results")
                } catch (e: Exception) {
                    Timber.e(e, "❌ Error centering map on search results")
                }
            }
        }
    }
    
    fun dismissAttractionDetail() {
        _uiState.update { it.copy(showAttractionDetail = false) }
        
        // Reset the flag when closing bottom sheet
        if (_selectedFromPanel.value) {
            _selectedFromPanel.value = false
            // Panel is already visible in half state, no need to restore
        }
        
        // Check if we need to return to DetailScreen
        if (_selectedFromDetailScreen.value) {
            _selectedFromDetailScreen.value = false
            // Trigger return to DetailScreen
            _shouldReturnToDetail.value = true
            Timber.d("📍 Bottom sheet closed, triggering return to DetailScreen")
        }
    }
    
    /**
     * Clear the return to detail screen flag after navigation completed
     */
    fun clearReturnToDetail() {
        _returnToDetailAttractionId.value = null
        _shouldReturnToDetail.value = false
    }
    
    /**
     * Set attraction ID to show on map when navigating from DetailScreen
     */
    fun setAttractionToShowOnMap(attractionId: String) {
        _attractionIdToShowOnMap.value = attractionId
        Timber.d("📍 Set attraction to show on map: $attractionId")
    }
    
    /**
     * Clear attraction to show on map after displaying
     */
    fun clearAttractionToShowOnMap() {
        _attractionIdToShowOnMap.value = null
    }
    
    fun filterByCategory(category: AttractionCategory?) {
        viewModelScope.launch {
            _uiState.update { it.copy(selectedCategory = category) }
            
            if (category == null) {
                loadAttractions()
            } else {
                attractionRepository.getAttractionsByCategory(category)
                    .catch { error ->
                        Timber.e(error, "Error filtering attractions")
                    }
                    .collect { filteredList ->
                        _attractions.value = filteredList
                    }
            }
        }
    }
    
    fun toggleFavorite(attractionId: String) {
        viewModelScope.launch {
            val attraction = _attractions.value.find { it.id == attractionId }
            attraction?.let {
                val newFavoriteStatus = !it.isFavorite
                
                // Обновляем в репозитории
                attractionRepository.updateFavoriteStatus(
                    attractionId = attractionId,
                    isFavorite = newFavoriteStatus
                )
                
                // Мгновенно обновляем локальный список без перезагрузки
                _attractions.value = _attractions.value.map { attr ->
                    if (attr.id == attractionId) {
                        attr.copy(isFavorite = newFavoriteStatus)
                    } else {
                        attr
                    }
                }
                
                // Update selected attraction if it's the same one
                if (_selectedAttraction.value?.id == attractionId) {
                    _selectedAttraction.value = _selectedAttraction.value?.copy(
                        isFavorite = newFavoriteStatus
                    )
                }
                
                // НЕ вызываем loadAttractions() - это вызывало пропадание контейнера!
            }
        }
    }
    
    fun onLocationPermissionGranted() {
        viewModelScope.launch {
            _uiState.update { it.copy(hasLocationPermission = true) }
            
            // Проверяем настройки перед запуском отслеживания
            val preferences = preferencesManager.userPreferencesFlow.first()
            if (preferences.autoCenterLocation) {
                Timber.d("📍 Location permission granted and tracking enabled in settings")
                startLocationTracking()
                // Получаем текущее местоположение сразу после получения разрешения
                getCurrentLocation()
            } else {
                Timber.d("🚫 Location permission granted but tracking disabled in settings")
            }
        }
    }
    
    fun onLocationPermissionDenied() {
        _uiState.update { it.copy(hasLocationPermission = false) }
    }
    
    private fun startLocationTracking() {
        // Останавливаем предыдущее отслеживание если есть
        stopLocationTracking()
        
        locationTrackingJob = viewModelScope.launch {
            getLocationUseCase.getLocationUpdates(5000L)
                .catch { error ->
                    Timber.e(error, "Error getting location updates")
                }
                .collect { location ->
                    location?.let {
                        // Проверяем настройки перед обновлением маркера
                        val preferences = preferencesManager.userPreferencesFlow.first()
                        if (!preferences.autoCenterLocation) {
                            Timber.d("🚫 Location tracking disabled during update, stopping")
                            stopLocationTracking()
                            return@collect
                        }
                        
                        val newLocation = Pair(location.latitude, location.longitude)
                        val currentLocation = _uiState.value.userLocation
                        
                        // Проверяем, изменилось ли местоположение значительно (более 10 метров)
                        val shouldUpdate = currentLocation == null || 
                            calculateDistance(currentLocation, newLocation) > 10.0
                        
                        if (shouldUpdate) {
                            _uiState.update { state ->
                                state.copy(
                                    userLocation = newLocation,
                                    showUserLocationMarker = preferences.autoCenterLocation // Проверяем настройки
                                )
                            }
                            Timber.d("User location updated: ${location.latitude}, ${location.longitude}")
                        } else {
                            Timber.d("User location change too small, skipping update")
                        }
                    }
                }
        }
    }
    
    private fun stopLocationTracking() {
        locationTrackingJob?.cancel()
        locationTrackingJob = null
        Timber.d("🛑 Location tracking stopped")
    }
    
    fun getCurrentLocation() {
        viewModelScope.launch {
            // Проверяем настройки перед получением местоположения
            val preferences = preferencesManager.userPreferencesFlow.first()
            if (!preferences.autoCenterLocation) {
                Timber.d("🚫 Location tracking disabled in settings, skipping getCurrentLocation")
                return@launch
            }
            
            try {
                _uiState.update { it.copy(isLoadingLocation = true) }
                val location = getLocationUseCase.getCurrentLocation()
                location?.let {
                    _uiState.update { state ->
                        state.copy(
                            userLocation = Pair(location.latitude, location.longitude),
                            isLoadingLocation = false,
                            locationError = null,
                            showUserLocationMarker = preferences.autoCenterLocation // Проверяем настройки
                        )
                    }
                    Timber.d("✅ Got current location: ${location.latitude}, ${location.longitude}")
                } ?: run {
                    _uiState.update { it.copy(
                        isLoadingLocation = false,
                        locationError = "Не удалось получить местоположение"
                    ) }
                    Timber.w("❌ Failed to get current location")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoadingLocation = false,
                    locationError = e.message ?: "Ошибка получения местоположения"
                ) }
                Timber.e(e, "❌ Error getting current location")
            }
        }
    }
    
    /**
     * Плавно перемещает камеру к местоположению пользователя
     */
    fun moveToUserLocation(mapView: com.yandex.mapkit.mapview.MapView?) {
        viewModelScope.launch {
            val userLocation = _uiState.value.userLocation
            if (userLocation != null && mapView != null) {
                try {
                    val targetPoint = com.yandex.mapkit.geometry.Point(userLocation.first, userLocation.second)
                    val cameraPosition = com.yandex.mapkit.map.CameraPosition(
                        targetPoint,
                        16.0f, // Увеличенный зум для лучшего обзора
                        0.0f,
                        0.0f
                    )
                    
                    // Плавная анимация перехода к местоположению пользователя
                    mapView.map.move(
                        cameraPosition,
                        com.yandex.mapkit.Animation(com.yandex.mapkit.Animation.Type.SMOOTH, 1.0f), // 1 секунда анимации
                        null
                    )
                    
                    Timber.d("🎯 Moving camera to user location: ${userLocation.first}, ${userLocation.second}")
                } catch (e: Exception) {
                    Timber.e(e, "❌ Error moving camera to user location")
                }
            } else {
                // Если местоположение не известно, получаем его
                if (getLocationUseCase.hasLocationPermission()) {
                    getCurrentLocation()
                } else {
                    _uiState.update { it.copy(locationError = "Нет разрешения на доступ к местоположению") }
                }
            }
        }
    }
    
    fun navigateToAttraction(attraction: Attraction) {
        val userLocation = _uiState.value.userLocation
        if (userLocation != null) {
            navigationUseCase.buildRouteFromCurrentLocation(
                attraction = attraction,
                userLatitude = userLocation.first,
                userLongitude = userLocation.second
            )
        } else {
            navigationUseCase.buildRouteToAttraction(attraction)
        }
    }
    
    fun shareAttraction(attraction: Attraction) {
        shareUseCase.shareAttraction(attraction)
    }
    
    fun shareAttractionLocation(attraction: Attraction) {
        shareUseCase.shareAttractionLocation(attraction)
    }
    
    fun openAttractionInMaps(attraction: Attraction) {
        navigationUseCase.openAttractionInMaps(attraction)
    }
    
    fun filterAttractionsByDistance(maxDistanceKm: Float) {
        val userLocation = _uiState.value.userLocation
        if (userLocation != null) {
            viewModelScope.launch {
                attractionRepository.getAllAttractions()
                    .collect { allAttractions ->
                        val filteredAttractions = attractionDisplayUseCase.filterAttractionsForDisplay(
                            attractions = allAttractions,
                            userLatitude = userLocation.first,
                            userLongitude = userLocation.second,
                            maxDistance = maxDistanceKm
                        )
                        _attractions.value = filteredAttractions
                    }
            }
        }
    }
    
    fun sortAttractions(sortBy: SortCriteria) {
        val userLocation = _uiState.value.userLocation
        val currentAttractions = _attractions.value
        
        val sortedAttractions = attractionDisplayUseCase.sortAttractionsByRelevance(
            attractions = currentAttractions,
            userLatitude = userLocation?.first,
            userLongitude = userLocation?.second,
            sortBy = sortBy
        )
        
        _attractions.value = sortedAttractions
    }
    
    fun getRecommendedAttractions(limit: Int = 5): List<Attraction> {
        viewModelScope.launch {
            val allAttractions = _attractions.value
            val favorites = attractionRepository.getFavoriteAttractions().first()
            
            val recommended = attractionDisplayUseCase.getRecommendedAttractions(
                allAttractions = allAttractions,
                favoriteAttractions = favorites,
                recentlyViewed = emptyList(), // TODO: Implement recently viewed tracking
                limit = limit
            )
            
            _uiState.update { it.copy(recommendedAttractions = recommended) }
        }
        return emptyList()
    }
    
    fun checkNetworkStatus() {
        viewModelScope.launch {
            networkUseCase.getNetworkStatus().collect { status ->
                _uiState.update { 
                    it.copy(
                        isOnline = status is NetworkStatus.Connected,
                        networkStatus = status
                    )
                }
            }
        }
    }
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        
        // Show panel when typing in map mode
        if (query.isNotEmpty() && viewMode.value == ViewMode.MAP) {
            _showSearchPanel.value = true
        } else if (query.isEmpty()) {
            _showSearchPanel.value = false
            _searchPanelHasKeyboard.value = false
        }
    }
    
    fun search() {
        // Filtering is done automatically through the filteredAttractions flow
        Timber.d("Searching with query: ${_searchQuery.value}")
    }
    
    fun toggleCategory(category: AttractionCategory) {
        _selectedCategories.update { categories ->
            if (categories.contains(category)) {
                categories - category
            } else {
                categories + category
            }
        }
    }
    
    fun clearFilters() {
        _selectedCategories.value = emptySet()
    }
    
    fun clearSelection() {
        Timber.d("Clearing attraction selection")
        _selectedAttraction.value = null
        _selectedFromPanel.value = false
        _uiState.update { it.copy(showAttractionDetail = false) }
        
        // If we selected from panel, show panel again when bottom sheet closes
        if (_selectedFromPanel.value && _searchQuery.value.isNotEmpty()) {
            _showSearchPanel.value = true
        }
    }
    
    /**
     * Update search panel visibility
     */
    fun setSearchPanelVisibility(visible: Boolean) {
        _showSearchPanel.value = visible
        if (!visible) {
            _searchPanelHasKeyboard.value = false
        }
    }
    
    /**
     * Update keyboard state for search panel
     */
    fun setSearchPanelKeyboardState(hasKeyboard: Boolean) {
        _searchPanelHasKeyboard.value = hasKeyboard
    }
    
    /**
     * Handle search field focus change
     */
    fun onSearchFieldFocusChanged(isFocused: Boolean) {
        if (isFocused && viewMode.value == ViewMode.MAP) {
            _showSearchPanel.value = true
            _searchPanelHasKeyboard.value = true
        } else if (!isFocused) {
            _searchPanelHasKeyboard.value = false
            // When focus is lost (keyboard hidden), collapse to half and center map
            if (_searchQuery.value.isNotEmpty()) {
                // Keep panel visible in half state - map centering will be handled from UI
                _showSearchPanel.value = true
            } else {
                _showSearchPanel.value = false
            }
        }
    }
    
    /**
     * Handle search query clear - hide panel completely
     */
    fun clearSearchQuery() {
        _searchQuery.value = ""
        _showSearchPanel.value = false
        _searchPanelHasKeyboard.value = false
    }
    
    fun navigateToAttractionById(attractionId: String) {
        val attraction = _attractions.value.find { it.id == attractionId }
        attraction?.let { navigateToAttraction(it) }
    }
    
    fun shareAttractionById(attractionId: String) {
        val attraction = _attractions.value.find { it.id == attractionId }
        attraction?.let { shareAttraction(it) }
    }
    
    /**
     * Update marker positions for the new overlay system
     * Called when the map camera moves
     */
    fun updateMarkerPositions() {
        viewModelScope.launch {
            val updatedMarkers = filteredAttractions.value.map { attraction ->
                MarkerState(
                    attraction = attraction,
                    screenPosition = null, // Will be calculated in MarkerOverlay
                    isVisible = true,
                    isSelected = attraction.id == _selectedAttraction.value?.id,
                    isLoading = false,
                    imageUrl = attraction.images.firstOrNull()
                )
            }
            
            _markerOverlayState.update { state ->
                state.copy(
                    markers = updatedMarkers,
                    selectedMarkerId = _selectedAttraction.value?.id,
                    isUpdating = false
                )
            }
            
            Timber.d("📍 Updated ${updatedMarkers.size} marker positions")
        }
    }
    
    /**
     * Handle marker click from the new overlay system
     * This provides 100% reliable click detection
     */
    fun onMarkerClick(attraction: Attraction) {
        Timber.d("✅ Marker clicked via overlay: ${attraction.name}")
        selectAttraction(attraction)
    }
    
    // New functions for list view enhancements
    fun selectCategoryFilter(filter: CategoryFilter) {
        Timber.d("🔍 CATEGORY FILTER: Selecting filter: $filter")
        _selectedCategoryFilter.value = filter
        
        // Log the effect on attractions
        viewModelScope.launch {
            delay(100) // Small delay to ensure flow updates
            Timber.d("🔍 CATEGORY FILTER: After filter, attractions count: ${filteredAttractions.value.size}")
            Timber.d("🔍 CATEGORY FILTER: Filtered attractions: ${filteredAttractions.value.map { it.name }.joinToString(", ")}")
        }
    }
    
    fun setSortBy(sortOption: SortBy) {
        _sortBy.value = sortOption
    }
    
    fun toggleListViewMode() {
        _listViewMode.value = when (_listViewMode.value) {
            ListViewMode.LIST -> ListViewMode.GRID
            ListViewMode.GRID -> ListViewMode.LIST
        }
    }
    
    fun toggleViewMode() {
        _viewMode.value = when (_viewMode.value) {
            ViewMode.MAP -> ViewMode.LIST
            ViewMode.LIST -> ViewMode.MAP
        }
    }
    
    fun clearLocationError() {
        _uiState.update { it.copy(locationError = null) }
    }
    
    /**
     * Вычисляет расстояние между двумя точками в метрах
     */
    private fun calculateDistance(
        location1: Pair<Double, Double>,
        location2: Pair<Double, Double>
    ): Double {
        val earthRadius = 6371000.0 // Радиус Земли в метрах
        
        val lat1Rad = Math.toRadians(location1.first)
        val lat2Rad = Math.toRadians(location2.first)
        val deltaLatRad = Math.toRadians(location2.first - location1.first)
        val deltaLonRad = Math.toRadians(location2.second - location1.second)
        
        val a = kotlin.math.sin(deltaLatRad / 2) * kotlin.math.sin(deltaLatRad / 2) +
                kotlin.math.cos(lat1Rad) * kotlin.math.cos(lat2Rad) *
                kotlin.math.sin(deltaLonRad / 2) * kotlin.math.sin(deltaLonRad / 2)
        
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        
        return earthRadius * c
    }
    
    // Enums for new functionality
    enum class SortBy(val displayName: String) {
        NAME("По названию"),
        CATEGORY("По категории"),
        RATING("По рейтингу"),
        DISTANCE("По расстоянию")
    }
    
    enum class ListViewMode {
        LIST,
        GRID
    }
    
    sealed class CategoryFilter {
        object All : CategoryFilter()
        object Favorites : CategoryFilter()
        data class Category(val category: AttractionCategory) : CategoryFilter()
    }
    
    override fun onCleared() {
        super.onCleared()
        stopLocationTracking()
        Timber.d("🧹 MapViewModel cleared, location tracking stopped")
    }
}

/**
 * UI state for the Map screen
 */
data class MapUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedCategory: AttractionCategory? = null,
    val showAttractionDetail: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val userLocation: Pair<Double, Double>? = null,
    val isLoadingLocation: Boolean = false,
    val locationError: String? = null,
    val showUserLocationMarker: Boolean = false,
    val isOnline: Boolean = true,
    val networkStatus: NetworkStatus? = null,
    val recommendedAttractions: List<Attraction> = emptyList()
)
