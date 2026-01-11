# Auth & Reviews Implementation — Kotlin (AdygGIS-KT)

**Дата:** 8 января 2026  
**Версия:** 1.3  
**Статус:** ✅ Реализовано и проверено

---

## 📋 Обзор

Реализация модуля авторизации пользователей и отзывов с модерацией для Android-приложения AdygGIS-KT. Архитектура аналогична React Native версии (AdygGIS-RN), но адаптирована под нативный Android с использованием:

- **Retrofit + OkHttp** для REST API вызовов к Supabase GoTrue
- **EncryptedSharedPreferences (AndroidX Security Crypto)** для безопасного хранения сессии
- **Hilt** для DI
- **Jetpack Compose** для UI

**Важно про обязательность логина:**
- Приложение работает без входа в аккаунт.
- Auth требуется только:
   1) при попытке оставить отзыв (Write Review)
   2) если пользователь сам нажимает «Войти» в настройках в плитке «Аккаунт».

---

## 🏗️ Архитектура

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          SUPABASE (Backend)                                  │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────────────┐   │
│  │   PostgreSQL     │  │   GoTrue Auth    │  │      Storage             │   │
│  │   ────────────   │  │   ──────────     │  │      ───────             │   │
│  │   attractions    │  │   Email/Password │  │   images/                │   │
│  │   reviews        │  │   JWT tokens     │  │   avatars/               │   │
│  │   profiles       │  │                  │  │                          │   │
│  └──────────────────┘  └──────────────────┘  └──────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
                    ↑                               ↑
                    │ REST API                      │ REST API
                    │ (Retrofit)                    │ (Retrofit)
                    │                               │
┌───────────────────┴───────────────────────────────┴─────────────────────────┐
│                     AdygGIS-KT (Android)                                  │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                           Data Layer                                    │ │
│  │  ┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────┐ │ │
│  │  │  AuthRepository     │  │  ReviewRepository   │  │  Secure prefs  │ │ │
│  │  │  ───────────────    │  │  ────────────────   │  │  ─────────      │ │ │
│  │  │  • signIn()         │  │  • submitReview()   │  │  • auth_prefs   │ │ │
│  │  │  • signUp()         │  │  • refreshReviews() │  │  • session      │ │ │
│  │  │  • signOut()        │  │  • hasUserReviewed()│  │                 │ │ │
│  │  │  • refreshToken()   │  │  • deleteReview()   │  │                 │ │ │
│  │  └─────────────────────┘  └─────────────────────┘  └─────────────────┘ │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                         Presentation Layer                              │ │
│  │  ┌─────────────────────┐  ┌─────────────────────┐                      │ │
│  │  │  AuthViewModel      │  │  ReviewViewModel    │                      │ │
│  │  │  ───────────────    │  │  ───────────────    │                      │ │
│  │  │  • authState Flow   │  │  • reviews Flow     │                      │ │
│  │  │  • uiState Flow     │  │  • canWriteReview() │                      │ │
│  │  │  • events Flow      │  │  • submitReview()   │                      │ │
│  │  └─────────────────────┘  └─────────────────────┘                      │ │
│  │                                                                         │ │
│  │  ┌─────────────────────┐  ┌─────────────────────┐                      │ │
│  │  │  AuthModal          │  │  WriteReviewModal   │                      │ │
│  │  │  (Compose)          │  │  (Compose)          │                      │ │
│  │  └─────────────────────┘  └─────────────────────┘                      │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 📁 Структура файлов

### Data Layer

```
app/src/main/java/com/adygyes/app/
├── data/
│   ├── local/
│   │   └── preferences/
│   │       ├── AuthPreferencesManager.kt    # Legacy (DataStore) — не используется в актуальной auth цепочке
│   │       └── SecureAuthPreferencesManager.kt # EncryptedSharedPreferences (актуально)
│   ├── remote/
│   │   ├── TokenAuthenticator.kt            # 401 refresh + proactive refresh interceptor
│   │   ├── api/
│   │   │   ├── SupabaseApiService.kt        # REST API (attractions, reviews)
│   │   │   └── SupabaseAuthApi.kt           # GoTrue Auth API
│   │   └── dto/
│   │       ├── AuthDto.kt                   # Auth request/response DTOs
│   │       ├── ReviewDto.kt                 # Review DTO
│   │       └── CreateReviewDto.kt           # Review submission DTO
│   └── repository/
│       ├── AuthRepository.kt                # Auth business logic
│       └── ReviewRepository.kt              # Reviews business logic
```

