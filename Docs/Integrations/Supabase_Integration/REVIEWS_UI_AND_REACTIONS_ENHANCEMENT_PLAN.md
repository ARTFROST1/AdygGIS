# 📝 Планирование (история): Улучшения UI отзывов и система реакций

**Дата:** 6 января 2026  
**Статус:** ✅ Реализовано (документ оставлен как история планирования)  
**Цель (исторически):** Улучшить отображение отзывов, усилить систему авторизации и добавить функционал лайков/дизлайков

## ✅ Актуальный статус (кратко)

На текущий момент функциональность из этого плана реализована в приложении Kotlin:
- Реакции лайк/дизлайк сохраняются в Supabase (`review_reactions`) и отражаются в `reviews.likes_count`/`reviews.dislikes_count`.
- В UI есть мгновенный отклик (optimistic update) + фоновая синхронизация.
- При отсутствии авторизации попытка поставить реакцию вызывает запрос на логин/регистрацию (AuthModal).
- Отзывы работают в offline-first режиме: мгновенный показ из Room + фоновый sync (ненавязчивый `isSyncing`).

См. актуальные документы:
- MasterDocs/technical/AUTH_REVIEWS.md
- MasterDocs/technical/DATA_SYNC.md
- AdygGIS-KT/Docs/fixes/REVIEWS_OFFLINE_FIRST_CACHING.md
- AdygGIS-KT/Docs/fixes/REVIEW_REACTIONS_FIX.md

---

## 🎯 Обзор требований

### 1. **Отображение собственного отзыва**
- ✅ **Текущее состояние:** Свой отзыв и отзывы других пользователей показываются в одном списке
- 🎯 **Требуемое состояние:** 
  - Свой отзыв отображается **ВЫШЕ** всех остальных в отдельной секции "Ваш отзыв"
  - Бейдж статуса: `На модерации` | `Опубликован` | `Отклонён`
  - Отзывы других пользователей ниже в списке
  - **НЕТ дубликатов** - если отзыв approved, он не должен дублироваться в обоих секциях

### 2. **Усиление системы авторизации**
- ✅ **Текущее состояние:** Проверка `isAuthenticated` при попытке написать отзыв
- 🎯 **Требуемое состояние:**
  - Если пользователь уже залогинен в `SettingsScreen`, то при нажатии "Написать отзыв" **НЕ показывать** AuthModal
  - Связать `AuthState` между `SettingsScreen` и `ReviewViewModel`
  - Централизованный `AuthRepository.authState` flow - единственный источник истины

### 3. **Система лайков и дизлайков**
- ✅ **Текущее состояние:** UI кнопки есть, но реакции хранятся только локально в памяти (in-memory)
- 🎯 **Требуемое состояние:**
  - Новая таблица `review_reactions` в Supabase
  - RLS политики: один юзер = одна реакция на отзыв
  - Toggle механизм: like → none, dislike → none, like → dislike (переключение)
  - Обновление счётчиков `likes_count` и `dislikes_count` через триггер

### 4. **Выравнивание звёзд в WriteReviewModal**
- ✅ **Текущее состояние:** `InteractiveRatingLarge` с size=48, spacing=12
- 🎯 **Требуемое состояние:**
  - Проверить визуальное выравнивание всех 5 звёзд
  - Убедиться что spacing одинаковый, центрирование корректное
  - Возможно нужен `horizontalAlignment = Alignment.CenterHorizontally`

---

## 📊 Текущее состояние системы

### Архитектура отзывов (Kotlin)

```
ReviewRepository
├── _reviews: MutableStateFlow<Map<String, List<Review>>>  // Approved public reviews
├── _userOwnReviews: MutableStateFlow<List<Review>>        // User's own reviews (all statuses)
└── refreshApprovedReviews() -> Fetches from Supabase

ReviewViewModel
├── reviews: StateFlow<List<Review>>                        // Currently displayed reviews
├── hasUserReviewed: StateFlow<Boolean>
└── isAuthenticated: StateFlow<Boolean> (from AuthRepository)

UI Components
├── ReviewSection.kt           // Renders review list
├── ReviewCard.kt              // Individual review card (with like/dislike buttons)
├── WriteReviewModal.kt        // Modal for writing review (with star rating)
└── AuthModal.kt               // Login/Register modal
```

### Схема БД (текущая)

```sql
-- reviews table
CREATE TABLE reviews (
  id UUID PRIMARY KEY,
  attraction_id UUID NOT NULL REFERENCES attractions(id),
  user_id UUID NOT NULL REFERENCES auth.users(id),
  rating SMALLINT NOT NULL CHECK (rating >= 1 AND rating <= 5),
  title TEXT,
  body TEXT NOT NULL,
  status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'rejected')),
  moderated_at TIMESTAMPTZ,
  moderated_by UUID REFERENCES auth.users(id),
  rejection_reason TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Unique constraint: one review per user per attraction
CREATE UNIQUE INDEX ux_reviews_attraction_user ON reviews(attraction_id, user_id);
```

