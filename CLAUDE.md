# Platform Project - Документация для Claude

## 🎯 ОБЩЕЕ ОПИСАНИЕ

**Platform** - это платформа для спортивного сообщества с функциями стриминга, социальных сетей и архива событий.

## 🏗️ АРХИТЕКТУРА ПРОЕКТА

### **Структура проекта:**
```
platform/
├── backend/          # Spring Boot приложение (Java 17)
├── frontend/         # React приложение (основной)
├── frontend-ready/   # Готовый фронтенд с node_modules
├── docker/           # Docker конфигурация
├── scripts/          # Скрипты инициализации
└── nginx/            # Nginx конфигурация
```

## ✅ РЕАЛИЗОВАННЫЕ ФУНКЦИИ

### 1. **Защищенный медиа контент**
- ✅ AES-256 шифрование всех файлов
- ✅ Водяные знаки на видео и изображениях
- ✅ HLS стриминг с шифрованием сегментов
- ✅ Presigned URLs с истечением через 5 минут
- ✅ Аудит всех обращений к файлам

### 2. **Система ролей и прав доступа**
- ✅ 10+ ролей с иерархией (USER_0 → SUPER_ADMIN)
- ✅ Только SUPER_ADMIN может скачивать оригиналы файлов
- ✅ Keycloak интеграция (OAuth2/OIDC)
- ✅ Мок-режим для разработки (USER_0 всегда в мок-режиме)

### 3. **Чат-комнаты**
- ✅ 7 чат-комнат с разными уровнями доступа:
  1. `user1` - "Чат новичков" (general)
  2. `user2` - "Чат продвинутых" (general)
  3. `nutrition` - "Чат Питание" (nutrition)
  4. `questions-general` - "Общие вопросы" (question)
  5. `questions-nutrition` - "Вопросы по питанию" (question)
  6. `questions-vyacheslav` - "Вопросы лично Вячеславу" (question)
  7. `admins` - "Групповой чат администраторов" (admin)

### 4. **API Endpoints (Backend)**
```
ChatController (/api/chat):
  GET    /api/chat/rooms              - Список активных комнат
  GET    /api/chat/rooms/{slug}       - Получить комнату по slug
  POST   /api/chat/rooms/{slug}/messages - Отправить сообщение
  GET    /api/chat/rooms/{slug}/messages - Получить сообщения комнаты

MediaController (/api/media):
  POST   /api/media/upload            - Загрузка с шифрованием + водяные знаки
  GET    /api/media/stream/{fileId}   - Presigned URL (5 мин)
  GET    /api/media/key/{fileId}      - Ключ шифрования для HLS
  GET    /api/media/download/{fileId} - Скачать расшифрованный (SUPER_ADMIN)
  GET    /api/media/{fileId}          - Информация о файле
  GET    /api/media/my-files          - Файлы пользователя
  GET    /api/media/hls/{fileId}/playlist.m3u8 - HLS плейлист
  DELETE /api/media/{fileId}          - Удалить файл

ArchiveController (/api/archives):
  GET    /api/archives                - Все архивы
  GET    /api/archives/{id}           - Архив + инкремент просмотров
  GET    /api/archives/event/{eventId} - Архивы по событию
  POST   /api/archives                - Создать (admin/moderator)
  PUT    /api/archives/{id}           - Обновить (admin/moderator)
  DELETE /api/archives/{id}           - Удалить (admin/moderator)
```

## 🔧 ТЕХНОЛОГИЧЕСКИЙ СТЕК

### **Backend:**
- Java 17
- Spring Boot 3.x
- Spring Security (Keycloak)
- Spring Data JPA
- PostgreSQL
- Liquibase (миграции)
- Redis (кеш)
- AWS S3 SDK (для Яндекс Cloud Storage)
- FFmpeg (водяные знаки, HLS)

### **Frontend:**
- React 18
- TypeScript
- Tailwind CSS
- React Query
- Axios
- Keycloak React Adapter

### **Инфраструктура:**
- Docker / Docker Compose
- Keycloak (аутентификация)
- Nginx (прокси)
- Яндекс Cloud Storage (S3-совместимый)

## 🚀 ЗАПУСК ПРОЕКТА

