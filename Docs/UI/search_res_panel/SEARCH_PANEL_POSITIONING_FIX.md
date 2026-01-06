# Исправление позиционирования SearchResultsPanel

## Проблема
Панель занимала весь экран, а должна была начинаться только от строки поиска.

## Решение

### 1. Учет высоты строки поиска
```kotlin
val searchBarHeight = 120.dp // Height of search bar + padding
val searchBarHeightPx = with(density) { searchBarHeight.toPx() }
```

### 2. Обновленные позиции состояний
```kotlin
val expandedOffset = searchBarHeightPx        // Начинается под строкой поиска
val halfOffset = screenHeight * 0.6f         // 60% экрана от верха
val hiddenOffset = screenHeight              // Полностью скрыта
```

### 3. Ограничение высоты панели
```kotlin
.height(with(density) { (screenHeight - searchBarHeightPx).toDp() })
```
Панель теперь занимает только пространство от строки поиска до низа экрана.

### 4. Обновленная логика drag-жестов
```kotlin
// Позиция относительно области под строкой поиска
val relativePosition = (offsetY - searchBarHeightPx) / (screenHeight - searchBarHeightPx)

panelState = when {
    relativePosition < 0.1f -> SearchPanelState.Expanded  // Близко к строке поиска
    relativePosition < 0.6f -> SearchPanelState.Half     // Средняя область
    else -> SearchPanelState.Hidden                      // Нижняя область
}

// Ограничение перетаскивания областью под строкой поиска
val newOffset = (offsetY + dragAmount.y).coerceIn(searchBarHeightPx, screenHeight)
```

## Результат

✅ **Expanded**: Панель начинается сразу под строкой поиска  
✅ **Half**: Панель на 60% экрана от верха  
✅ **Hidden**: Панель полностью скрыта  
✅ **Drag-жесты**: Работают только в области под строкой поиска  
✅ **Высота**: Панель не перекрывает строку поиска  

Теперь панель корректно позиционируется и не занимает лишнее пространство!
