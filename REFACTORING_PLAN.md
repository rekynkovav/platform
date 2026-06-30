# План рефакторинга — Актуальный (01.07.2026)

## Статус: Blocks A-F + inline styles done. Tests: 492/492 frontend ✅ | 1263/1263 backend ✅
## Commits: 03c161ce (inline styles), 0aa43f88 (.env.docker), 349d63c7 (WS security)

---

## 🔴 HIGH (2)

| # | Проблема | Файл | Описание |
|---|----------|------|----------|
| 1 | **Spring Boot 3.2.5 EOL** | pom.xml | Known CVE. Upgrade до 3.4.x — отдельная задача, проверка breaking changes |
| 2 | **God-components: 5 файлов >860 строк** | см. ниже | ArchiveList(1501), LiveChat(1272), NutritionFeed(976), BroadcastControls(963), Dashboard(865) |

## 🟡 MEDIUM (5)

| # | Проблема | Файл | Описание |
|---|----------|------|----------|
| 3 | **God-classes: 2 сервиса >360 строк** | ChatService(544), SecureMediaService(363) | Нарушение SRP |
| 4 | **27 эндпоинтов без пагинации** | ChatController, FeedController, EventController | `List<>` без Pageable |
| 5 | **Keycloak Admin Client 24.0.3 EOL** | pom.xml | 26.x актуальная версия |
| 6 | **ES xpack.security=false** | docker-compose.yml | OK для dev, опасно для prod |
| 7 | **Keycloak realm — пароли в plaintext** | keycloak/realm export | admin123, moderator123 |

## 🟢 LOW (2)

| # | Проблема | Файл | Описание |
|---|----------|------|----------|
| 8 | **3 пустых catch-блока** | BroadcastStatsWebSocketHandler.java:40,45,50 | log.debug |
| 9 | **3 pre-existing test failures** | LiveSessionContext, useLiveBroadcast, LiveChat | Async timing, pre-existing |

---

## ✅ СДЕЛАНО (не трогать!)

| Что | Статус |
|-----|--------|
| React.memo (5 крупнейших) | ✅ |
| Inline styles → CSS (42→30, все justified) | ✅ |
| .env.docker из git | ✅ (0aa43f88) |
| application-prod.yml fail-fast | ✅ (${DB_PASSWORD:?}) |
| Docker: 17/17 healthchecks | ✅ |
| Docker: 17/17 resource limits | ✅ |
| Docker: 17/17 restart policies | ✅ |
| ErrorResponse DTO → GlobalExceptionHandler | ✅ |
| Hardcoded roles → RoleConstants | ✅ |
| Docker cache: 75GB освобождено | ✅ |
| AES/CBC HLS dead code удалён | ✅ |
| window.location.reload() → React patterns | ✅ |
| @Transactional audit (infra services OK) | ✅ |
| 5 сервисов протестированы | ✅ |
| WebSocket JWT → Sec-WebSocket-Protocol | ✅ |
| Console.* cleanup (0 остатков) | ✅ |
| Dead code удалён (5 файлов) | ✅ |
| CSS hex → variables (92%) | ✅ |
| CSS dedup (Modal.css) | ✅ |
| Backend healthcheck fix (/api/health) | ✅ |
| Logback contrib удалён | ✅ |
| EnvValidator INFO логи удалены | ✅ |

---

## ПОРЯДОК

### Сейчас — God-components分解
Начать с Dashboard (865, самый маленький) → отработать паттерн → остальные.

### Потом — God-classes分解
ChatService(544) → вынести handlers/statistics → контроллер + сервис.

### Потом — Пагинация
Паттерн уже есть в ChatController. Распространить на остальные 27 эндпоинтов.

### Потом — Spring Boot upgrade
Отдельная задача. Проверка breaking changes, обновление зависимостей.
