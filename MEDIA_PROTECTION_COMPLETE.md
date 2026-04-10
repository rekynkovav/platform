# 🎉 Защищенный медиа контент - ГОТОВО!

## ✅ Что реализовано

### 1. Базовая защита
- ✅ **AES-256 шифрование** всех файлов
- ✅ **Уникальные ключи** для каждого файла (masterKey + userId + fileId)
- ✅ **Presigned URLs** с истечением через 5 минут
- ✅ **Аудит логи** всех обращений к файлам
- ✅ **Проверка прав доступа** на каждый запрос

### 2. Водяные знаки
- ✅ **Видимые водяные знаки** на видео (userId + timestamp + fileId)
- ✅ **Водяные знаки на изображения**
- ✅ **Настраиваемые параметры** (размер, прозрачность, позиция)
- ✅ **Автоматическое наложение** при загрузке

### 3. HLS Streaming
- ✅ **Автоматическая конвертация** видео в HLS
- ✅ **Шифрование сегментов** AES-128
- ✅ **Динамические ключи** для каждого пользователя

### 4. Хранилище
- ✅ **Яндекс Cloud Storage** (S3-совместимый)
- ✅ **Зашифрованное хранение** всех файлов
- ✅ **Метаданные в PostgreSQL**

### 5. Security & Auth
- ✅ **Keycloak интеграция**
- ✅ **Роли**: USER, PREMIUM, CREATOR, ADMIN, MODERATOR, SUPER_ADMIN
- ✅ **JWT токены** с коротким TTL
- ✅ **Method-level security**

### 6. API Endpoints

```
POST   /api/media/upload                    - Загрузка с шифрованием + водяные знаки
GET    /api/media/stream/{fileId}           - Presigned URL (5 мин)
GET    /api/media/key/{fileId}              - Ключ шифрования для HLS
GET    /api/media/download/{fileId}         - Скачать расшифрованный (SUPER_ADMIN)
GET    /api/media/{fileId}                  - Информация о файле
GET    /api/media/my-files                  - Файлы пользователя
GET    /api/media/hls/{fileId}/playlist.m3u8 - HLS плейлист
DELETE /api/media/{fileId}                  - Удалить файл
```

## 📋 Быстрый старт

### 1. Установить FFmpeg

```bash
choco install ffmpeg  # Windows
# или
sudo apt install ffmpeg  # Linux
```

### 2. Настроить переменные окружения

```bash
# Яндекс S3
YC_ACCESS_KEY=your-access-key
YC_SECRET_KEY=your-secret-key
YC_BUCKET=platform-media

# Шифрование
MEDIA_MASTER_KEY=change-this-to-strong-random-key

# Keycloak
KEYCLOAK_URL=http://localhost:8080
KEYCLOAK_CLIENT_SECRET=your-client-secret
```

### 3. Запустить приложение

```bash
mvn clean install
mvn spring-boot:run
```

## 🔒 Уровни защиты

### Уровень 1: Базовая защита (реализовано)
- ✅ AES-256 шифрование
- ✅ Presigned URLs (5 мин)
- ✅ Видимые водяные знаки
- ✅ Аудит логи
- ✅ JWT аутентификация

### Уровень 2: Продвинутая защита (опционально)
- ⚠️ Невидимые водяные знаки (forensic)
- ⚠️ Device fingerprinting
- ⚠️ Rate limiting
- ⚠️ Geo-blocking

### Уровень 3: Максимальная защита (платно)
- ❌ DRM (Widevine, FairPlay, PlayReady)
- ❌ Hardware-backed encryption
- ❌ Облачные anti-piracy сервисы

## ⚠️ Важные замечания

### Безопасность
- **MEDIA_MASTER_KEY** должен быть случайным и длинным (минимум 32 символа)
- Использовать HTTPS везде
- Ротация ключей раз в квартал
- Мониторинг access_logs на аномалии

### Что НЕ защищено
- ❌ Screen recording (запись экрана)
- ❌ Камера, снимающая экран
- ❌ Инсайдеры с легитимным доступом
