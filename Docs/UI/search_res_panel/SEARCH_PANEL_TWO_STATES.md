# Упрощение панели результатов поиска до двух состояний

## Изменение
Убрано состояние "Hidden" (у нижнего края экрана) из панели результатов поиска. Теперь панель имеет только два состояния: Expanded и Half.

## Новая логика состояний

### Состояния панели:
1. **Expanded** - полностью развернута (с клавиатурой)
2. **Half** - наполовину развернута (по умолчанию)
3. **Hidden** - только при полном закрытии поиска (`!isVisible`)

### Обновленная логика:
```kotlin
LaunchedEffect(isVisible, hasKeyboard, isBottomSheetOpen) {
    panelState = when {
        !isVisible -> SearchPanelState.Hidden // Only hide when search is completely dismissed
        hasKeyboard -> SearchPanelState.Expanded // Full expansion with keyboard
        else -> SearchPanelState.Half // Default to half state (no hidden state during interaction)
    }
}
```

## Обновленные drag-жесты

### Ограничение перетаскивания:
```kotlin
// Update offset during drag - constrain between Expanded and Half positions
val newOffset = (offsetY + dragAmount.y).coerceIn(totalTopHeightPx, halfOffset)
```

### Определение финального состояния:
```kotlin
// Only two states: Expanded and Half (no Hidden state during interaction)
val relativePosition = (offsetY - totalTopHeightPx) / (halfOffset - totalTopHeightPx)
panelState = when {
    relativePosition < 0.5f -> SearchPanelState.Expanded  // Upper half -> Expanded
    else -> SearchPanelState.Half                         // Lower half -> Half
}
```

## Сценарии использования

### Поиск и взаимодействие:
```
1. [Начало поиска] → Панель: Expanded (с клавиатурой)
2. [Enter/скрытие клавиатуры] → Панель: Half
3. [Клик на карточку] → Панель: Half + BottomSheet поверх
4. [Закрытие BottomSheet] → Панель: Half (остается видимой)
5. [Drag вверх] → Панель: Expanded
6. [Drag вниз] → Панель: Half (не может уйти ниже)
```

### Полное закрытие:
```
[Очистка поиска] → Панель: Hidden (полностью скрывается)
[Новый поиск] → Панель: Expanded (появляется снова)
```

## Визуальное поведение

### Доступные позиции:
```
┌─────────────────┐
│ [Строка поиска] │ ← Защищенная область
│ [Фильтры]       │
├─────────────────┤ ← Expanded (максимальная позиция)
│ ╔═══════════════╗ │
│ ║ [Панель]      ║ │
│ ║               ║ │
│ ╠═══════════════╣ │ ← Half (минимальная позиция при взаимодействии)
│ ║               ║ │
│ ║               ║ │
│ ╚═══════════════╝ │
└─────────────────┘
```

### Drag-жесты:
- **Вверх**: Expanded ↔ Half
- **Вниз**: Half ↔ Expanded (не может уйти ниже Half)
- **Область перетаскивания**: От `totalTopHeightPx` до `halfOffset`

## Преимущества

✅ **Упрощенное взаимодействие** - только два активных состояния  
✅ **Постоянная доступность** - панель всегда видна при поиске  
✅ **Логичное поведение** - нет случайного скрытия панели  
✅ **Быстрое переключение** - между Expanded и Half  
✅ **Сохранение контекста** - результаты поиска всегда доступны  

## Результат

Теперь панель результатов поиска работает предсказуемо:
- При поиске всегда видна (Expanded или Half)
- Не может случайно скрыться при взаимодействии
- BottomSheet открывается поверх Half состояния
- После закрытия BottomSheet панель остается в Half состоянии

Пользователь может легко переключаться между результатами поиска без потери контекста!
