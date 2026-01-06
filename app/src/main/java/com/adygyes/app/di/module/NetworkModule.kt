package com.adygyes.app.di.module

import com.adygyes.app.BuildConfig
import com.adygyes.app.data.remote.api.SupabaseApiService
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
 * Qualifier for Supabase-specific OkHttpClient
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SupabaseClient

/**
 * Qualifier for Supabase auth interceptor
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SupabaseInterceptor

/**
 * Hilt module for network-related dependencies (Retrofit, OkHttp, Supabase)
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    /**
     * JSON parser configuration for kotlinx.serialization
     */
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true  // Ignore unknown fields from API
        isLenient = true          // Allow lenient parsing
        coerceInputValues = true  // Coerce null to default values
        encodeDefaults = false    // Don't encode default values
    }
    
    /**
     * Supabase auth interceptor - adds required headers for API authentication
     */
    @Provides
    @Singleton
    @SupabaseInterceptor
    fun provideSupabaseInterceptor(): Interceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        
        val newRequest = originalRequest.newBuilder()
            .header(SupabaseConfig.Headers.API_KEY, SupabaseConfig.ANON_KEY)
            .header(
                SupabaseConfig.Headers.AUTHORIZATION,
                "${SupabaseConfig.HeaderValues.BEARER_PREFIX}${SupabaseConfig.ANON_KEY}"
            )
            .header(
                SupabaseConfig.Headers.CONTENT_TYPE,
                SupabaseConfig.HeaderValues.APPLICATION_JSON
            )
            .build()
        
        chain.proceed(newRequest)
    }
    
    /**
     * Logging interceptor for debug builds
     */
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }
    
    /**
     * OkHttp client configured for Supabase API
     */
    @Provides
    @Singleton
    @SupabaseClient
    fun provideSupabaseOkHttpClient(
        @SupabaseInterceptor supabaseInterceptor: Interceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(supabaseInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    /**
     * Retrofit instance for Supabase REST API
     */
    @Provides
    @Singleton
    fun provideRetrofit(
        @SupabaseClient okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(SupabaseConfig.BASE_URL + "/") // Ensure trailing slash
            .client(okHttpClient)
            .addConverterFactory(
                json.asConverterFactory("application/json".toMediaType())
            )
            .build()
    }
    
    /**
     * Supabase API service
     */
    @Provides
    @Singleton
    fun provideSupabaseApiService(retrofit: Retrofit): SupabaseApiService {
        return retrofit.create(SupabaseApiService::class.java)
    }
}
