package com.adygyes.app.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adygyes.app.domain.model.AttractionCategory

/**
 * Size variants for CategoryChip (unified with RN)
 */
enum class ChipSize {
    SMALL,   // 28dp height, 10sp text
    MEDIUM,  // 32dp height, 12sp text
    LARGE    // 40dp height, 14sp text
}

/**
 * Colored chip component for displaying attraction categories
 * Unified with RN CategoryChip component
 * 
 * @param category The category to display
 * @param modifier Modifier for styling
 * @param size Size variant (SMALL, MEDIUM, LARGE)
 * @param showEmoji Whether to show emoji icon
 * @param showLabel Whether to show text label
 * @param onClick Optional click handler
 */
@Composable
fun CategoryChip(
    category: AttractionCategory,
    modifier: Modifier = Modifier,
    size: ChipSize = ChipSize.MEDIUM,
    showEmoji: Boolean = false,
    showLabel: Boolean = true,
    compact: Boolean = false, // Legacy param, maps to SMALL size
    onClick: (() -> Unit)? = null
) {
    val backgroundColor = Color(android.graphics.Color.parseColor(category.colorHex))
    val contentColor = if (isColorDark(backgroundColor)) Color.White else Color.Black
    
    // Size configuration (unified with RN)
    val effectiveSize = if (compact) ChipSize.SMALL else size
    val (textSize, emojiSize, paddingH, paddingV, cornerRadius) = when (effectiveSize) {
        ChipSize.SMALL -> SizeConfig(10.sp, 12.sp, 8.dp, 4.dp, 14.dp)
        ChipSize.MEDIUM -> SizeConfig(12.sp, 14.sp, 12.dp, 6.dp, 16.dp)
        ChipSize.LARGE -> SizeConfig(14.sp, 16.sp, 16.dp, 8.dp, 20.dp)
    }
    
    Surface(
        modifier = modifier,
        color = backgroundColor,
        shape = RoundedCornerShape(cornerRadius),
        onClick = onClick ?: {}
    ) {
        Row(
            modifier = Modifier.padding(horizontal = paddingH, vertical = paddingV),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (showEmoji) {
                Text(
                    text = category.emoji,
                    fontSize = emojiSize
                )
                if (showLabel) {
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
            if (showLabel) {
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = textSize),
                    color = contentColor,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Internal size configuration data class
 */
private data class SizeConfig(
    val textSize: androidx.compose.ui.unit.TextUnit,
    val emojiSize: androidx.compose.ui.unit.TextUnit,
    val paddingH: androidx.compose.ui.unit.Dp,
    val paddingV: androidx.compose.ui.unit.Dp,
    val cornerRadius: androidx.compose.ui.unit.Dp
)

/**
 * Multi-select category filter chips
 */
@Composable
fun CategoryFilterChips(
    selectedCategories: Set<AttractionCategory>,
    onCategoryToggle: (AttractionCategory) -> Unit,
    modifier: Modifier = Modifier,
    showAll: Boolean = true
) {
    Column(modifier = modifier) {
        if (showAll) {
            Text(
                text = "Categories",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AttractionCategory.values().forEach { category ->
                FilterChip(
                    selected = selectedCategories.contains(category),
                    onClick = { onCategoryToggle(category) },
                    enabled = true,
                    label = {
                        Text(
                            text = category.displayName,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(android.graphics.Color.parseColor(category.colorHex)).copy(alpha = 0.2f),
                        selectedLabelColor = Color(android.graphics.Color.parseColor(category.colorHex))
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedCategories.contains(category),
                        borderColor = Color(android.graphics.Color.parseColor(category.colorHex)),
                        selectedBorderColor = Color(android.graphics.Color.parseColor(category.colorHex))
                    )
                )
            }
        }
    }
}

/**
 * Helper function to determine if a color is dark
 */
private fun isColorDark(color: Color): Boolean {
    val darkness = 1 - (0.299 * color.red + 0.587 * color.green + 0.114 * color.blue)
    return darkness >= 0.5
}
