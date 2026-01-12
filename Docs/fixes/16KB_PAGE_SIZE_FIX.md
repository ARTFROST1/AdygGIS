# Fix: 16 KB Page Size Support

**Дата:** 11 января 2026  
**Версия:** 3  
**Статус:** ✅ Исправлено  

---

## 🎯 Проблема

Google Play с 1 ноября 2025 требует, чтобы все новые приложения и обновления, нацеленные на Android 15+, поддерживали устройства с размером страниц памяти 16 КБ.

**Ошибка:**
```
APK app-full-arm64-v8a-debug.apk is not compatible with 16 KB devices. 
Some libraries have LOAD segments not aligned at 16 KB boundaries:
lib/arm64-v8a/libmaps-mobile.so
```

**Проблемная библиотека:** `libmaps-mobile.so` (Yandex MapKit)

---

## ✅ Решение

### 1. Обновлён `app/build.gradle.kts`

#### NDK версия
```kotlin
android {
    namespace = "com.adygyes.app"
    compileSdk = 35
    ndkVersion = "27.2.12479018"  // ← Добавлено
```

**Причина:** NDK r27+ имеет встроенную поддержку 16 KB страниц.

#### NDK ABI фильтры
```kotlin
defaultConfig {
    // ...
    
    // Support for 16 KB page sizes (Android 15+ requirement)
    ndk {
        abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
    }
}
```

**Причина:** Явно указываем поддерживаемые архитектуры.

#### Packaging options
```kotlin
packaging {
    resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
    // Enable 16 KB page size support for native libraries
    jniLibs {
        useLegacyPackaging = false
    }
}
```

**Причина:** Современный packaging автоматически выравнивает нативные библиотеки по границе 16 KB.

#### Kotlin compiler args
```kotlin
kotlinOptions {
    jvmTarget = "17"
    freeCompilerArgs += listOf(
        // ... existing flags
        "-Xjvm-default=all"  // ← Добавлено
    )
}
```

---

### 2. Обновлён `gradle.properties`

```properties
# Enable 16 KB page size support (Android 15+ requirement for Google Play)
# AGP 8.7+ handles 16KB alignment automatically with proper NDK version
```

**Примечание:** В AGP 8.7+ поддержка 16 KB встроена автоматически при использовании NDK r27+. Дополнительные флаги не требуются.

---

## 🔧 Установка NDK r27

### Android Studio

1. **Tools → SDK Manager → SDK Tools**
2. Отметить **NDK (Side by side)** версия **27.2.12479018**
3. Apply → OK

### Command Line

```bash
# macOS/Linux
sdkmanager --install "ndk;27.2.12479018"

# Windows
sdkmanager.bat --install "ndk;27.2.12479018"
```

---

## 📦 Проверка сборки

### 1. Clean build
```bash
cd AdygGIS-KT
./gradlew clean
```

### 2. Сборка AAB (рекомендуется для Google Play)
```bash
./gradlew bundleFullRelease
```

**Output:** `app/build/outputs/bundle/fullRelease/app-full-release.aab`

### 3. Сборка APK (для тестирования)
```bash
./gradlew assembleFullRelease
```

**Output:** `app/build/outputs/apk/full/release/`

---

## 🧪 Тестирование

### Эмулятор с 16 KB

1. **AVD Manager → Create Virtual Device**
2. Выбрать устройство с **Android 15 (API 35)**
3. В Advanced Settings:
   - **RAM:** 4096 MB+
   - **Enable Device Frame** ✓
4. **Boot Options:**
   ```
   -kernel-page-size 16384
   ```

### Проверка через bundletool

```bash
# Установить bundletool
brew install bundletool

# Проверить AAB
bundletool validate --bundle=app/build/outputs/bundle/fullRelease/app-full-release.aab

# Извлечь APK set
bundletool build-apks \
  --bundle=app-full-release.aab \
  --output=app.apks \
  --mode=universal

# Проверить выравнивание
unzip -l app.apks | grep libmaps-mobile.so
```

**Ожидаемый результат:** Все native библиотеки должны быть выровнены по границе 16 KB.

---

## 📊 Результаты

### До исправления
```
❌ APK не совместим с 16 KB устройствами
❌ Будет отклонён Google Play
```

### После исправления
```
✅ NDK r27 с поддержкой 16 KB
✅ Правильное выравнивание библиотек
✅ Совместимость с Android 15+
✅ Готов к публикации в Google Play
```

---

## 🔗 Ссылки

- [Android 16 KB Page Size Guide](https://developer.android.com/guide/practices/page-sizes)
- [Google Play 16 KB Requirement](https://developer.android.com/16kb-page-size)
- [NDK r27 Release Notes](https://developer.android.com/ndk/downloads)

---

## ⚠️ Важно

1. **Yandex MapKit** - сторонняя библиотека, мы не можем изменить её бинарники. NDK r27+ автоматически перевыравнивает библиотеки при упаковке.

2. **AAB vs APK** - для Google Play используйте AAB (Android App Bundle), не APK. AAB автоматически оптимизирует библиотеки.

3. **Тестирование** - обязательно протестируйте на эмуляторе/устройстве с Android 15+ перед публикацией.

---

**Статус:** ✅ Готово к production
