# Этап 5: Оптимизация ProGuard правил

## ❌ УДАЛИТЬ из app/proguard-rules.pro:

### Gson правила (строки 132-140) - Gson НЕ ИСПОЛЬЗУЕТСЯ
```proguard
# УДАЛИТЬ ВСЁ:
# Gson (if used)
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
```

**Причина**: Используется kotlinx.serialization, а не Gson!

---

## ➕ ДОБАВИТЬ агрессивные оптимизации:

### В начало файла (после комментариев):
```proguard
# Агрессивная оптимизация кода
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-repackageclasses ''

# Удалить лишние атрибуты
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
```

---

## ⚠️ УПРОСТИТЬ Compose правила:

### Текущие (слишком широкие):
```proguard
# Было:
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.material3.** { *; }
```

### Заменить на более точные:
```proguard
# Стало (R8 сам знает о Compose):
-keep,allowobfuscation,allowshrinking class androidx.compose.runtime.** { *; }
-keep,allowobfuscation,allowshrinking class androidx.compose.ui.** { *; }

# Сохранить только Composable функции
-keep @androidx.compose.runtime.Composable class * { *; }
-keep class * extends androidx.compose.runtime.CompositionLocal { *; }
```

---

## 💰 Ожидаемая экономия: ~1-2 MB

### Почему это работает:
1. **Удаление Gson правил** - позволяет R8 удалить неиспользуемый код
2. **Агрессивные оптимизации** - больше проходов оптимизации
3. **Repackage** - объединяет классы в один пакет
4. **Удаление логов** - убирает debug логи из release
5. **Упрощенные Compose правила** - позволяет R8 оптимизировать

---

## 📝 Порядок действий:

1. Открыть `app/proguard-rules.pro`
2. Удалить строки 132-140 (Gson правила)
3. Добавить агрессивные оптимизации в начало
4. Заменить широкие Compose правила на точные
5. Сохранить файл
6. Пересобрать release APK
7. Проверить работоспособность