**Примечание:** этот блок был верен на момент планирования. Сейчас поля `likes_count` и `dislikes_count` присутствуют в `reviews`, а реакции вынесены в `review_reactions`.

---

## 🏗️ План реализации

### ФАЗА 1: Миграция БД - Система реакций

#### 1.1 Создать таблицу `review_reactions`

```sql
-- Table for tracking user reactions (likes/dislikes) on reviews
CREATE TABLE IF NOT EXISTS review_reactions (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  review_id UUID NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  reaction TEXT NOT NULL CHECK (reaction IN ('like', 'dislike')),
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Unique constraint: one reaction per user per review
CREATE UNIQUE INDEX ux_review_reactions_user_review 
  ON review_reactions(review_id, user_id);

-- Indexes for performance
CREATE INDEX idx_review_reactions_review_id ON review_reactions(review_id);
CREATE INDEX idx_review_reactions_user_id ON review_reactions(user_id);
```

#### 1.2 Добавить поля счётчиков в `reviews`

```sql
-- Add aggregated counters to reviews table
ALTER TABLE reviews 
  ADD COLUMN likes_count INTEGER DEFAULT 0 NOT NULL,
  ADD COLUMN dislikes_count INTEGER DEFAULT 0 NOT NULL;

-- Create index for sorting by popularity
CREATE INDEX idx_reviews_popularity ON reviews((likes_count - dislikes_count) DESC);
```

#### 1.3 Создать триггер для автоматического подсчёта

```sql
-- Function to update review reaction counts
CREATE OR REPLACE FUNCTION update_review_reaction_counts()
RETURNS TRIGGER AS $$
BEGIN
  -- Recalculate likes and dislikes for the affected review
  UPDATE reviews
  SET
    likes_count = (
      SELECT COUNT(*) FROM review_reactions
      WHERE review_id = COALESCE(NEW.review_id, OLD.review_id)
        AND reaction = 'like'
    ),
    dislikes_count = (
      SELECT COUNT(*) FROM review_reactions
      WHERE review_id = COALESCE(NEW.review_id, OLD.review_id)
        AND reaction = 'dislike'
    ),
    updated_at = NOW()
  WHERE id = COALESCE(NEW.review_id, OLD.review_id);

  RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Trigger on INSERT/UPDATE/DELETE in review_reactions
CREATE TRIGGER on_review_reaction_change
  AFTER INSERT OR UPDATE OR DELETE ON review_reactions
  FOR EACH ROW
  EXECUTE FUNCTION update_review_reaction_counts();
```

#### 1.4 RLS политики для `review_reactions`

```sql
-- Enable RLS
ALTER TABLE review_reactions ENABLE ROW LEVEL SECURITY;

-- Anyone can view reaction counts (через reviews.likes_count)
-- Direct reads to review_reactions - только для подсчётов

-- Authenticated users can view all reactions (for displaying counts)
CREATE POLICY "Anyone can view reactions" ON review_reactions
  FOR SELECT USING (true);

-- Users can insert/update/delete only their own reactions
CREATE POLICY "Users can manage own reactions" ON review_reactions
  FOR ALL USING (auth.uid() = user_id)
  WITH CHECK (auth.uid() = user_id);
```

---

### ФАЗА 2: Backend API - Supabase REST endpoints

#### 2.1 Эндпоинты для реакций

**Добавить в `SupabaseApiService.kt`:**

```kotlin
// Create or update user's reaction to a review
@POST("rest/v1/review_reactions")
suspend fun upsertReviewReaction(
    @Header("Authorization") authorization: String,
    @Header("Prefer") prefer: String = "resolution=merge-duplicates",
    @Body request: ReviewReactionRequest
): Response<ReviewReactionDto>

// Delete user's reaction from a review
@DELETE("rest/v1/review_reactions")
suspend fun deleteReviewReaction(
    @Header("Authorization") authorization: String,
    @Query("review_id") reviewId: String, // e.g., "eq.review-uuid"
    @Query("user_id") userId: String      // e.g., "eq.user-uuid"
): Response<Unit>

// Get user's reaction for a specific review (optional - для кэширования)
@GET("rest/v1/review_reactions")
suspend fun getUserReaction(
    @Header("Authorization") authorization: String,
    @Query("review_id") reviewId: String,
    @Query("user_id") userId: String,
    @Query("select") select: String = "reaction"
): Response<List<ReviewReactionDto>>
```

#### 2.2 DTOs для реакций

**Создать `ReviewReactionDto.kt`:**

