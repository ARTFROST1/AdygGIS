package com.adygyes.app.di.module

import com.adygyes.app.data.remote.api.SupabaseAuthApi
import com.adygyes.app.data.remote.config.SupabaseConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
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
 * Hilt module for authentication-related dependencies
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
     * OkHttp client for auth requests
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
}
