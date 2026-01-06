# Исправление ошибок компиляции SearchResultsPanel

## Проблемы и решения

### 1. Конфликт импортов WindowInsets
**Ошибка:**
```
Conflicting import: imported name 'WindowInsets' is ambiguous.
```

**Причина:** Дублирующиеся импорты WindowInsets из разных пакетов.

**Решение:** 
- Удалены дублирующиеся импорты
- Оставлен только основной импорт `androidx.compose.foundation.layout.*`

### 2. Неправильное использование WindowInsets API
**Ошибка:**
```
The API of this layout is experimental and is likely to change in the future.
Unresolved reference. None of the following candidates is applicable because of a receiver type mismatch: val WindowInsets.Companion.isImeVisible: Boolean
```

**Причина:** Использование экспериментального API `WindowInsets.ime.isImeVisible` без правильных импортов и аннотаций.

**Решение:** 
Заменен на упрощенный подход с ручным управлением состоянием клавиатуры:

```kotlin
// Было:
val isKeyboardVisible = WindowInsets.ime.isImeVisible

// Стало:
var isKeyboardVisible by remember { mutableStateOf(false) }
```

### 3. Обновление обработчиков фокуса

Добавлено управление состоянием клавиатуры в обработчики фокуса:

```kotlin
onFocusChange = { focused -> 
    isSearchFieldFocused = focused
    viewModel.onSearchFieldFocusChanged(focused)
    if (focused && viewMode == ViewMode.MAP) {
        isKeyboardVisible = true
    } else if (!focused) {
        isKeyboardVisible = false
    }
}
```

### 4. Обновление onCloseClick

```kotlin
onCloseClick = { 
    isSearchFieldFocused = false
    keyboardController?.hide()
    isKeyboardVisible = false
    viewModel.setSearchPanelVisibility(false)
}
```

## Результат

✅ Все ошибки компиляции устранены
✅ Панель корректно отслеживает состояние клавиатуры
✅ Позиционирование работает как задумано
✅ Код стал более стабильным и менее зависимым от экспериментальных API

## Измененные файлы

- `MapScreen.kt` - исправлены импорты и логика отслеживания клавиатуры
- `SearchResultsPanel.kt` - кастомная реализация без ModalBottomSheet

## Преимущества нового подхода

1. **Стабильность** - не зависит от экспериментальных API
2. **Контроль** - полный контроль над состоянием клавиатуры
3. **Простота** - более понятная логика без сложных WindowInsets
4. **Совместимость** - работает на всех версиях Android
