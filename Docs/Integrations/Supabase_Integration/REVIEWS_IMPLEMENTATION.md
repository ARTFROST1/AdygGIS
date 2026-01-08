# Review System Implementation (Kotlin ↔ Supabase)

## Обзор

Система отзывов в Kotlin реализована как **offline-first**:

- UI показывает отзывы **мгновенно из Room** (если кэш есть)
- Основная синхронизация подтягивает **все approved отзывы bulk-ом** (чтобы карточки открывались без ожидания сети)
- При переоткрытии карточки запускается **delta-sync только изменений** (если кэш устарел)
- Реакции (лайк/дизлайк) работают через **optimistic update**: UI обновляется сразу, потом данные уходят на сервер

Фактический статус Kotlin (по коду):
- ✅ Bulk sync всех approved отзывов во время `SyncService.performSync()`
- ✅ Delta sync отзывов на карточке по порогу устаревания кэша
- ✅ Мгновенная отрисовка из Room (cache-only методы, см. ниже)
- ✅ Реакции: моментальный отклик + запись в Room + фоновой запрос на Supabase
- ✅ Реакции не теряются при синхронизации (локальные поля сохраняются при upsert)

> Основная документация по всей связке Auth + Reviews: см. [AUTH_AND_REVIEWS_IMPLEMENTATION.md](./AUTH_AND_REVIEWS_IMPLEMENTATION.md).

## Архитектура

```
UI Layer (Compose)
├── ReviewSection.kt           - Основной контейнер секции отзывов
├── RatingSummaryBlock.kt      - Блок общего рейтинга + CTA
├── InteractiveRating.kt       - Интерактивные звёзды для оценки
├── ReviewCard.kt              - Карточка отдельного отзыва
└── ReviewSortDropdown.kt      - Выпадающий список сортировки

ViewModel Layer
└── ReviewViewModel.kt         - Управление состоянием отзывов

Data Layer
├── ReviewRepository.kt        - offline-first для отзывов + реакций
├── ReviewSyncService.kt       - bulk/delta синхронизация отзывов
├── ReviewDao.kt               - Room DAO (кэш + локальные поля)
└── Review.kt                  - доменная модель (унифицирована с RN)

Remote Layer
├── SupabaseApiService.kt      - REST endpoints для reviews + review_reactions
└── ReviewsRemoteDataSource.kt - retry + удобные методы для bulk/delta
```

## Компоненты

### 1. ReviewSection (Main Component)

**Расположение:** `presentation/ui/components/reviews/ReviewSection.kt`

**Назначение:** Полная секция отзывов с рейтингом и списком

**Параметры:**
- `attractionId` - ID места
- `attractionName` - Название (для модалки написания отзыва)
- `averageRating` - Средний рейтинг
- `totalReviews` - Количество отзывов
- `reviews` - Список отзывов
- `sortBy` - Текущая сортировка
- `onSortChange` - Callback изменения сортировки
- `onWriteReview` - Callback написания отзыва
- `onLike/onDislike/onShare` - Callbacks для действий с отзывом
- `loading` - Флаг загрузки

**Структура:**
1. Заголовок секции ("Отзывы")
2. RatingSummaryBlock - сводка рейтинга
3. Заголовок списка с количеством + сортировка
4. Список ReviewCard или Empty State

### 2. RatingSummaryBlock

**Расположение:** `presentation/ui/components/reviews/RatingSummaryBlock.kt`

**Назначение:** Сводная информация о рейтинге с призывом оставить отзыв

**Компоненты:**
- Большая цифра рейтинга (48sp)
- Звёзды рейтинга
- Количество оценок
- Divider
- CTA текст + интерактивные звёзды

### 3. InteractiveRating

**Расположение:** `presentation/ui/components/reviews/RatingSummaryBlock.kt`

**Назначение:** Интерактивные звёзды для оценки (1-5)

**Параметры:**
- `value` - Текущий рейтинг (0-5)
- `onRatingChange` - Callback при изменении
- `size` - Размер звёзд (Dp)
- `spacing` - Отступ между звёздами
- `enabled` - Активность
- `color` - Цвет заполненных звёзд
- `emptyColor` - Цвет пустых звёзд

### 4. ReviewCard

**Расположение:** `presentation/ui/components/reviews/ReviewCard.kt`

**Назначение:** Карточка отдельного отзыва

