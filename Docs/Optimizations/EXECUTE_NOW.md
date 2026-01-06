# ⚡ ВЫПОЛНИТЬ ОПТИМИЗАЦИЮ СЕЙЧАС

> Note (2026-01-06): this optimization checklist was written for a period where `assets/attractions.json` could be treated as the primary dataset.
> With offline-first **Supabase → Room** sync, treat JSON operations below as **optional** (only for the legacy fallback/seed file) and do not remove network dependencies required for sync.

**Время**: 15-20 минут  
**Экономия**: ~2.5-2.7 MB (30-40%)

---

## ✅ ПОДГОТОВКА ЗАВЕРШЕНА

Все изменения в коде уже сделаны:
- ✅ `app/build.gradle.kts` - удалено 8 зависимостей
- ✅ `app/proguard-rules.pro` - агрессивные оптимизации

---

## 🚀 ВЫПОЛНИТЬ ПО ПОРЯДКУ

### 1️⃣ Удалить файлы (2 мин)

```bash
# В корне проекта запустить:
cleanup_optimization.bat
```

**Что удалится:**
- Тестовые файлы (compile_test.kt, test_compilation.kt, logo.png)
- design_reference/ (805 KB)
- Developer Mode файлы
- Неиспользуемые Map файлы
- Неиспользуемые провайдеры
- geo_objects.json

**Экономия**: ~1 MB

---

### 2️⃣ Минифицировать JSON (опционально, legacy fallback) (2 мин)

```bash
# Запустить:
minify_json.bat

# Проверить attractions_minified.json
# Если OK и вы хотите оптимизировать именно legacy fallback файл, заменить:
del app\src\main\assets\attractions.json
ren app\src\main\assets\attractions_minified.json attractions.json
```

**Экономия**: ~12-17 KB

---

### 3️⃣ Gradle Sync (2 мин)

```bash
# В Android Studio:
File → Sync Project with Gradle Files
```

Или в терминале:
```bash
./gradlew --refresh-dependencies
```

---

### 4️⃣ Измерить ДО оптимизации (опционально)

```bash
# Собрать текущий APK для сравнения
./gradlew assembleFullRelease

# Скопировать куда-нибудь для сравнения
copy app\build\outputs\apk\full\release\app-full-release.apk app-full-release-OLD.apk
```

---

### 5️⃣ Пересобрать APK (5 мин)

```bash
# Очистка
./gradlew clean

# Debug сборка (для проверки компиляции)
./gradlew assembleFullDebug

# Release сборка (финальная)
./gradlew assembleFullRelease
```

---

### 6️⃣ Измерить результат (1 мин)

```bash
# Посмотреть размер
ls -lh app\build\outputs\apk\full\release\*.apk

# Или
dir app\build\outputs\apk\full\release\*.apk
```

#### В Android Studio:
```
Build → Analyze APK...
Выбрать: app-full-release.apk
```

Сравни разделы:
- classes.dex
- res/
- assets/
- lib/

---

### 7️⃣ Тестирование (5 мин)

Установи APK и проверь:
- [ ] ✅ Запуск приложения
- [ ] ✅ SplashScreen загружается
- [ ] ✅ Карта отображается
- [ ] ✅ Маркеры загружаются и кликабельны
- [ ] ✅ Поиск работает
- [ ] ✅ Фильтры работают
- [ ] ✅ Избранное работает
- [ ] ✅ Детали достопримечательности
- [ ] ✅ Смена темы
- [ ] ✅ Геолокация

---

## 📊 ЗАПИСЬ РЕЗУЛЬТАТОВ

После измерения, запиши результаты в `OPTIMIZATION_PROGRESS.md`:

```markdown
| Этап | Размер APK | Экономия | % уменьшения | Дата |
|------|-----------|----------|--------------|------|
| **Исходный** | X.XX MB | - | - | 30.09.2025 |
| **Финальный** | X.XX MB | X.XX MB | XX% | 30.09.2025 |
```

---

## 🎁 БОНУС: Gradient фон (+187 KB)

**Опционально**, если хочешь ещё больше экономии:

См. `STEP4_gradient_replacement.md` для инструкции.

Замена фона на gradient даст дополнительные **187 KB**.

---

## ⚠️ ЕСЛИ ОШИБКИ

### Ошибка компиляции:
```bash
# Откатить build.gradle.kts
git checkout HEAD -- app/build.gradle.kts
git checkout HEAD -- app/proguard-rules.pro

# Пересобрать
./gradlew clean
./gradlew assembleFullDebug --stacktrace
```

### Ошибка после удаления файлов:
```bash
# Восстановить из git
git status
git checkout -- [путь к файлу]
```

---

## 📝 Git Commit

После успешного выполнения:

```bash
git add .
git commit -m "feat: APK optimization (-2.5 MB, -30-35%)

Optimizations performed:
- Removed 8 unused dependencies (Retrofit, OkHttp, Accompanist Pager, Coil SVG, Google Fonts)
- Optimized ProGuard rules (5 passes, repackage, log removal)
- Removed test files and design_reference
- Removed unused Map screens and providers
- Minified attractions.json
- Removed geo_objects.json

Changes:
- app/build.gradle.kts: removed dependencies
- app/proguard-rules.pro: aggressive optimizations
- Deleted ~1 MB of unused code and resources

Result: APK size reduced by ~2.5 MB (30-35%)
"
```

---

## 🎯 ОЖИДАЕМЫЙ РЕЗУЛЬТАТ

### До:
- APK: ~8-10 MB

### После:
- APK: ~5.5-7.5 MB
- **Экономия: ~2.5 MB (30-35%)**

### С gradient:
- APK: ~5.3-7.3 MB  
- **Экономия: ~2.7 MB (35-40%)**

---

## 📚 ДОКУМЕНТАЦИЯ

- `OPTIMIZATION_PROGRESS.md` - трекинг прогресса
- `OPTIMIZATION_COMPLETE_REPORT.md` - полный отчёт
- `STEP3_dependencies_to_remove.md` - детали зависимостей
- `STEP4_resources_optimization.md` - анализ ресурсов
- `STEP5_proguard_optimization.md` - детали ProGuard
- `QUICK_CHECKLIST.md` - быстрая шпаргалка

---

**🚀 НАЧАТЬ С ШАГА 1: cleanup_optimization.bat**

Удачи! 🎉
