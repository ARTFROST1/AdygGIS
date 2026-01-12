# 📱 Adygyes - Complete Technical Stack & Implementation Guide

## 🎯 Project Overview
**Product Name:** AdygGIS (internal: Adygyes)  
**Type:** Native Android Mobile Application  
**Target Platform:** Android (Min SDK 29, Target SDK 35)  
**Architecture:** MVVM + Clean Architecture  
**Language:** Kotlin 2.0.21  
**UI Framework:** Jetpack Compose (BOM 2024.12.01)  
**Build System:** Gradle 8.13 with Kotlin DSL, AGP 8.7.3  
**Version:** 1.0.1 (versionCode 3)  
**Last Updated:** 2026-01-12

---

## 🏗️ Architecture & Design Patterns

### Architecture Layers
1. **Presentation Layer** - Jetpack Compose UI + ViewModels
2. **Domain Layer** - Use Cases + Business Logic
3. **Data Layer** - Repositories + Data Sources (Local/Remote)
4. **Framework Layer** - Android Framework dependencies

### Design Patterns
- **MVVM** (Model-View-ViewModel) with Compose
- **Repository Pattern** for data abstraction
- **Use Cases** for business logic encapsulation
- **Dependency Injection** with Hilt
- **State Management** with StateFlow/Compose State

---

## 📦 Core Technology Stack

### 🎨 UI & Design

#### Jetpack Compose (Latest Stable)
```kotlin
// Version Catalog (libs.versions.toml)
[versions]
composeBom = "2024.12.01"  # December 2024 Release

[libraries]
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-foundation = { group = "androidx.compose.foundation", name = "foundation" }
androidx-compose-animation = { group = "androidx.compose.animation", name = "animation" }
androidx-compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
```

#### Material Design 3
```kotlin
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.material3:material3-window-size-class")
// Material3 Adaptive artifacts can be added if needed.
// Keep versions aligned with the project's Version Catalog when introducing them.
```

#### Accompanist Libraries
```kotlin
[versions]
accompanist = "0.34.0"

[libraries]
accompanist-permissions = { group = "com.google.accompanist", name = "accompanist-permissions", version.ref = "accompanist" }
accompanist-systemuicontroller = { group = "com.google.accompanist", name = "accompanist-systemuicontroller", version.ref = "accompanist" }
accompanist-pager = { group = "com.google.accompanist", name = "accompanist-pager", version.ref = "accompanist" }
accompanist-pager-indicators = { group = "com.google.accompanist", name = "accompanist-pager-indicators", version.ref = "accompanist" }
```

### 🗺️ Maps & Location

#### Yandex MapKit SDK (Official)
```kotlin
[versions]
yandexMapkit = "4.8.0-full"  # Latest stable December 2024

[libraries]
# Full version includes all features (routing, search, geocoding, panoramas)
yandex-mapkit-full = { group = "com.yandex.android", name = "maps.mobile", version = "4.8.0-full" }
# Lite version for basic map display only
yandex-mapkit-lite = { group = "com.yandex.android", name = "maps.mobile", version = "4.8.0-lite" }
```

**Repository Configuration:**
```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.google.com/") }
    }
}
```

#### Location Services
```kotlin
[versions]
playServicesLocation = "21.3.0"

[libraries]
play-services-location = { group = "com.google.android.gms", name = "play-services-location", version.ref = "playServicesLocation" }
```

### 📡 Networking & API

#### Retrofit & OkHttp
```kotlin
[versions]
retrofit = "2.11.0"
okhttp = "4.12.0"
kotlinxSerialization = "1.7.3"

[libraries]
retrofit-core = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-kotlinx-serialization = { group = "com.squareup.retrofit2", name = "converter-kotlinx-serialization", version.ref = "retrofit" }
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerialization" }
```

### 💾 Local Storage

#### Room Database
```kotlin
[versions]
room = "2.6.1"

[libraries]
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
```

#### DataStore
```kotlin
[versions]
datastore = "1.1.1"

[libraries]
datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
datastore-proto = { group = "androidx.datastore", name = "datastore", version.ref = "datastore" }
```

### 🎯 Navigation

#### Compose Navigation
```kotlin
[versions]
navigation = "2.8.4"

[libraries]
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation" }
navigation-hilt = { group = "androidx.hilt", name = "hilt-navigation-compose", version = "1.2.0" }
```

### 💉 Dependency Injection

#### Hilt
```kotlin
[versions]
hilt = "2.51.1"
hiltCompiler = "1.2.0"

[libraries]
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltCompiler" }

[plugins]
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
kotlin-kapt = { id = "org.jetbrains.kotlin.kapt", version.ref = "kotlin" }
```

### 🖼️ Image Loading

#### Coil
```kotlin
[versions]
coil = "2.7.0"

[libraries]
coil-compose = { group = "io.coil-kt", name = "coil-compose", version.ref = "coil" }
coil-svg = { group = "io.coil-kt", name = "coil-svg", version.ref = "coil" }
```

### 🔄 Async & Reactive

