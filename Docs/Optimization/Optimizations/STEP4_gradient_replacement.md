# Замена dombay_background.jpg на Gradient

**Экономия**: 187 KB ⭐

---

## 📝 Инструкция

### Вариант 1: Простой gradient (максимальная экономия)

Замените строки 65-71 в `SplashScreen.kt`:

#### Было:
```kotlin
// Background image
Image(
    painter = painterResource(id = R.drawable.dombay_background),
    contentDescription = null,
    modifier = Modifier.fillMaxSize(),
    contentScale = ContentScale.FillBounds
)
```

#### Стало:
```kotlin
// Background gradient (replaces image, saves 187 KB)
Box(
    modifier = Modifier
        .fillMaxSize()
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF1A4D2E), // Темно-зеленый (горы)
                    Color(0xFF0C5329), // Основной зеленый
                    Color(0xFF0A3D1F)  // Темный зеленый (низ)
                )
            )
        )
)
```

**Добавить import:**
```kotlin
import androidx.compose.ui.graphics.Brush
```

---

### Вариант 2: Gradient с наложением (более красивый)

```kotlin
// Multi-layer background with mountain effect
Box(
    modifier = Modifier.fillMaxSize()
) {
    // Base gradient
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A4D2E),
                        Color(0xFF0C5329),
                        Color(0xFF0A3D1F)
                    )
                )
            )
    )
    
    // Mountain overlay gradient
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0A3D1F).copy(alpha = 0.3f),
                        Color.Transparent
                    ),
                    center = Offset(0.5f, 0.3f),
                    radius = 1000f
                )
            )
    )
}
```

**Добавить imports:**
```kotlin
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
```

---

## 🎨 Цветовая палитра (тема гор Адыгеи)

```kotlin
// Зеленые тона (леса и горы)
Color(0xFF1A4D2E) // Темно-зеленый
Color(0xFF0C5329) // Основной зеленый (из кнопки)
Color(0xFF0A3D1F) // Очень темный зеленый

// Альтернатива - синие тона (небо)
Color(0xFF1A237E) // Темно-синий
Color(0xFF0D47A1) // Синий
Color(0xFF01579B) // Глубокий синий

// Альтернатива - золотисто-зеленые (закат в горах)
Color(0xFF2C5530) // Темно-зеленый
Color(0xFF3E7D3E) // Зеленый
Color(0xFFD4A015) // Золотистый (от вашей палитры F6CA5F)
```

---

## 💰 Результаты

### До:
- `dombay_background.jpg`: 187 KB

### После:
- Gradient код: ~0.5 KB
- **Экономия: 186.5 KB** ⭐

---

## ⚠️ Важно

1. **Тестирование**: Запустите приложение и проверьте SplashScreen
2. **Визуальное качество**: Градиент должен хорошо сочетаться с золотистым текстом (0xFFF6CA5F)
3. **Откат**: Если не понравится, легко вернуть Image обратно

---

## 🔄 Удаление файла после замены

Если gradient понравился:
```bash
del app\src\main\res\drawable\dombay_background.jpg
```

Или переместить в архив:
```bash
mkdir archive
move app\src\main\res\drawable\dombay_background.jpg archive\
```

---

## 📋 Итоговые изменения

**Файл**: `app/src/main/java/com/adygyes/app/presentation/ui/screens/splash/SplashScreen.kt`

1. Добавить import: `androidx.compose.ui.graphics.Brush`
2. Заменить Image на Box с gradient (строки 65-71)
3. Удалить `dombay_background.jpg` (187 KB)

**Commit message:**
```
feat: replace splash background with gradient (-187 KB)

- Заменил dombay_background.jpg (187 KB) на Compose gradient
- Использованы зеленые тона, соответствующие горам Адыгеи
- Экономия: 187 KB
```

---

**Создано**: 30.09.2025  
**Рекомендация**: ⭐ Максимальная экономия при минимальных изменениях
