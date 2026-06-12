# Отчёт по безопасности проекта Platform

Дата проверки: 2026-06-12
Тип анализа: Статический анализ кода и конфигураций

---

## 1. Hardcoded Credentials (Учётные данные в коде)

### 1.1 [CRITICAL] Пароли пользователей Keycloak в открытом виде

| Файл | Строка | Уязвимость |
|------|--------|------------|
| `docker/keycloak/realm-export.json` | 54 | superadmin / admin123 |
| `docker/keycloak/realm-export.json` | 64 | moderator_admin / moderator123 |
| `docker/keycloak/realm-export.json` | 74 | user2 / password |
| `docker/keycloak/realm-export.json` | 84 | user1 / password |
| `docker/keycloak/realm-export.json` | 94 | user0 / password |
| `docker/keycloak/realm-export.json` | 104 | consultant_pitanie / pitanie123 |
| `docker/keycloak/realm-export.json` | 114 | consultant_sport / sport123 |
| `docker/keycloak/realm-export.json` | 124 | developer / dev123 |
| `docker/keycloak/realm-export.json` | 134 | vyacheslav / vyacheslav123 |

**Описание:** 9 пользователей с паролями в plaintext в JSON-файле импорта реалма. Пароли коммитятся в репозиторий. В корневом `docker/keycloak/realm-export.json` пароли помечены `temporary: true`, но в `backend/docker/keycloak/realm-export.json` они `temporary: false`.

**Рекомендация:** Удалить файлы realm-export.json из репозитория. Использовать Keycloak SPI для начальной загрузки пользователей, либо задавать пароли через переменные окружения при первом запуске.

### 1.2 [CRITICAL] Секрет клиента Keycloak backend в открытом виде

| Файл | Строка | Значение |
|------|--------|----------|
| `docker/keycloak/realm-export.json` | 37 | `"secret": "backend-secret-2026"` |
| `backend/docker/keycloak/realm-export.json` | 39 | `"secret": "backend-secret-2026"` |

**Описание:** Client secret для `platform-backend` (confidential client) захардкожен в реалм-экспорте.

**Рекомендация:** Генерировать секрет динамически или задавать через переменную окружения при импорте.

### 1.3 [HIGH] Админский пароль Keycloak в docker-compose.yml

| Файл | Строка | Значение |
|------|--------|----------|
| `docker-compose.yml` | 41 | `KEYCLOAK_ADMIN_PASSWORD: admin` |

**Описание:** Пароль администратора Keycloak (`admin`) в открытом виде в docker-compose.yml.

**Рекомендация:** Вынести в `${KEYCLOAK_ADMIN_PASSWORD:-...}` и задавать при деплое через .env или secrets.

### 1.4 [HIGH] Пароль PostgreSQL в docker-compose.yml

| Файл | Строка | Значение |
|------|--------|----------|
| `docker-compose.yml` | 10 | `POSTGRES_PASSWORD: platform_pass_2026` |
| `docker-compose.yml` | 33 | `KC_DB_PASSWORD: platform_pass_2026` |
| `docker-compose.yml` | 107 | `DB_PASSWORD: platform_pass_2026` |
| `docker-compose.yml` | 281 | `PGPASSWORD=platform_pass_2026` |

**Описание:** Пароль БД встречается 4 раза в открытом виде, в т.ч. в сервисе backup через переменную окружения `PGPASSWORD`.

**Рекомендация:** Использовать Docker secrets или `!secret` в docker-compose. Пароль не должен хардкодиться.

### 1.5 [MEDIUM] Пароль Redis в docker-compose.yml

| Файл | Строка | Значение |
|------|--------|----------|
| `docker-compose.yml` | 58 | `redis-server --requirepass ${REDIS_PASSWORD:-redis_pass}` |
| `docker-compose.yml` | 60 | `REDIS_PASSWORD=${REDIS_PASSWORD:-redis_pass}` |
| `docker-compose.yml` | 114 | `REDIS_PASSWORD: ${REDIS_PASSWORD:-redis_pass}` |

**Описание:** Пароль по умолчанию `redis_pass` — слабый дефолт. В production может забыться заменой.

**Рекомендация:** Убрать дефолт (`${REDIS_PASSWORD:?must be set}`) и задавать явно.

### 1.6 [MEDIUM] Admin пароль Grafana

