package com.adygyes.app.data.repository

import com.adygyes.app.BuildConfig
import com.adygyes.app.data.local.preferences.SecureAuthPreferencesManager
import com.adygyes.app.data.remote.api.SupabaseAuthApi
import com.adygyes.app.data.remote.dto.AuthErrorResponse
import com.adygyes.app.data.remote.dto.AuthResponse
import com.adygyes.app.data.remote.dto.PasswordResetRequest
import com.adygyes.app.data.remote.dto.RefreshTokenRequest
import com.adygyes.app.data.remote.dto.SignInRequest
import com.adygyes.app.data.remote.dto.SignUpRequest
import com.adygyes.app.data.remote.dto.SignUpResponse
import com.adygyes.app.data.remote.dto.UserMetadata
import com.adygyes.app.domain.model.AuthState
import com.adygyes.app.domain.model.User
import com.adygyes.app.domain.model.isAuthenticated
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing user authentication via Supabase GoTrue API.
 * 
 * Features:
 * - Email/password sign in and sign up
 * - Secure session persistence via EncryptedSharedPreferences
 * - Automatic token refresh with expiration tracking
 * - Password reset
 * 
 * Security improvements:
 * - Tokens are encrypted at rest using AndroidKeyStore
 * - Token expiration time is tracked for proactive refresh
 * - Automatic token refresh on 401 via TokenAuthenticator
 */
