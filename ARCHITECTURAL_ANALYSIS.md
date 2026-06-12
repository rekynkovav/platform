# Глубокий архитектурный анализ Java/Spring Boot Backend

**Проект:** /home/suveren/platform
**Дата:** 2026-06-12
**Java 17, Spring Boot 3.2.5, Maven multi-module**

---

## Легенда

| Severity | Описание |
|----------|----------|
| CRITICAL | Может привести к потере данных, падению в production, дыре в безопасности |
| HIGH | Нарушение архитектурных принципов, значительный технический долг |
| MEDIUM | Нарушение best practices, потенциальные проблемы |
| LOW | Косметические/незначительные замечания |

---

## 1. CRITICAL

### 1.1. Hibernate `ddl-auto: update` в production

**Файл:** `backend/src/main/resources/application-prod.yml:33`
**Файл:** `backend/src/main/resources/application.yml:18`

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
```

**Почему CRITICAL:** Hibernate DDL auto-update может незаметно изменить схему БД в production (удалить колонки, изменить типы). В сочетании с `liquibase.enabled: false` (строки 26, 47) миграции полностью отключены — нет контроля версионирования схемы.

**Рекомендация:** В `application-prod.yml` установить `ddl-auto: validate` или `none`. Включить Liquibase/Flyway для всех окружений.

---

### 1.2. Liquibase отключён во всех окружениях, включая production

**Файл:** `application.yml:26-27` — `liquibase.enabled: false`
**Файл:** `application-prod.yml:46-47` — `liquibase.enabled: false`

**Почему CRITICAL:** Без миграций любые изменения схемы БД — ручные или через Hibernate DDL. В production это означает отсутствие отката, аудита и воспроизводимости изменений.

**Рекомендация:** Включить Liquibase или Flyway. Даже для dev-окружения (application-dev.yml:20-23 — Liquibase включён) нужна единая стратегия для всех профилей.

---

### 1.3. ErrorResponse определён, но НЕ используется — GlobalExceptionHandler возвращает `Map<String,String>`

**Файл:** `exception/ErrorResponse.java:1-63` — полноценный DTO с timestamp, status, error, message, path, errorCode, validationErrors, traceId
**Файл:** `exception/GlobalExceptionHandler.java:28-210` — ВСЕ 12 методов возвращают `Map.of("error", message)` или `Map.of("error", ..., "details", ...)`

**Почему CRITICAL:** Клиентский код не может стандартно парсить ошибки. Нет HTTP status code, timestamp, path, traceId — невозможно отлаживать в production. Формат ответа разный:
- `Map.of("error", e.getMessage())` — простые ошибки
- `Map.of("error", "Validation failed", "details", errors)` — для MethodArgumentNotValidException
- Нигде нет `path`, `timestamp`, `status` в теле

**Рекомендация:** Переписать GlobalExceptionHandler, чтобы ВСЕ методы возвращали `ResponseEntity<ErrorResponse>`, используя статические фабрики `ErrorResponse.of(...)`.

---

### 1.4. ChatService — God-класс (544 строки, 6 зависимостей, множественные ответственности)

**Файл:** `service/ChatService.java:1-544`

**Что не так:**
- Смешивает: legacy сообщения (general/admin/private), room-based сообщения, QuestionDto (вопросы/ответы), архивацию
- 21 публичный метод при 6 injected dependencies
- `getQuestionsByMode()` (line 277) — 50+ строк с логикой сортировки, маппинга, формирования QuestionDto
- `archiveAnsweredQuestions()` (line 453) — 50+ строк с пейджингом, поиском ответов, архивированием
- Нарушение SRP (Single Responsibility Principle) — класс отвечает и за чат, и за вопросы, и за архивацию

**Рекомендация:**
- Выделить `QuestionService` (создание, ответы, архивация вопросов)
- Выделить `LegacyChatSupport` для backward-compatibility методов
- ChatService должен содержать только room-based CRUD сообщений

---

### 1.5. ChatWebSocketHandler — слишком много ответственностей (363 строки)

**Файл:** `websocket/ChatWebSocketHandler.java:1-363`

**Ответственности:**
- Управление WebSocket сессиями (roomSessions, userSessions)
- Rate limiting сообщений (messageCounts, MAX_MESSAGES_PER_MINUTE)
- Broadcast сообщений в комнаты
- Retry-механизм доставки (sendWithRetry, PendingMessage, retryExecutor)
- Пинг/сердцебиения
- Очистка "старых" сессий
- Отправка нотификаций по WebSocket
- Внутренний вложенный класс ChatMessagePayload

**Рекомендация:** Выделить `WebSocketSessionManager` (управление сессиями), `WebSocketRateLimiter` (rate limit), `WebSocketBroadcaster` (рассылка), `MessageDeliveryRetryHandler` (retry).

---

## 2. HIGH

### 2.1. N+1 запрос в PostService.toDto()

**Файл:** `service/PostService.java:176-177`

```java
boolean userLiked = likedPostIds != null ? likedPostIds.contains(post.getId())
    : reactionRepository.existsByPostIdAndUserIdAndType(post.getId(), userId, "like");
