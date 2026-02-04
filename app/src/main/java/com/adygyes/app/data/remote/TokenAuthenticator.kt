package com.adygyes.app.data.remote

import com.adygyes.app.data.local.preferences.SecureAuthPreferencesManager
import com.adygyes.app.data.remote.api.SupabaseAuthApi
import com.adygyes.app.data.remote.config.SupabaseConfig
import com.adygyes.app.data.remote.dto.RefreshTokenRequest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp Authenticator that automatically refreshes expired tokens.
 * 
 * This authenticator is triggered when the server returns a 401 Unauthorized response.
 * It attempts to refresh the access token using the stored refresh token.
 * 
 * Best practices implemented:
 * - Thread-safe token refresh using Mutex
 * - Prevents infinite retry loops
 * - Updates secure storage with new tokens
 * - Graceful fallback on failure
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val secureAuthPrefs: SecureAuthPreferencesManager,
    private val authApiProvider: dagger.Lazy<SupabaseAuthApi>
) : Authenticator {
    
    private val mutex = Mutex()
    
    // Track retry count to prevent infinite loops
    private companion object {
        const val MAX_RETRY_COUNT = 1
        const val RETRY_COUNT_HEADER = "X-Auth-Retry-Count"
    }
    
    override fun authenticate(route: Route?, response: Response): Request? {
        val retryCount = response.request.header(RETRY_COUNT_HEADER)?.toIntOrNull() ?: 0
        
        // Prevent infinite retry loops
        if (retryCount >= MAX_RETRY_COUNT) {
            Timber.w("Max retry count reached, not retrying authentication")
            return null
        }
        
        // Check if this request already has a Bearer token (user auth request)
        val authHeader = response.request.header(SupabaseConfig.Headers.AUTHORIZATION)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // This is not an authenticated request, don't retry
            return null
        }

        // Never attempt refresh for anon-key requests (public access)
        if (authHeader == "Bearer ${SupabaseConfig.ANON_KEY}") {
            return null
        }
        
        // Don't retry if the original request was the token endpoint itself
        if (response.request.url.encodedPath.contains("auth/v1/token")) {
            Timber.w("Token refresh request failed, clearing session")
            runBlocking { secureAuthPrefs.clearSession() }
            return null
        }
        
        return runBlocking {
            mutex.withLock {
                // Double-check: maybe another thread already refreshed the token
                val currentToken = secureAuthPrefs.getAccessToken()
                val originalToken = authHeader.removePrefix("Bearer ")
                
                if (currentToken != null && currentToken != originalToken) {
                    // Token was already refreshed by another thread
                    Timber.d("Token already refreshed by another thread, retrying with new token")
                    return@runBlocking response.request.newBuilder()
                        .header(SupabaseConfig.Headers.AUTHORIZATION, "Bearer $currentToken")
                        .header(RETRY_COUNT_HEADER, (retryCount + 1).toString())
                        .build()
                }
                
                // Attempt to refresh the token
                val refreshedRequest = attemptTokenRefresh(response.request, retryCount)
                refreshedRequest
            }
        }
    }
    
    private suspend fun attemptTokenRefresh(originalRequest: Request, retryCount: Int): Request? {
        val refreshToken = secureAuthPrefs.getRefreshToken()
        
        if (refreshToken == null) {
            Timber.w("No refresh token available, cannot refresh session")
            return null
        }
        
        return try {
            Timber.d("Attempting to refresh access token...")
            
            val authApi = authApiProvider.get()
            val response = authApi.refreshToken(RefreshTokenRequest(refreshToken))
            
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!

                val newAccessToken = authResponse.accessToken
                val newRefreshToken = authResponse.refreshToken
                if (newAccessToken.isNullOrBlank() || newRefreshToken.isNullOrBlank()) {
                    Timber.w("Token refresh response missing tokens; clearing session")
                    secureAuthPrefs.clearSession()
                    return null
                }
                
                // Calculate expiration time
                val expiresAt = authResponse.expiresAt 
                    ?: (System.currentTimeMillis() / 1000 + (authResponse.expiresIn ?: 3600))
                
                // Update secure storage with new tokens
                secureAuthPrefs.updateTokens(
                    accessToken = newAccessToken,
                    refreshToken = newRefreshToken,
                    expiresAt = expiresAt
                )
                
                Timber.d("Token refreshed successfully")
                
                // Retry the original request with the new access token
                originalRequest.newBuilder()
                    .header(SupabaseConfig.Headers.AUTHORIZATION, "Bearer $newAccessToken")
                    .header(RETRY_COUNT_HEADER, (retryCount + 1).toString())
                    .build()
            } else {
                Timber.w("Token refresh failed with status: ${response.code()}")
                // Clear session on refresh failure
                secureAuthPrefs.clearSession()
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Token refresh failed with exception")
            // Don't clear session on network errors - might be temporary
            null
        }
    }
}

