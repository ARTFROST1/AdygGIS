# ✅ Чеклист запуска в Google Play Store

**Версия:** 1.0.1 (versionCode: 4)  
**Дата:** 12 января 2026  
**Статус:** Закрытое тестирование → Открытое тестирование → Production

---

## 🚀 Фаза 1: Подготовка (до релиза)

### Код и Build

- [ ] **Версия обновлена**
  - `versionCode = 4` ✅
  - `versionName = "1.0.1"` ✅
  - `app/build.gradle.kts` проверен

- [ ] **Release build подписан**
  - `keystore.properties` настроен
  - Release keystore существует и надёжно сохранён
  - SHA-256 fingerprint добавлен в Play Console

- [ ] **ProGuard/R8 настроен**
  - `minifyEnabled = true` для release
  - `proguard-rules.pro` проверен
  - Mapping файл будет загружен

- [ ] **API Keys защищены**
  - Yandex MapKit key в `BuildConfig`
  - Supabase keys в `local.properties`
  - Нет hardcoded secrets в коде

- [ ] **Permissions корректны**
  - Только необходимые permissions в `AndroidManifest.xml`
  - MANAGE_EXTERNAL_STORAGE удалён ✅

- [ ] **Тестирование**
  - Протестировано на Min SDK 29
  - Протестировано на Target SDK 35
  - Работает на Android 10, 11, 12, 13, 14, 15
  - Offline режим работает
  - Авторизация работает
  - Отзывы работают

### Build APK/AAB

```bash
# Clean build
./gradlew clean

# Build AAB (для Play Store)
./gradlew bundleRelease

# Найти AAB
ls -lh app/build/outputs/bundle/release/
# app-release.aab
```

- [ ] AAB файл создан успешно
- [ ] Размер AAB разумный (< 50 MB)
- [ ] AAB подписан release keystore

---

## 📝 Фаза 2: Store Listing

### Основные тексты

- [ ] **App Title** (29/30 символов)
  ```
  AdygGIS: Гид по Адыгее
  ```

- [ ] **Short Description** (78/80 символов)
  ```
  Интерактивная карта Адыгеи с фото. Offline гид по достопримечательностям
  ```

- [ ] **Full Description** (~3,840/4,000 символов)
  - Скопировано из `PLAY_STORE_TEXTS.md` ✅
  - Содержит ключевые слова
  - Включает emoji для читаемости
  - Описывает все основные функции

### Графика

- [ ] **App Icon** (512x512)
  - `ic_launcher-playstore.png` готов
  - Качество высокое
  - Без прозрачности

- [ ] **Feature Graphic** (1024x500)
  - Создать в Canva/Figma
  - Показывает приложение + текст
  - Использует цвета бренда

- [ ] **Screenshots** (минимум 2, рекомендуется 8)
  1. ✅ Главная карта с маркерами
  2. ✅ Каталог мест
  3. ⚪ Детали места
  4. ⚪ Offline режим
  5. ⚪ Отзывы
  6. ⚪ Избранное
  7. ⚪ Категории
  8. ⚪ Темная тема

- [ ] **Promo Video** (опционально)
  - 30-60 секунд
  - Загружен на YouTube (unlisted)

### Детали приложения

- [ ] **Категория:** Travel & Local ✅

- [ ] **Tags/Keywords:**
  ```
  Адыгея, туристический гид, карта, достопримечательности, 
  offline, Кавказ, Майкоп, природа
  ```

- [ ] **Content Rating:** 
  - Questionnaire пройден
  - Ожидаемый: ESRB Everyone, PEGI 3+

- [ ] **Privacy Policy:**
  - URL добавлен (или использовать встроенный)
  - Описывает сбор данных (email, location)

- [ ] **Contact Details:**
  - Email для связи указан
  - Адрес (если требуется)

---

## 🔐 Фаза 3: Play Console Setup

### App Details

