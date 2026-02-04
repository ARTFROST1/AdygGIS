package com.adygyes.app.presentation.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Accessible
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adygyes.app.domain.model.Attraction
import com.adygyes.app.domain.model.ContactInfo

/**
 * Modern Attraction Info Components
 * Redesigned to match RN version with green accent color, cards with shadows,
 * and improved visual hierarchy.
 */

// Info card data class
private data class InfoCardData(
    val icon: ImageVector,
    val label: String,
    val value: String,
    val onClick: (() -> Unit)? = null
)

/**
 * Modern info section with color-coded cards (RN style spacing)
 * Unified green color scheme for consistency
 */
@Composable
fun ModernAttractionInfo(
    attraction: Attraction,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    
    // DEBUG: Log attraction data
    android.util.Log.d("ModernAttractionInfo", """
        📍 Displaying attraction: ${attraction.name}
        ☀️ Operating Season: ${attraction.operatingSeason ?: "NULL"}
        💰 Price Info: ${attraction.priceInfo ?: "NULL"}
        ⏱️ Duration: ${attraction.duration ?: "NULL"}
        📅 Best Time: ${attraction.bestTimeToVisit ?: "NULL"}
        🎯 Amenities count: ${attraction.amenities.size}
        🏷️ Tags count: ${attraction.tags.size}
    """.trimIndent())
    
    // Build essential info list
    val essentialInfo = buildList {
        attraction.location.address?.let { address ->
            add(InfoCardData(
                icon = Icons.Outlined.LocationOn,
                label = "Адрес",
                value = address,
                onClick = {
                    val uri = Uri.parse("geo:${attraction.location.latitude},${attraction.location.longitude}?q=${Uri.encode(address)}")
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                }
            ))
        }
        
        attraction.location.directions?.let { directions ->
            add(InfoCardData(
                icon = Icons.Outlined.Explore,
                label = "Как добраться",
                value = directions
            ))
        }
        
        attraction.workingHours?.let { hours ->
            add(InfoCardData(
                icon = Icons.Outlined.Schedule,
                label = "Время работы",
                value = hours
            ))
        }
        
        attraction.operatingSeason?.let { season ->
            add(InfoCardData(
                icon = Icons.Outlined.WbSunny,
                label = "Сезон работы",
                value = season
            ))
        }
        
        attraction.priceInfo?.let { price ->
            add(InfoCardData(
                icon = Icons.Outlined.Payments,
                label = "Стоимость",
                value = price
            ))
        }
        
        attraction.duration?.let { duration ->
            add(InfoCardData(
                icon = Icons.Outlined.Timer,
                label = "Продолжительность",
                value = duration
            ))
        }
        
        attraction.bestTimeToVisit?.let { bestTime ->
            add(InfoCardData(
                icon = Icons.Outlined.CalendarMonth,
                label = "Лучшее время",
                value = bestTime
            ))
        }
    }
    
    // Build contact info list
    val contactInfo = buildList {
        attraction.contactInfo?.phone?.let { phone ->
            add(InfoCardData(
                icon = Icons.Outlined.Phone,
                label = "Позвонить",
                value = phone,
                onClick = {
                    val uri = Uri.parse("tel:$phone")
                    context.startActivity(Intent(Intent.ACTION_DIAL, uri))
                }
            ))
        }
        
        attraction.contactInfo?.email?.let { email ->
            add(InfoCardData(
                icon = Icons.Outlined.Email,
                label = "Написать",
                value = email,
                onClick = {
                    val uri = Uri.parse("mailto:$email")
                    context.startActivity(Intent(Intent.ACTION_SENDTO, uri))
                }
            ))
        }
        
        attraction.contactInfo?.website?.let { website ->
            add(InfoCardData(
                icon = Icons.Outlined.Language,
                label = "Перейти на сайт",
                value = website,
                onClick = {
                    val uri = Uri.parse(if (website.startsWith("http")) website else "https://$website")
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                }
            ))
        }
    }
    
    Column(
        modifier = modifier.padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Essential information cards
        if (essentialInfo.isNotEmpty()) {
            essentialInfo.forEach { info ->
                ModernInfoCard(
                    icon = info.icon,
                    label = info.label,
                    value = info.value,
                    iconColor = primaryColor,
                    iconBgColor = primaryContainer,
                    onClick = info.onClick
                )
            }
        }
        
        // Contact action buttons
        if (contactInfo.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            contactInfo.forEach { info ->
                ModernContactButton(
                    icon = info.icon,
                    label = info.label,
                    value = info.value,
                    onClick = info.onClick!!
                )
            }
        }
    }
}