```kotlin
package com.adygyes.app.data.remote.dto

import com.squareup.moshi.Json

/**
 * Request body for upserting a review reaction
 */
data class ReviewReactionRequest(
    @Json(name = "review_id")
    val reviewId: String,
    @Json(name = "user_id")
    val userId: String,
    val reaction: String // "like" or "dislike"
)

/**
 * Response from Supabase review_reactions table
 */
data class ReviewReactionDto(
    val id: String,
    @Json(name = "review_id")
    val reviewId: String,
    @Json(name = "user_id")
    val userId: String,
    val reaction: String,
    @Json(name = "created_at")
    val createdAt: String,
    @Json(name = "updated_at")
    val updatedAt: String
)
```

---

### ФАЗА 3: Data Layer - Repository & Domain Model

#### 3.1 Обновить `Review` модель

**`app/domain/model/Review.kt`:**

```kotlin
data class Review(
    val id: String,
    val attractionId: String,
    val authorId: String,
    val authorName: String,
    val authorAvatar: String? = null,
    val authorBadge: String? = null,
    val rating: Int, // 1-5
    val text: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant? = null,
    
    // ❗ ИЗМЕНИТЬ: использовать счётчики из БД
    val likesCount: Int = 0,      // ← renamed from 'likes'
    val dislikesCount: Int = 0,   // ← renamed from 'dislikes'
    
    // ✅ ДОБАВИТЬ: состояние реакции текущего юзера
    val userReaction: ReviewReaction = ReviewReaction.NONE,
    
    val isOwn: Boolean = false,
    val status: String? = null, // 'pending', 'approved', 'rejected'
    val rejectionReason: String? = null
)

enum class ReviewReaction {
    LIKE,
    DISLIKE,
    NONE
}
```

#### 3.2 Расширить `ReviewRepository`

**Новые методы:**

```kotlin
class ReviewRepository @Inject constructor(
    private val reviewsRemoteDataSource: ReviewsRemoteDataSource,
    private val authRepository: AuthRepository
) {
    // ✅ СУЩЕСТВУЮЩИЕ методы остаются без изменений
    
    // ✅ НОВЫЕ методы
    
    /**
     * Submit like/dislike reaction to a review.
     * Toggle logic: like → none, dislike → none, like ↔ dislike
     */
    suspend fun reactToReview(
        reviewId: String,
        isLike: Boolean
    ): NetworkResult<Unit> {
        val authState = authRepository.authState.value
        if (!authState.isAuthenticated || authState.user == null) {
            return NetworkResult.Error(401, "Требуется авторизация")
        }
        
        // TODO: Implement with reviewsRemoteDataSource.upsertReaction()
        // Logic:
        // 1. Get current user reaction from _userReactions cache or fetch from API
        // 2. If same reaction -> DELETE (toggle off)
        // 3. If different reaction -> UPSERT (toggle to new)
        // 4. If no reaction -> UPSERT (add new)
        // 5. Update local cache _userReactions and refresh review counts
    }
    
    /**
     * Refresh user's own reviews for an attraction.
     * Fetches all statuses (pending, approved, rejected).
     */
    suspend fun refreshUserOwnReviews(attractionId: String) {
        // Fetch user's reviews with all statuses
        // Store in _userOwnReviews flow
    }
    
    // Cache for user's reactions to reviews
    private val _userReactions = MutableStateFlow<Map<String, ReviewReaction>>(emptyMap())
    val userReactions: StateFlow<Map<String, ReviewReaction>> = _userReactions.asStateFlow()
}
```

---

### ФАЗА 4: UI Layer - Отображение собственного отзыва

#### 4.1 Обновить `ReviewSection.kt`

**Текущая логика:**
```kotlin
// reviews - все отзывы включая свои
reviews.forEach { review ->
    ReviewCard(review = review, ...)
}
```

**Новая логика (по примеру RN):**

