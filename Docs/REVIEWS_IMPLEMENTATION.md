# Review System Implementation

## Обзор

Система отзывов полностью унифицирована с React Native версией, обеспечивая одинаковый UX на обеих платформах.

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
├── ReviewRepository.kt        - Репозиторий с mock данными
└── Review.kt                  - Модель данных
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
    val authorBadge: String? = null, // "Знаток города 5 уровня"
    val rating: Int, // 1-5
    val text: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant? = null,
    val likes: Int = 0,
    val dislikes: Int = 0,
    val isOwn: Boolean = false
)
```

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

**Методы:**
- `loadReviews(attractionId)` - Загрузить отзывы для места
- `setSortBy(sortBy)` - Изменить сортировку
- `likeReview(reviewId)` - Лайкнуть отзыв
- `dislikeReview(reviewId)` - Дизлайкнуть отзыв
- `clearReviews()` - Очистить кэш

## ReviewRepository

**Расположение:** `data/repository/ReviewRepository.kt`

**Методы:**
- `getReviewsForAttraction(attractionId, sortBy): Flow<List<Review>>`
- `fetchReviews(attractionId): Result<List<Review>>`
- `submitReview(submission): Result<Review>`
- `updateReaction(reviewId, isLike): Result<Unit>`

**⚠️ Mock Data:**
Репозиторий сейчас использует mock данные. При интеграции с Supabase необходимо:
1. Создать таблицу `reviews` в Supabase
2. Добавить endpoint в `SupabaseApiService`
3. Реализовать `SupabaseReviewDataSource`
4. Обновить `ReviewRepository` для работы с Supabase

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

## TODO для Production

1. ✅ Создать компоненты UI
2. ✅ Создать модели данных
3. ✅ Создать ViewModel
4. ✅ Создать Repository с mock данными
5. ✅ Интегрировать в AttractionBottomSheet
6. ⬜ Создать модалку написания отзыва (WriteReviewModal)
7. ⬜ Добавить таблицу `reviews` в Supabase
8. ⬜ Реализовать Supabase integration
9. ⬜ Добавить функцию Share review
10. ⬜ Добавить функцию Edit/Delete own review
11. ⬜ Добавить пагинацию для больших списков
12. ⬜ Добавить pull-to-refresh

## Соответствие RN версии

| Компонент RN | Компонент Kotlin | Статус |
|--------------|------------------|--------|
| ReviewSection.tsx | ReviewSection.kt | ✅ |
| ReviewCard.tsx | ReviewCard.kt | ✅ |
| RatingSummaryBlock.tsx | RatingSummaryBlock.kt | ✅ |
| InteractiveRating.tsx | InteractiveRating.kt | ✅ |
| ReviewSortDropdown.tsx | ReviewSortDropdown.kt | ✅ |
| WriteReviewModal.tsx | - | ⬜ |
| useReviewStore.ts | ReviewViewModel.kt | ✅ |
| Review type | Review.kt | ✅ |

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