#### Coroutines & Flow
```kotlin
[versions]
coroutines = "1.9.0"

[libraries]
kotlinx-coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
```

### 📊 Analytics & Monitoring

#### Firebase
```kotlin
[versions]
firebaseBom = "33.6.0"

[libraries]
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebaseBom" }
firebase-analytics = { group = "com.google.firebase", name = "firebase-analytics-ktx" }
firebase-crashlytics = { group = "com.google.firebase", name = "firebase-crashlytics-ktx" }
firebase-performance = { group = "com.google.firebase", name = "firebase-perf-ktx" }

[plugins]
google-services = { id = "com.google.gms.google-services", version = "4.4.2" }
firebase-crashlytics = { id = "com.google.firebase.crashlytics", version = "3.0.2" }
firebase-perf = { id = "com.google.firebase.firebase-perf", version = "1.4.2" }
```

### 🧪 Testing

#### Unit Testing
```kotlin
[versions]
junit = "4.13.2"
mockk = "1.13.13"
turbine = "1.2.0"

[libraries]
junit = { group = "junit", name = "junit", version.ref = "junit" }
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
```

#### UI Testing
```kotlin
[versions]
espresso = "3.6.1"
androidxTest = "1.6.1"

[libraries]
androidx-test-runner = { group = "androidx.test", name = "runner", version.ref = "androidxTest" }
androidx-test-rules = { group = "androidx.test", name = "rules", version.ref = "androidxTest" }
espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espresso" }
compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
```

### 🛠️ Development Tools

