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
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.adygyes.app.R
import com.adygyes.app.domain.model.Attraction
import com.adygyes.app.presentation.theme.Dimensions
import com.vanniktech.emoji.EmojiTextView

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
 * Horizontal layout: Content left + Square image right (like RN AttractionCard)
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
            .height(120.dp), // Compact height matching RN
        shape = RoundedCornerShape(Dimensions.CornerRadiusMedium),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        // Horizontal layout: Content left + Image right
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // Left side: Content section
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Title
                Text(
                    text = attraction.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
                
                // Rating and Category badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    // Rating badge
                    attraction.averageRating?.let { rating ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = Color(0xFFFFB800) // Yellow star
                                )
                                Text(
                                    text = String.format("%.1f", rating),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    
                    // Category badge with Apple-style emoji
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            AndroidView(
                                factory = { context ->
                                    EmojiTextView(context).apply {
                                        text = attraction.category.emoji
                                        textSize = 13f
                                        includeFontPadding = false
                                    }
                                },
                                modifier = Modifier.wrapContentHeight(Alignment.CenterVertically)
                            )
                            Text(
                                text = attraction.category.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                
                // Description
                ExpandableText(
                    text = attraction.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        lineHeight = 16.sp,
                        fontSize = 12.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    collapsedMaxLines = 2,
                    expandButtonText = "Ещё",
                    collapseButtonText = "Скрыть",
                    showIcon = false,
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                // Address
                attraction.location.address?.let { address ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = address,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 11.sp
                        )
                    }
                }
            }
            
            // Right side: Square image section
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .fillMaxHeight()
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(attraction.images.firstOrNull() ?: "")
                        .crossfade(true)
                        .build(),
                    contentDescription = attraction.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Favorite button on image - compact
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
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
                                .size(32.dp)
                                .background(
                                    Color.Black.copy(alpha = 0.4f),
                                    RoundedCornerShape(16.dp)
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
                                    Color(0xFFE53935) // Red for favorite
                                else 
                                    Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
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
