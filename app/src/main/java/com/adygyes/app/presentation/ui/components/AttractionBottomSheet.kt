package com.adygyes.app.presentation.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.adygyes.app.R
import com.adygyes.app.domain.model.Attraction
import com.adygyes.app.domain.model.ReviewSortOption
import com.adygyes.app.presentation.theme.Dimensions
import com.adygyes.app.presentation.ui.components.reviews.ReviewSection
import com.adygyes.app.presentation.ui.components.reviews.WriteReviewModal
import com.adygyes.app.presentation.viewmodel.ReviewViewModel
import kotlinx.coroutines.launch

/**
 * Bottom sheet for displaying attraction details on the map.
 * Unified with RN UnifiedAttractionSheet component.
 * 
 * Structure:
 * - Photo gallery (PhotoGallery component)
 * - Header: title + category chip + rating + favorite button
 * - Action buttons: route + share
 * - Description (3 lines preview)
 * - Info section: address, working hours, price, phone, website, duration, bestTime
 * - Reviews section (like RN)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttractionBottomSheet(
    attraction: Attraction,
    onDismiss: () -> Unit,
    onNavigateToDetail: () -> Unit,
    onBuildRoute: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    distance: Float? = null,
    reviewViewModel: ReviewViewModel = hiltViewModel()
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showWriteReviewModal by remember { mutableStateOf(false) }
    
    // Load reviews when attraction changes
    LaunchedEffect(attraction.id) {
        reviewViewModel.loadReviews(attraction.id)
    }
    
    val reviews by reviewViewModel.reviews.collectAsState()
    val sortBy by reviewViewModel.sortBy.collectAsState()
    val loading by reviewViewModel.loading.collectAsState()
    val submitting by reviewViewModel.submitting.collectAsState()
    val errorMessage by reviewViewModel.error.collectAsState()
    val submitSuccess by reviewViewModel.submitSuccess.collectAsState()
    
    // Close modal on successful submit
    LaunchedEffect(submitSuccess) {
        if (submitSuccess) {
            showWriteReviewModal = false
            reviewViewModel.resetSubmitSuccess()
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
        dragHandle = {
            BottomSheetDefaults.DragHandle()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = Dimensions.PaddingLarge)
        ) {
            // Photo gallery
            PhotoGallery(
                images = attraction.images,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .padding(horizontal = Dimensions.PaddingLarge),
                onImageClick = { 
                    scope.launch {
                        sheetState.hide()
                        onNavigateToDetail()
                    }
                },
                onFullscreenClick = {
                    scope.launch {
                        sheetState.hide()
                        onNavigateToDetail()
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Header section
            Column(
                modifier = Modifier.padding(horizontal = Dimensions.PaddingLarge)
            ) {
                // Title row with favorite button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = attraction.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (attraction.isFavorite) 
                                Icons.Default.Favorite 
                            else 
                                Icons.Default.FavoriteBorder,
                            contentDescription = if (attraction.isFavorite) 
                                stringResource(R.string.cd_remove_from_favorites) 
                            else 
                                stringResource(R.string.cd_add_to_favorites),
                            tint = if (attraction.isFavorite) 
                                Color(0xFFE53935) // Red for favorite
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Meta row: category chip + rating + review count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CategoryChip(
                        category = attraction.category,
                        size = ChipSize.MEDIUM,
                        showEmoji = true,
                        showLabel = true
                    )
                    
                    // Rating with review count
                    attraction.rating?.let { rating ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFFFFB300) // Gold
                            )
                            Text(
                                text = String.format("%.1f", rating),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            attraction.reviewsCount?.let { count ->
                                Text(
                                    text = "($count)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                // Distance
                distance?.let { dist ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Navigation,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = formatDistance(dist),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimensions.PaddingLarge),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Route button (primary)
                Button(
                    onClick = onBuildRoute,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Directions,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.detail_route))
                }
                
                // Share button (outlined)
                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.detail_share))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = Dimensions.PaddingLarge),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // Description
            Text(
                text = attraction.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = Dimensions.PaddingLarge)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Info section (unified with RN AttractionInfo)
            Column(
                modifier = Modifier.padding(horizontal = Dimensions.PaddingLarge),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Address (clickable)
                attraction.location.address?.let { address ->
                    InfoRow(
                        icon = Icons.Outlined.LocationOn,
                        label = stringResource(R.string.detail_address),
                        value = address,
                        showChevron = true,
                        onClick = {
                            val uri = Uri.parse("geo:${attraction.location.latitude},${attraction.location.longitude}?q=${Uri.encode(address)}")
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        }
                    )
                }
                
                // Working hours
                attraction.workingHours?.let { hours ->
                    InfoRow(
                        icon = Icons.Outlined.Schedule,
                        label = stringResource(R.string.detail_working_hours),
                        value = hours
                    )
                }
                
                // Price
                attraction.priceInfo?.let { price ->
                    InfoRow(
                        icon = Icons.Outlined.Payments,
                        label = stringResource(R.string.detail_price),
                        value = price
                    )
                }
                
                // Phone (clickable)
                attraction.contactInfo?.phone?.let { phone ->
                    InfoRow(
                        icon = Icons.Outlined.Phone,
                        label = stringResource(R.string.detail_phone),
                        value = phone,
                        showChevron = true,
                        onClick = {
                            val uri = Uri.parse("tel:$phone")
                            context.startActivity(Intent(Intent.ACTION_DIAL, uri))
                        }
                    )
                }
                
                // Website (clickable)
                attraction.contactInfo?.website?.let { website ->
                    InfoRow(
                        icon = Icons.Outlined.Language,
                        label = stringResource(R.string.detail_website),
                        value = website,
                        showChevron = true,
                        maxLines = 1,
                        onClick = {
                            val uri = Uri.parse(if (website.startsWith("http")) website else "https://$website")
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        }
                    )
                }
                
                // Duration (extended field)
                attraction.duration?.let { duration ->
                    InfoRow(
                        icon = Icons.Outlined.Timer,
                        label = stringResource(R.string.detail_duration),
                        value = duration
                    )
                }
                
                // Best time to visit (extended field)
                attraction.bestTimeToVisit?.let { bestTime ->
                    InfoRow(
                        icon = Icons.Outlined.CalendarMonth,
                        label = stringResource(R.string.detail_best_time),
                        value = bestTime
                    )
                }
                
                // Operating season (extended field)
                attraction.operatingSeason?.let { season ->
                    InfoRow(
                        icon = Icons.Outlined.WbSunny,
                        label = stringResource(R.string.detail_operating_season),
                        value = season
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = Dimensions.PaddingLarge),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            
            // Reviews Section (like RN UnifiedAttractionSheet)
            ReviewSection(
                attractionId = attraction.id,
                attractionName = attraction.name,
                averageRating = attraction.averageRating ?: attraction.rating ?: 0f,
                totalReviews = attraction.reviewsCount ?: 0,
                reviews = reviews,
                sortBy = sortBy,
                onSortChange = { reviewViewModel.setSortBy(it) },
                onWriteReview = { showWriteReviewModal = true },
                onLike = { reviewId -> reviewViewModel.likeReview(reviewId) },
                onDislike = { reviewId -> reviewViewModel.dislikeReview(reviewId) },
                onShare = { reviewId -> /* TODO: Share review */ },
                loading = loading
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // "More details" button
            TextButton(
                onClick = {
                    scope.launch {
                        sheetState.hide()
                        onNavigateToDetail()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimensions.PaddingLarge)
            ) {
                Text(stringResource(R.string.detail_details))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        // Write Review Modal
        WriteReviewModal(
            visible = showWriteReviewModal,
            onClose = { showWriteReviewModal = false },
            onSubmit = { rating, text ->
                reviewViewModel.submitReview(attraction.id, rating, text)
            },
            attractionName = attraction.name,
            submitting = submitting,
            errorMessage = errorMessage
        )
    }
}

/**
 * Info row component matching RN AttractionInfo style
 */
@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    showChevron: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon in circular container
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        // Chevron for clickable rows
        if (showChevron && onClick != null) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    
    // Bottom divider
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

fun formatDistance(distanceInMeters: Float): String {
    return when {
        distanceInMeters < 1000 -> "${distanceInMeters.toInt()} м"
        else -> "%.1f км".format(distanceInMeters / 1000)
    }
}