### Domain Layer

```
├── domain/
│   └── model/
│       ├── User.kt                          # User + AuthState models
│       └── Review.kt                        # Review model (updated with status)
```

### Presentation Layer

```
├── presentation/
│   ├── viewmodel/
│   │   ├── AuthViewModel.kt                 # Auth UI state management
│   │   └── ReviewViewModel.kt               # Reviews UI state (updated)
│   └── ui/
│       ├── components/
│       │   ├── auth/
│       │   │   └── AuthModal.kt             # Login/Register/Reset modal
│       │   └── reviews/
│       │       ├── WriteReviewModal.kt      # Review submission modal
│       │       └── ReviewSection.kt         # Reviews list section
│       └── screens/
│           ├── settings/
│           │   └── SettingsScreen.kt        # Account section added
│           └── detail/
│               └── AttractionDetailScreen.kt # Auth integration for reviews
```

### DI Module

```
├── di/
│   └── module/
│       └── AuthModule.kt                    # Hilt DI for Auth dependencies
```

---

## 🔐 Авторизация

## 🔑 Заголовки Supabase (anon vs user JWT)

В Supabase есть два разных смысла «Authorization»:

1) **Публичный доступ (anon)**
   - Для PostgREST запросов приложение всегда добавляет `apikey: <anon_key>`.
   - Если запрос не требует пользовательской авторизации, добавляется `Authorization: Bearer <anon_key>`.

2) **Пользовательская авторизация (user JWT)**
   - Для операций, где нужен `auth.uid()` (создание/проверка/удаление своих отзывов), в запрос передаётся `Authorization: Bearer <access_token>` пользователя.
   - Важно: общий интерсептор Supabase НЕ перезатирает `Authorization`, если он уже задан в конкретном запросе.

**Ключевой момент:** именно user JWT даёт Supabase возможность вычислить `auth.uid()` и применить RLS (например, `DEFAULT user_id = auth.uid()`).

### Supabase GoTrue API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/auth/v1/token?grant_type=password` | POST | Sign in with email/password |
| `/auth/v1/signup` | POST | Register new user |
| `/auth/v1/token?grant_type=refresh_token` | POST | Refresh access token |
| `/auth/v1/logout` | POST | Sign out (invalidate token) |
| `/auth/v1/recover` | POST | Send password reset email |

### Auth State Machine

```
┌─────────────┐
│   Unknown   │ ← App start
└──────┬──────┘
       │ Check stored session
       ▼
┌─────────────┐     No session     ┌─────────────────────┐
│   Loading   │ ─────────────────► │   Unauthenticated   │
└──────┬──────┘                    └──────────┬──────────┘
       │ Token found                          │
       │ (refresh token)                      │ User signs in
       ▼                                      ▼
┌─────────────────────┐            ┌─────────────────────┐
│   Authenticated     │ ◄──────────┤     Loading         │
│                     │            └─────────────────────┘
│  • user: User       │
│  • accessToken      │
│  • refreshToken     │
└──────────┬──────────┘
           │ User signs out
           ▼
┌─────────────────────┐
│   Unauthenticated   │
└─────────────────────┘
```

### Session Persistence

- **SecureAuthPreferencesManager** используется для хранения:
  - `access_token` — JWT токен для API запросов
  - `refresh_token` — токен для обновления сессии
   - `token_expires_at` — время истечения access token (для проактивного refresh)
  - `user_id`, `user_email`, `user_display_name`, `user_avatar_url`

> Примечание: если EncryptedSharedPreferences не может быть инициализирован (редкие device/keystore проблемы), сессия хранится **только в памяти процесса** (без plaintext persistence).