| Файл | Строка | Значение |
|------|--------|----------|
| `docker-compose.yml` | 194 | `GF_SECURITY_ADMIN_PASSWORD=admin` |

**Описание:** Пароль admin Grafana = `admin`.

**Рекомендация:** Задать сложный пароль через переменные окружения.

### 1.7 [MEDIUM] Учётные данные Coturn

| Файл | Строка | Значение |
|------|--------|----------|
| `docker-compose.yml` | 313 | `--user=platform:platform123` |

**Описание:** Учётные данные TURN-сервера в открытом виде.

---

## 2. CSRF — Полностью отключен

### 2.1 [HIGH] CSRF protection disabled

| Файл | Строка | Код |
|------|--------|-----|
| `SecurityConfig.java` | 40 | `.csrf(csrf -> csrf.disable())` |

**Описание:** CSRF-защита Spring Security полностью отключена. Атаки CSRF возможны на все endpoints, включая админские (broadcast create/delete, media upload, админ-панель). Хотя при stateless JWT-аутентификации CSRF обычно не нужен, здесь есть публичные endpoints, работающие с сессионными cookie не через Bearer.

**Осложнение:** CORS настроен с `allowCredentials: true` и разрешёнными origin'ами. Если в будущем появятся cookie-сессии (например, для websocket), CSRF-атаки станут возможны.

**Рекомендация:** Если нет необходимости в CSRF (чистый Bearer-токен), то можно оставить, но зафиксировать в комментарии rationale. Добавить `SameSite=Strict` или `Lax` для всех cookie.

---

## 3. CORS — Чрезмерно разрешительная конфигурация

### 3.1 [MEDIUM] allowCredentials: true при кастомных origin'ах

| Файл | Строка | Код |
|------|--------|-----|
| `SecurityConfig.java` | 120-123 | `setAllowedOrigins(...)` + `setAllowCredentials(true)` |

**Описание:** Spring Security *запрещает* `allowCredentials(true)` в комбинации с `allowedOrigins("*")`. Здесь origin'ы заданы списком, так что формально это валидно. Однако `allowCredentials(true)` означает, что браузер будет отправлять куки и авторизационные заголовки на все разрешённые origin'ы. При неверно настроенном списке origin'ов (например, если туда попадёт `null` или `*`) — уязвимость.

**Рекомендация:** Убедиться, что `allowedOrigins` всегда задан явно и не содержит wildcard. Рассмотреть `allowedOriginPatterns` вместо `allowedOrigins`.

### 3.2 [LOW] Чрезмерно разрешённые методы и заголовки

| Файл | Строка | Код |
|------|--------|-----|
| `SecurityConfig.java` | 121-122 | Все методы + Authorization, Content-Type, X-Requested-With |

**Описание:** Разрешены все основные HTTP-методы и все нужные заголовки, что нормально для REST API. Безопасность нарушена не будет, но стоит проверить, нужны ли PUT/DELETE на публичных endpoints.

**Рекомендация:** Ограничить методы для публичных endpoints, если это возможно.

---

## 4. Keycloak Configuration

### 4.1 [HIGH] directAccessGrantsEnabled: true на публичном клиенте

| Файл | Строка | Клиент |
|------|--------|--------|
| `docker/keycloak/realm-export.json` | 31 | `platform-frontend` (public) |
| `docker/keycloak/realm-export.json` | 43 | `platform-backend` (confidential) |

**Описание:** OAuth2 Resource Owner Password Credentials (direct grants) включён на обоих клиентах. Это позволяет отправлять логин+пароль напрямую на `/token`. На публичном клиенте (`platform-frontend`) это особенно опасно — нет client secret, любой может имперсонировать клиент и пытаться подобрать пароль.

**Impact:** Brute-force атаки на пароли напрямую через Keycloak token endpoint.

**Рекомендация:** Отключить `directAccessGrantsEnabled` на публичном клиенте `platform-frontend`. Использовать Authorization Code flow с PKCE.

### 4.2 [MEDIUM] KEYCLOAK_SECRET пустой в application.yml

| Файл | Строка | Код |
|------|--------|-----|
| `application.yml` | 95 | `secret: ${KEYCLOAK_SECRET:}` |

**Описание:** Значение по умолчанию — пустая строка. `KeycloakService.validateConfig()` проверяет это и кидает `IllegalStateException`, но только если сервис реально используется. Если `KeycloakService` не загружен (например, profile dev), приложение запустится без secret.

