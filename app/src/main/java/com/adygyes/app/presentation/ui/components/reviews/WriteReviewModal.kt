package com.adygyes.app.presentation.ui.components.reviews

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.adygyes.app.presentation.theme.Dimensions

/**
 * Write Review Modal Component
 * Модальное окно для написания отзыва
 * Unified with RN WriteReviewModal component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteReviewModal(
    visible: Boolean,
    onClose: () -> Unit,
    onSubmit: (rating: Int, text: String?) -> Unit,
    attractionName: String,
    submitting: Boolean = false,
    errorMessage: String? = null,
    initialRating: Int = 0,
    modifier: Modifier = Modifier
) {
    if (!visible) return
    
    var rating by remember { mutableIntStateOf(initialRating) }
    var text by remember { mutableStateOf("") }
    
    // Reset state when modal opens
    LaunchedEffect(visible) {
        if (visible) {
            rating = initialRating
            text = ""
        }
    }
    
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onClose() },
            contentAlignment = Alignment.BottomCenter
        ) {
            // Modal content
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f)
                    // Edge-to-edge: add insets padding for keyboard and navigation bar
                    .navigationBarsPadding()
                    .imePadding()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { /* Consume click to prevent closing */ },
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimensions.PaddingLarge),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Оставить отзыв",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Закрыть",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    
                    // Content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = Dimensions.PaddingLarge)
                    ) {
                        // Attraction name
                        Text(
                            text = attractionName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Rating section
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Interactive stars
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(5) { index ->
                                    val starIndex = index + 1
                                    val isFilled = starIndex <= rating
                                    
                                    IconButton(
                                        onClick = { rating = starIndex },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isFilled) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                            contentDescription = "Оценка $starIndex",
                                            modifier = Modifier.size(40.dp),
                                            tint = if (isFilled) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Rating label
                            Text(
                                text = if (rating == 0) "Ваша оценка" else getRatingLabel(rating),
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (rating == 0) 
                                    MaterialTheme.colorScheme.onSurfaceVariant 
                                else 
                                    MaterialTheme.colorScheme.primary,
                                fontWeight = if (rating > 0) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Text input
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            placeholder = {
                                Text(
                                    text = "Напишите отзыв (необязательно)...",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        if (!errorMessage.isNullOrBlank()) {
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                    
                    // Footer buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimensions.PaddingLarge),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Cancel button
                        OutlinedButton(
                            onClick = onClose,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Отмена")
                        }
                        
                        // Submit button
                        Button(
                            onClick = {
                                if (rating > 0 && !submitting) {
                                    onSubmit(rating, text.ifBlank { null })
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = rating > 0 && !submitting,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                        ) {
                            if (submitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Отправить")
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Large Interactive Rating Component for modal
 * Большие интерактивные звёзды для оценки
 */
@Composable
fun InteractiveRatingLarge(
    value: Int,
    onRatingChange: (Int) -> Unit,
    size: Int = 40,
    spacing: Int = 8,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(5) { index ->
            val starIndex = index + 1
            val isFilled = starIndex <= value
            
            IconButton(
                onClick = { onRatingChange(starIndex) },
                modifier = Modifier.size((size + 16).dp)
            ) {
                Icon(
                    imageVector = if (isFilled) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Оценка $starIndex",
                    modifier = Modifier.size(size.dp),
                    tint = if (isFilled) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

/**
 * Get rating label based on rating value
 * Matches RN getRatingLabel function
 */
private fun getRatingLabel(rating: Int): String {
    return when (rating) {
        1 -> "Ужасно"
        2 -> "Плохо"
        3 -> "Нормально"
        4 -> "Хорошо"
        5 -> "Отлично!"
        else -> ""
    }
}