```kotlin
@Composable
fun ReviewSection(
    attractionId: String,
    attractionName: String,
    averageRating: Float,
    totalReviews: Int,
    reviews: List<Review>,           // Approved public reviews
    userOwnReviews: List<Review>,    // ← НОВЫЙ параметр
    sortBy: ReviewSortOption,
    onSortChange: (ReviewSortOption) -> Unit,
    onWriteReview: () -> Unit,
    onLike: (String) -> Unit,
    onDislike: (String) -> Unit,
    onShare: (String) -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Section Title
        Text("Отзывы", ...)
        
        // Rating Summary
        RatingSummaryBlock(...)
        
        // ✅ НОВАЯ СЕКЦИЯ: User's own reviews (if exists)
        if (userOwnReviews.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Ваш отзыв",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            userOwnReviews.forEach { review ->
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    // Status badge
                    StatusBadge(status = review.status ?: "pending")
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Review card (без like/dislike для своего отзыва)
                    ReviewCard(
                        review = review,
                        onLike = null,      // Нельзя лайкать свой отзыв
                        onDislike = null,
                        onShare = onShare
                    )
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        }
        
        // ✅ Reviews List Header
        if (reviews.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (userOwnReviews.isNotEmpty()) 
                        "Отзывы других пользователей" 
                    else 
                        "${reviews.size} отзывов"
                )
                
                ReviewSortDropdown(sortBy = sortBy, onSortChange = onSortChange)
            }
            
            HorizontalDivider()
        }
        
        // ✅ Other users' reviews
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            reviews.forEach { review ->
                ReviewCard(
                    review = review,
                    onLike = onLike,
                    onDislike = onDislike,
                    onShare = onShare
                )
            }
        }
    }
}

/**
 * Status Badge Component
 * Бейдж статуса отзыва (На модерации, Опубликован, Отклонён)
 */
@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val (backgroundColor, textColor, label) = when (status) {
        "pending" -> Triple(
            Color(0xFFFFF4E5),
            Color(0xFFFF9800),
            "На модерации"
        )
        "approved" -> Triple(
            Color(0xFFE8F5E9),
            Color(0xFF4CAF50),
            "Опубликован"
        )
        "rejected" -> Triple(
            Color(0xFFFFEBEE),
            Color(0xFFF44336),
            "Отклонён"
        )
        else -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Неизвестно"
        )
    }
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}
```

#### 4.2 Обновить `ReviewViewModel`

**Добавить поддержку `userOwnReviews`:**

```kotlin
class ReviewViewModel @Inject constructor(
    private val reviewRepository: ReviewRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    // ✅ СУЩЕСТВУЮЩИЕ
    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews: StateFlow<List<Review>> = _reviews.asStateFlow()
    
    // ✅ НОВЫЙ: User's own reviews
    private val _userOwnReviews = MutableStateFlow<List<Review>>(emptyList())
    val userOwnReviews: StateFlow<List<Review>> = _userOwnReviews.asStateFlow()
    
    val isAuthenticated: StateFlow<Boolean> = authRepository.authState
        .map { it.isAuthenticated }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    fun loadReviews(attractionId: String, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            try {
                // 1. Refresh approved public reviews
                reviewRepository.refreshApprovedReviews(attractionId)
                
                // 2. If authenticated, refresh user's own reviews
                if (isAuthenticated.value) {
                    reviewRepository.refreshUserOwnReviews(attractionId)
                    
                    // Collect user's own reviews
                    reviewRepository.userOwnReviews.collect { userReviews ->
                        _userOwnReviews.value = userReviews
                    }
                }
                
                // 3. Collect approved reviews (excluding user's own to avoid duplicates)
                reviewRepository.getReviewsForAttraction(attractionId, _sortBy.value)
                    .collect { allReviews ->
                        val userReviewIds = _userOwnReviews.value.map { it.id }.toSet()
                        
                        // Filter out user's own reviews from public list
                        _reviews.value = allReviews.filter { it.id !in userReviewIds }
                        
                        _loading.value = false
                    }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load reviews")
                _error.value = e.message
                _loading.value = false
            }
        }
    }
    
    // ✅ НОВЫЙ: React to review (like/dislike)
    fun reactToReview(reviewId: String, isLike: Boolean) {
        if (!isAuthenticated.value) {
            _error.value = "Требуется авторизация"
            return
        }
        
        viewModelScope.launch {
            when (val result = reviewRepository.reactToReview(reviewId, isLike)) {
                is NetworkResult.Success -> {
                    // Refresh review to get updated counts
                    currentAttractionId?.let { loadReviews(it, forceRefresh = true) }
                }
                is NetworkResult.Error -> {
                    _error.value = result.message
                }
            }
        }
    }
}
```

#### 4.3 Обновить вызов `ReviewSection` в `AttractionDetailScreen.kt`

```kotlin
// Текущий код
ReviewSection(
    attractionId = attractionId,
    attractionName = attraction.name,
    averageRating = attraction.averageRating ?: 0f,
    totalReviews = attraction.reviewsCount ?: 0,
    reviews = reviews,
    sortBy = sortBy,
    onSortChange = reviewViewModel::setSortBy,
    onWriteReview = { showWriteReviewModal = true },
    onLike = { reviewViewModel.reactToReview(it, isLike = true) },      // ← ОБНОВИТЬ
    onDislike = { reviewViewModel.reactToReview(it, isLike = false) }, // ← ОБНОВИТЬ
    onShare = { /* TODO: Share review */ }
)

// ✅ НОВЫЙ код
val userOwnReviews by reviewViewModel.userOwnReviews.collectAsStateWithLifecycle()

ReviewSection(
    attractionId = attractionId,
    attractionName = attraction.name,
    averageRating = attraction.averageRating ?: 0f,
    totalReviews = attraction.reviewsCount ?: 0,
    reviews = reviews,
    userOwnReviews = userOwnReviews,  // ← ДОБАВИТЬ
    sortBy = sortBy,
    onSortChange = reviewViewModel::setSortBy,
    onWriteReview = { showWriteReviewModal = true },
    onLike = { reviewViewModel.reactToReview(it, isLike = true) },
    onDislike = { reviewViewModel.reactToReview(it, isLike = false) },
    onShare = { /* TODO */ }
)
```

