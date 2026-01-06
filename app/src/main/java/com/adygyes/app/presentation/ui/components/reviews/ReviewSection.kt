package com.adygyes.app.presentation.ui.components.reviews

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adygyes.app.domain.model.Review
import com.adygyes.app.domain.model.ReviewSortOption
import com.adygyes.app.presentation.theme.Dimensions

/**
 * Review Section Component
 * Полная секция отзывов с рейтингом и списком
 * Unified with RN ReviewSection component
 */
@Composable
fun ReviewSection(
    attractionId: String,
    attractionName: String,
    averageRating: Float,
    totalReviews: Int,
    reviews: List<Review>,
    userOwnReviews: List<Review>,
    sortBy: ReviewSortOption,
    onSortChange: (ReviewSortOption) -> Unit,
    onWriteReview: () -> Unit,
    onLike: (String) -> Unit,
    onDislike: (String) -> Unit,
    onShare: (String) -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimensions.PaddingExtraLarge)
    ) {
        // Section Title
        Text(
            text = "Отзывы",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = Dimensions.PaddingLarge, vertical = Dimensions.PaddingMedium)
        )
        
        // Rating Summary
        RatingSummaryBlock(
            averageRating = averageRating,
            totalReviews = totalReviews,
            onRatePress = onWriteReview,
            modifier = Modifier.padding(horizontal = Dimensions.PaddingLarge)
        )
        
        Spacer(modifier = Modifier.height(Dimensions.PaddingLarge))
        
        // User's Own Reviews Section
        if (userOwnReviews.isNotEmpty()) {
            Text(
                text = "Ваш отзыв",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = Dimensions.PaddingLarge, vertical = Dimensions.PaddingMedium)
            )
            
            Column(
                modifier = Modifier.padding(horizontal = Dimensions.PaddingLarge),
                verticalArrangement = Arrangement.spacedBy(Dimensions.PaddingMedium)
            ) {
                userOwnReviews.forEach { review ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusBadge(status = review.status ?: "pending")
                        ReviewCard(
                            review = review,
                            onLike = onLike,
                            onDislike = onDislike,
                            onShare = onShare
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(Dimensions.PaddingLarge))
            
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = Dimensions.PaddingLarge),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            
            Spacer(modifier = Modifier.height(Dimensions.PaddingLarge))
        }
        
        // Public Reviews List Header
        if (reviews.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimensions.PaddingLarge, vertical = Dimensions.PaddingMedium),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (userOwnReviews.isNotEmpty()) {
                        "Отзывы других пользователей"
                    } else {
                        "${reviews.size} отзывов"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                ReviewSortDropdown(
                    sortBy = sortBy,
                    onSortChange = onSortChange
                )
            }
            
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = Dimensions.PaddingLarge),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            
            Spacer(modifier = Modifier.height(Dimensions.PaddingLarge))
        }
        
        // Reviews List or Empty State
        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimensions.PaddingExtraLarge),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (reviews.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimensions.PaddingExtraLarge),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Отзывов пока нет",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Будьте первым!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Reviews list
            Column(
                modifier = Modifier.padding(horizontal = Dimensions.PaddingLarge),
                verticalArrangement = Arrangement.spacedBy(Dimensions.PaddingMedium)
            ) {
                reviews.forEach { review ->
                    ReviewCard(
                        review = review,
                        onLike = onLike,
                        onDislike = onDislike,
                        onShare = onShare
                    )
                }
            }
        }
    }
}

/**
 * Review Sort Dropdown Component
 * Выпадающий список для сортировки отзывов
 * Unified with RN ReviewSortDropdown component
 */
@Composable
fun ReviewSortDropdown(
    sortBy: ReviewSortOption,
    onSortChange: (ReviewSortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box(modifier = modifier) {
        TextButton(onClick = { expanded = true }) {
            Text(
                text = sortBy.displayName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ReviewSortOption.values().forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.displayName) },
                    onClick = {
                        onSortChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