List<CommentDto> recentComments = commentService.getRecentComments(post.getId(), 3);
```

**Почему HIGH:** Если `likedPostIds == null` (вызов `getPost()` line 74), для каждого поста выполняется отдельный запрос `existsByPostIdAndUserIdAndType` + запрос комментариев. При пагинации из 50 постов — 100 дополнительных SQL запросов.

**Рекомендация:**
- Всегда передавать `likedPostIds` (Set<String>)
- Загружать комментарии через `@EntityGraph` или batch fetch
- Добавить `@BatchSize` на коллекции Post

---

### 2.2. Все 34 сервиса — конкретные классы без интерфейсов (нарушение DIP)

**Все сервисы в** `service/*.java` — `@Service` без `implements IService`

**Почему HIGH:** Dependency Inversion Principle требует зависимости от абстракций, не от конкреций. Невозможно:
- Подменить реализацию (например, InMemoryPaymentService для тестов)
- Использовать JDK-прокси для AOP (только CGLIB)
- Ослабить связанность

**Рекомендация:** Ввести интерфейсы для каждого сервиса или как минимум для внешних интеграций (StorageService — уже интерфейс, но его реализации S3StorageService и LocalStorageService есть).

---

### 2.3. Role checking размазан по коду — hardcoded строки ролей

**Файлы с дублированием `isAdmin`-подобной логики:**

| Файл | Строки | Роли |
|------|--------|------|
| `service/SecureMediaService.java` | 349-352 | SUPER_ADMIN, VYACHESLAV, MODERATOR_ADMIN, DEVELOPER |
| `service/SecureMediaService.java` | 355-356 | SUPER_ADMIN, VYACHESLAV |
| `service/UserService.java` | 88-89 | MODERATOR_ADMIN, SUPER_ADMIN, VYACHESLAV |
| `service/UserService.java` | 98-99 | MODERATOR_ADMIN, SUPER_ADMIN, VYACHESLAV |
| `service/EventService.java` | 144-145 | MODERATOR_ADMIN, SUPER_ADMIN, VYACHESLAV |
| `service/AdminService.java` | 53-54 | SUPER_ADMIN, VYACHESLAV |
| `service/AdminService.java` | 73-74 | MODERATOR_ADMIN, SUPER_ADMIN, VYACHESLAV |
| `service/AdminService.java` | 93-94 | MODERATOR_ADMIN, SUPER_ADMIN, VYACHESLAV |
| `service/AdminService.java` | 119-120 | MODERATOR_ADMIN, SUPER_ADMIN, VYACHESLAV |
| `service/CommentService.java` | 122-123 | MODERATOR_ADMIN, SUPER_ADMIN, VYACHESLAV |
| `service/ArchiveService.java` | 163 | MODERATOR_ADMIN, SUPER_ADMIN, VYACHESLAV |

**Почему HIGH:** Hardcoded строки — источник багов. Роли `VYACHESLAV` — имя пользователя, не роль (нарушение принципа ролевой модели). Изменение ролевой модели требует правки N файлов.

**Рекомендация:**
- Создать enum `PlatformRole` с методами `isAdmin()`, `isSuperAdmin()`, `canModerate()`
- В `RoleUtils` убрать `getUserRole`, добавить методы проверки
- Удалить `VYACHESLAV` как роль — это пользователь
- Пересмотреть `RoleHierarchy` в `SecurityConfig:74-87`

---

### 2.4. GlobalExceptionHandler — нестандартный формат ответа

**Файл:** `exception/GlobalExceptionHandler.java`

**Проблема:** Методы возвращают разные типы:
- `ResponseEntity<Map<String, String>>` — 10 методов
- `ResponseEntity<Map<String, Object>>` — 2 метода (MethodArgumentNotValidException, ConstraintViolationException)

Нет поля:
- `path` — какой URL вызвал ошибку
- `timestamp` — когда
- `status` — HTTP status code
- `traceId` — для трейсинга

**Рекомендация:** Использовать единый `ErrorResponse` как возвращаемый тип.

---

### 2.5. Cacheable на методах с аргументами userRole — кэширование прав доступа

**Файл:** `service/ChatService.java:212` и `:230`

```java
@Cacheable(value = "chatMessages", key = "#roomSlug + '_' + #limit + '_' + #before")
public List<MessageDto> getMessagesByRoom(String roomSlug, int limit, Instant before, String userRole) {
```

**Почему HIGH:** Ключ кэша НЕ включает `userRole`. Первый пользователь с ролью "read" инициирует кэш, второй с "read_write" получит те же данные — нарушение авторизации.

**Рекомендация:** Добавить `userRole` в key, разделять кэш по ролям, или убрать `@Cacheable` с методов авторизации.

---

### 2.6. SecureMediaService.uploadSecureMedia() — метод-монстр (130 строк + cleanup)

**Файл:** `service/SecureMediaService.java:50-179`

**Делает всё:**
1. Валидация размера
2. Проверка лимитов загрузки
3. Сохранение временного файла
4. Вставка водяного знака (видео или изображение)
5. Шифрование (AES/GCM)
6. Загрузка в S3
7. HLS конвертация
8. Сохранение в БД
9. Аудит (AccessLog)
10. Инкремент счётчика
11. Cleanup временных файлов

**Проблемы:**
- `@Transactional` на 130 строк — долгая транзакция с I/O (watermark, encryption, S3 upload внутри транзакции)
- `RuntimeException("Failed to upload media: " + e.getMessage())` — теряет исходный exception
- 4 уровня вложенности try/catch/finally

**Рекомендация:**
- Разделить на этапы с отдельными транзакциями
- Upload-статус хранить в БД (PENDING -> PROCESSING -> READY/FAILED)
- Вынести watermark, encryption, HLS в отдельные шаги
- Использовать `@Async` для долгих операций

---

### 2.7. `VYACHESLAV` как роль — нарушение RBAC

**Файл:** `config/SecurityConfig.java:74-87`
**Файл:** `service/SecureMediaService.java:350`
**И другие.**

**Проблема:** `VYACHESLAV` — имя конкретного пользователя, но используется как роль в RoleHierarchy, `@PreAuthorize`, `hasAnyRole`. Это означает:
- Роль жёстко завязана на конкретного человека
- Прекращает работать, когда пользователь меняется
- Невозможно дать те же права другому

**Рекомендация:** Заменить на абстрактную роль (например, `OWNER`), и назначать её нужным пользователям через Keycloak.

---

## 3. MEDIUM

### 3.1. Роль VYACHESLAV в иерархии — некорректная настройка

**Файл:** `config/SecurityConfig.java:75`

```java
"ROLE_SUPER_ADMIN > ROLE_VYACHESLAV\n" +
```

VYACHESLAV стоит **ниже** SUPER_ADMIN в иерархии, но во всех `@PreAuthorize` они перечислены наравне. SUPER_ADMIN имеет права VYACHESLAV, хотя должен быть наоборот.

---

### 3.2. ResilienceConfig дублирует бины registries

**Файл:** `config/ResilienceConfig.java:103-114`
**Файл:** `config/PlatformCircuitBreakerConfig.java:18-20`

Оба конфига создают `CircuitBreakerRegistry`. ResilienceConfig также создаёт `RetryRegistry`, `TimeLimiterRegistry`, `BulkheadRegistry` — это создаёт риск конфликта бинов.

**Рекомендация:** Свести все Resilience4j конфигурации в один класс или использовать свойства в application.yml.

---

### 3.3. MediaController — закомментированные @RateLimiting

**Файл:** `controller/MediaController.java:30,42,57,73,90,100,110,131`

Все аннотации `@RateLimiting(name = "media-limit")` закомментированы, при этом в ChatController они работают (`@RateLimiting(name = "chat-limit")`). Это означает, что media endpoint не защищён от rate limiting.

**Рекомендация:** Раскомментировать или удалить — не оставлять dead code.

---

### 3.4. DashboardController — заглушки

**Файл:** `controller/DashboardController.java:40,56-59`

```java
return ResponseEntity.ok(List.of());  // Заглушка
return ResponseEntity.ok(Map.of("saved", true, "itemsCount", menuData.size()));  // Заглушка
```

Эндпоинты дашборда всегда возвращают пустой ответ. Frontend работает с заглушками , но в production это неработающий функционал.

**Рекомендация:** Удалить заглушки или добавить TODO с тикетом.

---

### 3.5. BroadcastArchiveService.detectContentType() — всегда возвращает "video"

**Файл:** `service/BroadcastArchiveService.java:191-193`

```java
private String detectContentType(LiveBroadcast broadcast) {
    return "video";
}
```

Аудио-конвертация (`convertHlsToM4a`) никогда не вызывается.

---

### 3.6. NotificationService — ручная работа с CacheManager

**Файл:** `service/NotificationService.java:86-88,114-115`

```java
cacheManager.getCache("unreadNotifications").evict(userId);
```

Вместо `@CacheEvict` используется прямой вызов CacheManager, что связывает сервис с Redis/Caffeine.

**Рекомендация:** Использовать `@CacheEvict` для всех cache-операций.

---

### 3.7. Темп-файлы с хардкоженными путями

- `/tmp/watermarks` — `application.yml:66`
- `/tmp/hls/` — `BroadcastArchiveService.java:34`
- `/tmp/archive_*` — `BroadcastArchiveService.java:150`

**Рекомендация:** Вынести все temp-пути в конфигурацию.

---

### 3.8. Composite Key Message.MessageId содержит LocalDateTime

**Файл:** `model/Message.java:22-31`

`@IdClass(Message.MessageId.class)` c полями `UUID id` + `LocalDateTime createdAt`. LocalDateTime как часть PK может вызвать проблемы с точностью (наносекунды vs миллисекунды БД).

**Рекомендация:** Использовать `Instant` или отдельный `Long sortKey`.

---

### 3.9. LiveBroadcastService.addViewer() — select после update

**Файл:** `service/LiveBroadcastService.java:175-186`

```java
broadcastRepository.incrementViewerCount(UUID.fromString(broadcastId));
LiveBroadcast broadcast = getBroadcast(broadcastId);  // redundant SELECT or cached
```

После `incrementViewerCount` (UPDATE) следует `getBroadcast(broadcastId)`, который идёт в кэш (если кэш не сброшен) или в БД — потенциально stale read.

**Рекомендация:** Использовать атомарный UPDATE с возвратом нового значения.

---

### 3.10. LiveBroadcastService.startBroadcast() — кэш после save

**Файл:** `service/LiveBroadcastService.java:68-83`

```java
@CacheEvict(value = "broadcasts", allEntries = true)
public String startBroadcast(...) {
    LiveBroadcast broadcast = getBroadcast(broadcastId); // из кэша
    broadcast.setStatus(BroadcastStatus.LIVE);
    broadcastRepository.save(broadcast); // должен обновить кэш, но @CachePut не указан
    ...
}
```

После `@CacheEvict` кэш очищен, но не обновлён. Следующий `getBroadcast()` не найдёт broadcast в кэше и пойдёт в БД. OK, но избыточно.

---

### 3.11. Хардкоженный RTMP/HLS URL

**Файл:** `service/LiveBroadcastService.java:79`

```java
String rtmpUrl = String.format("rtmp://localhost:1935/live/%s", broadcast.getRtmpStreamKey());
```

Также line 142:
```java
return String.format("http://localhost:8000/hls/%s/index.m3u8", broadcast.getRtmpStreamKey());
```

В production `localhost` не будет работать — нужно конфигурировать через application.yml.

---

### 3.12. UUID.toString().equals() вместо Objects.equals()

**Файл:** `service/LiveBroadcastService.java:71,91,125`

```java
if (!broadcast.getOwnerId().toString().equals(userId)) {
```

Создаёт две строки из UUID для сравнения. Лучше:
```java
if (!broadcast.getOwnerId().equals(UUID.fromString(userId))) {
```

---

## 4. LOW

### 4.1. Несогласованное логирование

Большинство сервисов используют `@Slf4j`, но `ChatWebSocketHandler` использует ручной `LoggerFactory.getLogger()` (line 293). Лучше унифицировать.

### 4.2. Магические числа

- `MAX_SESSIONS_PER_ROOM = 10000` (ChatWebSocketHandler:44)
- `maxAttempts = 3` в нескольких `@Retryable` (PaymentService:43, KeycloakService:68, LiveBroadcastService:63)
- `batchSize = 100` (ChatService:473)
- `buffer = new byte[8192]` (SecureMediaService:110)

**Рекомендация:** Вынести в константы или конфигурацию.

### 4.3. Неиспользуемые импорты (предположительно)

- `ChatService.java:30` — `HashMap` (не используется прямо)
- `Controller/PaymentController.java` — проверь `import com.platform.service.ResilienceService;`
- `MediaController.java:5` — закомментированный импорт

### 4.4. Слаг "admins" в ChatController не создан

**Файл:** `controller/ChatController.java:190`

```java
return getMessagesByRoom("admins", limit, before, jwt); // позже создадим комнату admins
```

Требуется создать комнату с slug "admins" или удалить legacy код.

### 4.5. KeycloakService — @PostConstruct не защищён

**Файл:** `service/KeycloakService.java:41-48`

```java
@PostConstruct
public void validateConfig() {
    if (clientSecret == null || clientSecret.isBlank()) {
        throw new IllegalStateException("...");
    }
```

Валидация при старте, но если Keycloak недоступен, приложение упадёт. Возможно, так и задумано, но стоит обернуть в `@Bean` с conditional.

---

## 5. Архитектурные метрики

| Метрика | Значение | Статус |
|---------|----------|--------|
| Java files | 152 | ✓ |
| Test files | 141 | ✓ (93% coverage ratio) |
| Services (конкретные) | 34 | — |
| Services (интерфейсы) | 1 (StorageService) | ✗ |
| Средняя длина сервиса | ~150 строк | ✓ |
| Самая длинная функция | SecureMediaService.uploadSecureMedia() — 130 строк | ✗ |
| God-классы (>= 350 строк) | ChatService (544), ChatWebSocketHandler (363), SecureMediaService (363), ChatController (363) | ✗ |
| Пакетов | 15 | ✓ |
| Exception types | 12 + 1 ErrorResponse | ✓/✗ (ErrorResponse не используется) |
| Config classes | 13 | ✓ |
| @Transactional | 40+ методов | ✓ |
| @Cacheable | 15+ методов | ✓ |
| @Timed/@Counted | 30+ методов (LiveBroadcastController, ChatService, LiveBroadcastService) | ✓ |

---

## 6. Резюме по категориям

### God-классы
1. **ChatService** (544 строки) — CRITICAL — чат + вопросы + архивация
2. **ChatWebSocketHandler** (363 строки) — HIGH — сессии + rate limiting + broadcast + retry
3. **SecureMediaService.uploadSecureMedia()** (130 строк) — HIGH — монолитная загрузка
4. **ChatController** (363 строки) — MEDIUM — legacy + новые + questions endpoints

### SOLID
- **SRP (Single Responsibility):** Нарушен в ChatService, ChatWebSocketHandler, SecureMediaService
- **OCP (Open-Closed):** Нарушен — хардкоженные роли не дают расширять без модификации
- **LSP (Liskov):** OK
- **ISP (Interface Segregation):** Нарушен — нет интерфейсов для сервисов
- **DIP (Dependency Inversion):** Нарушен — зависимости от конкретных классов

### Транзакции
- `SecureMediaService.uploadSecureMedia()` — I/O внутри `@Transactional` (watermark, encryption, S3)
- `ChatService.archiveAnsweredQuestions()` — Долгая транзакция с пейджингом (до 100+ save per page)
- `PaymentService.confirmPayment()` — Вызов Keycloak внутри транзакции (внешний HTTP I/O)
- `@Transactional(isolation = Isolation.READ_COMMITTED)` — Хорошо (PaymentService)

### N+1
- `PostService.toDto()` — для каждого поста: existsByPostIdAndUserIdAndType + getRecentComments
- `ChatService.archiveAnsweredQuestions()` — Потенциально N+1 при поиске ответов через `batchMessages.stream().anyMatch()`
- `AdminService.getStats()` — 5 отдельных count-запросов (можно оптимизировать в один)

### Дублирование
- **isAdmin()** — 8 копий в 7 сервисах
- **getUserRole(Jwt)** — минимум 5 копий
- **toDto()** — каждый сервис пишет свой converter: можно использовать MapStruct
- **Hardcoded roles** — SUPER_ADMIN, MODERATOR_ADMIN, VYACHESLAV в десятках мест

### Отсутствие интерфейсов
- 33 из 34 сервисов — конкретные классы
- Только `StorageService` — интерфейс с реализациями `S3StorageService`, `LocalStorageService`
- Невозможно mock-заменить сервис без Mockito

### DI
- В основном constructor injection через `@RequiredArgsConstructor` ✓
- `ChatWebSocketHandler` — field injection? Нет, constructor через `@RequiredArgsConstructor` ✓
- `LiveBroadcastService` — `@Qualifier("taskExecutor") private final Executor taskExecutor` — OK

### Архитектура слоёв
- Controller → Service → Repository: соблюдено ✓
- Service не вызывает другие Controller ✓
- DTO в model/dto/ — правильно изолированы ✓
- Нарушение: ChatWebSocketHandler (websocket) вызывает ChatService (service) — OK, cross-layer
- WebSocket не имеет service-прослойки, напрямую вызывает handler

### Error Handling
- 12 типов исключений + GlobalExceptionHandler ✓
- ErrorResponse определён, но не используется ✗
- Стандартизированный формат ошибок ОТСУТСТВУЕТ ✗
- Нет errorCode для маппинга на фронтенде
- Нет traceId для дебага в production
- `RuntimeException("Failed to upload media: " + e.getMessage())` в SecureMediaService — теряет root cause
- `Throwable` не обрабатывается (только `Exception`)
- `catch (Exception e)` в `handleGeneric()` — ловит всё, включая `RuntimeException`
