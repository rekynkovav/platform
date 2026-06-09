# План исправлений: Раунд 3 и 4

## Текущее состояние проекта

- ✅ Все 16 сервисов запущены и работают
- ✅ `test_system.sh` проходит без ошибок (0/6 фаз — FAIL)
- ✅ Backend запускается за ~46 секунд
- ✅ Аутентификация, API, БД, мониторинг — всё работает

---

## РАУНД 3: Безопасность и критические баги

### CRITICAL (9 штук)

| # | Проблема | Файл:Строка | Описание |
|---|----------|-------------|----------|
| 1 | **Иммитация userId в WebSocket** | `ChatWebSocketHandler.java:112-118` | Клиент передаёт `userId` в JSON payload — можно отправить сообщение от имени любого пользователя. Нужно брать userId из сессии (`session.getAttributes().get("userId")`) |
| 2 | **BroadcastStats WS без аутентификации** | `WebSocketConfig.java:34-39` | Хендлер зарегистрирован без `WebSocketHandshakeInterceptor` — любой клиент может подключиться. Нужно добавить интерцептор |
| 3 | **String vs UUID type mismatch** | `MessageRepository.java:50,51,60` | `@Param("userId") String userId` но `Message.userId` — `UUID`. 3 запроса упадут в рантайме. Исправить на `UUID` |
| 4 | **@Modifying без @Transactional** | `MessageRepository.java:78` | `deleteAnswersByRoomAndQuestionId` — будет `TransactionRequiredException` |
| 5 | **StreamingFileService без авторизации** | `StreamingFileService.java:20-83` | `streamFile()` и `streamZipArchive()` принимают произвольный путь файла — path traversal, любой файл на сервере читаем |
| 6 | **moderatePost() без проверки роли** | `AdminService.java:114-136` | Любой аутентифицированный пользователь может одобрять/удалять посты |
| 7 | **@Data на Post с @OneToMany** | `Post.java:23` | equals/hashCode/toString триггерят загрузку всех комментариев и реакций. Нужно `@EqualsAndHashCode(exclude = {"comments", "reactions"})` + `@ToString.Exclude` |
| 8 | **@Data на Comment с @ManyToOne Post** | `Comment.java:14` | equals/hashCode/toString загружают lazy `post`. Нужно `@EqualsAndHashCode(exclude = "post")` + `@ToString.Exclude` |
| 9 | **@Data на Reaction с @ManyToOne Post** | `Reaction.java:16` | Та же проблема что у Comment. Нужно исключить `post` из equals/hashCode/toString |

### HIGH (8 штук)

