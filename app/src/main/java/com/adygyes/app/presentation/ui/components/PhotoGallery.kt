package com.adygyes.app.presentation.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.CachePolicy
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

/**
 * Swipeable photo gallery component
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoGallery(
    images: List<String>,
    modifier: Modifier = Modifier,
    initialPage: Int = 0,
    showIndicators: Boolean = true,
    aspectRatio: Float = 16f / 9f,
    onImageClick: ((Int) -> Unit)? = null,
    onFullscreenClick: (() -> Unit)? = null
) {
    if (images.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No images available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { images.size }
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            // Lazy loading: only load images when visible
            // First image is already preloaded, others load on-demand
            val imageUrl = images[page]
            val isFirstImage = page == 0
            
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    // Use cache for all images
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    // Placeholder while loading
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .build(),
                contentDescription = "Photo ${page + 1}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onImageClick?.invoke(page) },
                onSuccess = {
                    // Log cache status for debugging
                    if (!isFirstImage) {
                        timber.log.Timber.d("🖼️ Loaded photo ${page + 1} for gallery")
                    }
                }
            )
        }
        
        // Page indicators
        if (showIndicators && images.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(images.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (pagerState.currentPage == index) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (pagerState.currentPage == index)
                                    Color.White
                                else
                                    Color.White.copy(alpha = 0.5f)
                            )
                    )
                }
            }
        }
        
        // Image counter (moved to top left)
        if (images.size > 1) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "${pagerState.currentPage + 1}/${images.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        
        // Fullscreen button (in top right corner)
        onFullscreenClick?.let { fullscreenClick ->
            var isPressed by remember { mutableStateOf(false) }
            val coroutineScope = rememberCoroutineScope()
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.9f else 1f,
                animationSpec = tween(durationMillis = 100),
                label = "fullscreen_button_scale"
            )
            
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                color = Color.Black.copy(alpha = 0.6f),
                shape = CircleShape
            ) {
                IconButton(
                    onClick = {
                        isPressed = true
                        // Reset animation state after a short delay
                        coroutineScope.launch {
                            delay(100)
                            isPressed = false
                        }
                        fullscreenClick()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Fullscreen",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Full-screen photo viewer with zoom support
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoViewer(
    images: List<String>,
    initialPage: Int = 0,
    onDismiss: () -> Unit,
    onShare: ((String) -> Unit)? = null
) {
    // Note: System bars hiding is deprecated, using manual window insets instead
    // Users can swipe to show system bars as needed
    
    DisposableEffect(Unit) {
        onDispose {
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
            val pagerState = rememberPagerState(
                initialPage = initialPage,
                pageCount = { images.size }
            )
            
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                ZoomableImage(
                    imageUrl = images[page],
                    contentDescription = "Photo ${page + 1}"
                )
            }
            
            // Top bar with gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.7f),
                                Color.Transparent
                            )
                        )
                    )
            )
            
            // Top navigation bar (similar to AttractionDetailScreen)
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Close button
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
                
                // Share button
                onShare?.let { share ->
                    IconButton(
                        onClick = { share(images[pagerState.currentPage]) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color.White
                        )
                    }
                } ?: Spacer(modifier = Modifier.width(48.dp))
            }
            
            // Page counter
            if (images.size > 1) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${images.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

/**
 * Zoomable image component
 */
@Composable
private fun ZoomableImage(
    imageUrl: String,
    contentDescription: String?
) {
    val zoomState = rememberZoomState()
    
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxSize()
            .zoomable(zoomState)
    )
}

/**
 * Thumbnail grid for multiple images
 */
@Composable
fun PhotoThumbnailGrid(
    images: List<String>,
    onImageClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    maxVisible: Int = 4
) {
    val displayImages = images.take(maxVisible)
    val remainingCount = (images.size - maxVisible).coerceAtLeast(0)
    
    Row(
        modifier = modifier.height(80.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        displayImages.forEachIndexed { index, imageUrl ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Thumbnail ${index + 1}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onImageClick(index) }
                )
                
                // Overlay for last image if there are more
                if (index == maxVisible - 1 && remainingCount > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Color.Black.copy(alpha = 0.6f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onImageClick(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+$remainingCount",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