- При старте приложения:
  1. `AuthRepository.init()` проверяет наличие сохранённой сессии
   2. Если токен истёк — пробует `refreshToken()` (иначе logout)
   3. Если токен скоро истечёт — делает проактивный refresh (ошибка не выкидывает пользователя, если текущий токен ещё валиден)
   4. При успехе — `AuthState.Authenticated`, при фатальной ошибке — очищает сессию

### Авто-refresh на сетевом уровне

- **ProactiveTokenRefreshInterceptor**: обновляет токен до запроса, если он скоро истечёт (только для user JWT запросов)
- **TokenAuthenticator (OkHttp Authenticator)**: при `401 Unauthorized` пытается обновить токен и повторить запрос (1 retry, защита от циклов)

---

## 📝 Отзывы

### API Endpoints

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/rest/v1/reviews?attraction_id=eq.{id}&status=eq.approved` | GET | No | Get approved reviews |
| `/rest/v1/reviews` | POST | Yes | Create review (pending) |
| `/rest/v1/reviews?user_id=eq.{id}` | GET | Yes | Get user's own reviews |
| `/rest/v1/reviews?id=eq.{id}` | DELETE | Yes | Delete own review |

### Review Flow

```
User clicks "Write Review"
         │
         ▼
┌─────────────────────┐
│  canWriteReview()   │
│  Check auth status  │
└──────────┬──────────┘
           │
     ┌─────┴─────┐
     │           │
Not authenticated  Authenticated
     │           │
     ▼           ▼
┌─────────────┐  ┌───────────────┐
│ AuthModal   │  │ hasUserReviewed?│
│ (login/reg) │  └───────┬───────┘
└──────┬──────┘          │
       │           ┌─────┴─────┐
       │           │           │
       │          Yes          No
       │           │           │
       │           ▼           ▼
       │   ┌─────────────┐  ┌──────────────┐
       │   │ Show error  │  │WriteReviewModal│
       │   │"Уже оставили│  │ rating + text │
       │   │    отзыв"   │  └───────┬──────┘
       │   └─────────────┘          │
       │                            ▼
       │                 ┌──────────────────┐
       └─────────────────┤  submitReview()  │
                         │  (status=pending)│
                         └────────┬─────────┘
                                  │
                                  ▼
                         ┌──────────────────┐
                         │  "Отзыв отправлен│
                         │  на модерацию"   │
                         └──────────────────┘
```

### RLS Policies

Reviews table policies ensure:
- ✅ Anyone can read `status='approved'` reviews
- ✅ Authenticated users can read their own reviews (any status)
- ✅ Authenticated users can create reviews (auto `status='pending'`)
- ✅ Users can update own `status='pending'` reviews
- ✅ Users can delete their own reviews
- ❌ Only admin (via service_role) can approve/reject

### Модерация в админке

- Все новые отзывы создаются со статусом `pending`.
- В Admin Panel (Landing-Admin) администратор:
   - Approved: выставляет `status = 'approved'`
   - Rejected: выставляет `status = 'rejected'` + `rejection_reason`
- Клиентское приложение показывает всем пользователям только `approved` отзывы (RLS + явный фильтр `status=eq.approved`).

---

## 🎨 UI Components

### AuthModal

Модальное окно с тремя режимами:
- **SIGN_IN** — вход по email/password
- **SIGN_UP** — регистрация с опциональным display_name
- **FORGOT_PASSWORD** — сброс пароля

Features:
- Переключение между режимами
- Валидация полей (email format, password min 6 chars)
- Показ/скрытие пароля
- Локализованные сообщения об ошибках
- Loading state

### Settings Integration

В SettingsScreen добавлена секция "Аккаунт":
- Если не авторизован: кнопка "Войти"
- Если авторизован: имя пользователя + email, клик для выхода
- Диалог подтверждения выхода

### WriteReviewModal Integration

- При клике на "Оставить отзыв":
   1. `ReviewViewModel.canWriteReview()` проверяет auth
   2. Если не авторизован → `showAuthRequired` → показывается `AuthModal`
   3. После успешного входа → автоматически открывается `WriteReviewModal`

---

## 🔧 Локализация ошибок

```kotlin
// AuthRepository.translateAuthError()
"Invalid login credentials" → "Неверный email или пароль"
"Email not confirmed" → "Email не подтверждён. Проверьте почту."
"User already registered" → "Пользователь с таким email уже зарегистрирован"
"Password should be at least" → "Пароль должен содержать минимум 6 символов"
"rate limit" → "Слишком много попыток. Подождите немного."