/**
 * Interceptor that proactively refreshes tokens before they expire.
 * This prevents 401 errors by refreshing the token early.
 */
@Singleton
class ProactiveTokenRefreshInterceptor @Inject constructor(
    private val secureAuthPrefs: SecureAuthPreferencesManager,
    private val authApiProvider: dagger.Lazy<SupabaseAuthApi>
) : okhttp3.Interceptor {
    
    private val refreshMutex = Mutex()
    
    override fun intercept(chain: okhttp3.Interceptor.Chain): Response {
        val request = chain.request()
        
        // Only check for authenticated requests
        val authHeader = request.header(SupabaseConfig.Headers.AUTHORIZATION)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return chain.proceed(request)
        }

        // Skip public/anon requests
        if (authHeader == "Bearer ${SupabaseConfig.ANON_KEY}") {
            return chain.proceed(request)
        }
        
        // Skip all auth endpoints (including token refresh) to avoid recursion
        if (request.url.encodedPath.contains("auth/v1")) {
            return chain.proceed(request)
        }
        
        // Check if token should be refreshed proactively
        val shouldRefresh = runBlocking { secureAuthPrefs.shouldRefreshToken() }
        
        if (shouldRefresh) {
            runBlocking {
                refreshMutex.withLock {
                    // Double-check after acquiring lock
                    if (secureAuthPrefs.shouldRefreshToken()) {
                        proactivelyRefreshToken()
                    }
                }
            }
        }
        
        // Get the current token (might have been refreshed)
        val currentToken = runBlocking { secureAuthPrefs.getAccessToken() }
        
        return if (currentToken != null && currentToken != authHeader.removePrefix("Bearer ")) {
            // Token was refreshed, update the request
            val newRequest = request.newBuilder()
                .header(SupabaseConfig.Headers.AUTHORIZATION, "Bearer $currentToken")
                .build()
            chain.proceed(newRequest)
        } else {
            chain.proceed(request)
        }
    }
    
    private suspend fun proactivelyRefreshToken() {
        val refreshToken = secureAuthPrefs.getRefreshToken() ?: return
        
        try {
            Timber.d("Proactively refreshing token before expiration...")
            
            val authApi = authApiProvider.get()
            val response = authApi.refreshToken(RefreshTokenRequest(refreshToken))
            
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!

                val newAccessToken = authResponse.accessToken
                val newRefreshToken = authResponse.refreshToken
                if (newAccessToken.isNullOrBlank() || newRefreshToken.isNullOrBlank()) {
                    Timber.w("Proactive token refresh response missing tokens")
                    return
                }
                
                val expiresAt = authResponse.expiresAt 
                    ?: (System.currentTimeMillis() / 1000 + (authResponse.expiresIn ?: 3600))
                
                secureAuthPrefs.updateTokens(
                    accessToken = newAccessToken,
                    refreshToken = newRefreshToken,
                    expiresAt = expiresAt
                )
                
                Timber.d("Proactive token refresh successful")
            } else {
                Timber.w("Proactive token refresh failed: ${response.code()}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Proactive token refresh failed with exception")
        }
    }
}