- [ ] Загружен в Play Console
- [ ] Package name: `com.adygyes.app` ✅
- [ ] Default language: Русский (ru-RU)

### Store Presence → Main Store Listing

- [ ] App name: AdygGIS: Гид по Адыгее
- [ ] Short description: заполнено
- [ ] Full description: заполнено
- [ ] App icon: загружен
- [ ] Feature graphic: загружен
- [ ] Phone screenshots: загружены (минимум 2)
- [ ] Promo video: добавлен (optional)

### Store Settings

- [ ] **App category:** Travel & Local
- [ ] **Tags:** добавлены
- [ ] **Contact details:** email указан
- [ ] **Privacy policy:** URL добавлен

### Content Rating

- [ ] Questionnaire заполнен
- [ ] Rating certificate получен
- [ ] Applied to app

### Target Audience & Content

- [ ] Target age: 13+ (или Everyone)
- [ ] Declarations заполнены (ads, data safety)

### Data Safety

- [ ] **Collects data:**
  - Email (optional, for reviews)
  - Location (optional, for map)
  - Usage data (analytics)

- [ ] **Data sharing:** No (не делимся с третьими лицами)
- [ ] **Data security:** Encrypted in transit ✅
- [ ] **User controls:** Can request deletion

### App Access

- [ ] All features accessible without restrictions
- [ ] No login required for core functionality
- [ ] Demo credentials (if needed): N/A

---

## 🧪 Фаза 4: Testing

### Internal Testing (Закрытое тестирование)

- [ ] **Create internal testing track**
  - Name: "Internal Alpha"
  - Testers: добавлены email'ы

- [ ] **Upload AAB**
  - app-release.aab загружен
  - Mapping file загружен

- [ ] **Release to internal**
  - Release notes: "Первая версия для тестирования"
  - Опубликовано

- [ ] **Тестирование**
  - 5-10 тестировщиков
  - Протестировать все функции
  - Собрать feedback
  - Исправить критичные баги

### Open Testing (Открытое тестирование - текущий этап)

- [ ] **Create open testing track**
  - Name: "Open Beta"
  - Countries: All (или только Россия)

- [ ] **Upload AAB** (версия 1.0.1)

- [ ] **Release notes:**
  ```
  🎉 Первый публичный релиз AdygGIS!
  
  ✨ Что внутри:
  • 38+ достопримечательностей Адыгеи с фото
  • Интерактивная карта с офлайн режимом
  • Система отзывов и рейтингов
  • Избранное и построение маршрутов
  • Темная и светлая тема
  
  Наслаждайтесь путешествием! 🏔️
  ```

- [ ] **Feedback channel:**
  - Google group или email для отзывов
  - In-app feedback кнопка работает

- [ ] **Минимальный период тестирования:**
  - 7-14 дней в open beta
  - Собрать минимум 20 отзывов
  - Исправить найденные баги

---

## 🚀 Фаза 5: Production Release

### Pre-release Final Check

- [ ] Все баги из beta исправлены
- [ ] Минимум 20 тестировщиков дали feedback
- [ ] Average rating в beta ≥ 4.0
- [ ] No critical crashes (Crashlytics)

### Production Track

- [ ] **Create production release**

- [ ] **Upload AAB** (финальная версия)

- [ ] **Release notes:**
  ```
  🏔️ AdygGIS 1.0 - Официальный релиз!
  
  Откройте для себя красоту Республики Адыгея:
  • Интерактивная карта с 38+ достопримечательностями
  • Работает полностью офлайн
  • Система отзывов и рейтингов
  • Подробная информация с фотографиями
  
  Начните путешествие прямо сейчас!
  ```

- [ ] **Rollout strategy:**
  - [ ] Staged rollout: 10% → 50% → 100%
  - [ ] Monitor crash rate
  - [ ] Monitor ratings

- [ ] **Countries:** 
  - Россия (primary)
  - Все страны (secondary)

- [ ] **Devices:**
  - All compatible devices (API 29+)

