# Глубинный анализ Java Spring Boot Backend (Platform)

**Проект:** `/home/suveren/platform/backend`
**Java 17, Spring Boot 3.2.5, PostgreSQL, Liquibase, Redis, Keycloak, WebSocket**
**Дата анализа:** 2026-06-12

---

## 1. СТРУКТУРА ПРОЕКТА

```
com.platform/
├── PlatformApplication.java          # Main class
├── aspect/                           # TimedAspect
├── config/                           # 16 конфигурационных классов
├── controller/                       # 18 REST контроллеров
├── dto/                              # CreateBroadcastRequest
├── exception/                        # 10 кастомных исключений + GlobalExceptionHandler
├── filter/                           # IpRateLimitFilter
├── health/                           # 4 health indicators
├── model/                            # 13 JPA сущностей
│   └── dto/                          # 18 DTO классов
├── monitoring/                       # 4 мониторинг-компонента
├── repository/                       # 17 Spring Data JPA репозиториев
├── service/                          # 36 сервисов
├── util/                             # 6 утилитарных классов
├── validation/                       # FileValidator
└── websocket/                        # 5 WebSocket компонентов
```

**Стек:** Spring Boot 3.2.5, Spring Security OAuth2 (Keycloak), JPA/Hibernate, PostgreSQL, Redis (lettuce), Liquibase, Resilience4j, Bucket4j, Micrometer, AWS S3 SDK, FFmpeg.

---

## 2. КОНТРОЛЛЕРЫ (REST API)

18 контроллеров, ~80+ endpoints. Паттерн: `@RestController` + `@RequestMapping` + `@RequiredArgsConstructor`. Используется `@AuthenticationPrincipal Jwt jwt` для получения пользователя.

**Проблемы:**
- Валидация через `@Valid` есть только в `create`/`update` методах
- `@PreAuthorize` дублируется на уровне класса и метода (AdminController)
- Множество фронтенд-алиасов (дубли endpoints)
- Нет rate-limiting на публичных endpoints (health)
- Нет явной документации OpenAPI/Swagger (аннотации)

---

## 3. ОТЧЁТ ПО ПРОБЛЕМАМ

---

### 🔴 CRITICAL

#### C1. Репозиторий Message использует неверный тип ID

- **Файл:** `src/main/java/com/platform/repository/MessageRepository.java:21`
- **Проблема:** `extends JpaRepository<Message, UUID>` — но сущность `Message` использует `@IdClass(Message.MessageId.class)` с составным ключом из `UUID id` + `LocalDateTime createdAt`. Фактический тип первичного ключа — `MessageId`, а не `UUID`. Все вызовы `findById(UUID)` и `findById(UUID.fromString(...))` (например, `ChatService.java:173`, `ChatService.java:382`, `ChatService.java:431`) будут падать с `ClassCastException` или `InvalidDataAccessApiUsageException`.
- **Почему:** `@IdClass` меняет тип идентификатора сущности. Spring Data JPA ожидает ID типа, указанного в generic-параметре репозитория. Несоответствие типов вызывает runtime-ошибки.
- **Как исправить:** Изменить на `extends JpaRepository<Message, Message.MessageId>` и обновить все методы, использующие `findById`, чтобы передавать `MessageId`.

#### C2. Composite primary key + @GeneratedValue конфликт в Message

- **Файл:** `src/main/java/com/platform/model/Message.java:25-31`
- **Проблема:** В составном ключе (`@IdClass`) только поле `id` генерируется через `@PrePersist`, а `createdAt` помечен `@CreationTimestamp`. При попытке `save()` с новым объектом — если `createdAt` ещё не установлен Hibernate, будет нарушение NOT NULL или ошибка вставки.
- **Почему:** Hibernate может сначала вставить строку с null createdAt (до срабатывания @CreationTimestamp), что нарушает NOT NULL constraint.
- **Как исправить:** Явно устанавливать `createdAt` на стороне сервиса или использовать `@PrePersist` для обоих полей.

#### C3. JPA ddl-auto: update в production

