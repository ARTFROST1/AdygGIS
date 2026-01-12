package com.adygyes.app.presentation.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.adygyes.app.R

/**
 * Bottom navigation bar for Adygyes app
 * Contains three items: View Toggle (Map/List), Favorites, Settings
 */
@Composable
fun AdygyesBottomNavigation(
    currentViewMode: ViewMode,
    onViewModeToggle: () -> Unit,
    onFavoritesClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    showBadgeOnFavorites: Boolean = false,
    favoritesCount: Int = 0
) {
    NavigationBar(
        modifier = modifier,
        tonalElevation = 8.dp
    ) {
        // View Mode Toggle (Map/List)
        NavigationBarItem(
            icon = {
                AnimatedContent(
                    targetState = currentViewMode,
                    transitionSpec = {
                        (scaleIn(animationSpec = tween(220)) + fadeIn(animationSpec = tween(220)))
                            .togetherWith(scaleOut(animationSpec = tween(90)) + fadeOut(animationSpec = tween(90)))
                    }
                ) { mode ->
                    Icon(
                        imageVector = if (mode == ViewMode.MAP) {
                            Icons.AutoMirrored.Filled.List
                        } else {
                            Icons.Filled.Map
                        },
                        contentDescription = if (mode == ViewMode.MAP) {
                            stringResource(R.string.switch_to_list_view)
                        } else {
                            stringResource(R.string.switch_to_map_view)
                        },
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            label = { 
                AnimatedContent(
                    targetState = currentViewMode,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220))
                            .togetherWith(fadeOut(animationSpec = tween(90)))
                    }
                ) { mode ->
                    Text(
                        text = if (mode == ViewMode.MAP) {
                            stringResource(R.string.nav_list)
                        } else {
                            stringResource(R.string.nav_map)
                        },
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            selected = false,
            onClick = onViewModeToggle,
            alwaysShowLabel = true,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                indicatorColor = androidx.compose.ui.graphics.Color.Transparent
            )
        )
        
        // Favorites
        NavigationBarItem(
            icon = {
                BadgedBox(
                    badge = {
                        if (showBadgeOnFavorites && favoritesCount > 0) {
                            Badge {
                                Text(
                                    text = if (favoritesCount > 99) "99+" else favoritesCount.toString()
                                )
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        contentDescription = stringResource(R.string.nav_favorites),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            label = { 
                Text(
                    text = stringResource(R.string.nav_favorites),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            selected = false,
            onClick = onFavoritesClick,
            alwaysShowLabel = true,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                indicatorColor = androidx.compose.ui.graphics.Color.Transparent
            )
        )
        
        // Settings
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.nav_settings),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            label = { 
                Text(
                    text = stringResource(R.string.nav_settings),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            selected = false,
            onClick = onSettingsClick,
            alwaysShowLabel = true,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                indicatorColor = androidx.compose.ui.graphics.Color.Transparent
            )
        )
    }
}

/**
 * View mode enum for map/list toggle
 */
enum class ViewMode {
    MAP, LIST
}

/**
 * Navigation item data class
 */
data class BottomNavItem(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String,
    val badge: Int? = null
)