**Рекомендация:** Использовать `${KEYCLOAK_SECRET:?must be set}` — тогда приложение упадёт на старте, если secret не задан.

### 4.3 [MEDIUM] verifyEmail: false в одной из версий realm-export

| Файл | Строка | Значение |
|------|--------|----------|
| `backend/docker/keycloak/realm-export.json` | 10 | `verifyEmail: false` |
| `docker/keycloak/realm-export.json` | 10 | `verifyEmail: true` |

**Описание:** Два разных realm-export файла с разными настройками. Тот, что используется в docker-compose (`./docker/keycloak/realm-export.json`), имеет `verifyEmail: true`. Но `backend/docker/` версия имеет `verifyEmail: false`. Это может привести к путанице — какой файл реально используется.

**Рекомендация:** Удалить дублирующийся файл `backend/docker/keycloak/realm-export.json`, оставить только один источник правды.

### 4.4 [MEDIUM] registrationAllowed зависит от файла

| Файл | Строка | Значение |
|------|--------|----------|
| `docker/keycloak/realm-export.json` | 7 | `registrationAllowed: false` |
| `backend/docker/keycloak/realm-export.json` | 7 | `registrationAllowed: true` |

**Описание:** В файле, который монтируется в docker-compose — `registrationAllowed: false`, что правильно. Но дублирующий файл разрешает регистрацию.

---

## 5. Webhook Security — RTMP

### 5.1 [CRITICAL] Webhook секрет не обязателен — fallback без аутентификации

| Файл | Строка | Код |
|------|--------|-----|
| `LiveBroadcastController.java` | 200-213 | `/webhook/rtmp` — секрет проверяется только если `RTMP_WEBHOOK_SECRET` задан |

```java
String expectedSecret = System.getenv("RTMP_WEBHOOK_SECRET");
if (expectedSecret == null || expectedSecret.isBlank()) {
    log.warn("RTMP_WEBHOOK_SECRET not configured - webhook security disabled");
} else {
    if (secret == null || !secret.equals(expectedSecret)) {
        log.warn("Invalid or missing webhook secret");
        return ResponseEntity.status(403).build();
    }
}
```

**Описание:** Если `RTMP_WEBHOOK_SECRET` не задан (а в коде нигде не задан — нет env в docker-compose для backend), webhook endpoint полностью открыт:

- `POST /api/broadcast/webhook/rtmp` — без аутентификации
- Любой может вызвать `onStreamStart` / `onStreamStop` для любого streamKey
- RTMP сервер (`on_publish http://backend:8081/api/broadcast/webhook/rtmp`) тоже не передаёт секрет

**Impact:** Злоумышленник может манипулировать статусами трансляций — запускать/останавливать чужие стримы, создавать DoS-условия.

**Рекомендации:**
1. Добавить `RTMP_WEBHOOK_SECRET` в `environment` backend в docker-compose.yml
2. Сделать проверку обязательной — если секрет не задан, возвращать 403/500, а не `warn` + пропуск
3. Добавить подпись запросов (HMAC) от RTMP-сервера
4. Ограничить endpoint только внутренней сетью Docker (сейчас endpoint публичный, доступный через nginx)

### 5.2 [MEDIUM] RTMP on_publish использует внутренний URL без проверки

| Файл | Строка | URL |
|------|--------|-----|
| `docker/rtmp.conf` | 22-23 | `http://backend:8081/api/broadcast/webhook/rtmp` |

**Описание:** RTMP server вызывает webhook через Docker internal network, но endpoint также доступен снаружи через nginx (если не запрещено location-блоком).

---

## 6. OAuth2 / JWT Конфигурация

### 6.1 [MEDIUM] Роль "VYACHESLAV" как отдельная роль в ролевой иерархии

| Файл | Строка | Иерархия |
|------|--------|----------|
| `SecurityConfig.java` | 76-86 | `ROLE_SUPER_ADMIN > ROLE_VYACHESLAV > ROLE_USER_*` |

**Описание:** "VYACHESLAV" — это имя пользователя (владельца), вынесенное в отдельную роль с собственными permission'ами. Если этот пользователь сменит имя или будет удалён, система останется с неуправляемой ролью без владельца.

**Рекомендация:** Использовать проверку на `SUPER_ADMIN` вместо персональной роли для владельца.

