package com.adygyes.app.presentation.ui.screens.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Locale
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adygyes.app.R
import com.adygyes.app.domain.model.Attraction
import com.adygyes.app.domain.model.ReviewSortOption
import com.adygyes.app.presentation.theme.Dimensions
import com.adygyes.app.presentation.ui.components.*
import com.adygyes.app.presentation.ui.components.auth.AuthModal
import com.adygyes.app.presentation.viewmodel.PasswordStrength
import com.adygyes.app.presentation.ui.components.reviews.ReviewSection
import com.adygyes.app.presentation.ui.components.reviews.WriteReviewModal
import com.adygyes.app.presentation.viewmodel.AttractionDetailViewModel
import com.adygyes.app.presentation.viewmodel.AuthEvent
import com.adygyes.app.presentation.viewmodel.AuthViewModel
import com.adygyes.app.presentation.viewmodel.ReviewViewModel
import timber.log.Timber

/**
 * Detailed attraction information screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttractionDetailScreen(
    attractionId: String,
    onBackClick: () -> Unit,
    onBuildRoute: () -> Unit,
    onShareClick: () -> Unit,
    onShowOnMap: (() -> Unit)? = null,
    viewModel: AttractionDetailViewModel = hiltViewModel(),
    reviewViewModel: ReviewViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showPhotoViewer by remember { mutableStateOf(false) }
    var selectedPhotoIndex by remember { mutableIntStateOf(0) }
    var showWriteReviewModal by remember { mutableStateOf(false) }
    var initialRating by remember { mutableIntStateOf(0) }
    var showAuthModal by remember { mutableStateOf(false) }
    
    // Review states
    val reviews by reviewViewModel.reviews.collectAsStateWithLifecycle()
    val userOwnReviews by reviewViewModel.userOwnReviews.collectAsStateWithLifecycle()
    val reviewsLoading by reviewViewModel.loading.collectAsStateWithLifecycle()
    val reviewsSyncing by reviewViewModel.isSyncing.collectAsStateWithLifecycle()
    val reviewSortBy by reviewViewModel.sortBy.collectAsStateWithLifecycle()
    val submitting by reviewViewModel.submitting.collectAsStateWithLifecycle()
    val errorMessage by reviewViewModel.error.collectAsStateWithLifecycle()
    val submitSuccess by reviewViewModel.submitSuccess.collectAsStateWithLifecycle()
    val showAuthRequired by reviewViewModel.showAuthRequired.collectAsStateWithLifecycle()
    
    // Auth states
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val isAuthenticated by reviewViewModel.isAuthenticated.collectAsStateWithLifecycle()
    
    val context = LocalContext.current
    
    // Show auth modal when auth is required for review
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
                is AuthEvent.SignInSuccess -> {
                    showAuthModal = false
                    // User is now authenticated, show write review modal
                    showWriteReviewModal = true
                }
                is AuthEvent.SignUpSuccess -> {
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
    
    LaunchedEffect(attractionId) {
        viewModel.loadAttraction(attractionId)
        reviewViewModel.loadReviews(attractionId)
    }
    
    // Reload user's own reviews when auth state changes to authenticated
    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            Timber.d("User authenticated in detail screen, reloading reviews for $attractionId")
            reviewViewModel.loadReviews(attractionId, forceRefresh = true)
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
    
    // Handle back gesture - close photo viewer if open, otherwise navigate back
    BackHandler(enabled = true) {
        if (showPhotoViewer) {
            showPhotoViewer = false
        } else {
            onBackClick()
        }
    }
    
    when (val state = uiState) {
        is AttractionDetailViewModel.UiState.Loading -> {
            LoadingIndicator()
        }
        
        is AttractionDetailViewModel.UiState.Error -> {
            ErrorState(
                message = state.message,
                onRetry = { viewModel.loadAttraction(attractionId) }
            )
        }
        
        is AttractionDetailViewModel.UiState.Success -> {
            val attraction = state.attraction
            
            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    TopAppBar(
                        title = { },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = stringResource(R.string.cd_back)
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = onShareClick) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = stringResource(R.string.cd_share)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.toggleFavorite() }
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
                                    tint = if (attraction.isFavorite) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                },
                bottomBar = {
                    // Bottom action bar
                    Surface(
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Dimensions.PaddingLarge),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Show on Map button (only in list mode)
                            if (onShowOnMap != null) {
                                OutlinedButton(
                                    onClick = onShowOnMap,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Map,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.detail_show_on_map))
                                }
                            }
                            
                            // Get Directions button
                            Button(
                                onClick = onBuildRoute,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Directions,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.detail_get_directions))
                            }
                        }
                    }
                }
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(bottom = 40.dp)
                ) {
                    // Photo Gallery
                    item {
                        PhotoGallery(
                            images = attraction.images,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .padding(bottom = 4.dp),
                            onImageClick = { index ->
                                selectedPhotoIndex = index
                                showPhotoViewer = true
                            },
                            onFullscreenClick = {
                                selectedPhotoIndex = 0
                                showPhotoViewer = true
                            }
                        )
                    }
                    
                    // Main content
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Dimensions.PaddingLarge)
                        ) {
                            // Title and category
                            Text(
                                text = attraction.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                CategoryChip(category = attraction.category)
                                attraction.averageRating?.let { rating ->
                                    RatingBar(
                                        rating = rating,
                                        totalReviews = state.reviewCount
                                    )
                                }
                            }
                            
                            // Description
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.detail_about),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = attraction.description,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Information section - Modern design matching RN and bottom sheet
                    item {
                        Column(
                            modifier = Modifier.padding(horizontal = Dimensions.PaddingLarge),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Spacer(modifier = Modifier.height(8.dp))
                            ModernAttractionInfo(attraction = attraction)
                        }
                    }
                    
                    // Amenities - Modern design
                    if (attraction.amenities.isNotEmpty()) {
                        item {
                            ModernAmenitiesSection(
                                amenities = attraction.amenities,
                                modifier = Modifier.padding(
                                    horizontal = Dimensions.PaddingLarge,
                                    vertical = Dimensions.PaddingMedium
                                )
                            )
                        }
                    }
                    
                    // Tags - Modern design
                    if (attraction.tags.isNotEmpty()) {
                        item {
                            ModernTagsSection(
                                tags = attraction.tags,
                                modifier = Modifier.padding(
                                    horizontal = Dimensions.PaddingLarge,
                                    vertical = Dimensions.PaddingMedium
                                )
                            )
                        }
                    }
                    
                    // Reviews Section
                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = Dimensions.PaddingMedium),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        
                        ReviewSection(
                            attractionId = attraction.id,
                            attractionName = attraction.name,
                            averageRating = attraction.averageRating ?: 0f,
                            totalReviews = state.reviewCount,
                            reviews = reviews,
                            userOwnReviews = userOwnReviews,
                            sortBy = reviewSortBy,
                            onSortChange = { reviewViewModel.setSortBy(it) },
                            onWriteReview = { rating ->
                                // Check if user can write review (auth check)
                                if (reviewViewModel.canWriteReview()) {
                                    initialRating = rating
                                    showWriteReviewModal = true
                                }
                                // If not authenticated, showAuthRequired will trigger AuthModal
                            },
                            onLike = { reviewId ->
                                android.util.Log.d("AttractionDetailScreen", "🔗 onLike lambda called: $reviewId")
                                reviewViewModel.reactToReview(reviewId, true)
                            },
                            onDislike = { reviewId ->
                                android.util.Log.d("AttractionDetailScreen", "🔗 onDislike lambda called: $reviewId")
                                reviewViewModel.reactToReview(reviewId, false)
                            },
                            onShare = { /* TODO: Share review */ },
                            loading = reviewsLoading,
                            isSyncing = reviewsSyncing
                        )
                    }
                }
            }
            
            // Photo viewer dialog
            if (showPhotoViewer) {
                PhotoViewer(
                    images = attraction.images,
                    initialPage = selectedPhotoIndex,
                    onDismiss = { showPhotoViewer = false },
                    onShare = { imageUrl ->
                        // Handle image sharing
                        onShareClick()
                    }
                )
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
            var authPasswordStrength by remember { mutableStateOf(PasswordStrength.NONE) }
            
            AuthModal(
                visible = showAuthModal,
                onClose = { 
                    showAuthModal = false
                    authViewModel.clearError()
                    authPasswordStrength = PasswordStrength.NONE
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
                onClearError = { authViewModel.clearError() },
                passwordStrength = authPasswordStrength,
                onPasswordChange = { password ->
                    authPasswordStrength = authViewModel.calculatePasswordStrength(password)
                }
            )
        }
    }
}