/**
 * Modern info card with shadow and rounded corners
 */
@Composable
private fun ModernInfoCard(
    icon: ImageVector,
    label: String,
    value: String,
    iconColor: Color,
    iconBgColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 1.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color.Black.copy(alpha = 0.05f)
            )
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = iconBgColor,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = iconColor
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                ExpandableInfoText(
                    text = value,
                    collapsedMaxLines = 2
                )
            }
            
            // Chevron for clickable cards
            if (onClick != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Modern contact action button (RN style - removed extra shadow)
 */
@Composable
private fun ModernContactButton(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.5.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Arrow
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Modern amenities with icon circles and 2-column grid (RN exact layout)
 */
@Composable
fun ModernAmenitiesSection(
    amenities: List<String>,
    modifier: Modifier = Modifier
) {
    android.util.Log.d("ModernAmenitiesSection", "🎯 Amenities received: ${amenities.size} items")
    amenities.forEachIndexed { index, amenity ->
        android.util.Log.d("ModernAmenitiesSection", "  $index: $amenity")
    }
    
    if (amenities.isEmpty()) return
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "УДОБСТВА",
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // FlowRow with exact RN spacing (8dp gap) and proper sizing
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            amenities.forEach { amenity ->
                ModernAmenityCard(
                    amenity = amenity,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .widthIn(min = 150.dp, max = 200.dp)
                )
            }
        }
    }
}

/**
 * Modern amenity card with icon in colored circle
 */
@Composable
private fun ModernAmenityCard(
    amenity: String,
    modifier: Modifier = Modifier
) {
    val icon = getAmenityIcon(amenity)
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        ),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = primaryContainer,
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = primaryColor
                )
            }
            
            Text(
                text = amenity,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Modern tags with green gradient (RN exact style)
 */
@Composable
fun ModernTagsSection(
    tags: List<String>,
    modifier: Modifier = Modifier
) {
    android.util.Log.d("ModernTagsSection", "🏷️ Tags received: ${tags.size} items")
    tags.forEachIndexed { index, tag ->
        android.util.Log.d("ModernTagsSection", "  $index: #$tag")
    }
    
    if (tags.isEmpty()) return
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "МЕТКИ",
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // Wrap tags in rows with exact RN spacing (8dp gap)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tags.forEach { tag ->
                GradientTag(tag = tag)
            }
        }
    }
}

/**
 * Gradient tag with shadow
 */
@Composable
private fun GradientTag(
    tag: String,
    modifier: Modifier = Modifier
) {
    val gradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF4CAF50), // Primary green
            Color(0xFF66BB6A)  // Lighter green
        )
    )
    
    Box(
        modifier = modifier
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color.Black.copy(alpha = 0.1f)
            )
            .background(
                brush = gradient,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = "#$tag",
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp
            ),
            color = Color.White
        )
    }
}

/**
 * Simple FlowRow implementation for tags
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement
    ) {
        content()
    }
}

/**
 * Get icon for amenity type
 */
private fun getAmenityIcon(amenity: String): ImageVector {
    return when (amenity.lowercase()) {
        "parking", "парковка" -> Icons.Outlined.DirectionsCar
        "wifi", "wi-fi" -> Icons.Outlined.Wifi
        "restaurant", "ресторан", "кафе", "cafe" -> Icons.Outlined.Restaurant
        "toilet", "туалет", "restroom", "wc" -> Icons.Outlined.Wc
        "shop", "магазин", "store" -> Icons.Outlined.ShoppingCart
        "guide", "гид", "экскурсия" -> Icons.Outlined.Person
        "photo", "фото", "photography" -> Icons.Outlined.PhotoCamera
        "disabled", "инвалиды", "accessibility", "доступность" -> Icons.AutoMirrored.Outlined.Accessible
        "atm", "банкомат" -> Icons.Outlined.LocalAtm
        else -> Icons.Outlined.CheckCircle
    }
}
