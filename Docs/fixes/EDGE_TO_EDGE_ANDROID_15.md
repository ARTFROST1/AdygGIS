# Edge-to-Edge Support для Android 15+

**Дата:** 11 января 2026  
**Цель:** Обеспечить корректное отображение от края до края на Android 15 (SDK 35)

## 📋 Обзор проблемы

Начиная с Android 15, edge-to-edge включен по умолчанию для всех приложений с `targetSdk = 35`. Это требует правильной обработки system bars (status bar и navigation bar) во избежание перекрытия контента.

## ✅ Выполненные изменения

### Обзор

Все изменения внесены 11 января 2026 для обеспечения корректной работы edge-to-edge на Android 15 (SDK 35).

**Затронутые файлы (13):**

#### Activity
1. `MainActivity.kt` - улучшена настройка edge-to-edge с прозрачными system bars

#### Экраны (Screens)
2. `AttractionDetailScreen.kt` - использует `WindowInsets.systemBars`
3. `SearchScreen.kt` - использует `WindowInsets.systemBars`
4. `FavoritesScreen.kt` - использует `WindowInsets.systemBars`
5. `SettingsScreen.kt` - использует `WindowInsets.systemBars`
6. `AboutScreen.kt` - использует `WindowInsets.systemBars`
7. `TermsOfUseScreen.kt` - использует `WindowInsets.systemBars`
8. `PrivacyPolicyScreen.kt` - использует `WindowInsets.systemBars`

#### Модальные окна (Dialogs)
9. `WriteReviewModal.kt` - добавлены `.navigationBarsPadding()` и `.imePadding()`
10. `AuthModal.kt` - добавлены `.navigationBarsPadding()` и `.imePadding()`

#### Удаление deprecated API (КРИТИЧНО для Android 15)
11. `Theme.kt` - удалён импорт `accompanist-systemuicontroller`
12. `PhotoGallery.kt` - удалён импорт `accompanist-systemuicontroller`
13. `themes.xml` - удалены deprecated атрибуты `statusBarColor`, `navigationBarColor`
14. `build.gradle.kts` - удалена зависимость `accompanist-systemuicontroller`

**Компоненты, которые уже были правильно настроены:**
- ✅ `PhotoGallery.kt` - использует `.windowInsetsPadding(WindowInsets.systemBars)`
- ✅ Map-related компоненты - рисуются edge-to-edge

### Критические изменения для Android 15

#### ❌ Удалены deprecated API

**Проблема:** В Android 15 следующие методы и атрибуты больше не поддерживаются:
- `Window.setStatusBarColor()`
- `Window.setNavigationBarColor()`
- `Window.setNavigationBarDividerColor()`
- XML атрибуты `android:statusBarColor`, `android:navigationBarColor`

**Решение:** Использовать только `Activity.enableEdgeToEdge()` с параметрами `SystemBarStyle`.

#### 🗑️ Удалена библиотека `accompanist-systemuicontroller`

**Почему:** Эта библиотека внутри использует deprecated методы и вызывает warning в Android 15.

**Замена:** 
- Для MainActivity: `enableEdgeToEdge()` с `SystemBarStyle`
- Для Theme: `WindowCompat.getInsetsController()` для управления иконками

### 1. MainActivity - улучшена настройка edge-to-edge

**Файл:** `app/src/main/java/com/adygyes/app/MainActivity.kt`

```kotlin
private fun setupEdgeToEdge() {
    enableEdgeToEdge(
        // Прозрачные system bars для light/dark темы
        statusBarStyle = androidx.activity.SystemBarStyle.auto(
            lightScrim = AndroidColor.TRANSPARENT,
            darkScrim = AndroidColor.TRANSPARENT
        ),
        navigationBarStyle = androidx.activity.SystemBarStyle.auto(
            lightScrim = AndroidColor.TRANSPARENT,
            darkScrim = AndroidColor.TRANSPARENT
        )
    )
    
    // Отключение автоматического scrim для navigation bar
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        window.isNavigationBarContrastEnforced = false
    }
}
```

**Что изменилось:**
- ✅ Добавлены параметры `statusBarStyle` и `navigationBarStyle`
- ✅ Использованы прозрачные scrim для обеих тем
- ✅ `SystemBarStyle.auto()` автоматически адаптируется к теме

### 2. MainActivity - обновлен Scaffold

