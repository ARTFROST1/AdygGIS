# ⚠️ КРИТИЧЕСКИЕ ФАЙЛЫ - НЕ УДАЛЯТЬ!

## 🔐 KEYSTORE (КРИТИЧНО ДЛЯ ПОДПИСИ)

### Файлы:
- ❌ `keystore/adygyes-release-key.jks` - **ЕДИНСТВЕННЫЙ ключ для подписи**
- ❌ `keystore.properties` - Пароли и алиасы
- ❌ `IMPORTANT_KEYSTORE_BACKUP.md` - Инструкции восстановления

**ВНИМАНИЕ**: Потеря этих файлов = невозможность обновить приложение в Google Play!

---

## 📦 BUILD КОНФИГУРАЦИЯ

### Gradle файлы:
- ❌ `build.gradle.kts` (корень)
- ❌ `app/build.gradle.kts`
- ❌ `settings.gradle.kts`
- ❌ `gradle.properties`
- ❌ `gradle/libs.versions.toml`
- ❌ `gradle/wrapper/*`
- ❌ `local.properties`

**Зачем**: Без них проект не соберётся

---

## 🗺️ YANDEX MAPKIT

### Зависимости:
```kotlin
// НЕ УДАЛЯТЬ!
implementation(libs.yandex.mapkit.full)
```

### ProGuard правила (строки 91-97):
```proguard
# НЕ УДАЛЯТЬ! Без этого карта не будет работать
-keep class com.yandex.mapkit.** { *; }
-keep class com.yandex.runtime.** { *; }
-keep interface com.yandex.mapkit.** { *; }
-keep interface com.yandex.runtime.** { *; }
-dontwarn com.yandex.mapkit.**
-dontwarn com.yandex.runtime.**
```

### Файлы:
- ❌ Все файлы Map провайдеров (кроме Reliable, Tablet, если не используются)
- ❌ MapViewModel
- ❌ MapScreen (основной)

**Зачем**: Основной функционал приложения

---

## 💾 DATABASE & STORAGE

### Зависимости:
```kotlin
// НЕ УДАЛЯТЬ!
implementation(libs.room.runtime)
implementation(libs.room.ktx)
ksp(libs.room.compiler)
implementation(libs.datastore.preferences)
```

### Файлы:
- ❌ `AdygyesDatabase.kt`
- ❌ `AttractionDao.kt`
- ❌ `AttractionEntity.kt`
- ❌ `PreferencesManager.kt`
- ❌ `app/schemas/` (Room схемы для миграций)

**Зачем**: Хранение данных и настроек

---

## 💉 DEPENDENCY INJECTION (HILT)

### Зависимости:
```kotlin
// НЕ УДАЛЯТЬ!
implementation(libs.hilt.android)
ksp(libs.hilt.compiler)
implementation(libs.hilt.navigation.compose)
```

### Файлы:
- ❌ `di/module/AppModule.kt`
- ❌ `di/module/DatabaseModule.kt`
- ❌ `AdygyesApplication.kt` (с @HiltAndroidApp)

**Зачем**: Система внедрения зависимостей

---

## 🎨 COMPOSE UI

### Зависимости:
```kotlin
// НЕ УДАЛЯТЬ!
implementation(platform(libs.androidx.compose.bom))
implementation(libs.androidx.ui)
implementation(libs.androidx.material3)
implementation(libs.androidx.compose.foundation)
implementation(libs.androidx.compose.animation)
```

### Файлы:
- ❌ Все активные экраны (MapScreen, SearchScreen, FavoritesScreen, etc.)
- ❌ Все активные компоненты UI
- ❌ Theme файлы (Color, Theme, Typography, Dimensions)
- ❌ Navigation файлы

**Зачем**: Весь UI приложения на Compose

---

## 📊 DATA & BUSINESS LOGIC

### Assets:
- ⚠️ `app/src/main/assets/attractions.json` - legacy fallback/seed (основные данные: Supabase → Room)
- ⚠️ `geo_objects.json` - проверить использование перед удалением

### Domain Layer:
- ❌ `domain/model/Attraction.kt`
- ❌ `domain/model/GeoObject.kt`
- ❌ `domain/repository/AttractionRepository.kt`
- ❌ Все UseCases

