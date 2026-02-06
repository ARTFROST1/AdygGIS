package com.adygyes.app.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for app settings synchronized from Supabase
 * 
 * These settings are managed via Admin Panel and synced to mobile apps
 * during regular delta sync. Local values act as fallback if not synced yet.
 */
@Singleton
class AppSettingsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        // Keys for DataStore
        private val KEY_WEBSITE_URL = stringPreferencesKey("app_settings_website_url")
        private val KEY_SUPPORT_EMAIL = stringPreferencesKey("app_settings_support_email")
        private val KEY_TELEGRAM_URL = stringPreferencesKey("app_settings_telegram_url")
        private val KEY_TELEGRAM_HANDLE = stringPreferencesKey("app_settings_telegram_handle")
        private val KEY_TELEGRAM_URL_2 = stringPreferencesKey("app_settings_telegram_url_2")
        private val KEY_TELEGRAM_HANDLE_2 = stringPreferencesKey("app_settings_telegram_handle_2")
        private val KEY_GOOGLE_PLAY_URL = stringPreferencesKey("app_settings_google_play_url")
        private val KEY_APP_STORE_URL = stringPreferencesKey("app_settings_app_store_url")
        private val KEY_APP_VERSION = stringPreferencesKey("app_settings_app_version")
        private val KEY_APP_SLOGAN = stringPreferencesKey("app_settings_app_slogan")
        private val KEY_APP_DESCRIPTION = stringPreferencesKey("app_settings_app_description")
        private val KEY_DEVELOPER_1_NAME = stringPreferencesKey("app_settings_developer_1_name")
        private val KEY_DEVELOPER_1_ROLE = stringPreferencesKey("app_settings_developer_1_role")
        private val KEY_DEVELOPER_2_NAME = stringPreferencesKey("app_settings_developer_2_name")
        private val KEY_DEVELOPER_2_ROLE = stringPreferencesKey("app_settings_developer_2_role")
        private val KEY_LAST_UPDATED = stringPreferencesKey("app_settings_last_updated")
        
        // Default values (fallback if not synced)
        const val DEFAULT_WEBSITE_URL = "https://adyggis.vercel.app"
        const val DEFAULT_SUPPORT_EMAIL = "frostmoontechsmc@gmail.com"
        const val DEFAULT_TELEGRAM_URL = "https://t.me/MaykopTech"
        const val DEFAULT_TELEGRAM_HANDLE = "@MaykopTech"
        const val DEFAULT_TELEGRAM_URL_2 = "https://t.me/ArtFrost"
        const val DEFAULT_TELEGRAM_HANDLE_2 = "@ArtFrost"
        const val DEFAULT_GOOGLE_PLAY_URL = "https://play.google.com/store/apps/details?id=com.adygyes.app"
        const val DEFAULT_APP_STORE_URL = ""
        const val DEFAULT_APP_VERSION = "1.0.0"
        const val DEFAULT_APP_SLOGAN = "Открой Адыгею заново"
        const val DEFAULT_APP_DESCRIPTION = "Удобный путеводитель по самым красивым местам Адыгеи. Прогулки, достопримечательности, маршруты и советы для путешественников."
        const val DEFAULT_DEVELOPER_1_NAME = "Sokurov Salim"
        const val DEFAULT_DEVELOPER_1_ROLE = "Разработчик"
        const val DEFAULT_DEVELOPER_2_NAME = "Morozov Artem"
        const val DEFAULT_DEVELOPER_2_ROLE = "Разработчик"
        
        // Mapping from server keys to local keys
        private val SERVER_KEY_MAP = mapOf(
            "website_url" to KEY_WEBSITE_URL,
            "support_email" to KEY_SUPPORT_EMAIL,
            "telegram_url" to KEY_TELEGRAM_URL,
            "telegram_handle" to KEY_TELEGRAM_HANDLE,
            "telegram_url_2" to KEY_TELEGRAM_URL_2,
            "telegram_handle_2" to KEY_TELEGRAM_HANDLE_2,
            "google_play_url" to KEY_GOOGLE_PLAY_URL,
            "app_store_url" to KEY_APP_STORE_URL,
            "app_version" to KEY_APP_VERSION,
            "app_slogan" to KEY_APP_SLOGAN,
            "app_description" to KEY_APP_DESCRIPTION,
            "developer_1_name" to KEY_DEVELOPER_1_NAME,
            "developer_1_role" to KEY_DEVELOPER_1_ROLE,
            "developer_2_name" to KEY_DEVELOPER_2_NAME,
            "developer_2_role" to KEY_DEVELOPER_2_ROLE
        )
    }
    
    /**
     * Data class for all app settings
     */
    data class AppSettings(
        val websiteUrl: String = DEFAULT_WEBSITE_URL,
        val supportEmail: String = DEFAULT_SUPPORT_EMAIL,
        val telegramUrl: String = DEFAULT_TELEGRAM_URL,
        val telegramHandle: String = DEFAULT_TELEGRAM_HANDLE,
        val telegramUrl2: String = DEFAULT_TELEGRAM_URL_2,
        val telegramHandle2: String = DEFAULT_TELEGRAM_HANDLE_2,
        val googlePlayUrl: String = DEFAULT_GOOGLE_PLAY_URL,
        val appStoreUrl: String = DEFAULT_APP_STORE_URL,
        val appVersion: String = DEFAULT_APP_VERSION,
        val appSlogan: String = DEFAULT_APP_SLOGAN,
        val appDescription: String = DEFAULT_APP_DESCRIPTION,
        val developer1Name: String = DEFAULT_DEVELOPER_1_NAME,
        val developer1Role: String = DEFAULT_DEVELOPER_1_ROLE,
        val developer2Name: String = DEFAULT_DEVELOPER_2_NAME,
        val developer2Role: String = DEFAULT_DEVELOPER_2_ROLE
    )
    
    /**
     * Flow of app settings
     */
    val settingsFlow: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            websiteUrl = prefs[KEY_WEBSITE_URL] ?: DEFAULT_WEBSITE_URL,
            supportEmail = prefs[KEY_SUPPORT_EMAIL] ?: DEFAULT_SUPPORT_EMAIL,
            telegramUrl = prefs[KEY_TELEGRAM_URL] ?: DEFAULT_TELEGRAM_URL,
            telegramHandle = prefs[KEY_TELEGRAM_HANDLE] ?: DEFAULT_TELEGRAM_HANDLE,
            telegramUrl2 = prefs[KEY_TELEGRAM_URL_2] ?: DEFAULT_TELEGRAM_URL_2,
            telegramHandle2 = prefs[KEY_TELEGRAM_HANDLE_2] ?: DEFAULT_TELEGRAM_HANDLE_2,
            googlePlayUrl = prefs[KEY_GOOGLE_PLAY_URL] ?: DEFAULT_GOOGLE_PLAY_URL,
            appStoreUrl = prefs[KEY_APP_STORE_URL] ?: DEFAULT_APP_STORE_URL,
            appVersion = prefs[KEY_APP_VERSION] ?: DEFAULT_APP_VERSION,
            appSlogan = prefs[KEY_APP_SLOGAN] ?: DEFAULT_APP_SLOGAN,
            appDescription = prefs[KEY_APP_DESCRIPTION] ?: DEFAULT_APP_DESCRIPTION,
            developer1Name = prefs[KEY_DEVELOPER_1_NAME] ?: DEFAULT_DEVELOPER_1_NAME,
            developer1Role = prefs[KEY_DEVELOPER_1_ROLE] ?: DEFAULT_DEVELOPER_1_ROLE,
            developer2Name = prefs[KEY_DEVELOPER_2_NAME] ?: DEFAULT_DEVELOPER_2_NAME,
            developer2Role = prefs[KEY_DEVELOPER_2_ROLE] ?: DEFAULT_DEVELOPER_2_ROLE
        )
    }
    
    /**
     * Get current settings synchronously
     */
    suspend fun getSettings(): AppSettings {
        return settingsFlow.first()
    }
    
    /**
     * Get last update timestamp
     */
    suspend fun getLastUpdated(): String? {
        return dataStore.data.first()[KEY_LAST_UPDATED]
    }
    
    /**
     * Update settings from server response
     * @param serverSettings Map of server key to value
     * @param lastUpdated Latest updated_at timestamp from server
     */
    suspend fun updateFromServer(serverSettings: Map<String, String>, lastUpdated: String) {
        dataStore.edit { prefs ->
            serverSettings.forEach { (serverKey, value) ->
                SERVER_KEY_MAP[serverKey]?.let { localKey ->
                    prefs[localKey] = value
                }
            }
            prefs[KEY_LAST_UPDATED] = lastUpdated
        }
        Timber.d("📱 App settings updated from server: ${serverSettings.size} keys")
    }
    
    /**
     * Check if settings need to be synced
     * @param serverLastUpdated Latest updated_at from server
     */
    suspend fun needsSync(serverLastUpdated: String): Boolean {
        val localLastUpdated = getLastUpdated() ?: return true
        return serverLastUpdated > localLastUpdated
    }
    
    /**
     * Clear all settings (for testing/debugging)
     */
    suspend fun clearSettings() {
        dataStore.edit { prefs ->
            SERVER_KEY_MAP.values.forEach { key ->
                prefs.remove(key)
            }
            prefs.remove(KEY_LAST_UPDATED)
        }
        Timber.d("📱 App settings cleared")
    }
}