```kotlin
@Composable
fun AdygyesApp() {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        // Не потребляем insets, даем child composables управлять отступами
        contentWindowInsets = WindowInsets(0.dp)
    ) { innerPadding ->
        MapHost(modifier = Modifier.fillMaxSize()) {
            AdygyesNavHost(
                navController = navController,
                paddingValues = innerPadding
            )
        }
    }
}
```

**Что изменилось:**
- ✅ `contentWindowInsets = WindowInsets(0.dp)` вместо `WindowInsets(0, 0, 0, 0)`
- ✅ Передаем `innerPadding` в навигацию для гибкости

### 3. AttractionDetailScreen - правильная обработка insets

**Файл:** `app/src/main/java/com/adygyes/app/presentation/ui/screens/detail/AttractionDetailScreen.kt`

```kotlin
Scaffold(
    // Edge-to-edge: используем system bars insets
    contentWindowInsets = WindowInsets.systemBars,
    topBar = { TopAppBar(...) }
) { paddingValues ->
    // LazyColumn автоматически учитывает paddingValues от Scaffold
}
```

**Что изменилось:**
- ✅ `contentWindowInsets = WindowInsets.systemBars` вместо `WindowInsets(0, 0, 0, 0)`
- ✅ Scaffold правильно рассчитывает padding для TopAppBar и контента

## 🎨 Рекомендации для других экранов

### Pattern 1: Экраны с TopAppBar (как DetailScreen)

```kotlin
Scaffold(
    contentWindowInsets = WindowInsets.systemBars,
    topBar = { TopAppBar(...) }
) { paddingValues ->
    LazyColumn(
        contentPadding = paddingValues
    ) {
        // Контент
    }
}
```

### Pattern 2: Fullscreen компоненты (как MapHost)

```kotlin
Box(
    modifier = Modifier
        .fillMaxSize()
        // Контент рисуется edge-to-edge
) {
    // Map content
    
    // UI элементы с отступами
    TopBar(
        modifier = Modifier
            .align(Alignment.Top)
            .statusBarsPadding() // Отступ от status bar
    )
    
    BottomSheet(
        modifier = Modifier
            .align(Alignment.Bottom)
            .navigationBarsPadding() // Отступ от navigation bar
    )
}
```

### Pattern 3: Диалоги и модальные окна (как PhotoGallery)

```kotlin
Dialog(...) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars) // Общий отступ от всех system bars
    ) {
        // Контент
    }
}
```

## 🔧 Полезные модификаторы

| Modifier | Назначение |
|----------|------------|
| `.systemBarsPadding()` | Отступ от status bar + navigation bar |
| `.statusBarsPadding()` | Отступ только от status bar |
| `.navigationBarsPadding()` | Отступ только от navigation bar |
| `.windowInsetsPadding(WindowInsets.systemBars)` | То же что systemBarsPadding() |
| `.imePadding()` | Отступ от экранной клавиатуры |

## ✅ Проверка работы

### Тестирование на Android 15

1. **Эмулятор:**
   - Android Studio → Device Manager
   - Создать Pixel 8 с Android 15 (API 35)

2. **Сценарии тестирования:**
   - ✅ Карта рисуется edge-to-edge
   - ✅ TopAppBar не перекрывается status bar
   - ✅ Bottom Sheet не перекрывается navigation bar
   - ✅ Детальный экран корректно отображается
   - ✅ PhotoGallery fullscreen работает правильно
   - ✅ Переключение Light/Dark темы корректно

3. **Gesture navigation:**
   - Settings → System → Gestures → System navigation → Gesture navigation
   - Проверить, что контент не перекрывается с gesture bar

## 📚 Ссылки

- [Android 15 Edge-to-Edge](https://developer.android.com/about/versions/15/behavior-changes-15#edge-to-edge)
- [WindowInsets Guide](https://developer.android.com/develop/ui/views/layout/insets)
- [Compose Insets](https://developer.android.com/jetpack/compose/layouts/insets)

## 🔄 Обратная совместимость

Код совместим с Android 14 и ниже благодаря:
- `enableEdgeToEdge()` доступен в Activity 1.8.0+
- `WindowInsets.systemBars` доступен в Compose 1.5.0+
- Fallback для старых версий Android

## ⚠️ Важные замечания

1. **Не использовать** `WindowInsets(0, 0, 0, 0)` без причины - это отключает все insets
2. **Всегда тестировать** на эмуляторе Android 15 с gesture navigation
3. **Проверять темы** - light и dark должны работать корректно
4. **Учитывать keyboard** - использовать `.imePadding()` для полей ввода

---

*Документ обновлен: 11 января 2026*
