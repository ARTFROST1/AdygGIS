package com.adygyes.app.presentation.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.adygyes.app.domain.model.Attraction
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Advanced search results panel with three states: Expanded, Half, Hidden
 * Supports drag gestures and smooth animations
 */
@Composable
fun SearchResultsPanel(
    attractions: List<Attraction>,
    isVisible: Boolean,
    hasKeyboard: Boolean,
    isBottomSheetOpen: Boolean = false,
    onAttractionClick: (Attraction) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }
    
    // Panel states
    var panelState by remember { mutableStateOf(SearchPanelState.Hidden) }
    var offsetY by remember { mutableFloatStateOf(screenHeight) }
    
    // State positions - account for search bar and category filters
    val searchBarHeight = 120.dp // Height of search bar + padding
    val categoryFiltersHeight = 40.dp // Height of category filters block + consistent spacing
    val totalTopHeight = searchBarHeight + categoryFiltersHeight // Total reserved top area
    val totalTopHeightPx = with(density) { totalTopHeight.toPx() }
    
    val expandedOffset = totalTopHeightPx // Start below search bar + category filters
    val halfOffset = screenHeight * 0.6f // 60% down from top
    val hiddenOffset = screenHeight
    
    // Update panel state based on props - only Expanded and Half states
    LaunchedEffect(isVisible, hasKeyboard, isBottomSheetOpen) {
        panelState = when {
            !isVisible -> SearchPanelState.Hidden // Only hide when search is completely dismissed
            isBottomSheetOpen -> SearchPanelState.Half // Always Half when BottomSheet is open
            hasKeyboard -> SearchPanelState.Expanded // Full expansion with keyboard
            else -> SearchPanelState.Half // Default to half state (no hidden state during interaction)
        }
    }
    
    // Animate to target position
    val targetOffset = when (panelState) {
        SearchPanelState.Expanded -> expandedOffset
        SearchPanelState.Half -> halfOffset
        SearchPanelState.Hidden -> hiddenOffset
        SearchPanelState.Collapsed -> halfOffset // Same as Half for now
    }
    
    val animatedOffset by animateFloatAsState(
        targetValue = targetOffset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        finishedListener = {
            offsetY = it
        }
    )
    
    // Show panel only when not completely hidden
    if (isVisible || panelState != SearchPanelState.Hidden) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(10f)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .offset(y = with(density) { animatedOffset.toDp() })
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    )
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                // Only two states: Expanded and Half (no Hidden state during interaction)
                                val relativePosition = (offsetY - totalTopHeightPx) / (halfOffset - totalTopHeightPx)
                                panelState = when {
                                    relativePosition < 0.5f -> SearchPanelState.Expanded  // Upper half -> Expanded
                                    else -> SearchPanelState.Half                         // Lower half -> Half
                                }
                            }
                        ) { _, dragAmount ->
                            // Update offset during drag - constrain between Expanded and Half positions
                            val newOffset = (offsetY + dragAmount.y).coerceIn(totalTopHeightPx, halfOffset)
                            offsetY = newOffset
                        }
                    },
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    // Drag handle
                    Surface(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(vertical = 12.dp)
                            .size(width = 40.dp, height = 4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(2.dp)
                    ) {}
                    
                    // Header with results count
                    Text(
                        text = when {
                            attractions.isEmpty() -> "Ничего не найдено"
                            attractions.size == 1 -> "Найдено 1 место"
                            attractions.size in 2..4 -> "Найдено ${attractions.size} места"
                            else -> "Найдено ${attractions.size} мест"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // Results list
                    if (attractions.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 200.dp) // Extra space for bottom sheet state
                        ) {
                            items(
                                items = attractions,
                                key = { it.id }
                            ) { attraction ->
                                CompactAttractionCard(
                                    attraction = attraction,
                                    onClick = {
                                        keyboardController?.hide()
                                        // Smooth transition to hidden state
                                        panelState = SearchPanelState.Hidden
                                        onAttractionClick(attraction)
                                    }
                                )
                            }
                        }
                    } else {
                        // Empty state
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "🔍",
                                    style = MaterialTheme.typography.displayMedium
                                )
                                Text(
                                    text = "Попробуйте изменить запрос",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Search panel state for managing visibility and interactions
 */
enum class SearchPanelState {
    Hidden,
    Half,          // When keyboard is hidden - 50% screen
    Expanded,      // When keyboard is visible - full screen
    Collapsed      // Minimized but visible
}

/**
 * Remember search panel state
 */
@Composable
fun rememberSearchPanelState(
    initialState: SearchPanelState = SearchPanelState.Hidden
): MutableState<SearchPanelState> {
    return remember { mutableStateOf(initialState) }
}
