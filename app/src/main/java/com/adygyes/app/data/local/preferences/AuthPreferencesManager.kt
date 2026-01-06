package com.adygyes.app.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

/**
 * Manages authentication token persistence using DataStore
 * Stores access_token, refresh_token securely
 */
@Singleton
class AuthPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private object Keys {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val USER_ID = stringPreferencesKey("user_id")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_DISPLAY_NAME = stringPreferencesKey("user_display_name")
        val USER_AVATAR_URL = stringPreferencesKey("user_avatar_url")
    }
    
    /**
     * Save auth session to preferences
     */
    suspend fun saveSession(
        accessToken: String,
        refreshToken: String,
        userId: String,
        email: String,
        displayName: String?,
        avatarUrl: String?
    ) {
        context.authDataStore.edit { prefs ->
            prefs[Keys.ACCESS_TOKEN] = accessToken
            prefs[Keys.REFRESH_TOKEN] = refreshToken
            prefs[Keys.USER_ID] = userId
            prefs[Keys.USER_EMAIL] = email

            // Prevent stale optional fields when switching accounts.
            prefs.remove(Keys.USER_DISPLAY_NAME)
            prefs.remove(Keys.USER_AVATAR_URL)
            displayName?.let { prefs[Keys.USER_DISPLAY_NAME] = it }
            avatarUrl?.let { prefs[Keys.USER_AVATAR_URL] = it }
        }
    }
    
    /**
     * Clear auth session from preferences
     */
    suspend fun clearSession() {
        context.authDataStore.edit { prefs ->
            prefs.remove(Keys.ACCESS_TOKEN)
            prefs.remove(Keys.REFRESH_TOKEN)
            prefs.remove(Keys.USER_ID)
            prefs.remove(Keys.USER_EMAIL)
            prefs.remove(Keys.USER_DISPLAY_NAME)
            prefs.remove(Keys.USER_AVATAR_URL)
        }
    }
    
    /**
     * Get stored access token
     */
    suspend fun getAccessToken(): String? {
        return context.authDataStore.data.firstOrNull()?.get(Keys.ACCESS_TOKEN)
    }
    
    /**
     * Get stored refresh token
     */
    suspend fun getRefreshToken(): String? {
        return context.authDataStore.data.firstOrNull()?.get(Keys.REFRESH_TOKEN)
    }
    
    /**
     * Flow of access token changes
     */
    val accessTokenFlow: Flow<String?> = context.authDataStore.data.map { prefs ->
        prefs[Keys.ACCESS_TOKEN]
    }
    
    /**
     * Get stored user data as a map
     */
    suspend fun getStoredUser(): StoredUserData? {
        val prefs = context.authDataStore.data.firstOrNull() ?: return null
        val userId = prefs[Keys.USER_ID] ?: return null
        val email = prefs[Keys.USER_EMAIL] ?: return null
        val accessToken = prefs[Keys.ACCESS_TOKEN] ?: return null
        val refreshToken = prefs[Keys.REFRESH_TOKEN] ?: return null
        
        return StoredUserData(
            userId = userId,
            email = email,
            displayName = prefs[Keys.USER_DISPLAY_NAME],
            avatarUrl = prefs[Keys.USER_AVATAR_URL],
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }
    
    data class StoredUserData(
        val userId: String,
        val email: String,
        val displayName: String?,
        val avatarUrl: String?,
        val accessToken: String,
        val refreshToken: String
    )
}
