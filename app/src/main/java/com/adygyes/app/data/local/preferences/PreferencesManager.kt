package com.adygyes.app.data.local.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.adygyes.app.data.local.locale.LocaleManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for handling user preferences using DataStore
 */
@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>
) {
    
    // SharedPreferences for synchronous language access in attachBaseContext
    private val sharedPrefs: SharedPreferences by lazy {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    }
    
    // Preference Keys
    companion object {
        private val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_LANGUAGE = stringPreferencesKey("language")
        private val KEY_MAP_TYPE = stringPreferencesKey("map_type")
        private val KEY_SHOW_TRAFFIC = booleanPreferencesKey("show_traffic")
        private val KEY_AUTO_CENTER_LOCATION = booleanPreferencesKey("auto_center_location")
        private val KEY_FIRST_LAUNCH = booleanPreferencesKey("first_launch")
        private val KEY_FILTER_CATEGORIES = stringSetPreferencesKey("filter_categories")
        private val KEY_SORT_OPTION = stringPreferencesKey("sort_option")
        private val KEY_OFFLINE_MODE = booleanPreferencesKey("offline_mode")
        private val KEY_NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled")
        private val KEY_DATA_VERSION = stringPreferencesKey("data_version")
        private val KEY_LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
        // Supabase sync timestamp (ISO 8601 string)
        private val KEY_LAST_SYNC_TIMESTAMP = stringPreferencesKey("last_sync_timestamp")
        // Map camera state keys
        private val KEY_CAMERA_LAT = doublePreferencesKey("camera_lat")
        private val KEY_CAMERA_LON = doublePreferencesKey("camera_lon")
        private val KEY_CAMERA_ZOOM = floatPreferencesKey("camera_zoom")
        private val KEY_CAMERA_AZIMUTH = floatPreferencesKey("camera_azimuth")
        private val KEY_CAMERA_TILT = floatPreferencesKey("camera_tilt")
    }
    
    /**
     * User preferences data class
     */
    data class UserPreferences(
        val isDarkTheme: Boolean = false,
        val themeMode: String = "system", // "light", "dark", "system"
        val language: String = LocaleManager.DEFAULT_LANGUAGE, // "ru" or "en" - default to Russian
        val mapType: String = "normal", // "normal", "satellite", "hybrid"
        val showTraffic: Boolean = false,
        val autoCenterLocation: Boolean = true,
        val isFirstLaunch: Boolean = true,
        val filterCategories: Set<String> = emptySet(),
        val sortOption: String = "name", // "name", "rating", "distance"
        val offlineMode: Boolean = false,
        val notificationEnabled: Boolean = true,
        val dataVersion: String? = null,
        val lastSyncTime: Long = 0L
    )

    /**
     * Map camera preferences data class
     */
    data class CameraPreferences(
        val lat: Double = 44.6098,
        val lon: Double = 40.1006,
        val zoom: Float = 10f,
        val azimuth: Float = 0f,
        val tilt: Float = 0f
    )
    
    /**
     * Flow of user preferences
     */
    val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception, "Error reading preferences")
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            mapUserPreferences(preferences)
        }

    /**
     * Flow of map camera preferences
     */
    val cameraStateFlow: Flow<CameraPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception, "Error reading camera preferences")
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            CameraPreferences(
                lat = preferences[KEY_CAMERA_LAT] ?: 44.6098,
                lon = preferences[KEY_CAMERA_LON] ?: 40.1006,
                zoom = preferences[KEY_CAMERA_ZOOM] ?: 10f,
                azimuth = preferences[KEY_CAMERA_AZIMUTH] ?: 0f,
                tilt = preferences[KEY_CAMERA_TILT] ?: 0f
            )
        }
    
    /**
     * Map preferences to UserPreferences data class
     */
    private fun mapUserPreferences(preferences: Preferences): UserPreferences {
        // Get language from DataStore or fallback to SharedPreferences
        val language = preferences[KEY_LANGUAGE] ?: run {
            val savedLanguage = sharedPrefs.getString("language", LocaleManager.DEFAULT_LANGUAGE) 
                ?: LocaleManager.DEFAULT_LANGUAGE
            savedLanguage
        }
        
        return UserPreferences(
            isDarkTheme = preferences[KEY_DARK_THEME] ?: false,
            themeMode = preferences[KEY_THEME_MODE] ?: run {
                // Backward compatibility: derive from old boolean if present
                when (preferences[KEY_DARK_THEME] ?: false) {
                    true -> "dark"
                    false -> "light"
                }
            },
            language = language,
            mapType = preferences[KEY_MAP_TYPE] ?: "normal",
            showTraffic = preferences[KEY_SHOW_TRAFFIC] ?: false,
            autoCenterLocation = preferences[KEY_AUTO_CENTER_LOCATION] ?: true,
            isFirstLaunch = preferences[KEY_FIRST_LAUNCH] ?: true,
            filterCategories = preferences[KEY_FILTER_CATEGORIES] ?: emptySet(),
            sortOption = preferences[KEY_SORT_OPTION] ?: "name",
            offlineMode = preferences[KEY_OFFLINE_MODE] ?: false,
            notificationEnabled = preferences[KEY_NOTIFICATION_ENABLED] ?: true,
            dataVersion = preferences[KEY_DATA_VERSION],
            lastSyncTime = preferences[KEY_LAST_SYNC_TIME] ?: 0L
        )
    }
    
    /**
     * Update dark theme preference
     */
    suspend fun updateDarkTheme(isDarkTheme: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_DARK_THEME] = isDarkTheme
        }
    }

    /**
     * Update theme mode preference ("light", "dark", or "system")
     */
    suspend fun updateThemeMode(mode: String) {
        val normalized = when (mode.lowercase()) {
            "light", "dark", "system" -> mode.lowercase()
            else -> "system"
        }
        dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = normalized
            // Keep legacy boolean in sync for backward compatibility
            preferences[KEY_DARK_THEME] = (normalized == "dark")
        }
    }
    
    /**
     * Update language preference
     */
    suspend fun updateLanguage(language: String) {
        // Save to SharedPreferences for synchronous access
        sharedPrefs.edit().putString("language", language).apply()
        
        // Also save to DataStore for flow-based access
        dataStore.edit { preferences ->
            preferences[KEY_LANGUAGE] = language
        }
    }
    
    /**
     * Update map type preference
     */
    suspend fun updateMapType(mapType: String) {
        dataStore.edit { preferences ->
            preferences[KEY_MAP_TYPE] = mapType
        }
    }
    
    /**
     * Update traffic display preference
     */
    suspend fun updateShowTraffic(showTraffic: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_SHOW_TRAFFIC] = showTraffic
        }
    }
    
    /**
     * Update auto center location preference
     */
    suspend fun updateAutoCenterLocation(autoCenter: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_AUTO_CENTER_LOCATION] = autoCenter
        }
    }
    
    /**
     * Mark first launch as completed
     */
    suspend fun setFirstLaunchCompleted() {
        dataStore.edit { preferences ->
            preferences[KEY_FIRST_LAUNCH] = false
        }
    }
    
    /**
     * Update filter categories
     */
    suspend fun updateFilterCategories(categories: Set<String>) {
        dataStore.edit { preferences ->
            preferences[KEY_FILTER_CATEGORIES] = categories
        }
    }
    
    /**
     * Update sort option
     */
    suspend fun updateSortOption(sortOption: String) {
        dataStore.edit { preferences ->
            preferences[KEY_SORT_OPTION] = sortOption
        }
    }
    
    /**
     * Update offline mode
     */
    suspend fun updateOfflineMode(offlineMode: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_OFFLINE_MODE] = offlineMode
        }
    }
    
    /**
     * Update notification enabled
     */
    suspend fun updateNotificationEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATION_ENABLED] = enabled
        }
    }
    
    /**
     * Update data version
     */
    suspend fun updateDataVersion(version: String) {
        dataStore.edit { preferences ->
            preferences[KEY_DATA_VERSION] = version
        }
    }
    
    /**
     * Update last sync time
     */
    suspend fun updateLastSyncTime(timestamp: Long) {
        dataStore.edit { preferences ->
            preferences[KEY_LAST_SYNC_TIME] = timestamp
        }
    }
    
    /**
     * Get last sync timestamp (ISO 8601 string) for Supabase delta sync
     * Returns null if never synced
     */
    suspend fun getLastSyncTimestamp(): String? {
        return dataStore.data.first()[KEY_LAST_SYNC_TIMESTAMP]
    }
    
    /**
     * Update last sync timestamp (ISO 8601 string) for Supabase delta sync
     */
    suspend fun updateLastSyncTimestamp(timestamp: String) {
        dataStore.edit { preferences ->
            preferences[KEY_LAST_SYNC_TIMESTAMP] = timestamp
        }
    }

    /**
     * Update map camera state
     */
    suspend fun updateCameraState(
        lat: Double,
        lon: Double,
        zoom: Float,
        azimuth: Float,
        tilt: Float
    ) {
        dataStore.edit { preferences ->
            preferences[KEY_CAMERA_LAT] = lat
            preferences[KEY_CAMERA_LON] = lon
            preferences[KEY_CAMERA_ZOOM] = zoom
            preferences[KEY_CAMERA_AZIMUTH] = azimuth
            preferences[KEY_CAMERA_TILT] = tilt
        }
    }
    
    /**
     * Clear all preferences
     */
    suspend fun clearPreferences() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