// Network errors
"Unable to resolve host" → "Нет подключения к интернету"
"timeout" → "Превышено время ожидания. Проверьте подключение."
```

---

## ✅ Чеклист готовности

- [x] AuthRepository — sign in/up/out, token refresh
- [x] SecureAuthPreferencesManager — encrypted session persistence
- [x] TokenAuthenticator / ProactiveTokenRefreshInterceptor — auto refresh (401 + proactive)
- [x] SupabaseAuthApi — Retrofit interface
- [x] AuthModule — Hilt DI
- [x] AuthViewModel — UI state management
- [x] AuthModal — Compose UI (login/register/reset) + password strength
- [x] ReviewRepository — submit review with auth
- [x] ReviewViewModel — canWriteReview() auth check
- [x] SettingsScreen — account section
- [x] AttractionDetailScreen — auth integration
- [x] AttractionBottomSheet — auth integration
- [x] Build successful ✅

---

## ✅ Реализовано (фактическое состояние на 2026-01-08)

- Secure session storage: EncryptedSharedPreferences (SecureAuthPreferencesManager)
- Token lifecycle: expires_at + проактивный refresh + refresh при 401
- AuthModal UX: debounce на отправку, нормальная email-валидация, индикатор силы пароля при регистрации
- Reviews: offline-first + moderation (pending/approved/rejected) + optimistic reactions

## ⏳ В планах (не реализовано в коде)

- Миграция старой DataStore-сессии в SecureAuthPreferencesManager (чтобы не требовать перелогин после обновления)
- Дополнительные UX подсказки/обработка offline сценариев именно для Auth экрана (явные сообщения при отсутствии сети)

---

## 🚀 Тестирование

### Manual Testing Checklist

1. **Sign Up Flow**
   - [ ] Регистрация нового пользователя
   - [ ] Проверка email confirmation (если включено в Supabase)
   - [ ] Ошибка при дубликате email

2. **Sign In Flow**
   - [ ] Вход существующего пользователя
   - [ ] Ошибка при неверном пароле
   - [ ] Сессия сохраняется после рестарта

3. **Sign Out Flow**
   - [ ] Выход из аккаунта
   - [ ] Очистка сессии

4. **Review Submission**
   - [ ] Попытка без авторизации → AuthModal
   - [ ] Успешный вход → WriteReviewModal
   - [ ] Отправка отзыва → "На модерации"
   - [ ] Повторная попытка → "Уже оставили отзыв"

5. **Session Persistence**
   - [ ] Закрыть приложение
   - [ ] Открыть снова
   - [ ] Пользователь авторизован

---

## 🐛 Исправленные баги

### Январь 2026 — Runtime Fixes

#### 1. Location Permissions (SecurityException)
**Проблема:** `uid does not have ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION`  
**Причина:** Permission объявлен в манифесте, но не запрашивается runtime  
**Решение:** Добавлен `LaunchedEffect` в `MapScreen.kt` для автоматического запроса permissions при старте  
**Файл:** [MapScreen.kt#L164-L168](../../../app/src/main/java/com/adygyes/app/presentation/ui/screens/map/MapScreen.kt#L164-L168)

#### 2. Tombstones Timeout (SocketTimeoutException)
**Проблема:** Запрос `sync_metadata` timeout 30 секунд при старте  
**Причина:** Неправильный бюджет таймаутов мог приводить к долгим ожиданиям/повторам на слабой сети  
**Решение:** Введён общий бюджет `callTimeout` и настроены таймауты клиента (актуально по коду: `connectTimeout: 15s`, `readTimeout: 25s`, `writeTimeout: 15s`, `callTimeout: 120s`)  
**Файл:** [NetworkModule.kt](../../../app/src/main/java/com/adygyes/app/di/module/NetworkModule.kt)

#### 3. Marker Images JobCancellationException
**Проблема:** `JobCancellationException` при загрузке изображений маркеров  
**Причина:** Coroutine scope отменяется при recomposition/navigation  
**Решение:**  
- Добавлена проверка `isActive` перед обновлением UI  
- Отдельная обработка `CancellationException` (не логируется как error)  
- Поддержка передачи lifecycle-aware scope в `VisualMarkerProvider`  
**Файлы:**  
- [VisualMarkerProvider.kt#L745-L801](../../../app/src/main/java/com/adygyes/app/presentation/ui/map/markers/VisualMarkerProvider.kt#L745-L801)  
- [ImageCacheManager.kt#L137-L141](../../../app/src/main/java/com/adygyes/app/data/local/cache/ImageCacheManager.kt#L137-L141)

#### 4. Review Loading JobCancellationException
**Проблема:** `Failed to load reviews` с `JobCancellationException`  
**Причина:** Coroutine отменяется при закрытии bottom sheet / navigation  
**Решение:** Добавлена обработка `CancellationException` в `ReviewViewModel.loadReviews()`  
**Файл:** [ReviewViewModel.kt#L79-L95](../../../app/src/main/java/com/adygyes/app/presentation/viewmodel/ReviewViewModel.kt#L79-L95)

#### 5. Sign Up MissingFieldException
**Проблема:** `MissingFieldException: Fields [access_token, refresh_token, user] are required`  
**Причина:** Supabase иногда возвращает 200 OK с error payload (не 4xx), десериализация падает  
**Решение:** Обработка `SerializationException` при десериализации `AuthResponse` — если ошибка, парсим как error response  
**Файлы:**  
- [AuthRepository.kt#L132-L164](../../../app/src/main/java/com/adygyes/app/data/repository/AuthRepository.kt#L132-L164) — `signUp()`  
- [AuthRepository.kt#L97-L122](../../../app/src/main/java/com/adygyes/app/data/repository/AuthRepository.kt#L97-L122) — `signIn()`

#### 6. Review Submission RLS Error (Missing user_id)
**Проблема:** Отзыв не отправляется, RLS policy violation при INSERT  
**Причина:** `CreateReviewRequest` не включал `user_id`, а колонка в БД не имеет DEFAULT значения. RLS policy требует `auth.uid() = user_id`.  
**Решение:**  
- Добавлен `userId` в `CreateReviewRequest`
- При создании отзыва передаётся `authState.user.id`
- `body` сделан non-nullable (в БД это NOT NULL)  
**Файлы:**  
- [CreateReviewDto.kt](../../../app/src/main/java/com/adygyes/app/data/remote/dto/CreateReviewDto.kt)  
- [ReviewRepository.kt#L202-L210](../../../app/src/main/java/com/adygyes/app/data/repository/ReviewRepository.kt#L202-L210)

#### 7. Missing Success Toast for Review Submission
**Проблема:** После успешной отправки отзыва пользователь не получал обратную связь  
**Решение:** Добавлен Toast "Отзыв отправлен на модерацию" после успешной отправки  
**Файлы:**  
- [AttractionBottomSheet.kt](../../../app/src/main/java/com/adygyes/app/presentation/ui/components/AttractionBottomSheet.kt)  
- [AttractionDetailScreen.kt](../../../app/src/main/java/com/adygyes/app/presentation/ui/screens/detail/AttractionDetailScreen.kt)

#### 8. Review Reactions System (v1.2)
**Дата:** 6 января 2026  
**Проблема:** Отсутствовала система лайков/дизлайков, отзывы пользователя дублировались в общем списке, слабая связь Settings ↔ Reviews  
**Решение:**  
✅ **База данных:**
- Создана таблица `review_reactions` (review_id, user_id, reaction, UNIQUE constraint)
- Добавлены колонки `likes_count`, `dislikes_count` в `reviews`
- Создан триггер `update_review_reaction_counts()` для автоподсчёта
- Настроены RLS политики (публичное чтение, управление только своими реакциями)

✅ **API Layer:**
- `ReviewReactionDto.kt` — DTOs для запросов/ответов
- 3 новых endpoint в `SupabaseApiService`: upsert/delete/get reactions

✅ **Domain Layer:**
- Обновлена модель `Review`: `likes`→`likesCount`, `dislikes`→`dislikesCount`, добавлено `userReaction: ReviewReaction`
- `ReviewReaction` enum (LIKE, DISLIKE, NONE)

✅ **Repository:**
- `reactToReview(reviewId, isLike)` — toggle логика (та же реакция → DELETE, другая → UPSERT)
- `refreshUserOwnReviews()` — загрузка всех отзывов пользователя (pending/approved/rejected)
- Обновлены все маппинги DTOs для использования `likesCount`/`dislikesCount`

✅ **ViewModel:**
- `userOwnReviews: StateFlow<List<Review>>` — отдельный поток для отзывов пользователя
- Фильтрация дубликатов: `loadReviews()` исключает userReviewIds из публичного списка
- `reactToReview()` заменяет старые `likeReview()`/`dislikeReview()`
- `canWriteReview()` использует синхронную проверку `isCurrentlyAuthenticated()`

✅ **UI Components:**
- `StatusBadge.kt` — цветные бейджи статуса (pending/approved/rejected)
- `ReviewSection.kt` — секция "Ваш отзыв" выше публичных с бейджем статуса
- `ReviewCard.kt`:
  - Использует `likesCount`/`dislikesCount`
  - Визуальная индикация активной реакции (заполненная иконка, цвет primary/error)
  - Отключение кнопок для своих отзывов (`enabled = !review.isOwn`)
- `WriteReviewModal.kt` — исправлено выравнивание звёзд (убран `InteractiveRatingLarge`, встроен Row с `horizontalAlignment`)

✅ **Auth Integration:**
- `AuthRepository.isCurrentlyAuthenticated()` — синхронная проверка без StateFlow
- `AuthRepository.getCurrentUser()` — немедленный доступ к user без Flow

**Файлы:**
- Database: migrations (review_reactions, likes_count/dislikes_count, trigger, RLS)
- [ReviewReactionDto.kt](../../../app/src/main/java/com/adygyes/app/data/remote/dto/ReviewReactionDto.kt)
- [SupabaseApiService.kt](../../../app/src/main/java/com/adygyes/app/data/remote/api/SupabaseApiService.kt)
- [Review.kt](../../../app/src/main/java/com/adygyes/app/domain/model/Review.kt)
- [ReviewRepository.kt](../../../app/src/main/java/com/adygyes/app/data/repository/ReviewRepository.kt)
- [ReviewViewModel.kt](../../../app/src/main/java/com/adygyes/app/presentation/viewmodel/ReviewViewModel.kt)
- [StatusBadge.kt](../../../app/src/main/java/com/adygyes/app/presentation/ui/components/reviews/StatusBadge.kt)
- [ReviewSection.kt](../../../app/src/main/java/com/adygyes/app/presentation/ui/components/reviews/ReviewSection.kt)
- [ReviewCard.kt](../../../app/src/main/java/com/adygyes/app/presentation/ui/components/reviews/ReviewCard.kt)
- [WriteReviewModal.kt](../../../app/src/main/java/com/adygyes/app/presentation/ui/components/reviews/WriteReviewModal.kt)
- [AttractionDetailScreen.kt](../../../app/src/main/java/com/adygyes/app/presentation/ui/screens/detail/AttractionDetailScreen.kt)
- [AttractionBottomSheet.kt](../../../app/src/main/java/com/adygyes/app/presentation/ui/components/AttractionBottomSheet.kt)

---

## 📚 Референсы
