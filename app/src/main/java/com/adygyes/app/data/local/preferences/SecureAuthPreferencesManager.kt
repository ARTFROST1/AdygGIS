package com.adygyes.app.data.local.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure authentication token storage using EncryptedSharedPreferences.
 * 
 * Security features:
 * - AES-256 encryption for all stored tokens
 * - AndroidKeyStore-backed MasterKey
 * - Automatic token expiration tracking
 * - Proactive token refresh support
 * 
 * Best practices implemented:
 * - Tokens are encrypted at rest
 * - Token expiration is stored to enable proactive refresh
 * - Clear separation of concerns from plain DataStore
 */
@Singleton
class SecureAuthPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private object Keys {
        const val PREFS_FILE = "secure_auth_prefs"
        const val ACCESS_TOKEN = "access_token"
        const val REFRESH_TOKEN = "refresh_token"
        const val TOKEN_EXPIRES_AT = "token_expires_at"
        const val USER_ID = "user_id"
        const val USER_EMAIL = "user_email"
        const val USER_DISPLAY_NAME = "user_display_name"
        const val USER_AVATAR_URL = "user_avatar_url"
    }
    
    // Buffer time before expiration to trigger refresh (5 minutes)
    companion object {
        const val TOKEN_REFRESH_BUFFER_MS = 5 * 60 * 1000L
    }
    
    private val _accessTokenFlow = MutableStateFlow<String?>(null)
    val accessTokenFlow: Flow<String?> = _accessTokenFlow.asStateFlow()

    @Volatile
    private var encryptionAvailable: Boolean = true

    @Volatile
    private var memoryAccessToken: String? = null

    @Volatile
    private var memoryRefreshToken: String? = null

    @Volatile
    private var memoryTokenExpiresAtMs: Long = 0L

    @Volatile
    private var memoryUserId: String? = null

    @Volatile
    private var memoryUserEmail: String? = null

    @Volatile
    private var memoryUserDisplayName: String? = null

    @Volatile
    private var memoryUserAvatarUrl: String? = null
    
    private val encryptedPrefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            EncryptedSharedPreferences.create(
                context,
                Keys.PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            encryptionAvailable = false
            Timber.e(e, "Failed to create EncryptedSharedPreferences; session will be stored in-memory only")
            // IMPORTANT: do NOT fall back to plaintext persistence for tokens.
            // Return a prefs instance only to satisfy API usage; it won't be used for sensitive reads/writes.
            context.getSharedPreferences(Keys.PREFS_FILE, Context.MODE_PRIVATE)
        }
    }
    
    init {
        // Initialize flow with current token value
        _accessTokenFlow.value = if (encryptionAvailable) {
            encryptedPrefs.getString(Keys.ACCESS_TOKEN, null)
        } else {
            memoryAccessToken
        }
    }
    
    /**
     * Save auth session securely with token expiration time
     */
    suspend fun saveSession(
        accessToken: String,
        refreshToken: String,
        expiresAt: Long, // Unix timestamp in seconds when token expires
        userId: String,
        email: String,
        displayName: String?,
        avatarUrl: String?
    ) = withContext(Dispatchers.IO) {
        if (encryptionAvailable) {
            encryptedPrefs.edit().apply {
                putString(Keys.ACCESS_TOKEN, accessToken)
                putString(Keys.REFRESH_TOKEN, refreshToken)
                putLong(Keys.TOKEN_EXPIRES_AT, expiresAt * 1000) // Convert to milliseconds
                putString(Keys.USER_ID, userId)
                putString(Keys.USER_EMAIL, email)

                // Clear optional fields first to prevent stale data
                remove(Keys.USER_DISPLAY_NAME)
                remove(Keys.USER_AVATAR_URL)
                displayName?.let { putString(Keys.USER_DISPLAY_NAME, it) }
                avatarUrl?.let { putString(Keys.USER_AVATAR_URL, it) }

                apply()
            }
            _accessTokenFlow.value = accessToken
            Timber.d("Session saved securely, expires at: ${java.util.Date(expiresAt * 1000)}")
        } else {
            memoryAccessToken = accessToken
            memoryRefreshToken = refreshToken
            memoryTokenExpiresAtMs = expiresAt * 1000
            memoryUserId = userId
            memoryUserEmail = email
            memoryUserDisplayName = displayName
            memoryUserAvatarUrl = avatarUrl
            _accessTokenFlow.value = accessToken
            Timber.w("Session stored in-memory only (encryption unavailable)")
        }
    }
    
    /**
     * Clear auth session from secure storage
     */
    suspend fun clearSession() = withContext(Dispatchers.IO) {
        if (encryptionAvailable) {
            encryptedPrefs.edit().clear().apply()
        }
        memoryAccessToken = null
        memoryRefreshToken = null
        memoryTokenExpiresAtMs = 0L
        memoryUserId = null
        memoryUserEmail = null
        memoryUserDisplayName = null
        memoryUserAvatarUrl = null
        _accessTokenFlow.value = null
        Timber.d("Session cleared from secure storage")
    }
    
    /**
     * Get stored access token
     */
    suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        if (encryptionAvailable) {
            encryptedPrefs.getString(Keys.ACCESS_TOKEN, null)
        } else {
            memoryAccessToken
        }
    }
    
    /**
     * Get stored refresh token
     */
    suspend fun getRefreshToken(): String? = withContext(Dispatchers.IO) {
        if (encryptionAvailable) {
            encryptedPrefs.getString(Keys.REFRESH_TOKEN, null)
        } else {
            memoryRefreshToken
        }
    }
    
    /**
     * Get token expiration timestamp in milliseconds
     */
    suspend fun getTokenExpiresAt(): Long = withContext(Dispatchers.IO) {
        if (encryptionAvailable) {
            encryptedPrefs.getLong(Keys.TOKEN_EXPIRES_AT, 0L)
        } else {
            memoryTokenExpiresAtMs
        }
    }
    
    /**
     * Check if token needs to be refreshed (expires within buffer time)
     */
    suspend fun shouldRefreshToken(): Boolean = withContext(Dispatchers.IO) {
        val expiresAt = getTokenExpiresAt()
        if (expiresAt == 0L) return@withContext false
        
        val now = System.currentTimeMillis()
        val shouldRefresh = (expiresAt - now) < TOKEN_REFRESH_BUFFER_MS
        
        if (shouldRefresh) {
            Timber.d("Token should be refreshed. Expires in ${(expiresAt - now) / 1000}s")
        }
        
        shouldRefresh
    }
    
    /**
     * Check if token is expired
     */
    suspend fun isTokenExpired(): Boolean = withContext(Dispatchers.IO) {
        val expiresAt = getTokenExpiresAt()
        if (expiresAt == 0L) return@withContext true
        
        System.currentTimeMillis() > expiresAt
    }
    
    /**
     * Get stored user data
     */
    suspend fun getStoredUser(): StoredUserData? = withContext(Dispatchers.IO) {
        if (encryptionAvailable) {
            val userId = encryptedPrefs.getString(Keys.USER_ID, null) ?: return@withContext null
            val email = encryptedPrefs.getString(Keys.USER_EMAIL, null) ?: return@withContext null
            val accessToken = encryptedPrefs.getString(Keys.ACCESS_TOKEN, null) ?: return@withContext null
            val refreshToken = encryptedPrefs.getString(Keys.REFRESH_TOKEN, null) ?: return@withContext null
            val expiresAt = encryptedPrefs.getLong(Keys.TOKEN_EXPIRES_AT, 0L)

            StoredUserData(
                userId = userId,
                email = email,
                displayName = encryptedPrefs.getString(Keys.USER_DISPLAY_NAME, null),
                avatarUrl = encryptedPrefs.getString(Keys.USER_AVATAR_URL, null),
                accessToken = accessToken,
                refreshToken = refreshToken,
                tokenExpiresAt = expiresAt
            )
        } else {
            val userId = memoryUserId ?: return@withContext null
            val email = memoryUserEmail ?: return@withContext null
            val accessToken = memoryAccessToken ?: return@withContext null
            val refreshToken = memoryRefreshToken ?: return@withContext null
            val expiresAt = memoryTokenExpiresAtMs

            StoredUserData(
                userId = userId,
                email = email,
                displayName = memoryUserDisplayName,
                avatarUrl = memoryUserAvatarUrl,
                accessToken = accessToken,
                refreshToken = refreshToken,
                tokenExpiresAt = expiresAt
            )
        }
    }
    
    /**
     * Update only the tokens (after refresh)
     */
    suspend fun updateTokens(
        accessToken: String,
        refreshToken: String,
        expiresAt: Long
    ) = withContext(Dispatchers.IO) {
        if (encryptionAvailable) {
            encryptedPrefs.edit().apply {
                putString(Keys.ACCESS_TOKEN, accessToken)
                putString(Keys.REFRESH_TOKEN, refreshToken)
                putLong(Keys.TOKEN_EXPIRES_AT, expiresAt * 1000)
                apply()
            }
            _accessTokenFlow.value = accessToken
            Timber.d("Tokens updated securely, new expiration: ${java.util.Date(expiresAt * 1000)}")
        } else {
            memoryAccessToken = accessToken
            memoryRefreshToken = refreshToken
            memoryTokenExpiresAtMs = expiresAt * 1000
            _accessTokenFlow.value = accessToken
            Timber.w("Tokens updated in-memory only (encryption unavailable)")
        }
    }
    
    /**
     * Stored user data with token expiration
     */
    data class StoredUserData(
        val userId: String,
        val email: String,
        val displayName: String?,
        val avatarUrl: String?,
        val accessToken: String,
        val refreshToken: String,
        val tokenExpiresAt: Long // Unix timestamp in milliseconds
    )
}
