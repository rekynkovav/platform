# Анализ backend и инфраструктуры проекта

## 1. Backend (Spring Boot)

### 1.1. PlatformApplication.java
- **Корректно**: точка входа, @SpringBootApplication, @EnableScheduling, ShutdownHook.
- **Проблемы**:
  - Внутри `performGracefulShutdown()` используются `Thread.sleep(2000)` и `Thread.sleep(5000)` – это блокирует поток и не гарантирует корректного завершения WebSocket сессий и транзакций. Лучше использовать `ExecutorService` и `CountDownLatch`.
  - `GracefulShutdownCallback` закомментирован (фабрика Tomcat).
  - `cleanupExecutor` – однопоточный, но не используется нигде, кроме shutdown.
- **Рекомендации**: переписать shutdown с использованием Spring’s `@PreDestroy` и `WebSocketHandler.close()`.

### 1.2. application.properties
- **Проблемы**:
  - Повторяющиеся настройки multipart (сначала `100MB`, затем `10KB`) – последняя перезаписывает первую, поэтому загрузка файлов больше 10KB невозможна.
  - `enable_lazy_load_no_trans=true` – может привести к `LazyInitializationException` при обращении к неинициализированным связям вне транзакции (хотя Spring решает это автоматически).
  - `hibernate.read_only=true` – неправильно: делает все сессии только для чтения, что сломает запись.
- **Исправления**: удалить дублирующие `spring.servlet.multipart.*` (оставить только последние 10KB для чата, или вынести для всех). Исправить `read_only=false`.

### 1.3. application.yml
- **Проблемы**:
  - `bucket4j.enabled: false` – корректно (отключает bucket4j).
  - `aes-gcm/noPadding` – это правильный алгоритм, но `key-size: 256` должен быть согласован с `MEDIA_ENCRYPTION_KEY`.
- **Замечание**: `spring.cache.type: simple` – используется Caffeine, но `type: simple` означает ConcurrentHashMap, а не Caffeine. В pom.xml есть Caffeine, но конфигурация не использует его. Нужно изменить на `type: caffeine` или `cache.type: caffeine`.

### 1.4. application-dev.yml
- **Корректно**: включено подробное логирование, drop-first не установлен.

### 1.5. application-prod.yml
- **Проблемы**:
  - `allow-circular-references: false` – может сломать бины, если они циклически зависимы (например, SecurityConfig -> UserService -> SecurityConfig). Рекомендуется оставить `true` для Spring Boot 3.x.
  - `hibernate.format_sql: false` – нормально для прода.

### 1.6. application-staging.yml
- **Корректно**: параметры для стейджинга.

### 1.7. application-test.yml
- **Проблемы**:
  - Использует H2 в режиме PostgreSQL, но `ddl-auto: create-drop` – при каждом запуске тестов создаёт и удаляет таблицы, что медленно. Рекомендуется `validate`.
  - Отключает Liquibase – корректно для тестов, но seed-данные не вставляются.

### 1.8. logback-spring.xml
- **Проблемы**:
  - Использует `JsonEncoder` для консоли, но в `application.yml` и `application.properties` не задан `logging.pattern.console` – это нормально.
  - Имена файлов логов: `/app/logs/platform.log` – жёстко закодированы. Для Docker это ок, но для локальной разработки может не быть прав на запись.

### 1.9. Dockerfile
- **Проблемы**:
  - Builder использует `maven:3.9-eclipse-temurin-21`, runtime – `eclipse-temurin:17`. Это несовместимость, даже если сборка с target 17. Рекомендуется использовать одну версию JDK (17).
  - `ffmpeg` установлен в Dockerfile, но не используется в runtime? В platform.conf нет вызова ffmpeg, используется только в Java через ProcessBuilder. Это ок.
  - Отсутствует `HEALTHCHECK`.

## 2. Docker Compose и инфраструктура

### 2.1. docker/start.sh
- **Корректно**: проверка наличия Docker, генерация сертификатов, добавление hosts.

### 2.2. docker/stop.sh, clean.sh
- **Корректно**: простые bash-скрипты.

### 2.3. docker/nginx/nginx.conf
- **Корректно**: базовый Nginx с gzip, client_max_body_size 100M.

