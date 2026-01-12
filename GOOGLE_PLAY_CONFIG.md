# ✅ Конфигурация названия приложения для Google Play

**Дата:** 11 января 2026  
**Статус:** ИСПРАВЛЕНО И ПРОВЕРЕНО ✅

---

## 📱 Правильная конфигурация для Google Play

### 1. Application ID (Package Name)

**Файл:** `app/build.gradle.kts`

```kotlin
defaultConfig {
    applicationId = "com.adygyes.app"  // ✅ Уникальный ID для Google Play
    minSdk = 29
    targetSdk = 35
    versionCode = 3                     // ✅ Номер версии (increment для каждого релиза)
    versionName = "1.0.2"               // ✅ Пользовательская версия
}
```

**Варианты (flavors) с разными ID:**
```kotlin
productFlavors {
    create("full") {
        applicationIdSuffix = ".full"   // → com.adygyes.app.full
        versionNameSuffix = "-full"     // → 1.0.2-full
    }
    create("lite") {
        applicationIdSuffix = ".lite"   // → com.adygyes.app.lite
        versionNameSuffix = "-lite"     // → 1.0.2-lite
    }
}
```

### 2. App Name (отображается в Google Play)

**Файл:** `app/src/main/res/values/strings.xml`

```xml
<resources>
    <string name="app_name">AdygGIS</string>  ✅ User-facing название
</resources>
```

**Файл:** `app/src/main/AndroidManifest.xml`

```xml
<application
    android:label="@string/app_name"  ✅ Ссылка на строковый ресурс (НЕ хардкод!)
    ...>
```

**Почему важна ссылка на @string/app_name:**
- ✅ Легко локализовать (можно создать `values-en/strings.xml`)
- ✅ Единое место для изменения названия
- ✅ Автоматически используется в Google Play

### 3. Project Name (внутреннее)

**Файл:** `settings.gradle.kts`

```kotlin
rootProject.name = "AdygGIS"  ✅ Соответствует user-facing названию
```

---

## 📊 Как это отображается в Google Play

### В Google Play Console

| Поле | Значение | Источник |
|------|----------|----------|
| **Package name** | `com.adygyes.app` | `applicationId` в build.gradle.kts |
| **App name** | `AdygGIS` | `@string/app_name` (может быть переопределено в консоли) |
| **Version** | `1.0.2 (3)` | `versionName (versionCode)` |

### На устройстве пользователя

| Место | Значение |
|-------|----------|
| **Launcher (иконка)** | AdygGIS |
| **Настройки → Приложения** | AdygGIS |
| **Recent Apps** | AdygGIS |

---

## 🌍 Локализация (опционально)

Для поддержки английского языка уже создан файл:

**Файл:** `app/src/main/res/values-en/strings.xml`

```xml
<resources>
    <string name="app_name">AdygGIS</string>
</resources>
```

Можно добавить другие языки:
- `values-ru/strings.xml` - Русский
- `values-en/strings.xml` - English (уже есть)
- `values-fr/strings.xml` - Français
- И т.д.

---

## ✅ Проверка перед публикацией

### 1. Application ID уникален

```bash
# Проверить в Google Play Console
# Package name: com.adygyes.app должен быть свободен или принадлежать вам
```

### 2. Version Code увеличивается

```kotlin
// Каждый новый релиз ДОЛЖЕН иметь versionCode больше предыдущего
versionCode = 3  // Текущий
// Следующий релиз:
versionCode = 4  // ← ОБЯЗАТЕЛЬНО увеличить!
```

### 3. App Name корректен

```bash
# Запустить на устройстве и проверить:
# - Название на домашнем экране
# - Название в списке приложений
# - Название в Recent Apps
```

### 4. Создать release build

```bash
cd /Users/artfrost/Projects/AdygGIS/AdygGIS-KT

# Для AAB (рекомендуется для Google Play)
./gradlew bundleFullRelease

# Или для APK
./gradlew assembleFullRelease

# Результат:
# AAB: app/build/outputs/bundle/fullRelease/app-full-release.aab
# APK: app/build/outputs/apk/full/release/app-full-release.apk
```

---

## 🚀 Публикация в Google Play

### Шаг 1: Создать приложение в Google Play Console

1. Зайти в https://play.google.com/console
2. Создать новое приложение
3. Указать:
   - **App name:** AdygGIS
   - **Default language:** Русский
   - **App or game:** App
   - **Free or paid:** Free

### Шаг 2: Загрузить AAB

```bash
# Создать signed AAB с keystore
./gradlew bundleFullRelease

# Файл будет в:
# app/build/outputs/bundle/fullRelease/app-full-release.aab
```

### Шаг 3: Заполнить метаданные

- **App name:** AdygGIS (макс. 50 символов)
- **Short description:** Краткое описание (макс. 80 символов)
- **Full description:** Полное описание (макс. 4000 символов)
- **Screenshots:** Минимум 2, рекомендуется 8
- **Feature graphic:** 1024 x 500 px

### Шаг 4: Установить категорию

- **Category:** Maps & Navigation
- **Content rating:** For Everyone
- **Target audience:** 13+

---

## ⚠️ Важные замечания

### 1. Application ID нельзя изменить после публикации

После первой публикации `com.adygyes.app` закреплён навсегда.  
Чтобы изменить ID, нужно создавать новое приложение.

### 2. Version Code всегда увеличивается

```kotlin
// ❌ НЕПРАВИЛЬНО
versionCode = 3  // Первый релиз
versionCode = 3  // Обновление - НЕ РАБОТАЕТ!

// ✅ ПРАВИЛЬНО
versionCode = 3  // Первый релиз
versionCode = 4  // Обновление
versionCode = 5  // Следующее обновление
```

### 3. App Name может отличаться

- **В коде:** `@string/app_name` = "AdygGIS"
- **В Google Play:** Можно переопределить в Console (независимо от кода)
- **Рекомендация:** Держать одинаковым для консистентности

---

## 📋 Текущая конфигурация (проверено)

| Файл | Параметр | Значение | Статус |
|------|----------|----------|--------|
| `build.gradle.kts` | applicationId | com.adygyes.app | ✅ Корректно |
| `build.gradle.kts` | versionCode | 3 | ✅ Корректно |
| `build.gradle.kts` | versionName | 1.0.2 | ✅ Корректно |
| `strings.xml` | app_name | AdygGIS | ✅ Корректно |
| `AndroidManifest.xml` | android:label | @string/app_name | ✅ Исправлено (было хардкод) |
| `settings.gradle.kts` | rootProject.name | AdygGIS | ✅ Исправлено (было Adygyes) |

---

## 🎉 Готово к публикации!

Все конфигурационные файлы настроены правильно для публикации в Google Play.

**Следующие шаги:**
1. ✅ Увеличить `versionCode` для нового релиза
2. ✅ Создать signed release AAB: `./gradlew bundleFullRelease`
3. ✅ Загрузить в Google Play Console
4. ✅ Заполнить метаданные приложения
5. ✅ Отправить на review

---

*Последнее обновление: 11 января 2026*
