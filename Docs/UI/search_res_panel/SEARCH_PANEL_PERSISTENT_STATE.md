# Постоянная видимость панели результатов поиска

## Обновленное поведение
Панель результатов поиска теперь остается видимой в половинном состоянии при открытии BottomSheet, вместо полного скрытия.

## Изменения в логике

### Старое поведение:
```kotlin
fun selectAttractionFromPanel(attraction: Attraction, mapView: MapView?) {
    _selectedFromPanel.value = true
    _showSearchPanel.value = false // ❌ Полностью скрывали панель
    _searchPanelHasKeyboard.value = false
    // ...
}
```

### Новое поведение:
```kotlin
fun selectAttractionFromPanel(attraction: Attraction, mapView: MapView?) {
    _selectedFromPanel.value = true
    // ✅ Панель остается видимой в половинном состоянии
    _searchPanelHasKeyboard.value = false // Ensure it's in half state, not expanded
    // ...
}
```

### Упрощенное закрытие:
```kotlin
fun dismissAttractionDetail() {
    _uiState.update { it.copy(showAttractionDetail = false) }
    
    // Reset the flag when closing bottom sheet
    if (_selectedFromPanel.value) {
        _selectedFromPanel.value = false
        // Panel is already visible in half state, no need to restore
    }
}
```

## Визуальное поведение

### Сценарий использования:
```
1. [Поиск] → Панель результатов (Expanded)
2. [Клик на карточку] → Панель сворачивается (Half) + BottomSheet открывается
3. [Закрытие BottomSheet] → Панель остается (Half)
```

### Состояния панели:
- **При поиске**: `Expanded` (с клавиатурой)
- **При выборе карточки**: `Half` (без клавиатуры)
- **При закрытии BottomSheet**: `Half` (остается видимой)

## Преимущества нового подхода

✅ **Постоянная доступность** - панель всегда видна  
✅ **Быстрое переключение** - не нужно ждать анимации появления  
✅ **Логичное поведение** - BottomSheet просто перекрывает панель  
✅ **Сохранение контекста** - результаты поиска всегда на виду  
✅ **Улучшенный UX** - плавные переходы между состояниями  

## Визуальная схема

### Состояние при выборе карточки:
```
┌─────────────────┐
│ [Строка поиска] │
│ [Фильтры]       │
├─────────────────┤
│ ╔═══════════════╗ │ ← BottomSheet поверх панели
│ ║ [BottomSheet] ║ │
│ ║               ║ │
│ ╠═══════════════╣ │
│ ║ [Панель Half] ║ │ ← Панель видна снизу
│ ╚═══════════════╝ │
└─────────────────┘
```

### После закрытия BottomSheet:
```
┌─────────────────┐
│ [Строка поиска] │
│ [Фильтры]       │
├─────────────────┤
│ ╔═══════════════╗ │
│ ║ [Панель Half] ║ │ ← Панель сразу доступна
│ ║ результатов   ║ │
│ ║               ║ │
│ ╚═══════════════╝ │
└─────────────────┘
```

Теперь панель результатов поиска ведет себя более естественно и удобно для пользователя!
