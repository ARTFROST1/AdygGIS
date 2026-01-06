package com.adygyes.app.presentation.ui.components.reviews

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Status Badge Component
 * Бейдж статуса отзыва (На модерации, Опубликован, Отклонён)
 * Unified with RN ReviewSection status badge
 */
@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor, label) = when (status) {
        "pending" -> Triple(
            Color(0xFFFFF4E5), // Light orange
            Color(0xFFFF9800), // Orange
            "На модерации"
        )
        "approved" -> Triple(
            Color(0xFFE8F5E9), // Light green
            Color(0xFF4CAF50), // Green
            "Опубликован"
        )
        "rejected" -> Triple(
            Color(0xFFFFEBEE), // Light red
            Color(0xFFF44336), // Red
            "Отклонён"
        )
        else -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Неизвестно"
        )
    }
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}
