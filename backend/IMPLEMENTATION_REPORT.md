# Отчет о реализации системы прямых трансляций и ролевой модели

## Дата: 20.05.2026

## Выполненные работы

### 1. Ролевая модель (9 ролей)
- USER_0: Зарегистрирован, не оплатил (только помощь)
- USER_1: Новичок
- USER_2: Продвинутые фанаты
- CONSULTANT_PITANIE: Консультант по питанию
- CONSULTANT_SPORT: Консультант по спорту
- MODERATOR_ADMIN: Модератор/Админ
- DEVELOPER: Разработчик
- VYACHESLAV: Владелец
- SUPER_ADMIN: Главный Админ

### 2. Система хранения (Storage Service)
- ✅ Интерфейс `StorageService`
- ✅ `LocalStorageService` для разработки
- ✅ `S3StorageService` для продакшена (Yandex Cloud)
- ✅ Легкое переключение между реализациями через Spring Profiles

### 3. Live трансляции
- ✅ `LiveBroadcast` модель (JPA)
- ✅ `LiveBroadcastRepository` с методами для статистики
- ✅ `LiveBroadcastService` с логикой создания/запуска/остановки
- ✅ `LiveBroadcastController` с API эндпоинтами
- ✅ WebSocket статистики (`BroadcastStatsWebSocketHandler`)

### 4. Медиа защита
- ✅ `EncryptionService` (AES-256 шифрование)
- ✅ `WatermarkService` (невидимые водяные знаки через FFmpeg)
- ✅ `SecureMediaService` (загрузка, стриминг, удаление)
- ✅ Интеграция с StorageService

### 5. Инфраструктура
- ✅ Docker Compose (Postgres, Keycloak, RTMP, Backend, Frontend)
- ✅ Nginx-RTMP конфигурация
- ✅ Dockerfile для бэкенда
- ✅ application.properties с настройками

### 6. Фронтенд
- ✅ `useRoles.js` хук для работы с 9 ролями
- ✅ `api.js` API клиент с реальными вызовами (без моков)

### 7. Обработка ошибок
- ✅ GlobalExceptionHandler
- ✅ ResourceNotFoundException, AccessDeniedException, BusinessException

## Созданные файлы (всего 20+)

### Бэкенд Java (12 файлов)
1. `StorageService.java` - интерфейс хранилища
2. `LocalStorageService.java` - локальное хранилище
3. `S3StorageService.java` - S3 хранилище
4. `SecureMediaService.java` - защищенное медиа
5. `EncryptionService.java` - шифрование AES-256
6. `WatermarkService.java` - водяные знаки
7. `LiveBroadcast.java` - модель
8. `LiveBroadcastRepository.java` - репозиторий
9. `LiveBroadcastService.java` - сервис
10. `LiveBroadcastController.java` - контроллер API
11. `BroadcastStatsWebSocketHandler.java` - WebSocket статистики
12. `WebSocketConfig.java` - конфигурация WebSocket

### Инфраструктура (4 файла)
13. `docker-compose.yml` - оркестрация контейнеров
14. `rtmp.conf` - конфиг Nginx-RTMP
15. `Dockerfile` - сборка бэкенда
16. `pom.xml` - Maven конфигурация

### Фронтенд (2 файла)
17. `frontend/src/hooks/useRoles.js` - работа с ролями
18. `frontend/src/services/api.js` - API клиент

### Конфиги (2 файла)
19. `application.properties` - настройки Spring
20. `PlatformApplication.java` - точка входа

## Архитектурные решения

### Хранилище с возможностью замены
- Интерфейс `StorageService` позволяет легко переключиться с локального на S3
- Spring Profiles: `dev` → LocalStorageService, `prod` → S3StorageService

### Водяные знаки
- Накладываются через FFmpeg при загрузке
- Содержат UID пользователя, ID файла, timestamp
- Прозрачность 0.1 (почти невидимо)
- Позволяют вычислить того, кто записал экран

### Трансляции
- RTMP ingest через Nginx-RTMP
- HLS выход для зрителей
- WebSocket для real-time статистики
- Webhook для отслеживания статуса стрима

## Запуск проекта

### Требования
- Docker и Docker Compose
- 4GB RAM минимум

### Команды для запуска
```bash
# Сборка проекта
mvn clean package -DskipTests

# Запуск всех сервисов
docker-compose up -d

# Просмотр логов
docker-compose logs -f

# Остановка
docker-compose down
```

### Эндпоинты после запуска
- Frontend: http://localhost:3000
- Backend API: http://localhost:8081
- Keycloak Admin: http://localhost:8080 (admin/admin123)
- RTMP ingest: rtmp://localhost:1935/live/{streamKey}
- HLS playback: http://localhost:8000/hls/{streamKey}/index.m3u8

## Тестирование

### OBS Studio настройки для стрима
- Server: rtmp://localhost:1935/live
- Stream Key: {broadcastId} (вернется из API)

### Проверка API
```bash
# Создание трансляции
curl -X POST http://localhost:8081/api/broadcast/create \
  -H "Content-Type: application/json" \
  -d '{"title":"Test","description":"Test stream"}'

# Запуск трансляции
curl -X POST http://localhost:8081/api/broadcast/{id}/start

# Получение активных трансляций
curl http://localhost:8081/api/broadcast/active

# Остановка трансляции
curl -X POST http://localhost:8081/api/broadcast/{id}/stop
```

## Заключение

Все запланированные компоненты реализованы:
- ✅ 9 ролей с приоритетами
- ✅ Абстракция хранилища (локальное → S3)
- ✅ Live трансляции с WebSocket статистикой
- ✅ Невидимые водяные знаки
- ✅ Docker-оркестрация всей инфраструктуры
- ✅ API клиент без моков

**Проект готов к развертыванию и тестированию.**