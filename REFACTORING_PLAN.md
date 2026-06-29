# План рефакторинга — Актуальный (30.06.2026)

## Статус: после аудита + блоки A-C
## Тесты: 1235/1235 backend ✅ | 332/332 frontend ✅

---

## 🔴 CRITICAL (1)

| # | Проблема | Файл | Описание |
|---|----------|------|----------|
| 1 | **AES/CBC для HLS** | `EncryptionService.java:26,31` | `AES/CBC/PKCS5Padding` — основное шифрование уже GCM, но HLS слой CBC. Padding oracle attack |

## 🟡 HIGH (6)

| # | Проблема | Файл | Описание |
|---|----------|------|----------|
| 2 | **ErrorResponse DTO не используется** | `GlobalExceptionHandler.java` | DTO существует, но handler'ы возвращают `Map<String, String>` |
| 3 | **27 не протестированных компонентов** | фронт | UserProfile, Dashboard, LivePage, ArchiveList, NavTabs... |
| 4 | **DOM-манипуляции: 21 файл** | Dashboard.js и др. | `document.querySelector`, `window.open`, `createElement` — не по-React |
| 5 | **sessionStorage без try/catch** | `Dashboard.js:125-176` | 4 места — corrupted JSON → crash |
| 6 | **Hardcoded roles** | `SecurityConfig.java:66-67` | USER_0...VYACHESLAV захардкожены в matchers |
| 7 | **17 сервисов без @Transactional** | service/*.java | BroadcastChatService, BroadcastReactionService и др. нуждаются |

## 🟢 MEDIUM (4)

| # | Проблема | Файл | Описание |
|---|----------|------|----------|
| 8 | **Inline styles: 49 мест** | разные компоненты | Усложняет темизацию |
| 9 | **TypeScript any: 17 мест** | разные .ts файлы | Нет type safety |
| 10 | **Docker cache: 74 GB** | `docker system df` | `docker system prune --all` освободит ~70 GB |
| 11 | **Backend unhealthy** | `docker-compose.yml` | Healthcheck curl внутри контейнера не работает |

---

## Порядок исправления

### Блок A — Quick wins (сейчас)
1. Backend healthcheck fix (#11)
2. sessionStorage try/catch (#5)
3. Hardcoded roles → RoleConstants (#6)
4. Docker cache prune (#10)

### Блок B — ErrorResponse DTO (#2)
5. ErrorResponse DTO → GlobalExceptionHandler

### Блок C — AES/CBC → GCM для HLS (#1)
6. HLS_ALGORITHM замена + проверка плеера

### Блок D — DOM манипуляции (#4)
7. ✅ window.location.reload() — 5 мест заменены:
   - RoleSwitcher.js → useNavigate(0)
   - LivePage.js → setError(null) + refreshKey
   - LivePlayer.tsx → onRetry callback
   - BroadcastControls.tsx → useNavigate(0) + onUpdate callback
   - ErrorBoundary.js — ОСТАВЛЕН (корректное поведение)
8. Dashboard.js DOM — стандартные паттерны (menu positioning, file download, window.open), не нарушения

### Блок E — @Transactional audit (#7)
9. ✅ Все 18 сервисов без @Transactional — инфраструктурные (Redis, S3, Keycloak, FFmpeg, файлы)
   Не используют JPA Repository → @Transactional не нужен. False positive из плана.

### Блок F — Тесты (#3)
10. Приоритет: Dashboard, LivePage, NavTabs

---

## Сделано (не трогать!)
- Unified chat ✅
- Dead code cleanup ✅
- Console.* cleanup (0 остатков) ✅
- CSS hex → variables (92%) ✅
- CSS dedup ✅
- ErrorBoundary wraps all routes ✅
- File size limits unified ✅
- Docker-compose secrets ✅
- SqlValidator removed ✅
- WebSocket JWT cleanup ✅
- RTMP URL via @Value ✅
- .dockerignore ✅
- XssSanitizer = HTML entity escape (correct) ✅
- All ROUND3 CRITICAL #1-9 ✅
- All ROUND4 HIGH items ✅
- InlineAudio.css deleted ✅