#### Kotlin & Android Core
```kotlin
[versions]
kotlin = "2.0.21"
agp = "8.7.3"
coreKtx = "1.13.1"
lifecycleRuntimeKtx = "2.9.4"
activityCompose = "1.9.3"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleRuntimeKtx" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

---

## 🏛️ Architecture Implementation

### 1. Project Structure
```
app/
├── src/main/java/com/adygyes/app/
│   ├── core/                  # Core utilities and extensions
│   │   ├── common/
│   │   ├── utils/
│   │   └── extensions/
│   ├── data/                  # Data layer
│   │   ├── local/
│   │   │   ├── dao/
│   │   │   ├── database/
│   │   │   └── entities/
│   │   ├── remote/
│   │   │   ├── api/
│   │   │   └── dto/
│   │   ├── repository/
│   │   └── mappers/
│   ├── domain/                # Domain layer
│   │   ├── model/
│   │   ├── repository/
│   │   └── usecase/
│   ├── presentation/          # Presentation layer
│   │   ├── navigation/
│   │   ├── theme/
│   │   ├── components/
│   │   └── screens/
│   │       ├── map/
│   │       ├── details/
│   │       ├── favorites/
│   │       ├── search/
│   │       └── settings/
│   └── di/                    # Dependency injection modules
│       ├── AppModule.kt
│       ├── DatabaseModule.kt
│       ├── NetworkModule.kt
│       └── MapModule.kt
```

### 2. Key Features Implementation

#### Map Integration
- **Primary:** Yandex MapKit SDK (Full version)
- **Features:** Map display, markers, clustering, routing, search
- **Offline:** Map caching and offline data storage

#### Data Management
- **Local:** Room database for POIs and favorites
- **Remote:** Retrofit for API calls (future backend)
- **Caching:** DataStore for preferences and settings

#### State Management
- **ViewModels** with StateFlow
- **Compose State** for UI state
- **SavedStateHandle** for process death handling

---

## 📋 Implementation Checklist

### Phase 1: Foundation Setup ✅ COMPLETE
- [x] Project structure with Kotlin & Compose
- [x] Basic Gradle configuration
- [x] Add all core dependencies
- [x] Setup version catalog (libs.versions.toml)
- [x] Configure build variants (debug/release/full/lite)

### Phase 2: Core Features ✅ COMPLETE
- [x] Yandex MapKit integration (v4.8.0-full)
- [x] Room database setup (v2.6.1)
- [x] Hilt dependency injection (v2.51.1)
- [x] Navigation component setup (v2.8.4)
- [x] Theme and design system (Material Design 3)

### Phase 3: Map Features ✅ COMPLETE
- [x] Map display with Yandex MapKit
- [x] Custom markers and clustering (Dual-Layer System)
- [x] POI data model
- [x] Location services (GPS + permissions)
- [x] Offline support (Room cache + Supabase sync)

### Phase 4: Application Features ✅ COMPLETE
- [x] Search functionality (real-time with debounce)
- [x] Favorites management (swipe-to-delete, sorting)
- [x] POI detail cards (BottomSheet)
- [x] Category filtering (CategoryCarousel, FilterBottomSheet)
- [x] Settings screen (Overlay architecture)

### Phase 5: Polish & Optimization ✅ COMPLETE
- [x] Performance optimization (ImageCacheManager)
- [x] Memory leak prevention (LeakCanary configured)
- [x] ProGuard rules (fully configured for release)
- [x] Analytics integration (Firebase prepared)
- [x] Crash reporting (Firebase Crashlytics prepared)

### Phase 6: Auth & Reviews ✅ COMPLETE (Stage 12)
- [x] Auth System (Supabase GoTrue)
  - [x] AuthRepository (signIn, signUp, signOut, refreshToken)
  - [x] AuthViewModel + AuthModal UI
  - [x] SecureAuthPreferencesManager (EncryptedSharedPreferences session persistence)
  - [x] TokenAuthenticator + ProactiveTokenRefreshInterceptor (401 + proactive refresh)
  - [x] SupabaseAuthApi (Retrofit interface)
- [x] Reviews System
  - [x] ReviewRepository (submitReview, refreshReviews, hasUserReviewed)
  - [x] ReviewViewModel + ReviewSection UI
  - [x] ReviewCard, RatingSummaryBlock, WriteReviewModal
  - [x] StatusBadge, InteractiveRating

---

## 🔐 API Keys & Configuration

### Required API Keys
1. **Yandex MapKit API Key**
   - Obtain from: https://developer.tech.yandex.com/
   - Add to `local.properties`: `MAPKIT_API_KEY=your_api_key`

### Security Best Practices
- Store API keys in `local.properties` (git-ignored)
- Use BuildConfig for accessing keys in code
- Implement certificate pinning for API calls
- Enable ProGuard/R8 for code obfuscation

---

## 📱 Compatibility Matrix

### Android Version Support
- **Minimum SDK:** 29 (Android 10)
- **Target SDK:** 35 (Android 15)
- **Compile SDK:** 35

### Device Support
- **Phones:** Full support
- **Tablets:** Adaptive layouts with Material3
- **Foldables:** WindowManager support

### Language Support
- **Primary:** Russian
- **Secondary:** English
- **Localization:** Android resource system

---

## 🚀 Build & Deployment

### Build Variants
```kotlin
android {
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    flavorDimensions += "mapVersion"
    productFlavors {
        create("lite") {
            dimension = "mapVersion"
            buildConfigField("String", "MAP_VERSION", "\"lite\"")
        }
        create("full") {
            dimension = "mapVersion"
            buildConfigField("String", "MAP_VERSION", "\"full\"")
        }
    }
}
```

### CI/CD Pipeline (Future)
- **Build:** GitHub Actions / GitLab CI
- **Testing:** Automated UI tests with Espresso
- **Distribution:** Google Play Console
- **Monitoring:** Firebase Crashlytics

---

## 🔄 Version Management

### Versioning Strategy
- **Format:** MAJOR.MINOR.PATCH (Semantic Versioning)
- **Version Code:** Auto-increment on release
- **Version Name:** Manual update following SemVer

### Dependency Updates
- Monthly security updates check
- Quarterly major dependency updates
- Use Gradle Version Catalogs for centralized management

---

## 📚 Future Enhancements

### Realized Features (Stage 12):
1. **Backend Integration** ✅
   - Supabase (PostgreSQL)
   - RESTful API with Retrofit
   - Delta sync with updated_at

2. **User Features** ✅
   - Authentication (Email/Password via Supabase GoTrue)
   - User profiles (AuthPreferencesManager)
   - Reviews and ratings (ReviewSection, WriteReviewModal)
   - Social sharing (ShareUseCase)

### Planned Features:
1. **Advanced Authentication**
   - OAuth 2.0 (Google/Apple/VK)
   - Social login integration

2. **Advanced Map Features**
   - AR navigation
   - 3D building views
   - Custom map styles
   - Heatmaps for popular areas

3. **Monetization**
   - Premium features
   - Partner locations
   - In-app purchases
   - Advertisement integration

4. **Regional Expansion**
   - Additional regions beyond Adygea
   - Multi-region data structure

---

## 📖 Documentation & Resources

### Official Documentation
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Yandex MapKit](https://yandex.com/dev/mapkit/)
- [Material Design 3](https://m3.material.io/)
- [Android Developers](https://developer.android.com/)

### Community Resources
- [Compose Samples](https://github.com/android/compose-samples)
- [Yandex MapKit Demo](https://github.com/yandex/mapkit-android-demo)
- [Architecture Components](https://developer.android.com/topic/architecture)

---

## ⚠️ Important Notes

1. **API Limits:** Yandex MapKit has request limits in free tier
2. **Offline Maps:** Available only in paid version of MapKit
3. **iOS Support:** Consider KMM for future iOS version
4. **Backend:** Start with local JSON, migrate to API later
5. **Testing:** Maintain >80% code coverage for critical paths

---

## 📝 Changelog
- 2026-01-06: Added Phase 6 (Auth & Reviews) to checklist; updated Future Enhancements to reflect realized features; bumped version to 1.0.1.
- 2025-09-22: Updated Gradle to 8.13 (wrapper), AGP to 8.7.3; aligned lifecycleRuntimeKtx to 2.9.4; documented centralized repositories in settings.gradle.kts.

*Document Version: 1.1.1*  
*Last Updated: January 12, 2026*  
*Author: CTO - AdygGIS Project*