- **Файл:** `src/main/resources/application-prod.yml:33`
- **Проблема:** `spring.jpa.hibernate.ddl-auto: update` в production-профиле. Это может привести к потере данных, непреднамеренному созданию/изменению схемы БД.
- **Почему:** Hibernate может перезаписать существующие колонки, изменить типы данных или случайно дропнуть данные.
- **Как исправить:** Установить `spring.jpa.hibernate.ddl-auto: validate` (или `none`) в production. Схема управляется Liquibase.

#### C4. Liquibase отключён в production

- **Файл:** `src/main/resources/application-prod.yml:47`
- **Проблема:** `spring.liquibase.enabled: false` в production. Это означает, что миграции БД не применяются автоматически при деплое. Нарушение принципа Infrastructure as Code.
- **Почему:** Любые изменения схемы БД нужно применять вручную или через внешний скрипт — риск рассинхронизации схемы.
- **Как исправить:** Включить Liquibase: `spring.liquibase.enabled: true` и убедиться, что все миграции идемпотентны (`runOnChange="true"`, preConditions).

#### C5. Partitioned messages table никогда не используется

- **Файл:** `src/main/resources/db/changelog/v7/01-partition-messages.sql`
- **Проблема:** Миграция v7 создаёт таблицу `messages_new` с партициями, но сущность `Message` и репозиторий `MessageRepository` продолжают работать со старой таблицей `messages`. Нет механизма переключения или миграции данных.
- **Почему:** Таблица `messages_new` создаётся, но никто в неё не пишет и не читает. Вся кодовая база маппит `@Table(name = "messages")`. Партиционирование не применяется.
- **Как исправить:** Либо обновить `@Table(name = "messages_new")` и добавить миграцию данных, либо отказаться от партиционирования.

---

### 🟠 HIGH

#### H1. N+1 запрос в PostService.toDto() для single post

- **Файл:** `src/main/java/com/platform/service/PostService.java:177`
- **Проблема:** В методе `toDto()` при вызове из `getPost()` (одиночный пост) `likedPostIds == null`, и выполняется `reactionRepository.existsByPostIdAndUserIdAndType(post.getId(), userId, "like")` — это дополнительный SQL-запрос для каждого поста.
- **Почему:** Для списка постов (`getFeed`) используется батч-загрузка, но для одиночного поста — отдельный запрос.
- **Как исправить:** Передавать `likedPostIds` даже для одиночного запроса или сделать метод `getPost` с проверкой лайка в одном запросе.

#### H2. ChatService.getMessagesByRoomWithPagination — неверная пагинация

- **Файл:** `src/main/java/com/platform/service/ChatService.java:239`
- **Проблема:** `new PageImpl<>(..., pageable, messages.size())` — третий параметр ожидает общее количество записей, но передаётся размер текущей страницы. Свойство `totalElements` и `totalPages` будут неверными.
- **Почему:** `PageImpl` с неправильным total делает навигацию по страницам сломанной для фронтенда.
- **Как исправить:** Использовать `Page<Message> messages = messageRepository.findByRoomId(room.getId(), pageable)` и вернуть `messages.map(this::toDto)`.

#### H3. Дублирование endpoints (Frontend aliases)

- **Файлы:** `ChatController.java:131-138`, `LiveBroadcastController.java:120-126`, `PaymentController.java:39-45`
- **Проблема:** Несколько методов-алиасов, которые просто делегируют в другой метод контроллера:
  - `sendMessageToRoomAlias` -> `sendMessageToRoom`
  - `endBroadcast` -> `stopBroadcast`
  - `createPaymentAlias` -> `createPayment`
- **Почему:** Загрязняет API поверхность, усложняет поддержку. Фронтенд может быть настроен на один правильный путь.
- **Как исправить:** Удалить алиасы, настроить фронтенд на единый API.

#### H4. NotificationService отправляет уведомление автору вопроса вместо админам

- **Файл:** `src/main/java/com/platform/service/NotificationService.java:130-131`
- **Проблема:** Метод `notifyAboutNewQuestionMessage` отправляет уведомление `savedMessage.getUserId()` — то есть автору сообщения. Для `new_answer` это верно, но для `new_question` уведомление должно идти администраторам, а не автору вопроса.
- **Почему:** Логика не различает, кому адресовано уведомление в зависимости от типа.
- **Как исправить:** Для `new_question` отправлять уведомления всем админам, а не автору.