### **1. Запуск фронтенда:**
```bash
# Основной фронтенд (требует установки зависимостей)
cd frontend
npm install
npm start  # порт 3006

# Или готовый фронтенд (уже с node_modules)
cd frontend-ready
npm start  # порт 3000
```

### **2. Запуск бэкенда:**
```bash
cd backend
./gradlew bootRun  # или mvn spring-boot:run
```

### **3. Запуск через Docker:**
```bash
cd docker
./start.sh
```

### **4. Требования:**
- **FFmpeg** (для водяных знаков и HLS):
  ```bash
  choco install ffmpeg  # Windows
  sudo apt install ffmpeg  # Linux
  brew install ffmpeg  # macOS
  ```

- **Переменные окружения** (создать `.env`):
  ```bash
  # PostgreSQL
  DB_URL=jdbc:postgresql://localhost:5432/platform_db
  DB_USER=platform_user
  DB_PASSWORD=your_password

  # Keycloak
  KEYCLOAK_URL=http://localhost:8080
  KEYCLOAK_REALM=platform-realm
  KEYCLOAK_CLIENT_SECRET=your-client-secret

  # Яндекс S3
  YC_ACCESS_KEY=your-access-key
  YC_SECRET_KEY=your-secret-key
  YC_BUCKET=platform-media

  # Шифрование
  MEDIA_MASTER_KEY=change-this-to-strong-random-key-32-chars-minimum

  # Redis
  REDIS_HOST=localhost
  REDIS_PORT=6379
  ```

## 🎮 СИСТЕМА РОЛЕЙ

### **Маппинг ролей Frontend ↔ Backend:**
| Frontend Role | Keycloak Role | Backend Role | Права |
|--------------|---------------|--------------|-------|
| `user0` | `USER` | `USER` | Только просмотр help |
| `user1` | `PREMIUM` | `PREMIUM` | Базовый контент + Чат 1 |
| `user2` | `ADVANCED` | `ADVANCED` | Расширенный контент + Чат 2 |
| `admin-moderator` | `MODERATOR` | `MODERATOR` | Модерация + создание контента |
| `admin-consultant` | `CONSULTANT_ADMIN` | `CONSULTANT_ADMIN` | Консультации |
| `admin-nutrition` | `NUTRITION_ADMIN` | `NUTRITION_ADMIN` | Лента питания |
| `admin-sport` | `SPORT_ADMIN` | `SPORT_ADMIN` | Спортивный контент |
| `developer` | `CREATOR` | `CREATOR` | Загрузка медиа + разработка |
| `owner` | `OWNER` | `OWNER` | Полный доступ |
| `super-admin` | `SUPER_ADMIN` | `SUPER_ADMIN` | **Скачивание оригиналов** |

### **Иерархия ролей (от высшего к низшему):**
1. **SUPER_ADMIN** 🔱 - Главный админ (единственный, кто может скачивать оригиналы)
2. **OWNER** 👑 - Владелец (полный доступ ко всему)
3. **ADMIN** 🛡️ - Администратор (модерация, управление пользователями)
4. **MODERATOR** 🛡️ - Модератор (модерация контента, ответы на вопросы)
5. **Специализированные админы** (SPORT_ADMIN, NUTRITION_ADMIN, CONSULTANT_ADMIN)
6. **CREATOR** 💻 - Создатель контента (загрузка медиа файлов)
7. **ADVANCED** ⭐ - Продвинутый пользователь (дост��п к продвинутому контенту)
8. **PREMIUM** 🌱 - Премиум пользователь (доступ к премиум контенту)
9. **USER** 👁️ - Базовый пользователь (только просмотр)

## 🔒 БЕЗОПАСНОСТЬ

### **Что защищено:**
✅ Файлы шифруются AES-256  
✅ Ключи генерируются уникально для каждого файла (masterKey + userId + fileId)  
✅ Presigned URLs живут 5 минут  
✅ Все обращения логируются  
✅ Проверка прав доступа на каждый запрос  
✅ JWT токены с коротким TTL  
✅ Водяные знаки на всех файлах (кроме оригиналов для SUPER_ADMIN)  

### **Что НЕ защищено:**
❌ Screen recording (запись экрана)  
❌ Камера, снимающая экран  
❌ Инсайдеры с легитимным доступом  