### 2.4. docker/nginx/conf.d/platform.conf
- **Проблемы**:
  - В секции HTTPS для `platform.local` отсутствует указание `proxy_set_header Upgrade $http_upgrade;` для WebSocket, но для `api.platform.local` оно есть.
  - Директива `location /media/` с `alias /uploads/` – если бэкенд загружает в `/app/uploads`, то нужно изменить путь.
  - `location /actuator/` разрешает только localhost – это нормально для безопасности.

### 2.5. docker/postgres/init.sql
- **Корректно**: создание расширений и привилегий.

### 2.6. docker/postgres/seed.sql
- **Пустой** – намеренно.

### 2.7. docker/postgres/seed-data.sql
- **Корректно**: вставка тестовых данных с `ON CONFLICT DO NOTHING`.

### 2.8. docker/keycloak/realm-export.json
- **Корректно**: 9 пользователей и роли.

### 2.9. SSL сертификаты (api.crt, auth.crt)
- **Самоподписанные** – только для разработки.

## 3. pom.xml

### Проблемы:
- **Версия JDK**: `maven.compiler.source` и `target` – 17, что соответствует runtime в Dockerfile (17). Но builder использует JDK 21 – это не критично, но не консистентно.
- **Зависимости**: `bucket4j-spring-boot-starter` и `bucket4j-spring-boot-starter-context` – дублируются, достаточно одного.
- **Тестирование**: jest-worker в frontend, не backend.
- **JaCoCo**: плагин настроен, отчёт будет создаваться.

### Рекомендации:
- Добавить `spring-boot-starter-webflux` для реактивного WebSocket? Нет, используется обычный Tomcat.
- Удалить `aws-java-sdk-core` (всегда тянется с `aws-java-sdk-s3`).

## 4. Миграции (Liquibase)

Миграции уже исправлены и проанализированы ранее. Осталась одна проблема:

- **v7** всё ещё может быть рискованной, если таблица `messages` уже партиционирована другими средствами. Добавлена проверка `IF NOT EXISTS`.

## 5. Синергия frontend ↔ backend

- **WebSocket**: frontend подключается к `ws://localhost:8081/ws/{channel}`, backend ожидает `/ws/chat` и `/ws/broadcast-stats`. В `useWebSocket.js` URL формируется как `${API_URL.replace(/^http/, 'ws').replace(/\/api$/, '')}/ws/${channel}`, который даёт `ws://localhost:8081/api/ws/general` (из-за того, что API_URL = `http://localhost:8081/api`). Это неверно!
  - **Исправление**: в `useWebSocket.js` нужно корректно формировать URL: `ws://localhost:8081/ws/${channel}`.
- **REST**: baseURL = `http://localhost:8081/api`, эндпоинты начинаются с `/broadcast/active` и т.д. – корректно.
- **Media**: frontend отправляет файлы на `/media/upload`, backend ожидает `/api/media/upload` (через nginx префикс `/api/`). Но при прямой разработке (без nginx) порт 8081 на backend, поэтому путь `/media/upload` не совпадёт с `/api/media/upload`. В `api.js` клиент использует baseURL, поэтому запрос идёт на `http://localhost:8081/api/media/upload` – это корректно (через nginx тоже, так как nginx удаляет префикс `/api/`).
- **Дэшборд**: `dashboardApi.saveMenuData` POST `/dashboard/menu` – контроллер stub, данные не сохраняются. Это архитектурная проблема.

## 6. Общие рекомендации

1. **Исправить URL WebSocket** в `useWebSocket.js`.
2. **Убрать дублирование multipart limits** в `application.properties`.
3. **Установить `spring.jpa.properties.hibernate.read_only=false`** (или удалить).
4. **Изменить `spring.cache.type` на `caffeine`**.
5. **Согласовать версии JDK** в Dockerfile (builder и runtime).
6. **Добавить `HEALTHCHECK`** в Dockerfile.
7. **Исправить `allow-circular-references: false`** в prod-yml на `true`.
8. **Проверить пути media** в nginx.conf.
9. **Добавить `preConditions`** для всех задач в миграциях, которые ещё не имеют.

Большинство из перечисленных проблем не изменяют внешний вид, поэтому могут быть исправлены.
