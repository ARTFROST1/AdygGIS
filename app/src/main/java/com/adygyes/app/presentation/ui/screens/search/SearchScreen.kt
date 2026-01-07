package com.adygyes.app.presentation.ui.screens.search

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adygyes.app.R
import com.adygyes.app.domain.model.Attraction
import com.adygyes.app.domain.model.AttractionCategory
import com.adygyes.app.presentation.theme.Dimensions
import com.adygyes.app.presentation.ui.components.*
import com.adygyes.app.presentation.ui.components.CategoryFilterBottomSheet
import com.adygyes.app.presentation.viewmodel.SearchViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Search screen with real-time filtering and category selection
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun SearchScreen(
    onBackClick: () -> Unit,
    mapViewModel: com.adygyes.app.presentation.viewmodel.MapViewModel,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategories by viewModel.selectedCategories.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    
    // Локальное состояние избранного - НЕ вызывает перерисовку всего экрана
    val localFavoriteStates = remember { mutableStateMapOf<String, Boolean>() }
    
    // Инициализируем локальное состояние при изменении uiState
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is SearchViewModel.UiState.Success -> {
                state.attractions.forEach { attraction ->
                    if (!localFavoriteStates.containsKey(attraction.id)) {
                        localFavoriteStates[attraction.id] = attraction.isFavorite
                    }
                }
            }
            is SearchViewModel.UiState.Initial -> {
                state.popularAttractions.forEach { attraction ->
                    if (!localFavoriteStates.containsKey(attraction.id)) {
                        localFavoriteStates[attraction.id] = attraction.isFavorite
                    }
                }
            }
            else -> {}
        }
    }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    val scope = rememberCoroutineScope()
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var showFilterSheet by remember { mutableStateOf(false) }
    
    // Handle back gesture - close filter sheet if open, otherwise navigate back
    BackHandler(enabled = true) {
        if (showFilterSheet) {
            showFilterSheet = false
        } else {
            onBackClick()
        }
    }
    
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.nav_search),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    // Clear filters button
                    if (selectedCategories.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.clearFilters() }
                        ) {
                            Text(
                                text = "Clear",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            SearchBar(
                query = searchQuery,
                onQueryChange = { query ->
                    viewModel.updateSearchQuery(query)
                    // Debounce search
                    searchJob?.cancel()
                    searchJob = scope.launch {
                        delay(300)
                        viewModel.search()
                    }
                },
                onSearch = {
                    keyboardController?.hide()
                    viewModel.search()
                },
                onFilterClick = { showFilterSheet = true },
                hasActiveFilters = selectedCategories.isNotEmpty(),
                placeholder = "Search attractions...",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimensions.PaddingMedium)
                    .padding(top = Dimensions.PaddingMedium),
                focusOnStart = true
            )
            
            // Quick category filters
            if (selectedCategories.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimensions.PaddingSmall),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = Dimensions.PaddingMedium)
                ) {
                    items(selectedCategories.toList()) { category ->
                        InputChip(
                            selected = true,
                            onClick = { viewModel.toggleCategory(category) },
                            enabled = true,
                            label = { Text(category.displayName) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }
            }
            
            // Search suggestions
            AnimatedVisibility(
                visible = suggestions.isNotEmpty() && searchQuery.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                SearchSuggestions(
                    suggestions = suggestions,
                    onSuggestionClick = { suggestion ->
                        viewModel.updateSearchQuery(suggestion)
                        viewModel.search()
                        keyboardController?.hide()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimensions.PaddingMedium)
                        .padding(top = Dimensions.PaddingSmall),
                    currentQuery = searchQuery
                )
            }
            
            // Results
            when (val state = uiState) {
                is SearchViewModel.UiState.Loading -> {
                    LoadingShimmerList(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = Dimensions.PaddingMedium)
                    )
                }
                
                is SearchViewModel.UiState.Success -> {
                    if (state.attractions.isEmpty()) {
                        NoSearchResultsState(
                            query = searchQuery,
                            onClearFilters = if (selectedCategories.isNotEmpty()) {
                                { viewModel.clearFilters() }
                            } else null,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        SearchResultsList(
                            attractions = state.attractions,
                            favoriteStates = localFavoriteStates,
                            onAttractionClick = { attractionId ->
                                mapViewModel.selectAttractionById(attractionId, switchToMap = true)
                                onBackClick()
                            },
                            onFavoriteClick = { attractionId ->
                                // Мгновенно обновляем локальное состояние
                                val currentStatus = localFavoriteStates[attractionId] ?: false
                                localFavoriteStates[attractionId] = !currentStatus
                                // Обновляем в ViewModel асинхронно
                                viewModel.toggleFavorite(attractionId)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        
                        // Results count
                        Text(
                            text = "${state.attractions.size} results found",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(horizontal = Dimensions.PaddingMedium)
                                .padding(top = Dimensions.PaddingSmall)
                        )
                    }
                }
                
                is SearchViewModel.UiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.search() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                is SearchViewModel.UiState.Initial -> {
                    // Initial state - show recent searches or suggestions
                    InitialSearchState(
                        recentSearches = state.recentSearches,
                        popularAttractions = state.popularAttractions,
                        favoriteStates = localFavoriteStates,
                        onSearchClick = { query ->
                            viewModel.updateSearchQuery(query)
                            viewModel.search()
                        },
                        onAttractionClick = { attractionId ->
                            mapViewModel.selectAttractionById(attractionId, switchToMap = true)
                            onBackClick()
                        },
                        onFavoriteClick = { attractionId ->
                            // Мгновенно обновляем локальное состояние
                            val currentStatus = localFavoriteStates[attractionId] ?: false
                            localFavoriteStates[attractionId] = !currentStatus
                            // Обновляем в ViewModel асинхронно
                            viewModel.toggleFavorite(attractionId)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
    
    // Filter bottom sheet
    if (showFilterSheet) {
        CategoryFilterBottomSheet(
            selectedCategories = selectedCategories,
            onCategoryToggle = { category ->
                viewModel.toggleCategory(category)
            },
            onApply = {
                showFilterSheet = false
                viewModel.search()
            },
            onDismiss = { showFilterSheet = false },
            onClearAll = { viewModel.clearFilters() }
        )
    }
}

@Composable
private fun SearchResultsList(
    attractions: List<Attraction>,
    favoriteStates: SnapshotStateMap<String, Boolean>,
    onAttractionClick: (String) -> Unit,
    onFavoriteClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = Dimensions.PaddingMedium,
            vertical = Dimensions.PaddingMedium
        ),
        verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
    ) {
        items(
            items = attractions,
            key = { it.id }
        ) { attraction ->
            // Получаем актуальное состояние избранного из отдельного StateFlow
            val actualFavoriteStatus = favoriteStates[attraction.id] ?: attraction.isFavorite
            val attractionWithActualFavorite = attraction.copy(isFavorite = actualFavoriteStatus)
            
            AttractionListItem(
                attraction = attractionWithActualFavorite,
                onClick = { onAttractionClick(attraction.id) },
                onFavoriteClick = { onFavoriteClick(attraction.id) }
            )
        }
    }
}

@Composable
private fun InitialSearchState(
    recentSearches: List<String>,
    popularAttractions: List<Attraction>,
    favoriteStates: SnapshotStateMap<String, Boolean>,
    onSearchClick: (String) -> Unit,
    onAttractionClick: (String) -> Unit,
    onFavoriteClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(Dimensions.PaddingMedium),
        verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium)
    ) {
        // Recent searches
        if (recentSearches.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.search_recent),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = Dimensions.PaddingSmall)
                )
            }

            items(recentSearches) { search ->
                Surface(
                    onClick = { onSearchClick(search) },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimensions.PaddingMedium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = search,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(Dimensions.SpacingLarge))
            }
        }

        // Popular attractions
        if (popularAttractions.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.search_popular),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = Dimensions.PaddingSmall)
                )
            }

            items(
                items = popularAttractions,
                key = { it.id }
            ) { attraction ->
                // Получаем актуальное состояние избранного из отдельного StateFlow
                val actualFavoriteStatus = favoriteStates[attraction.id] ?: attraction.isFavorite
                val attractionWithActualFavorite = attraction.copy(isFavorite = actualFavoriteStatus)
                
                AttractionCard(
                    attraction = attractionWithActualFavorite,
                    onClick = { onAttractionClick(attraction.id) },
                    onFavoriteClick = { onFavoriteClick(attraction.id) },
                    modifier = Modifier.padding(vertical = Dimensions.PaddingExtraSmall)
                )
            }
        }
    }
}