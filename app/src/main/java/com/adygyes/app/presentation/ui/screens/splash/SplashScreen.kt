package com.adygyes.app.presentation.ui.screens.splash

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adygyes.app.R
import com.adygyes.app.presentation.ui.screens.map.LocalMapHostController
import kotlinx.coroutines.delay

/**
 * Splash screen with background image and styled button
 */
@Composable
fun SplashScreen(
    onNavigateToMain: () -> Unit
) {
    // Get preload manager from MapHost
    val preloadManager = LocalMapHostController.current?.preloadManager
    val preloadState = preloadManager?.preloadState?.collectAsStateWithLifecycle()
    
    // Track if button was clicked and loading is complete
    var navigateWhenReady by remember { mutableStateOf(false) }
    
    // Auto-navigate when preload is complete and button was clicked
    LaunchedEffect(preloadState?.value?.allMarkersReady, navigateWhenReady) {
        if (navigateWhenReady && preloadState?.value?.allMarkersReady == true) {
            // Small delay for smooth transition
            delay(100)
            onNavigateToMain()
        }
    }
    
    // Safety net:
    // Splash is shown before MapHost/Map init is composed in many navigation flows.
    // In that case preloadManager/preloadState is null and we'd be stuck forever.
    // Also, if preloading fails/hangs, don't block entry indefinitely.
    var allowProceedFallback by remember { mutableStateOf(false) }

    LaunchedEffect(preloadManager) {
        // If preloader isn't available on Splash, allow proceed shortly.
        if (preloadManager == null) {
            delay(600)
            allowProceedFallback = true
        } else {
            allowProceedFallback = false
        }
    }

    LaunchedEffect(Unit) {
        // Hard timeout in case preloading never reaches allMarkersReady.
        delay(10_000)
        allowProceedFallback = true
    }

    // Check if everything is fully loaded
    val isFullyLoaded = allowProceedFallback || preloadState?.value?.allMarkersReady == true
    // Use system default font family for compatibility
    val ralewayFontFamily = FontFamily.Default
    
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Background image
        Image(
            painter = painterResource(id = R.drawable.dombay_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
        
        // Content overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Centered title/subtitle block
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Main title - Black 54px
                    Text(
                        text = "AdygGis",
                        fontSize = 54.sp,
                        fontFamily = ralewayFontFamily,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFF6CA5F),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Subtitle - ExtraBold 22px
                    Text(
                        text = "Шъукъеблагъ !",
                        fontSize = 22.sp,
                        fontFamily = ralewayFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Bottom action and caption
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Preload progress indicator (visible while loading)
                val progress = preloadState?.value?.progress ?: 0f
                val isLoading = !isFullyLoaded && (preloadState?.value?.isLoading ?: true)
                
                AnimatedVisibility(
                    visible = isLoading && !navigateWhenReady,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier.padding(bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier
                                .width(194.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color(0xFFF6CA5F),
                            trackColor = Color(0xFF0C5329).copy(alpha = 0.3f)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = when {
                                progress < 0.3f -> "Загружаем данные..."
                                progress < 0.5f -> "Создаём маркеры на карте..."
                                progress < 0.8f -> "Загружаем изображения..."
                                progress < 1.0f -> "Финальная подготовка..."
                                else -> "Всё готово!"
                            },
                            fontSize = 12.sp,
                            fontFamily = ralewayFontFamily,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
                
                // Start button with airplane icon
                Row(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 194.dp, minHeight = 40.dp)
                        .alpha(if (!isFullyLoaded) 0.5f else 1f)
                        .background(
                            color = if (isFullyLoaded) 
                                Color(0xFF0C5329) 
                            else 
                                Color(0xFF0C5329).copy(alpha = 0.7f),
                            shape = RoundedCornerShape(25.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isFullyLoaded) Color(0xFFF6CA5F) else Color(0xFFF6CA5F).copy(alpha = 0.5f),
                            shape = RoundedCornerShape(25.dp)
                        )
                        .clip(RoundedCornerShape(25.dp))
                        .clickable(enabled = isFullyLoaded) { 
                            // Only allow click when fully loaded
                            if (isFullyLoaded) {
                                onNavigateToMain()
                            }
                        }
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isFullyLoaded) {
                        // Show loading spinner when not ready
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color(0xFFF6CA5F).copy(alpha = 0.7f),
                            strokeWidth = 2.dp
                        )
                    } else {
                        // Airplane icon when ready
                        Icon(
                            painter = painterResource(id = R.drawable.ic_airplane),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFFF6CA5F)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(10.dp))
                    
                    // Button text - Semibold 16px
                    Text(
                        text = if (!isFullyLoaded) 
                            "Подготавливаем карту..." 
                        else 
                            "В путешествие",
                        fontSize = 16.sp,
                        fontFamily = ralewayFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isFullyLoaded) Color(0xFFF6CA5F) else Color(0xFFF6CA5F).copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Bottom text - slightly larger
                Text(
                    text = "Created by FrostMoon Tech",
                    fontSize = 12.sp,
                    fontFamily = ralewayFontFamily,
                    fontWeight = FontWeight.Normal,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        // Блокировщик кликов на логотип Яндекс Карт (правый нижний угол)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .width(110.dp) // Ширина для покрытия логотипа "Яндекс"
                .height(30.dp) // Уменьшенная высота
                .background(
                    color = Color.Transparent, // Полностью прозрачный
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    // Поглощаем клик, ничего не делаем
                }
                .zIndex(2000f) // Поверх всех элементов
        )
    }
}
