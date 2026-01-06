package com.adygyes.app.presentation.ui.components.reviews

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adygyes.app.domain.model.Review
import com.adygyes.app.domain.model.ReviewReaction
import com.adygyes.app.presentation.theme.Dimensions
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Review Card Component
 * Карточка отдельного отзыва
 * Unified with RN ReviewCard component
 */
@Composable
fun ReviewCard(
    review: Review,
    onLike: ((String) -> Unit)? = null,
    onDislike: ((String) -> Unit)? = null,
    onShare: ((String) -> Unit)? = null,
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
                .padding(Dimensions.PaddingLarge)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Author info
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Name and badge
                    Column {
                        Text(
                            text = review.authorName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        review.authorBadge?.let { badge ->
                            Text(
                                text = badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Rating and date
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    // Stars
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        repeat(5) { index ->
                            Text(
                                text = if (index < review.rating) "⭐" else "☆",
                                fontSize = 14.sp,
                                color = if (index < review.rating) 
                                    Color(0xFFFFB300) 
                                else 
                                    MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    
                    Text(
                        text = formatDate(review.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Review text
            review.text?.let { text ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )
            }
            
            // Actions
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Like button
                onLike?.let { onLikeClick ->
                    val isLiked = review.userReaction == ReviewReaction.LIKE
                    IconButton(
                        onClick = { onLikeClick(review.id) },
                        modifier = Modifier.size(32.dp),
                        enabled = !review.isOwn
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                                contentDescription = "Like",
                                modifier = Modifier.size(18.dp),
                                tint = if (isLiked) 
                                    MaterialTheme.colorScheme.primary 
                                else if (!review.isOwn)
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                else
                                    MaterialTheme.colorScheme.outline
                            )
                            if (review.likesCount > 0) {
                                Text(
                                    text = review.likesCount.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isLiked) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                // Dislike button
                onDislike?.let { onDislikeClick ->
                    val isDisliked = review.userReaction == ReviewReaction.DISLIKE
                    IconButton(
                        onClick = { onDislikeClick(review.id) },
                        modifier = Modifier.size(32.dp),
                        enabled = !review.isOwn
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isDisliked) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                                contentDescription = "Dislike",
                                modifier = Modifier.size(18.dp),
                                tint = if (isDisliked) 
                                    MaterialTheme.colorScheme.error 
                                else if (!review.isOwn)
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                else
                                    MaterialTheme.colorScheme.outline
                            )
                            if (review.dislikesCount > 0) {
                                Text(
                                    text = review.dislikesCount.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDisliked) 
                                        MaterialTheme.colorScheme.error 
                                    else 
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                // Share button
                onShare?.let { onShareClick ->
                    IconButton(
                        onClick = { onShareClick(review.id) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Format date for review display
 * Matches RN formatDate logic
 */
private fun formatDate(instant: Instant): String {
    val now = Instant.now()
    val daysDiff = ChronoUnit.DAYS.between(instant, now)
    
    return when {
        daysDiff == 0L -> "сегодня"
        daysDiff == 1L -> "вчера"
        daysDiff < 7 -> "$daysDiff дн. назад"
        daysDiff < 30 -> "${daysDiff / 7} нед. назад"
        daysDiff < 365 -> "${daysDiff / 30} мес. назад"
        else -> {
            val date = instant.atZone(ZoneId.systemDefault()).toLocalDate()
            "${date.dayOfMonth} ${getMonthName(date.monthValue)} ${date.year}"
        }
    }
}

private fun getMonthName(month: Int): String = when (month) {
    1 -> "января"
    2 -> "февраля"
    3 -> "марта"
    4 -> "апреля"
    5 -> "мая"
    6 -> "июня"
    7 -> "июля"
    8 -> "августа"
    9 -> "сентября"
    10 -> "октября"
    11 -> "ноября"
    12 -> "декабря"
    else -> ""
}
