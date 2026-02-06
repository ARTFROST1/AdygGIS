# Project Structure Guide

**Last Updated:** 2026-02-06  
**App Version:** 1.0.1 (versionCode: 3)  
**Current Status:** Offline-first Supabase sync + Auth/Reviews + app_settings integrated

> Branding: User-facing app name is "AdygGIS". Internal code/package retains "Adygyes" to avoid breaking changes.

## 🎯 Ключевые архитектурные достижения:
- **✅ 🔐 Система авторизации (Auth):** Supabase GoTrue через Retrofit, SecureAuthPreferencesManager для сессий
- **✅ ⭐ Система отзывов (Reviews):** ReviewSection, ReviewCard, WriteReviewModal с модерацией
- **✅ ⚙️ app_settings (Admin-managed):** динамические контакты/ссылки/тексты в Settings/About/Privacy/Terms через AppSettingsManager + SyncService
- **✅ 🎬 Премиум система анимации маркеров:** Ультра-плавная 12-кадровая анимация с предзагруженными изображениями для кинематографического UX
- **✅ Dual-Layer Marker System:** Революционная архитектура - нативные визуальные маркеры + Compose интерактивный слой для 100% надежности кликов
- **✅ 🆕 SearchResultsPanel:** Интерактивная панель результатов поиска с двухстадийной архитектурой (Expanded/Half), drag-жестами и умным позиционированием
- **✅ 🔒 Защита от двойного клика:** Надежная блокировка навигации во время переходов для всех экранов (Settings/About/Privacy/Terms)
- **✅ 🎨 Settings как Overlay:** Settings/About/Privacy/Terms выезжают поверх карты точно как List mode - единая AnimatedContent система с идентичными анимациями
- **✅ 📦 MapScreenContainer:** Новый архитектурный паттерн - контейнер управляет Map/Settings/About/Privacy/Terms как overlay слоями, не Navigation routes
- **✅ Предзагрузка карты:** Фоновая подготовка во время splash screen для мгновенной анимации маркеров
- **✅ Динамическая кластеризация:** Умная группировка маркеров на основе уровня масштабирования с визуальными индикаторами
- **✅ Единый интерфейс:** Интегрированная навигация с переключением Карта/Список
- **✅ Полная интеграция избранного:** CategoryCarousel + переключение Список/Плитки + Сортировка в MapScreen
- **✅ Умное центрирование карты:** Автоматическое позиционирование результатов поиска в верхней части экрана с учетом панели
- **✅ Продвинутое кэширование:** ImageCacheManager (предзагрузка изображений для маркеров)
- **✅ Offline-first данные:** Supabase → Room cache (delta sync по `updated_at`), UI всегда читает из Room
- **✅ Legacy JSON режим:** `assets/attractions.json` используется только если Supabase не сконфигурирован (или как seed)
- **✅ Полная локализация:** 100% русский интерфейс + архитектура для английского
- **✅ Исправления совместимости:** Решены проблемы Canvas с hardware bitmap для маркеров карты
- **✅ Готов к публикации:** Все этапы MVP завершены, приложение готово к Google Play Store

## Project Directory Layout

> Note: The tree below is a high-level overview (as of 2026-01-12) and may omit legacy/unused components for clarity.

