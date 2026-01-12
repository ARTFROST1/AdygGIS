package com.adygyes.app.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.floor

/**
 * 5-star rating display component
 */
@Composable
fun RatingBar(
    rating: Float,
    modifier: Modifier = Modifier,
    maxRating: Int = 5,
    color: Color = Color(0xFFFFB300), // Gold color
    size: Dp = 16.dp,
    compact: Boolean = false,
    showNumericValue: Boolean = true,
    totalReviews: Int? = null
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 4.dp)
    ) {
        // Stars
        val fullStars = floor(rating).toInt()
        val hasHalfStar = (rating - fullStars) >= 0.5f
        val emptyStars = maxRating - fullStars - if (hasHalfStar) 1 else 0
        
        repeat(fullStars) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = "Full star",
                tint = color,
                modifier = Modifier.size(size)
            )
        }
        
        if (hasHalfStar) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.StarHalf,
                contentDescription = "Half star",
                tint = color,
                modifier = Modifier.size(size)
            )
        }
        
        repeat(emptyStars) {
            Icon(
                imageVector = Icons.Outlined.StarOutline,
                contentDescription = "Empty star",
                tint = color.copy(alpha = 0.3f),
                modifier = Modifier.size(size)
            )
        }
        
        // Numeric value and reviews count
        if (showNumericValue) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = String.format("%.1f", rating),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = if (compact) 11.sp else 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            totalReviews?.let { count ->
                Text(
                    text = "($count)",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = if (compact) 11.sp else 12.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Interactive rating bar for user input
 */
@Composable
fun RatingInput(
    rating: Float,
    onRatingChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    maxRating: Int = 5,
    color: Color = Color(0xFFFFB300),
    size: Dp = 32.dp,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (i in 1..maxRating) {
            IconButton(
                onClick = { if (enabled) onRatingChange(i.toFloat()) },
                modifier = Modifier.size(size),
                enabled = enabled
            ) {
                Icon(
                    imageVector = if (i <= rating) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = "Rate $i stars",
                    tint = if (i <= rating) color else color.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