@Singleton
class AuthRepository @Inject constructor(
    private val authApi: SupabaseAuthApi,
    private val secureAuthPrefs: SecureAuthPreferencesManager,
    private val json: Json
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unknown)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    // Current access token for authenticated requests
    private var currentAccessToken: String? = null
    
    init {
        // Check for stored session on startup
        scope.launch {
            restoreSession()
        }

        // Keep repository state in sync with token updates (e.g., OkHttp refresh)
        scope.launch {
            secureAuthPrefs.accessTokenFlow.collect { newToken ->
                if (newToken.isNullOrBlank()) {
                    currentAccessToken = null
                    val current = _authState.value
                    if (current is AuthState.Authenticated) {
                        _authState.value = AuthState.Unauthenticated
                    }
                    return@collect
                }

                currentAccessToken = newToken
                val current = _authState.value
                if (current is AuthState.Authenticated && current.accessToken != newToken) {
                    val refreshedRefreshToken = secureAuthPrefs.getRefreshToken() ?: current.refreshToken
                    _authState.value = current.copy(
                        accessToken = newToken,
                        refreshToken = refreshedRefreshToken
                    )
                }
            }
        }
    }
    
    /**
     * Get current access token for authenticated API requests
     */
    fun getAccessToken(): String? = currentAccessToken
    
    /**
     * Synchronous check if user is currently authenticated.
     * Use this instead of authState.value.isAuthenticated to avoid timing issues.
     */
    fun isCurrentlyAuthenticated(): Boolean {
        return _authState.value.isAuthenticated
    }
    
    /**
     * Get current user synchronously (if authenticated).
     */
    fun getCurrentUser(): User? {
        val state = _authState.value
        return if (state is AuthState.Authenticated) state.user else null
    }
    
    /**
     * Restore session from secure stored preferences
     */
    private suspend fun restoreSession() {
        try {
            val storedData = secureAuthPrefs.getStoredUser()
            if (storedData != null) {
                Timber.d("Found stored session for user: ${storedData.email}")
                
                // Check if token is expired
                if (secureAuthPrefs.isTokenExpired()) {
                    Timber.d("Token is expired, attempting to refresh")
                    val result = refreshToken(storedData.refreshToken)
                    if (result.isFailure) {
                        Timber.w("Token refresh failed, clearing session")
                        secureAuthPrefs.clearSession()
                        _authState.value = AuthState.Unauthenticated
                    }
                } else if (secureAuthPrefs.shouldRefreshToken()) {
                    // Token will expire soon, refresh proactively
                    Timber.d("Token expiring soon, refreshing proactively")
                    val result = refreshToken(storedData.refreshToken)
                    if (result.isFailure) {
                        // Still use the old token if refresh fails but token is not yet expired
                        Timber.w("Proactive refresh failed, using existing token")
                        restoreFromStoredData(storedData)
                    }
                } else {
                    // Token is still valid
                    restoreFromStoredData(storedData)
                }
            } else {
                Timber.d("No stored session found")
                _authState.value = AuthState.Unauthenticated
            }
        } catch (e: Exception) {
            Timber.e(e, "Error restoring session")
            _authState.value = AuthState.Unauthenticated
        }
    }
    
    private fun restoreFromStoredData(storedData: SecureAuthPreferencesManager.StoredUserData) {
        val user = User(
            id = storedData.userId,
            email = storedData.email,
            displayName = storedData.displayName,
            avatarUrl = storedData.avatarUrl
        )
        currentAccessToken = storedData.accessToken
        _authState.value = AuthState.Authenticated(
            user = user,
            accessToken = storedData.accessToken,
            refreshToken = storedData.refreshToken
        )
        Timber.d("Session restored for user: ${user.email}")
    }
    
    /**
     * Sign in with email and password
     */
    suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            _authState.value = AuthState.Loading
            
            val normalizedEmail = email.trim().lowercase()
            val response = authApi.signIn(SignInRequest(normalizedEmail, password))
            
            if (response.isSuccessful && response.body() != null) {
                try {
                    val authResponse = response.body()!!
                    val user = handleAuthSuccess(authResponse)
                    Result.success(user)
                } catch (e: AuthException) {
                    val message = e.message ?: "Ошибка входа"
                    _authState.value = AuthState.Error(message)
                    Result.failure(e)
                } catch (e: kotlinx.serialization.SerializationException) {
                    // Response was 200 OK but body is an error
                    Timber.w(e, "Sign in response deserialization failed - likely error response")
                    val errorMessage = parseAuthError(response.errorBody()?.string() ?: response.raw().toString())
                    _authState.value = AuthState.Error(errorMessage)
                    Result.failure(AuthException(errorMessage))
                }
            } else {
                val errorMessage = parseAuthError(response.errorBody()?.string())
                _authState.value = AuthState.Error(errorMessage)
                Result.failure(AuthException(errorMessage))
            }
        } catch (e: kotlinx.serialization.SerializationException) {
            Timber.e(e, "Sign in response parsing failed")
            val message = "Ошибка обработки ответа сервера"
            _authState.value = AuthState.Error(message)
            Result.failure(AuthException(message))
        } catch (e: Exception) {
            Timber.e(e, "Sign in failed")
            val message = translateNetworkError(e)
            _authState.value = AuthState.Error(message)
            Result.failure(AuthException(message))
        }
    }
    
    /**
     * Sign up with email and password
     */
    suspend fun signUp(
        email: String, 
        password: String, 
        displayName: String?
    ): Result<User> {
        return try {
            _authState.value = AuthState.Loading
            
            val normalizedEmail = email.trim().lowercase()
            val request = SignUpRequest(
                email = normalizedEmail,
                password = password,
                data = displayName?.let { UserMetadata(displayName = it.trim()) }
            )
            
            val response = authApi.signUp(request)

            if (response.isSuccessful && response.body() != null) {
                try {
                    val signUpResponse = response.body()!!

                    val user = signUpResponseToUser(signUpResponse)

                    // /signup usually does NOT return a session when email confirmation is enabled.
                    // Treat this as a successful signup (email sent, user created) and ask user to confirm email.
                    val hasSession = !signUpResponse.accessToken.isNullOrBlank() && !signUpResponse.refreshToken.isNullOrBlank()
                    if (!hasSession) {
                        _authState.value = AuthState.Unauthenticated
                        return Result.success(user)
                    }

                    // Some configurations may issue a session on signup.
                    val authResponse = signUpResponseToAuthResponse(signUpResponse)
                    val authedUser = handleAuthSuccess(authResponse)
                    Result.success(authedUser)
                } catch (e: AuthException) {
                    val message = e.message ?: "Ошибка регистрации"
                    _authState.value = AuthState.Error(message)
                    Result.failure(e)
                } catch (e: kotlinx.serialization.SerializationException) {
                    // Response was 200 OK but body is an error (Supabase sometimes does this)
                    Timber.w(e, "Sign up response deserialization failed - likely error response")
                    val errorMessage = parseAuthError(response.errorBody()?.string() ?: response.raw().toString())
                    _authState.value = AuthState.Error(errorMessage)
                    Result.failure(AuthException(errorMessage))
                }
            } else {
                val errorMessage = parseAuthError(response.errorBody()?.string())
                _authState.value = AuthState.Error(errorMessage)
                Result.failure(AuthException(errorMessage))
            }
        } catch (e: kotlinx.serialization.SerializationException) {
            Timber.e(e, "Sign up response parsing failed")
            val message = "Ошибка обработки ответа сервера"
            _authState.value = AuthState.Error(message)
            Result.failure(AuthException(message))
        } catch (e: Exception) {
            Timber.e(e, "Sign up failed")
            val message = translateNetworkError(e)
            _authState.value = AuthState.Error(message)
            Result.failure(AuthException(message))
        }
    }
    
    /**
     * Sign out current user
     */
    suspend fun signOut(): Result<Unit> {
        return try {
            val token = currentAccessToken
            if (token != null) {
                // Try to invalidate token on server (ignore errors)
                try {
                    authApi.signOut("Bearer $token")
                } catch (e: Exception) {
                    Timber.w(e, "Server signout failed, continuing with local signout")
                }
            }
            
            // Clear local session
            secureAuthPrefs.clearSession()
            currentAccessToken = null
            _authState.value = AuthState.Unauthenticated
            
            Timber.d("User signed out successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Sign out failed")
            // Still clear local state even if server call failed
            secureAuthPrefs.clearSession()
            currentAccessToken = null
            _authState.value = AuthState.Unauthenticated
            Result.success(Unit)
        }
    }
    
    /**
     * Refresh access token using refresh token
     */
    suspend fun refreshToken(refreshToken: String? = null): Result<Unit> {
        return try {
            val token = refreshToken ?: secureAuthPrefs.getRefreshToken()
            if (token == null) {
                _authState.value = AuthState.Unauthenticated
                return Result.failure(AuthException("Сессия истекла. Войдите снова."))
            }
            
            val response = authApi.refreshToken(RefreshTokenRequest(token))
            
            if (response.isSuccessful && response.body() != null) {
                try {
                    handleAuthSuccess(response.body()!!)
                    Result.success(Unit)
                } catch (e: AuthException) {
                    // Token response is invalid -> clear session
                    secureAuthPrefs.clearSession()
                    currentAccessToken = null
                    _authState.value = AuthState.Unauthenticated
                    Result.failure(e)
                }
            } else {
                // Token invalid, clear session
                secureAuthPrefs.clearSession()
                currentAccessToken = null
                _authState.value = AuthState.Unauthenticated
                Result.failure(AuthException("Сессия истекла. Войдите снова."))
            }
        } catch (e: Exception) {
            Timber.e(e, "Token refresh failed")
            val isExpired = runCatching { secureAuthPrefs.isTokenExpired() }.getOrDefault(true)
            if (isExpired) {
                secureAuthPrefs.clearSession()
                currentAccessToken = null
                _authState.value = AuthState.Unauthenticated
                Result.failure(AuthException("Сессия истекла. Войдите снова."))
            } else {
                Result.failure(AuthException("Не удалось обновить сессию. Проверьте интернет."))
            }
        }
    }
    
    /**
     * Send password reset email
     */
    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            val normalizedEmail = email.trim().lowercase()
            val response = authApi.recoverPassword(PasswordResetRequest(normalizedEmail))
            
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMessage = parseAuthError(response.errorBody()?.string())
                Result.failure(AuthException(errorMessage))
            }
        } catch (e: Exception) {
            Timber.e(e, "Password reset failed")
            val message = translateNetworkError(e)
            Result.failure(AuthException(message))
        }
    }
    
    /**
     * Clear any error state
     */
    fun clearError() {
        val current = _authState.value
        if (current is AuthState.Error) {
            _authState.value = AuthState.Unauthenticated
        }
    }
    
    // ===== Private helpers =====
    
    private suspend fun handleAuthSuccess(authResponse: AuthResponse): User {
        val accessToken = authResponse.accessToken?.takeIf { it.isNotBlank() }
            ?: throw AuthException("Сервер не вернул access token")
        val refreshToken = authResponse.refreshToken?.takeIf { it.isNotBlank() }
            ?: throw AuthException("Сервер не вернул refresh token")

        val user = authResponseToUser(authResponse)
        
        // Calculate expiration time (Supabase returns seconds or timestamp)
        val expiresAt = authResponse.expiresAt 
            ?: (System.currentTimeMillis() / 1000 + (authResponse.expiresIn ?: 3600))
        
        // Save to secure preferences with expiration time
        secureAuthPrefs.saveSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAt = expiresAt,
            userId = user.id,
            email = user.email,
            displayName = user.displayName,
            avatarUrl = user.avatarUrl
        )
        
        // Update in-memory token
        currentAccessToken = accessToken
        
        // Update state
        _authState.value = AuthState.Authenticated(
            user = user,
            accessToken = accessToken,
            refreshToken = refreshToken
        )
        
        Timber.d("Auth success for user: ${user.email}, token expires at: ${java.util.Date(expiresAt * 1000)}")

        return user
    }
    
    private fun authResponseToUser(response: AuthResponse): User {
        val userDto = response.user ?: throw AuthException("Сервер не вернул данные пользователя")
        return User(
            id = userDto.id,
            email = userDto.email ?: "",
            displayName = userDto.userMetadata?.displayName,
            avatarUrl = userDto.userMetadata?.avatarUrl
        )
    }

    private fun signUpResponseToUser(response: SignUpResponse): User {
        val rawErrorMessage = response.errorDescription
            ?: response.msg
            ?: response.message
            ?: response.error
        if (!rawErrorMessage.isNullOrBlank()) {
            throw AuthException(translateAuthError(rawErrorMessage))
        }

        val userDto = response.user
        if (userDto != null) {
            return User(
                id = userDto.id,
                email = userDto.email ?: "",
                displayName = userDto.userMetadata?.displayName,
                avatarUrl = userDto.userMetadata?.avatarUrl
            )
        }

        val id = response.id
        if (id.isNullOrBlank()) {
            throw AuthException("Сервер не вернул данные пользователя")
        }

        return User(
            id = id,
            email = response.email ?: "",
            displayName = response.userMetadata?.displayName,
            avatarUrl = response.userMetadata?.avatarUrl
        )
    }

    private fun signUpResponseToAuthResponse(response: SignUpResponse): AuthResponse {
        val userDto = response.user ?: run {
            val id = response.id
                ?: throw AuthException("Сервер не вернул данные пользователя")

            com.adygyes.app.data.remote.dto.AuthUserDto(
                id = id,
                email = response.email,
                emailConfirmedAt = response.emailConfirmedAt,
                userMetadata = response.userMetadata,
                appMetadata = response.appMetadata,
                createdAt = response.createdAt,
                updatedAt = response.updatedAt
            )
        }

        return AuthResponse(
            accessToken = response.accessToken,
            refreshToken = response.refreshToken,
            tokenType = response.tokenType,
            expiresIn = response.expiresIn,
            expiresAt = response.expiresAt,
            user = userDto
        )
    }
    
    private fun parseAuthError(errorBody: String?): String {
        if (errorBody.isNullOrBlank()) {
            return "Неизвестная ошибка"
        }
        
        return try {
            val errorResponse = json.decodeFromString<AuthErrorResponse>(errorBody)
            val rawMessage = errorResponse.errorDescription 
                ?: errorResponse.msg 
                ?: errorResponse.message 
                ?: errorResponse.error
                ?: "Неизвестная ошибка"
            
            translateAuthError(rawMessage)
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse error response: $errorBody")
            "Ошибка сервера"
        }
    }
    
    private fun translateAuthError(message: String): String {
        return when {
            message.contains("Invalid login credentials", ignoreCase = true) -> 
                "Неверный email или пароль"
            message.contains("Email not confirmed", ignoreCase = true) -> 
                "Email не подтверждён. Проверьте почту."
            message.contains("User already registered", ignoreCase = true) ||
            message.contains("already been registered", ignoreCase = true) -> 
                "Пользователь с таким email уже зарегистрирован"
            message.contains("Password should be at least", ignoreCase = true) -> 
                "Пароль должен содержать минимум 6 символов"
            message.contains("rate limit", ignoreCase = true) ||
            message.contains("too many requests", ignoreCase = true) -> 
                "Слишком много попыток. Подождите немного."
            message.contains("invalid email", ignoreCase = true) -> 
                "Некорректный email"
            message.contains("signup is disabled", ignoreCase = true) ->
                "Регистрация временно недоступна"
            else -> message
        }
    }
    
    private fun translateNetworkError(e: Exception): String {
        return when {
            e.message?.contains("Unable to resolve host", ignoreCase = true) == true ||
            e.message?.contains("No address associated", ignoreCase = true) == true ->
                "Нет подключения к интернету"
            e.message?.contains("timeout", ignoreCase = true) == true ->
                "Превышено время ожидания. Проверьте подключение."
            else -> "Ошибка сети. Попробуйте позже."
        }
    }
}

/**
 * Exception for authentication errors
 */
class AuthException(message: String) : Exception(message)
