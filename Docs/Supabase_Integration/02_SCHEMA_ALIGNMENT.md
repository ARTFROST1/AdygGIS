# 📊 Выравнивание схемы данных: Kotlin ↔ Supabase ↔ RN

**Дата:** 5 января 2026  
**Версия:** 1.0  
**Статус:** План рефакторинга

---

## 📋 Содержание

1. [Текущие модели данных](#текущие-модели-данных)
2. [Целевая схема Supabase](#целевая-схема-supabase)
3. [Маппинг полей](#маппинг-полей)
4. [Требуемые изменения в Kotlin](#требуемые-изменения-в-kotlin)
5. [Чеклист изменений](#чеклист-изменений)

---

## 🗂️ Текущие модели данных

### Kotlin: AttractionDto (текущая)

```kotlin
// data/remote/dto/AttractionDto.kt
@Serializable
data class AttractionDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("description") val description: String,
    @SerialName("category") val category: String,
    @SerialName("latitude") val latitude: Double,
    @SerialName("longitude") val longitude: Double,
    @SerialName("address") val address: String? = null,
    @SerialName("directions") val directions: String? = null,
    @SerialName("images") val images: List<String> = emptyList(),
    @SerialName("rating") val rating: Float? = null,
    @SerialName("workingHours") val workingHours: String? = null,
    @SerialName("phoneNumber") val phoneNumber: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("website") val website: String? = null,
    @SerialName("isFavorite") val isFavorite: Boolean = false,
    @SerialName("tags") val tags: List<String> = emptyList(),
    @SerialName("priceInfo") val priceInfo: String? = null,
    @SerialName("amenities") val amenities: List<String> = emptyList()
)
```

### RN: RawAttractionData (текущая)

```typescript
// src/types/attraction.ts
interface RawAttractionData {
  id: string;
  name: string;
  description: string;
  category: string;
  latitude: number;
  longitude: number;
  address?: string;
  directions?: string;
  images: string[];
  rating?: number;
  workingHours?: string;
  phoneNumber?: string;
  email?: string;
  website?: string | null;
  isFavorite: boolean;
  tags: string[];
  priceInfo?: string;
  amenities: string[];
  // Дополнительные поля в RN
  operatingSeason?: string;
  duration?: string;
  difficulty?: string;
  bestTimeToVisit?: string;
}
```

---

## 🎯 Целевая схема Supabase

```sql
-- Supabase PostgreSQL Schema
CREATE TABLE attractions (
  -- Primary key (UUID вместо string)
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  
  -- Basic info (ОБЩИЕ для Kotlin и RN)
  name TEXT NOT NULL,
  description TEXT NOT NULL,
  category TEXT NOT NULL CHECK (category IN (
    'NATURE', 'CULTURE', 'HISTORY', 'ADVENTURE',
    'RECREATION', 'GASTRONOMY', 'RELIGIOUS', 'ENTERTAINMENT'
  )),
  
  -- Location
  latitude DOUBLE PRECISION NOT NULL,
  longitude DOUBLE PRECISION NOT NULL,
  address TEXT,
  directions TEXT,
  
  -- Media
  images TEXT[] DEFAULT '{}',
  
  -- Details
  rating DECIMAL(2,1) CHECK (rating >= 0 AND rating <= 5),
    -- Reviews aggregate (нужно, если включаем UI отзывов)
    reviews_count INTEGER DEFAULT 0,
    average_rating DECIMAL(2,1) CHECK (average_rating >= 0 AND average_rating <= 5),
  working_hours TEXT,
  phone_number TEXT,
  email TEXT,
  website TEXT,
  tags TEXT[] DEFAULT '{}',
  price_info TEXT,
  amenities TEXT[] DEFAULT '{}',
  
  -- Extended info (из RN, добавить в Kotlin)
  operating_season TEXT,
  duration TEXT,
  difficulty TEXT,
  best_time_to_visit TEXT,
  
  -- Metadata
  is_published BOOLEAN DEFAULT true,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ВАЖНО: isFavorite НЕ хранится в Supabase!
-- Это локальное состояние каждого устройства

-- Profiles (расширение auth.users)
CREATE TABLE profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    display_name TEXT,
    avatar_url TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Reviews table (пользовательские отзывы с модерацией)
CREATE TABLE reviews (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    attraction_id UUID NOT NULL REFERENCES attractions(id) ON DELETE CASCADE,
    user_id UUID NOT NULL DEFAULT auth.uid() REFERENCES auth.users(id) ON DELETE CASCADE,
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

CREATE UNIQUE INDEX ux_reviews_attraction_user ON reviews(attraction_id, user_id);
```

---

## 🔄 Маппинг полей

### Supabase → Kotlin (snake_case → camelCase)

| Supabase (snake_case) | Kotlin (camelCase) | Тип | Изменения в Kotlin |
|----------------------|-------------------|-----|-------------------|
| id | id | UUID → String | Парсить как String |
| name | name | TEXT → String | ✅ Без изменений |
| description | description | TEXT → String | ✅ Без изменений |
| category | category | TEXT → String | ✅ Без изменений |
| latitude | latitude | DOUBLE → Double | ✅ Без изменений |
| longitude | longitude | DOUBLE → Double | ✅ Без изменений |
| address | address | TEXT → String? | ✅ Без изменений |
| directions | directions | TEXT → String? | ✅ Без изменений |
| images | images | TEXT[] → List<String> | ✅ Без изменений |
| rating | rating | DECIMAL → Float? | ✅ Без изменений |
| **reviews_count** | reviewsCount | INT → Int? | 🆕 Добавить (для UI отзывов) |
| **average_rating** | averageRating | DECIMAL → Float? | 🆕 Добавить (для UI отзывов) |
| **working_hours** | workingHours | TEXT → String? | ⚠️ @SerialName |
| **phone_number** | phoneNumber | TEXT → String? | ⚠️ @SerialName |
| email | email | TEXT → String? | ✅ Без изменений |
| website | website | TEXT → String? | ✅ Без изменений |
| tags | tags | TEXT[] → List<String> | ✅ Без изменений |
| **price_info** | priceInfo | TEXT → String? | ⚠️ @SerialName |
| amenities | amenities | TEXT[] → List<String> | ✅ Без изменений |
| **operating_season** | operatingSeason | TEXT → String? | 🆕 Добавить |
| duration | duration | TEXT → String? | 🆕 Добавить |
| difficulty | difficulty | TEXT → String? | 🆕 Добавить |
| **best_time_to_visit** | bestTimeToVisit | TEXT → String? | 🆕 Добавить |
| **is_published** | isPublished | BOOLEAN → Boolean | 🆕 Добавить |
| **created_at** | createdAt | TIMESTAMPTZ → String | 🆕 Добавить |
| **updated_at** | updatedAt | TIMESTAMPTZ → String | 🆕 Добавить |

---

## 🛠️ Требуемые изменения в Kotlin

### 1. Обновить AttractionDto

```kotlin
// data/remote/dto/AttractionDto.kt - НОВАЯ ВЕРСИЯ
@Serializable
data class AttractionDto(
    @SerialName("id")
    val id: String,
    
    @SerialName("name")
    val name: String,
    
    @SerialName("description")
    val description: String,
    
    @SerialName("category")
    val category: String,
    
    @SerialName("latitude")
    val latitude: Double,
    
    @SerialName("longitude")
    val longitude: Double,
    
    @SerialName("address")
    val address: String? = null,
    
    @SerialName("directions")
    val directions: String? = null,
    
    @SerialName("images")
    val images: List<String> = emptyList(),
    
    @SerialName("rating")
    val rating: Float? = null,
    
    // ⚠️ ИЗМЕНИТЬ: working_hours вместо workingHours
    @SerialName("working_hours")
    val workingHours: String? = null,
    
    // ⚠️ ИЗМЕНИТЬ: phone_number вместо phoneNumber
    @SerialName("phone_number")
    val phoneNumber: String? = null,
    
    @SerialName("email")
    val email: String? = null,
    
    @SerialName("website")
    val website: String? = null,
    
    @SerialName("tags")
    val tags: List<String> = emptyList(),
    
    // ⚠️ ИЗМЕНИТЬ: price_info вместо priceInfo
    @SerialName("price_info")
    val priceInfo: String? = null,
    
    @SerialName("amenities")
    val amenities: List<String> = emptyList(),
    
    // 🆕 ДОБАВИТЬ: Расширенные поля из RN
    @SerialName("operating_season")
    val operatingSeason: String? = null,
    
    @SerialName("duration")
    val duration: String? = null,
    
    @SerialName("difficulty")
    val difficulty: String? = null,
    
    @SerialName("best_time_to_visit")
    val bestTimeToVisit: String? = null,
    
    // 🆕 ДОБАВИТЬ: Метаданные Supabase
    @SerialName("is_published")
    val isPublished: Boolean = true,
    
    @SerialName("created_at")
    val createdAt: String? = null,
    
    @SerialName("updated_at")
    val updatedAt: String? = null
)

// УДАЛИТЬ isFavorite из DTO!
// Это локальное состояние, хранится в Room
```

### 2. Обновить AttractionEntity (Room)

```kotlin
// data/local/entities/AttractionEntity.kt - ОБНОВИТЬ
@Entity(tableName = "attractions")
@TypeConverters(Converters::class)
data class AttractionEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val address: String?,
    val directions: String?,
    val images: List<String>,
    val rating: Float?,
    val workingHours: String?,
    val phoneNumber: String?,
    val email: String?,
    val website: String?,
    val tags: List<String>,
    val priceInfo: String?,
    val amenities: List<String>,
    
    // 🆕 ДОБАВИТЬ расширенные поля
    val operatingSeason: String?,
    val duration: String?,
    val difficulty: String?,
    val bestTimeToVisit: String?,
    
    // 🆕 ДОБАВИТЬ метаданные синхронизации
    val isPublished: Boolean = true,
    val createdAt: String?,
    val updatedAt: String?,
    
    // Локальные поля (НЕ синхронизируются)
    val isFavorite: Boolean = false,
    val lastSyncedAt: Long = System.currentTimeMillis()
)
```

### 3. Обновить Domain Model

```kotlin
// domain/model/Attraction.kt - ОБНОВИТЬ
data class Attraction(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val category: AttractionCategory,
    val location: Location,
    val images: List<String>,
    val rating: Float? = null,
    val workingHours: String? = null,
    val contactInfo: ContactInfo? = null,
    val tags: List<String> = emptyList(),
    val priceInfo: String? = null,
    val amenities: List<String> = emptyList(),
    
    // 🆕 ДОБАВИТЬ
    val operatingSeason: String? = null,
    val duration: String? = null,
    val difficulty: String? = null,
    val bestTimeToVisit: String? = null,
    
    // Локальные поля
    val isFavorite: Boolean = false
)
```

### 4. Обновить Mapper

```kotlin
// data/mapper/AttractionMapper.kt - ОБНОВИТЬ
object AttractionMapper {
    
    fun AttractionDto.toEntity(): AttractionEntity {
        return AttractionEntity(
            id = id,
            name = name,
            description = description,
            category = category,
            latitude = latitude,
            longitude = longitude,
            address = address,
            directions = directions,
            images = images,
            rating = rating,
            workingHours = workingHours,
            phoneNumber = phoneNumber,
            email = email,
            website = website,
            tags = tags,
            priceInfo = priceInfo,
            amenities = amenities,
            // 🆕 Новые поля
            operatingSeason = operatingSeason,
            duration = duration,
            difficulty = difficulty,
            bestTimeToVisit = bestTimeToVisit,
            isPublished = isPublished,
            createdAt = createdAt,
            updatedAt = updatedAt,
            // Локальные
            isFavorite = false,
            lastSyncedAt = System.currentTimeMillis()
        )
    }
    
    // ... остальные mappers
}
```

---

## ✅ Чеклист изменений

### Файлы для изменения

| Файл | Изменение | Приоритет |
|------|-----------|-----------|
| `data/remote/dto/AttractionDto.kt` | Обновить @SerialName, добавить поля | 🔴 High |
| `data/local/entities/AttractionEntity.kt` | Добавить новые поля | 🔴 High |
| `domain/model/Attraction.kt` | Добавить extended поля | 🔴 High |
| `data/mapper/AttractionMapper.kt` | Обновить маппинг | 🔴 High |
| `data/local/dao/AttractionDao.kt` | Добавить query для updatedAt | 🟡 Medium |
| Room Migration | Создать миграцию схемы | 🔴 High |

### Room Migration

```kotlin
// data/local/database/Migrations.kt
val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Добавить новые колонки
        database.execSQL("ALTER TABLE attractions ADD COLUMN operatingSeason TEXT")
        database.execSQL("ALTER TABLE attractions ADD COLUMN duration TEXT")
        database.execSQL("ALTER TABLE attractions ADD COLUMN difficulty TEXT")
        database.execSQL("ALTER TABLE attractions ADD COLUMN bestTimeToVisit TEXT")
        database.execSQL("ALTER TABLE attractions ADD COLUMN isPublished INTEGER NOT NULL DEFAULT 1")
        database.execSQL("ALTER TABLE attractions ADD COLUMN createdAt TEXT")
        database.execSQL("ALTER TABLE attractions ADD COLUMN updatedAt TEXT")
        database.execSQL("ALTER TABLE attractions ADD COLUMN lastSyncedAt INTEGER NOT NULL DEFAULT 0")
    }
}
```

---

## 📋 Следующий шаг

После выравнивания схемы → [03_RETROFIT_SUPABASE.md](03_RETROFIT_SUPABASE.md) — Интеграция Retrofit + Supabase API

