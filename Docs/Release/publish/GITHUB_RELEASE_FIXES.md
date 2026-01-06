# 🔧 Исправление ошибок GitHub Release

## ❌ Проблемы которые были:

### 1. **Pattern does not match any files**
```
🤔 Pattern 'app/build/outputs/apk/full/release/app-full-release.apk' does not match any files.
```

**Причина:** Неправильные жестко заданные пути к файлам APK и AAB.

### 2. **GitHub release failed with status: 403**
```
⚠️ GitHub release failed with status: 403
❌ Too many retries. Aborting...
```

**Причина:** Недостаточные права доступа для создания релизов.

---

## ✅ Решения применены:

### 1. **Динамическое определение путей к файлам**

Добавлен шаг `🔍 Find build files`:
```yaml
- name: 🔍 Find build files
  id: find_files
  run: |
    APK_FILE=$(find app/build/outputs/apk -name "*.apk" -type f | head -1)
    AAB_FILE=$(find app/build/outputs/bundle -name "*.aab" -type f | head -1)
    
    echo "Found APK: $APK_FILE"
    echo "Found AAB: $AAB_FILE"
    
    echo "apk_file=$APK_FILE" >> $GITHUB_OUTPUT
    echo "aab_file=$AAB_FILE" >> $GITHUB_OUTPUT
```

**Преимущества:**
- ✅ Автоматически находит файлы независимо от точного пути
- ✅ Проверяет существование файлов перед использованием
- ✅ Выводит найденные пути для отладки
- ✅ Прерывает процесс если файлы не найдены

### 2. **Исправлены права доступа**

Добавлены необходимые разрешения:
```yaml
jobs:
  build-release:
    permissions:
      contents: write  # Required for creating releases
      actions: read
```

**Что это дает:**
- ✅ Разрешение на создание GitHub Releases
- ✅ Разрешение на загрузку файлов в релиз
- ✅ Доступ к чтению метаданных Actions

### 3. **Добавлена диагностика сборки**

Новый шаг `🔍 List build outputs (debug)`:
```yaml
- name: 🔍 List build outputs (debug)
  run: |
    echo "=== APK outputs ==="
    find app/build/outputs/apk -name "*.apk" -type f || echo "No APK files found"
    echo "=== AAB outputs ==="
    find app/build/outputs/bundle -name "*.aab" -type f || echo "No AAB files found"
```

**Для чего:**
- 🔍 Показывает все созданные файлы
- 🐛 Помогает диагностировать проблемы с путями
- 📊 Дает полную картину результатов сборки

### 4. **Обновлены пути в Upload Artifacts**

Теперь используются динамические пути:
```yaml
- name: 📊 Upload APK artifact
  with:
    path: ${{ steps.find_files.outputs.apk_file }}
    
- name: 📱 Upload AAB artifact
  with:
    path: ${{ steps.find_files.outputs.aab_file }}
```

---

## 🔄 Новый процесс создания релиза:

### 1. **Сборка файлов:**
```
📦 Build Release APK  → ./gradlew assembleFullRelease
📱 Build Release AAB  → ./gradlew bundleFullRelease
```

### 2. **Диагностика:**
```
🔍 List build outputs → Показывает все созданные файлы
🔍 Find build files   → Находит APK и AAB автоматически
```

### 3. **Создание релиза:**
```
🚀 Create GitHub Release → Использует найденные файлы
📊 Upload APK artifact   → Сохраняет APK как артефакт
📱 Upload AAB artifact   → Сохраняет AAB как артефакт
```

---

## 🎯 Ожидаемый результат:

После исправлений в логах GitHub Actions увидите:

```
=== APK outputs ===
app/build/outputs/apk/full/release/app-full-release.apk

=== AAB outputs ===
app/build/outputs/bundle/fullRelease/app-full-release.aab

Found APK: app/build/outputs/apk/full/release/app-full-release.apk
Found AAB: app/build/outputs/bundle/fullRelease/app-full-release.aab
✅ Both files found successfully

👩‍🏭 Creating new GitHub release for tag v1.0.0...
🎉 Release created successfully!
```

---

## 📋 Что нужно сделать:

### 1. **Коммит исправлений:**
```bash
git add .
git commit -m "🔧 Fix GitHub Release creation issues"
git push origin main
```

### 2. **Создать release ветку:**
```bash
git checkout -b release
git push origin release
```

### 3. **Настроить GitHub Secrets** (если еще не сделано):
- `KEYSTORE_BASE64` - содержимое `keystore-base64.txt`
- `KEYSTORE_PASSWORD` - `A12345`
- `KEY_PASSWORD` - `A12345`
- `KEY_ALIAS` - `adygyes-release`
- `YANDEX_MAPKIT_API_KEY` - ваш API ключ

### 4. **Создать тестовый релиз:**
```bash
git checkout release
git merge main
git tag v1.0.0-test
git push origin v1.0.0-test
```

---

## 🛡️ Дополнительные улучшения:

### **Проверка размера файлов:**
Workflow теперь может показать размеры созданных файлов:
```bash
ls -lh $APK_FILE $AAB_FILE
```

### **Валидация файлов:**
Проверка что файлы действительно являются корректными:
```bash
file $APK_FILE  # Должен показать: Android application package
file $AAB_FILE  # Должен показать: Zip archive data
```

### **Автоматическое именование:**
Файлы автоматически получают правильные имена в релизе:
- `AdygGIS-v1.0.0-APK` - для APK артефакта
- `AdygGIS-v1.0.0-AAB` - для AAB артефакта

**🎉 Теперь создание GitHub Release должно работать без ошибок!**