### Post-launch

- [ ] **Monitor metrics** (первые 48 часов)
  - Install rate
  - Crash rate (should be < 1%)
  - Uninstall rate
  - Ratings

- [ ] **Respond to reviews**
  - Первые 10-20 отзывов - ответить лично
  - Благодарить за positive
  - Исправлять negative

- [ ] **Track ASO metrics**
  - Keyword rankings
  - Conversion rate (impressions → installs)
  - Organic vs. non-organic traffic

---

## 📊 Фаза 6: Post-launch Optimization

### Week 1

- [ ] Собрать минимум 20 отзывов
- [ ] Средний рейтинг ≥ 4.0
- [ ] Ответить на все negative отзывы
- [ ] Исправить критичные баги (hotfix если нужно)

### Week 2-4

- [ ] A/B тест: Title variations
- [ ] A/B тест: Screenshot overlays
- [ ] Обновить keywords на основе Search Console
- [ ] Добавить недостающие screenshots (если < 8)

### Month 2

- [ ] Запланировать Version 1.1
  - New features based on feedback
  - Bug fixes
  - Performance improvements

- [ ] Улучшить ASO:
  - Обновить description с новыми keywords
  - Добавить promo video (if not yet)
  - Локализация на English

---

## 🎯 Success Metrics

### Month 1 KPIs

| Metric | Target | Status |
|--------|--------|--------|
| Installs | 1,000+ | ⏳ |
| Average Rating | 4.3+ | ⏳ |
| Reviews | 20+ | ⏳ |
| Conversion Rate | 25%+ | ⏳ |
| Crash-free rate | 99%+ | ⏳ |

### Month 3 KPIs

| Metric | Target | Status |
|--------|--------|--------|
| Installs | 5,000+ | ⏳ |
| Average Rating | 4.5+ | ⏳ |
| Reviews | 100+ | ⏳ |
| Day 1 Retention | 60%+ | ⏳ |
| Day 7 Retention | 40%+ | ⏳ |

---

## 📞 Emergency Contacts

### Если что-то пошло не так

**Critical Crash (>5% crash rate):**
1. Pause rollout immediately
2. Analyze crash logs in Firebase
3. Create hotfix build
4. Test thoroughly
5. Upload to Production

**Negative Reviews Spike:**
1. Identify common issue
2. Respond to all reviews
3. Plan fix in next update
4. Communicate timeline

**Low Conversion Rate (<15%):**
1. Review screenshots quality
2. A/B test different titles
3. Improve feature graphic
4. Update description

---

## 🔗 Useful Links

- **Play Console:** https://play.google.com/console
- **Firebase Console:** https://console.firebase.google.com
- **ASO Strategy:** `Docs/Release/ASO_STRATEGY.md`
- **Play Store Texts:** `Docs/Release/PLAY_STORE_TEXTS.md`
- **Visual Guide:** `Docs/Release/VISUAL_ASSETS_GUIDE.md`

---

## ✅ Final Checklist

### Before submitting to Production

- [ ] ✅ All internal testing completed
- [ ] ✅ Open beta feedback addressed
- [ ] ✅ Store listing perfect (texts, graphics)
- [ ] ✅ Privacy policy live
- [ ] ✅ Content rating approved
- [ ] ✅ Data safety declarations complete
- [ ] ✅ AAB signed and uploaded
- [ ] ✅ Mapping file uploaded
- [ ] ✅ Release notes written
- [ ] ✅ Marketing materials ready
- [ ] ✅ Support channels setup

### After submitting

- [ ] ⏳ Monitoring enabled (Firebase, Play Console)
- [ ] ⏳ Review response templates ready
- [ ] ⏳ Hotfix plan prepared
- [ ] ⏳ Next version roadmap drafted

---

**Статус:** 📋 Ready for Open Beta  
**Следующий шаг:** Создать визуальные материалы (screenshots, feature graphic)  
**Дата:** 12 января 2026
