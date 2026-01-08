# 🔄 Сервис синхронизации Kotlin ↔ Supabase

**Дата:** 6 января 2026  
**Версия:** 1.1  
**Статус:** ✅ Core реализован (актуализировано по коду)

---

## 📋 Содержание

1. [Архитектура синхронизации](#архитектура-синхронизации)
2. [SyncService](#syncservice)
3. [SyncManager](#syncmanager)
4. [Network Monitor](#network-monitor)
5. [Интеграция с Repository](#интеграция-с-repository)
6. [Чеклист реализации](#чеклист-реализации)

---

## 🏗️ Архитектура синхронизации

### Стратегия: Offline-First с Delta Sync

```
┌─────────────────────────────────────────────────────────────────────┐
│                         APP STARTUP FLOW                             │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   App Launch                                                         │
│       ↓                                                              │
│   Load from Room DB (instant)                                        │
│       ↓                                                              │
│   Display UI immediately                                             │
│       ↓                                                              │
│   Check network status                                               │
│       ↓                                                              │
│   ┌─────────────┐    ┌──────────────┐                               │
│   │   Online    │    │   Offline    │                               │
│   └──────┬──────┘    └──────┬───────┘                               │
│          ↓                  ↓                                        │
│   Fetch delta from    Continue with                                  │
│   Supabase            cached data                                    │
│          ↓                                                           │
│   Merge & update                                                     │
│   Room DB                                                            │
│          ↓                                                           │
│   Update UI                                                          │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Принципы:
1. **Cache-first** — всегда сначала читаем из Room
2. **Background sync** — синхронизация не блокирует UI
3. **Delta updates** — только изменения с последней синхронизации
4. **Tombstones** — отслеживание удалённых записей
5. **Conflict resolution** — Server wins (сервер — источник истины)

---

## 🔧 SyncService

Фактическая реализация находится в:
- `app/src/main/java/com/adygyes/app/data/sync/SyncService.kt`
- `app/src/main/java/com/adygyes/app/data/sync/SyncManager.kt`
- `app/src/main/java/com/adygyes/app/data/sync/NetworkMonitor.kt`
- `app/src/main/java/com/adygyes/app/data/remote/*` (Supabase API)

### Создать `SyncService.kt`

> ⚠️ Актуализация: файл уже реализован в коде. Блок ниже оставлен как справочный фрагмент.

## ✅ Актуально по коду (2026-01-08): синхронизация отзывов

Помимо синхронизации `attractions`, `SyncService` интегрирован с системой отзывов:

- После успешного обновления достопримечательностей вызывается `ReviewSyncService.performBulkSync()`.
- Это обеспечивает, что при открытии карточки отзывы показываются **мгновенно из Room**, без ожидания сети.

### Порядок шагов (фактический)

1) Проверка сети (`NetworkUseCase.isOnline()`)
2) FULL или DELTA sync `attractions`
3) Обновление `lastSyncTimestamp`
4) **Bulk sync отзывов** через `ReviewSyncService`

### Tombstones

В текущей конфигурации tombstones для `attractions` временно отключены (из-за подвисаний на cellular) и возвращается пустой список.

## ReviewSyncService (кратко)

Сервис отзывов реализует hybrid стратегию:

- **Bulk sync** (во время общего синка):
    - если кэша нет → загрузка всех `approved` отзывов
    - если кэш есть → глобальный delta sync по `MAX(updated_at)`
- **Delta sync на карточке**:
    - если кэш свежий (< 5 минут) → сеть не трогаем
    - иначе тянем обновления по `updated_at` только для конкретного `attraction_id`

Важно: при upsert отзывов сохраняются локальные поля (`userReaction`, `isOwnReview`), чтобы не терять реакции и «мой отзыв».

```kotlin
// data/sync/SyncService.kt
package com.adygyes.app.data.sync

import com.adygyes.app.data.local.dao.AttractionDao
import com.adygyes.app.data.local.preferences.PreferencesManager
import com.adygyes.app.data.mapper.AttractionMapper.toEntity
import com.adygyes.app.data.remote.NetworkResult
import com.adygyes.app.data.remote.SupabaseRemoteDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of sync operation
 */
data class SyncResult(
    val success: Boolean,
    val added: Int = 0,
    val updated: Int = 0,
    val deleted: Int = 0,
    val errorMessage: String? = null
)

/**
 * Service responsible for syncing data between Room and Supabase
 */
@Singleton
class SyncService @Inject constructor(
    private val remoteDataSource: SupabaseRemoteDataSource,
    private val attractionDao: AttractionDao,
    private val preferencesManager: PreferencesManager
) {
    
    /**
     * Perform delta sync with Supabase
     * 
     * 1. Get last sync timestamp
     * 2. Fetch updated attractions since last sync
     * 3. Fetch deleted attractions (tombstones) since last sync
     * 4. Apply changes to Room DB
     * 5. Update last sync timestamp
     */
    suspend fun performSync(): SyncResult = withContext(Dispatchers.IO) {
        try {
            Timber.d("🔄 Starting sync with Supabase...")
            
            // 1. Get last sync timestamp
            val lastSyncTimestamp = preferencesManager.getLastSyncTimestamp()
            val syncSince = lastSyncTimestamp ?: DEFAULT_SYNC_TIMESTAMP
            
            Timber.d("📅 Last sync: $syncSince")
            
            // 2. Fetch updated attractions
            val updatedResult = remoteDataSource.getUpdatedAttractions(syncSince)
            
            // 3. Fetch deleted attractions
            val deletedResult = remoteDataSource.getDeletedAttractions(syncSince)
            
            // Handle errors
            if (updatedResult is NetworkResult.Error) {
                Timber.e("❌ Failed to fetch updated attractions: ${updatedResult.message}")
                return@withContext SyncResult(
                    success = false,
                    errorMessage = updatedResult.message
                )
            }
            
            val updatedAttractions = (updatedResult as NetworkResult.Success).data
            val deletedIds = when (deletedResult) {
                is NetworkResult.Success -> deletedResult.data
                is NetworkResult.Error -> {
                    Timber.w("⚠️ Could not fetch tombstones: ${deletedResult.message}")
                    emptyList()
                }
            }
            
            Timber.d("📊 Sync data: ${updatedAttractions.size} updated, ${deletedIds.size} deleted")
            
            // 4. Apply changes to Room DB
            var added = 0
            var updated = 0
            
            updatedAttractions.forEach { dto ->
                val existingEntity = attractionDao.getAttractionById(dto.id)
                val newEntity = dto.toEntity()
                
                if (existingEntity != null) {
                    // Update existing - preserve local favorite status
                    attractionDao.updateAttraction(
                        newEntity.copy(isFavorite = existingEntity.isFavorite)
                    )
                    updated++
                } else {
                    // Insert new
                    attractionDao.insertAttraction(newEntity)
                    added++
                }
            }
            
            // Delete removed attractions
            deletedIds.forEach { id ->
                attractionDao.deleteAttractionById(id)
            }
            
            // 5. Update last sync timestamp
            val newTimestamp = Instant.now().toString()
            preferencesManager.updateLastSyncTimestamp(newTimestamp)
            
            Timber.d("✅ Sync complete: +$added updated=$updated deleted=${deletedIds.size}")
            
            SyncResult(
                success = true,
                added = added,
                updated = updated,
                deleted = deletedIds.size
            )
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Sync failed")
            SyncResult(
                success = false,
                errorMessage = e.message
            )
        }
    }
    
    /**
     * Force full reload from Supabase (ignores delta sync)
     */
    suspend fun forceFullSync(): SyncResult = withContext(Dispatchers.IO) {
        try {
            Timber.d("🔄 Starting FULL sync with Supabase...")
            
            val result = remoteDataSource.getAllAttractions()
            
            when (result) {
                is NetworkResult.Success -> {
                    val attractions = result.data
                    
                    // Clear existing data (preserve favorites)
                    val favorites = attractionDao.getFavoriteIds()
                    attractionDao.deleteAll()
                    
                    // Insert new data
                    attractions.forEach { dto ->
                        val entity = dto.toEntity().copy(
                            isFavorite = favorites.contains(dto.id)
                        )
                        attractionDao.insertAttraction(entity)
                    }
                    
                    // Update sync timestamp
                    preferencesManager.updateLastSyncTimestamp(Instant.now().toString())
                    
                    Timber.d("✅ Full sync complete: ${attractions.size} attractions")
                    
                    SyncResult(
                        success = true,
                        added = attractions.size
                    )
                }
                is NetworkResult.Error -> {
                    Timber.e("❌ Full sync failed: ${result.message}")
                    SyncResult(
                        success = false,
                        errorMessage = result.message
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Full sync failed")
            SyncResult(
                success = false,
                errorMessage = e.message
            )
        }
    }
    
    companion object {
        // Far past timestamp to get all data on first sync
        private const val DEFAULT_SYNC_TIMESTAMP = "1970-01-01T00:00:00Z"
    }
}
```

---

## 🎛️ SyncManager

### Создать `SyncManager.kt`

```kotlin
// data/sync/SyncManager.kt
package com.adygyes.app.data.sync

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sync state for UI observation
 */
sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Success(val result: SyncResult) : SyncState()
    data class Error(val message: String) : SyncState()
}

/**
 * Manager for orchestrating sync operations
 * 
 * - Handles sync lifecycle
 * - Observes network changes
 * - Provides sync state to UI
 */
@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncService: SyncService,
    private val networkMonitor: NetworkMonitor
) : DefaultLifecycleObserver {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    
    private var networkJob: Job? = null
    
    /**
     * Start observing network changes
     */
    override fun onStart(owner: LifecycleOwner) {
        startNetworkObserver()
    }
    
    /**
     * Stop observing when app goes to background
     */
    override fun onStop(owner: LifecycleOwner) {
        networkJob?.cancel()
    }
    
    /**
     * Start observing network and trigger sync on reconnect
     */
    private fun startNetworkObserver() {
        networkJob?.cancel()
        networkJob = scope.launch {
            networkMonitor.isOnline.collect { isOnline ->
                if (isOnline && _syncState.value is SyncState.Idle) {
                    Timber.d("📶 Network connected, starting sync...")
                    performSync()
                }
            }
        }
    }
    
    /**
     * Trigger sync (delta sync by default)
     */
    suspend fun performSync(): SyncResult {
        if (_syncState.value is SyncState.Syncing) {
            Timber.d("⏳ Sync already in progress, skipping...")
            return SyncResult(success = false, errorMessage = "Sync already in progress")
        }
        
        _syncState.value = SyncState.Syncing
        
        return try {
            val result = syncService.performSync()
            
            _syncState.value = if (result.success) {
                SyncState.Success(result)
            } else {
                SyncState.Error(result.errorMessage ?: "Unknown error")
            }
            
            result
        } catch (e: Exception) {
            val error = e.message ?: "Unknown error"
            _syncState.value = SyncState.Error(error)
            SyncResult(success = false, errorMessage = error)
        } finally {
            // Reset to idle after delay
            scope.launch {
                delay(3000)
                if (_syncState.value !is SyncState.Syncing) {
                    _syncState.value = SyncState.Idle
                }
            }
        }
    }
    
    /**
     * Force full sync (reload all data)
     */
    suspend fun forceFullSync(): SyncResult {
        if (_syncState.value is SyncState.Syncing) {
            return SyncResult(success = false, errorMessage = "Sync already in progress")
        }
        
        _syncState.value = SyncState.Syncing
        
        return try {
            val result = syncService.forceFullSync()
            
            _syncState.value = if (result.success) {
                SyncState.Success(result)
            } else {
                SyncState.Error(result.errorMessage ?: "Unknown error")
            }
            
            result
        } catch (e: Exception) {
            _syncState.value = SyncState.Error(e.message ?: "Unknown error")
            SyncResult(success = false, errorMessage = e.message)
        }
    }
    
    /**
     * Reset sync state to idle
     */
    fun resetState() {
        _syncState.value = SyncState.Idle
    }
}
```

---

## 📡 Network Monitor

### Создать `NetworkMonitor.kt`

```kotlin
// data/sync/NetworkMonitor.kt
package com.adygyes.app.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitors network connectivity status
 */
@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val connectivityManager = context.getSystemService(
        Context.CONNECTIVITY_SERVICE
    ) as ConnectivityManager
    
    /**
     * Flow that emits network status changes
     */
    val isOnline: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Timber.d("📶 Network available")
                trySend(true)
            }
            
            override fun onLost(network: Network) {
                Timber.d("📵 Network lost")
                trySend(false)
            }
            
            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities
            ) {
                val hasInternet = capabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET
                )
                Timber.d("📶 Network capabilities changed: hasInternet=$hasInternet")
                trySend(hasInternet)
            }
        }
        
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(request, callback)
        
        // Emit current state
        trySend(isCurrentlyOnline())
        
        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()
    
    /**
     * Check current network status synchronously
     */
    fun isCurrentlyOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
```

---

## 🔗 Интеграция с Repository

### Обновить `AttractionRepositoryImpl.kt`

```kotlin
// data/repository/AttractionRepositoryImpl.kt - ДОБАВИТЬ методы

/**
 * Trigger sync with Supabase
 */
suspend fun syncWithRemote(): SyncResult {
    return syncManager.performSync()
}

/**
 * Force full reload from Supabase
 */
suspend fun forceFullSync(): SyncResult {
    return syncManager.forceFullSync()
}

/**
 * Observe sync state
 */
val syncState: StateFlow<SyncState>
    get() = syncManager.syncState
```

### Обновить `PreferencesManager.kt`

```kotlin
// data/local/preferences/PreferencesManager.kt - ДОБАВИТЬ

object PreferencesKeys {
    // ... existing keys ...
    val LAST_SYNC_TIMESTAMP = stringPreferencesKey("last_sync_timestamp")
}

/**
 * Get last sync timestamp
 */
suspend fun getLastSyncTimestamp(): String? {
    return dataStore.data.first()[PreferencesKeys.LAST_SYNC_TIMESTAMP]
}

/**
 * Update last sync timestamp
 */
suspend fun updateLastSyncTimestamp(timestamp: String) {
    dataStore.edit { preferences ->
        preferences[PreferencesKeys.LAST_SYNC_TIMESTAMP] = timestamp
    }
}
```

---

## 🗂️ Обновить DAO

### Добавить в `AttractionDao.kt`

```kotlin
// data/local/dao/AttractionDao.kt - ДОБАВИТЬ

/**
 * Get list of favorite attraction IDs (for preserving during sync)
 */
@Query("SELECT id FROM attractions WHERE isFavorite = 1")
suspend fun getFavoriteIds(): List<String>

/**
 * Delete attraction by ID
 */
@Query("DELETE FROM attractions WHERE id = :id")
suspend fun deleteAttractionById(id: String)

/**
 * Update attraction (for sync)
 */
@Update
suspend fun updateAttraction(attraction: AttractionEntity)
```

---

## ✅ Чеклист реализации

### Файлы для создания

| Файл | Статус | Описание |
|------|--------|----------|
| `app/src/main/java/com/adygyes/app/data/sync/SyncService.kt` | ✅ | Сервис синхронизации |
| `app/src/main/java/com/adygyes/app/data/sync/SyncManager.kt` | ✅ | Менеджер sync lifecycle |
| `app/src/main/java/com/adygyes/app/data/sync/NetworkMonitor.kt` | ✅ | Мониторинг сети |

### Файлы для изменения

| Файл | Изменение |
|------|-----------|
| `data/local/dao/AttractionDao.kt` | Добавить методы для sync |
| `data/local/preferences/PreferencesManager.kt` | Добавить lastSyncTimestamp |
| `data/repository/AttractionRepositoryImpl.kt` | Интеграция SyncManager |

### Тестирование

| Сценарий | Что проверить |
|----------|---------------|
| Первый запуск | Полная загрузка из Supabase |
| Delta sync | Только новые/изменённые записи |
| Offline | Приложение работает без сети |
| Reconnect | Автоматический sync при подключении |
| Tombstones | Удалённые записи удаляются локально |
| Favorites | Локальные favorites сохраняются при sync |

---

## 📋 Следующий шаг

Следующий шаг:
- Пройти E2E сценарии синхронизации (первый запуск / delta / offline / reconnect / tombstones)
- Затем: [05_UI_UNIFICATION.md](05_UI_UNIFICATION.md) — унификация UI компонентов