```
AdygGIS-KT/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── assets/                    # Static data files
│   │   │   │   ├── attractions.json      # Legacy fallback/seed data
│   │   │   │   └── geo_objects.json      # Geographic objects data
│   │   │   ├── java/com/adygyes/app/
│   │   │   │   ├── core/                 # Core utilities
│   │   │   │   ├── data/                 # Data layer
│   │   │   │   │   ├── local/            # Local data sources
│   │   │   │   │   │   ├── cache/        # Cache management
│   │   │   │   │   │   │   ├── CacheManager.kt
│   │   │   │   │   │   │   └── ImageCacheManager.kt      # ⭐ Advanced image caching
│   │   │   │   │   │   ├── dao/          # Room DAOs
│   │   │   │   │   │   │   ├── AttractionDao.kt
│   │   │   │   │   │   │   └── ReviewDao.kt
│   │   │   │   │   │   ├── database/     # Room database
│   │   │   │   │   │   │   └── AdygyesDatabase.kt
│   │   │   │   │   │   ├── entities/     # Room entities
│   │   │   │   │   │   │   ├── AttractionEntity.kt
│   │   │   │   │   │   │   └── ReviewEntity.kt
│   │   │   │   │   │   ├── locale/       # Locale management
│   │   │   │   │   │   │   └── LocaleManager.kt
│   │   │   │   │   │   ├── preferences/  # DataStore preferences
│   │   │   │   │   │   │   ├── PreferencesManager.kt
│   │   │   │   │   │   │   ├── AuthPreferencesManager.kt
│   │   │   │   │   │   │   ├── SecureAuthPreferencesManager.kt  # 🔐 Secure auth session storage
│   │   │   │   │   │   │   └── AppSettingsManager.kt            # ⚙️ app_settings cache (DataStore)
│   │   │   │   │   │   └── JsonFileManager.kt  # Legacy JSON reader (fallback/seed)
│   │   │   │   │   ├── mapper/           # Data mappers
│   │   │   │   │   │   └── AttractionMapper.kt
│   │   │   │   │   ├── remote/           # Supabase REST (Retrofit)
│   │   │   │   │   │   ├── api/          # API interfaces
│   │   │   │   │   │   │   ├── SupabaseApiService.kt     # Attractions & reviews API
│   │   │   │   │   │   │   └── SupabaseAuthApi.kt        # 🔐 GoTrue Auth API
│   │   │   │   │   │   ├── config/       # Supabase configuration
│   │   │   │   │   │   │   └── SupabaseConfig.kt
│   │   │   │   │   │   ├── dto/          # Data transfer objects
│   │   │   │   │   │   │   ├── AttractionDto.kt
│   │   │   │   │   │   │   ├── SyncMetadataDto.kt
│   │   │   │   │   │   │   ├── AuthDto.kt                # 🔐 Auth request/response
│   │   │   │   │   │   │   ├── ReviewDto.kt              # ⭐ Review DTO
│   │   │   │   │   │   │   ├── CreateReviewDto.kt        # ⭐ Review submission
│   │   │   │   │   │   │   ├── ReviewReactionDto.kt      # ⭐ Review reactions
│   │   │   │   │   │   │   └── AppSettingDto.kt          # ⚙️ app_settings DTO
│   │   │   │   │   │   ├── SupabaseRemoteDataSource.kt
│   │   │   │   │   │   └── ReviewsRemoteDataSource.kt    # ⭐ Reviews data source
│   │   │   │   │   ├── repository/       # Repository implementations
│   │   │   │   │   │   ├── AttractionRepositoryImpl.kt
│   │   │   │   │   │   ├── AuthRepository.kt             # 🔐 Auth business logic
│   │   │   │   │   │   └── ReviewRepository.kt           # ⭐ Reviews business logic
│   │   │   │   │   ├── sync/             # Data synchronization
│   │   │   │   │   │   ├── DataSyncManager.kt
│   │   │   │   │   │   ├── SyncService.kt
│   │   │   │   │   │   ├── SyncManager.kt
│   │   │   │   │   │   ├── SyncModels.kt
│   │   │   │   │   │   └── NetworkMonitor.kt
│   │   │   │   │   └── util/             # Data utilities
│   │   │   │   ├── domain/               # Business logic
│   │   │   │   │   ├── model/            # Domain models
│   │   │   │   │   │   ├── Attraction.kt
│   │   │   │   │   │   ├── GeoObject.kt
│   │   │   │   │   │   ├── Review.kt                     # ⭐ Review model
│   │   │   │   │   │   └── User.kt                       # 🔐 User model
│   │   │   │   │   ├── repository/       # Repository interfaces
│   │   │   │   │   │   └── AttractionRepository.kt
│   │   │   │   │   └── usecase/          # Use cases
│   │   │   │   │       ├── AttractionDisplayUseCase.kt
│   │   │   │   │       ├── ContactActionUseCase.kt       # 📞 Contact actions
│   │   │   │   │       ├── DataSyncUseCase.kt
│   │   │   │   │       ├── GetLocationUseCase.kt
│   │   │   │   │       ├── NavigationUseCase.kt
│   │   │   │   │       ├── NetworkUseCase.kt
│   │   │   │   │       └── ShareUseCase.kt
│   │   │   │   ├── di/                   # Dependency injection
│   │   │   │   │   ├── module/
│   │   │   │   │   │   ├── AppModule.kt
│   │   │   │   │   │   └── DatabaseModule.kt
│   │   │   │   │   └── qualifier/        # Hilt qualifiers
│   │   │   │   ├── presentation/         # UI layer
│   │   │   │   │   ├── navigation/       # Navigation setup
│   │   │   │   │   │   ├── AdygyesNavHost.kt
│   │   │   │   │   │   └── NavDestinations.kt
│   │   │   │   │   ├── theme/            # Material Design 3 theme
│   │   │   │   │   │   ├── Color.kt
│   │   │   │   │   │   ├── Dimensions.kt
│   │   │   │   │   │   ├── Shapes.kt
│   │   │   │   │   │   ├── Theme.kt
│   │   │   │   │   │   └── Typography.kt
│   │   │   │   │   ├── ui/               # Screens and components
│   │   │   │   │   │   ├── screens/
│   │   │   │   │   │   │   ├── splash/   # Splash screen
│   │   │   │   │   │   │   │   └── SplashScreen.kt
│   │   │   │   │   │   │   ├── map/      # Map screen (unified)
│   │   │   │   │   │   │   │   ├── MapScreenContainer.kt     # 🎨 Container for overlays
│   │   │   │   │   │   │   │   ├── MapScreen.kt              # 🎬 Main map screen
│   │   │   │   │   │   │   │   ├── MapHost.kt                # Persistent MapView
│   │   │   │   │   │   │   │   ├── MapStyleProvider.kt       # Map styling
│   │   │   │   │   │   │   │   ├── CategoryMarkerProvider.kt # Category markers
│   │   │   │   │   │   │   │   └── TextImageProvider.kt      # Text image utils
│   │   │   │   │   │   │   ├── detail/   # Attraction details
│   │   │   │   │   │   │   │   └── AttractionDetailScreen.kt
│   │   │   │   │   │   │   ├── favorites/ # Favorites management
│   │   │   │   │   │   │   │   └── FavoritesScreen.kt
│   │   │   │   │   │   │   ├── search/   # Search functionality
│   │   │   │   │   │   │   │   └── SearchScreen.kt
│   │   │   │   │   │   │   ├── settings/ # App settings (overlay mode)
│   │   │   │   │   │   │   │   ├── SettingsScreen.kt
│   │   │   │   │   │   │   │   └── SettingsComponents.kt
│   │   │   │   │   │   │   ├── about/    # About screen (overlay mode)
│   │   │   │   │   │   │   │   └── AboutScreen.kt
│   │   │   │   │   │   │   ├── privacy/  # Privacy policy (overlay mode)
│   │   │   │   │   │   │   │   └── PrivacyPolicyScreen.kt
│   │   │   │   │   │   │   ├── terms/    # Terms of use (overlay mode)
│   │   │   │   │   │   │   │   └── TermsOfUseScreen.kt
│   │   │   │   │   │   │   └── onboarding/ # First launch
│   │   │   │   │   │   │       └── OnboardingScreen.kt
│   │   │   │   │   │   └── components/   # Reusable UI components
│   │   │   │   │   │       ├── AccessibilityHelper.kt
│   │   │   │   │   │       ├── AttractionBottomSheet.kt
│   │   │   │   │   │       ├── AttractionCard.kt
│   │   │   │   │   │       ├── AttractionsList.kt
│   │   │   │   │   │       ├── CategoryCarousel.kt
│   │   │   │   │   │       ├── CategoryChip.kt
│   │   │   │   │   │       ├── CategoryFilterBottomSheet.kt
│   │   │   │   │   │       ├── ClickableContactInfo.kt       # 📞 Clickable contacts
│   │   │   │   │   │       ├── CompactAttractionCard.kt
│   │   │   │   │   │       ├── DataUpdateOverlay.kt
│   │   │   │   │   │       ├── EmptyState.kt
│   │   │   │   │   │       ├── HapticFeedback.kt
│   │   │   │   │   │       ├── LoadingShimmer.kt
│   │   │   │   │   │       ├── PhotoGallery.kt
│   │   │   │   │   │       ├── RatingBar.kt
│   │   │   │   │   │       ├── RatingComingSoonDialog.kt
│   │   │   │   │   │       ├── SearchBar.kt
│   │   │   │   │   │       ├── SearchResultsHeader.kt
│   │   │   │   │   │       ├── SearchResultsPanel.kt
│   │   │   │   │   │       ├── SearchResultsWithCategories.kt
│   │   │   │   │   │       ├── UnifiedCategoryCarousel.kt
│   │   │   │   │   │       ├── auth/                         # 🔐 Auth components
│   │   │   │   │   │       │   └── AuthModal.kt
│   │   │   │   │   │       └── reviews/                      # ⭐ Reviews components
│   │   │   │   │   │           ├── ReviewSection.kt
│   │   │   │   │   │           ├── ReviewCard.kt
│   │   │   │   │   │           ├── RatingSummaryBlock.kt
│   │   │   │   │   │           ├── WriteReviewModal.kt
│   │   │   │   │   │           └── StatusBadge.kt
│   │   │   │   │   └── viewmodel/        # ViewModels
│   │   │   │   │       ├── AttractionDetailViewModel.kt
│   │   │   │   │       ├── AuthViewModel.kt                  # 🔐 Auth state
│   │   │   │   │       ├── ContactActionViewModel.kt         # 📞 Contact actions
│   │   │   │   │       ├── FavoritesViewModel.kt
│   │   │   │   │       ├── ImageCacheViewModel.kt
│   │   │   │   │       ├── LocaleViewModel.kt
│   │   │   │   │       ├── MapViewModel.kt
│   │   │   │   │       ├── MapPreloadViewModel.kt
│   │   │   │   │       ├── MapStateViewModel.kt
│   │   │   │   │       ├── ReviewViewModel.kt                # ⭐ Reviews state
│   │   │   │   │       ├── AppSettingsViewModel.kt            # ⚙️ app_settings state
│   │   │   │   │       ├── SearchViewModel.kt
│   │   │   │   │       ├── SettingsViewModel.kt
│   │   │   │   │       └── ThemeViewModel.kt
│   │   │   │   ├── AdygyesApplication.kt  # Application class
│   │   │   │   └── MainActivity.kt        # Main activity
│   │   │   └── res/
│   │   │       ├── values/
│   │   │       │   ├── strings.xml
│   │   │       │   ├── colors.xml
│   │   │       │   └── themes.xml
│   │   │       ├── values-en/            # English translations
│   │   │       ├── raw/                  # JSON data files
│   │   │       └── drawable/             # Icons and images
│   │   ├── androidTest/                  # Instrumented tests
│   │   └── test/                         # Unit tests
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
│   ├── wrapper/
│   └── libs.versions.toml          # Version catalog
├── Docs/                           # Documentation
│   ├── README.md                  # Docs index (core docs are in Docs/ root)
│   ├── fixes/                     # Bug fix documentation
│   ├── Markers/                   # Marker system documentation
│   ├── Optimization/              # APK/data optimization guides
│   ├── Release/                   # Publishing/release guides
│   ├── UI/                        # UI subsystem docs
│   └── Integrations/              # External integrations (Supabase, etc.)
├── build.gradle.kts               # Project build configuration
├── settings.gradle.kts            # Project settings
├── gradle.properties             # Gradle properties
└── README.md                     # Project overview
```
├── API_SETUP.md                  # API configuration guide
└── README.md                     # Project overview
```

### Supabase sync (source of truth)

Current offline-first data flow is implemented in these packages:

**API Layer:**
- `data/remote/api/SupabaseApiService.kt` - PostgREST endpoints for attractions & reviews
- `data/remote/api/SupabaseAuthApi.kt` - GoTrue Auth API (login, register, refresh, logout)

**Configuration:**
- `data/remote/config/SupabaseConfig.kt` - URL + API key configuration

**DTOs:**
- `data/remote/dto/AttractionDto.kt` - Attraction data transfer object
- `data/remote/dto/SyncMetadataDto.kt` - Sync metadata
- `data/remote/dto/AuthDto.kt` - Auth request/response DTOs (SignIn, SignUp, RefreshToken)
- `data/remote/dto/ReviewDto.kt` - Review DTO
- `data/remote/dto/CreateReviewDto.kt` - Review submission DTO
- `data/remote/dto/ReviewReactionDto.kt` - Review reactions DTO

**Data Sources:**
- `data/remote/SupabaseRemoteDataSource.kt` - Attractions remote data source
- `data/remote/ReviewsRemoteDataSource.kt` - Reviews remote data source

**Sync Layer:**
- `data/sync/SyncService.kt` - Delta sync using `updated_at` + tombstones
- `data/sync/SyncManager.kt` - Sync orchestration
- `data/sync/DataSyncManager.kt` - Data sync manager
- `data/sync/NetworkMonitor.kt` - Network connectivity monitoring
- `data/sync/SyncModels.kt` - Sync data models
- `data/sync/ReviewSyncService.kt` - Reviews sync service

**Repositories:**
- `data/repository/AttractionRepositoryImpl.kt` - Attractions repository
- `data/repository/AuthRepository.kt` - Auth business logic (signIn, signUp, signOut, refreshToken)
- `data/repository/ReviewRepository.kt` - Reviews business logic (submitReview, refreshReviews, hasUserReviewed)

**Preferences:**
- `data/local/preferences/PreferencesManager.kt` - App preferences (DataStore)
- `data/local/preferences/SecureAuthPreferencesManager.kt` - Auth session persistence (EncryptedSharedPreferences with AES-256 encryption)
- `data/local/preferences/AuthPreferencesManager.kt` - Legacy auth preferences (deprecated, kept for compatibility)

## Key Architecture Patterns

### 🏗️ **Clean Architecture Implementation**
- **Domain Layer**: Business logic and entities
- **Data Layer**: Repository pattern with local/remote data sources
- **Presentation Layer**: MVVM with Compose UI

### 🎯 **Реализованные ключевые функции**

#### ✅ **MVP ЭТАПЫ ЗАВЕРШЕНЫ - Проект готов к публикации (100% Complete):**
- **✅ UI/UX Review**: Комплексный обзор всех экранов и взаимодействий
- **✅ Оптимизация производительности**: Улучшения производительности карты и использования памяти
- **✅ Продвинутое кэширование изображений**: ImageCacheManager с Coil интеграцией
- **✅ Offline-first sync**: Supabase → Room cache, UI reads Room (JSON only as fallback/seed)
- **✅ Dual-Layer маркеры**: 100% надежность кликов с нативными визуальными маркерами
- **✅ Премиум анимации**: 12-кадровая система анимации с предзагруженными изображениями
- **✅ Полная локализация**: Русский интерфейс + архитектура для мультиязычности
- **✅ Release сборка**: Подписанный APK готов для Google Play Store

#### ✅ **Stage 12 - Post-MVP Расширения (РЕАЛИЗОВАНО):**
- **🔐 Auth System (Supabase GoTrue):**
  - Email/Password авторизация через REST API
  - AuthRepository с signIn, signUp, signOut, refreshToken
  - AuthViewModel для управления состоянием авторизации
  - AuthModal (Compose) - UI для входа/регистрации
  - SecureAuthPreferencesManager - encrypted хранение сессии + expires_at
  - TokenAuthenticator/ProactiveTokenRefreshInterceptor - auto refresh (401 + проактивный)
  - Приложение работает без входа; Auth требуется только для отзывов

- **⭐ Reviews System (Supabase):**
  - ReviewRepository с submitReview, refreshReviews, hasUserReviewed, deleteReview
  - ReviewViewModel для управления состоянием отзывов
  - ReviewSection - контейнер секции отзывов
  - ReviewCard - карточка отдельного отзыва
  - RatingSummaryBlock - сводка рейтинга с CTA
  - WriteReviewModal - модальное окно написания отзыва
  - StatusBadge - бейджи статуса отзыва
  - Чтение approved отзывов из Supabase
  - Отправка через user JWT (статус pending по умолчанию)

#### ✅ **Stage 9 Completed - Advanced Map Features:**
- **Revolutionary Architecture** - Native visual + Compose interactive layers
- **100% Click Reliability** - Perfect marker tap handling with transparent overlay
- **Zero Visual Lag** - Native MapKit rendering with hardware acceleration
- **Full Map Interactivity** - Preserved pan, zoom, rotate functionality
- **Production Ready** - Optimized performance with minimal overhead
- **Навигация (актуально):** `AdygyesNavHost` маршруты (Map/Search/Favorites) + Settings/About/Privacy/Terms как overlay внутри `MapScreenContainer`
- **Persistent MapHost** - Single `MapView` at app root, `NavHost` rendered inside `MapHost`
- **Camera state persistence** - `MapStateViewModel` + `PreferencesManager.cameraStateFlow`
- **Marker persistence** - `VisualMarkerRegistry` + incremental updates in `VisualMarkerProvider`
- **Real-time search** - Debounced search with instant filtering
- **Category filtering** - Bottom sheet with category selection

#### 🗺️ **Map Features:**
- **Yandex MapKit v4.8.0** - Full interactive map integration
- **Location Services** - GPS positioning with permission handling
- **Dual-Layer Markers** - Native visual markers with Compose overlay for clicks
- **Dynamic Clustering** - Automatic grouping with ClusteringAlgorithm
- **Circular Image Markers** - Attraction photos with fallback to transparent
- **Geo-objects Support** - Polygons and polylines for parks/trails
- **Map Styles** - Light/Dark theme support with MapStyleProvider

#### 📱 **UI Components:**
- **Material Design 3** - Complete theme system with Typography, Colors, Shapes
- **Responsive Design** - Phone and tablet layouts
- **Top Bar Controls (актуально)** - основные действия на карте (включая Settings)
- **Search Bar** - Real-time search with suggestions
- **Category Carousel** - Horizontal scrolling category filter
- **Photo Gallery** - Swipeable gallery with zoom support
- **Loading Shimmers** - Skeleton loading animations
- **Empty States** - Contextual empty state messages
- **Haptic Feedback** - Touch feedback for interactions

#### 💾 **Data Management:**
- **Room Database** - Local persistence with migrations support
- **DataStore Preferences** - User settings and preferences
- **JSON Assets** - Legacy fallback/seed data (not the primary source)
- **Image Caching** - Coil-based caching (JSON-version invalidation is fallback-only)
- **Data Sync** - Supabase delta sync updates Room via `updated_at`
- **Offline Support** - Full offline functionality
- **Repository Pattern** - Clean separation of data sources

## Development Guidelines

### 📋 **Code Organization**
- Each screen has its own package under `ui/screens/`
- Reusable components in `ui/components/`
- ViewModels follow MVVM pattern with StateFlow
- Use cases encapsulate business logic

### 🔧 **Key Dependencies**
- **Jetpack Compose** - UI toolkit (BOM 2024.12.01)
- **Hilt** - Dependency injection (2.52)
- **Room** - Local database (2.6.1)
- **Yandex MapKit** - Map functionality (4.8.0-full)
- **Coil** - Image loading and caching (2.7.0)
- **Accompanist** - Permissions and utilities
- **Kotlinx Serialization** - JSON parsing (1.7.3)
- **DataStore** - Preferences storage
- **Timber** - Logging

### 🎨 **UI Standards**
- Material Design 3 components
- Consistent spacing using Dimensions.kt
- Dark/Light theme support
- ✅ **Полная локализация на русский язык** - весь интерфейс переведен и адаптирован
- English localization (архитектура готова для будущего расширения)

## Recent Major Updates

### ✅ **Stage 9 - Advanced Map Features:**
- Implemented dual-layer marker system for 100% click reliability
- Added dynamic marker clustering with zoom-based grouping
- UI навигация упрощена: основные действия перенесены в top bar; Settings/About/Privacy/Terms работают как overlay внутри `MapScreenContainer`
- Added CategoryCarousel for quick filtering
- Implemented favorites integration in main map screen

### ✅ **Stage 8 - Navigation & UI Enhancement:**
- Unified multiple MapScreen implementations into single version
- Навигация: `AdygyesNavHost` (Map/Search/Favorites) + overlay-экраны внутри `MapScreenContainer`
- Implemented category filtering with bottom sheet
- Enhanced search with real-time suggestions

### ✅ **Data Architecture Simplification:**
- Removed Developer Mode completely
- Offline-first data flow: Supabase → Room cache → UI
- `assets/attractions.json` retained only as legacy fallback/seed
- Added comprehensive image caching with Coil

## Architecture Pattern: MVVM + Clean Architecture

### Layers:
1. **Presentation Layer** (UI)
   - Compose UI screens
   - ViewModels
   - Navigation
   - Theme

2. **Domain Layer** (Business Logic)
   - Use cases
   - Domain models
   - Repository interfaces

3. **Data Layer** (Data Management)
   - Repository implementations
   - Local data sources (Room)
   - Remote data sources (Retrofit)
   - Data mappers

4. **Core Layer** (Utilities)
   - Extensions
   - Constants
   - Utilities
   - Base classes

## Package Naming Convention
- Base package: `com.adygyes.app`
- Features grouped by layer, then by feature
- Use lowercase with underscores for resource files

## File Naming Conventions
- **Kotlin Files:** PascalCase (e.g., `MapViewModel.kt`)
- **Compose Screens:** PascalCase + "Screen" (e.g., `MapScreen.kt`)
- **Compose Components:** PascalCase (e.g., `AttractionCard.kt`)
- **XML Resources:** lowercase_with_underscores (e.g., `ic_marker.xml`)
- **Test Files:** ClassName + "Test" (e.g., `MapViewModelTest.kt`)

## Gradle Configuration Structure
- Use Gradle Kotlin DSL (`.kts` files)
- Version catalog for dependency management (`gradle/libs.versions.toml`)
- Centralized repositories in `settings.gradle.kts` using `dependencyResolutionManagement` (project build files must not declare repositories)
- Build variants for different app flavors
- Separate debug and release configurations

Example repository configuration in `settings.gradle.kts`:
```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.google.com/") }
        // Yandex repository will be added at Stage 2 when MapKit is integrated
    }
}
```

## Git Structure
- Main branch: `main`
- Development branch: `develop`
- Feature branches: `feature/feature-name`
- Bugfix branches: `bugfix/issue-description`
- Release branches: `release/version-number`

## Build Variants
- **debug:** Development build with debugging enabled
- **release:** Production build with ProGuard/R8
- **full:** Full version with Yandex MapKit Full
- **lite:** Lite version with Yandex MapKit Lite

## Dependency Management Rules
- All versions defined in `libs.versions.toml`
- Group related dependencies
- Use BOM where available (Compose, Firebase)
- Keep dependencies up to date

## Resource Organization
- Strings: Centralized in `strings.xml` (app display name: `<string name="app_name">AdygGIS</string>`)
- Colors: Defined in `colors.xml`, referenced in theme
- Dimensions: Use Material Design spacing
- Drawables: Vector drawables preferred
- App icon: Adaptive icon configured via `@mipmap/ic_launcher` and `@mipmap/ic_launcher_round`.
  - Foreground: `res/drawable/ic_launcher_foreground.xml` (gold compass, VectorDrawable; uses only `<path>` elements for compatibility)
  - Background: `res/drawable/ic_launcher_background.xml` (green gradient)

## Testing Structure
- Unit tests mirror source structure
- Integration tests in `androidTest`
- Use MockK for mocking
- Compose UI tests for screens

## Code Style Guidelines
- Follow Kotlin coding conventions
- Maximum line length: 120 characters
- Use meaningful variable/function names
- Document public APIs
- Use data classes for models
- Prefer immutable data structures

## Important Files Locations
- API Keys: `local.properties` (never commit)
- ProGuard Rules: `app/proguard-rules.pro`
- Version Catalog: `gradle/libs.versions.toml`
- Application Class: `app/src/main/java/com/adygyes/app/AdygyesApplication.kt`
- Navigation Host: `presentation/navigation/AdygyesNavHost.kt`
- Navigation Destinations: `presentation/navigation/NavDestinations.kt`

## 🖼️ Image Caching System

### Architecture Overview:
The app now features a sophisticated image caching system that optimizes performance and reduces network usage:

#### Components:
1. **ImageCacheManager** (`data/local/cache/ImageCacheManager.kt`)
   - Manages Coil ImageLoader with optimized cache settings
   - Memory cache: 25% of available app memory
   - Disk cache: Up to 250MB persistent storage
   - Version-based cache invalidation

2. **ImageCacheViewModel** (`presentation/viewmodel/ImageCacheViewModel.kt`)
   - Provides ImageLoader instance to UI components
   - Manages cache statistics and monitoring
   - Handles preloading of first attraction images

#### Key Features:
- **Smart Preloading**: First image of each attraction preloaded on app start
- **Lazy Loading**: Additional gallery images loaded on-demand
- **Legacy JSON Version Sync**: Cache cleared when attractions.json version changes (fallback/seed only)
- **Hardware Bitmap Fix**: Resolved Canvas compatibility for map markers with `.allowHardware(false)`

#### Integration Points:
- **Map Markers**: VisualMarkerProvider uses cached images for circular markers
- **Photo Gallery**: PhotoGallery component with lazy loading and cache policies
- **Attraction Cards**: All attraction images benefit from caching
- **Repository**: AttractionRepositoryImpl integrates with cache versioning

## Changelog
- 2025-10-05: **Settings Overlay Architecture** 🎨📦 — Complete architectural refactor of Settings navigation:
  - Created `MapScreenContainer.kt` - new wrapper managing Map/Settings/About/Privacy/Terms as overlay layers
  - Settings now works EXACTLY like List mode - slides over Map using AnimatedContent (not Navigation routes)
  - Removed Settings/About/Privacy/Terms from NavHost - now managed internally by container
  - Identical animation syntax as Map/List toggle: `slideInHorizontally { width -> width } + fadeIn()`
  - 300ms default animations (not 250ms tween) - matching Compose defaults exactly
  - Memory efficient - Map stays in background when Settings shown
  - Consistent UX pattern - users understand it immediately (same as Map/List)
  - Created documentation in `Docs/Fixes/SETTINGS_OVERLAY_IMPLEMENTATION.md`
- 2025-10-05: **Navigation Double-Click Protection** 🔒 — Fixed critical navigation bug (discovered this was THE bug initially suspected in Map/List toggle)
  - Implemented `isNavigating` state flag with 500ms protection window
  - Applied to all Settings overlay screens with visual feedback (50% alpha)
  - Created documentation in `Docs/Fixes/DOUBLE_CLICK_NAVIGATION_FIX.md`
- 2025-09-27: **Search Field Animation Enhancement** 🎬 — Implemented cinema-quality search field animations in MapScreen.kt:
  - Replaced `Crossfade` with `AnimatedContent` + `SizeTransform` for smooth expansion
  - Added spring-based animations (`DampingRatioLowBouncy`, `StiffnessVeryLow`) for organic movement
  - Implemented Cubic Bezier easing curves for professional Material Design feel
  - Fixed mode-specific logic: `EnhancedSearchTextField` (List mode) vs `UnifiedSearchTextField` (Map mode)
  - Sequential button animations with staggered delays (200ms/250ms) for elegant appearance
  - Enhanced scale effects (0.7f ↔ 1.0f) and optimized timing (450ms fade-in, 200ms fade-out)
- 2025-09-26: **Favorites Integration** — Integrated favorites functionality into MapScreen with CategoryCarousel, List/Grid toggle, and sorting. Enhanced AttractionsList with compact card mode matching FavoritesScreen design.
- 2025-09-26: **Branding Update** — App display name changed to "AdygGIS" (no internal package rename). Adaptive icon updated (green gradient background + gold compass foreground). `AndroidManifest.xml` `android:label` set to `AdygGIS`; `values/strings.xml` and `values-en/strings.xml` updated accordingly.
- 2025-09-26: **Marker Visuals Update** — Removed colored background and emoji fallback for markers without photos. Default fallback is now fully transparent with a white border and shadow until an image loads. Updated `AppMap_adygyes.md`, `Implementation_Plan.md`, and `IMAGE_CACHING_SYSTEM.md` accordingly.
- 2025-09-25: **MAJOR UPDATE** - Added ImageCacheManager system with version-based invalidation, fixed hardware bitmap issues in map markers, integrated lazy loading in PhotoGallery
- 2025-09-25: Documentation update - Simplified JsonFileManager, removed Developer Mode files (replaced with stubs), added LocaleViewModel for language switching
- 2025-09-24: Stage 9 Complete - Dual-Layer Marker System with DualLayerMarkerSystem, VisualMarkerProvider, and transparent overlay
- 2025-09-22: Centralized repositories in `settings.gradle.kts`; Gradle wrapper updated to 8.13; AGP aligned to 8.7.3.

## Commands Reference
```bash
# Build debug variant
./gradlew assembleDebug