**Структура:**
1. **Header:**
   - Avatar (круг с иконкой person)
   - Имя автора + бейдж
   - Звёзды рейтинга (справа)
   - Дата (относительная)

2. **Content:**
   - Текст отзыва (если есть)

3. **Actions:**
   - Like (👍) + счётчик
   - Dislike (👎) + счётчик
   - Share (🔗)

### 5. ReviewSortDropdown

**Расположение:** `presentation/ui/components/reviews/ReviewSection.kt`

**Назначение:** Выпадающее меню сортировки отзывов

**Опции:**
- `DEFAULT` - "По популярности" (likes - dislikes, затем по дате)
- `NEWEST` - "Сначала новые"
- `OLDEST` - "Сначала старые"
- `HIGHEST` - "Высокая оценка"
- `LOWEST` - "Низкая оценка"

## Модели данных

### Review

```kotlin
data class Review(
    val id: String,
    val attractionId: String,
    val authorId: String,
    val authorName: String,
    val authorAvatar: String? = null,
    val authorBadge: String? = null,
    val rating: Int,
    val text: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant? = null,
    val likesCount: Int = 0,
    val dislikesCount: Int = 0,
    val userReaction: ReviewReaction = ReviewReaction.NONE,
    val isOwn: Boolean = false,
    val status: String? = null,
    val rejectionReason: String? = null
)
```

### ReviewReaction

```kotlin
enum class ReviewReaction {
    LIKE,
    DISLIKE,
    NONE
}
```

### ReviewEntity (Room)

Room хранит не только серверные поля, но и локальные:

- `userReaction` — реакция текущего пользователя (кэш, чтобы UI не делал лишних запросов)
- `isOwnReview` — флаг «это мой отзыв» (важно не потерять при bulk/delta sync)
- `lastSyncedAt` — время последней синхронизации для логики staleness

### ReviewSortOption

```kotlin
enum class ReviewSortOption {
    DEFAULT,   // По популярности
    NEWEST,    // Сначала новые
    OLDEST,    // Сначала старые
    HIGHEST,   // Высокая оценка
    LOWEST     // Низкая оценка
}
```

## ReviewViewModel

**Расположение:** `presentation/viewmodel/ReviewViewModel.kt`

**Состояние:**
- `reviews: StateFlow<List<Review>>` - Список отзывов
- `loading: StateFlow<Boolean>` - Флаг загрузки
- `error: StateFlow<String?>` - Ошибка
- `sortBy: StateFlow<ReviewSortOption>` - Текущая сортировка

**Ключевые методы:**
- `loadReviews(attractionId)` — сначала грузит Room кэш мгновенно, затем запускает background sync
- `reactToReview(reviewId, isLike)` — **optimistic update**: UI обновляется мгновенно, сервер подтверждает в фоне

## ReviewRepository

**Расположение:** `data/repository/ReviewRepository.kt`

**Основные методы (актуально):**
- `getReviewsFromCacheOnly(attractionId)` — мгновенно из Room, без сети
- `performBackgroundSync(attractionId)` — синхронизация в фоне (delta/bulk по необходимости)
- `submitReview(submission)` — создание review (server sets status=pending)
- `reactToReviewOptimistic(...)` — запись в Room + отправка на сервер

> `getReviewsOfflineFirst()` и `backgroundSyncReviews()` остаются для совместимости, но помечены как deprecated.

Важно:
- Репозиторий НЕ должен делать «полный рефреш всех отзывов» после реакции — реакция фиксируется локально и синхронизируется отдельно.

## Интеграция в AttractionBottomSheet

**Расположение:** `presentation/ui/components/AttractionBottomSheet.kt`

**Код:**
```kotlin
// Load reviews when attraction changes
LaunchedEffect(attraction.id) {
    reviewViewModel.loadReviews(attraction.id)
}

val reviews by reviewViewModel.reviews.collectAsState()
val sortBy by reviewViewModel.sortBy.collectAsState()
val loading by reviewViewModel.loading.collectAsState()

// ... в конце BottomSheet перед кнопкой "Подробнее"

// Reviews Section
ReviewSection(
    attractionId = attraction.id,
    attractionName = attraction.name,
    averageRating = attraction.averageRating ?: attraction.rating ?: 0f,
    totalReviews = attraction.reviewsCount ?: 0,
    reviews = reviews,
    sortBy = sortBy,
    onSortChange = { reviewViewModel.setSortBy(it) },
    onWriteReview = { /* TODO */ },
    onLike = { reviewId -> reviewViewModel.likeReview(reviewId) },
    onDislike = { reviewId -> reviewViewModel.dislikeReview(reviewId) },
    onShare = { reviewId -> /* TODO */ },
    loading = loading
)
```

