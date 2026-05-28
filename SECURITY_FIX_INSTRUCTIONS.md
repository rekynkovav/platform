# 🔒 ИНСТРУКЦИЯ ПО ИСПРАВЛЕНИЮ ПРОБЛЕМ БЕЗОПАСНОСТИ
## Platform Security Audit — Полное руководство для AI-агента

> **Дата аудита:** 2026-05-27  
> **Всего проблем:** 30 (8 критических, 14 средних, 8 низких)  
> **Файлов затронуто:** ~25  
> **Приоритет выполнения:** P0 → P1 → P2

---

## 📋 ОГЛАВЛЕНИЕ

1. [P0-1: WebRTCController — закрыть доступ](#p0-1)
2. [P0-2: NotificationController — IDOR уязвимость](#p0-2)
3. [P0-3: RTMP Webhook — нет аутентификации](#p0-3)
4. [P0-4: LiveBroadcastController — утечка сущности](#p0-4)
5. [P0-5: XSS через innerHTML в ArchiveList.js](#p0-5)
6. [P0-6: JWT токен в WebSocket URL](#p0-6)
7. [P0-7: Клиентская авторизация без серверной проверки](#p0-7)
8. [P0-8: MediaRenderer — URL injection](#p0-8)
9. [P1-1: Раскомментировать Rate Limiting](#p1-1)
10. [P1-2: FileValidator — проверка magic bytes](#p1-2)
11. [P1-3: XssSanitizer — заменить на whitelist](#p1-3)
12. [P1-4: IpRateLimitFilter — IP spoofing](#p1-4)
13. [P1-5: BruteForceProtectionFilter — интеграция](#p1-5)
14. [P1-6: console.log в production](#p1-6)
15. [P1-7: REACT_APP_USE_MOCK_AUTH защита](#p1-7)
16. [P2: Низкоприоритетные исправления](#p2)
17. [САМОПРОВЕРКИ](#self-checks)

---

<a id="p0-1"></a>
## 🔴 P0-1: WebRTCController — полностью открыт без аутентификации

**Файл:** `backend/src/main/java/com/platform/controller/WebRTCController.java`

### Проблема
- Нет `@PreAuthorize` — любой анонимный пользователь может обращаться
- Нет `@AuthenticationPrincipal Jwt` — невозможно определить пользователя
- `@RequestBody Map<String, String>` без `@Valid` — нет валидации входных данных
- `ConcurrentHashMap` хранит SDP в памяти — DoS через заполнение, подмена чужих сессий
- Возвращает заглушку `"generated-answer-sdp"` — функционал не реализован

### Что исправить

Заменить содержимое файла на:

 

### ✅ Самопроверка
1. Файл компилируется без ошибок
2. Все эндпоинты имеют `@PreAuthorize("isAuthenticated()")`
3. `@Valid` присутствует на `@RequestBody`
4. Есть ограничение `MAX_SESSIONS`
5. `getIceCandidates` проверяет ownerId
6. Запрос `curl -X POST http://localhost:8081/api/webrtc/offer` без токена возвращает 401
7. Запрос с токеном, но без `broadcastId` в теле возвращает 400

---

<a id="p0-2"></a>
## 🔴 P0-2: NotificationController.markAsRead — IDOR уязвимость

**Файл:** `backend/src/main/java/com/platform/controller/NotificationController.java`

### Проблема
Строка 38: `@PutMapping("/{id}/read")` принимает `@PathVariable Long id`, но НЕ принимает `Jwt`. Любой авторизованный пользователь может пометить ЧУЖОЕ уведомление как прочитанное.

### Что исправить

Заменить метод `markAsRead` на:

 

Добавить в `NotificationService.java` метод:

 

### ✅ Самопроверка
1. Метод `markAsRead` теперь принимает `@AuthenticationPrincipal Jwt jwt`
2. `NotificationService` имеет метод `markAsReadForUser(Long, String)`
3. При попытке пометить чужое уведомление возвращается 403
4. Существующий метод `markAllAsRead` уже принимает Jwt — не изменён
5. Тест: запрос PUT `/api/notifications/999/read` с чужим userId возвращает 403 или 404

---

<a id="p0-3"></a>
## 🔴 P0-3: RTMP Webhook — нет аутентификации

**Файл:** `backend/src/main/java/com/platform/controller/LiveBroadcastController.java`

### Проблема
Строка 131: `@PostMapping("/webhook/rtmp")` — нет `@PreAuthorize`, нет проверки источника. Любой может отправить фейковый webhook.

### Что исправить

Добавить проверку HMAC-подписи или secret-token. Минимальный вариант:

 

Добавить в `application.yml`:
 

### ✅ Самопроверка
1. Запрос без заголовка `X-Webhook-Secret` возвращает 403 (если секрет настроен)
2. Запрос с неверным секретом возвращает 403
3. Запрос с верным секретом обрабатывается нормально
4. В логах появляется предупреждение если секрет не настроен
5. Переменная читается из env `RTMP_WEBHOOK_SECRET`

---

<a id="p0-4"></a>
## 🔴 P0-4: LiveBroadcastController — утечка полной сущности

**Файл:** `backend/src/main/java/com/platform/controller/LiveBroadcastController.java`

### Проблема
Методы `createBroadcast`, `getBroadcast`, `getActiveBroadcasts` возвращают `ResponseEntity<LiveBroadcast>` — полную JPA-сущность. Если сущность содержит `streamKey`, внутренние URL, ключи шифрования — они утекут.

### Что исправить

Создать DTO и использовать его вместо сущности:

1. Создать файл `backend/src/main/java/com/platform/model/dto/LiveBroadcastDto.java`:

 

2. Добавить метод маппинга в `LiveBroadcastService`:

 

3. Заменить возвращаемые типы в контроллере:
- `createBroadcast` → `ResponseEntity<LiveBroadcastDto>`
- `getBroadcast` → `ResponseEntity<LiveBroadcastDto>`
- `getActiveBroadcasts` → `ResponseEntity<List<LiveBroadcastDto>>`

### ✅ Самопроверка
1. Ответ JSON НЕ содержит полей `streamKey`, `internalStreamUrl`, `encryptionKey`
2. Все публичные поля DTO безопасны для клиента
3. Компиляция успешна
4. GET `/api/broadcast/active` возвращает массив DTO

---

<a id="p0-5"></a>
## 🔴 P0-5: XSS через innerHTML в ArchiveList.js

**Файл:** `frontend/src/components/Archive/ArchiveList.js`

### Проблема
~Строка 430: `previewModal.innerHTML = \`...\`` — пользовательские данные (`broadcast.title`, `broadcast.description`) вставляются в DOM без экранирования.

### Что исправить

Найти блок `handleStartLiveBroadcast` где создаётся `previewModal` и заменить `innerHTML` на безопасное создание элементов:

Заменить весь блок создания previewModal (от `previewModal.innerHTML = \`` до закрывающей обратной кавычки) на:

 

Также удалить строки:
 
(они теперь создаются выше через `document.createElement`)

### ✅ Самопроверка
1. В файле НЕТ строки `.innerHTML =`
2. Поиск по файлу: `grep -c "innerHTML" ArchiveList.js` возвращает 0
3. Создание трансляции с названием `<img src=x onerror=alert(1)>` НЕ вызывает alert
4. Предпросмотр работает корректно для audio и video
5. Кнопки «Начать» и «Отмена» функционируют

---

<a id="p0-6"></a>
## 🔴 P0-6: JWT токен передаётся в WebSocket URL

**Файл:** `frontend/src/hooks/useWebSocket.js`

### Проблема
Строка 27: `const wsUrl = \`${WS_URL}/chat?room=${roomSlug}&token=${encodeURIComponent(token)}\``
Токен попадает в access-логи nginx, историю браузера, Referer-заголовки.

### Что исправить

**Шаг 1 — Frontend:** Убрать токен из URL, отправить первым сообщением после connect:

Заменить функцию `connect` в `useWebSocket.js`:

 

**Шаг 2 — Backend:** Обновить `ChatWebSocketHandler.java` для обработки AUTH-сообщения.

В `backend/src/main/java/com/platform/websocket/ChatWebSocketHandler.java` добавить обработку:

 

**Шаг 3 — Backend:** Убрать fallback на query param в `WebSocketHandshakeInterceptor.java`:

Удалить строки 44-48 (блок «Приоритет 2: Query parameter»):
 

И изменить логику: если токен не найден в заголовке — разрешить handshake (аутентификация произойдёт через первое сообщение), но пометить сессию как неаутентифицированную:

 

### ✅ Самопроверка
1. В `useWebSocket.js` НЕТ строки `token=` в URL
2. После `ws.onopen` отправляется `{type: "AUTH", token: "..."}`
3. В `WebSocketHandshakeInterceptor` НЕТ блока `query.contains("token=")`
4. В `ChatWebSocketHandler` обрабатывается сообщение типа `AUTH`
5. Неаутентифицированные сообщения отклоняются
6. Логи nginx НЕ содержат JWT-токены
7. Чат работает корректно после аутентификации

---

<a id="p0-7"></a>
## 🔴 P0-7: Клиентская авторизация без серверной проверки

**Файлы (frontend):**
- `components/Archive/ArchiveList.js`
- `components/Dashboard/Dashboard.js`
- `components/Events/EventsHub.js`
- `components/Feed/MainFeed.js`
- `components/Scheduled/ScheduledEvents.js`
- `context/live/LiveSessionContext.tsx`

### Проблема
Все эти файлы используют `sessionStorage.getItem('demoRole')` для определения прав доступа. В production это защищено проверкой `REACT_APP_ENVIRONMENT === 'development'`, но:
- Если переменная случайно = `development` в prod → вся защита падает
- Сервер не проверяет действия, инициированные клиентом

### Что исправить

**Это архитектурная проблема.** Полное исправление требует изменения всех API-эндпоинтов. Минимальные шаги:

1. **Во всех 6 файлах** найти паттерн:
 

Заменить на:
 

2. **Добавить ProductionGuard на фронтенд** — создать `frontend/src/utils/securityGuard.js`:

 

Вызвать в `App.js` / `App.tsx` в самом начале компонента.

### ✅ Самопроверка
1. В production-сборке `sessionStorage.getItem('demoRole')` возвращает null
2. Установка `REACT_APP_ENVIRONMENT=production` + наличие `demoRole` в storage → автоматическое удаление
3. Все мутации (удаление, редактирование) проходят серверную проверку через `@PreAuthorize`

---

<a id="p0-8"></a>
## 🔴 P0-8: MediaRenderer — URL injection в <video>/<img>

**Файл:** `frontend/src/components/Chat/MediaRenderer.js`

### Проблема
`streamUrl` берётся из API и напрямую подставляется в `src` атрибутов `<video>`, `<img>`, `<audio>`. Если API возвращает `javascript:` URL — возможна XSS. Защита есть только в `renderFile()`, но НЕ в `renderVideo()`, `renderImage()`, `renderAudio()`.

### Что исправить

Добавить функцию валидации URL в начало файла:

 

Применить в каждом рендерере:

В `renderVideo()`:
 

В `renderImage()`:
 

В `renderAudio()` добавить аналогичную проверку перед использованием `streamUrl`.

### ✅ Самопроверка
1. `isValidMediaUrl('javascript:alert(1)')` возвращает `false`
2. `isValidMediaUrl('https://example.com/video.mp4')` возвращает `true`
3. `isValidMediaUrl('/api/media/stream/123')` возвращает `true`
4. `isValidMediaUrl('data:text/html,...')` возвращает `false`
5. Видео/изображения с невалидным URL показывают ошибку вместо выполнения кода

---

<a id="p1-1"></a>
## 🟡 P1-1: Раскомментировать Rate Limiting на всех эндпоинтах

**Файлы:**
- `backend/src/main/java/com/platform/controller/MediaController.java` (8 эндпоинтов)
- `backend/src/main/java/com/platform/controller/ChatController.java` (все эндпоинты)
- `backend/src/main/java/com/platform/controller/NotificationController.java` (4 эндпоинта)

### Что исправить

1. В `application.yml` убрать исключение Bucket4j:

Найти:
 

Заменить на:
 

2. Во всех трёх контроллерах раскомментировать `@RateLimiting` аннотации:

Заменить `// @RateLimiting(name = "media-limit")` на `@RateLimiting(name = "media-limit")`
Заменить `// @RateLimiting(name = "chat-limit")` на `@RateLimiting(name = "chat-limit")`
Заменить `// @RateLimiting(name = "notification-limit")` на `@RateLimiting(name = "notification-limit")`

3. Добавить конфигурацию лимитов в `application.yml`:

 

### ✅ Самопроверка
1. Приложение стартует без ошибок
2. Превышение лимита возвращает HTTP 429
3. Заголовки `X-RateLimit-*` присутствуют в ответах
4. `grep -r "// @RateLimiting" backend/src` возвращает 0 результатов

---

<a id="p1-2"></a>
## 🟡 P1-2: FileValidator — добавить проверку magic bytes

**Файл:** `backend/src/main/java/com/platform/validation/FileValidator.java`

### Проблема
Проверяется только `Content-Type` из HTTP-заголовка, который клиент контролирует.

### Что исправить

Добавить проверку magic bytes после проверки Content-Type:

 

Вызвать `validateMagicBytes(file, type)` в конце метода `validate()` после проверки Content-Type.

### ✅ Самопроверка
1. Загрузка `.exe` с `Content-Type: image/png` отклоняется
2. Загрузка настоящего PNG проходит
3. Загрузка настоящего JPEG проходит
4. Ошибка содержит понятное сообщение

---

<a id="p1-3"></a>
## 🟡 P1-3: XssSanitizer — заменить blacklist на whitelist

**Файл:** `backend/src/main/java/com/platform/util/XssSanitizer.java`

### Проблема
Blacklist-подход обходим. OWASP рекомендует только whitelist.

### Что исправить

Заменить метод `sanitize` на whitelist-подход:

 

### ✅ Самопроверка
1. `<script>alert(1)</script>` → полностью экранировано
2. `<b>жирный</b>` → сохраняется
3. `<a href="javascript:alert(1)">link</a>` → экранировано (не https)
4. `<a href="https://example.com">link</a>` → сохраняется с `rel="noopener noreferrer"`
5. `<img src=x onerror=alert(1)>` → полностью экранировано
6. `onclick=`, `onerror=` и другие event handlers → экранированы

---

<a id="p1-4"></a>
## 🟡 P1-4: IpRateLimitFilter — IP spoofing через X-Forwarded-For

**Файл:** `backend/src/main/java/com/platform/filter/IpRateLimitFilter.java`

### Что исправить

Заменить метод `getClientIp` на версию с trusted proxy check:

 

Добавить в `application.yml`:
 

### ✅ Самопроверка
1. Запрос с `X-Forwarded-For: 1.2.3.4` от НЕ-trusted proxy → используется remoteAddr
2. Запрос с `X-Forwarded-For: 1.2.3.4` от trusted proxy → используется 1.2.3.4
3. Rate limit применяется к правильному IP

---

<a id="p1-5"></a>
## 🟡 P1-5: BruteForceProtectionFilter — интеграция с Keycloak

**Файл:** `backend/src/main/java/com/platform/security/BruteForceProtectionFilter.java`

### Проблема
Фильтр защищает `/login` POST, но приложение использует Keycloak OAuth2. Метод `loginFailed()` никогда не вызывается.

### Что исправить

Интегрировать с Spring Security authentication events. Создать listener:

 

Также обновить фильтр для защиты OAuth2 callback endpoint:

 

### ✅ Самопроверка
1. Listener регистрируется и получает события
2. Фильтр защищает OAuth2 endpoints
3. Логи фиксируют неудачные попытки входа

---

<a id="p1-6"></a>
## 🟡 P1-6: Удалить console.log из production frontend

**Файлы:** 18 файлов, 112+ вызовов console.log

### Что исправить

**Вариант A (рекомендуемый):** Добавить babel-plugin для удаления console в production.

В `frontend/package.json` добавить devDependency:
 

В `frontend/babel.config.js` или `.babelrc`:
 

**Вариант B (быстрый):** Добавить override в `frontend/src/index.js`:

 

### ✅ Самопроверка
1. `npm run build` завершается успешно
2. В production-бандле НЕТ вызовов console.log
3. В development console.log работает нормально

---

<a id="p1-7"></a>
## 🟡 P1-7: Защита REACT_APP_USE_MOCK_AUTH от попадания в production

**Файл:** `frontend/src/services/api.js`

### Что исправить

Добавить runtime-проверку:

 

Заменить все использования `USE_MOCK` на `EFFECTIVE_USE_MOCK`.

### ✅ Самопроверка
1. `REACT_APP_USE_MOCK_AUTH=true` + `NODE_ENV=production` → mock отключен
2. В development mock работает нормально
3. В консоли появляется SECURITY CRITICAL если misconfigured

---

<a id="p2"></a>
## 🟢 P2: Низкоприоритетные исправления

### P2-1: System.out/err → SLF4J
**Файлы:** `EnvValidator.java`, `ProductionGuard.java`, `ChatRoomService.java`
- Заменить `System.out.println` на `log.info`
- Заменить `System.err.println` на `log.error`
- Добавить `@Slf4j` аннотацию Lombok

### P2-2: Thread.sleep → ScheduledExecutorService
**Файлы:** `MemoryMonitor.java`, `PlatformApplication.java`
- Заменить `Thread.sleep()` на `ScheduledExecutorService.scheduleAtFixedRate()`

### P2-3: Дублирование файлов
- Объединить `App.js` и `App.tsx` → оставить один
- Объединить `keycloak.js` и `keycloak.ts` → оставить один
- Объединить `api.js` и `api.ts` → оставить один

### P2-4: alert() → toast-уведомления
- 78 вызовов alert() в 15 файлах
- Создать единый компонент `<Toast />` и заменить все alert()

---

<a id="self-checks"></a>
## 🔍 ОБЩИЕ САМОПРОВЕРКИ ПОСЛЕ ВСЕХ ИСПРАВЛЕНИЙ

### Автоматическая проверка

 

### Ручная проверка

1. Запустить приложение
2. Попробовать обратиться к `/api/webrtc/offer` без токена → 401
3. Попробовать PUT `/api/notifications/1/read` с чужим JWT → 403
4. Отправить RTMP webhook без секрета → 403
5. Загрузить `.exe` с Content-Type `image/png` → 400
6. Открыть чат → проверить что токен НЕ виден в URL WebSocket в DevTools
7. Создать трансляцию с XSS-названием → проверить предпросмотр
8. Превысить rate limit → получить 429

---

> **ВАЖНО:** После каждого P0-исправления запускайте компиляцию и базовые тесты. Не переходите к следующему пункту пока текущий не прошёл самопроверку.