### 6.2 [LOW] JWT principal — preferred_username вместо sub

| Файл | Строка | Код |
|------|--------|-----|
| `SecurityConfig.java` | 112 | `jwtConverter.setPrincipalClaimName("preferred_username")` |

**Описание:** Использование `preferred_username` как principal — username может меняться. Стандартный подход — `sub` (UUID пользователя).

**Рекомендация:** Использовать `sub` claim, он стабилен.

### 6.3 [LOW] Отсутствует проверка audience в JWT

**Описание:** В JWT конвертере нет проверки `aud` claim. Токен, выпущенный для другого client_id, может быть принят.

**Рекомендация:** Добавить JwtValidator с проверкой `aud` на `platform-backend`.

---

## 7. Media Encryption

### 7.1 [HIGH] MEDIA_ENCRYPTION_KEY пустой/слабый

| Файл | Строка | Значение |
|------|--------|----------|
| `application.yml` | 78 | `master-key: ${MEDIA_ENCRYPTION_KEY:}` |
| `.env` | — | `MEDIA_ENCRYPTION_KEY=change-me-in-production` |

**Описание:** В `application.yml` дефолтное значение — пустая строка. В `.env` написано `change-me-in-production`. Если ключ пуст, AES/GCM/NoPadding будет использовать пустой ключ — алгоритм не будет работать корректно, encrypt/decrypt может падать или шифровать нулевым ключом.

В docker-compose.yml, строка 115: `${MEDIA_ENCRYPTION_KEY:?MEDIA_ENCRYPTION_KEY must be set}` — это хорошо, но только для prod-профиля.

**Рекомендация:** Сделать ключ обязательным для всех профилей. В `application.yml` тоже использовать `:?must be set`.

---

## 8. Платёжные данные в открытом виде

### 8.1 [HIGH] Платёжные реквизиты в конфигурации

| Файл | Строка | Данные |
|------|--------|--------|
| `application.yml` | 82-88 | Расчётный счёт, БИК, корр. счёт, ИНН, КПП |

```yaml
account: ${PAYMENT_ACCOUNT:40703810512000002359}
bik: ${PAYMENT_BIK:041909644}
inn: ${PAYMENT_INN:9715470089}
kpp: ${PAYMENT_KPP:771501001}
```

**Описание:** Платёжные реквизиты организации (р/с, БИК, ИНН, КПП) в открытом виде в YAML-файле. Эти данные не секретны по своей природе (обычно публикуются на сайте), но их коммит в репозиторий — плохая практика. Более серьёзно: если это dev-данные, то в production они могут быть другими, и есть риск утечки реальных реквизитов.

**Рекомендация:** Убрать дефолтные значения оставить только переменные окружения: `${PAYMENT_ACCOUNT:?must be set}`.

---

## 9. Валидация входных данных

### 9.1 [MEDIUM] Нет ограничения на размер broadcast ID (Path Traversal)

| Файл | Строка | Код |
|------|--------|-----|
| `LiveBroadcastController.java` | 88, 106, 141, 151, 168, 194 | `@PathVariable String id` |

**Описание:** Все `@PathVariable` не валидируются на формат. Если broadcast ID используется в файловой системе (например, для HLS плейлистов), возможна Path Traversal атака.

**Рекомендация:** Добавить `@Pattern(regexp = "^[0-9a-f-]+$")` для UUID-like ID, или проверять на `.` и `/`.

### 9.2 [MEDIUM] RTMP webhook payload не валидируется

| Файл | Строка | Код |
|------|--------|-----|
| `LiveBroadcastController.java` | 202 | `@RequestBody Map<String, String> payload` |

**Описание:** Webhook принимает сырой Map без валидации. Нет проверки на наличие ключей `action` и `streamKey` — если они null, `broadcastService.onStreamStart(null)` может привести к NPE или неожиданному поведению.

**Рекомендация:** Создать DTO с `@NotNull` валидацией и использовать `@jakarta.validation.Valid`.

---

## 10. Actuator / Monitoring

### 10.1 [MEDIUM] Actuator endpoints открыты для всех

| Файл | Строка | Код |
|------|--------|-----|
| `SecurityConfig.java` | 43 | `.requestMatchers("/actuator/**", "/health", ...).permitAll()` |