#### H5. ReactionService — race condition на счётчике лайков

- **Файл:** `src/main/java/com/platform/service/ReactionService.java:40-41,61-62`
- **Проблема:** `post.setLikesCount(post.getLikesCount() + 1)` без блокировки или атомарного обновления. При конкурентных запросах счётчик может разойтись.
- **Почему:** Два параллельных запроса могут прочитать `likesCount=5` и записать `6` вместо `7`.
- **Как исправить:** Использовать `@Query("UPDATE Post p SET p.likesCount = p.likesCount + 1 WHERE p.id = :id")` с `@Modifying`.

#### H6. AdminController дублирует @PreAuthorize на классе и методах

- **Файл:** `src/main/java/com/platform/controller/AdminController.java:18,25,36,42,52,63,73,79,91`
- **Проблема:** `@PreAuthorize("hasAnyRole('MODERATOR_ADMIN', 'SUPER_ADMIN', 'VYACHESLAV')")` указан и на классе, и на каждом методе. Это избыточно и может запутать при изменении доступа.
- **Почему:** При удалении аннотации с класса часть методов останется с аннотацией, но для незащищённых методов защита исчезнет.
- **Как исправить:** Оставить только на классе для общих правил, переопределять только для методов с особыми правами.

#### H7. PostService.createPost() — ручное выставление времени

- **Файл:** `src/main/java/com/platform/service/PostService.java:107`
- **Проблема:** Явно устанавливается `createdAt = LocalDateTime.now()`, хотя сущность использует `@CreationTimestamp` и `@UpdateTimestamp`. Это может перезаписать timestamp, установленный Hibernate.
- **Почему:** Двойная установка может привести к несогласованности времени.
- **Как исправить:** Убрать ручную установку дат, положиться на `@CreationTimestamp` / `@UpdateTimestamp`.

#### H8. EventMaterial `@Version` поле названо `version`, но Liquibase создаёт колонку `version` как nullable

- **Файл:** `src/main/java/com/platform/model/EventMaterial.java:34`
- **Проблема:** `@Version private Long version = 0L;` без `@Column(nullable = false)`. Liquibase может создать колонку без `NOT NULL`, что вызывает `OptimisticLockException` при null.
- **Почему:** Hibernate требует, чтобы `@Version` поле не было null.
- **Как исправить:** Добавить `@Column(nullable = false)`.

---

### 🟡 MEDIUM

#### M1. UserController — дублирование маппинга UserDto

- **Файл:** `src/main/java/com/platform/controller/UserController.java:27-39, 53-63, 74-81`
- **Проблема:** Маппинг `User -> UserDto` повторяется 3 раза (getCurrentUser, updateCurrentUser, getUserById). При изменении DTO нужно править в 3 местах.
- **Почему:** Нарушение DRY.
- **Как исправить:** Вынести маппинг в отдельный приватный метод или использовать MapStruct.

#### M2. FeedController дублирует функционал PostController

- **Файл:** `src/main/java/com/platform/controller/FeedController.java`
- **Проблема:** `FeedController` дублирует create/update/delete посты (делегирует в `PostService`), создавая два способа манипуляции постами. Это путает и усложняет поддержку.
- **Почему:** Два контроллера с пересекающейся функциональностью.
- **Как исправить:** Объединить или явно разделить ответственность. FeedController должен быть read-only.

#### M3. KeycloakService — ленивая инициализация без синхронизации

- **Файл:** `src/main/java/com/platform/service/KeycloakService.java:50-61`
- **Проблема:** `getKeycloak()` с ленивой инициализацией `keycloak` не синхронизирован. При конкурентном доступе может создать несколько экземпляров.
- **Почему:** Гонка при старте приложения.
- **Как исправить:** Добавить `synchronized` или использовать `@PostConstruct` для eager инициализации.

#### M4. ChatWebSocketHandler — ThreadPoolExecutor без мониторинга

