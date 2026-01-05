# 🔌 Интеграция Retrofit + Supabase для Kotlin

**Дата:** 5 января 2026  
**Версия:** 1.0  
**Статус:** План рефакторинга

---

## 📋 Содержание

1. [Зависимости](#зависимости)
2. [Конфигурация Supabase](#конфигурация-supabase)
3. [Retrofit API Service](#retrofit-api-service)
4. [Remote Data Source](#remote-data-source)
5. [Интеграция с Repository](#интеграция-с-repository)
6. [Чеклист реализации](#чеклист-реализации)

---

## 📦 Зависимости

### Добавить в `gradle/libs.versions.toml`

```toml
[versions]
# Existing
retrofit = "2.11.0"
okhttp = "4.12.0"
kotlinxSerialization = "1.7.3"

[libraries]
# Existing - убедиться что есть
retrofit-core = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-kotlinx-serialization = { group = "com.squareup.retrofit2", name = "converter-kotlinx-serialization", version.ref = "retrofit" }
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }
```

### Добавить в `app/build.gradle.kts`

```kotlin
dependencies {
    // Retrofit + OkHttp (если ещё нет)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
}
```

---

## ⚙️ Конфигурация Supabase

### 1. Создать `local.properties`

```properties
# local.properties (НЕ коммитить в git!)
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-anon-key
```

### 2. Создать `SupabaseConfig.kt`

```kotlin
// data/remote/config/SupabaseConfig.kt
package com.adygyes.app.data.remote.config

import com.adygyes.app.BuildConfig

/**
 * Supabase configuration for API access
 */
object SupabaseConfig {
    /**
     * Supabase project URL
     */
    val BASE_URL: String = BuildConfig.SUPABASE_URL
    
    /**
     * Supabase anon key for public read access
     * This key is safe to include in client apps (RLS protects data)
     */
    val ANON_KEY: String = BuildConfig.SUPABASE_ANON_KEY
    
    /**
     * REST API endpoint for attractions table
     */
    val ATTRACTIONS_ENDPOINT = "/rest/v1/attractions"
    
    /**
     * Required headers for Supabase REST API
     */
    object Headers {
        const val API_KEY = "apikey"
        const val AUTHORIZATION = "Authorization"
        const val CONTENT_TYPE = "Content-Type"
        const val PREFER = "Prefer"
    }
    
    /**
     * Header values
     */
    object HeaderValues {
        const val APPLICATION_JSON = "application/json"
        const val BEARER_PREFIX = "Bearer "
        const val RETURN_REPRESENTATION = "return=representation"
    }
}
```

### 3. Обновить `app/build.gradle.kts`

```kotlin
android {
    defaultConfig {
        // Read from local.properties
        val properties = java.util.Properties()
        val localPropsFile = rootProject.file("local.properties")
        if (localPropsFile.exists()) {
            properties.load(localPropsFile.inputStream())
        }
        
        buildConfigField(
            "String", 
            "SUPABASE_URL", 
            "\"${properties.getProperty("SUPABASE_URL", "")}\""
        )
        buildConfigField(
            "String", 
            "SUPABASE_ANON_KEY", 
            "\"${properties.getProperty("SUPABASE_ANON_KEY", "")}\""
        )
    }
    
    buildFeatures {
        buildConfig = true
    }
}
```

---

## 🌐 Retrofit API Service

### Создать `SupabaseApiService.kt`

```kotlin
// data/remote/api/SupabaseApiService.kt
package com.adygyes.app.data.remote.api

import com.adygyes.app.data.remote.dto.AttractionDto
import com.adygyes.app.data.remote.dto.SyncMetadataDto
import retrofit2.Response
import retrofit2.http.*

/**
 * Supabase REST API service for attractions
 */
interface SupabaseApiService {
    
    /**
     * Get all published attractions
     * RLS policy ensures only is_published=true are returned
     */
    @GET("rest/v1/attractions")
    suspend fun getAllAttractions(
        @Query("select") select: String = "*",
        @Query("order") order: String = "name.asc"
    ): Response<List<AttractionDto>>
    
    /**
     * Get attractions updated after specific timestamp (delta sync)
     * 
     * @param updatedAt ISO 8601 timestamp (e.g., "2025-01-01T00:00:00Z")
     */
    @GET("rest/v1/attractions")
    suspend fun getUpdatedAttractions(
        @Query("select") select: String = "*",
        @Query("updated_at") updatedAt: String, // e.g., "gt.2025-01-01T00:00:00Z"
        @Query("order") order: String = "updated_at.asc"
    ): Response<List<AttractionDto>>
    
    /**
     * Get single attraction by ID
     */
    @GET("rest/v1/attractions")
    suspend fun getAttractionById(
        @Query("id") id: String, // e.g., "eq.uuid-here"
        @Query("select") select: String = "*"
    ): Response<List<AttractionDto>>
    
    /**
     * Get tombstones (deleted/unpublished records) for sync
     */
    @GET("rest/v1/sync_metadata")
    suspend fun getDeletedAttractions(
        @Query("entity_type") entityType: String = "eq.attraction",
        @Query("deleted_at") deletedAt: String, // e.g., "gt.2025-01-01T00:00:00Z"
        @Query("select") select: String = "entity_id"
    ): Response<List<SyncMetadataDto>>
}
```

### Создать `SyncMetadataDto.kt`

```kotlin
// data/remote/dto/SyncMetadataDto.kt
package com.adygyes.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for sync_metadata table (tombstones)
 */
@Serializable
data class SyncMetadataDto(
    @SerialName("id")
    val id: String? = null,
    
    @SerialName("entity_type")
    val entityType: String,
    
    @SerialName("entity_id")
    val entityId: String,
    
    @SerialName("action")
    val action: String? = null, // "DELETE" or "UNPUBLISH"
    
    @SerialName("deleted_at")
    val deletedAt: String? = null
)
```

---

## 🏗️ OkHttp + Retrofit Setup

### Создать `NetworkModule.kt` (Hilt)

```kotlin
// di/module/NetworkModule.kt
package com.adygyes.app.di.module

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
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    /**
     * JSON parser configuration
     */
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = false
    }
    
    /**
     * Supabase auth interceptor - adds required headers
     */
    @Provides
    @Singleton
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
     * OkHttp client with Supabase configuration
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        supabaseInterceptor: Interceptor,
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
     * Retrofit instance for Supabase
     */
    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(SupabaseConfig.BASE_URL)
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
```

---

## 📡 Remote Data Source

### Создать `SupabaseRemoteDataSource.kt`

```kotlin
// data/remote/SupabaseRemoteDataSource.kt
package com.adygyes.app.data.remote

import com.adygyes.app.data.remote.api.SupabaseApiService
import com.adygyes.app.data.remote.dto.AttractionDto
import com.adygyes.app.data.remote.dto.SyncMetadataDto
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result wrapper for network operations
 */
sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(val message: String, val code: Int? = null) : NetworkResult<Nothing>()
}

/**
 * Remote data source for Supabase API
 */
@Singleton
class SupabaseRemoteDataSource @Inject constructor(
    private val apiService: SupabaseApiService
) {
    
    /**
     * Fetch all published attractions
     */
    suspend fun getAllAttractions(): NetworkResult<List<AttractionDto>> {
        return try {
            val response = apiService.getAllAttractions()
            
            if (response.isSuccessful) {
                val data = response.body() ?: emptyList()
                Timber.d("✅ Fetched ${data.size} attractions from Supabase")
                NetworkResult.Success(data)
            } else {
                Timber.e("❌ API error: ${response.code()} - ${response.message()}")
                NetworkResult.Error(response.message(), response.code())
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Network error fetching attractions")
            NetworkResult.Error(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Fetch attractions updated since timestamp (delta sync)
     * 
     * @param since ISO 8601 timestamp
     */
    suspend fun getUpdatedAttractions(since: String): NetworkResult<List<AttractionDto>> {
        return try {
            // Supabase filter format: gt.timestamp
            val response = apiService.getUpdatedAttractions(
                updatedAt = "gt.$since"
            )
            
            if (response.isSuccessful) {
                val data = response.body() ?: emptyList()
                Timber.d("✅ Fetched ${data.size} updated attractions since $since")
                NetworkResult.Success(data)
            } else {
                Timber.e("❌ API error: ${response.code()}")
                NetworkResult.Error(response.message(), response.code())
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Network error fetching updated attractions")
            NetworkResult.Error(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Fetch tombstones (deleted/unpublished records)
     * 
     * @param since ISO 8601 timestamp
     */
    suspend fun getDeletedAttractions(since: String): NetworkResult<List<String>> {
        return try {
            val response = apiService.getDeletedAttractions(
                deletedAt = "gt.$since"
            )
            
            if (response.isSuccessful) {
                val data = response.body()?.map { it.entityId } ?: emptyList()
                Timber.d("✅ Fetched ${data.size} deleted attraction IDs since $since")
                NetworkResult.Success(data)
            } else {
                Timber.e("❌ API error: ${response.code()}")
                NetworkResult.Error(response.message(), response.code())
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Network error fetching deleted attractions")
            NetworkResult.Error(e.message ?: "Unknown error")
        }
    }
}
```

---

## 🔗 Интеграция с Repository

### Обновить `AttractionRepositoryImpl.kt`

```kotlin
// data/repository/AttractionRepositoryImpl.kt
// ДОБАВИТЬ параметр в конструктор:

@Singleton
class AttractionRepositoryImpl @Inject constructor(
    private val attractionDao: AttractionDao,
    private val preferencesManager: PreferencesManager,
    private val imageCacheManager: ImageCacheManager,
    private val remoteDataSource: SupabaseRemoteDataSource, // 🆕 ДОБАВИТЬ
    @ApplicationContext private val context: Context
) : AttractionRepository {
    
    // ... существующий код ...
    
    /**
     * 🆕 ДОБАВИТЬ: Проверка доступности сети
     */
    private suspend fun isNetworkAvailable(): Boolean {
        // TODO: Implement network check
        return true
    }
    
    /**
     * 🆕 ДОБАВИТЬ: Синхронизация с Supabase
     */
    suspend fun syncWithSupabase(): SyncResult {
        // Реализация в 04_SYNC_SERVICE.md
    }
}
```

---

## ✅ Чеклист реализации

### Файлы для создания

| Файл | Статус | Описание |
|------|--------|----------|
| `data/remote/config/SupabaseConfig.kt` | 📋 TODO | Конфигурация Supabase |
| `data/remote/api/SupabaseApiService.kt` | 📋 TODO | Retrofit API interface |
| `data/remote/dto/SyncMetadataDto.kt` | 📋 TODO | DTO для tombstones |
| `data/remote/SupabaseRemoteDataSource.kt` | 📋 TODO | Remote data source |
| `di/module/NetworkModule.kt` | 📋 TODO | Hilt DI module |

### Файлы для изменения

| Файл | Изменение |
|------|-----------|
| `app/build.gradle.kts` | Добавить BuildConfig fields |
| `gradle/libs.versions.toml` | Проверить Retrofit deps |
| `local.properties` | Добавить SUPABASE_* |
| `data/repository/AttractionRepositoryImpl.kt` | Добавить remoteDataSource |

---

## 📋 Следующий шаг

После настройки Retrofit → [04_SYNC_SERVICE.md](04_SYNC_SERVICE.md) — Сервис синхронизации

