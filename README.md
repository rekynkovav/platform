# Платформа - Первый релиз

## Описание
Платформа для общения, обмена медиа и управления контентом с системой ролей и комнатами чата.

## Основные возможности
- **Чат-комнаты**: Общие, приватные, вопросы, питание, админские
- **Система ролей**: USER_0, USER_1, USER_2, CONSULTANT_PITANIE, MODERATOR_ADMIN, VYACHESLAV, SUPER_ADMIN
- **Медиа-файлы**: Загрузка, шифрование, водяные знаки, HLS-стриминг
- **Права доступа**: Гранулярные права для каждой комнаты
- **WebSocket**: Реальное время для чата
- **Keycloak**: Аутентификация и авторизация

## Архитектура
- **Backend**: Spring Boot 3, Java 17
- **База данных**: PostgreSQL 16
- **Аутентификация**: Keycloak 24
- **Контейнеризация**: Docker Compose

## Быстрый старт

### 1. Предварительные требования
- Docker 20.10+
- Docker Compose 2.20+
- Java 17 (для локальной разработки)
- Maven 3.9+ (для локальной разработки)

### 2. Запуск через Docker Compose
```bash
# Клонировать репозиторий
git clone <repository-url>
cd platform

# Запустить все сервисы
docker-compose -f docker-compose.simple.yml up -d

# Проверить статус
docker-compose -f docker-compose.simple.yml ps
```

### 3. Локальная разработка
```bash
# Запустить инфраструктуру
docker-compose -f docker-compose.simple.yml up -d postgres keycloak

# Собрать и запустить backend
cd backend
mvn clean package
java -jar target/platform-*.jar
```

## Сервисы и порты
- **Backend API**: http://localhost:8081
- **Keycloak Admin**: http://localhost:8080 (admin/admin)
- **PostgreSQL**: localhost:5432 (postgres/postgres)

## API Endpoints

### Чат
- `GET /api/v1/chat/{roomSlug}` - Получить сообщения комнаты
- `POST /api/v1/chat/{roomSlug}` - Отправить сообщение в комнату
- `GET /api/v1/chat/questions/{mode}` - Получить вопросы (general/nutrition/vyacheslav)

### Медиа
- `POST /api/v1/media/upload` - Загрузить медиа-файл
- `GET /api/v1/media/{id}/stream` - Стриминг медиа
- `GET /api/v1/media/{id}/download` - Скачать медиа

### Пользователи
- `GET /api/v1/users/me` - Информация о текущем пользователе
- `GET /api/v1/users/{id}` - Информация о пользователе

## Структура базы данных

### Основные таблицы
- `users` - Пользователи
- `messages` - Сообщения
- `chat_rooms` - Комнаты чата
- `chat_room_permissions` - Права доступа к комнатам
- `media_files` - Медиа-файлы
- `access_logs` - Логи доступа

### Миграции
Миграции выполняются автоматически через Liquibase при старте приложения.

## Настройка Keycloak

### 1. Создать realm "platform"
### 2. Создать клиент "platform-backend"
### 3. Настроить роли:
- USER_0, USER_1, USER_2
- CONSULTANT_PITANIE
- MODERATOR_ADMIN
- VYACHESLAV
- SUPER_ADMIN

### 4. Создать пользователей и назначить роли

## Мониторинг и логи
- Логи приложения: `logs/application.log`
- Логи базы данных: через Docker logs
- Health check: `GET /actuator/health`

## Безопасность
- Все запросы требуют JWT токен
- Медиа-файлы шифруются AES-256
- Водяные знаки для видео
- Rate limiting на API endpoints

## Развертывание

### Production
```bash
# Собрать образ
docker build -t platform-backend:1.0.0 ./backend

# Запустить с production конфигурацией
docker-compose -f docker-compose.prod.yml up -d
```

### Environment variables
Создать `.env` файл с настройками:
```bash
DB_URL=jdbc:postgresql://localhost:5432/platform
DB_USERNAME=postgres
DB_PASSWORD=postgres
KEYCLOAK_ISSUER_URI=http://localhost:8080/realms/platform
MEDIA_ENCRYPTION_KEY=<your-encryption-key>
```

## Лицензия
Проприетарная

## Контакты
Для вопросов и поддержки обращаться к администратору платформы. 