---

### ФАЗА 5: Усиление системы Auth - связка между Settings и Reviews

#### 5.1 Текущая проблема

**SettingsScreen:**
```kotlin
val authState by authViewModel.authState.collectAsStateWithLifecycle()
```

**ReviewViewModel:**
```kotlin
val isAuthenticated: StateFlow<Boolean> = authRepository.authState
    .map { it.isAuthenticated }
    .stateIn(...)
```

**AttractionDetailScreen:**
```kotlin
val isAuthenticated by reviewViewModel.isAuthenticated.collectAsStateWithLifecycle()

// При попытке написать отзыв
if (!isAuthenticated) {
    showAuthModal = true
}
```

**✅ Проблема:** Если пользователь уже вошёл в SettingsScreen, но `isAuthenticated` в `ReviewViewModel` ещё не обновился, покажется AuthModal.

#### 5.2 Решение: Единый источник истины

**Уже реализовано:** `AuthRepository.authState` - единственный Flow.

**Проблема:** Возможна задержка в обновлении StateFlow между экранами.

**Решение:**

1. **Добавить метод прямой проверки в `AuthRepository`:**

```kotlin
class AuthRepository @Inject constructor(...) {
    
    // ✅ ДОБАВИТЬ: синхронная проверка текущего состояния
    fun isCurrentlyAuthenticated(): Boolean {
        return _authState.value.isAuthenticated
    }
    
    fun getCurrentUser(): User? {
        return _authState.value.user
    }
}
```

2. **Обновить `ReviewViewModel.canWriteReview()`:**

```kotlin
fun canWriteReview(): Boolean {
    // Используем синхронную проверку вместо StateFlow
    if (!authRepository.isCurrentlyAuthenticated()) {
        _showAuthRequired.value = true
        return false
    }
    
    if (_hasUserReviewed.value) {
        _error.value = "Вы уже оставили отзыв для этого места"
        return false
    }
    
    return true
}
```

3. **Обновить логику в `AttractionDetailScreen.kt` и `AttractionBottomSheet.kt`:**

```kotlin
// Before
if (!isAuthenticated) {
    showAuthModal = true
    return
}

// After - используем ViewModel метод
if (!reviewViewModel.canWriteReview()) {
    if (reviewViewModel.showAuthRequired.value) {
        showAuthModal = true
    }
    return
}
```

#### 5.3 Связать AuthModal с успешной авторизацией

**В `AttractionDetailScreen.kt`:**

```kotlin
// AuthModal callback
onAuthSuccess = {
    showAuthModal = false
    // После успешного логина открываем WriteReviewModal
    showWriteReviewModal = true
}
```

**В `AttractionBottomSheet.kt`:**

```kotlin
var pendingReviewAfterAuth by remember { mutableStateOf(false) }

// В WriteReview клик
onWriteReview = {
    if (!reviewViewModel.canWriteReview()) {
        if (reviewViewModel.showAuthRequired.value) {
            pendingReviewAfterAuth = true
            showAuthModal = true
        }
        return@BottomSheet
    }
    showWriteReviewModal = true
}

// AuthModal
AuthModal(
    visible = showAuthModal,
    onDismiss = { showAuthModal = false },
    onAuthSuccess = {
        showAuthModal = false
        if (pendingReviewAfterAuth) {
            pendingReviewAfterAuth = false
            showWriteReviewModal = true
        }
    }
)
```

---

### ФАЗА 6: Выравнивание звёзд в WriteReviewModal

#### 6.1 Текущий код

```kotlin
@Composable
fun InteractiveRatingLarge(
    value: Int,
    onRatingChange: (Int) -> Unit,
    size: Int = 40,
    spacing: Int = 8,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(5) { index ->
            IconButton(
                onClick = { onRatingChange(index + 1) },
                modifier = Modifier.size((size + 16).dp)
            ) {
                Icon(
                    imageVector = if (isFilled) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Оценка $starIndex",
                    modifier = Modifier.size(size.dp),
                    tint = if (isFilled) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
```

**Вызов:**
```kotlin
InteractiveRatingLarge(
    value = rating,
    onRatingChange = { rating = it },
    size = 48,
    spacing = 12
)
```

#### 6.2 Возможные проблемы и фиксы

**Проблема 1:** Spacing неравномерный из-за `IconButton` padding

