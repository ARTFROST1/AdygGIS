package com.adygyes.app.data.remote.api

import com.adygyes.app.data.remote.dto.AuthResponse
import com.adygyes.app.data.remote.dto.PasswordResetRequest
import com.adygyes.app.data.remote.dto.RefreshTokenRequest
import com.adygyes.app.data.remote.dto.SignInRequest
import com.adygyes.app.data.remote.dto.SignUpRequest
import com.adygyes.app.data.remote.dto.SignUpResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Header

/**
 * Supabase GoTrue Auth API
 * 
 * Endpoints:
 * - POST /auth/v1/token?grant_type=password - Sign in with email/password
 * - POST /auth/v1/signup - Sign up with email/password
 * - POST /auth/v1/token?grant_type=refresh_token - Refresh access token
 * - POST /auth/v1/logout - Sign out
 * - POST /auth/v1/recover - Password reset email
 */
interface SupabaseAuthApi {
    
    /**
     * Sign in with email and password
     */
    @POST("auth/v1/token?grant_type=password")
    suspend fun signIn(
        @Body request: SignInRequest
    ): Response<AuthResponse>
    
    /**
     * Sign up with email and password
     */
    @POST("auth/v1/signup")
    suspend fun signUp(
        @Body request: SignUpRequest
    ): Response<SignUpResponse>
    
    /**
     * Refresh access token using refresh token
     */
    @POST("auth/v1/token?grant_type=refresh_token")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): Response<AuthResponse>
    
    /**
     * Sign out (invalidate refresh token)
     * Requires Authorization header with access token
     */
    @POST("auth/v1/logout")
    suspend fun signOut(
        @Header("Authorization") authorization: String
    ): Response<Unit>
    
    /**
     * Send password reset email
     */
    @POST("auth/v1/recover")
    suspend fun recoverPassword(
        @Body request: PasswordResetRequest
    ): Response<Unit>
}