**Описание:** Все Actuator endpoints доступны без аутентификации. В `ProductionGuard.java` есть проверка на опасные endpoints (env, beans, heapdump), но они не запрещены — проверка только логирует ошибку.

**Рекомендация:** Разрешить только `/actuator/health` публично. Остальные actuator endpoints закрыть через `management.endpoints.web.exposure.include=health,info`.

---

## 11. JPA Configuration

### 11.1 [HIGH] ddl-auto: update в production

| Файл | Строка | Значение |
|------|--------|----------|
| `application.yml` | 18 | `ddl-auto: update` |

**Описание:** Hibernate `ddl-auto: update` автоматически изменяет схему БД при старте приложения. В production это может привести к:
- Непреднамеренному удалению данных
- Блокировкам таблиц при ALTER TABLE
- Неконтролируемым изменениям схемы

**Рекомендация:** В production использовать `ddl-auto: validate` или Flyway/Liquibase для миграций. Сейчас Liquibase отключен (`enabled: false`).

---

## 12. Дублирование realm-export.json

### 12.1 [MEDIUM] Два разных realm-export файла

| Файл | Версия |
|------|--------|
| `docker/keycloak/realm-export.json` | Используется в docker-compose |
| `backend/docker/keycloak/realm-export.json` | Дубликат |

**Различия между файлами:**
- `verifyEmail`: true (root) vs false (backend)
- `registrationAllowed`: false (root) vs true (backend)
- `temporary` на паролях: true (root) vs false (backend)
- `duplicateEmailsAllowed`/`resetPasswordAllowed`: отсутствуют в root

**Рекомендация:** Удалить дублирующийся файл, оставить только один.

---

## 13. EnvValidator — Логирование чувствительных данных

### 13.1 [MEDIUM] Логирование URL и имени пользователя БД

| Файл | Строка | Код |
|------|--------|-----|
| `EnvValidator.java` | 52-55 | `log.info("   - Database URL: {}", dbUrl)` |

**Описание:** EnvValidator логирует URL БД и имя пользователя на уровне INFO. Это может привести к утечке internal-адресов и служебной информации в логах.

**Рекомендация:** Убрать детальное логирование конфигурации БД, оставить только факт успешной проверки.

---

## 14. ProductionGuard — Только логирование, не блокировка

### 14.1 [LOW] Опасные endpoints только логируются

| Файл | Строка | Код |
|------|--------|-----|
| `ProductionGuard.java` | 57-63 | `log.error(...)` + throw `IllegalStateException` |

**Описание:** ProductionGuard кидает исключение, что *останавливает* приложение, если обнаружена проблема. Это хорошо. Однако опасные endpoints (`env`, `beans`, `heapdump`) проверяются по `exposedEndpoints.contains()`, но `contains` будет true и для `heapdump_settings` или `env_internal`, давая false positive.

**Рекомендация:** Использовать точное совпадение или `Set<String>` для проверки.

---

## 15. Устаревший HTTP протокол

### 15.1 [MEDIUM] Keycloak JWK Set URI через HTTP

| Файл | Строка | URL |
|------|--------|-----|
| `application.yml` | 41 | `jwk-set-uri: ${KEYCLOAK_JWK_SET_URI:http://keycloak:8080/...}` |

**Описание:** JWK Set URI использует HTTP (внутренняя Docker network). Это приемлемо внутри Docker, но если issuer-uri (строка 40) меняется на внешний HTTPS URL, JWK Set должен тоже быть HTTPS.

---

## Сводка по severity

| Severity | Количество |
|----------|-----------|
| CRITICAL | 2 |
| HIGH | 6 |
| MEDIUM | 12 |
| LOW | 3 |
| **Total** | **23** |

### Критические (немедленное исправление):
1. Пароли пользователей в открытом виде в realm-export.json (9 пользователей)
2. Webhook RTMP без обязательной аутентификации

### Высокие (приоритетное исправление):
3. Client secret в открытом виде в realm-export.json
4. Admin пароль Keycloak в docker-compose.yml
5. Пароль PostgreSQL в открытом виде
6. CSRF полностью отключен
7. Пустой MEDIA_ENCRYPTION_KEY
8. Платёжные реквизиты в коде
9. ddl-auto: update в production
10. directAccessGrantsEnabled на публичном клиенте

**Итого: 23 уязвимости, из них 2 CRITICAL, 6 HIGH, 12 MEDIUM, 3 LOW**