### Data Layer:
- ❌ `data/repository/AttractionRepositoryImpl.kt`
- ❌ `data/mapper/AttractionMapper.kt`
- ❌ `data/remote/dto/AttractionDto.kt`
- ❌ `data/remote/*` и `data/sync/*` (Supabase sync)
- ⚠️ `JsonFileManager.kt` (нужен только если оставляем legacy JSON fallback)

**Зачем**: Бизнес-логика и данные приложения

---

## 🖼️ КРИТИЧНЫЕ РЕСУРСЫ

### Launcher Icons:
- ❌ `res/mipmap-*/ic_launcher*.webp` (все плотности)
- ❌ `res/mipmap-anydpi-v26/ic_launcher*.xml`
- ❌ `res/drawable/ic_launcher_background.xml`
- ❌ `res/drawable/ic_launcher_foreground.xml`

**Зачем**: Иконка приложения

### Strings:
- ❌ `res/values/strings.xml`
- ❌ `res/values-en/strings.xml`
- ❌ `res/values-ru/strings.xml` (если есть)

**Зачем**: Все тексты приложения

### Themes:
- ❌ `res/values/themes.xml`
- ❌ `res/values/colors.xml`

**Зачем**: Стилизация приложения

---

## ⚙️ CORE ANDROID

### Зависимости:
```kotlin
// НЕ УДАЛЯТЬ!
implementation(libs.androidx.core.ktx)
implementation(libs.androidx.lifecycle.runtime.ktx)
implementation(libs.androidx.lifecycle.viewmodel.compose)
implementation(libs.androidx.activity.compose)
implementation(libs.kotlinx.coroutines.core)
implementation(libs.kotlinx.coroutines.android)
implementation(libs.kotlinx.serialization.json)
```

**Зачем**: Базовая функциональность Android и Kotlin

---

## 🌍 LOCATION & NAVIGATION

### Зависимости:
```kotlin
// НЕ УДАЛЯТЬ!
implementation(libs.play.services.location)
implementation(libs.accompanist.permissions)
```

### Файлы:
- ❌ `GetLocationUseCase.kt`
- ❌ `NavigationUseCase.kt`

**Зачем**: Геолокация и построение маршрутов

---

## 🖼️ IMAGE LOADING

### Зависимости:
```kotlin
// НЕ УДАЛЯТЬ!
implementation(libs.coil.compose)
// ⚠️ coil-svg - МОЖНО удалить если SVG не используются
```

### Файлы:
- ❌ `ImageCacheManager.kt`
- ❌ `ImageCacheViewModel.kt`

**Зачем**: Загрузка и кэширование изображений

---

## 📱 ACTIVE VIEWMODELS

Проверить использование перед удалением:
- ❌ `MapViewModel.kt` - ✅ ИСПОЛЬЗУЕТСЯ (карта)
- ❌ `SearchViewModel.kt` - ✅ ИСПОЛЬЗУЕТСЯ (поиск)
- ❌ `FavoritesViewModel.kt` - ✅ ИСПОЛЬЗУЕТСЯ (избранное)
- ❌ `SettingsViewModel.kt` - ✅ ИСПОЛЬЗУЕТСЯ (настройки)
- ❌ `AttractionDetailViewModel.kt` - ✅ ИСПОЛЬЗУЕТСЯ (детали)
- ❌ `ThemeViewModel.kt` - ✅ ИСПОЛЬЗУЕТСЯ (темы)
- ❌ `LocaleViewModel.kt` - ✅ ИСПОЛЬЗУЕТСЯ (язык)
- ❌ `ImageCacheViewModel.kt` - ✅ ИСПОЛЬЗУЕТСЯ (кэш)
- ❌ `MapPreloadViewModel.kt` - ✅ ИСПОЛЬЗУЕТСЯ (предзагрузка)
- ⚠️ `MapStateViewModel.kt` - ПРОВЕРИТЬ (возможно дублирует MapViewModel)
- ✅ `DeveloperViewModel.kt` - МОЖНО УДАЛИТЬ (пустой)

