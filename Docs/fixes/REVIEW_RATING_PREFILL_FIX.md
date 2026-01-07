# Исправление: Предзаполнение рейтинга при написании отзыва

**Дата:** 2026-01-07  
**Статус:** ✅ Исправлено

## Проблема

При нажатии на звезду в блоке рейтинга (например, 4★), модальное окно написания отзыва открывалось с рейтингом 0 звезд. Пользователю приходилось повторно выбирать то же количество звезд.

## Решение

Теперь при нажатии на звезду в `RatingSummaryBlock`, выбранный рейтинг передается в `WriteReviewModal` как `initialRating`.

## Измененные файлы

### 1. RatingSummaryBlock.kt
```kotlin
// ДО:
onRatePress: () -> Unit

// ПОСЛЕ:
onRatePress: (Int) -> Unit

// Использование:
InteractiveRating(
    value = 0,
    onRatingChange = { rating -> onRatePress(rating) },
    size = 32.dp,
    spacing = 4.dp
)
```

### 2. ReviewSection.kt
```kotlin
// ДО:
onWriteReview: () -> Unit

// ПОСЛЕ:
onWriteReview: (Int) -> Unit
```

### 3. AttractionBottomSheet.kt
```kotlin
// Добавлено состояние:
var initialRating by remember { mutableIntStateOf(0) }

// Callback с рейтингом:
onWriteReview = { rating ->
    if (reviewViewModel.canWriteReview()) {
        initialRating = rating
        showWriteReviewModal = true
    }
}

// Передача в модальное окно:
WriteReviewModal(
    // ...
    initialRating = initialRating
)
```

### 4. AttractionDetailScreen.kt
Аналогичные изменения как в `AttractionBottomSheet.kt`.

## Результат

**ДО:**
1. Пользователь нажимает на 4★ в блоке рейтинга
2. Открывается модальное окно с 0★
3. Пользователь снова выбирает 4★

**ПОСЛЕ:**
1. Пользователь нажимает на 4★ в блоке рейтинга
2. Открывается модальное окно с уже выбранными 4★ ✅
3. Пользователь сразу может писать текст отзыва

## Примечание

`WriteReviewModal` уже имел параметр `initialRating`, но он не использовался. Теперь он корректно передается из UI компонентов.
