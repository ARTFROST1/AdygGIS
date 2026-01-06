# ✅ Финальный чеклист рефакторинга Kotlin → Supabase

**Дата:** 6 января 2026  
**Версия:** 1.2  
**Статус:** ✅ ПОЛНОСТЬЮ РЕАЛИЗОВАНО

> Все этапы Supabase интеграции завершены. Приложение использует offline-first архитектуру с Supabase как источником правды.

---

## 🎉 SUMMARY: ВСЁ РЕАЛИЗОВАНО

### ✅ Attractions Sync
- Supabase → Room delta sync по `updated_at`
- SyncService, SyncManager, NetworkMonitor
- SupabaseRemoteDataSource + SupabaseApiService

### ✅ Auth System (Stage 12)
- SupabaseAuthApi (GoTrue REST)
- AuthRepository (signIn, signUp, signOut, refreshToken)
- AuthViewModel + AuthModal UI
- AuthPreferencesManager (DataStore session)

### ✅ Reviews System (Stage 12)
- ReviewRepository (submitReview, refreshReviews, hasUserReviewed)
- ReviewViewModel + ReviewSection UI
- ReviewCard, RatingSummaryBlock, WriteReviewModal
- Approved reviews from Supabase, pending модерация

---

## 📋 Содержание

1. [Обзор этапов](#обзор-этапов)
2. [Этап 1: Схема данных](#этап-1-схема-данных)
3. [Этап 2: Retrofit + Supabase](#этап-2-retrofit--supabase)
4. [Этап 3: Sync Service](#этап-3-sync-service)
5. [Этап 4: UI унификация](#этап-4-ui-унификация)
6. [Этап 5: Тестирование](#этап-5-тестирование)
7. [Этап 6: Миграция данных](#этап-6-миграция-данных)
8. [Оценка времени](#оценка-времени)

---

## 🎯 Обзор этапов

```
┌─────────────────────────────────────────────────────────────────────┐
│                    ROADMAP РЕФАКТОРИНГА                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   Этап 1: Схема данных                     [~4 часа]                │
│   ├── Обновить AttractionDto                                        │
│   ├── Обновить AttractionEntity                                     │
│   ├── Обновить Domain Model                                         │
│   ├── Создать Room Migration                                        │
│   └── Обновить Mapper                                               │
│                                                                      │
│   Этап 2: Retrofit + Supabase              [~6 часов]               │
│   ├── Создать SupabaseConfig                                        │
│   ├── Создать SupabaseApiService                                    │
│   ├── Создать NetworkModule                                         │
│   ├── Создать SupabaseRemoteDataSource                              │
│   └── Обновить build.gradle                                         │
│                                                                      │
│   Этап 3: Sync Service                     [~6 часов]               │
│   ├── Создать SyncService                                           │
│   ├── Создать SyncManager                                           │
│   ├── Создать NetworkMonitor                                        │
│   ├── Обновить PreferencesManager                                   │
│   └── Интегрировать в Repository                                    │
│                                                                      │
│   Этап 4: UI унификация                    [~4 часа]                │
│   ├── Обновить CategoryChip                                         │
│   ├── Добавить ExtendedInfoSection                                  │
│   ├── Добавить строки локализации                                   │
│   └── Тестирование UI                                               │
│                                                                      │
│   Этап 5: Тестирование                     [~4 часа]                │
│   ├── Unit tests для SyncService                                    │
│   ├── Integration tests                                             │
│   ├── E2E тестирование                                              │
│   └── Regression testing                                            │
│                                                                      │
│   Этап 6: Миграция данных                  [~2 часа]                │
│   ├── Загрузить данные в Supabase                                   │
│   ├── Верифицировать данные                                         │
│   └── Финальное тестирование                                        │
│                                                                      │
│   ИТОГО:                                   [~26 часов]              │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 📊 Этап 1: Схема данных

### Чеклист файлов

| Файл | Действие | Статус |
|------|----------|--------|
| `data/remote/dto/AttractionDto.kt` | Обновить @SerialName для snake_case | ✅ |
| `data/remote/dto/AttractionDto.kt` | Добавить extended fields | ✅ |
| `data/remote/dto/AttractionDto.kt` | Добавить metadata fields | ✅ |
| `data/remote/dto/AttractionDto.kt` | УДАЛИТЬ isFavorite | ✅ |
| `data/remote/dto/SyncMetadataDto.kt` | СОЗДАТЬ | ✅ |
| `data/local/entities/AttractionEntity.kt` | Добавить extended fields | ✅ |
| `data/local/entities/AttractionEntity.kt` | Добавить lastSyncedAt | ✅ |
| `domain/model/Attraction.kt` | Добавить extended fields | ✅ |
| `data/mapper/AttractionMapper.kt` | Обновить маппинг | ✅ |
| `data/local/database/AdygyesDatabase.kt` | Обновить версию + MIGRATION_1_2 | ✅ |

### Детали изменений

```kotlin
// AttractionDto.kt - ключевые изменения
@SerialName("working_hours") val workingHours: String?    // было workingHours
@SerialName("phone_number") val phoneNumber: String?       // было phoneNumber
@SerialName("price_info") val priceInfo: String?          // было priceInfo
@SerialName("operating_season") val operatingSeason: String?  // 🆕
@SerialName("duration") val duration: String?                  // 🆕
@SerialName("best_time_to_visit") val bestTimeToVisit: String? // 🆕
@SerialName("reviews_count") val reviewsCount: Int?             // 🆕 (для UI отзывов)
@SerialName("average_rating") val averageRating: Float?         // 🆕 (для UI отзывов)
@SerialName("is_published") val isPublished: Boolean           // 🆕
@SerialName("created_at") val createdAt: String?               // 🆕
@SerialName("updated_at") val updatedAt: String?               // 🆕
// УДАЛИТЬ: isFavorite - это локальное состояние
```

---

## 🌐 Этап 2: Retrofit + Supabase

### Чеклист файлов

| Файл | Действие | Статус |
|------|----------|--------|
| `local.properties` | Добавить SUPABASE_URL, SUPABASE_ANON_KEY | ✅ (если заполнено локально) |
| `.gitignore` | Убедиться что local.properties игнорируется | ✅ |
| `app/build.gradle.kts` | Добавить BuildConfig fields | ✅ |
| `gradle/libs.versions.toml` | Проверить Retrofit dependencies | ✅ |
| `data/remote/config/SupabaseConfig.kt` | СОЗДАТЬ | ✅ |
| `data/remote/api/SupabaseApiService.kt` | СОЗДАТЬ | ✅ |
| `data/remote/SupabaseRemoteDataSource.kt` | СОЗДАТЬ | ✅ |
| `di/module/NetworkModule.kt` | СОЗДАТЬ | ✅ |
| `di/module/AppModule.kt` | Добавить RemoteDataSource | ⚠️ (используются NetworkModule/SyncModule) |

### Ключевые моменты

```kotlin
// SupabaseApiService.kt - endpoints
GET /rest/v1/attractions                    // Все attractions
GET /rest/v1/attractions?updated_at=gt.{ts} // Delta sync
GET /rest/v1/sync_metadata                  // Tombstones

// Headers (обязательные)
apikey: {SUPABASE_ANON_KEY}
Authorization: Bearer {SUPABASE_ANON_KEY}
Content-Type: application/json
```

---

## 🔄 Этап 3: Sync Service

### Чеклист файлов

| Файл | Действие | Статус |
|------|----------|--------|
| `data/sync/SyncModels.kt` | SyncResult + SyncState (объединено) | ✅ |
| `data/sync/SyncService.kt` | СОЗДАТЬ | ✅ |
| `data/sync/SyncManager.kt` | СОЗДАТЬ | ✅ |
| `data/sync/NetworkMonitor.kt` | СОЗДАТЬ | ✅ |
| `data/local/preferences/PreferencesManager.kt` | Добавить lastSyncTimestamp | ✅ |
| `data/local/dao/AttractionDao.kt` | Добавить sync методы | ✅ |
| `data/repository/AttractionRepositoryImpl.kt` | Интегрировать SyncManager | ⚠️ (зависит от текущей реализации репозитория) |
| `presentation/viewmodel/MapViewModel.kt` | Добавить syncState | ⚠️ (есть initial sync вызов; UI-state можно доработать) |

### Sync логика

```
1. App Start
   └── Load from Room (instant)
   └── Check network
       ├── Online → performSync()
       └── Offline → use cached data

2. performSync()
   └── Get lastSyncTimestamp
   └── Fetch updated attractions (updated_at > lastSync)
   └── Fetch tombstones (deleted_at > lastSync)
   └── UPSERT updated to Room (preserve isFavorite)
   └── DELETE tombstoned records
   └── Update lastSyncTimestamp

3. Network Reconnect
   └── NetworkMonitor emits isOnline = true
   └── SyncManager triggers performSync()
```

---

## 🎨 Этап 4: UI унификация

### Чеклист файлов

| Файл | Действие | Статус |
|------|----------|--------|
| `presentation/ui/components/CategoryChip.kt` | Добавить size, showEmoji, showLabel | ⬜ |
| `presentation/ui/screens/detail/AttractionDetailScreen.kt` | Добавить ExtendedInfoSection | ⬜ |
| `res/values/strings.xml` | Добавить строки extended fields | ⬜ |
| `res/values-en/strings.xml` | Добавить английские строки | ⬜ |

### Extended Info Section

```kotlin
// В AttractionDetailScreen добавить после основного контента:
ExtendedInfoSection(
    operatingSeason = attraction.operatingSeason,
    duration = attraction.duration,
    bestTimeToVisit = attraction.bestTimeToVisit
)
```

---

## 🧪 Этап 5: Тестирование

### Unit Tests

| Тест | Описание | Статус |
|------|----------|--------|
| `SyncServiceTest.kt` | Тест delta sync логики | ⬜ |
| `SyncServiceTest.kt` | Тест full sync логики | ⬜ |
| `SyncServiceTest.kt` | Тест tombstone handling | ⬜ |
| `AttractionMapperTest.kt` | Тест маппинга DTO → Entity | ⬜ |
| `NetworkMonitorTest.kt` | Тест network detection | ⬜ |

### Integration Tests

| Тест | Описание | Статус |
|------|----------|--------|
| Первый запуск | Полная загрузка из Supabase | ⬜ |
| Delta sync | Только новые записи | ⬜ |
| Offline mode | Приложение работает без сети | ⬜ |
| Reconnect sync | Автоматический sync при подключении | ⬜ |
| Favorites preserve | Favorites сохраняются при sync | ⬜ |
| Tombstones | Удалённые записи удаляются | ⬜ |

### E2E Scenarios

| Сценарий | Шаги | Статус |
|----------|------|--------|
| Новый пользователь | Install → Open → See attractions | ⬜ |
| Offline start | Disable network → Open → See cached data | ⬜ |
| Data update | Admin adds attraction → User syncs → Sees new | ⬜ |
| Data delete | Admin deletes → User syncs → Gone | ⬜ |

---

## 📦 Этап 6: Миграция данных

### Чеклист

| Задача | Описание | Статус |
|--------|----------|--------|
| Создать Supabase проект | supabase.com | ⬜ |
| Выполнить SQL миграции | attractions + sync_metadata tables | ⬜ |
| Настроить RLS policies | Public read, admin write | ⬜ |
| Создать Storage bucket | images bucket | ⬜ |
| Опционально: миграция seed данных | JSON → Supabase (one-off) | ⬜ |
| Верифицировать данные | Проверить все записи | ⬜ |
| Протестировать API | curl/Postman запросы | ⬜ |
| Протестировать приложение | E2E тест | ⬜ |

### Миграционный скрипт

```kotlin
// scripts/MigrateToSupabase.kt
fun main() {
    val json = File("app/src/main/assets/attractions.json").readText()
    val attractions = Json.decodeFromString<AttractionsResponse>(json)
    
    val client = createSupabaseClient(
        supabaseUrl = System.getenv("SUPABASE_URL"),
        supabaseKey = System.getenv("SUPABASE_SERVICE_ROLE_KEY")
    )
    
    attractions.attractions.forEach { attraction ->
        client.from("attractions").insert(attraction.toSupabaseFormat())
    }
    
    println("✅ Migrated ${attractions.attractions.size} attractions")
}
```

---

## ⏱️ Оценка времени

| Этап | Время | Зависимости |
|------|-------|-------------|
| 1. Схема данных | 4 часа | - |
| 2. Retrofit + Supabase | 6 часов | Этап 1 |
| 3. Sync Service | 6 часов | Этап 2 |
| 4. UI унификация | 4 часа | Этап 1 |
| 5. Тестирование | 4 часа | Этапы 1-4 |
| 6. Миграция данных | 2 часа | Supabase проект |
| **ИТОГО** | **~26 часов** | |

### Рекомендуемый порядок

```
День 1: Этапы 1-2 (10 часов)
День 2: Этап 3 (6 часов)
День 3: Этапы 4-5 (8 часов)
День 4: Этап 6 + финальное тестирование (2-4 часа)
```

---

## � Этап 7: Авторизация и Отзывы (Post-MVP)

> **Примечание:** Этот этап добавляется после базовой Supabase интеграции.
> См. полную документацию: [AdygGIS-RN/Docs/Extra/Reviews/08_REVIEWS_AND_AUTH_MODULE.md](../../../AdygGIS-RN/Docs/Extra/Reviews/08_REVIEWS_AND_AUTH_MODULE.md)

### Auth Module

| Файл | Действие | Статус |
|------|----------|--------|
| `data/auth/AuthRepository.kt` | СОЗДАТЬ — Supabase Auth REST API | ⬜ |
| `data/auth/AuthRepositoryImpl.kt` | СОЗДАТЬ — реализация | ⬜ |
| `domain/model/User.kt` | СОЗДАТЬ — модель пользователя | ⬜ |
| `presentation/viewmodel/AuthViewModel.kt` | СОЗДАТЬ | ⬜ |
| `presentation/ui/screens/auth/LoginScreen.kt` | СОЗДАТЬ | ⬜ |
| `presentation/ui/screens/auth/RegisterScreen.kt` | СОЗДАТЬ | ⬜ |
| `presentation/ui/screens/settings/SettingsScreen.kt` | Добавить Auth секцию | ⬜ |

### Reviews Module

| Файл | Действие | Статус |
|------|----------|--------|
| `data/reviews/ReviewDto.kt` | СОЗДАТЬ | ⬜ |
| `data/reviews/ReviewRepository.kt` | СОЗДАТЬ | ⬜ |
| `domain/model/Review.kt` | СОЗДАТЬ | ⬜ |
| `presentation/viewmodel/ReviewViewModel.kt` | СОЗДАТЬ | ⬜ |
| `presentation/ui/components/ReviewCard.kt` | СОЗДАТЬ | ⬜ |
| `presentation/ui/components/ReviewSection.kt` | СОЗДАТЬ | ⬜ |
| `presentation/ui/components/WriteReviewModal.kt` | СОЗДАТЬ | ⬜ |
| `presentation/ui/screens/detail/AttractionDetailScreen.kt` | Интегрировать ReviewSection | ⬜ |

### Auth + Reviews Integration Flow

```
[Пользователь в приложении]
         │
         ├── Settings → "Войти" → LoginScreen → Supabase Auth
         │
         └── AttractionDetail → "Оставить отзыв"
                   │
                   ├── [Не авторизован] → AuthRequiredDialog → Login/Register
                   │
                   └── [Авторизован] → WriteReviewModal → POST review (status: pending)
                              │
                              └── "Отзыв отправлен на модерацию" Toast
```

### Дополнительное время

| Этап | Время |
|------|-------|
| 7.1 Auth Module | ~6 часов |
| 7.2 Reviews Module | ~8 часов |
| **Итого Этап 7** | **~14 часов** |

---

## 🔗 Ссылки на документацию

| Документ | Описание |
|----------|----------|
| [01_ANALYSIS_AND_FEASIBILITY.md](01_ANALYSIS_AND_FEASIBILITY.md) | Анализ целесообразности |
| [02_SCHEMA_ALIGNMENT.md](02_SCHEMA_ALIGNMENT.md) | Выравнивание схемы |
| [03_RETROFIT_SUPABASE.md](03_RETROFIT_SUPABASE.md) | Retrofit интеграция |
| [04_SYNC_SERVICE.md](04_SYNC_SERVICE.md) | Сервис синхронизации |
| [05_UI_UNIFICATION.md](05_UI_UNIFICATION.md) | Унификация UI |
| [../../../AdygGIS-RN/Docs/Extra/Reviews/08_REVIEWS_AND_AUTH_MODULE.md](../../../AdygGIS-RN/Docs/Extra/Reviews/08_REVIEWS_AND_AUTH_MODULE.md) | 🆕 Модуль отзывов и авторизации |
| [../../../ECOSYSTEM.md](../../../ECOSYSTEM.md) | Обзор экосистемы |

---

## 📝 Примечания

### После завершения рефакторинга:

1. **Kotlin версия** → получает данные из Supabase
2. **RN версия** → получает данные из того же Supabase
3. **Admin Panel** → управляет данными в Supabase
4. **Единый источник истины** → Supabase PostgreSQL

### Обратная совместимость:

- JSON в assets сохраняется как fallback
- Если Supabase недоступен → используется локальный JSON
- Room DB сохраняет все данные для offline