# Build release variant
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Clean build
./gradlew clean

# Generate dependency updates report
./gradlew dependencyUpdates
```

---

## 🎬 Premium Marker Animation System

### Overview
The Premium Marker Animation System provides ultra-smooth marker appearance with preloaded images, delivering cinema-quality UX comparable to top-tier applications.

### Key Components:
- **MapPreloadManager**: Orchestrates background preparation during splash screen
- **VisualMarkerProvider**: Enhanced with 12-frame animation and bitmap caching
- **ImageCacheManager**: In-memory bitmap cache for instant access
- **DualLayerMarkerSystem**: Integrates animation with dual-layer architecture

### Performance Metrics:
- **Animation Duration**: 200ms per marker (12 frames)
- **Stagger Interval**: 50ms between markers
- **Image Load Time**: 0ms (preloaded)
- **Frame Rate**: 60 FPS smooth animation
- **Memory Usage**: ~5-10MB for image cache

### Technical Features:
- **Parallel Preloading**: All images loaded simultaneously during splash
- **Quadratic Fade-in**: Natural appearance animation
- **Hardware Optimization**: Anti-aliasing, filtering, dithering
- **Fallback System**: Reliable marker display in all scenarios
- **Zero-Lag Playback**: Pre-created animation frames

For detailed technical documentation, see: `Docs/Markers/markers_update/MARKER_ANIMATION_SYSTEM.md`

---

## 📝 Changelog

### 2025-09-27: Premium Marker Animation System ✨
- **🎬 Ultra-smooth Animation**: 12-frame marker appearance with quadratic fade-in
- **⚡ Zero-Lag Startup**: Parallel image preloading during splash screen  
- **🖼️ Bitmap Caching**: In-memory cache for instant animation playback
- **📱 Premium UX**: Cinema-quality marker appearance like top-tier apps
- **🔄 Fallback System**: Reliable marker display in all scenarios
- **📊 Performance**: 0ms image load, 50ms stagger, 60 FPS animation
