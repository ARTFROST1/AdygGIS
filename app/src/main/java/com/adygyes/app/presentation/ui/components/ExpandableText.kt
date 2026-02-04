package com.adygyes.app.presentation.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Expandable text component with "Read more"/"Read less" functionality
 * 
 * Mobile UX best practices:
 * - Shows truncated text initially (2-3 lines)
 * - Smooth animation when expanding/collapsing
 * - Clear visual indicator (icon + text button)
 * - Only shows expand button if text is actually truncated
 * - Full-width collapse button when expanded
 */
@Composable
fun ExpandableText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    collapsedMaxLines: Int = 3,
    expandButtonText: String = "Показать всё",
    collapseButtonText: String = "Свернуть",
    showIcon: Boolean = true
) {
    var isExpanded by remember { mutableStateOf(false) }
    var isTextTruncated by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier.animateContentSize(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    ) {
        Text(
            text = text,
            style = style,
            color = color,
            maxLines = if (isExpanded) Int.MAX_VALUE else collapsedMaxLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { layoutResult ->
                // Check if text is actually truncated (has more lines than visible)
                if (!isExpanded) {
                    isTextTruncated = layoutResult.hasVisualOverflow
                }
            }
        )
        
        // Show expand/collapse button only if text is actually truncated
        if (isTextTruncated || isExpanded) {
            Spacer(modifier = Modifier.height(8.dp))
            
            if (isExpanded) {
                // Full-width collapse button
                Surface(
                    onClick = { isExpanded = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (showIcon) {
                            Icon(
                                imageVector = Icons.Default.ExpandLess,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = collapseButtonText,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                // Compact expand button
                TextButton(
                    onClick = { isExpanded = true },
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    if (showIcon) {
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = expandButtonText,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

/**
 * Variant for info cards with custom styling
 */
@Composable
fun ExpandableInfoText(
    text: String,
    modifier: Modifier = Modifier,
    collapsedMaxLines: Int = 2
) {
    ExpandableText(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        collapsedMaxLines = collapsedMaxLines,
        expandButtonText = "Ещё",
        collapseButtonText = "Скрыть",
        showIcon = true
    )
}

/**
 * Variant for description text with larger collapsed view
 */
@Composable
fun ExpandableDescription(
    description: String,
    modifier: Modifier = Modifier,
    collapsedMaxLines: Int = 4
) {
    ExpandableText(
        text = description,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium.copy(
            lineHeight = 20.sp
        ),
        color = MaterialTheme.colorScheme.onSurface,
        collapsedMaxLines = collapsedMaxLines,
        expandButtonText = "Читать дальше",
        collapseButtonText = "Свернуть",
        showIcon = true
    )
}
