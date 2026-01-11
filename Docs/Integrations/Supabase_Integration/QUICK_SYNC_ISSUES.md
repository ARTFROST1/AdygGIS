# ⚡ Критические проблемы синхронизации - Quick Summary

**Дата:** 11 января 2026  
**Статус:** 🔴 Требуется исправление

---

## 🔴 КРИТИЧЕСКИЕ ПРОБЛЕМЫ

### 1. Первый запуск без интернета = Пустое приложение

**Сценарий:**
```
Пользователь устанавливает приложение → WiFi выключен → 
Открывает приложение → ❌ Пустая карта навсегда
```

**Причина:**
```kotlin
// SyncService.kt - линия 63
if (!networkUseCase.isOnline()) {
    return SyncResult(success = false, errorMessage = "Нет подключения...")
    // ❌ Room остаётся пустой
}
```

**Решение: Fallback на bundled JSON**
```kotlin
// AttractionRepositoryImpl.kt
override suspend fun loadInitialData() {
    val hasData = attractionDao.getAttractionsCount() > 0
    if (hasData) return
    
    if (SupabaseConfig.isConfigured()) {
        if (!networkUseCase.isOnline()) {
            // ⚠️ Offline на первом запуске - загружаем из assets
            loadFromAssetsJson()
            return
        }
        // Ждём SyncService
        return
    }
    
    loadFromAssetsJson()
}
```

**Приоритет:** 🔴 КРИТИЧНО - **исправить немедленно**

---

### 2. Нет повторной синхронизации при появлении сети

**Сценарий:**
```
Первый запуск без WiFi → Синхронизация провалилась → 
WiFi включился → ❌ Ничего не происходит → 
Пользователь должен перезапустить приложение
```

**Причина:**
```kotlin
// MapViewModel.kt - линия 156
init {
    performInitialSupabaseSync()  // Выполняется ОДИН раз
}
```

**Решение: Network Listener**
```kotlin
// MapViewModel.kt
private var isSyncSuccessful = false

private fun observeNetworkChanges() {
    viewModelScope.launch {
        networkUseCase.networkStatus.collect { status ->
            when (status) {
                NetworkStatus.Available -> {
                    if (!isSyncSuccessful) {
                        performInitialSupabaseSync()
                    }
                }
            }
        }
    }
}

init {
    observeNetworkChanges()  // ✅ Добавить
    performInitialSupabaseSync()
}
```

**Приоритет:** 🔴 КРИТИЧНО - **исправить немедленно**

---

## 🟡 ВАЖНЫЕ ПРОБЛЕМЫ

### 3. Tombstones отключены - удалённые данные остаются

**Код:**
```kotlin
// SyncService.kt - линия 101
// 3. Skip tombstones for now (they cause hangs on cellular)
val deletedResult = NetworkResult.Success(emptyList<String>())
```

**Последствия:**
- Администратор удаляет attraction в Supabase
- ❌ Attraction остаётся в приложении навсегда

**Решение (временное):**
- Периодический FULL SYNC (раз в неделю)

**Решение (постоянное):**
- Включить tombstones когда cellular sync стабилен

**Приоритет:** 🟡 ВАЖНО - **исправить когда будет стабильно**

---

### 4. Нет UI индикации синхронизации

**Проблема:**
- Синхронизация идёт в фоне
- Пользователь не знает, что происходит
- На 3G может занять 30-60 секунд → кажется что зависло

**Решение:**
```kotlin
// MapViewModel.kt
sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Success(val message: String) : SyncState()
    data class Error(val message: String) : SyncState()
}

val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
```

```kotlin
// MapScreen.kt
if (syncState is SyncState.Syncing) {
    Card {
        Row {
            CircularProgressIndicator()
            Text("Синхронизация данных...")
        }
    }
}
```

**Приоритет:** 🟡 ВАЖНО - **улучшает UX**

---

## 🟢 ЖЕЛАТЕЛЬНЫЕ УЛУЧШЕНИЯ

### 5. Нет периодической синхронизации

**Проблема:**
- Синхронизация только при запуске MapScreen
- Если пользователь долго не возвращается на карту → устаревшие данные

