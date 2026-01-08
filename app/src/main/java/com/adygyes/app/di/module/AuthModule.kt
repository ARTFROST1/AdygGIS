package com.adygyes.app.di.module

import com.adygyes.app.data.local.preferences.SecureAuthPreferencesManager
import com.adygyes.app.data.remote.ProactiveTokenRefreshInterceptor
import com.adygyes.app.data.remote.TokenAuthenticator
import com.adygyes.app.data.remote.api.SupabaseAuthApi
import com.adygyes.app.data.remote.config.SupabaseConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Qualifier for Auth-specific Retrofit instance
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthRetrofit

/**
 * Qualifier for Auth-specific OkHttpClient
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthClient

/**
 * Qualifier for authenticated API client with token refresh
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthenticatedClient

/**
 * Hilt module for authentication-related dependencies
 * 
 * Provides:
 * - Auth API client (for login/register/refresh endpoints)
 * - Token authenticator (for automatic 401 retry)
 * - Proactive token refresh interceptor
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthModule {
    
    /**
     * Auth interceptor - adds apikey header for GoTrue API
     * Note: Auth endpoints use apikey header, not Bearer token
     */
    @Provides
    @Singleton
    fun provideAuthInterceptor(): Interceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        
        val newRequest = originalRequest.newBuilder()
            .header("apikey", SupabaseConfig.ANON_KEY)
            .header("Content-Type", "application/json")
            .build()
        
        chain.proceed(newRequest)
    }
    
    /**
     * OkHttp client for auth requests (login, register, refresh)
     */
    @Provides
    @Singleton
    @AuthClient
    fun provideAuthOkHttpClient(
        authInterceptor: Interceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    /**
     * Retrofit instance for Supabase Auth API
     */
    @Provides
    @Singleton
    @AuthRetrofit
    fun provideAuthRetrofit(
        @AuthClient okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(SupabaseConfig.BASE_URL + "/")
            .client(okHttpClient)
            .addConverterFactory(
                json.asConverterFactory("application/json".toMediaType())
            )
            .build()
    }
    
    /**
     * Supabase Auth API service
     */
    @Provides
    @Singleton
    fun provideSupabaseAuthApi(@AuthRetrofit retrofit: Retrofit): SupabaseAuthApi {
        return retrofit.create(SupabaseAuthApi::class.java)
    }
    
    /**
     * Token authenticator for automatic 401 retry with token refresh
     */
    @Provides
    @Singleton
    fun provideTokenAuthenticator(
        secureAuthPrefs: SecureAuthPreferencesManager,
        authApiProvider: dagger.Lazy<SupabaseAuthApi>
    ): TokenAuthenticator {
        return TokenAuthenticator(secureAuthPrefs, authApiProvider)
    }
    
    /**
     * Proactive token refresh interceptor
     */
    @Provides
    @Singleton
    fun provideProactiveTokenRefreshInterceptor(
        secureAuthPrefs: SecureAuthPreferencesManager,
        authApiProvider: dagger.Lazy<SupabaseAuthApi>
    ): ProactiveTokenRefreshInterceptor {
        return ProactiveTokenRefreshInterceptor(secureAuthPrefs, authApiProvider)
    }
}
