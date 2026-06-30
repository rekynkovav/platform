# План рефакторинга — Актуальный (30.06.2026)

## Статус: после полного аудита backend + frontend + infra
## Тесты: backend 1235/1235 ✅ | frontend 332/332 ✅

---

## 🔴 CRITICAL (1)

| # | Проблема | Файл | Описание |
|---|----------|------|----------|
| 1 | **0 React.memo** | все компоненты | LiveChat 1272стр, feeds, Dashboard перерисовываются полностью. 0 мемоизации на 16K строк |

## 🔴 HIGH (5)

| # | Проблема | Файл | Описание |
|---|----------|------|----------|
| 2 | **application-prod.yml:5** | application-prod.yml | Захардкоженный fallback пароль `platform_pass_2026` — если DB_PASSWORD не задан, прод работает с дефолтным |
| 3 | **Spring Boot 3.2.5 EOL** | pom.xml | Known CVE (Spring4Shell follow-ups, PathTraversal). Upgrade до 3.4.x |
| 4 | **11/17 сервисов без healthcheck** | docker-compose.yml | keycloak, nginx, prometheus, grafana, coturn, zipkin, alertmanager, backup, nginx-cdn, rtmp, frontend |
| 5 | **14/17 сервисов без resource limits** | docker-compose.yml | ES по умолчанию = 50% RAM. Нет mem_limit/cpus у绝大部分 сервисов |
| 6 | **.env.docker закоммичен** | .git/.env.docker | Не в .gitignore, содержит пароли |

## 🟡 MEDIUM (7)

| # | Проблема | Файл | Описание |
|---|----------|------|----------|
| 7 | **6 компонентов >500 строк** | ArchiveList(1501), LiveChat(1272), NutritionFeed(976), BroadcastControls(963), Dashboard(867), Questions(693) | God-components |
| 8 | **49 inline styles** | 21 файл | ~12 display:none (ок), ~5 динамические позиции, остальные → CSS классы |
| 9 | **28 эндпоинтов без пагинации** | ChatController, FeedController, EventController и др. | Возвращают `List<>` без лимита |
| 10 | **5 сервисов без тестов** | BroadcastChat, BroadcastReaction, DashboardMenu, KeycloakRetry, StreamingFile | Пробел в покрытии |
| 11 | **Keycloak Admin Client 24.0.3 EOL** | pom.xml | 26.x актуальная версия |
| 12 | **ES xpack.security=false** | docker-compose.yml | OK для dev, опасно для prod |
| 13 | **Keycloak realm — пароли в plaintext** | keycloak/ realm export | admin123, moderator123, password, vyacheslav123 |

## 🟢 LOW (4)

| # | Проблема | Файл | Описание |
|---|----------|------|----------|
| 14 | **3 пустых catch-блока** | BroadcastStatsWebSocketHandler.java:40,45,50 | Нужен минимум log.debug |
| 15 | **EnvValidator логирует DB URL** | EnvValidator.java:23 | INFO level — в prod раскрывает connection string |
| 16 | **9 сервисов без restart policy** | docker-compose.yml | prometheus, grafana, elasticsearch и др. |
| 17 | **Logback contrib 0.1.5** | pom.xml | Unmaintained с 2017, несовместим с SLF4J 2.x |

---

## ✅ ЧИСТО (подтверждено аудитом)

- 0 System.out.println в backend
- 0 TODO/FIXME/HACK
- 0 мёртвого кода (все private методы имеют callers)
- N+1 queries исправлены (PostService, ContactService)
- @Transactional у всех DB-сервисов
- XSS фильтр работает
- Rate limiting работает
- CSS variables 95% миграция
- 0 циклических зависимостей в frontend
- Frontend билд работает
- 4 `any` типа в продакшне (helpers.ts — throttle/debounce)
- Тесты покрывают все критические пути

---

## ПРИОРИТЕТЫ ВЫПОЛНЕНИЯ

### Раунд 1 — Безопасность (сейчас)
1. application-prod.yml: fallback пароли → `${DB_PASSWORD:?}` (fail fast)
2. .env.docker → добавить в .gitignore + git rm --cached
3. Keycloak realm: plaintext пароли → env vars

### Раунд 2 — Docker (сейчас)
4. Healthchecks: добавить keycloak, nginx, prometheus, grafana, frontend
5. Resource limits: добавить всем сервисам
6. Restart policies: добавить недостающим

### Раунд 3 — Performance (следующий)
7. React.memo: LiveChat, feeds, Dashboard — мемоизировать
8. Inline styles → CSS классы (5 динамических позиций + layout блоки)

### Раунд 4 — Code Quality
9. God-components: ArchiveList, LiveChat, NutritionFeed, BroadcastControls, Dashboard
10. Тесты: 5 сервисов без покрытия
11. Пагинация: критические эндпоинты

### Раунд 5 — Dependencies
12. Spring Boot 3.2.5 → 3.4.x
13. Keycloak Admin Client → 26.x
14. Logback contrib → удалить/заменить
