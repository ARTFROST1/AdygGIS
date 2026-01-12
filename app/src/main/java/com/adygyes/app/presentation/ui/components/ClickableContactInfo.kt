package com.adygyes.app.presentation.ui.components

import android.content.Intent
import android.net.Uri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adygyes.app.R
import com.adygyes.app.domain.model.ContactInfo
import com.adygyes.app.presentation.theme.Dimensions
import com.adygyes.app.presentation.viewmodel.ContactActionViewModel

/**
 * Компонент для отображения кликабельной контактной информации
 * с минималистичным дизайном и плавными анимациями
 */
@Composable
fun ClickableContactInfo(
    contactInfo: ContactInfo,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    viewModel: ContactActionViewModel = hiltViewModel()
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp)
    ) {
        // Телефон
        contactInfo.phone?.let { phone ->
            ClickableContactItem(
                icon = Icons.Default.Phone,
                label = if (compact) null else "Телефон",
                value = phone,
                onClick = { context ->
                    viewModel.makePhoneCall(context, phone)
                },
                compact = compact,
                color = MaterialTheme.colorScheme.primary,
                viewModel = viewModel
            )
        }

        // Email
        contactInfo.email?.let { email ->
            ClickableContactItem(
                icon = Icons.Default.Email,
                label = if (compact) null else "Email",
                value = email,
                onClick = { context ->
                    viewModel.sendEmail(context, email)
                },
                compact = compact,
                color = MaterialTheme.colorScheme.secondary,
                viewModel = viewModel
            )
        }

        // Веб-сайт
        contactInfo.website?.let { website ->
            ClickableContactItem(
                icon = Icons.Default.Language,
                label = if (compact) null else "Веб-сайт",
                value = formatWebsiteForDisplay(website),
                onClick = { context ->
                    viewModel.openWebsite(context, website)
                },
                compact = compact,
                color = MaterialTheme.colorScheme.tertiary,
                viewModel = viewModel
            )
        }

        // Социальные сети
        contactInfo.socialMedia.forEach { (platform, url) ->
            val (icon, label) = getSocialMediaInfo(platform)
            ClickableContactItem(
                icon = icon,
                label = if (compact) null else label,
                value = formatSocialMediaForDisplay(platform, url),
                onClick = { context ->
                    viewModel.openSocialMedia(context, platform, url)
                },
                compact = compact,
                color = getSocialMediaColor(platform),
                viewModel = viewModel
            )
        }
    }
}

/**
 * Отдельный элемент контактной информации с анимациями
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ClickableContactItem(
    icon: ImageVector,
    label: String?,
    value: String,
    onClick: (android.content.Context) -> Unit,
    compact: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    viewModel: ContactActionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(100),
        label = "scale"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isPressed) color.copy(alpha = 0.1f) else Color.Transparent,
        animationSpec = tween(100),
        label = "backgroundColor"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(if (compact) 8.dp else 12.dp))
            .background(backgroundColor)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(
                    color = color,
                    bounded = true
                ),
                onClick = { onClick(context) },
                onLongClick = { 
                    viewModel.copyToClipboard(context, value, label ?: "Контакт")
                }
            )
            .padding(
                vertical = if (compact) 6.dp else 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        // Иконка (такая же как в InfoCard для точного выравнивания)
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = color
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Текстовая информация
        Column(
            modifier = Modifier.weight(1f)
        ) {
            if (!compact && label != null) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
            }
            
            Text(
                text = value,
                style = if (compact) {
                    MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    )
                },
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(if (compact) 4.dp else 8.dp))

        // Стрелка для указания кликабельности
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(if (compact) 16.dp else 18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }

    // Обработка нажатий для анимации
    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(100)
            isPressed = false
        }
    }
}

/**
 * Форматирование веб-сайта для отображения
 */
private fun formatWebsiteForDisplay(website: String): String {
    return website
        .removePrefix("https://")
        .removePrefix("http://")
        .removePrefix("www.")
}

/**
 * Форматирование социальных сетей для отображения
 */
private fun formatSocialMediaForDisplay(platform: String, url: String): String {
    return when (platform.lowercase()) {
        "instagram" -> "@${extractUsernameFromUrl(url)}"
        "telegram" -> "@${extractUsernameFromUrl(url)}"
        "vk", "vkontakte" -> "VK: ${extractUsernameFromUrl(url)}"
        "facebook" -> "Facebook"
        "youtube" -> "YouTube"
        else -> platform.replaceFirstChar { it.uppercase() }
    }
}

/**
 * Извлечение имени пользователя из URL
 */
private fun extractUsernameFromUrl(url: String): String {
    return url.split("/").lastOrNull()?.takeIf { it.isNotEmpty() } ?: url
}

/**
 * Получение информации о социальной сети
 */
private fun getSocialMediaInfo(platform: String): Pair<ImageVector, String> {
    return when (platform.lowercase()) {
        "instagram" -> Icons.Default.Camera to "Instagram"
        "telegram" -> Icons.AutoMirrored.Filled.Send to "Telegram"
        "vk", "vkontakte" -> Icons.Default.Group to "VKontakte"
        "facebook" -> Icons.Default.Group to "Facebook"
        "youtube" -> Icons.Default.PlayArrow to "YouTube"
        else -> Icons.Default.Link to platform.replaceFirstChar { it.uppercase() }
    }
}

/**
 * Получение цвета для социальной сети
 */
@Composable
private fun getSocialMediaColor(platform: String): Color {
    return when (platform.lowercase()) {
        "instagram" -> Color(0xFFE4405F)
        "telegram" -> Color(0xFF0088CC)
        "vk", "vkontakte" -> Color(0xFF4C75A3)
        "facebook" -> Color(0xFF1877F2)
        "youtube" -> Color(0xFFFF0000)
        else -> MaterialTheme.colorScheme.primary
    }
}