## Форматирование даты

Функция `formatDate()` в ReviewCard конвертирует Instant в человекочитаемый формат:

- "сегодня" - если сегодня
- "вчера" - если вчера
- "X дн. назад" - если < 7 дней
- "X нед. назад" - если < 30 дней
- "X мес. назад" - если < 365 дней
- "DD месяц YYYY" - если > года

## Синхронизация отзывов (bulk/delta)

### Bulk sync (во время общего sync)

Источник: `SyncService.performSync()` вызывает `ReviewSyncService.performBulkSync()`.

Логика:
- Если кэш пустой → `getAllApprovedReviews()` и `replaceAllApprovedReviews()`
- Если кэш уже есть → глобальный delta sync по `MAX(updatedAt)`

### Delta sync на карточке

Источник: `ReviewRepository.backgroundSyncReviews()` → `ReviewSyncService.syncReviewsForAttraction()`.

Правила:
- Если `now - lastSyncedAt < 5 минут` → сеть пропускаем
- Иначе:
    - если есть cached `MAX(updatedAt)` → тянем `getUpdatedReviewsForAttraction()`
    - если кэша нет → `getApprovedReviewsForAttraction()`

### Сохранение локальных полей

При upsert отзывов из Supabase сохраняются локальные поля (`isOwnReview`, `userReaction`, `rejectionReason`) по `id`.

Это важно, чтобы:
- реакции не сбрасывались после sync
- “мой отзыв” не превращался в “чужой” при появлении публичной версии

## Реакции (лайк/дизлайк)

### Цель

Сделать реакции мгновенными:

1) UI меняется сразу
2) Room кэш обновляется сразу
3) Сервер получает запрос в фоне
4) Если сервер вернул ошибку — откат

### Хранение в Room

`ReviewEntity.userReaction` хранит `"like" | "dislike" | null`.

### Синхронизация с сервером

Используются endpoints `review_reactions`:

- `POST /rest/v1/review_reactions` (Prefer: resolution=merge-duplicates)
- `DELETE /rest/v1/review_reactions?review_id=eq...&user_id=eq...`
- `GET /rest/v1/review_reactions?...` (batch) — используется для сверки, но без N×Room запросов

## Соответствие RN версии

| Компонент RN | Компонент Kotlin | Статус |
|--------------|------------------|--------|
| ReviewSection.tsx | ReviewSection.kt | ✅ |
| ReviewCard.tsx | ReviewCard.kt | ✅ |
| RatingSummaryBlock.tsx | RatingSummaryBlock.kt | ✅ |
| InteractiveRating.tsx | InteractiveRating.kt | ✅ |
| ReviewSortDropdown.tsx | ReviewSortDropdown.kt | ✅ |
| WriteReviewModal.tsx | WriteReviewModal.kt | ✅ |
| useReviewStore.ts | ReviewViewModel.kt | ✅ |
| Review type | Review.kt | ✅ |

## Где смотреть код

- `data/sync/ReviewSyncService.kt` — bulk/delta логика
- `data/sync/SyncService.kt` — общий sync + вызов bulk sync отзывов
- `data/repository/ReviewRepository.kt` — offline-first + реакции
- `presentation/viewmodel/ReviewViewModel.kt` — моментальный UI + rollback
- `data/local/dao/ReviewDao.kt` — методы кэша и обновления реакций

## Примеры использования

### Standalone использование

```kotlin
@Composable
fun MyScreen() {
    val reviewViewModel: ReviewViewModel = viewModel()
    
    LaunchedEffect(attractionId) {
        reviewViewModel.loadReviews(attractionId)
    }
    
    val reviews by reviewViewModel.reviews.collectAsState()
    
    ReviewSection(
        attractionId = attractionId,
        attractionName = "Хаджохская теснина",
        averageRating = 4.8f,
        totalReviews = 156,
        reviews = reviews,
        sortBy = ReviewSortOption.DEFAULT,
        onSortChange = { reviewViewModel.setSortBy(it) },
        onWriteReview = { /* ... */ },
        onLike = { reviewViewModel.likeReview(it) },
        onDislike = { reviewViewModel.dislikeReview(it) },
        onShare = { /* ... */ }
    )
}
```