---

## 📱 ACTIVE SCREENS

Проверить использование в навигации:
- ❌ `MapScreen.kt` - ✅ ИСПОЛЬЗУЕТСЯ
- ❌ `SearchScreen.kt` - ✅ ИСПОЛЬЗУЮТСЯ
- ❌ `FavoritesScreen.kt` - ✅ ИСПОЛЬЗУЕТСЯ
- ❌ `AttractionDetailScreen.kt` - ✅ ИСПОЛЬЗУЕТСЯ
- ❌ `SettingsScreen.kt` - ✅ ИСПОЛЬЗУЕТСЯ
- ❌ `SplashScreen.kt` - ✅ ИСПОЛЬЗУЕТСЯ
- ❌ `OnboardingScreen.kt` - ⚠️ ПРОВЕРИТЬ использование
- ❌ `AboutScreen.kt` - ✅ ИСПОЛЬЗУЕТСЯ
- ❌ `PrivacyPolicyScreen.kt` - ✅ ИСПОЛЬЗУЕТСЯ
- ❌ `TermsOfUseScreen.kt` - ✅ ИСПОЛЬЗУЕТСЯ
- ⚠️ `MapScreenReliable.kt` - МОЖНО УДАЛИТЬ (пустой)
- ⚠️ `MapScreenTablet.kt` - ПРОВЕРИТЬ использование

---

## 🔧 UTILITIES & HELPERS

### Критичные:
- ❌ `CacheManager.kt`
- ❌ `DataSyncManager.kt`
- ❌ `LocaleManager.kt`
- ❌ `ShareUseCase.kt`
- ❌ `NetworkUseCase.kt`

### Проверить:
- ⚠️ `WaypointMarkerProvider.kt`
- ⚠️ `GeoObjectProvider.kt`

---

## 📄 ДОКУМЕНТАЦИЯ (критичная)

Не удалять основные документы:
- ❌ `IMPORTANT_KEYSTORE_BACKUP.md`
- ❌ `Docs/Core/PRD_adygyes.md`
- ❌ `Docs/Core/Implementation_Plan.md`
- ❌ `Docs/Core/AppMap_adygyes.md`
- ❌ `Docs/Core/project_structure.md`

---

## ⚠️ ПЕРЕД УДАЛЕНИЕМ ЛЮБОГО ФАЙЛА:

### 1. Grep проверка:
```bash
grep -r "ИмяФайла" app/src/main/java/
grep -r "имя_ресурса" app/src/main/
```

### 2. Проверка в навигации:
```bash
grep -r "ИмяЭкрана" app/src/main/java/com/adygyes/app/presentation/navigation/
```

### 3. Проверка зависимостей:
```bash
# Найти все импорты класса
grep -r "import.*ИмяКласса" app/src/main/java/
```

### 4. Тест компиляции после удаления:
```bash
./gradlew clean assembleFullDebug
```

---

## 🎯 ИТОГО: БЕЗОПАСНО УДАЛЯТЬ

### Файлы:
✅ `1.txt`, `compile_test.kt`, `test_compilation.kt`  
✅ `logo.png` (в корне)  
✅ `app/src/design_reference/` (вся папка)  
✅ `app/src/main/java/.../developer/` (вся папка)  
✅ `DeveloperViewModel.kt`  
✅ `MapScreenReliable.kt`  
⚠️ `MapScreenTablet.kt` (после grep проверки)  
⚠️ `WaypointMarkerProvider.kt` (после проверки)  
⚠️ `GeoObjectProvider.kt` (после проверки)  
⚠️ `geo_objects.json` (после проверки)

### Зависимости:
✅ Retrofit (все)  
✅ OkHttp (явные, Coil подтянет свой)  
✅ Accompanist Pager  
✅ Coil SVG (если нет SVG)  
✅ Google Fonts (если не используются)

### Документация:
✅ История разработки (`Docs/Markers/markers_update/`, `Docs/Optimization/Data_optimization/`)  
✅ Дублирующие publishing guides  

---

**ЗОЛОТОЕ ПРАВИЛО**: Если сомневаешься - не удаляй! Сначала grep проверка.
