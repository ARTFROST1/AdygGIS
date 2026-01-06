# 🎨 Унификация UI компонентов: Kotlin ↔ RN

**Дата:** 5 января 2026  
**Версия:** 1.0  
**Статус:** План рефакторинга

---

## 📋 Содержание

1. [Цель унификации](#цель-унификации)
2. [Сравнение AttractionCard](#сравнение-attractioncard)
3. [Сравнение CategoryChip](#сравнение-categorychip)
4. [Расширенные поля UI](#расширенные-поля-ui)
5. [Чеклист изменений](#чеклист-изменений)

---

## 🎯 Цель унификации

Обе версии приложения (Kotlin и RN) должны отображать **одинаковые данные** в **одинаковом формате**:

| Аспект | Требование |
|--------|------------|
| Поля карточки | Идентичные |
| Порядок информации | Идентичный |
| Категории | Одинаковые цвета и emoji |
| Расширенные поля | Отображаются в обоих версиях |

---

## 📊 Сравнение AttractionCard

### Kotlin версия (текущая)

```kotlin
// AttractionCard.kt - отображаемые данные:
- images[0] → фон карточки
- category → CategoryChip (emoji + displayName)
- isFavorite → кнопка ❤️
- name → заголовок
- rating → звёзды
- address → адрес с иконкой 📍
- description → описание (2 строки)
```

### RN версия (текущая)

```typescript
// AttractionCard.tsx - отображаемые данные:
- images[0] → фон карточки (expo-image)
- category → CategoryChip (small, no emoji)
- isFavorite → кнопка ❤️ (animated)
- name → заголовок
- rating → RatingBar (звёзды)
- category → emoji + displayNameRu (в metaRow)
- description → описание (2 строки)
- address → адрес с иконкой 📍
```

### Различия

| Элемент | Kotlin | RN | Действие |
|---------|--------|----|---------| 
| Category chip | emoji + name | no emoji, small | ✅ Унифицировать |
| Category meta row | Нет | emoji + displayNameRu | Добавить в Kotlin |
| Rating position | После title | После category row | Унифицировать |
| Extended fields | Не отображаются | Не отображаются | Добавить в Detail |

### Kotlin: как повторить RN "карточку места" (Bottom Sheet)

В RN детальная карточка реализована как bottom sheet (snap points). Чтобы добиться максимально похожего UX в Kotlin:

- Использовать **Material3** `ModalBottomSheet` или `BottomSheetScaffold`
- Держать 2–3 состояния: `Hidden` → `PartiallyExpanded (preview)` → `Expanded (full)`
- Секции/порядок данных сделать такими же, как в RN (заголовок → meta row → рейтинг/сводка → описание → адрес/контакты → extended info → отзывы)

Ссылка на структуру RN карточки: [UNIFIED_ATTRACTION_CARD_PLAN.md](../../../AdygGIS-RN/Docs/Extra/UNIFIED_ATTRACTION_CARD_PLAN.md)

---

## 🏷️ Сравнение CategoryChip

### Kotlin версия

```kotlin
// CategoryChip.kt
@Composable
fun CategoryChip(
    category: AttractionCategory,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(android.graphics.Color.parseColor(category.colorHex)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(category.emoji, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(category.displayName, color = Color.White, fontSize = 12.sp)
        }
    }
}
```

### RN версия

```typescript
// CategoryChip.tsx
export const CategoryChip: FC<CategoryChipProps> = ({
  category,
  size = 'medium',
  showEmoji = true,
  showLabel = true,
}) => {
  const categoryInfo = CATEGORIES[category];
  
  return (
    <View style={[styles.container, { backgroundColor: categoryInfo.colorHex }]}>
      {showEmoji && <Text style={styles.emoji}>{categoryInfo.emoji}</Text>}
      {showLabel && <Text style={styles.label}>{categoryInfo.displayNameRu}</Text>}
    </View>
  );
};
```

### Унификация

RN версия более гибкая (showEmoji, showLabel, size). Рекомендация:
- **Kotlin** — добавить аналогичные параметры

```kotlin
// CategoryChip.kt - ОБНОВИТЬ
@Composable
fun CategoryChip(
    category: AttractionCategory,
    modifier: Modifier = Modifier,
    size: ChipSize = ChipSize.MEDIUM,
    showEmoji: Boolean = true,
    showLabel: Boolean = true
) {
    val (textSize, emojiSize, padding) = when (size) {
        ChipSize.SMALL -> Triple(10.sp, 12.sp, 4.dp)
        ChipSize.MEDIUM -> Triple(12.sp, 14.sp, 6.dp)
        ChipSize.LARGE -> Triple(14.sp, 16.sp, 8.dp)
    }
    
    Surface(
        color = Color(android.graphics.Color.parseColor(category.colorHex)),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = padding, vertical = padding / 2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showEmoji) {
                Text(category.emoji, fontSize = emojiSize)
                if (showLabel) Spacer(modifier = Modifier.width(4.dp))
            }
            if (showLabel) {
                Text(
                    text = category.displayName,
                    color = Color.White,
                    fontSize = textSize,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

enum class ChipSize { SMALL, MEDIUM, LARGE }
```

---

## 📝 Расширенные поля UI

### Новые поля для Detail Screen (Kotlin)

В Kotlin версии нужно отобразить расширенные поля в `AttractionDetailScreen`:

```kotlin
// AttractionDetailScreen.kt - ДОБАВИТЬ секцию

@Composable
private fun ExtendedInfoSection(attraction: Attraction) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        SectionHeader(stringResource(R.string.additional_info))
        
        // Operating Season
        attraction.operatingSeason?.let { season ->
            InfoRow(
                icon = Icons.Default.CalendarMonth,
                label = stringResource(R.string.operating_season),
                value = season
            )
        }
        
        // Duration
        attraction.duration?.let { duration ->
            InfoRow(
                icon = Icons.Default.Timer,
                label = stringResource(R.string.duration),
                value = duration
            )
        }
        
        // Best Time to Visit
        attraction.bestTimeToVisit?.let { bestTime ->
            InfoRow(
                icon = Icons.Default.WbSunny,
                label = stringResource(R.string.best_time_to_visit),
                value = bestTime
            )
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
```

### Strings для новых полей

```xml
<!-- values/strings.xml - ДОБАВИТЬ -->
<string name="additional_info">Дополнительная информация</string>
<string name="operating_season">Сезон работы</string>
<string name="duration">Продолжительность</string>
<string name="best_time_to_visit">Лучшее время для посещения</string>

<!-- values-en/strings.xml - ДОБАВИТЬ -->
<string name="additional_info">Additional Information</string>
<string name="operating_season">Operating Season</string>
<string name="duration">Duration</string>
<string name="best_time_to_visit">Best Time to Visit</string>
```

---

## 🎨 Унификация цветов категорий

### Убедиться что цвета идентичны

**Kotlin (`AttractionCategory.kt`):**
```kotlin
enum class AttractionCategory(val displayName: String, val colorHex: String, val emoji: String) {
    NATURE("Природа", "#4CAF50", "🌲"),
    CULTURE("Культура", "#9C27B0", "🎭"),
    HISTORY("История", "#795548", "🏛️"),
    ADVENTURE("Приключения", "#FF5722", "🏔️"),
    RECREATION("Отдых", "#03A9F4", "🏖️"),
    GASTRONOMY("Гастрономия", "#FF9800", "🍽️"),
    RELIGIOUS("Религиозные места", "#607D8B", "⛪"),
    ENTERTAINMENT("Развлечения", "#E91E63", "🎪")
}
```

**RN (`category.ts`):**
```typescript
export const CATEGORIES: Record<AttractionCategory, CategoryInfo> = {
  NATURE: { colorHex: '#4CAF50', emoji: '🌲', ... },
  CULTURE: { colorHex: '#9C27B0', emoji: '🎭', ... },
  HISTORY: { colorHex: '#795548', emoji: '🏛️', ... },
  ADVENTURE: { colorHex: '#FF5722', emoji: '🏔️', ... },
  RECREATION: { colorHex: '#03A9F4', emoji: '🏖️', ... },
  GASTRONOMY: { colorHex: '#FF9800', emoji: '🍽️', ... },
  RELIGIOUS: { colorHex: '#607D8B', emoji: '⛪', ... },
  ENTERTAINMENT: { colorHex: '#E91E63', emoji: '🎪', ... },
};
```

✅ **Цвета и emoji идентичны** — унификация не требуется.

---

## 🖼️ Унификация CompactAttractionCard

### Kotlin версия

```kotlin
// CompactAttractionCard.kt - для SearchResultsPanel
@Composable
fun CompactAttractionCard(
    attraction: Attraction,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Image thumbnail
        AsyncImage(
            model = attraction.images.firstOrNull(),
            contentDescription = null,
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = attraction.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryChip(
                    category = attraction.category,
                    size = ChipSize.SMALL,
                    showLabel = false
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                attraction.rating?.let { rating ->
                    RatingBar(rating = rating, size = RatingSize.SMALL)
                }
            }
        }
        
        // Favorite button
        IconButton(onClick = onFavoriteClick) {
            Icon(
                imageVector = if (attraction.isFavorite) 
                    Icons.Filled.Favorite 
                else 
                    Icons.Outlined.FavoriteBorder,
                contentDescription = null,
                tint = if (attraction.isFavorite) 
                    MaterialTheme.colorScheme.error 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

### RN версия

```typescript
// CompactAttractionCard.tsx
export const CompactAttractionCard: FC<CompactAttractionCardProps> = ({
  attraction,
  onPress,
  onFavoritePress,
}) => {
  return (
    <Pressable style={styles.container} onPress={() => onPress?.(attraction)}>
      <Image source={{ uri: attraction.images[0] }} style={styles.thumbnail} />
      
      <View style={styles.content}>
        <Text style={styles.title} numberOfLines={1}>{attraction.name}</Text>
        
        <View style={styles.metaRow}>
          <CategoryChip category={attraction.category} size="small" showLabel={false} />
          {attraction.rating && <RatingBar rating={attraction.rating} size="small" />}
        </View>
      </View>
      
      <Pressable onPress={() => onFavoritePress?.(attraction)}>
        <Ionicons 
          name={attraction.isFavorite ? 'heart' : 'heart-outline'}
          size={24}
          color={attraction.isFavorite ? colors.error : colors.onSurfaceVariant}
        />
      </Pressable>
    </Pressable>
  );
};
```

✅ **Структура идентична** — компоненты унифицированы.

---

## 📝 Reviews (Kotlin ↔ RN)

Цель: Kotlin `AttractionDetailScreen` должен показывать **тот же блок отзывов**, что и RN (и считать те же агрегаты), при этом источником истины остаётся Supabase.

**Source of truth (backend модуль):**
- [AdygGIS-RN/Docs/Extra/Reviews/08_REVIEWS_AND_AUTH_MODULE.md](../../../AdygGIS-RN/Docs/Extra/Reviews/08_REVIEWS_AND_AUTH_MODULE.md)

### Данные (что должно совпадать на 100%)

- **Сводка по рейтингу** берётся из `attractions.average_rating` и `attractions.reviews_count`.
- **Лента отзывов** берётся из `reviews` (только `status='approved'`).

Важно: поле `attractions.rating` можно использовать только как fallback/историческое (если `average_rating` ещё null и отзывов нет).

### UX-правило по авторизации

- Смотреть отзывы можно без аккаунта.
- При попытке написать отзыв: если пользователь не авторизован → показать Auth (login/register), затем открыть форму отзыва.

### Kotlin: что добавить в Detail Screen

Минимальный состав секции:
- Header: "Отзывы"
- Summary row: средний рейтинг (1 знак после запятой) + счётчик отзывов
- List: несколько последних approved отзывов (display_name + дата + текст + ⭐)
- CTA: "Оставить отзыв" (гейтится авторизацией)

### Kotlin: данные/слои

- `ReviewRepository`:
    - `getApprovedReviews(attractionId)` → select `reviews` where attraction_id + status=approved order by created_at desc
    - `submitReview(attractionId, rating, title?, body)` → insert в `reviews` (получится `status='pending'`)
- `AuthRepository`:
    - `getSession()` / `isAuthed`
    - `signIn(email,password)` / `signUp(email,password)`

### Kotlin: важные детали для совместимости с RN

- Использовать **UUID attraction_id** из Supabase как ключ (не name и не локальный int).
- Не хранить favorite в Supabase (как и раньше) — local-only.
- Для офлайна: можно кэшировать approved отзывы в Room (опционально). Для MVP допустимо грузить отзывы только онлайн.

## ✅ Чеклист изменений

### Kotlin изменения

| Файл | Изменение | Приоритет |
|------|-----------|-----------|
| `CategoryChip.kt` | Добавить size, showEmoji, showLabel | 🟡 Medium |
| `AttractionDetailScreen.kt` | Добавить ExtendedInfoSection | 🟡 Medium |
| `strings.xml` | Добавить строки для extended fields | 🟡 Medium |
| `domain/model/Attraction.kt` | Добавить extended fields | 🔴 High |

### RN изменения

| Файл | Изменение | Приоритет |
|------|-----------|-----------|
| `AttractionBottomSheet.tsx` | Добавить секцию Extended Info | 🟡 Medium |

### Общие требования

| Требование | Kotlin | RN |
|------------|--------|-----|
| Одинаковые цвета категорий | ✅ | ✅ |
| Одинаковые emoji | ✅ | ✅ |
| Extended fields в Detail | 📋 TODO | 📋 TODO |
| CategoryChip параметры | 📋 TODO | ✅ |

---

## 📋 Следующий шаг

После унификации UI → [06_REFACTORING_CHECKLIST.md](06_REFACTORING_CHECKLIST.md) — Финальный чеклист

