package com.adygyes.app.presentation.ui.screens.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adygyes.app.R
import com.adygyes.app.presentation.theme.Dimensions
import com.adygyes.app.presentation.ui.components.HapticFeedback
import com.adygyes.app.presentation.ui.components.rememberHapticFeedback
import kotlinx.coroutines.launch

/**
 * Onboarding screen for first-time users
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit
) {
    val haptic = rememberHapticFeedback()
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val scope = rememberCoroutineScope()
    
    val isLastPage = pagerState.currentPage == onboardingPages.size - 1
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Background gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )
        
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Skip button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimensions.PaddingLarge),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        haptic.light()
                        onOnboardingComplete()
                    }
                ) {
                    Text("Пропустить")
                }
            }
            
            // Pager content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPage(
                    page = onboardingPages[page],
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Page indicators
            Row(
                modifier = Modifier.padding(Dimensions.PaddingMedium),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(onboardingPages.size) { index ->
                    val isSelected = index == pagerState.currentPage
                    val animatedWidth by animateFloatAsState(
                        targetValue = if (isSelected) 24f else 8f,
                        animationSpec = tween(300),
                        label = "indicator_width"
                    )
                    
                    Box(
                        modifier = Modifier
                            .width(animatedWidth.dp)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                    )
                }
            }
            
            // Navigation buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimensions.PaddingLarge),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous button
                if (pagerState.currentPage > 0) {
                    TextButton(
                        onClick = {
                            haptic.light()
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                    ) {
                        Text("Назад")
                    }
                } else {
                    Spacer(modifier = Modifier.width(80.dp))
                }
                
                // Next/Finish button
                FloatingActionButton(
                    onClick = {
                        haptic.medium()
                        if (isLastPage) {
                            onOnboardingComplete()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = if (isLastPage) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = if (isLastPage) "Завершить" else "Далее",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingPage(
    page: OnboardingPageData,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(Dimensions.PaddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Illustration
        Card(
            modifier = Modifier
                .size(280.dp)
                .padding(bottom = Dimensions.PaddingLarge),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Image(
                painter = painterResource(id = page.imageRes),
                contentDescription = page.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        
        // Title
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = Dimensions.PaddingMedium)
        )
        
        // Description
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 24.sp,
            modifier = Modifier.padding(horizontal = Dimensions.PaddingMedium)
        )
    }
}

/**
 * Data class for onboarding page
 */
data class OnboardingPageData(
    val imageRes: Int,
    val title: String,
    val description: String
)

/**
 * Onboarding pages data
 */
private val onboardingPages = listOf(
    OnboardingPageData(
        imageRes = R.drawable.onboarding_welcome, // You'll need to add these images
        title = "Добро пожаловать в Adygyes!",
        description = "Откройте для себя удивительные места Адыгеи с помощью нашего интерактивного путеводителя"
    ),
    OnboardingPageData(
        imageRes = R.drawable.onboarding_map,
        title = "Интерактивная карта",
        description = "Исследуйте достопримечательности на детальной карте с удобными маркерами и маршрутами"
    ),
    OnboardingPageData(
        imageRes = R.drawable.onboarding_attractions,
        title = "Богатая информация",
        description = "Узнайте подробности о каждом месте: описания, фотографии, часы работы и контакты"
    ),
    OnboardingPageData(
        imageRes = R.drawable.onboarding_favorites,
        title = "Избранное и маршруты",
        description = "Сохраняйте любимые места и планируйте маршруты для незабываемых путешествий"
    ),
    OnboardingPageData(
        imageRes = R.drawable.onboarding_offline,
        title = "Работает офлайн",
        description = "Пользуйтесь приложением даже без интернета благодаря офлайн-режиму"
    )
)
