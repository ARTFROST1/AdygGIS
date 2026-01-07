package com.adygyes.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adygyes.app.domain.model.Attraction
import com.adygyes.app.domain.repository.AttractionRepository
import com.adygyes.app.domain.usecase.ShareUseCase
import com.adygyes.app.domain.usecase.NavigationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the favorites screen
 */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repository: AttractionRepository,
    private val shareUseCase: ShareUseCase,
    private val navigationUseCase: NavigationUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    private val _viewMode = MutableStateFlow(ViewMode.GRID)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()
    
    private val _sortBy = MutableStateFlow(SortBy.DATE_ADDED)
    val sortBy: StateFlow<SortBy> = _sortBy.asStateFlow()
    
    init {
        loadFavorites()
        observeSortChanges()
    }
    
    private fun observeSortChanges() {
        sortBy
            .drop(1) // Skip initial value
            .onEach { sortFavorites() }
            .launchIn(viewModelScope)
    }
    
    fun loadFavorites() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            
            try {
                repository.getFavoriteAttractions().collect { favorites ->
                    _uiState.value = UiState.Success(
                        favorites = sortAttractions(favorites, _sortBy.value)
                    )
                }
            } catch (exception: Exception) {
                _uiState.value = UiState.Error(
                    message = exception.message ?: "Failed to load favorites"
                )
            }
        }
    }
    
    fun refresh() {
        loadFavorites()
    }
    
    fun removeFavorite(attractionId: String) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is UiState.Success) {
                // Optimistically update UI
                val updatedFavorites = currentState.favorites.filter { it.id != attractionId }
                _uiState.value = currentState.copy(favorites = updatedFavorites)
                
                // Update in repository
                repository.updateFavoriteStatus(attractionId, isFavorite = false)
            }
        }
    }
    
    fun toggleViewMode() {
        _viewMode.value = when (_viewMode.value) {
            ViewMode.LIST -> ViewMode.GRID
            ViewMode.GRID -> ViewMode.LIST
        }
    }
    
    fun setSortBy(sortOption: SortBy) {
        _sortBy.value = sortOption
    }
    
    private fun sortFavorites() {
        val currentState = _uiState.value
        if (currentState is UiState.Success) {
            _uiState.value = currentState.copy(
                favorites = sortAttractions(currentState.favorites, _sortBy.value)
            )
        }
    }
    
    private fun sortAttractions(attractions: List<Attraction>, sortBy: SortBy): List<Attraction> {
        return when (sortBy) {
            SortBy.NAME -> attractions.sortedBy { it.name }
            SortBy.CATEGORY -> attractions.sortedBy { it.category.displayName }
            SortBy.RATING -> attractions.sortedByDescending { it.averageRating ?: 0f }
            SortBy.DATE_ADDED -> attractions // Already sorted by date added from repository
        }
    }
    
    fun shareAttraction(attraction: Attraction) {
        shareUseCase.shareAttraction(attraction)
    }
    
    fun shareFavoritesCollection() {
        val currentState = _uiState.value
        if (currentState is UiState.Success && currentState.favorites.isNotEmpty()) {
            shareUseCase.shareAttractionCollection(
                attractions = currentState.favorites,
                collectionName = "Мои избранные места в Адыгее"
            )
        }
    }
    
    fun navigateToAttraction(attraction: Attraction) {
        navigationUseCase.buildRouteToAttraction(attraction)
    }
    
    fun openAttractionInMaps(attraction: Attraction) {
        navigationUseCase.openAttractionInMaps(attraction)
    }
    
    sealed interface UiState {
        data object Loading : UiState
        data class Success(
            val favorites: List<Attraction>
        ) : UiState
        data class Error(val message: String) : UiState
    }
    
    enum class ViewMode {
        LIST,
        GRID
    }
    
    enum class SortBy(val displayName: String) {
        DATE_ADDED("Recently Added"),
        NAME("Name"),
        CATEGORY("Category"),
        RATING("Rating")
    }
}
