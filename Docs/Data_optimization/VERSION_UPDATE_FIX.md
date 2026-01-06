# Исправление проблем при обновлении версии attractions.json

> Status (2026-01-06): this document describes fixes for the **legacy JSON version update** flow.
> With offline-first Supabase sync, the app should not depend on `attractions.json` version changes for regular data updates.

## Проблема

При обновлении версии файла `attractions.json` возникали критические ошибки:

```
E  Failed to load marker image for La Villa Pine
   kotlinx.coroutines.JobCancellationException: Job was cancelled
E  FATAL EXCEPTION: main
   java.lang.RuntimeException: Native object's weak_ptr for com.yandex.mapkit.map.MapObject has expired
```

### Причины:
1. **JobCancellationException** - корутины отменялись при обновлении версии
2. **Native object's weak_ptr expired** - попытка работы с недействительными объектами MapKit
3. **Отсутствие синхронизации** между очисткой данных и активными корутинами

## Архитектура решения

### 1. Проверки валидности объектов MapKit

Добавлены проверки `placemark.isValid` перед всеми операциями с маркерами:

```kotlin
// В VisualMarkerProvider.kt
private fun animateMarkerAppearance(placemark: PlacemarkMapObject) {
    coroutineScope.launch {
        try {
            // Check if placemark is still valid before animation
            if (!placemark.isValid) {
                Timber.w("Placemark is no longer valid, skipping animation")
                return@launch
            }
            
            // ... animation logic
        } catch (e: CancellationException) {
            Timber.d("Animation cancelled for placemark")
            throw e // Re-throw to properly handle cancellation
        }
    }
}
```

### 2. Правильная обработка отмены корутин

Добавлена корректная обработка `CancellationException`:

```kotlin
} catch (e: CancellationException) {
    Timber.d("Animation cancelled for ${attraction.name}")
    throw e // Re-throw cancellation to properly handle it
} catch (e: Exception) {
    Timber.e(e, "Error during marker animation")
}
```

### 3. Принудительная очистка при смене версии

#### VisualMarkerProvider.kt:
```kotlin
fun forceReset() {
    Timber.d("🔄 Force resetting VisualMarkerProvider due to data version change")
    
    // Cancel all coroutines immediately
    coroutineScope.cancel()
    
    // Wait a bit for cancellation to complete
    runBlocking { delay(100) }
    
    // Clear everything
    markers.clear()
    markerImages.clear()
    preloadedImages.clear()
    markersPreloaded = false
    
    // Clear map objects safely
    try {
        mapObjectCollection.clear()
    } catch (e: Exception) {
        Timber.w(e, "Error clearing map objects during force reset")
    }
    
    // Create fresh coroutine scope
    coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
}
```

#### VisualMarkerRegistry.kt:
```kotlin
fun forceResetAll() {
    Timber.d("🔄 Force resetting all ${map.size} VisualMarkerProviders")
    
    map.values.forEach { entry ->
        try {
            entry.provider.forceReset()
            entry.lastIds = emptySet() // Clear cached IDs
        } catch (e: Exception) {
            Timber.w(e, "Error force resetting VisualMarkerProvider")
        }
    }
}
```

### 4. Автоматическое отслеживание версии данных

MapPreloadManager теперь автоматически отслеживает изменения версии:

```kotlin
init {
    // Monitor data version changes and reset when needed
    scope.launch {
        preferencesManager.userPreferencesFlow.collect { preferences ->
            val currentVersion = preferences.dataVersion
            if (lastKnownDataVersion != null && lastKnownDataVersion != currentVersion) {
                Timber.d("🔄 Data version changed, resetting preload manager")
                forceReset()
                // Force reset all visual marker providers
                VisualMarkerRegistry.forceResetAll()
            }
            lastKnownDataVersion = currentVersion
        }
    }
}
```

## Поток исправления

```
Обновление attractions.json (версия изменилась)
    ↓
AttractionRepositoryImpl.loadInitialData()
    ↓
preferencesManager.updateDataVersion(newVersion)
    ↓
MapPreloadManager отслеживает изменение через Flow
    ↓
MapPreloadManager.forceReset() + VisualMarkerRegistry.forceResetAll()
    ↓
Все корутины отменяются, объекты MapKit очищаются
    ↓
Создаются новые корутины и маркеры с новыми данными
    ↓
Мгновенная загрузка без ошибок
```

## Ключевые улучшения

### 1. Безопасность операций:
- ✅ Проверка `placemark.isValid` перед каждой операцией
- ✅ Правильная обработка `CancellationException`
- ✅ Безопасная очистка объектов MapKit

### 2. Синхронизация состояния:
- ✅ Автоматическое отслеживание версии данных
- ✅ Принудительная очистка всех связанных компонентов
- ✅ Пересоздание корутин с чистым состоянием

### 3. Отладка и мониторинг:
- ✅ Подробное логирование всех операций
- ✅ Отслеживание состояния маркеров
- ✅ Информативные сообщения об ошибках

## Результат

### До исправления:
- ❌ JobCancellationException при смене версии
- ❌ Crash приложения из-за недействительных объектов MapKit
- ❌ Необходимость перезапуска приложения

### После исправления:
- ✅ Плавное обновление данных без ошибок
- ✅ Автоматическая очистка и пересоздание маркеров
- ✅ Стабильная работа при любых изменениях версии
- ✅ Мгновенная загрузка обновленных данных

## Совместимость

Исправления полностью совместимы с существующей архитектурой:
- ✅ Система предзагрузки маркеров
- ✅ DualLayerMarkerSystem
- ✅ ImageCacheManager
- ✅ Кластеризация маркеров
- ✅ Анимации появления

Теперь обновление версии attractions.json работает безупречно!
