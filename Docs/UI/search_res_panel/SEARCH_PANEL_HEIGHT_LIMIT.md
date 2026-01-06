# Ограничение максимальной высоты панели результатов

## Проблема
Панель результатов поиска в развернутом состоянии поднималась слишком высоко, перекрывая блок с фильтрами категорий под строкой поиска.

## Решение
Ограничение максимальной высоты панели уровнем начала блока с фильтрами категорий.

## Реализация

### Расчет верхней границы:
```kotlin
val searchBarHeight = 120.dp // Height of search bar + padding
val categoryFiltersHeight = 60.dp // Height of category filters block
val totalTopHeight = searchBarHeight + categoryFiltersHeight // Total reserved top area
val totalTopHeightPx = with(density) { totalTopHeight.toPx() }
```

### Обновленные позиции состояний:
```kotlin
val expandedOffset = totalTopHeightPx // Start below search bar + category filters
val halfOffset = screenHeight * 0.6f // 60% down from top
val hiddenOffset = screenHeight
```

### Ограничение высоты панели:
```kotlin
.height(with(density) { (screenHeight - totalTopHeightPx).toDp() })
```

### Обновленная логика drag-жестов:
```kotlin
// Determine final state based on position relative to top area (search + filters)
val relativePosition = (offsetY - totalTopHeightPx) / (screenHeight - totalTopHeightPx)

// Update offset during drag - constrain to area below search + filters
val newOffset = (offsetY + dragAmount.y).coerceIn(totalTopHeightPx, screenHeight)
```

## Структура экрана

### До изменения:
```
┌─────────────────┐
│ [Строка поиска] │ ← 120dp
│ [Фильтры]       │ ← 60dp (перекрывалось панелью)
├─────────────────┤
│                 │
│ [Панель могла   │ ← Панель поднималась слишком высоко
│  подниматься    │
│  сюда]          │
│                 │
└─────────────────┘
```

### После изменения:
```
┌─────────────────┐
│ [Строка поиска] │ ← 120dp (защищено)
│ [Фильтры]       │ ← 60dp (защищено)
├─────────────────┤ ← Максимальная граница панели
│ ╔═══════════════╗ │
│ ║ [Панель       ║ │ ← Панель начинается здесь
│ ║  результатов] ║ │
│ ║               ║ │
│ ╚═══════════════╝ │
└─────────────────┘
```

## Параметры

- **Строка поиска**: 120dp (поле + отступы)
- **Фильтры категорий**: 60dp (высота блока фильтров)
- **Общая защищенная область**: 180dp
- **Доступная область для панели**: `screenHeight - 180dp`

## Преимущества

✅ **Защита интерфейса**: Фильтры категорий всегда видны  
✅ **Логичная структура**: Панель не перекрывает функциональные элементы  
✅ **Улучшенная навигация**: Пользователь всегда видит доступные фильтры  
✅ **Консистентность**: Четкое разделение областей интерфейса  

## Состояния панели

1. **Expanded**: Начинается от уровня фильтров (180dp от верха)
2. **Half**: 60% экрана от верха
3. **Hidden**: Полностью скрыта

Теперь панель результатов корректно уважает границы других элементов интерфейса!
