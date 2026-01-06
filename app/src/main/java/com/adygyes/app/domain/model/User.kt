package com.adygyes.app.domain.model

/**
 * User profile information
 */
data class User(
    val id: String,
    val email: String,
    val displayName: String?,
    val avatarUrl: String?
)

/**
 * Current authentication state
 */
sealed class AuthState {
    /** Initial state, checking session */
    data object Unknown : AuthState()
    
    /** Loading - auth operation in progress */
    data object Loading : AuthState()
    
    /** User is authenticated */
    data class Authenticated(
        val user: User,
        val accessToken: String,
        val refreshToken: String
    ) : AuthState()
    
    /** User is not authenticated */
    data object Unauthenticated : AuthState()
    
    /** Authentication error */
    data class Error(val message: String) : AuthState()
}

/**
 * Check if user is authenticated
 */
val AuthState.isAuthenticated: Boolean
    get() = this is AuthState.Authenticated

/**
 * Get current user if authenticated
 */
val AuthState.currentUser: User?
    get() = (this as? AuthState.Authenticated)?.user

/**
 * Get access token if authenticated
 */
val AuthState.accessToken: String?
    get() = (this as? AuthState.Authenticated)?.accessToken
