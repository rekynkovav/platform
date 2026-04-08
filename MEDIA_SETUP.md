# Инструкция по настройке защищенного медиа контента

## Что реализовано

### 1. Базовая инфраструктура
- ✅ MediaFile entity - хранение метаданных файлов
- ✅ AccessLog entity - аудит всех обращений к файлам
- ✅ Repositories для работы с БД
- ✅ Liquibase миграции для новых таблиц

### 2. Сервисы
- ✅ **EncryptionService** - AES-256 шифрование/расшифровка
- ✅ **S3StorageService** - работа с Яндекс Cloud Storage
- ✅ **SecureMediaService** - основная логика защиты контента

### 3. API endpoints
- `POST /api/media/upload` - загрузка с шифрованием
- `GET /api/media/stream/{fileId}` - получить presigned URL
- `GET /api/media/key/{fileId}` - получить ключ для HLS
- `GET /api/media/download/{fileId}` - скачать и расшифровать
- `GET /api/media/{fileId}` - информация о файле
- `GET /api/media/my-files` - файлы пользователя
- `DELETE /api/media/{fileId}` - удалить файл

### 4. Security
- ✅ Keycloak интеграция настроена
- ✅ Роли: USER, PREMIUM, CREATOR, ADMIN, MODERATOR
- ✅ Method-level security на всех endpoints
- ✅ Проверка прав доступа к файлам

### 5. Зависимости добавлены
- Keycloak Spring Boot Starter
- AWS S3 SDK (для Яндекс S3)
- Redis для кеша
- Apache Commons IO

## Настройка

### 1. Переменные окружения

```bash
# PostgreSQL
DB_URL=jdbc:postgresql://localhost:5432/platform_db
DB_USER=platform_user
DB_PASSWORD=your_password

# Keycloak
KEYCLOAK_URL=http://localhost:8080
KEYCLOAK_REALM=platform-realm
KEYCLOAK_CLIENT_SECRET=your-client-secret

# Яндекс Cloud Storage
YC_ACCESS_KEY=your-access-key
YC_SECRET_KEY=your-secret-key
YC_BUCKET=platform-media
YC_ENDPOINT=https://storage.yandexcloud.net
YC_REGION=ru-central1

# Шифрование
MEDIA_MASTER_KEY=change-this-to-strong-random-key-in-production

# Redis (опционально)
REDIS_HOST=localhost
REDIS_PORT=6379
```

### 2. Настройка Keycloak

**Запуск через Docker:**
```bash
docker run -d \
  --name keycloak \
  -p 8080:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:24.0.3 \
  start-dev
```

**Настройка:**
1. Открыть http://localhost:8080
2. Войти как admin/admin
3. Создать Realm: `platform-realm`
4. Создать Client: `platform-backend`
   - Client Protocol: openid-connect
   - Access Type: confidential
   - Valid Redirect URIs: `http://localhost:3000/*`
   - Web Origins: `http://localhost:3000`
5. Создать роли в Realm Roles:
   - USER
   - PREMIUM
   - CREATOR
   - ADMIN
   - MODERATOR
6. Создать пользователей и назначить роли

### 3. Настройка Яндекс Cloud Storage

1. Создать бакет в Яндекс Object Storage
2. Получить access key и secret key
3. Настроить CORS для бакета (если нужен доступ из браузера)

### 4. Запуск миграций

```bash
# Включить Liquibase в application.yaml
spring.liquibase.enabled: true

# Запустить приложение - миграции применятся автоматически
mvn spring-boot:run
```

## Как работает защита

### Загрузка файла
1. Пользователь загружает файл через `/api/media/upload`
2. Генерируется уникальный ключ шифрования (masterKey + userId + fileId)
3. Файл шифруется AES-256
4. Зашифрованный файл загружается в Яндекс S3
5. Метаданные сохраняются в PostgreSQL

### Просмотр файла
1. Пользователь запрашивает `/api/media/stream/{fileId}`
2. Проверяются права доступа (владелец/premium/admin)
3. Генерируется presigned URL на 5 минут
4. Логируется обращение в access_logs
5. URL возвращается клиенту

### HLS стриминг (для будущей реализации)
1. Видео конвертируется в HLS сегменты
2. Каждый сегмент шифруется отдельным ключом
3. Клиент запрашивает ключи через `/api/media/key/{fileId}`
4. Все запросы логируются для отслеживания утечек

## Что еще нужно добавить

### 1. HLS стриминг (FFmpeg)
- Конвертация видео в HLS формат
- Шифрование сегментов
- Генерация .m3u8 плейлистов

### 2. Водяные знаки
- Видимые: наложение userId + timestamp
- Невидимые: forensic watermarking

### 3. Redis кеш
- Кеширование presigned URLs
- Кеширование ключей шифрования
- Rate limiting

### 4. Мониторинг
- Алерты при подозрительной активности
- Дашборд с метриками доступа
- Отчеты по утечкам

## Frontend интеграция

```javascript
// Keycloak инициализация
import Keycloak from 'keycloak-js';

const keycloak = new Keycloak({
  url: 'http://localhost:8080',
  realm: 'platform-realm',
  clientId: 'platform-frontend'
});

keycloak.init({ onLoad: 'login-required' }).then(authenticated => {
  if (authenticated) {
    // Загрузка файла
    const formData = new FormData();
    formData.append('file', file);
    formData.append('type', 'video');

    fetch('/api/media/upload', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${keycloak.token}`
      },
      body: formData
    });

    // Получение URL для просмотра
    fetch(`/api/media/stream/${fileId}`, {
      headers: {
        'Authorization': `Bearer ${keycloak.token}`
      }
    })
    .then(res => res.json())
    .then(data => {
      // data.url - presigned URL на 5 минут
      videoElement.src = data.url;
    });
  }
});
```

## Безопасность

### Что защищено
✅ Файлы шифруются AES-256  
✅ Ключи генерируются уникально для каждого файла  
✅ Presigned URLs живут 5 минут  
✅ Все обращения логируются  
✅ Проверка прав доступа на каждый запрос  
✅ JWT токены с коротким TTL  

### Что НЕ защищено от
❌ Screen recording (запись экрана)  
❌ Камера, снимающая экран  
❌ Инсайдеры с легитимным доступом  

### Рекомендации
- Использовать HTTPS везде
- Ротация MEDIA_MASTER_KEY раз в квартал
- Мониторинг access_logs на аномалии
- Rate limiting на API endpoints
- Для критичного контента - DRM (платно)

## Тестирование

```bash
# Загрузка файла
curl -X POST http://localhost:8081/api/media/upload \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@video.mp4" \
  -F "type=video"

# Получение URL
curl http://localhost:8081/api/media/stream/FILE_ID \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Просмотр логов доступа
SELECT * FROM media_access_logs 
WHERE file_id = 'FILE_ID' 
ORDER BY accessed_at DESC;
```
