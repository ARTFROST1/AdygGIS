package com.adygyes.app.presentation.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.adygyes.app.R
import com.adygyes.app.domain.model.Attraction
import com.adygyes.app.presentation.theme.Dimensions
import com.adygyes.app.presentation.theme.getOverlayTextColor
import com.adygyes.app.presentation.theme.getOverlayTextColorWithAlpha
import com.adygyes.app.presentation.theme.getOverlayIconTint
import com.adygyes.app.presentation.theme.getContentIconTint
import com.adygyes.app.presentation.theme.getSecondaryTextColor
import com.adygyes.app.presentation.theme.getContentTextColor

/**
 * Reusable attraction card component
 * Unified with RN AttractionCard component
 * 
 * Layout structure (matching RN):
 * - Image container with overlay
 *   - Favorite button (top-right)
 *   - Category chip (bottom-left, small, no emoji)
 * - Content section
 *   - Title (2 lines max)
 *   - Meta row: emoji + category name + rating
 *   - Description (2 lines, optional)
 *   - Address row with icon
 */
@Composable
fun AttractionCard(
    attraction: Attraction,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDistance: Boolean = false,
    distance: Float? = null,
    showDescription: Boolean = true,
    imageHeight: Int = 180,
    compactForFavorites: Boolean = false
) {
    val hasImage = attraction.images.isNotEmpty()
    val imageUrl = attraction.images.firstOrNull()
    
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimensions.CornerRadiusMedium),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            // Image Container (like RN imageContainer)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(imageHeight.dp)
            ) {
                if (hasImage && imageUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .crossfade(200)
                            .build(),
                        contentDescription = attraction.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Gradient overlay (subtle, like RN)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.1f))
                    )
                } else {
                    // Fallback when no image
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Favorite button (top-right, like RN)
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(40.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(20.dp)
                        )
                ) {
                    AnimatedContent(
                        targetState = attraction.isFavorite,
                        transitionSpec = {
                            scaleIn(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            ) togetherWith scaleOut(animationSpec = tween(150))
                        }
                    ) { isFavorite ->
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorite) 
                                stringResource(R.string.cd_remove_from_favorites) 
                            else 
                                stringResource(R.string.cd_add_to_favorites),
                            tint = if (isFavorite) Color(0xFFE53935) else Color.White, // Red for favorite, white for not
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                // Category chip (bottom-left, small, no emoji - like RN categoryOverlay)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                ) {
                    CategoryChip(
                        category = attraction.category,
                        size = ChipSize.SMALL,
                        showEmoji = false,
                        showLabel = true
                    )
                }
            }
            
            // Content section (like RN content)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Title
                Text(
                    text = attraction.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Meta row: emoji + category + rating (like RN metaRow)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category with emoji
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = attraction.category.emoji,
                            fontSize = 14.sp
                        )
                        Text(
                            text = attraction.category.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Rating (like RN RatingBar)
                    attraction.averageRating?.let { rating ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFFFFB300) // Gold
                            )
                            Text(
                                text = String.format("%.1f", rating),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            // Show review count if available
                            attraction.reviewsCount?.let { count ->
                                if (count > 0) {
                                    Text(
                                        text = "($count)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Description (like RN description)
                if (showDescription && !compactForFavorites) {
                    Text(
                        text = attraction.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                }
                
                // Address row (like RN addressRow)
                attraction.location.address?.let { address ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (compactForFavorites) extractSettlement(address) else address,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
                
                // Distance if available
                if (showDistance && distance != null) {
                    Text(
                        text = formatDistanceForCard(distance),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
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

/**
 * Rating component for attraction cards
 */
@Composable
private fun AttractionRating(
    rating: Float?,
    showDistance: Boolean = false,
    distance: Float? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Rating with star (only show if rating exists)
        if (rating != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⭐",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = String.format("%.1f", rating),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        } else {
            // Empty space if no rating
            Spacer(modifier = Modifier.width(1.dp))
        }

        // Distance if available
        if (showDistance && distance != null) {
            Text(
                text = formatDistanceForCard(distance),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Extract settlement from address
 */
private fun extractSettlement(address: String): String {
    val parts = address.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    return if (parts.isNotEmpty()) parts.last() else address
}

/**
 * Format distance for display in attraction cards
 */
private fun formatDistanceForCard(distance: Float): String {
    return when {
        distance < 1000 -> "${distance.toInt()} м"
        distance < 10000 -> "${"%.1f".format(distance / 1000)} км"
        else -> "${(distance / 1000).toInt()} км"
    }
}