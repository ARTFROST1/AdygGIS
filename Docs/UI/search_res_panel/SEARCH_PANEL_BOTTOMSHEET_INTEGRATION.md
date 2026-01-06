# Интеграция панели поиска с BottomSheet

## Проблема
Панель результатов поиска полностью скрывалась при открытии BottomSheet, вместо того чтобы оставаться в половинном состоянии.

## Решение
Добавлен параметр `isBottomSheetOpen` для отслеживания состояния BottomSheet и корректировки поведения панели.

## Изменения в SearchResultsPanel

### Новый параметр:
```kotlin
@Composable
fun SearchResultsPanel(
    attractions: List<Attraction>,
    isVisible: Boolean,
    hasKeyboard: Boolean,
    isBottomSheetOpen: Boolean = false, // ✅ Новый параметр
    onAttractionClick: (Attraction) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
)
```

### Обновленная логика состояний:
```kotlin
LaunchedEffect(isVisible, hasKeyboard, isBottomSheetOpen) {
    panelState = when {
        !isVisible -> SearchPanelState.Hidden
        hasKeyboard -> SearchPanelState.Expanded
        isBottomSheetOpen -> SearchPanelState.Half // ✅ Остается в половинном состоянии
        else -> SearchPanelState.Half
    }
}
```

## Изменения в MapScreen

### Передача состояния BottomSheet:
```kotlin
SearchResultsPanel(
    attractions = filteredAttractions,
    isVisible = showSearchPanel,
    hasKeyboard = isKeyboardVisible,
    isBottomSheetOpen = uiState.showAttractionDetail, // ✅ Передаем состояние BottomSheet
    onAttractionClick = { attraction ->
        viewModel.selectAttractionFromPanel(attraction, mapView)
    },
    // ...
)
```

## Логика работы

### Состояния панели:
1. **Hidden**: `!isVisible` - панель полностью скрыта
2. **Expanded**: `hasKeyboard` - панель развернута с клавиатурой
3. **Half**: `isBottomSheetOpen || else` - панель в половинном состоянии

### Приоритет состояний:
```
1. !isVisible → Hidden (высший приоритет)
2. hasKeyboard → Expanded
3. isBottomSheetOpen → Half
4. else → Half (по умолчанию)
```

## Сценарий использования

### Поток взаимодействия:
```
1. [Поиск] → Панель: Expanded (с клавиатурой)
2. [Enter/скрытие клавиатуры] → Панель: Half
3. [Клик на карточку] → Панель: Half + BottomSheet открывается
4. [Закрытие BottomSheet] → Панель: Half (остается видимой)
```

### Визуальное поведение:
```
┌─────────────────┐
│ [Строка поиска] │
│ [Фильтры]       │
├─────────────────┤
│ ╔═══════════════╗ │ ← BottomSheet поверх панели
│ ║ [BottomSheet] ║ │
│ ║               ║ │
│ ╠═══════════════╣ │
│ ║ [Панель Half] ║ │ ← Панель остается в Half состоянии
│ ╚═══════════════╝ │
└─────────────────┘
```

## Преимущества

✅ **Постоянная видимость** - панель не исчезает при открытии BottomSheet  
✅ **Логичное поведение** - панель остается в половинном состоянии  
✅ **Быстрое переключение** - мгновенный доступ к другим результатам  
✅ **Сохранение контекста** - результаты поиска всегда доступны  

Теперь панель результатов поиска корректно взаимодействует с BottomSheet!