- **Файл:** `src/main/java/com/platform/websocket/ChatWebSocketHandler.java:48-51`
- **Проблема:** `new ThreadPoolExecutor(...)` создаётся вручную без мониторинга. При утечке задач пул может переполниться, и никто не узнает.
- **Почему:** Отсутствие метрик и алертов на пул потоков.
- **Как исправить:** Использовать `ThreadPoolTaskExecutor` из Spring или добавить Micrometer мониторинг.

#### M5. PlatformApplication — Thread.sleep для graceful shutdown

- **Файл:** `src/main/java/com/platform/PlatformApplication.java:72`
- **Проблема:** `Thread.sleep(3000)` для ожидания завершения транзакций — это хак. Нет гарантии, что 3 секунд достаточно, или что это безопасно.
- **Почему:** Не детерминированное поведение. На реальной нагрузке транзакции могут не уложиться.
- **Как исправить:** Использовать `TaskExecutor` с `awaitTermination()` и настраиваемым таймаутом.

#### M6. Archive и Contact сущности без генерации ID

- **Файлы:** `Archive.java:22`, `Contact.java:24`
- **Проблема:** `@Id private String id;` без `@GeneratedValue` и без `@PrePersist`. Если сервис не установит ID явно, будет `null` и ошибка сохранения.
- **Почему:** Риск runtime-ошибки.
- **Как исправить:** Добавить `@PrePersist` или `@GeneratedValue(strategy = GenerationType.UUID)`.

#### M7. ChatController — `@RateLimiting(name = "chat-limit")` на всех методах, но bucket4j отключён

- **Файл:** `ChatController.java:50,88,111,132,144,167,175,185,193,202,213,224,230,248,256,275,298,308,319,335,350`
- **Файл:** `application.yml:47-48`
- **Проблема:** `bucket4j.enabled: false` в application.yml, но `@RateLimiting` активно используется. Rate limiting фактически не работает.
- **Почему:** Bucket4j отключён. Все аннотации `@RateLimiting` игнорируются.
- **Как исправить:** Включить Bucket4j или убрать аннотации.

#### M8. NotificationController.syncNotifications — небезопасное приведение типов

- **Файл:** `NotificationController.java:73`
- **Проблема:** `Boolean read = (Boolean) notif.get("read");` — если `read` приходит как строка "true"/"false", будет ClassCastException.
- **Почему:** JSON десериализация может вернуть Boolean или String.
- **Как исправить:** Использовать ObjectMapper или явную проверку типа.

#### M9. SecureMediaService — метод `saveTempFile` не используется

- **Файл:** `src/main/java/com/platform/service/SecureMediaService.java:182-187`
- **Проблема:** Приватный метод `saveTempFile()` никогда не вызывается. В uploadSecureMedia используется другой механизм сохранения.
- **Почему:** Мёртвый код.
- **Как исправить:** Удалить.

---

### 🟢 LOW

#### L1. `role` поле в User — String вместо enum

- **Файл:** `src/main/java/com/platform/model/User.java:34`
- **Проблема:** `private String role;` — роль хранится как строка, что приводит к хардкоду сравнений. Типобезопасность отсутствует.
- **Как исправить:** Использовать enum UserRole.

#### L2. Notification.relatedId — Long для большинства ID (UUID)

- **Файл:** `Notification.java:31`
- **Проблема:** `private Long relatedId;` — в системе ID обычно UUID (String), но relatedId хранится как Long. В NotificationService выполняется `Long.parseLong()` для UUID, что всегда будет падать с NumberFormatException.
- **Почему:** Практически всегда `relatedId` будет null из-за ошибки парсинга UUID в Long.
- **Как исправить:** Изменить тип на String.

#### L3. LiveBroadcastController.stopBroadcast возвращает 200 OK вместо 204

- **Файл:** `LiveBroadcastController.java:113`
- **Проблема:** `return ResponseEntity.ok().build();` — для операции удаления/остановки должен быть `noContent()`.
- **Как исправить:** `return ResponseEntity.noContent().build();`

#### L4. PostDto.CreateRequest и UpdateRequest без `@NotBlank`/`@NotNull`

