package com.adygyes.app.presentation.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.adygyes.app.R
import com.adygyes.app.domain.model.Attraction
import com.adygyes.app.presentation.theme.Dimensions

enum class ListViewMode {
    LIST,
    GRID
}

/**
 * Reusable attractions list component for displaying search results
 * Can be used in both MapScreen (list view) and SearchScreen
 * Now supports both List and Grid view modes
 */
@Composable
fun AttractionsList(
    attractions: List<Attraction>,
    onAttractionClick: (String) -> Unit,
    onFavoriteClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    searchQuery: String = "",
    selectedCategories: Set<String> = emptySet(),
    emptyStateMessage: String? = null,
    showResultCount: Boolean = true,
    viewMode: ListViewMode = ListViewMode.LIST,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = Dimensions.PaddingMedium,
        vertical = Dimensions.PaddingMedium
    )
) {
    val listState = rememberLazyListState()
    
    Box(modifier = modifier) {
        when {
            isLoading -> {
                // Loading state with shimmer effect
                LoadingShimmerList(
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            attractions.isEmpty() -> {
                // Empty state with card-style message
                Column(modifier = Modifier.fillMaxSize()) {
                    // Show empty results with same layout as normal results
                    if (showResultCount) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = Dimensions.PaddingMedium,
                                    vertical = Dimensions.PaddingSmall
                                ),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.attractions_count, 0),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            // Applied filters indicator
                            if (searchQuery.isNotEmpty() || selectedCategories.isNotEmpty()) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = buildString {
                                            var filterCount = 0
                                            if (searchQuery.isNotEmpty()) filterCount++
                                            filterCount += selectedCategories.size
                                            append("$filterCount ")
                                            append(if (filterCount == 1) "filter" else "filters")
                                        },
                                        modifier = Modifier.padding(
                                            horizontal = 8.dp,
                                            vertical = 4.dp
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                    
                    // Empty state card
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = contentPadding,
                        verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
                    ) {
                        item {
                            EmptyStateAttractionCard(
                                message = when {
                                    emptyStateMessage != null -> emptyStateMessage
                                    searchQuery.isNotEmpty() && selectedCategories.isNotEmpty() -> 
                                        stringResource(R.string.no_results_with_filters, searchQuery)
                                    searchQuery.isNotEmpty() -> 
                                        stringResource(R.string.no_results_for_query, searchQuery)
                                    selectedCategories.isNotEmpty() -> 
                                        stringResource(R.string.no_results_for_categories)
                                    else -> stringResource(R.string.no_attractions_available)
                                }
                            )
                        }
                    }
                }
            }
            
            else -> {
                // Success state with attractions list or grid
                Column(modifier = Modifier.fillMaxSize()) {
                    // Result count header (shown for both view modes)
                    if (showResultCount) {
                        AnimatedContent(
                            targetState = attractions.size,
                            transitionSpec = {
                                fadeIn() + slideInVertically() togetherWith 
                                fadeOut() + slideOutVertically()
                            }
                        ) { count ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = Dimensions.PaddingMedium,
                                        vertical = Dimensions.PaddingSmall
                                    ),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when {
                                        searchQuery.isNotEmpty() -> 
                                            stringResource(R.string.search_results_count, count)
                                        selectedCategories.isNotEmpty() -> 
                                            stringResource(R.string.filtered_results_count, count)
                                        else -> 
                                            stringResource(R.string.attractions_count, count)
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                // Applied filters indicator
                                if (searchQuery.isNotEmpty() || selectedCategories.isNotEmpty()) {
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = buildString {
                                                var filterCount = 0
                                                if (searchQuery.isNotEmpty()) filterCount++
                                                filterCount += selectedCategories.size
                                                append("$filterCount ")
                                                append(if (filterCount == 1) "filter" else "filters")
                                            },
                                            modifier = Modifier.padding(
                                                horizontal = 8.dp,
                                                vertical = 4.dp
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Content based on view mode
                    when (viewMode) {
                        ListViewMode.LIST -> {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = contentPadding,
                                verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
                            ) {
                                // Attractions list items
                                items(
                                    items = attractions,
                                    key = { it.id }
                                ) { attraction ->
                                    AttractionListItem(
                                        attraction = attraction,
                                        onClick = { onAttractionClick(attraction.id) },
                                        onFavoriteClick = { onFavoriteClick(attraction.id) },
                                        highlightQuery = searchQuery.takeIf { it.isNotEmpty() }
                                    )
                                }
                                
                                // Bottom spacing for FAB
                                item {
                                    Spacer(modifier = Modifier.height(80.dp))
                                }
                            }
                        }
                        
                        else -> { // GRID mode - точно как на экране избранного
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = contentPadding,
                                horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall),
                                verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
                            ) {
                                items(
                                    items = attractions,
                                    key = { it.id }
                                ) { attraction ->
                                    AttractionCard(
                                        attraction = attraction,
                                        onClick = { onAttractionClick(attraction.id) },
                                        onFavoriteClick = { onFavoriteClick(attraction.id) },
                                        compactForFavorites = true // Точно как на экране избранного!
                                    )
                                }
                                
                                // Bottom spacing for FAB (spanning 2 columns)
                                item(span = { GridItemSpan(2) }) {
                                    Spacer(modifier = Modifier.height(80.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact list item for attraction
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttractionListItem(
    attraction: Attraction,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlightQuery: String? = null
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp), // Further increased height for horizontal cards
        shape = RoundedCornerShape(Dimensions.CornerRadiusMedium),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Image section (takes about 50% of card height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(attraction.images.firstOrNull() ?: "")
                        .crossfade(true)
                        .build(),
                    contentDescription = attraction.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(
                            topStart = Dimensions.CornerRadiusMedium,
                            topEnd = Dimensions.CornerRadiusMedium
                        ))
                )

                // Favorite button positioned on image with animation
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp)
                ) {
                    AnimatedContent(
                        targetState = attraction.isFavorite,
                        transitionSpec = {
                            scaleIn(
                                animationSpec = androidx.compose.animation.core.spring(
                                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                                    stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                                )
                            ) togetherWith scaleOut(
                                animationSpec = androidx.compose.animation.core.tween(150)
                            )
                        }
                    ) { isFavorite ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .then(
                                    if (!isFavorite) {
                                        Modifier.background(
                                            Color.Black.copy(alpha = 0.3f),
                                            RoundedCornerShape(50)
                                        )
                                    } else {
                                        Modifier
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (isFavorite) 
                                    stringResource(R.string.remove_from_favorites) 
                                else 
                                    stringResource(R.string.add_to_favorites),
                                tint = if (isFavorite) 
                                    Color(0xFF0C5329) // Зеленый цвет
                                else 
                                    MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Content section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Take remaining space after image
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = Dimensions.PaddingMedium, vertical = 6.dp)
            ) {
                // Category tag and rating on the same level
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CategoryChip(
                        category = attraction.category,
                        compact = true
                    )
                    
                    // Rating section - moved to the same level as category
                    attraction.averageRating?.let { rating ->
                        if (rating > 0) {
                            RatingBar(
                                rating = rating,
                                size = 12.dp,
                                showNumericValue = true,
                                totalReviews = null,
                                compact = true
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(2.dp))

                // Attraction name - увеличенный размер
                Text(
                    text = attraction.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))

                // Short description (first sentence) - увеличенный размер
                Text(
                    text = getFirstSentence(attraction.description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

/**
 * No results state for empty list
 */
@Composable
fun NoResultsState(
    message: String,
    modifier: Modifier = Modifier,
    action: (() -> Unit)? = null,
    actionLabel: String? = null
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium)
    ) {
        // Icon or illustration would go here
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        if (action != null && actionLabel != null) {
            Button(
                onClick = action,
                modifier = Modifier.padding(top = Dimensions.SpacingMedium)
            ) {
                Text(actionLabel)
            }
        }
    }
}

/**
 * Empty state card that looks like a regular attraction card
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmptyStateAttractionCard(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.PaddingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Empty image placeholder
            Surface(
                modifier = Modifier
                    .size(64.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(Dimensions.SpacingMedium))
            
            // Message text
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "Попробуйте изменить фильтры или поисковый запрос",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Extract first sentence from description
 */
private fun getFirstSentence(description: String): String {
    val sentences = description.split(". ", "! ", "? ")
    return if (sentences.isNotEmpty()) {
        val firstSentence = sentences[0].trim()
        if (firstSentence.endsWith(".") || firstSentence.endsWith("!") || firstSentence.endsWith("?")) {
            firstSentence
        } else {
            "$firstSentence."
        }
    } else {
        description
    }
}