@Composable
private fun InfoCard(
    icon: ImageVector,
    title: String,
    content: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.PaddingMedium)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                content()
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AmenitiesGrid(
    amenities: List<String>,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        amenities.forEach { amenity ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFFB300)
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = 12.dp,
                        vertical = 8.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = getAmenityIcon(amenity),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.Black
                    )
                    Text(
                        text = amenity.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color.Black
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsFlow(
    tags: List<String>,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        tags.forEach { tag ->
            AssistChip(
                onClick = { },
                label = {
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Tag,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}

private fun getAmenityIcon(amenity: String): ImageVector {
    return when (amenity.lowercase()) {
        "parking", "парковка" -> Icons.Default.LocalParking
        "wifi", "wi-fi" -> Icons.Default.Wifi
        "restaurant", "ресторан", "кафе", "cafe" -> Icons.Default.Restaurant
        "toilet", "туалет", "restroom" -> Icons.Default.Wc
        "shop", "магазин", "store" -> Icons.Default.ShoppingCart
        "guide", "гид", "экскурсия" -> Icons.Default.Person
        "photo", "фото", "photography" -> Icons.Default.PhotoCamera
        "disabled", "инвалиды", "accessibility" -> Icons.Default.Accessible
        else -> Icons.Default.CheckCircle
    }
}