- **Файл:** `PostDto.java:42-46, 52-56`
- **Проблема:** В `CreateRequest` поле `content` не помечено `@NotNull` или `@NotBlank`. Пустой пост можно создать.
- **Как исправить:** Добавить `@NotBlank(message = "Content is required")`.

#### L5. RoleUtils.ADMIN_ROLES и SUPER_ADMIN_ROLES дублируют проверки в сервисах

- **Файлы:** `AdminService.java:53-55,73-75`, `EventService.java:145`, `SecureMediaService.java:348-353`
- **Проблема:** Логика определения админа дублируется в нескольких сервисах вместо использования `RoleUtils.isAdmin()`.
- **Как исправить:** Заменить inline-проверки на вызовы `RoleUtils.isAdmin()` / `RoleUtils.isSuperAdmin()`.

---

## 4. ПРОБЛЕМЫ БЕЗОПАСНОСТИ (SECURITY)

### S1. RTMP Webhook проверяет секрет после логирования ошибки

- **Файл:** `LiveBroadcastController.java:205-213`
- **Проблема:** Если `RTMP_WEBHOOK_SECRET` не настроен, webhook доступен без аутентификации (предупреждение в лог — и всё). Любой может вызвать `onStreamStart`/`onStreamStop`.
- **Severity:** HIGH
- **Исправление:** Блокировать запрос, если секрет не настроен.

### S2. @PreAuthorize на эндпоинтах не консистентен с role hierarchy

- **Файл:** `SecurityConfig.java:46-61`
- **Проблема:** Role hierarchy определяет SUPER_ADMIN > MODERATOR_ADMIN > ..., но `@PreAuthorize("hasAnyRole('MODERATOR_ADMIN', ...)")` по-прежнему перечисляет все роли. Role hierarchy делает это избыточным.
- **Severity:** LOW
- **Исправление:** Использовать `hasRole('MODERATOR_ADMIN')` — hierarchy автоматически даст доступ SUPER_ADMIN.

### S3. Отсутствует защита от массовогоAssignment в AdminController

- **Файл:** `AdminController.java:43-49`
- **Проблема:** `updateUserRole` принимает `role` как `@RequestParam String` без валидации на допустимые значения. Любая строка может быть установлена как роль.
- **Severity:** MEDIUM
- **Исправление:** Добавить валидацию на enum допустимых ролей.

---

## 5. ТЕСТЫ

### Покрытие
- **Service tests:** ~440 @Test методов во всех 36 сервисах
- **Controller tests:** ~163 @Test методов во всех 18 контроллерах
- **Model tests:** ~30 тестов сущностей и DTO
- **Config tests:** ~18 тестов конфигураций
- **WebSocket tests:** ~6 тестов WebSocket хендлеров

### Проблемы покрытия

**Не покрыты тестами:**
- `SecurityConfig.java` — нет интеграционного теста безопасности (кроме `SecurityConfigTest`)
- `WebSocketConfig.java` — проверка CORS конфигов
- `IpRateLimitFilter.java` — rate limiting фильтр
- `ChatWebSocketHandler.java` — основные сценарии: отправка, broadcast, rate limiting (только базовый тест)
- `BroadcastStatsWebSocketHandler.java` — отправка статистики
- `MessageRepository.java` — никаких интеграционных тестов для кастомных @Query методов (все 15+ запросов)
- Интеграционные тесты с Testcontainers — не используются (зависимость есть в pom.xml, но реальных тестов с контейнерами нет)
- Liquibase миграции — не валидируются в тестах
- `PostService.createPost()` — не тестируется с медиа
- `ChatService.archiveAnsweredQuestions()` — граничные случаи
- Optimistic lock recovery — не тестируется

---

## 6. ДУБЛИРОВАНИЕ КОДА

1. **UserDto маппинг** — 3 копии в UserController, 1 в AdminService
2. **AdminController + FeedController + PostController** — пересекающиеся CRUD для постов
3. **isAdmin() проверки** — в 5+ местах (RoleUtils.isAdmin должен быть единым)
4. **Frontend-алиасы** — 3 пары дублирующих endpoints
5. **toDto() методы** — в каждом сервисе вручную, без MapStruct

---

## 7. ПРОИЗВОДИТЕЛЬНОСТЬ

