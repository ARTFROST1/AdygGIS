package com.adygyes.app.presentation.ui.map.markers

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.adygyes.app.R
import com.adygyes.app.domain.model.Attraction
import com.adygyes.app.domain.model.AttractionCategory
import timber.log.Timber

/**
 * Circular marker component with image loading for map attractions
 * Provides 100% reliable click detection and beautiful visual appearance
 * 
 * Can operate in two modes:
 * - Visual mode: Shows the marker with image/emoji (default)
 * - Transparent mode: Only handles clicks, no visual (for dual-layer system)
 */
@Composable
fun CircularImageMarker(
    attraction: Attraction,
    screenPosition: Offset,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    size: Dp = MarkerDimensions.DefaultSize,
    borderWidth: Dp = MarkerDimensions.BorderWidth,
    showLoadingIndicator: Boolean = true,
    animateAppearance: Boolean = true,
    transparentMode: Boolean = false // New parameter for transparent click-only mode
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Animation for appearance
    val appearanceScale by animateFloatAsState(
        targetValue = if (animateAppearance) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "appearance_scale"
    )
    
    // Animation for press
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "press_scale"
    )
    
    // Animation for selection
    val selectionScale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "selection_scale"
    )
    
    // Combined scale
    val finalScale = appearanceScale * pressScale * selectionScale
    
    // Shadow elevation based on selection
    val shadowElevation = if (isSelected) {
        MarkerDimensions.SelectedShadowElevation
    } else {
        MarkerDimensions.ShadowElevation
    }
    
    // Border width based on selection
    val finalBorderWidth = if (isSelected) {
        MarkerDimensions.SelectedBorderWidth
    } else {
        borderWidth
    }
    
    // In transparent mode, create an invisible but properly sized clickable area
    if (transparentMode) {
        // Use slightly larger hit area for better touch detection
        val hitAreaSize = size * 1.1f // Slightly larger for better touch
        
        Box(
            modifier = modifier
                .size(hitAreaSize) // Larger hit area for better touch detection
                .testTag("transparent_marker_${attraction.id}")
                .clickable(
                    interactionSource = interactionSource,
                    indication = null // No visual feedback
                ) {
                    Timber.d("🎯 TRANSPARENT CLICK SUCCESS: ${attraction.name} (hit area: ${hitAreaSize}dp)")
                    onClick()
                }
                // Completely transparent - no background
        ) {
            // Empty box - just for click detection
        }
    } else {
        // Original visual mode
        Surface(
            modifier = modifier
                .size(size)
                .scale(finalScale)
                .testTag("circular_marker_${attraction.id}")
                .clickable(
                    interactionSource = interactionSource,
                    indication = null // We handle visual feedback ourselves
                ) {
                    Timber.d("🎯 CircularImageMarker clicked: ${attraction.name}")
                    onClick()
                },
            shape = CircleShape,
            shadowElevation = shadowElevation,
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // Transparent background by default
                    .border(
                        width = finalBorderWidth,
                        color = Color.White,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
            // Load image (no emoji or colored fallback)
            var isLoading by remember { mutableStateOf(true) }
            var hasError by remember { mutableStateOf(false) }
            
            val imageUrl = attraction.images.firstOrNull()

            if (showLoadingIndicator && imageUrl != null && isLoading && !hasError) {
                CircularProgressIndicator(
                    modifier = Modifier.size(size * 0.4f),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            }
            
            if (imageUrl != null && !hasError) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = attraction.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Fit,
                    onLoading = {
                        isLoading = true
                        hasError = false
                    },
                    onSuccess = {
                        isLoading = false
                        hasError = false
                    },
                    onError = {
                        isLoading = false
                        hasError = true
                        Timber.w("Failed to load image for marker: ${attraction.name}")
                    }
                )
            }
            // No emoji fallback — keep marker transparent if no image
        }
    }
    } // End of else block for visual mode
}

/**
 * Get emoji representation for category
 */
private fun getCategoryEmoji(category: AttractionCategory): String {
    return when (category) {
        AttractionCategory.NATURE -> "🌲"
        AttractionCategory.CULTURE -> "🎭"
        AttractionCategory.HISTORY -> "🏛️"
        AttractionCategory.ADVENTURE -> "⛰️"
        AttractionCategory.RECREATION -> "🏖️"
        AttractionCategory.GASTRONOMY -> "🍴"
        AttractionCategory.RELIGIOUS -> "⛪"
        AttractionCategory.ENTERTAINMENT -> "🎪"
    }
}

/**
 * Marker dimension constants
 */
object MarkerDimensions {
    val DefaultSize = 52.dp
    val SmallSize = 44.dp
    val LargeSize = 60.dp
    val BorderWidth = 2.dp
    val SelectedBorderWidth = 3.dp
    val ShadowElevation = 4.dp
    val SelectedShadowElevation = 8.dp
}

/**
 * Marker color provider based on category
 */
object MarkerColors {
    fun getColorForCategory(category: AttractionCategory, isDark: Boolean): Color {
        return when (category) {
            AttractionCategory.NATURE -> if (isDark) Color(0xFF2E7D32) else Color(0xFF0C5329)
            AttractionCategory.CULTURE -> if (isDark) Color(0xFFAB47BC) else Color(0xFF9C27B0)
            AttractionCategory.HISTORY -> if (isDark) Color(0xFF8D6E63) else Color(0xFF795548)
            AttractionCategory.ADVENTURE -> if (isDark) Color(0xFFFF7043) else Color(0xFFFF5722)
            AttractionCategory.RECREATION -> if (isDark) Color(0xFF29B6F6) else Color(0xFF03A9F4)
            AttractionCategory.GASTRONOMY -> if (isDark) Color(0xFFFFB74D) else Color(0xFFFF9800)
            AttractionCategory.RELIGIOUS -> if (isDark) Color(0xFF90A4AE) else Color(0xFF607D8B)
            AttractionCategory.ENTERTAINMENT -> if (isDark) Color(0xFFEC407A) else Color(0xFFE91E63)
        }
    }
}
