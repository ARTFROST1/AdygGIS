# Техническое описание приложения AdygGIS

## Общая информация
**AdygGIS** - это мобильное приложение-гид по достопримечательностям Республики Адыгея, разработанное на платформе Android с использованием современного стека технологий.

## Основные технологии

### Язык программирования
- **Kotlin** - основной язык разработки Android-приложения
- **Java 17** - версия JDK для компиляции

### Архитектурные паттерны
- **MVVM (Model-View-ViewModel)** - основной архитектурный паттерн
- **Clean Architecture** - разделение на слои (Domain, Data, Presentation)
- **Repository Pattern** - абстракция доступа к данным
- **Use Cases** - бизнес-логика приложения

### UI Framework
- **Jetpack Compose** - современный декларативный UI фреймворк от Google
- **Material Design 3** - система дизайна Google
- **Compose Navigation** - навигация между экранами

### Управление зависимостями
- **Hilt (Dagger)** - Dependency Injection контейнер
- **Kotlin Symbol Processing (KSP)** - генерация кода во время компиляции

### Локальное хранение данных
- **Room Database** - ORM для SQLite базы данных
- **DataStore Preferences** - хранение настроек приложения
- **JSON файлы** - хранение данных достопримечательностей в assets

### Карты и геолокация
- **Yandex MapKit** - картографический SDK (версия 4.8.0-full)
- **Google Play Services Location** - определение местоположения пользователя

### Сетевые запросы и изображения
- **Coil** - загрузка и кэширование изображений
- **Kotlinx Serialization** - сериализация JSON данных

### Асинхронность
- **Kotlin Coroutines** - асинхронное программирование
- **StateFlow/SharedFlow** - реактивные потоки данных

## Структура проекта

### Корневая директория
```
AdyhyesKOTLIN/
├── app/                    # Основной модуль приложения
├── gradle/                 # Конфигурация Gradle
├── Docs/                   # Документация проекта
├── build.gradle.kts        # Конфигурация сборки проекта
├── settings.gradle.kts     # Настройки Gradle
├── gradle.properties       # Свойства Gradle
└── local.properties        # Локальные настройки (API ключи)
```

### Структура модуля app/
```
app/
├── src/main/
│   ├── java/com/adygyes/app/
│   │   ├── data/           # Слой данных
│   │   ├── domain/         # Бизнес-логика
│   │   ├── presentation/   # UI слой
│   │   ├── di/            # Dependency Injection
│   │   └── core/          # Общие утилиты
│   ├── res/               # Ресурсы приложения
│   ├── assets/            # Статические файлы
│   └── AndroidManifest.xml
├── build.gradle.kts       # Конфигурация модуля
└── schemas/               # Схемы базы данных Room
```

## Ключевые файлы и их назначение

### Конфигурационные файлы

#### `build.gradle.kts` (app)
- Конфигурация сборки приложения
- Зависимости библиотек
- Настройки компиляции Kotlin
- Конфигурация подписи APK
- Оптимизация размера приложения (APK splits)

#### `libs.versions.toml`
- Централизованное управление версиями библиотек
- Каталог зависимостей для всего проекта

#### `AndroidManifest.xml`
- Конфигурация приложения для Android системы
- Разрешения (интернет, геолокация, вибрация)
- Настройки Activity и Application класса

### Слой данных (Data Layer)

#### `data/local/database/AdygyesDatabase.kt`
- Конфигурация Room базы данных
- Определение таблиц и DAO

#### `data/local/dao/AttractionDao.kt`
- Data Access Object для работы с достопримечательностями
- SQL запросы для CRUD операций

#### `data/local/entities/AttractionEntity.kt`
- Entity класс для Room базы данных
- Определение структуры таблицы attractions

#### `data/repository/AttractionRepositoryImpl.kt`
- Реализация Repository паттерна
- Координация между локальными и удаленными источниками данных

#### `data/local/JsonFileManager.kt`
- Чтение JSON файлов из assets
- Парсинг данных достопримечательностей

#### `data/local/preferences/PreferencesManager.kt`
- Управление настройками приложения через DataStore
- Сохранение темы, языка, настроек карты

### Доменный слой (Domain Layer)

#### `domain/model/Attraction.kt`
- Основная доменная модель достопримечательности
- Enum категорий с эмодзи и цветами
- Вспомогательные data классы (Location, ContactInfo)

#### `domain/repository/AttractionRepository.kt`
- Интерфейс репозитория (абстракция)
- Определение контракта для работы с данными

