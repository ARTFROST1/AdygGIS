package com.adygyes.app.presentation.navigation

/**
 * Navigation destinations for the Adygyes app
 */
sealed class NavDestination(val route: String) {
    
    // Splash screen
    data object Splash : NavDestination("splash")
    
    // Main screens
    data object Map : NavDestination("map")
    data object Search : NavDestination("search")
    data object Favorites : NavDestination("favorites")
    data object Settings : NavDestination("settings")
    
    // Category filter
    data object CategoryFilter : NavDestination("category_filter")
    
    // Photo gallery
    data object PhotoGallery : NavDestination("photo_gallery/{attractionId}/{photoIndex}") {
        fun createRoute(attractionId: String, photoIndex: Int) = "photo_gallery/$attractionId/$photoIndex"
    }
    
    // Settings sub-screens
    data object LanguageSettings : NavDestination("settings/language")
    data object ThemeSettings : NavDestination("settings/theme")
    data object AboutScreen : NavDestination("settings/about")
    data object PrivacyPolicy : NavDestination("settings/privacy")
    data object TermsOfUse : NavDestination("settings/terms")
}

/**
 * Bottom navigation items
 */
enum class BottomNavItem(
    val destination: NavDestination,
    val iconResId: Int = 0, // Will be replaced with actual icon resources
    val labelResId: Int = 0  // Will be replaced with actual string resources
) {
    MAP(NavDestination.Map),
    SEARCH(NavDestination.Search),
    FAVORITES(NavDestination.Favorites),
    SETTINGS(NavDestination.Settings)
}
