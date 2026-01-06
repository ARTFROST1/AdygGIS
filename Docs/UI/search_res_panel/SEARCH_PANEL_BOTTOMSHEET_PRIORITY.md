# Приоритет состояния Half при открытом BottomSheet

## Проблема
При нажатии на карточку в панели результатов поиска панель переходила в состояние Expanded вместо Half.

## Причина
Логика состояний проверяла `hasKeyboard` раньше `isBottomSheetOpen`, поэтому панель могла развернуться даже при открытом BottomSheet.

## Решение
Изменен порядок приоритетов в логике состояний - `isBottomSheetOpen` теперь имеет более высокий приоритет.

### Обновленная логика:
```kotlin
LaunchedEffect(isVisible, hasKeyboard, isBottomSheetOpen) {
    panelState = when {
        !isVisible -> SearchPanelState.Hidden // Only hide when search is completely dismissed
        isBottomSheetOpen -> SearchPanelState.Half // ✅ ВЫСШИЙ ПРИОРИТЕТ - всегда Half при BottomSheet
        hasKeyboard -> SearchPanelState.Expanded // Full expansion with keyboard
        else -> SearchPanelState.Half // Default to half state
    }
}
```

## Приоритет состояний (по убыванию):
1. **!isVisible** → Hidden (поиск полностью отключен)
2. **isBottomSheetOpen** → Half (BottomSheet открыт - панель всегда Half)
3. **hasKeyboard** → Expanded (клавиатура активна)
4. **else** → Half (по умолчанию)

## Сценарий использования

### Правильное поведение:
```
1. [Поиск] → Панель: Expanded (с клавиатурой)
2. [Enter/скрытие клавиатуры] → Панель: Half
3. [Клик на карточку] → BottomSheet открывается → Панель: Half ✅
4. [Закрытие BottomSheet] → Панель: Half (остается)
```

### Было (неправильно):
```
3. [Клик на карточку] → BottomSheet открывается → Панель: Expanded ❌
```

## Результат
✅ При открытии BottomSheet панель всегда переходит в состояние Half  
✅ BottomSheet не перекрывается развернутой панелью  
✅ Логичное визуальное поведение  
✅ Панель остается доступной в половинном состоянии  

Теперь панель корректно сворачивается при открытии BottomSheet!