### **Рекомендации по безопасности:**
- Использовать HTTPS везде
- Ротация MEDIA_MASTER_KEY раз в квартал
- Мониторинг access_logs на аномалии
- Rate limiting на API endpoints
- Двухфакторная аутентификация для SUPER_ADMIN

## 📊 СУЩНОСТИ (ENTITIES)

### **Archive:**
- `id` (String, UUID), `title`, `description`, `videoUrl`, `thumbnailUrl`
- `duration` (секунды), `viewsCount`, `startTime`, `endTime`
- `eventId`, `createdAt`, `updatedAt`

### **Event:**
- `id` (String, UUID), `title`, `description`, `startTime`, `endTime`
- `coverImage`, `links` (Map<String, String>, JSON), `createdBy`
- `createdAt`, `streamUrl`, `status`

### **User:**
- `id`, `username` (unique), `email`, `phone`, `role`
- `avatarUrl`, `bio`, `lastActiveAt`, `banned`, `banReason`
- `bannedAt`, `createdAt`, `updatedAt`

### **Message (Чат):**
- `id` (String, UUID), `content`, `userId`, `userName`
- `chatType` (`general`, `admin_group`, `private`)
- `recipientId`, `isRead`, `mediaId`, `mediaType`
- `fileName`, `createdAt`

### **MediaFile:**
- `id` (String, UUID), `fileName`, `originalFilename`
- `fileType` (`image`, `video`, `audio`, `file`)
- `contentType`, `fileSize`, `storagePath`
- `uploadedBy`, `createdAt`

## 📁 ВАЖНЫЕ ФАЙЛЫ ДОКУМЕНТАЦИИ

### **В корне проекта:**
- `PROJECT_INFO.md` - Полная документация по проекту
- `README.md` - Основная информация и запуск
- `IMPLEMENTATION_COMPLETE.md` - Документация по защите медиа
- `MEDIA_SETUP.md` - Инструкция по настройке защищенного медиа
- `FFMPEG_SETUP.md` - Установка и настройка FFmpeg
- `ROLES_MAPPING.md` - Полный маппинг ролей
- `ROLES_AND_PERMISSIONS.md` - Роли и права доступа
- `ROLES_UPDATE.md` - Обновление системы ролей
- `PAYMENT_SYSTEM.md` - Система оплаты (в разработке)
- `PAYMENT_OPTIONS.md` - Варианты реализации оплаты
- `MEDIA_PROTECTION_COMPLETE.md` - Итог по защите медиа
- `IMPLEMENTATION_SUMMARY.md` - Итог внедрения архитектуры чат-комнат

### **Во фронтенде:**
- `frontend/CLAUDE.md` - Документация для фронтенда
- `frontend-ready/` - Готовый фронтенд с node_modules

## 🎯 СТАТУС ПРОЕКТА

### **✅ Завершено:**
- Защита медиа контента (шифрование, водяные знаки, HLS)
- Система ролей и прав доступа
- Чат-комнаты (7 комнат)
- Keycloak интеграция
- Frontend с мок-режимом
- Документация

### **🚧 В разработке:**
- Система оплаты (ЮKassa + СБП)
- Админ-панель для модерации
- Уведомления и email рассылки

### **📋 Планируется:**
- Мобильное приложение
- Live стриминг
- Социальные функции (лайки, комментарии, подписки)
- Аналитика и отчеты

## 🔗 ПОЛЕЗНЫЕ ССЫЛКИ

- **Frontend:** http://localhost:3000 (или 3006)
- **Backend API:** http://localhost:8081 (если запущен)
- **Keycloak:** http://localhost:8080
- **Swagger UI:** http://localhost:8081/swagger-ui.html (если настроен)

## 📞 КОНТАКТЫ И ПОДДЕРЖКА

### **Для разработки:**
- Использовать **мок-режим** для тестирования (USER_0 всегда в мок-режиме)
- **RoleSwitcher** в правом верхнем углу для смены ролей
- Все роли кроме USER_0 могут использовать реальный API

### **При возникновении проблем:**
1. Проверить логи приложения
2. Проверить консоль браузера (F12 → Console)
3. Проверить Network запросы (F12 → Network)
4. Очистить кэш браузера (Ctrl + Shift + Delete)

---

**Последнее обновление:** 2026-05-17  
**Версия системы:** Полная функциональность с защитой медиа  
**Статус:** Готово к использованию и дальнейшей разработке 🚀