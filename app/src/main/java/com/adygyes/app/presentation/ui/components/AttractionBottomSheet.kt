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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adygyes.app.R
import com.adygyes.app.domain.model.Attraction
import com.adygyes.app.domain.model.ReviewSortOption
import com.adygyes.app.presentation.theme.Dimensions
import com.adygyes.app.presentation.ui.components.reviews.ReviewSection
import com.adygyes.app.presentation.ui.components.reviews.WriteReviewModal
import com.adygyes.app.presentation.ui.components.auth.AuthModal
import com.adygyes.app.presentation.viewmodel.AuthEvent
import com.adygyes.app.presentation.viewmodel.AuthViewModel
import com.adygyes.app.presentation.viewmodel.ReviewViewModel
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Bottom sheet for displaying attraction details.
 * Unified component matching RN UnifiedAttractionSheet.
 * 
 * This is the ONLY component for showing attraction details - no separate detail screen.
 * 
 * Structure (RN style):
 * - Photo gallery (edge-to-edge, no horizontal padding)
 * - Header row: title on left, share + close buttons on right
 * - Meta row: category text + rating (if not null)
 * - Action buttons: context-aware (map/list)
 *   - From map: route + favorite
 *   - From list: show on map + route + favorite
 * - Description
 * - Modern info section
 * - Amenities section (if any)
 * - Tags section (if any)
 * - Reviews section
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttractionBottomSheet(
    attraction: Attraction,
    onDismiss: () -> Unit,
    onBuildRoute: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    openedFromList: Boolean = false,
    onShowOnMap: () -> Unit = {},
    distance: Float? = null,
    reviewViewModel: ReviewViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showWriteReviewModal by remember { mutableStateOf(false) }
    var initialRating by remember { mutableIntStateOf(0) }
    var showAuthModal by remember { mutableStateOf(false) }
    var showPhotoViewer by remember { mutableStateOf(false) }
    var selectedPhotoIndex by remember { mutableIntStateOf(0) }
    
    // Load reviews when attraction changes
    LaunchedEffect(attraction.id) {
        reviewViewModel.loadReviews(attraction.id)
    }
    
    // Auth states
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val isAuthenticated by reviewViewModel.isAuthenticated.collectAsState()
    
    // Reload user's own reviews when auth state changes to authenticated
    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            Timber.d("User authenticated, reloading reviews for ${attraction.id}")
            reviewViewModel.loadReviews(attraction.id, forceRefresh = true)
        }
    }
    
    val reviews by reviewViewModel.reviews.collectAsState()
    val userOwnReviews by reviewViewModel.userOwnReviews.collectAsState()
    val sortBy by reviewViewModel.sortBy.collectAsState()
    val loading by reviewViewModel.loading.collectAsState()
    val isSyncing by reviewViewModel.isSyncing.collectAsState()
    val submitting by reviewViewModel.submitting.collectAsState()
    val errorMessage by reviewViewModel.error.collectAsState()
    val submitSuccess by reviewViewModel.submitSuccess.collectAsState()
    val showAuthRequired by reviewViewModel.showAuthRequired.collectAsState()
    
    // Show auth modal when auth is required
    LaunchedEffect(showAuthRequired) {
        if (showAuthRequired) {
            showAuthModal = true
            reviewViewModel.dismissAuthRequired()
        }
    }
    
    // Handle auth events
    LaunchedEffect(Unit) {
        authViewModel.events.collect { event ->
            when (event) {
                is AuthEvent.SignInSuccess, is AuthEvent.SignUpSuccess -> {
                    showAuthModal = false
                    showWriteReviewModal = true
                }
                is AuthEvent.SignUpNeedsConfirmation -> {
                    showAuthModal = false
                    android.widget.Toast.makeText(
                        context, 
                        "Письмо для подтверждения отправлено на ${event.email}", 
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
                else -> {}
            }
        }
    }
    
    // Close modal on successful submit and show success message
    LaunchedEffect(submitSuccess) {
        if (submitSuccess) {
            showWriteReviewModal = false
            reviewViewModel.resetSubmitSuccess()
            // Show success toast
            android.widget.Toast.makeText(
                context, 
                "Отзыв отправлен на модерацию", 
                android.widget.Toast.LENGTH_LONG
            ).show()
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
            // Photo gallery - EDGE TO EDGE (no horizontal padding like RN)
            PhotoGallery(
                images = attraction.images,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp), // Slightly reduced height like RN (200dp)
                onImageClick = { index ->
                    selectedPhotoIndex = index
                    showPhotoViewer = true
                },
                onFullscreenClick = {
                    selectedPhotoIndex = 0
                    showPhotoViewer = true
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Header row: Title on left, Share + Close on right (RN style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimensions.PaddingLarge),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Title (takes available space)
                Text(
                    text = attraction.name,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                // Action buttons: Share + Close
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = stringResource(R.string.detail_share),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.common_close),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Meta row: category text + rating (simple text, not chips like RN)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimensions.PaddingLarge),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Category as simple text
                Text(
                    text = attraction.category.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Rating (only if not null)
                attraction.averageRating?.let { rating ->
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
            
            // Distance (if available)
            distance?.let { dist ->
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.padding(horizontal = Dimensions.PaddingLarge),
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action buttons - context-aware (RN style)
            // From map: Route + Favorite
            // From list: Show on map + Route + Favorite
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimensions.PaddingLarge),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Show on map button (only when opened from list)
                if (openedFromList) {
                    OutlinedButton(
                        onClick = onShowOnMap,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Map,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "На карте",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                
                // Route button (primary)
                Button(
                    onClick = onBuildRoute,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Directions,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.detail_route),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                
                // Favorite button - icon only
                OutlinedButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .size(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (attraction.isFavorite) 
                            Color(0xFFE53935) 
                        else 
                            MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (attraction.isFavorite) 
                            Color(0xFFE53935).copy(alpha = 0.5f) 
                        else 
                            MaterialTheme.colorScheme.outline
                    ),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (attraction.isFavorite) 
                            Icons.Default.Favorite 
                        else 
                            Icons.Default.FavoriteBorder,
                        contentDescription = if (attraction.isFavorite) 
                            stringResource(R.string.cd_remove_from_favorites) 
                        else 
                            stringResource(R.string.cd_add_to_favorites),
                        modifier = Modifier.size(22.dp),
                        tint = if (attraction.isFavorite) 
                            Color(0xFFE53935) 
                        else 
                            MaterialTheme.colorScheme.onSurface
                    )
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
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                ),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Dimensions.PaddingLarge)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Info section - Modern design matching RN
            ModernAttractionInfo(
                attraction = attraction,
                modifier = Modifier.padding(horizontal = Dimensions.PaddingLarge)
            )
            
            // Amenities section
            if (attraction.amenities.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                ModernAmenitiesSection(
                    amenities = attraction.amenities
                )
            }
            
            // Tags section
            if (attraction.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                ModernTagsSection(
                    tags = attraction.tags
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = Dimensions.PaddingLarge),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            
            // Reviews Section (like RN UnifiedAttractionSheet)
            ReviewSection(
                attractionId = attraction.id,
                attractionName = attraction.name,
                averageRating = attraction.averageRating ?: 0f,
                totalReviews = attraction.reviewsCount ?: 0,
                reviews = reviews,
                userOwnReviews = userOwnReviews,
                sortBy = sortBy,
                onSortChange = { reviewViewModel.setSortBy(it) },
                onWriteReview = { rating ->
                    if (reviewViewModel.canWriteReview()) {
                        initialRating = rating
                        showWriteReviewModal = true
                    }
                },
                onLike = { reviewId -> 
                    android.util.Log.d("AttractionBottomSheet", "🔗 onLike lambda called: $reviewId")
                    reviewViewModel.reactToReview(reviewId, true) 
                },
                onDislike = { reviewId -> 
                    android.util.Log.d("AttractionBottomSheet", "🔗 onDislike lambda called: $reviewId")
                    reviewViewModel.reactToReview(reviewId, false) 
                },
                onShare = { reviewId -> /* TODO: Share review */ },
                loading = loading,
                isSyncing = isSyncing
            )
            
            Spacer(modifier = Modifier.height(32.dp))
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
            errorMessage = errorMessage,
            initialRating = initialRating
        )
        
        // Auth Modal for review
        AuthModal(
            visible = showAuthModal,
            onClose = { 
                showAuthModal = false
                authViewModel.clearError()
            },
            onSignIn = { email, password -> 
                authViewModel.signIn(email, password) 
            },
            onSignUp = { email, password, displayName -> 
                authViewModel.signUp(email, password, displayName) 
            },
            onForgotPassword = { email -> 
                authViewModel.resetPassword(email) 
            },
            isLoading = authUiState.isLoading,
            errorMessage = authUiState.error,
            onClearError = { authViewModel.clearError() }
        )
        
        // Photo Viewer Modal
        if (showPhotoViewer) {
            PhotoViewer(
                images = attraction.images,
                initialPage = selectedPhotoIndex,
                onDismiss = { showPhotoViewer = false },
                onShare = { photoUrl -> /* TODO: Share photo */ }
            )
        }
    }
}

fun formatDistance(distanceInMeters: Float): String {
    return when {
        distanceInMeters < 1000 -> "${distanceInMeters.toInt()} м"
        else -> "%.1f км".format(distanceInMeters / 1000)
    }
}