**Решение:**
```kotlin
IconButton(
    onClick = { onRatingChange(starIndex) },
    modifier = Modifier
        .size((size + 16).dp)
        .padding(0.dp) // ← Убрать внутренний padding
) {
    Icon(...)
}
```

**Проблема 2:** Звёзды не выровнены по центру родителя

**Решение:** Добавить `horizontalAlignment` в родительский Column

```kotlin
// В WriteReviewModal.kt
Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally  // ← ВАЖНО
) {
    // Interactive stars
    InteractiveRatingLarge(...)
}
```

**Проблема 3:** Иконки `Icons.Outlined.StarBorder` могут иметь другой visual weight

**Решение:** Использовать кастомные иконки или убедиться что outline stars того же размера

```kotlin
// Alternative: Use custom icons with same dimensions
Icon(
    painter = painterResource(
        id = if (isFilled) R.drawable.ic_star_filled else R.drawable.ic_star_outline
    ),
    ...
)
```

---

## 📋 Чеклист задач (TODO List)

### Backend (Supabase)
- [ ] Создать миграцию для таблицы `review_reactions`
- [ ] Добавить поля `likes_count` и `dislikes_count` в таблицу `reviews`
- [ ] Создать триггер `update_review_reaction_counts()`
- [ ] Настроить RLS политики для `review_reactions`
- [ ] Тестировать через Supabase Dashboard

### API Layer (Kotlin)
- [ ] Создать `ReviewReactionDto.kt` с request/response моделями
- [ ] Добавить эндпоинты в `SupabaseApiService.kt`:
  - `upsertReviewReaction()`
  - `deleteReviewReaction()`
  - `getUserReaction()`
- [ ] Обновить `ReviewsRemoteDataSource` для реакций

### Domain Layer
- [ ] Обновить `Review` модель:
  - Переименовать `likes` → `likesCount`
  - Переименовать `dislikes` → `dislikesCount`
  - Добавить `userReaction: ReviewReaction`
- [ ] Обновить `ReviewDto` mapping в `ReviewRepository`

### Repository Layer
- [ ] Добавить `_userReactions: StateFlow<Map<String, ReviewReaction>>` в `ReviewRepository`
- [ ] Реализовать `reactToReview(reviewId, isLike)` с toggle логикой
- [ ] Реализовать `refreshUserOwnReviews(attractionId)` для fetch user's reviews
- [ ] Обновить mapping в `refreshApprovedReviews()` для `likesCount/dislikesCount`

### ViewModel Layer
- [ ] Добавить `_userOwnReviews: StateFlow<List<Review>>` в `ReviewViewModel`
- [ ] Обновить `loadReviews()` для загрузки user's own reviews
- [ ] Реализовать `reactToReview(reviewId, isLike)` метод
- [ ] Добавить фильтрацию дубликатов: user's approved review не должен быть в обоих списках
- [ ] Обновить `canWriteReview()` для синхронной проверки `authRepository.isCurrentlyAuthenticated()`

### UI Components
- [ ] Создать `StatusBadge` composable (На модерации, Опубликован, Отклонён)
- [ ] Обновить `ReviewSection.kt`:
  - Добавить параметр `userOwnReviews: List<Review>`
  - Рендерить секцию "Ваш отзыв" выше остальных
  - Фильтровать остальные отзывы (без своих)
- [ ] Обновить `ReviewCard.kt`:
  - Заменить `review.likes` → `review.likesCount`
  - Заменить `review.dislikes` → `review.dislikesCount`
  - Добавить visual state для `userReaction` (highlight like/dislike button)
- [ ] Обновить `WriteReviewModal.kt`:
  - Проверить выравнивание звёзд (добавить центрирование)
  - Убрать лишние paddings в `IconButton`
- [ ] Обновить `AttractionDetailScreen.kt`:
  - Добавить `userOwnReviews` state
  - Передать `userOwnReviews` в `ReviewSection`
  - Обновить обработчики `onLike`/`onDislike` для вызова `reviewViewModel.reactToReview()`

### Auth Integration
- [ ] Добавить `isCurrentlyAuthenticated()` в `AuthRepository`
- [ ] Добавить `getCurrentUser()` в `AuthRepository`
- [ ] Обновить `canWriteReview()` в `ReviewViewModel` для синхронной проверки
- [ ] Добавить `onAuthSuccess` callback в `AuthModal` компонентах:
  - `AttractionDetailScreen` - открывать `WriteReviewModal` после логина
  - `AttractionBottomSheet` - открывать `WriteReviewModal` после логина

### Testing
- [ ] Тестировать создание реакции (like)
- [ ] Тестировать toggle реакции (like → dislike, dislike → none)
- [ ] Тестировать RLS: один юзер = одна реакция
- [ ] Тестировать отображение своего отзыва (pending/approved/rejected)
- [ ] Тестировать отсутствие дубликатов
- [ ] Тестировать связку Auth между Settings и Reviews
- [ ] Тестировать выравнивание звёзд в разных размерах экрана

