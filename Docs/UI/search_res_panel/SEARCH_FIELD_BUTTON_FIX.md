# Исправление кнопок в поле поиска

## Проблема
В поле поиска отображались две кнопки (Clear и Close), что создавало путаницу в интерфейсе.

## Решение
Объединил функциональность в одну умную кнопку с адаптивным поведением.

### Логика работы:

#### Когда есть текст в поле:
- **Иконка**: `Icons.Default.Clear` (X для очистки)
- **Действие**: Очищает текст в поле
- **Описание**: "Очистить"

#### Когда поле пустое:
- **Иконка**: `Icons.Default.Close` (X для закрытия)
- **Действие**: Убирает фокус и закрывает расширенный режим
- **Описание**: "Закрыть"

### Код реализации:
```kotlin
trailingIcon = {
    // Single close button with smart behavior
    IconButton(
        onClick = {
            if (value.isNotEmpty()) {
                onValueChange("") // Clear text if there's text
            } else {
                focusManager.clearFocus() // Remove focus if no text
                onCloseClick() // Close expanded mode
            }
        }
    ) {
        Icon(
            imageVector = if (value.isNotEmpty()) Icons.Default.Clear else Icons.Default.Close,
            contentDescription = if (value.isNotEmpty()) "Очистить" else stringResource(R.string.common_close),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

## Преимущества:

✅ **Простота**: Одна кнопка вместо двух  
✅ **Интуитивность**: Поведение зависит от контекста  
✅ **Экономия места**: Меньше элементов в интерфейсе  
✅ **UX**: Пользователь всегда знает, что произойдет при нажатии  

## Поведение:

1. **Пользователь вводит текст** → кнопка становится "Clear" (очистить)
2. **Пользователь нажимает Clear** → текст очищается, кнопка становится "Close"
3. **Пользователь нажимает Close** → поле теряет фокус, панель закрывается

Теперь интерфейс стал чище и понятнее!
