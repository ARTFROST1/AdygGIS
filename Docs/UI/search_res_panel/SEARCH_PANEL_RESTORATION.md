# Восстановление панели результатов поиска после BottomSheet

## Функциональность
При закрытии BottomSheet, который был открыт из панели результатов поиска, панель автоматически восстанавливается в том же состоянии.

## Реализация

### Отслеживание источника открытия:
```kotlin
fun selectAttractionFromPanel(attraction: Attraction, mapView: MapView?) {
    _selectedFromPanel.value = true // Помечаем, что пришли из панели
    _showSearchPanel.value = false // Скрываем панель
    _searchPanelHasKeyboard.value = false
    
    centerMapOnAttraction(attraction, mapView)
    selectAttraction(attraction) // Открываем BottomSheet
}
```

### Восстановление при закрытии BottomSheet:
```kotlin
fun dismissAttractionDetail() {
    _uiState.update { it.copy(showAttractionDetail = false) }
    
    // If we came from search panel, restore it
    if (_selectedFromPanel.value) {
        _selectedFromPanel.value = false
        // Restore search panel if we have search query
        if (_searchQuery.value.isNotEmpty()) {
            _showSearchPanel.value = true
            _searchPanelHasKeyboard.value = false // Show in half state, not expanded
        }
    }
}
```

## Логика работы

### Сценарий использования:
1. **Пользователь ищет** → панель результатов появляется
2. **Нажимает на карточку** → панель скрывается, BottomSheet открывается
3. **Закрывает BottomSheet** → панель результатов восстанавливается

### Состояния флага `selectedFromPanel`:
- `true` - BottomSheet открыт из панели результатов
- `false` - BottomSheet открыт напрямую (клик по маркеру)

### Условия восстановления:
- BottomSheet был открыт из панели (`selectedFromPanel = true`)
- Есть активный поисковый запрос (`searchQuery.isNotEmpty()`)
- BottomSheet закрывается (`dismissAttractionDetail()`)

## Визуальный поток

### Обычный сценарий (клик по маркеру):
```
[Карта] → [Клик по маркеру] → [BottomSheet] → [Закрытие] → [Карта]
```

### Сценарий из поиска:
```
[Поиск] → [Панель результатов] → [Клик на карточку] → [BottomSheet] → [Закрытие] → [Панель результатов]
```

## Преимущества

✅ **Сохранение контекста** - пользователь возвращается к поиску  
✅ **Удобная навигация** - не нужно заново искать  
✅ **Логичное поведение** - панель остается в том же состоянии  
✅ **Эффективность** - быстрое переключение между результатами  

## Состояние панели при восстановлении

- **Видимость**: `showSearchPanel = true`
- **Клавиатура**: `searchPanelHasKeyboard = false` (половинное состояние)
- **Содержимое**: Те же результаты поиска
- **Позиция прокрутки**: Сохраняется автоматически

Теперь пользователь может удобно просматривать результаты поиска, переходить к деталям и возвращаться к списку!