**Решение: WorkManager**
```kotlin
class SyncWorker @AssistedInject constructor(
    private val syncService: SyncService
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val result = syncService.performSync()
        return if (result.success) Result.success() else Result.retry()
    }
}

// Запланировать периодическую синхронизацию (каждые 6 часов)
workManager.enqueueUniquePeriodicWork(...)
```

**Приоритет:** 🟢 ЖЕЛАТЕЛЬНО - **для production**

---

### 6. Favorites могут потеряться при Clear Data

**Проблема:**
- Пользователь делает Clear Data
- ❌ Все favorites исчезают

**Решение: Backup в SharedPreferences**
```kotlin
// PreferencesManager.kt
suspend fun saveFavorites(favoriteIds: Set<String>)
suspend fun restoreFavorites(): Set<String>
```

**Приоритет:** 🟢 ЖЕЛАТЕЛЬНО - **для лучшего UX**

---

### 7. Pull-to-Refresh

**Решение:**
```kotlin
SwipeRefresh(
    state = swipeRefreshState,
    onRefresh = { viewModel.manualSync() }
) {
    // Map content
}
```

**Приоритет:** 🟢 ЖЕЛАТЕЛЬНО - **стандартная функция**

---

## 📊 Приоритеты исправлений

### Сейчас (критично):
1. 🔴 Fallback на bundled JSON при первом запуске offline
2. 🔴 Network Listener для retry при появлении сети

### Ближайшее время (важно):
3. 🟡 UI индикация синхронизации
4. 🟡 Pull-to-Refresh

### Позднее (желательно):
5. 🟢 WorkManager для периодической синхронизации
6. 🟢 Favorites backup
7. 🟢 Tombstones (когда cellular стабилен)

---

## 🎯 Код для быстрого исправления

### Патч #1: Fallback на JSON (AttractionRepositoryImpl.kt)

**Добавить метод:**
```kotlin
private suspend fun loadFromAssetsJson() {
    try {
        val jsonString = context.assets.open("attractions.json")
            .bufferedReader().use { it.readText() }
        
        val response = json.decodeFromString<AttractionsResponse>(jsonString)
        val entities = response.attractions.toEntitiesFromDto()
        
        attractionDao.insertAttractions(entities)
        preferencesManager.updateDataVersion(response.version)
        
        Timber.d("✅ Loaded ${entities.size} attractions from bundled JSON")
    } catch (e: Exception) {
        Timber.e(e, "❌ Failed to load from bundled JSON")
    }
}
```

**Модифицировать loadInitialData():**
```kotlin
override suspend fun loadInitialData() {
    val hasData = attractionDao.getAttractionsCount() > 0
    if (hasData) return
    
    if (SupabaseConfig.isConfigured()) {
        val isOnline = try {
            withTimeout(2000) { networkUseCase.isOnline() }
        } catch (e: TimeoutCancellationException) { false }
        
        if (!isOnline) {
            Timber.w("⚠️ First launch offline - loading from bundled JSON")
            loadFromAssetsJson()
            return
        }
        return
    }
    
    loadFromAssetsJson()
}
```

---

### Патч #2: Network Listener (MapViewModel.kt)

**Добавить:**
```kotlin
private var isSyncSuccessful = false

private fun observeNetworkChanges() {
    viewModelScope.launch {
        networkUseCase.networkStatus.collect { status ->
            when (status) {
                NetworkStatus.Available -> {
                    if (!isSyncSuccessful) {
                        Timber.d("🌐 Network restored, retrying sync...")
                        performInitialSupabaseSync()
                    }
                }
                NetworkStatus.Unavailable -> {
                    Timber.d("📴 Network lost, using offline mode")
                }
            }
        }
    }
}

init {
    loadAttractions()
    observeNetworkChanges()  // ✅ Добавить эту строку
    performInitialSupabaseSync()
}

private fun performInitialSupabaseSync() {
    viewModelScope.launch {
        val result = syncService.performSync()
        isSyncSuccessful = result.success  // ✅ Запомнить результат
        
        if (result.success && (result.added > 0 || result.updated > 0)) {
            loadAttractions()
        }
    }
}
```

---

## 📚 Полная документация

Для детального анализа см.: [SYNC_ANALYSIS_AND_EDGE_CASES.md](./SYNC_ANALYSIS_AND_EDGE_CASES.md)

---

**Статус:** 🔴 Требуется немедленное исправление  
**Автор:** GitHub Copilot  
**Дата:** 11 января 2026
