package com.adygyes.app.presentation.ui.components.reviews

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adygyes.app.presentation.theme.Dimensions

/**
 * Rating Summary Block Component
 * Блок общего рейтинга с призывом оставить отзыв
 * Unified with RN RatingSummaryBlock component
 */
@Composable
fun RatingSummaryBlock(
    averageRating: Float,
    totalReviews: Int,
    onRatePress: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimensions.CornerRadiusMedium),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.PaddingExtraLarge)
        ) {
            // Rating Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large rating number
                Text(
                    text = String.format("%.1f", averageRating),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.width(Dimensions.PaddingExtraLarge))
                
                // Stars and review count
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Star rating bar
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val fullStars = averageRating.toInt()
                        val hasHalfStar = (averageRating - fullStars) >= 0.5f
                        
                        repeat(5) { index ->
                            val star = when {
                                index < fullStars -> "⭐"
                                index == fullStars && hasHalfStar -> "⭐" // Could use half star
                                else -> "☆"
                            }
                            Text(
                                text = star,
                                fontSize = 20.sp,
                                color = if (index < averageRating) 
                                    Color(0xFFFFB300) 
                                else 
                                    MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Review count
                    Text(
                        text = "$totalReviews оценок",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Divider
            Spacer(modifier = Modifier.height(Dimensions.PaddingLarge))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(Dimensions.PaddingLarge))
            
            // Call to Action
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Оцените и напишите отзыв",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(Dimensions.PaddingMedium))
                
                // Interactive Rating Stars
                InteractiveRating(
                    value = 0,
                    onRatingChange = { rating -> onRatePress(rating) },
                    size = 32.dp,
                    spacing = 4.dp
                )
            }
        }
    }
}

/**
 * Interactive Rating Component
 * Интерактивные звёзды для оценки места
 * Unified with RN InteractiveRating component
 */
@Composable
fun InteractiveRating(
    value: Int,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 40.dp,
    spacing: androidx.compose.ui.unit.Dp = 8.dp,
    enabled: Boolean = true,
    color: Color = Color(0xFFFFB300),
    emptyColor: Color = Color(0xFFE0E0E0)
) {
    var hoverRating by remember { mutableStateOf(0) }
    val displayRating = if (hoverRating > 0) hoverRating else value
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(5) { index ->
            val starIndex = index + 1
            IconButton(
                onClick = { 
                    if (enabled) {
                        onRatingChange(starIndex)
                    }
                },
                enabled = enabled,
                modifier = Modifier.size(size)
            ) {
                Text(
                    text = if (starIndex <= displayRating) "⭐" else "☆",
                    fontSize = (size.value * 0.8f).sp,
                    color = if (starIndex <= displayRating) color else emptyColor
                )
            }
        }
    }
}
