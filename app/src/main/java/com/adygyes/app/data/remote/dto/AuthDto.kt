package com.adygyes.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for email/password sign in
 */
@Serializable
data class SignInRequest(
    @SerialName("email")
    val email: String,
    @SerialName("password")
    val password: String
)

/**
 * Request body for email/password sign up
 */
@Serializable
data class SignUpRequest(
    @SerialName("email")
    val email: String,
    @SerialName("password")
    val password: String,
    @SerialName("data")
    val data: UserMetadata? = null
)

/**
 * User metadata for sign up (display_name, etc.)
 */
@Serializable
data class UserMetadata(
    @SerialName("display_name")
    val displayName: String? = null
)

/**
 * Request body for refreshing access token
 */
@Serializable
data class RefreshTokenRequest(
    @SerialName("refresh_token")
    val refreshToken: String
)

/**
 * Request body for password reset
 */
@Serializable
data class PasswordResetRequest(
    @SerialName("email")
    val email: String
)

/**
 * Response from Supabase Auth (sign in, sign up, token refresh)
 */
@Serializable
data class AuthResponse(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("refresh_token")
    val refreshToken: String,
    @SerialName("token_type")
    val tokenType: String? = null,
    @SerialName("expires_in")
    val expiresIn: Int? = null,
    @SerialName("expires_at")
    val expiresAt: Long? = null,
    @SerialName("user")
    val user: AuthUserDto
)

/**
 * User data from Supabase Auth
 */
@Serializable
data class AuthUserDto(
    @SerialName("id")
    val id: String,
    @SerialName("email")
    val email: String? = null,
    @SerialName("email_confirmed_at")
    val emailConfirmedAt: String? = null,
    @SerialName("user_metadata")
    val userMetadata: AuthUserMetadataDto? = null,
    @SerialName("app_metadata")
    val appMetadata: AppMetadataDto? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

/**
 * User metadata from auth response
 */
@Serializable
data class AuthUserMetadataDto(
    @SerialName("display_name")
    val displayName: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null
)

/**
 * App metadata from auth response
 */
@Serializable
data class AppMetadataDto(
    @SerialName("provider")
    val provider: String? = null,
    @SerialName("providers")
    val providers: List<String>? = null
)

/**
 * Error response from Supabase Auth
 */
@Serializable
data class AuthErrorResponse(
    @SerialName("error")
    val error: String? = null,
    @SerialName("error_description")
    val errorDescription: String? = null,
    @SerialName("msg")
    val msg: String? = null,
    @SerialName("message")
    val message: String? = null,
    @SerialName("code")
    val code: String? = null
)