### Documentation
- [ ] Обновить `AUTH_AND_REVIEWS_IMPLEMENTATION.md` с новыми фичами
- [ ] Задокументировать схему `review_reactions` таблицы
- [ ] Добавить примеры API calls для реакций
- [ ] Создать диаграмму flow для "User's Own Review Display"

---

## 🎨 UI/UX Референсы (RN версия)

### Пример отображения своего отзыва (React Native)

```tsx
{/* User's pending/rejected reviews */}
{userAttractionReviews.length > 0 && (
  <View style={styles.userReviewsSection}>
    <Text style={styles.userReviewsTitle}>Ваши отзывы</Text>
    {userAttractionReviews.map((review) => (
      <View key={review.id} style={styles.userReviewWrapper}>
        {/* Status badge */}
        <View
          style={[
            styles.statusBadge,
            review.status === 'pending' && styles.statusBadgePending,
            review.status === 'rejected' && styles.statusBadgeRejected,
            review.status === 'approved' && styles.statusBadgeApproved,
          ]}
        >
          <Text style={styles.statusBadgeText}>
            {review.status === 'pending' && 'На модерации'}
            {review.status === 'rejected' && 'Отклонён'}
            {review.status === 'approved' && 'Одобрен'}
          </Text>
        </View>
        <ReviewCard review={review} />
      </View>
    ))}
  </View>
)}
```

**Цвета для бейджей статуса:**

- **Pending (На модерации):**
  - Background: `#FFF4E5` (light orange)
  - Text: `#FF9800` (orange)
  
- **Approved (Опубликован):**
  - Background: `#E8F5E9` (light green)
  - Text: `#4CAF50` (green)
  
- **Rejected (Отклонён):**
  - Background: `#FFEBEE` (light red)
  - Text: `#F44336` (red)

---

## 🔗 Связанные файлы

### Backend
- `Landing-Admin/server/routes.ts` - Admin endpoints для approve/reject
- `supabase-storage-setup.sql` - Схема БД

### Kotlin (AdygGIS-KT)
- `app/data/repository/ReviewRepository.kt`
- `app/data/remote/api/SupabaseApiService.kt`
- `app/domain/model/Review.kt`
- `app/presentation/viewmodel/ReviewViewModel.kt`
- `app/presentation/ui/components/reviews/ReviewSection.kt`
- `app/presentation/ui/components/reviews/ReviewCard.kt`
- `app/presentation/ui/components/reviews/WriteReviewModal.kt`
- `app/presentation/ui/screens/detail/AttractionDetailScreen.kt`

### React Native (Референс)
- `AdygGIS-RN/src/components/reviews/ReviewSection.tsx`
- `AdygGIS-RN/src/components/reviews/ReviewCard.tsx`
- `AdygGIS-RN/src/stores/reviewStore.ts`
- `AdygGIS-RN/src/services/reviews/reviewService.ts`

---

## 📊 Ожидаемые результаты

### 1. Отображение отзывов

**До:**
```
[Отзывы]
─────────────────────────
 ⭐⭐⭐⭐⭐ Ваш отзыв
 ⭐⭐⭐⭐ Отзыв юзера 1
 ⭐⭐⭐⭐⭐ Отзыв юзера 2
```

**После:**
```
[Отзывы]
─────────────────────────
[Ваш отзыв]
┌─────────────────────────┐
│ 🟠 На модерации         │
│ ⭐⭐⭐⭐⭐ Отличное место!│
└─────────────────────────┘

[Отзывы других пользователей]
─────────────────────────
 ⭐⭐⭐⭐ Отзыв юзера 1 (👍 5)
 ⭐⭐⭐⭐⭐ Отзыв юзера 2 (👍 12)
```

### 2. Система реакций

**Сценарий:**
1. Пользователь нажимает 👍 → Запрос `POST /review_reactions` → Счётчик +1
2. Пользователь нажимает 👍 снова → Запрос `DELETE /review_reactions` → Счётчик -1 (toggle off)
3. Пользователь нажимает 👎 → Запрос `UPSERT /review_reactions` → Дизлайк +1, Лайк -1 (toggle switch)

**Ограничения:**
- Один юзер может иметь только одну реакцию (UNIQUE constraint)
- Нельзя лайкать свой отзыв (UI блокирует кнопки для `isOwn: true`)

### 3. Усиленный Auth Flow

**Сценарий:**
1. Пользователь логинится в Settings → `AuthRepository.authState` обновляется
2. Переходит к Attraction Detail → Нажимает "Написать отзыв"
3. **БЕЗ показа AuthModal** (т.к. `isCurrentlyAuthenticated() == true`)
4. Сразу открывается WriteReviewModal