| # | Проблема | Файл | Описание |
|---|----------|------|----------|
| 10 | **pom.xml без Spring Boot BOM** | `pom.xml` | Версии зависимостей не управляются — потенциальные несовместимости |
| 11 | **ddl-auto: update в production** | `application-prod.yml:33` | Hibernate может изменить схему БД в проде. Сменить на `validate` или `none` |
| 12 | **Твёрдые пароли в docker-compose** | `docker-compose.yml` | `platform_pass_2026` (×3), `admin` (×2), `platform123` — нужно через env-файлы |
| 13 | **XssSanitizer — регексы** | `util/XssSanitizer.java` | Blacklist-подход можно обойти. Ломает легитимный HTML. Убрать или заменить на OWASP Java HTML Sanitizer |
| 14 | **SqlValidator — регексы** | `util/SqlValidator.java` | Blacklist-подход. Параметризованные запросы JPA уже защищают. Ломает данные с кавычками (O'Brien). Убрать |
| 15 | **BroadcastStats WS без аутентификации** | `WebSocketConfig.java` | Дубль CRITICAL #2 — нужно добавить handshake interceptor |
| 16 | **WebSocket session store JWT токен** | `WebSocketHandshakeInterceptor.java:55` | Хранит сырой JWT в session attributes — нужно хранить только decoded claims |
| 17 | **PostRepository: String vs LocalDateTime** | `PostRepository.java:20,30` | `@Param("before") String before` для сравнения с `LocalDateTime`. Нужно `LocalDateTime` |

---

## РАУНД 4: Архитектура и техдолг

### HIGH (6 штук)

| # | Проблема | Где | Описание |
|---|----------|-----|----------|
| 1 | **N+1 запросы** | ChatService, PostService, SecureMediaService, etc. | 9+ мест. `getFeed()`: для каждого поста вызывает `getRecentComments()` и `existsByPostIdAndUserIdAndType()` |
| 2 | **God-классы** | `ChatService` (540 строк), `SecureMediaService` (363 строки) | Слишком много ответственности. Разбить на smaller services |
| 3 | **Отсутствующие @Transactional** | 8 мест в 6 сервисах | `UserService.updateUser()`, `ChatRoomService.setPermission()`, `NotificationService.createAndSend()`, etc. |
| 4 | **Concurrency race conditions** | 6 мест | `ReactionService.toggleReaction()`, `UploadLimitService` TOCTOU, `PaymentService` TOCTOU, `LiveBroadcastService.addViewer()` |
| 5 | **Дублирование ffmpeg** | 4 сервиса | WatermarkService, HLSStreamingService, BroadcastArchiveService, MediaQueueService — идентичный код запуска ffmpeg |
| 6 | **Hardcoded банковские реквизиты** | `QRCodeService.java:39-53` | Счета, ИНН, КПП, БИК в коде — убрать в конфиг/БД |

### MEDIUM (8 штук)

| # | Проблема | Где | Описание |
|---|----------|-----|----------|
| 7 | **Дубликаты endpoint'ов** | ChatController, PaymentController, LiveBroadcastController | `/{roomSlug}` и `/{roomSlug}/messages` (одно и то же), `/payments/create` и `/payments`, `/create` и `/start`, `/{id}/stop` и `/{id}/end` |
| 8 | **Stub-код в проде** | DemoController (12 заглушек), DashboardController (2 заглушки) | Удалить или пометить `@Profile("dev")` |
| 9 | **Нет RoleHierarchy** | SecurityConfig | Роли повторяются по 20+ раз. Создать `RoleHierarchy` bean |
| 10 | **Dead code** | Bucket4j конфиг, ErrorResponse, roomSessionsCache, unused imports | Удалить мёртвый код |
| 11 | **GlobalExceptionHandler не ловит** | GlobalExceptionHandler | `ValidationException` и кастомный `AccessDeniedException` не обрабатываются |
| 12 | **Keycloak в start-dev** | `docker-compose.yml:42` | В production использовать `start` вместо `start-dev` |
| 13 | **Duplicate CircuitBreakerRegistry** | ResilienceConfig + PlatformCircuitBreakerConfig | Два бина с одним именем — silent override |
| 14 | **JacksonConfig inconsistency** | `JacksonConfig.java:60-61` | Два ObjectMapper: один FAIL_ON_UNKNOWN_PROPERTIES=true, другой=false |

### LOW (7 штук)

| # | Проблема | Где | Описание |
|---|----------|-----|----------|
| 15 | **10/16 entity без @Version** | Comment, Contact, Post, etc. | Нет оптимистичных блокировок — потеря данных при конкурентных записях |
| 16 | **Русские комментарии** | ChatController, DashboardController, FeedController, EventMaterialController | Смешанный язык снижает поддерживаемость |
| 17 | **System.out.println в проде** | `ChatRoomService.java:29,64,173` | Заменить на `log.info()` |
| 18 | **Нет .dockerignore** | корень проекта | Build context — весь проект (node_modules, .git, etc.) |
| 19 | **@Builder без @Builder.Default** | LiveBroadcast, Payment, User | `version = 0L` игнорируется @Builder |
| 20 | **Размазанные DTO маппинги** | Все сервисы | Ручные toDto/toEntity в каждом сервисе — использовать MapStruct |
| 21 | **Аудит только в логах** | `AuditService.java` | Записи теряются при ротации логов. Нужна персистентность |

---

## Порядок исправления

### Раунд 3 (сначала)
1. CRITICAL #1-9 — исправить все критические уязвимости
2. HIGH #10-17 — исправить high-priority проблемы

### Раунд 4 (потом)
3. HIGH #1-6 — архитектурные улучшения
4. MEDIUM #7-14 — убрать дублирование и мёртвый код
5. LOW #15-21 — косметика и лучшие практики

---

## После исправлений
- Пересобрать Docker образ backend
- Запустить `test_system.sh` — 0 ошибок
- Ручное тестирование критических эндпоинтов