1. **N+1 в PostService.getPost()** — дополнительный запрос для проверки like
2. **MessageRepository полное сканирование** — все запросы без индексов по `roomId + createdAt` (есть составные индексы в v6, но не все покрыты)
3. **ChatService.getQuestionsByMode()** — загружает 100 сообщений для построения вопросов, сортирует в памяти. При 1000+ вопросах — проблема.
4. **@Cacheable("chatMessages")** — кэширует сообщения, но при новом сообщении очищает весь кэш (`@CacheEvict(allEntries = true)`), теряя преимущества кэширования
5. **MediaFile репозиторий** — `incrementViews` и `incrementDownloads` без `@Modifying(flushAutomatically = true)` — возможны stale данные

---

## 8. ТРАНЗАКЦИИ

1. **ReactionService** — два отдельных `save()` (reaction + post) в одной транзакции, но без retry на optimistic lock
2. **ChatService.markAsRead()** — `findById` может упасть из-за проблемы с составным ключом (C1)
3. **AdminService.banUser()/unbanUser()** — не используют `@Retryable` для `OptimisticLockingFailureException`
4. **PostService.createPost()** — не обновляет `@Version` при ручной установке полей
5. **@Transactional(readOnly = true)** на уровне класса + `@Transactional` на методе — корректно, но метод `uploadSecureMedia` в SecureMediaService явно помечен `@Transactional`, хотя класс уже `readOnly = true` — Spring переопределит, но это может быть неочевидно

---

## 9. WebSocket

1. **ChatWebSocketHandler** — `broadcastToRoom` не использует `RedisWebSocketMessageSubscriber` для масштабирования на несколько инстансов. При горизонтальном масштабировании сообщения не доходят до пользователей на других инстансах.
2. **Отсутствует heartbeat** — нет `@Scheduled` пинг-понга, клиенты могут не заметить разрыв соединения (кроме базового PingMessage в коде).
3. **BroadcastStatsWebSocketHandler** — накапливает сессии в памяти инстанса. При рестарте/масштабировании — потеря статистики.
4. **RedisWebSocketSessionRegistry** есть, но не используется в `ChatWebSocketHandler` для меж-инстансной синхронизации.

---

## 10. Liquibase Миграции

| Миграция | Статус | Проблема |
|----------|--------|----------|
| v1-v6 | OK | Стандартные изменения схемы |
| v7 (partition) | **BROKEN** | Создаёт мёртвую таблицу `messages_new` |
| v8 (archive) | OK | Добавляет поля в live_broadcasts |
| v9 (cleanup) | OK | Очистка данных |
| v10 (access_logs) | OK | Новая таблица |
| v11 (contacts) | OK | Новая таблица |
| v12 (event_materials) | OK | Новая таблица |
| v13 (fix notifications) | OK | SQL миграция |

**Общие проблемы:**
- `runOnChange="true"` — может перезапускать миграции при изменении содержимого
- `runInTransaction="false"` (v7/v8) — риск частичного применения
- Нет миграции для конвертации `messages` в партиционированную таблицу (см. C5)

---

## 11. SOLID Нарушения

1. **Single Responsibility**: `ChatService` — 544 строки, отвечает за legacy чат, room-based чат, вопросы, архивацию. Должен быть разбит на несколько сервисов.
2. **Open/Closed**: `AdminService` — каждый новый тип админ-действия требует модификации сервиса.
3. **Liskov Substitution**: `StorageService` интерфейс с default-методами, возвращающими `true`, не даёт контракта для реализации.
4. **Interface Segregation**: `StorageService` объединяет upload, download, delete, presigned URL, verify. Может быть разделён.
5. **Dependency Inversion**: `SecureMediaService` зависит от 7 конкретных реализаций, а не от абстракций.

---

## ИТОГО

| Категория | Количество |
|-----------|-----------|
| 🔴 CRITICAL | 5 |
| 🟠 HIGH | 8 |
| 🟡 MEDIUM | 9 |
| 🟢 LOW | 5 |
| Security | 3 (1 HIGH, 1 MED, 1 LOW) |
| **Total** | **30** |