**Сценарий 2 (без логина):**
1. Пользователь НЕ залогинен → Нажимает "Написать отзыв"
2. Показывается AuthModal
3. Логинится → `onAuthSuccess` callback → Открывается WriteReviewModal автоматически

---

## ⚠️ Важные замечания

### 1. Миграция данных
- При добавлении `likes_count` и `dislikes_count` в `reviews`, нужно установить DEFAULT 0
- Если есть существующие отзывы - они получат 0 лайков/дизлайков (корректно)

### 2. RLS Security
- `review_reactions.user_id` ДОЛЖЕН совпадать с `auth.uid()`
- Без этого пользователи смогут создавать фейковые реакции от чужих имён

### 3. Performance
- Индекс `idx_reviews_popularity` нужен для сортировки по `ReviewSortOption.DEFAULT`
- Триггер `update_review_reaction_counts()` выполняется синхронно - может быть bottleneck при большой нагрузке
- Возможно в будущем перейти на materialized view или background job

### 4. UI State
- Используем `collectAsStateWithLifecycle()` для автоматического управления lifecycle
- `userOwnReviews` и `reviews` - независимые StateFlows, обновляются параллельно

### 5. Дубликаты
- **Критически важно:** Фильтровать `reviews` по `userReviewIds` чтобы не показывать approved отзыв дважды
- Логика фильтрации в `ReviewViewModel.loadReviews()`

---

## 🚀 Последовательность реализации (рекомендуемая)

1. **Backend first:** Миграция БД → RLS политики → Тестирование через SQL
2. **API Layer:** DTOs → Retrofit endpoints → Remote Data Source
3. **Domain & Repository:** Обновить модели → Реализовать `reactToReview()` → Реализовать `refreshUserOwnReviews()`
4. **ViewModel:** Добавить `userOwnReviews` flow → Реализовать `reactToReview()` → Обновить `loadReviews()`
5. **UI Components:** StatusBadge → ReviewSection update → ReviewCard update
6. **Auth Integration:** Синхронная проверка → Callbacks после логина
7. **Star Alignment:** Фикс `WriteReviewModal` выравнивания
8. **Testing:** E2E тест всех flow
9. **Documentation:** Обновить доки

---

## 📚 Референсы и примеры кода

### RN: Fetch user's own reviews

```typescript
// AdygGIS-RN/src/services/reviews/reviewService.ts
export async function fetchUserReviews(attractionId: string): Promise<Review[]> {
  try {
    const { data: { user } } = await supabase.auth.getUser();
    if (!user) return [];

    const { data, error } = await supabase
      .from('reviews')
      .select('*')
      .eq('attraction_id', attractionId)
      .eq('user_id', user.id);

    if (error) {
      console.error('[ReviewService] Fetch user reviews error:', error);
      return [];
    }

    return (data || []).map((row) => ({
      ...mapRowToReview(row, profileMap[user.id]),
      isOwn: true,
    }));
  } catch (error) {
    console.error('[ReviewService] fetchUserReviews failed:', error);
    return [];
  }
}
```

### Kotlin: Equivalent implementation

```kotlin
suspend fun refreshUserOwnReviews(attractionId: String) {
    val authState = authRepository.authState.value
    if (!authState.isAuthenticated || authState.user == null) {
        _userOwnReviews.value = emptyList()
        return
    }
    
    when (val result = reviewsRemoteDataSource.getUserReviews(attractionId, authState.user.id)) {
        is NetworkResult.Success -> {
            val mapped = result.data.map { dto ->
                Review(
                    id = dto.id,
                    attractionId = dto.attractionId,
                    authorId = dto.userId ?: "",
                    authorName = authState.user.displayName ?: "Вы",
                    rating = dto.rating,
                    text = listOfNotNull(dto.title, dto.body).joinToString("\n").ifBlank { null },
                    createdAt = parseInstant(dto.createdAt) ?: Instant.now(),
                    updatedAt = parseInstant(dto.updatedAt),
                    likesCount = 0, // User can't see own review reactions
                    dislikesCount = 0,
                    isOwn = true,
                    status = dto.status,
                    rejectionReason = dto.rejectionReason
                )
            }
            _userOwnReviews.value = mapped
        }
        is NetworkResult.Error -> {
            Timber.w("Failed to fetch user's own reviews: ${result.message}")
            _userOwnReviews.value = emptyList()
        }
    }
}
```

---

**Конец документа**

Этот план покрывает все требования пользователя:
✅ Свой отзыв выше всех с бейджем статуса
✅ Нет дубликатов
✅ Усиленный Auth (связка между Settings и Reviews)
✅ Полноценная система лайков/дизлайков с БД
✅ Выравнивание звёзд в WriteReviewModal

Готов к реализации! 🚀
