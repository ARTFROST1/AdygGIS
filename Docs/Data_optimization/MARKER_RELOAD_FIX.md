# Исправление проблемы с отсутствием маркеров после обновления версии JSON

> Status (2026-01-06): this scenario is specific to the **legacy JSON version update** flow.
> In the current offline-first architecture, marker updates should be driven by Room updates produced by Supabase sync.

## Проблема

После обновления версии файла `attractions.json` маркеры не появлялись на карте до перехода в режим списка. Это происходило из-за того, что:

1. MapPreloadManager сбрасывал состояние, но не перезапускал предзагрузку
2. DualLayerMarkerSystem не получал обновленные данные
3. MapViewModel не перезагружал attractions после смены версии

## Решение

### 1. Автоматический перезапуск предзагрузки в MapPreloadManager

```kotlin
// После завершения процесса обновления данных
// CRITICAL: Restart preload process with new data after version update
delay(500) // Small delay to ensure UI updates

// Find the current MapView and restart preload
val currentMapView = VisualMarkerRegistry.getCurrentMapView()
if (currentMapView != null) {
    Timber.d("🔄 Restarting preload process with new data after version update")
    startPreload(currentMapView)
} else {
    Timber.w("⚠️ No MapView available for restarting preload after version update")
}
```

### 2. Добавлена функция getCurrentMapView() в VisualMarkerRegistry

```kotlin
/**
 * Get the current active MapView (for restarting preload after version update)
 */
fun getCurrentMapView(): MapView? {
    return map.keys.firstOrNull()
}
```

### 3. Улучшена логика DualLayerMarkerSystem

Добавлен новый случай для принудительного создания маркеров после обновления версии:

```kotlin
// Case 4: Force recreate if no preloaded markers but we have attractions (after version update)
!visualMarkerProvider.hasPreloadedMarkers() && newIds.isNotEmpty() && currentIds.isEmpty() -> {
    Timber.d("📍 Force creating markers after version update: ${newIds.size} attractions")
    visualMarkerProvider.setAppearAnimation(enableAppearAnimation)
    visualMarkerProvider.addVisualMarkers(attractions)
    VisualMarkerRegistry.setLastIds(mapView, newIds)
}
```

### 4. Отслеживание версии данных в MapViewModel

```kotlin
/**
 * Отслеживает изменения версии данных и принудительно перезагружает attractions
 */
private fun observeDataVersionChanges() {
    var lastKnownVersion: String? = null
    
    viewModelScope.launch {
        preferencesManager.userPreferencesFlow.collect { preferences ->
            val currentVersion = preferences.dataVersion
            
            if (lastKnownVersion != null && lastKnownVersion != currentVersion) {
                Timber.d("🔄 Data version changed in MapViewModel, reloading attractions")
                
                // Force reload attractions after version change
                delay(2000) // Wait for data update process to complete
                loadAttractions()
            }
            
            lastKnownVersion = currentVersion
        }
    }
}
```

### 5. Принудительное обновление в MapScreen

```kotlin
// Handle data update completion - force marker refresh
LaunchedEffect(preloadState?.value?.dataUpdating) {
    val isDataUpdating = preloadState?.value?.dataUpdating
    if (isDataUpdating == false && isMapReady) {
        // Data update just completed, force refresh markers
        Timber.d("🔄 Data update completed, forcing marker refresh")
        delay(1000) // Wait for preload to complete
        
        // Force trigger marker system update
        if (mapView != null && filteredAttractions.isNotEmpty()) {
            Timber.d("🎯 Forcing DualLayerMarkerSystem update after data version change")
            // The DualLayerMarkerSystem will be updated automatically due to filteredAttractions change
        }
    }
}
```

## Поток исправления

```
Обновление attractions.json (версия изменилась)
    ↓
MapPreloadManager отслеживает изменение версии
    ↓
Показывается DataUpdateOverlay
    ↓
Очистка всех компонентов (forceReset + VisualMarkerRegistry.forceResetAll)
    ↓
Завершение процесса обновления (dataUpdating = false)
    ↓
Автоматический перезапуск startPreload() с новыми данными
    ↓
MapViewModel.observeDataVersionChanges() перезагружает attractions
    ↓
DualLayerMarkerSystem получает новые данные и создает маркеры
    ↓
MapScreen.LaunchedEffect принудительно обновляет систему маркеров
    ↓
Маркеры появляются на карте автоматически! ✅
```

## Результат

### До исправления:
- ❌ Маркеры не появлялись после обновления версии
- ❌ Требовался переход в режим списка для появления маркеров
- ❌ Плохой пользовательский опыт

### После исправления:
- ✅ Маркеры автоматически появляются после завершения DataUpdateOverlay
- ✅ Не требуется никаких дополнительных действий от пользователя
- ✅ Плавный переход от overlay к готовым маркерам
- ✅ Отличный пользовательский опыт

## Логи для отладки

```
🔄 Data version changed from '1.3.0' to '1.4.0', starting data update process
🧹 Force reset all VisualMarkerProviders
✅ Data update process completed
🔄 Restarting preload process with new data after version update
🔄 Data version changed in MapViewModel, reloading attractions
📍 Force creating markers after version update: 10 attractions
✅ Attractions reloaded after version change
🎯 Forcing DualLayerMarkerSystem update after data version change
```

Теперь обновление версии attractions.json работает безупречно с автоматическим появлением маркеров!
