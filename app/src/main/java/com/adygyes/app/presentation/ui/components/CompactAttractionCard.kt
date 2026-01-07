package com.adygyes.app.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.adygyes.app.domain.model.Attraction
import com.adygyes.app.domain.model.AttractionCategory
import com.adygyes.app.domain.model.Location
import com.adygyes.app.domain.model.ContactInfo

/**
 * Compact attraction card for search results panel.
 * Unified with RN CompactAttractionCard component.
 * 
 * Layout:
 * - Circular image (64dp) with category dot indicator
 * - Content: title, category row (emoji + name), bottom row (rating + distance)
 * - Right section: favorite button + chevron
 */
@Composable
fun CompactAttractionCard(
    attraction: Attraction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFavoriteClick: (() -> Unit)? = null,
    showChevron: Boolean = true,
    distance: String? = null
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular image with category dot (like RN imageContainer)
            Box(
                modifier = Modifier.size(64.dp)
            ) {
                AsyncImage(
                    model = attraction.images.firstOrNull(),
                    contentDescription = attraction.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            getCategoryColor(attraction.category).copy(alpha = 0.1f),
                            CircleShape
                        ),
                    contentScale = ContentScale.Crop
                )
                
                // Category dot indicator (bottom-right)
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.BottomEnd)
                        .background(
                            color = getCategoryColor(attraction.category),
                            shape = CircleShape
                        )
                        .padding(2.dp)
                )
            }
            
            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // Name
                Text(
                    text = attraction.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                // Category row (emoji + name)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = attraction.category.emoji,
                        fontSize = 12.sp
                    )
                    Text(
                        text = attraction.category.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.height(2.dp))
                
                // Bottom row (rating + distance)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Rating
                    attraction.averageRating?.let { rating ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = Color(0xFFFFB300) // Gold
                            )
                            Text(
                                text = String.format("%.1f", rating),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    
                    // Distance
                    distance?.let { dist ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Navigation,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = dist,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // Right section (favorite + chevron)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Favorite button
                onFavoriteClick?.let { onFav ->
                    IconButton(
                        onClick = onFav,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (attraction.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (attraction.isFavorite) "Remove from favorites" else "Add to favorites",
                            modifier = Modifier.size(20.dp),
                            tint = if (attraction.isFavorite) 
                                Color(0xFFE53935) // Red
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Chevron
                if (showChevron) {
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

private fun getCategoryColor(category: AttractionCategory): Color = when (category) {
    AttractionCategory.NATURE -> Color(0xFF4CAF50)
    AttractionCategory.HISTORY -> Color(0xFF795548)
    AttractionCategory.CULTURE -> Color(0xFF9C27B0)
    AttractionCategory.ENTERTAINMENT -> Color(0xFFE91E63)
    AttractionCategory.RECREATION -> Color(0xFF03A9F4)
    AttractionCategory.GASTRONOMY -> Color(0xFFFF9800)
    AttractionCategory.RELIGIOUS -> Color(0xFF607D8B)
    AttractionCategory.ADVENTURE -> Color(0xFFFF5722)
}
