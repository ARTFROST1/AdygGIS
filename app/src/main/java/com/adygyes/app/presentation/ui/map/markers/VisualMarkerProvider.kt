package com.adygyes.app.presentation.ui.map.markers

import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.adygyes.app.domain.model.Attraction
import com.adygyes.app.domain.model.AttractionCategory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.*
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import coil.request.ImageRequest
import com.adygyes.app.data.local.cache.ImageCacheManager
import timber.log.Timber
import kotlinx.coroutines.*

/**
 * Provides native MapKit markers with beautiful visual representation
 * These markers are perfectly synchronized with map coordinates
 */
class VisualMarkerProvider(
    private val mapView: MapView,
    private val imageCacheManager: ImageCacheManager,
    externalScope: CoroutineScope? = null // Allow passing lifecycle-aware scope
) {
    private val mapObjectCollection = mapView.map.mapObjects
    private val markers = mutableMapOf<String, PlacemarkMapObject>()
    private val markerImages = mutableMapOf<String, ImageProvider>()
    // Use provided lifecycle scope or fallback to custom scope
    private var coroutineScope = externalScope ?: CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Animation settings - optimized for maximum smoothness
    private var enableAppearAnimation = true
    private val animationDelayMs = 50L // Reduced delay for faster appearance
    private val animationDurationMs = 200L // Shorter duration for snappier animation
    private var markersPreloaded = false // Track if markers were created during preload
    private val preloadedImages = mutableMapOf<String, Bitmap>() // Cache loaded images
    
    /**
     * Set whether to enable appear animation for new markers
     */
    fun setAppearAnimation(enabled: Boolean) {
        enableAppearAnimation = enabled
    }
    
    /**
     * Preload markers during splash screen - creates markers with loaded images but keeps them invisible
     */
    fun preloadMarkers(attractions: List<Attraction>) {
        clearMarkers()
        
        coroutineScope.launch {
            // First, preload all images in parallel
            val imageLoadJobs = attractions.mapNotNull { attraction ->
                attraction.images.firstOrNull()?.let { imageUrl ->
                    async(Dispatchers.IO) {
                        try {
                            val bitmap = loadImageBitmap(imageUrl)
                            if (bitmap != null) {
                                preloadedImages[attraction.id] = bitmap
                                Timber.d("✅ Preloaded image for ${attraction.name}")
                            }
                            bitmap
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to preload image for ${attraction.name}")
                            null
                        }
                    }
                }
            }
            
            // Wait for all images to load
            imageLoadJobs.awaitAll()
            
            // Then create markers with preloaded images
            withContext(Dispatchers.Main) {
                attractions.forEach { attraction ->
                    addVisualMarkerWithPreloadedImage(attraction, visible = false)
                }
                
                markersPreloaded = true
                Timber.d("📍 Preloaded ${attractions.size} invisible markers with images for instant animation")
            }
        }
    }
    
    /**
     * Animate appearance of preloaded markers
     */
    fun animatePreloadedMarkers() {
        if (!markersPreloaded) {
            Timber.w("No preloaded markers to animate")
            return
        }
        
        coroutineScope.launch {
            val markerList = markers.values.toList()
            markerList.forEachIndexed { index, placemark ->
                launch {
                    // Stagger animation start
                    delay(index * animationDelayMs)
                    animateMarkerAppearance(placemark)
                }
            }
            Timber.d("🎬 Started animation for ${markerList.size} preloaded markers")
        }
    }
    
    /**
     * Show preloaded markers immediately without animation (fallback)
     */
    fun showPreloadedMarkers() {
        if (!markersPreloaded) {
            Timber.w("No preloaded markers to show")
            return
        }
        
        markers.values.forEach { placemark ->
            placemark.isVisible = true
        }
        Timber.d("👁️ Showed ${markers.size} preloaded markers immediately")
    }
    
    /**
     * Check if markers are preloaded and ready
     */
    fun hasPreloadedMarkers(): Boolean {
        return markersPreloaded && markers.isNotEmpty()
    }
    
    /**
     * Add visual markers for attractions with optional animation
     */
    fun addVisualMarkers(attractions: List<Attraction>) {
        clearMarkers()
        
        if (enableAppearAnimation) {
            addVisualMarkersWithAnimation(attractions)
        } else {
            attractions.forEach { attraction ->
                addVisualMarker(attraction, animated = false)
            }
        }
        
        Timber.d("📍 Added ${attractions.size} native visual markers to MapView: ${mapView.hashCode()}")
    }
    
    /**
     * Add visual markers with staggered animation
     */
    private fun addVisualMarkersWithAnimation(attractions: List<Attraction>) {
        coroutineScope.launch {
            attractions.forEachIndexed { index, attraction ->
                // Add marker with animation
                addVisualMarker(attraction, animated = true)
                
                // Stagger the appearance of each marker
                if (index < attractions.size - 1) {
                    delay(animationDelayMs)
                }
            }
            Timber.d("🎬 Animated appearance of ${attractions.size} markers completed")
        }
    }

    /**
     * Incrementally sync visual markers with provided attractions list without full clear-add.
     * Keeps existing markers, removes those that are no longer needed, and adds only new ones.
     */
    fun updateVisualMarkers(attractions: List<Attraction>) {
        val desiredIds = attractions.map { it.id }.toSet()

        Timber.d("🎯 FILTER: updateVisualMarkers called with ${attractions.size} attractions")
        Timber.d("🎯 FILTER: Current markers: ${markers.keys.joinToString(", ")}")
        Timber.d("🎯 FILTER: Desired markers: ${desiredIds.joinToString(", ")}")

        // Remove markers that are no longer present
        val toRemove = markers.keys.toSet() - desiredIds
        toRemove.forEach { id ->
            val removedMarker = markers.remove(id)?.let { placemark ->
                mapObjectCollection.remove(placemark)
                Timber.d("🗑️ FILTER: Removed marker with id: $id")
                true
            } ?: false
            if (!removedMarker) {
                Timber.w("⚠️ FILTER: Failed to remove marker with id: $id")
            }
        }

        // Add new markers that don't exist yet (without animation for filtering)
        val currentIds = markers.keys.toSet()
        attractions.forEach { attraction ->
            if (!currentIds.contains(attraction.id)) {
                addVisualMarker(attraction, animated = false)
                Timber.d("➕ FILTER: Added marker for: ${attraction.name} (id: ${attraction.id})")
            }
        }

        Timber.d("✅ FILTER COMPLETE: Synced native visual markers on MapView ${mapView.hashCode()}: now ${markers.size} (added ${desiredIds.size - currentIds.size}, removed ${toRemove.size})")
    }
    
    /**
     * Add a single visual marker with optional animation and visibility
     */
    private fun addVisualMarker(attraction: Attraction, animated: Boolean = false, visible: Boolean = true) {
        val point = Point(
            attraction.location.latitude,
            attraction.location.longitude
        )
        
        // Create or get cached image provider
        val imageProvider = getOrCreateImageProvider(attraction)
        
        // Create native placemark
        val placemark = mapObjectCollection.addPlacemark(point, imageProvider)
        
        // IMPORTANT: Make placemark non-interactive to not block Compose clicks
        placemark.zIndex = 0f // Ensure it's at the bottom
        
        // Store reference
        markers[attraction.id] = placemark
        
        // Set user data for debugging (not for click handling)
        placemark.userData = attraction
        
        if (animated) {
            // Start with invisible marker and animate appearance
            placemark.isVisible = false
            animateMarkerAppearance(placemark)
        } else {
            // Set visibility based on parameter
            placemark.isVisible = visible
        }
        
        Timber.d("✅ Created marker for ${attraction.name} at ${attraction.location.latitude}, ${attraction.location.longitude} on MapView: ${mapView.hashCode()}")
    }
    
    /**
     * Add visual marker with preloaded image for instant animation
     */
    private fun addVisualMarkerWithPreloadedImage(attraction: Attraction, visible: Boolean = true) {
        val point = Point(
            attraction.location.latitude,
            attraction.location.longitude
        )
        
        // Use preloaded image if available
        val preloadedBitmap = preloadedImages[attraction.id]
        val imageProvider = if (preloadedBitmap != null) {
            // Create image provider with preloaded bitmap
            val circularBitmap = createCircularBitmapWithImage(
                bitmap = preloadedBitmap,
                attraction = attraction,
                isSelected = false
            )
            ImageProvider.fromBitmap(circularBitmap)
        } else {
            // Fallback to default image provider
            getOrCreateImageProvider(attraction)
        }
        
        // Create native placemark
        val placemark = mapObjectCollection.addPlacemark(point, imageProvider)
        
        // IMPORTANT: Make placemark non-interactive to not block Compose clicks
        placemark.zIndex = 0f // Ensure it's at the bottom
        placemark.isVisible = visible
        
        // Store reference
        markers[attraction.id] = placemark
        
        // Set user data for debugging (not for click handling)
        placemark.userData = attraction
        
        Timber.d("✅ Created preloaded marker for ${attraction.name} with ${if (preloadedBitmap != null) "image" else "fallback"}")
    }
    
    /**
     * Animate marker appearance with scale-up effect using preloaded image for maximum smoothness
     */
    private fun animateMarkerAppearance(placemark: PlacemarkMapObject) {
        coroutineScope.launch {
            try {
                // Check if placemark is still valid before animation
                if (!placemark.isValid) {
                    Timber.w("Placemark is no longer valid, skipping animation")
                    return@launch
                }
                
                val attraction = placemark.userData as? Attraction ?: return@launch
                
                // Use preloaded image if available for instant animation
                val preloadedBitmap = preloadedImages[attraction.id]
                
                if (preloadedBitmap != null) {
                    // Animate with preloaded image - instant and smooth
                    animateWithPreloadedImage(placemark, attraction, preloadedBitmap)
                } else {
                    // Fallback: load image and animate
                    attraction.images.firstOrNull()?.let { imageUrl ->
                        try {
                            val loadedBitmap = loadImageBitmap(imageUrl)
                            if (loadedBitmap != null && placemark.isValid) {
                                animateWithLoadedImage(placemark, attraction, loadedBitmap)
                            } else {
                                // Immediate show as fallback if placemark is still valid
                                if (placemark.isValid) {
                                    placemark.isVisible = true
                                }
                            }
                        } catch (e: Exception) {
                            if (e is CancellationException) {
                                Timber.d("Animation cancelled for ${attraction.name}")
                                throw e // Re-throw cancellation to properly handle it
                            }
                            Timber.e(e, "Failed to load image for animation: ${attraction.name}")
                            if (placemark.isValid) {
                                placemark.isVisible = true
                            }
                        }
                    } ?: run {
                        // No image URL, show immediately if placemark is valid
                        if (placemark.isValid) {
                            placemark.isVisible = true
                        }
                    }
                }
                
                Timber.d("🎬 Completed appearance animation for ${attraction.name}")
            } catch (e: CancellationException) {
                Timber.d("Animation cancelled for placemark")
                throw e // Re-throw to properly handle cancellation
            } catch (e: Exception) {
                Timber.e(e, "Error during marker animation")
            }
        }
    }
    
    /**
     * Load image bitmap from cache or URL
     */
    private suspend fun loadImageBitmap(imageUrl: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val request = ImageRequest.Builder(mapView.context)
                .data(imageUrl)
                .crossfade(false)
                .allowHardware(false)
                .build()
            
            val result = imageCacheManager.imageLoader.execute(request)
            val drawable = result.drawable
            
            when {
                drawable is BitmapDrawable -> {
                    val originalBitmap = drawable.bitmap
                    if (originalBitmap.config == Bitmap.Config.HARDWARE) {
                        originalBitmap.copy(Bitmap.Config.ARGB_8888, false) ?: originalBitmap
                    } else {
                        originalBitmap
                    }
                }
                drawable != null -> {
                    val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 100
                    val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 100
                    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bmp)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                    bmp
                }
                else -> null
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load bitmap for animation")
            null
        }
    }
    
    /**
     * Animate marker with preloaded image - maximum smoothness
     */
    private suspend fun animateWithPreloadedImage(
        placemark: PlacemarkMapObject,
        attraction: Attraction,
        preloadedBitmap: Bitmap
    ) = withContext(Dispatchers.Main) {
        try {
            val steps = 12 // More steps for smoother animation
            val stepDelay = animationDurationMs / steps
            
            // Pre-create all animation frames for ultra-smooth playback
            val animationFrames = mutableListOf<ImageProvider>()
            for (i in 1..steps) {
                val scale = 0.2f + (0.8f * i / steps) // From 0.2 to 1.0
                val frame = createSmoothAnimatedMarkerWithImage(attraction, preloadedBitmap, scale)
                animationFrames.add(frame)
            }
            
            // Play animation frames with minimal delay
            animationFrames.forEach { frame ->
                // Check if placemark is still valid before each frame
                if (!placemark.isValid) {
                    Timber.w("Placemark became invalid during animation, stopping")
                    return@withContext
                }
                
                placemark.setIcon(frame)
                placemark.isVisible = true
                delay(stepDelay)
            }
            
            // Set final high-quality icon if placemark is still valid
            if (placemark.isValid) {
                val finalIcon = createCircularBitmapWithImage(
                    bitmap = preloadedBitmap,
                    attraction = attraction,
                    isSelected = false
                )
                placemark.setIcon(ImageProvider.fromBitmap(finalIcon))
            }
        } catch (e: CancellationException) {
            Timber.d("Preloaded image animation cancelled for ${attraction.name}")
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Error during preloaded image animation for ${attraction.name}")
        }
    }
    
    /**
     * Animate marker with loaded image at different scales (fallback)
     */
    private suspend fun animateWithLoadedImage(
        placemark: PlacemarkMapObject,
        attraction: Attraction,
        loadedBitmap: Bitmap
    ) = withContext(Dispatchers.Main) {
        try {
            val steps = 8
            val stepDelay = animationDurationMs / steps
            
            for (i in 1..steps) {
                // Check if placemark is still valid before each frame
                if (!placemark.isValid) {
                    Timber.w("Placemark became invalid during loaded image animation, stopping")
                    return@withContext
                }
                
                val scale = 0.3f + (0.7f * i / steps) // From 0.3 to 1.0
                val animatedIcon = createAnimatedMarkerWithImage(attraction, loadedBitmap, scale)
                placemark.setIcon(animatedIcon)
                placemark.isVisible = true
                delay(stepDelay)
            }
            
            // Set final normal icon with full-size image if placemark is still valid
            if (placemark.isValid) {
                val finalIcon = createCircularBitmapWithImage(
                    bitmap = loadedBitmap,
                    attraction = attraction,
                    isSelected = false
                )
                placemark.setIcon(ImageProvider.fromBitmap(finalIcon))
            }
        } catch (e: CancellationException) {
            Timber.d("Loaded image animation cancelled for ${attraction.name}")
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Error during loaded image animation for ${attraction.name}")
        }
    }
    
    /**
     * Create animated marker with loaded image at specific scale
     */
    private fun createAnimatedMarkerWithImage(
        attraction: Attraction,
        loadedBitmap: Bitmap,
        scale: Float
    ): ImageProvider {
        val baseSize = 52
        val size = (baseSize * scale).toInt().coerceAtLeast(8) // Minimum 8dp
        val sizePx = (size * mapView.context.resources.displayMetrics.density).toInt()
        val borderWidth = 2
        val borderWidthPx = (borderWidth * mapView.context.resources.displayMetrics.density).toInt()
        
        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        
        val paint = Paint().apply {
            isAntiAlias = true
            alpha = (255 * scale).toInt().coerceIn(100, 255) // Fade in effect
        }
        
        val centerX = sizePx / 2f
        val centerY = sizePx / 2f
        val radius = (sizePx - borderWidthPx) / 2f - 4f
        
        // Draw shadow
        paint.color = Color.argb((50 * scale).toInt().coerceIn(20, 50), 0, 0, 0)
        paint.maskFilter = BlurMaskFilter(8f * scale, BlurMaskFilter.Blur.NORMAL)
        canvas.drawCircle(centerX + 2, centerY + 2, radius, paint)
        paint.maskFilter = null
        
        // Create circular clip path
        val path = android.graphics.Path().apply {
            addCircle(centerX, centerY, radius, android.graphics.Path.Direction.CW)
        }
        canvas.clipPath(path)
        
        // Draw scaled image with fade effect
        try {
            val safeBitmap = if (loadedBitmap.config == Bitmap.Config.HARDWARE) {
                loadedBitmap.copy(Bitmap.Config.ARGB_8888, false) ?: loadedBitmap
            } else {
                loadedBitmap
            }
            val scaledBitmap = Bitmap.createScaledBitmap(safeBitmap, sizePx, sizePx, true)
            paint.alpha = (255 * scale).toInt().coerceIn(100, 255)
            canvas.drawBitmap(scaledBitmap, 0f, 0f, paint)
            scaledBitmap.recycle()
        } catch (e: Exception) {
            Timber.e(e, "Failed to draw animated image for marker ${attraction.name}")
            // Fallback to category color
            paint.color = getCategoryColor(attraction.category)
            paint.alpha = (255 * scale).toInt().coerceIn(100, 255)
            canvas.drawCircle(centerX, centerY, radius, paint)
        }
        
        // Draw border
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = borderWidthPx.toFloat()
        paint.color = Color.WHITE
        paint.alpha = (255 * scale).toInt().coerceIn(100, 255)
        canvas.drawCircle(centerX, centerY, radius, paint)
        
        return ImageProvider.fromBitmap(output)
    }
    
    /**
     * Create smooth animated marker with optimized rendering for preloaded images
     */
    private fun createSmoothAnimatedMarkerWithImage(
        attraction: Attraction,
        preloadedBitmap: Bitmap,
        scale: Float
    ): ImageProvider {
        val baseSize = 52
        val size = (baseSize * scale).toInt().coerceAtLeast(8)
        val sizePx = (size * mapView.context.resources.displayMetrics.density).toInt()
        val borderWidth = 2
        val borderWidthPx = (borderWidth * mapView.context.resources.displayMetrics.density).toInt()
        
        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        
        // Optimized paint with anti-aliasing
        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true // Better quality scaling
            isDither = true // Smoother gradients
        }
        
        val centerX = sizePx / 2f
        val centerY = sizePx / 2f
        val radius = (sizePx - borderWidthPx) / 2f - 4f
        
        // Smooth shadow with scale-based intensity
        val shadowAlpha = (30 * scale).toInt().coerceIn(10, 30)
        paint.color = Color.argb(shadowAlpha, 0, 0, 0)
        paint.maskFilter = BlurMaskFilter(6f * scale, BlurMaskFilter.Blur.NORMAL)
        canvas.drawCircle(centerX + 1.5f, centerY + 1.5f, radius, paint)
        paint.maskFilter = null
        
        // Create smooth circular clip
        val path = android.graphics.Path().apply {
            addCircle(centerX, centerY, radius, android.graphics.Path.Direction.CW)
        }
        canvas.clipPath(path)
        
        // Draw scaled image with smooth interpolation
        try {
            val scaledBitmap = Bitmap.createScaledBitmap(preloadedBitmap, sizePx, sizePx, true)
            
            // Smooth fade-in effect
            val alpha = (255 * scale * scale).toInt().coerceIn(100, 255) // Quadratic for smoother fade
            paint.alpha = alpha
            canvas.drawBitmap(scaledBitmap, 0f, 0f, paint)
            scaledBitmap.recycle()
        } catch (e: Exception) {
            Timber.e(e, "Failed to draw smooth animated image for marker ${attraction.name}")
            // Fallback to category color
            paint.color = getCategoryColor(attraction.category)
            paint.alpha = (255 * scale).toInt().coerceIn(100, 255)
            canvas.drawCircle(centerX, centerY, radius, paint)
        }
        
        // Smooth border with scale-based opacity
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = borderWidthPx.toFloat()
        paint.color = Color.WHITE
        paint.alpha = (255 * scale).toInt().coerceIn(150, 255)
        canvas.drawCircle(centerX, centerY, radius, paint)
        
        return ImageProvider.fromBitmap(output)
    }
    
    /**
     * Get category color for marker background
     */
    private fun getCategoryColor(category: AttractionCategory): Int {
        return when (category) {
            AttractionCategory.NATURE -> Color.rgb(76, 175, 80)  // Green
            AttractionCategory.HISTORY -> Color.rgb(121, 85, 72)  // Brown
            AttractionCategory.CULTURE -> Color.rgb(156, 39, 176) // Purple
            AttractionCategory.ENTERTAINMENT -> Color.rgb(255, 152, 0) // Orange
            AttractionCategory.RECREATION -> Color.rgb(33, 150, 243) // Blue
            AttractionCategory.GASTRONOMY -> Color.rgb(255, 193, 7) // Amber
            AttractionCategory.RELIGIOUS -> Color.rgb(96, 125, 139) // Blue Grey
            AttractionCategory.ADVENTURE -> Color.rgb(255, 87, 34) // Deep Orange
        }
    }
    
    /**
     * Update selected marker appearance
     */
    fun updateSelectedMarker(selectedAttraction: Attraction?) {
        markers.forEach { (id, marker) ->
            val isSelected = id == selectedAttraction?.id
            val attraction = marker.userData as? Attraction
            
            if (attraction != null) {
                // Update marker image based on selection state
                val imageProvider = getOrCreateImageProvider(
                    attraction = attraction,
                    isSelected = isSelected
                )
                marker.setIcon(imageProvider)
            }
        }
    }
    
    /**
     * Create or retrieve cached image provider for marker
     */
    private fun getOrCreateImageProvider(
        attraction: Attraction,
        isSelected: Boolean = false
    ): ImageProvider {
        val cacheKey = "${attraction.id}_${if (isSelected) "selected" else "normal"}"
        
        return markerImages.getOrPut(cacheKey) {
            createCircularMarkerImage(
                attraction = attraction,
                isSelected = isSelected
            )
        }
    }
    
    /**
     * Create a circular marker image with attraction photo or category icon
     */
    private fun createCircularMarkerImage(
        attraction: Attraction,
        isSelected: Boolean = false
    ): ImageProvider {
        val size = if (isSelected) 60 else 52 // dp converted to pixels
        val sizePx = (size * mapView.context.resources.displayMetrics.density).toInt()
        val borderWidth = if (isSelected) 3 else 2
        val borderWidthPx = (borderWidth * mapView.context.resources.displayMetrics.density).toInt()
        
        // Create bitmap
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Paint for border
        val borderPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = borderWidthPx.toFloat()
        }
        
        // Paint for shadow
        val shadowPaint = Paint().apply {
            isAntiAlias = true
            color = Color.argb(50, 0, 0, 0)
            maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
        }
        
        val centerX = sizePx / 2f
        val centerY = sizePx / 2f
        val radius = (sizePx - borderWidthPx) / 2f - 4f // Account for shadow
        
        // Draw shadow
        canvas.drawCircle(centerX + 2, centerY + 2, radius, shadowPaint)
        // Do NOT draw background fill or emoji — keep fully transparent until image loads
        
        // Draw border
        canvas.drawCircle(centerX, centerY, radius, borderPaint)
        
        // Load actual image asynchronously if available
        attraction.images.firstOrNull()?.let { imageUrl ->
            coroutineScope.launch {
                loadAndUpdateMarkerImage(attraction, imageUrl, isSelected)
            }
        }
        
        return ImageProvider.fromBitmap(bitmap)
    }
    
    // No category emoji/content drawing — markers remain transparent until an image is loaded
    
    /**
     * Load image from URL and update marker
     */
    private suspend fun loadAndUpdateMarkerImage(
        attraction: Attraction,
        imageUrl: String,
        isSelected: Boolean
    ) {
        withContext(Dispatchers.IO) {
            try {
                // Load image using Coil with caching
                // IMPORTANT: Disable hardware bitmaps for Canvas operations
                val request = ImageRequest.Builder(mapView.context)
                    .data(imageUrl)
                    .crossfade(false)
                    .allowHardware(false) // Critical: Canvas cannot draw hardware bitmaps
                    .build()
                
                val result = imageCacheManager.imageLoader.execute(request)
                val drawable = result.drawable
                
                // Convert drawable to bitmap
                val bitmap = when {
                    drawable is BitmapDrawable -> {
                        val originalBitmap = drawable.bitmap
                        // Check if it's a hardware bitmap and convert if needed
                        if (originalBitmap.config == Bitmap.Config.HARDWARE) {
                            // Convert hardware bitmap to software bitmap
                            val softwareBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, false)
                            softwareBitmap ?: originalBitmap // Use original if copy fails
                        } else {
                            originalBitmap
                        }
                    }
                    drawable != null -> {
                        // Create bitmap from drawable
                        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 100
                        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 100
                        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bmp)
                        drawable.setBounds(0, 0, canvas.width, canvas.height)
                        drawable.draw(canvas)
                        bmp
                    }
                    else -> {
                        // Create placeholder bitmap if drawable is null
                        Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
                    }
                }
                
                // Check if coroutine is still active before updating UI
                if (!isActive) {
                    Timber.d("⚠️ Skipping marker update for ${attraction.name} - coroutine cancelled")
                    return@withContext
                }
                
                withContext(Dispatchers.Main) {
                    // Create circular image with loaded bitmap
                    val circularBitmap = createCircularBitmapWithImage(
                        bitmap = bitmap,
                        attraction = attraction,
                        isSelected = isSelected
                    )
                    
                    // Log cache status
                    val isCached = imageCacheManager.isImageCached(imageUrl)
                    if (isCached) {
                        Timber.d("✅ Loaded marker image from cache for ${attraction.name}")
                    } else {
                        Timber.d("📥 Downloaded marker image for ${attraction.name}")
                    }
                    
                    // Update marker only if it still exists
                    markers[attraction.id]?.setIcon(ImageProvider.fromBitmap(circularBitmap))
                    
                    // Update cache
                    val cacheKey = "${attraction.id}_${if (isSelected) "selected" else "normal"}"
                    markerImages[cacheKey] = ImageProvider.fromBitmap(circularBitmap)
                }
            } catch (e: CancellationException) {
                // Coroutine was cancelled - this is expected, don't log as error
                Timber.d("Marker image loading cancelled for ${attraction.name}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to load marker image for ${attraction.name}")
            }
        }
    }
    
    /**
     * Create circular bitmap with loaded image
     */
    private fun createCircularBitmapWithImage(
        bitmap: Bitmap,
        attraction: Attraction,
        isSelected: Boolean
    ): Bitmap {
        val size = if (isSelected) 60 else 52
        val sizePx = (size * mapView.context.resources.displayMetrics.density).toInt()
        val borderWidth = if (isSelected) 3 else 2
        val borderWidthPx = (borderWidth * mapView.context.resources.displayMetrics.density).toInt()
        
        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        
        val paint = Paint().apply {
            isAntiAlias = true
            alpha = 255 // Full opacity
        }
        
        val centerX = sizePx / 2f
        val centerY = sizePx / 2f
        val radius = (sizePx - borderWidthPx) / 2f - 4f
        
        // Draw shadow
        paint.color = Color.argb(50, 0, 0, 0)
        paint.maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
        canvas.drawCircle(centerX + 2, centerY + 2, radius, paint)
        paint.maskFilter = null
        
        // Create circular clip path
        val path = android.graphics.Path().apply {
            addCircle(centerX, centerY, radius, android.graphics.Path.Direction.CW)
        }
        canvas.clipPath(path)
        
        // Draw scaled image with full opacity
        try {
            // Ensure we're not working with a hardware bitmap
            val safeBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
                bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: bitmap
            } else {
                bitmap
            }
            val scaledBitmap = Bitmap.createScaledBitmap(safeBitmap, sizePx, sizePx, true)
            paint.alpha = 255 // Ensure full opacity
            canvas.drawBitmap(scaledBitmap, 0f, 0f, paint)
            scaledBitmap.recycle() // Clean up scaled bitmap
        } catch (e: Exception) {
            // If image drawing fails, do not draw any colored fallback — keep transparent
            Timber.e(e, "Failed to draw image for marker ${attraction.name}")
        }
        
        // Draw border
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = borderWidthPx.toFloat()
        paint.color = Color.WHITE
        canvas.drawCircle(centerX, centerY, radius, paint)
        
        return output
    }
    
    // Removed category color/emoji helpers — no visual fallback is used anymore
    
    /**
     * Clear all markers from the map
     */
    fun clearMarkers() {
        Timber.d("🧹 Clearing all markers and cancelling coroutines")
        
        // Cancel all active coroutines first
        coroutineScope.cancel()
        
        // Clear all collections
        markers.clear()
        markerImages.clear()
        preloadedImages.clear() // Clear preloaded image cache
        markersPreloaded = false
        
        // Clear map objects
        try {
            mapObjectCollection.clear()
        } catch (e: Exception) {
            Timber.w(e, "Error clearing map objects (may be already cleared)")
        }
        
        // Create new coroutine scope for future operations
        coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        
        Timber.d("✅ Cleared ${markers.size} markers and reset coroutine scope")
    }
    
    /**
     * Force clear everything when data version changes
     */
    fun forceReset() {
        Timber.d("🔄 Force resetting VisualMarkerProvider due to data version change")
        
        // Cancel all coroutines immediately
        coroutineScope.cancel()
        
        // Wait a bit for cancellation to complete
        runBlocking {
            delay(100)
        }
        
        // Clear everything
        markers.clear()
        markerImages.clear()
        preloadedImages.clear()
        markersPreloaded = false
        
        // Clear map objects safely
        try {
            mapObjectCollection.clear()
        } catch (e: Exception) {
            Timber.w(e, "Error clearing map objects during force reset")
        }
        
        // Create fresh coroutine scope
        coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        
        Timber.d("✅ Force reset completed - ready for new data")
    }
}
