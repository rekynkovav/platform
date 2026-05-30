# Platform Project Documentation

## Текущие роли (Keycloak + Spring Security)

| Роль | Описание | Доступ к медиа |
|------|----------|-----------------|
| `USER_0` | Базовый пользователь | ✅ |
| `USER_1` | Пользователь с доступом к контенту | ✅ |
| `USER_2` | Продвинутый пользователь | ✅ + загрузка |
| `CONSULTANT_PITANIE` | Консультант по питанию | ✅ |
| `CONSULTANT_SPORT` | Консультант по спорту | ✅ |
| `MODERATOR_ADMIN` | Модератор/админ | ✅ |
| `SUPER_ADMIN` | Супер админ | ✅ + загрузка + скачивание |
| `DEVELOPER` | Разработчик | ✅ |
| `VYACHESLAV` | Специальная роль | ✅ |

**Примечание:** Роли хранятся в Keycloak в `realm_access.roles`, преобразуются с префиксом `ROLE_` в Spring Security.

---

## Сущности (Entities)

### Archive
- `id` (String, UUID)
- `title` (String)
- `description` (Text)
- `videoUrl` (String)
- `thumbnailUrl` (String)
- `duration` (Integer, секунды)
- `viewsCount` (Integer)
- `startTime` (LocalDateTime)
- `endTime` (LocalDateTime)
- `eventId` (String, связь с Event)
- `createdAt` (LocalDateTime)
- `updatedAt` (LocalDateTime)

### Event
- `id` (String, UUID)
- `title` (String)
- `description` (Text)
- `startTime` (LocalDateTime)
- `endTime` (LocalDateTime)
- `coverImage` (String)
- `links` (Map<String, String>, JSON)
- `createdBy` (String)
- `createdAt` (LocalDateTime)
- `streamUrl` (String)
- `status` (String)

### User
- `id` (String)
- `username` (String, unique)
- `email` (String)
- `phone` (String)
- `role` (String)
- `avatarUrl` (String)
- `bio` (Text)
- `lastActiveAt` (LocalDateTime)
- `banned` (boolean)
- `banReason` (String)
- `bannedAt` (LocalDateTime)
- `createdAt` (LocalDateTime)
- `updatedAt` (LocalDateTime)

### Message (Чат)
- `id` (String, UUID)
- `content` (Text)
- `userId` (String)
- `userName` (String)
- `chatType` (String) - `general`, `admin_group`, `private`
- `recipientId` (String)
- `isRead` (Boolean)
- `mediaId` (String) - ссылка на MediaFile
- `mediaType` (String) - `image`, `video`, `audio`, `file`
- `fileName` (String)
- `createdAt` (LocalDateTime)

### MediaFile
- `id` (String, UUID)
- `fileName` (String)
- `originalFilename` (String)
- `fileType` (String) - `image`, `video`, `audio`, `file`
- `contentType` (String)
- `fileSize` (Long)
- `storagePath` (String)
- `uploadedBy` (String)
- `createdAt` (LocalDateTime)

---

## API Endpoints (Бэкенд)

### ChatController (`/api/chat`)

| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/api/chat/general` | Сообщения общего чата (limit, before) |
| POST | `/api/chat/general` | Отправить в общий чат {content, mediaId?} |
| GET | `/api/chat/admin-group` | Сообщения админ-группы (требует роль admin/moderator) |
| POST | `/api/chat/admin-group` | Отправить в админ-группу {content, mediaId?} |
| GET | `/api/chat/private/{userId}` | Личные сообщения (limit, before) |
| POST | `/api/chat/private/{userId}` | Отправить личное {content, mediaId?} |
| GET | `/api/chat/unread` | Количество непрочитанных |
| PUT | `/api/chat/messages/{messageId}/read` | Пометить как прочитанное |

**Request Body (SendRequest):**
```json
{
  "content": "текст сообщения",
  "mediaId": "опционально - ID загруженного файла"
}
```

### MediaController (`/api/media`)

| Метод | Путь | Описание | Доступ |
|-------|------|----------|--------|
| POST | `/api/media/upload` | Загрузить файл (file, type) | Любой авторизованный |
| GET | `/api/media/stream/{fileId}` | Получить presigned URL для стриминга | USER+ |
| GET | `/api/media/key/{fileId}` | Получить ключ шифрования | USER+ |
| GET | `/api/media/download/{fileId}` | Скачать файл | USER+ |
| GET | `/api/media/{fileId}` | Получить метаданные файла | USER+ |
| GET | `/api/media/my-files` | Файлы текущего пользователя | USER+ |
| GET | `/api/media/hls/{fileId}/playlist.m3u8` | HLS плейлист для видео | USER+ |
| DELETE | `/api/media/{fileId}` | Удалить файл | Владелец/ADMIN/MODERATOR |

**Параметры upload:**
- `file` - MultipartFile
- `type` - `image` | `video` | `audio` | `file`

### ArchiveController (`/api/archives`)

| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/api/archives` | Все архивы (limit, before) |
| GET | `/api/archives/{id}` | Архив + инкремент просмотров |
| GET | `/api/archives/event/{eventId}` | Архивы по событию |
| POST | `/api/archives` | Создать (admin/moderator) |
| PUT | `/api/archives/{id}` | Обновить (admin/moderator) |
| DELETE | `/api/archives/{id}` | Удалить (admin/moderator) |

---

## Чат с медиа

### Flow загрузки медиа в чат:
```
1. Пользователь выбирает файл → Фронтенд показывает превью
2. Файл загружается на /api/media/upload → сервер возвращает MediaFile { id, fileType, ... }
3. Сообщение отправляется на /api/chat/general с { content, mediaId }
4. При получении сообщений:
   - Если есть mediaId → запрашиваем URL через /api/media/stream/{id}
   - Фронтенд отображает контент по типу (image/video/audio/file)
```

### Поддерживаемые типы медиа:
| Тип | Отображение |
|-----|-------------|
| `image` | Картинка |
| `video` | Видео плеер |
| `audio` | Аудио плеер |
| `file` | Ссылка на скачивание |

---

## Технологии

- **Backend:** Java Spring Boot
- **Auth:** Keycloak (OAuth2/OIDC)
- **Database:** PostgreSQL
- **Media:** FFmpeg, HLS стриминг, шифрование
- **Frontend:** React
- **Migration:** Liquibase
- **Streaming:** HLS/DASH

---

## История изменений

### 2026-05-01
- Добавлена документация по ролям
- Добавлена документация по сущностям Archive, Event, User, Message, MediaFile
- Добавлены полные API endpoints для ChatController, MediaController, ArchiveController
- Добавлена поддержка медиа в чате (Message.mediaId, mediaType, fileName)
- Описан flow загрузки медиа в чате