#### `domain/usecase/`
- Use Cases для различных бизнес-операций:
  - `GetLocationUseCase.kt` - получение геолокации
  - `AttractionDisplayUseCase.kt` - отображение достопримечательностей
  - `NavigationUseCase.kt` - навигация к местам

### Презентационный слой (Presentation Layer)

#### `presentation/ui/screens/`
Основные экраны приложения:
- `map/MapScreen.kt` - экран карты с маркерами
- `search/SearchScreen.kt` - поиск достопримечательностей
- `favorites/FavoritesScreen.kt` - избранные места
- `settings/SettingsScreen.kt` - настройки приложения
- `detail/AttractionDetailScreen.kt` - детальная информация

#### `presentation/ui/components/`
Переиспользуемые UI компоненты:
- `AttractionCard.kt` - карточка достопримечательности
- `CategoryCarousel.kt` - карусель категорий
- `SearchBar.kt` - поле поиска
- `PhotoGallery.kt` - галерея фотографий

#### `presentation/viewmodel/`
ViewModel классы для управления состоянием:
- `MapViewModel.kt` - состояние карты и маркеров
- `SearchViewModel.kt` - логика поиска и фильтрации
- `FavoritesViewModel.kt` - управление избранным
- `SettingsViewModel.kt` - настройки приложения

### Dependency Injection

#### `di/module/AppModule.kt`
- Конфигурация Hilt модулей
- Предоставление зависимостей (Repository, UseCase, etc.)

#### `di/module/DatabaseModule.kt`
- Конфигурация Room базы данных
- Предоставление DAO объектов

### Ресурсы приложения

#### `res/values/strings.xml`
- Строковые ресурсы на русском языке
- Локализация интерфейса

#### `res/values-en/strings.xml`
- Английская локализация

#### `res/drawable/`
- Векторные иконки и изображения
- XML drawable ресурсы

#### `res/mipmap-*/`
- Иконки приложения для разных плотностей экрана

#### `assets/attractions.json`
- JSON файл с данными достопримечательностей (legacy)
- Используется только как fallback/seed, если Supabase sync недоступен

## Расширения файлов и их назначение

### `.kt` (Kotlin)
- Основные файлы исходного кода
- Классы, интерфейсы, функции

### `.kts` (Kotlin Script)
- Скрипты конфигурации Gradle
- `build.gradle.kts`, `settings.gradle.kts`

### `.xml` (XML)
- Конфигурационные файлы Android
- Ресурсы (strings, colors, themes)
- Манифест приложения

### `.toml` (TOML)
- Файл каталога версий Gradle
- `libs.versions.toml`

### `.json` (JSON)
- Файлы данных
- `attractions.json` - legacy fallback/seed данные (основной источник данных: Room cache + Supabase sync)

### `.properties`
- Файлы свойств
- `gradle.properties` - настройки Gradle
- `local.properties` - локальные настройки

### `.pro` (ProGuard)
- Правила обфускации кода
- `proguard-rules.pro`

## Ключевые особенности архитектуры

### Реактивное программирование
- Использование StateFlow для управления состоянием UI
- Корутины для асинхронных операций
- Compose для декларативного UI

### Модульность
- Четкое разделение на слои (Data, Domain, Presentation)
- Dependency Injection для слабой связанности
- Repository паттерн для абстракции данных

### Производительность
- Lazy loading изображений с кэшированием
- Оптимизация размера APK через splits
- Предзагрузка данных карты

### Локализация
- Поддержка русского и английского языков
- Адаптация под региональные особенности

### Безопасность
- Обфускация кода в release сборке
- Безопасное хранение API ключей
- Подпись APK для публикации

## Сборка и развертывание

### Debug сборка
```bash
./gradlew assembleDebug
```

### Release сборка
```bash
./gradlew assembleRelease
```

### Создание Android App Bundle
```bash
./gradlew bundleRelease
```

## Зависимости и библиотеки

### Основные зависимости
- **Compose BOM 2024.12.01** - управление версиями Compose
- **Hilt 2.51.1** - Dependency Injection
- **Room 2.6.1** - локальная база данных
- **Yandex MapKit 4.8.0** - картографический SDK
- **Coil 2.7.0** - загрузка изображений
- **Coroutines 1.9.0** - асинхронность

### Инструменты разработки
- **Timber** - логирование
- **LeakCanary** - обнаружение утечек памяти (debug)
- **KSP** - генерация кода

Это приложение демонстрирует современные подходы к разработке Android приложений с использованием актуальных технологий и архитектурных паттернов.
