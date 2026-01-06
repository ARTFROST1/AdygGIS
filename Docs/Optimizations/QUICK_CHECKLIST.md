# ⚡ Быстрый чеклист оптимизации APK

> Note (2026-01-06): under offline-first Supabase sync, treat `assets/attractions.json` as **legacy fallback/seed**.
> The JSON minification step is optional and should only be used if you keep shipping the fallback JSON.

**Время выполнения**: 15-20 минут  
**Ожидаемая экономия**: ~2.5-2.7 MB (30-40%)

---

## ✅ ПОДГОТОВКА УЖЕ ЗАВЕРШЕНА

- ✅ `app/build.gradle.kts` - обновлен (удалено 8 зависимостей)
- ✅ `app/proguard-rules.pro` - обновлен (агрессивные оптимизации)
- ✅ Создан `minify_json.bat`
- ✅ Создана документация

---

## 📋 ВЫПОЛНИТЬ СЕЙЧАС

### 1. Удалить мусорные файлы (2 мин)

```bash
# Тестовые файлы
del compile_test.kt
del test_compilation.kt  
del logo.png

# Design файлы (805 KB)
rmdir /S /Q app\src\design_reference

# Developer Mode (если не нужен)
rmdir /S /Q app\src\main\java\com\adygyes\app\presentation\ui\screens\developer
del app\src\main\java\com\adygyes\app\presentation\viewmodel\DeveloperViewModel.kt

# Пустая папка
rmdir app\src\main\assets\images
```

---

### 2. Удалить неиспользуемый код (1 мин)

```bash
del app\src\main\java\com\adygyes\app\presentation\ui\screens\map\MapScreenReliable.kt
del app\src\main\java\com\adygyes\app\presentation\ui\screens\map\MapScreenTablet.kt
del app\src\main\java\com\adygyes\app\presentation\ui\screens\map\WaypointMarkerProvider.kt
del app\src\main\java\com\adygyes\app\presentation\ui\screens\map\GeoObjectProvider.kt
del app\src\main\assets\geo_objects.json
```

---

### 3. Минифицировать JSON (опционально, legacy fallback) (2 мин)

```bash
# Запустить скрипт
minify_json.bat

# Проверить результат attractions_minified.json

# Если OK и вы хотите оптимизировать legacy fallback файл - заменить:
del app\src\main\assets\attractions.json
ren app\src\main\assets\attractions_minified.json attractions.json
```

---

### 4. Gradle Sync (2 мин)

```bash
# В Android Studio:
File → Sync Project with Gradle Files

# Или в терминале:
./gradlew --refresh-dependencies
```

---

### 5. Пересобрать APK (5 мин)

```bash
# Очистка
./gradlew clean

# Проверка debug
./gradlew assembleFullDebug

# Финальная release сборка
./gradlew assembleFullRelease
```

---

### 6. Измерить результат (1 мин)

```bash
# PowerShell
ls -lh app/build/outputs/apk/full/release/*.apk

# Или
Build → Analyze APK... (в Android Studio)
```

---

### 7. Тестирование (5 мин)

- [ ] Запуск приложения
- [ ] SplashScreen загружается
- [ ] Карта работает
- [ ] Маркеры отображаются
- [ ] Поиск работает
- [ ] Фильтры работают
- [ ] Избранное работает

---

## 🎁 БОНУС: Gradient фон (+187 KB)

**Опционально**, если хочешь дополнительную экономию:

1. Открой `STEP4_gradient_replacement.md`
2. Скопируй код gradient
3. Замени в `SplashScreen.kt` (строки 65-71)
4. Удали `app\src\main\res\drawable\dombay_background.jpg`
5. Пересобери APK

**Дополнительная экономия**: +187 KB

---

## 📊 ИТОГОВАЯ ТАБЛИЦА

| Шаг | Действие | Время | Экономия |
|-----|----------|-------|----------|
| 0 | Подготовка (УЖЕ СДЕЛАНО) | - | - |
| 1 | Удаление мусора | 2 мин | ~1 MB |
| 2 | Удаление кода | 1 мин | ~30 KB |
| 3 | JSON минификация | 2 мин | ~15 KB |
| 4 | Gradle Sync | 2 мин | - |
| 5 | Пересборка APK | 5 мин | ~2.7 MB (из зависимостей + ProGuard) |
| 6 | Измерение | 1 мин | - |
| 7 | Тестирование | 5 мин | - |
| **ИТОГО** | | **18 мин** | **~2.5 MB (30-35%)** |
| БОНУС | Gradient | +3 мин | **+187 KB (итого ~2.7 MB)** |

---

## ⚠️ ЕСЛИ ЧТО-ТО ПОШЛО НЕ ТАК

### Откатить build.gradle.kts:
```bash
git checkout HEAD -- app/build.gradle.kts
```

### Откатить proguard-rules.pro:
```bash
git checkout HEAD -- app/proguard-rules.pro
```

### Очистить и пересобрать:
```bash
./gradlew clean
./gradlew assembleFullDebug --stacktrace
```

---

## 📝 Git Commit (после успеха)

```bash
git add .
git commit -m "feat: APK optimization (-2.5 MB, -30-35%)

- Removed unused dependencies (Retrofit, OkHttp, etc.)
- Optimized ProGuard rules with aggressive settings
- Removed test files and design_reference
- Removed unused Map screens and providers
- Minified attractions.json
- Removed unused geo_objects.json

Result: APK size reduced by ~2.5 MB (30-35%)"
```

---

**🚀 НАЧАТЬ С ШАГА 1!**

Удачи! 🎉
