# Путь Сердца — Платформа сообщества

Полнофункциональная социальная платформа для сообщества «Путь Сердца», объединяющая **React** фронтенд с **Java Spring Boot** бэкендом, развёрнутую через **Docker Compose** с аутентификацией через **Keycloak**, живыми трансляциями (RTMP/HLS), чатом в реальном времени (WebSocket) и полным стеком наблюдаемости (Prometheus, Grafana, ELK, Zipkin).

---

## Содержание

1. [Обзор проекта](#1-обзор-проекта)
2. [Технологический стек](#2-технологический-стек)
3. [Структура проекта](#3-структура-проекта)
4. [Быстрый старт](#4-быстрый-старт)
5. [Разработка](#5-разработка)
6. [Тестирование](#6-тестирование)
7. [Архитектура](#7-архитектура)
8. [API — REST-эндпоинты](#8-api--rest-эндпоинты)
9. [Развёртывание](#9-развёртывание)
10. [Контрибьюция](#10-контрибьюция)

---

## 1. Обзор проекта

**Путь Сердца** — русскоязычный портал сообщества, централизованный хаб для:

| Модуль | Описание |
|--------|----------|
| **Лента новостей** | Текстовые, аудио и видео посты с CRUD-администрированием, пагинацией и встроенным воспроизведением аудио |
| **Живые трансляции** | RTMP-ингест → HLS-доставка с чатом в реальном времени, тестированием устройств и воспроизведением архивов |
| **Чат-система** | Многокомнатные WebSocket чаты (общий + питание) с модерацией вопросов и ответов |
| **Мероприятия** | Запланированные события с материалами, обратной связью и уведомлениями |
| **Дашборд** | Навигационный хаб с меню, таймерами и контентными секциями |
| **Профили пользователей** | Управление личным профилем с управлением доступом на основе ролей |
| **Администрирование** | Переключение ролей, управление пользователями, статистика и модерация контента |
| **Платежи** | Создание платежей, генерация QR-кодов, отслеживание статусов |
| **Уведомления** | Система уведомлений с подсчётом непрочитанных и пометкой прочтения |

Весь интерфейс выполнен на русском языке.

---

## 2. Технологический стек

### Фронтенд (`/frontend`)

| Технология | Версия | Назначение |
|------------|--------|------------|
| React | 18.2.0 | UI-фреймворк |
| React Router | 6.20.0 | Клиентская маршрутизация |
| Axios | 1.7.2 | HTTP-клиент с перехватчиками |
| Keycloak JS | 24.0.3 | OIDC/OAuth2-аутентификация |
| React Window | 1.8.10 | Виртуализированный рендеринг списков |
| Webpack | 5.88.2 | Модульный сборщик (через CRA) |
| Lodash | 4.17.21 | Утилитарная библиотека |
| ESLint | 8.57.0 | Статический анализ |
| Prettier | 3.2.5 | Форматирование кода |

**UI**: Кастомный CSS (без UI-библиотеки). Поддержка тем через CSS-пользовательские свойства (атрибут `data-theme`).

### Бэкенд (`/backend`, Maven мультимодуль)

| Технология | Версия | Назначение |
|------------|--------|------------|
| Java | 17 | Среда выполнения |
| Spring Boot | 3.2.5 | Фреймворк приложения |
| Spring Security + OAuth2 | — | JWT-ресурсный сервер |
| Spring Data JPA + Hibernate | — | ORM базы данных |
| Spring WebSocket | — | Обмен сообщениями в реальном времени |
| PostgreSQL | 16 | Основная база данных |
| Redis | 7 | Кэш и хранилище сессий |
| Liquibase | — | Миграции базы данных |
| AWS SDK S3 | 1.12.720 | S3-совместимое хранилище медиа |
| Keycloak Admin Client | 24.0.3 | Серверное управление пользователями/ролями |
| Bucket4j | 0.12.10 | Ограничение частоты запросов |
| Caffeine | 3.1.8 | In-memory кэш |
| Micrometer + Prometheus | — | Экспорт метрик |
| Zipkin (Brave) | — | Распределённая трассировка |
| Logback JSON | — | Структурированное логирование |
| SpringDoc OpenAPI | 2.5.0 | Документация API (Swagger UI) |
| FFmpeg | — | Транскодирование медиа (HLS) |

### Инфраструктура (Docker Compose)

| Сервис | Образ | Роль |
|--------|-------|------|
| frontend | nginx:alpine | SPA-хостинг через nginx |
| backend | eclipse-temurin:17-jre-alpine | Java-сервер приложений |
| postgres | postgres:16-alpine | Реляционная база данных |
| redis | redis:7-alpine | Кэш и хранилище сессий |
| keycloak | quay.io/keycloak/keycloak:24.0.5 | IDP / SSO |
| nginx | nginx:1.26-alpine | Reverse proxy, SSL, ограничение частоты |
| nginx-cdn | nginx:1.26-alpine | CDN для HLS-сегментов |
| rtmp | alfg/nginx-rtmp | RTMP-ингест → HLS-транскодирование |
| coturn | coturn/coturn:4.6.2 | TURN-сервер для WebRTC |
| prometheus | prom/prometheus:v2.53.0 | Сбор метрик |
| grafana | grafana/grafana:11.1.0 | Дашборды мониторинга |
| elasticsearch | elasticsearch:8.11.0 | Хранение логов |
| logstash | logstash:8.11.0 | Доставка/парсинг логов |
| kibana | kibana/kibana:8.11.0 | Визуализация логов |
| zipkin | openzipkin/zipkin:3.4.0 | Распределённая трассировка |
| alertmanager | prom/alertmanager:v0.27.0 | Маршрутизация алертов |
| backup | postgres:15 | Регулярные резервные копии БД |

---

## 3. Структура проекта

### Верхнеуровневая структура

```
platform/
├── frontend/              # React SPA
│   ├── public/            # Статические ресурсы + index.html
│   ├── src/               # Исходный код
│   ├── build/             # Продакшн-билд
│   ├── Dockerfile         # Мультистадийная сборка (Node → nginx)
│   ├── nginx.conf         # Конфигурация nginx для фронтенда
│   └── package.json
├── backend/               # Spring Boot приложение
│   ├── src/
│   │   ├── main/java/com/platform/   # Исходный код
│   │   └── test/java/com/platform/   # Тесты
│   ├── Dockerfile         # Мультистадийная сборка (Maven → JRE)
│   └── pom.xml
├── docker/                # Конфигурации инфраструктуры
│   ├── nginx/             # Reverse proxy конфигурация
│   ├── postgres/          # init.sql
│   └── keycloak/          # realm-export.json
├── docker-compose.yml     # Оркестрация мульти-сервисов
├── pom.xml                # Корневой Maven POM (родитель)
├── prometheus.yml         # Конфигурация скрейпинга Prometheus
├── alertmanager.yml       # Правила маршрутизации алертов
├── alerts.yml             # Определения алертов
├── logstash.conf          # Конвейер Logstash
├── rtmp.conf              # Конфигурация RTMP → HLS-транскодирования
├── nginx-cdn.conf         # Конфигурация CDN nginx
├── scripts/               # Утилитарные скрипты (бэкап и т.д.)
└── backups/               # Каталог для дампов базы данных
```

### Пакеты бэкенда (`backend/src/main/java/com/platform/`)

```
├── aspect/          # AOP-аспекты (аудит, логирование)
├── config/          # Безопасность, Jackson, Resilience, ProductionGuard
├── controller/      # REST-контроллеры (18 штук)
├── dto/             # Data Transfer Objects
├── exception/       # Глобальный обработчик исключений
├── filter/          # Фильтры запросов
├── health/          # Проверки здоровья приложения
├── model/           # JPA-сущности (Post, Event, ChatMessage и др.)
│   └── dto/         # DTO для API-обмена
├── monitoring/      # Метрики и мониторинг
├── repository/      # JPA-репозитории
├── security/        # JWT-аутентификация и авторизация
├── service/         # Бизнес-логика (35 сервисов)
├── util/            # Утилиты (FFmpegExecutor и др.)
├── validation/      # Валидация входных данных
└── websocket/       # WebSocket-обработчики
```

### Структура фронтенда (`frontend/src/`)

```
├── App.js                    # Корневой компонент — инициализация аутентификации, маршрутизация вкладок
├── index.js                  # Точка входа — ThemeProvider + ErrorBoundary
├── keycloak.js               # Синглтон Keycloak (жизненный цикл аутентификации)
├── components/
│   ├── Navigation/           # NavTabs (панель вкладок с поддержкой разделения)
│   ├── Feed/                 # MainFeed, NutritionFeed
│   ├── Chat/                 # GeneralChat, NutritionChat, ChatRoom
│   ├── Dashboard/            # Dashboard, Timer
│   ├── Events/               # EventsHub
│   ├── Archive/              # ArchiveList
│   ├── live/                 # LiveBroadcastView, LivePlayer, LiveChat, BroadcastControls
│   ├── Notification/         # NotificationBell, GlobalEventNotification
│   ├── Admin/                # RoleSwitcher
│   ├── UserProfile/          # UserProfile
│   ├── Help/                 # Questions (FAQ)
│   ├── Scheduled/            # ScheduledEvents
│   └── common/               # ErrorBoundary, ConfirmModal, ProtectedRoute и др.
├── services/
│   ├── api.js                # Axios-клиент + модули всех эндпоинтов
│   └── mockApi.js            # Мок-API для офлайн-разработки
├── hooks/
│   ├── useWebSocket.js       # Управление подключением WebSocket
│   ├── usePermissions.js     # Управление доступом на основе ролей/уровней
│   ├── useAudioRecorder.js   # Запись с микрофона в браузере
│   ├── useTheme.js           # Переключение темы тёмная/светлая
│   ├── useRoles.js           # Управление состоянием ролей
│   └── live/                 # useLiveBroadcast, useLiveChat
├── context/
│   └── AudioPlayerContext.js # Глобальное состояние аудиоплеера
├── utils/                    # safeStorage, chatLogger, chatLogViewer
└── constants/                # fallbackPosts, roles
```

---

## 4. Быстрый старт

### Требования

- **Docker** 24+ и **Docker Compose** v2
- **Node.js** 20+ (для разработки фронтенда)
- **Java** 17+ и **Maven** 3.9+ (для разработки бэкенда)
- **Make** (опционально, для удобных целей)
- SSL-сертификаты для `platform.local`, `api.platform.local`, `auth.platform.local` (самоподписанные для разработки)

### Полный стек через Docker

```bash
# 1. Клонирование репозитория
git clone <repo-url> platform
cd platform

# 2. (Опционально) Создание .env файла из шаблона
cp .env.example .env
# Отредактируйте .env для задания необходимых секретов: MEDIA_ENCRYPTION_KEY, REDIS_PASSWORD и т.д.

# 3. Генерация самоподписанных SSL-сертификатов (для разработки)
mkdir -p docker/nginx/ssl
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout docker/nginx/ssl/platform.key \
  -out docker/nginx/ssl/platform.crt \
  -subj "/CN=platform.local"
# Повторите для api.platform.local и auth.platform.local

# 4. Сборка и запуск всех сервисов
docker compose build --no-cache
docker compose up -d

# 5. Проверка работоспособности стека
docker compose ps
# Все сервисы должны показывать "Up" или "healthy"
```

### Только фронтенд (автономная разработка)

```bash
cd frontend
npm install
# Запуск dev-сервера с горячей перезагрузкой (порт 3000)
npm start
# Или сборка для продакшена
npm run build
```

> Dev-сервер ожидает Keycloak по адресу `https://auth.platform.local/auth`, а API по адресу `https://api.platform.local/api`. Эти адреса можно переопределить через переменные окружения (см. `.env`).

### Только бэкенд (автономная разработка)

```bash
# Сборка модуля
cd backend
mvn clean install -DskipTests

# Запуск с профилем разработки
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Необходимую инфраструктуру (PostgreSQL, Redis, Keycloak) можно запустить отдельно:

```bash
docker compose up -d postgres redis keycloak
```

---

## 5. Разработка

### Переменные окружения

| Переменная | Значение по умолчанию | Описание |
|------------|----------------------|----------|
| `REACT_APP_KEYCLOAK_URL` | `https://auth.platform.local/auth` | URL сервера Keycloak |
| `REACT_APP_API_URL` | `https://api.platform.local/api` | Базовый URL API бэкенда |
| `REACT_APP_USE_MOCK_AUTH` | `false` | Использовать мок-аутентификацию вместо Keycloak |
| `REACT_APP_ENVIRONMENT` | `development` | Целевое окружение сборки |

### Основные NPM-команды (`/frontend/package.json`)

```bash
npm start          # Dev-сервер (горячая перезагрузка)
npm run build      # Продакшн-билд
npm run build:prod # Продакшн-билд с REACT_APP_ENVIRONMENT=production
npm test           # Запуск тестов
npm run lint       # Проверка ESLint
npm run format     # Форматирование Prettier
npm run analyze    # Анализ размера бандла через source-map-explorer
```

### Стиль кода

- **Фронтенд**: ESLint (конфигурация CRA по умолчанию) + Prettier
- **Бэкенд**: Стандартные соглашения Spring Boot, Lombok для POJO
- **Коммиты**: Описательные, на русском или английском языке

### Добавление новой вкладки

1. Создайте компонент в `frontend/src/components/<Категория>/`
2. Импортируйте его в `App.js` и добавьте case в `renderContent()` switch
3. Добавьте запись вкладки в массив tabs в `NavTabs.js`
4. При необходимости добавьте кнопку/элемент меню вкладки

---

## 6. Тестирование

### Фронтенд

Тесты используют **React Testing Library** и **Jest** (встроенный тестовый раннер CRA):

```bash
cd frontend
npm test                         # Запуск всех тестов (41 тестовый файл, 333 теста)
npm test -- --coverage           # Запуск с отчётом покрытия
```

Тестовые файлы расположены рядом с компонентами в директориях `__tests__/`:

- `__tests__/App.test.js`
- `__tests__/api.test.js`
- `__tests__/MainFeed.test.js`
- `__tests__/NavTabs.test.js`
- `__tests__/GeneralChat.test.js`
- `__tests__/Dashboard.test.js`
- `__tests__/LiveBroadcastView.test.js`
- `__tests__/usePermissions.test.js`
- `__tests__/useAudioRecorder.test.js`
- `__tests__/useWebSocket.test.js`
- `__tests__/AudioPlayerContext.test.js`

### Бэкенд

```bash
cd backend
mvn test                 # Запуск всех тестов (140 тестовых файлов, 1065 тестов)
mvn verify               # Полный цикл: сборка + тесты + проверки
```

---

## 7. Архитектура

### Схема системы

```
                         ┌──────────────┐
                         │   Интернет   │
                         └──────┬───────┘
                                │ :443
                         ┌──────▼───────┐
                         │   nginx       │  Reverse proxy, SSL, ограничение частоты
                         │ (platform)   │
                         └───┬───┬───┬──┘
                        ┌────┘   │   └────┐
                   ┌────▼──┐ ┌──▼───┐ ┌──▼────┐
                   │nginx  │ │ nginx │ │ nginx │
                   │cdn    │ │ front │ │ auth  │
                   │:8001  │ │  :3000│ │  :8080│
                   └───┬───┘ └──┬───┘ └──┬───┘
                       │        │        │
              ┌────────▼──┐ ┌──▼──┐ ┌───▼──────┐
              │ RTMP      │ │React│ │ Keycloak │
              │(rtmp)     │ │ SPA │ │ 24.0.5   │
              │ :1935/8000│ └─────┘ └───┬──────┘
              └─────┬─────┘            │
                    │ HLS              │ JDBC
              ┌─────▼─────┐      ┌─────▼──────┐
              │  Backend  │◄─────┤ PostgreSQL │
              │  :8081    │      │  16-alpine │
              │  Spring   │      └────────────┘
              │  Boot 3.2 │         ┌────────┐
              └─────┬─────┘         │ Redis  │
                    │               │   7    │
                    │               └────────┘
              ┌─────▼─────┐
              │  S3 / EBS │  Хранилище медиа
              └───────────┘
```

### Ключевые паттерны проектирования

- **Синглтон Keycloak** — JS-клиент Keycloak создаётся один раз и переиспользуется во всём приложении
- **Axios-перехватчики** — централизованное управление JWT и обработка ошибок
- **Кастомные хуки** — вся общая состоятельная логика (WebSocket, разрешения, аудио) вынесена в хуки
- **CSS-пользовательские свойства** — переключение тем через атрибут `data-theme` на `<html>`
- **Docker мультистадийные сборки** — фронтенд: сборка Node → обслуживание nginx; бэкенд: сборка Maven → среда JRE
- **WebSocket** — обмен сообщениями чата в реальном времени через Spring WebSocket
- **HLS-стриминг** — RTMP-ингест через nginx-rtmp с транскодированием FFmpeg в HLS
- **WebRTC** — TURN-сервер coturn для peer-to-peer видеосвязи

---

## 8. API — REST-эндпоинты

API доступно по префиксу `/api`. Все эндпоинты защищены JWT-аутентификацией (кроме `/api/health`).

Документация API доступна через Swagger UI при запуске приложения:
```
http://localhost:8081/swagger-ui.html
```

### Лента и посты

| Метод | Эндпоинт | Описание |
|-------|----------|----------|
| `GET` | `/api/feed/main` | Получить основную ленту |
| `GET` | `/api/feed/nutrition` | Получить ленту питания |
| `POST` | `/api/feed/posts` | Создать пост в ленте |
| `PUT` | `/api/feed/posts/{id}` | Обновить пост |
| `DELETE` | `/api/feed/posts/{id}` | Удалить пост |
| `GET` | `/api/posts/feed` | Получить ленту постов |
| `GET` | `/api/posts/{id}` | Получить пост по ID |
| `POST` | `/api/posts` | Создать пост |
| `PUT` | `/api/posts/{id}` | Обновить пост |
| `DELETE` | `/api/posts/{id}` | Удалить пост |
| `POST` | `/api/posts/media` | Загрузить медиа к посту |

### Реакции и комментарии

| Метод | Эндпоинт | Описание |
|-------|----------|----------|
| `POST` | `/api/posts/{postId}/reactions` | Добавить реакцию |
| `GET` | `/api/posts/{postId}/reactions` | Получить реакции |
| `GET` | `/api/posts/{postId}/comments` | Получить комментарии |
| `POST` | `/api/posts/{postId}/comments` | Добавить комментарий |
| `DELETE` | `/api/posts/{postId}/comments/{commentId}` | Удалить комментарий |

### Чат

| Метод | Эндпоинт | Описание |
|-------|----------|----------|
| `GET` | `/api/chat/rooms` | Список комнат чата |
| `GET` | `/api/chat/{roomSlug}` | Информация о комнате |
| `GET` | `/api/chat/{roomSlug}/messages` | Сообщения комнаты |
| `POST` | `/api/chat/{roomSlug}/messages` | Отправить сообщение |
| `GET` | `/api/chat/{roomSlug}/users` | Пользователи в комнате |
| `GET` | `/api/chat/general` | Общий чат |
| `POST` | `/api/chat/general` | Отправить в общий чат |
| `GET` | `/api/chat/admin-group` | Админ-группа |
| `GET` | `/api/chat/private/{userId}` | Приватный чат |
| `GET` | `/api/chat/unread` | Непрочитанные сообщения |
| `PUT` | `/api/chat/messages/{messageId}/read` | Пометить сообщение прочитанным |
| `GET` | `/api/chat/questions/rooms` | Комнаты вопросов |
| `POST` | `/api/chat/questions-dto/{mode}/archive` | Архивация вопроса |

### Живые трансляции

| Метод | Эндпоинт | Описание |
|-------|----------|----------|
| `POST` | `/api/broadcast/create` | Создать трансляцию |
| `POST` | `/api/broadcast/{id}/start` | Начать трансляцию |
| `POST` | `/api/broadcast/{id}/stop` | Остановить трансляцию |
| `POST` | `/api/broadcast/{id}/end` | Завершить трансляцию |
| `GET` | `/api/broadcast/active` | Активные трансляции |
| `GET` | `/api/broadcast/{id}` | Информация о трансляции |
| `GET` | `/api/broadcast/{id}/stats` | Статистика трансляции |
| `GET` | `/api/broadcast/{id}/archive` | Архив трансляции |
| `GET` | `/api/broadcast/{id}/playlist.m3u8` | HLS-плейлист |
| `DELETE` | `/api/broadcast/{id}` | Удалить трансляцию |

### WebRTC

| Метод | Эндпоинт | Описание |
|-------|----------|----------|
| `POST` | `/api/webrtc/offer` | Отправить WebRTC offer |
| `GET` | `/api/webrtc/candidate/{broadcastId}` | Получить ICE-кандидата |

### Медиа

| Метод | Эндпоинт | Описание |
|-------|----------|----------|
| `POST` | `/api/media/upload` | Загрузить файл |
| `GET` | `/api/media/stream/{fileId}` | Потоковая передача файла |
| `GET` | `/api/media/download/{fileId}` | Скачать файл |
| `GET` | `/api/media/key/{fileId}` | Получить ключ шифрования |
| `GET` | `/api/media/my-files` | Мои файлы |
| `GET` | `/api/media/hls/{fileId}/playlist.m3u8` | HLS-плейлист медиа |
| `DELETE` | `/api/media/{fileId}` | Удалить файл |

### Мероприятия

| Метод | Эндпоинт | Описание |
|-------|----------|----------|
| `GET` | `/api/events/upcoming` | Предстоящие мероприятия |
| `GET` | `/api/events/past` | Прошедшие мероприятия |
| `GET` | `/api/events/next` | Ближайшее мероприятие |
| `GET` | `/api/events/{id}` | Информация о мероприятии |
| `POST` | `/api/events` | Создать мероприятие |
| `PUT` | `/api/events/{id}` | Обновить мероприятие |
| `DELETE` | `/api/events/{id}` | Удалить мероприятие |

### Материалы мероприятий

| Метод | Эндпоинт | Описание |
|-------|----------|----------|
| `GET` | `/api/events/materials/{sectionId}` | Материалы секции |
| `GET` | `/api/events/materials/item/{id}` | Материал по ID |
| `POST` | `/api/events/materials` | Создать материал |
| `PUT` | `/api/events/materials/{id}` | Обновить материал |
| `DELETE` | `/api/events/materials/{id}` | Удалить материал |

### Архивы

| Метод | Эндпоинт | Описание |
|-------|----------|----------|
| `GET` | `/api/archives` | Список архивов |
| `GET` | `/api/archives/{id}` | Архив по ID |
| `GET` | `/api/archives/event/{eventId}` | Архив мероприятия |
| `POST` | `/api/archives` | Создать архив |
| `PUT` | `/api/archives/{id}` | Обновить архив |
| `DELETE` | `/api/archives/{id}` | Удалить архив |

### Пользователи и профили

| Метод | Эндпоинт | Описание |
|-------|----------|----------|
| `GET` | `/api/users/me` | Текущий пользователь |
| `PUT` | `/api/users/me` | Обновить профиль |
| `GET` | `/api/users/{userId}` | Профиль пользователя |

### Платежи

| Метод | Эндпоинт | Описание |
|-------|----------|----------|
| `POST` | `/api/payments/create` | Создать платёж |
| `GET` | `/api/payments/{paymentId}` | Информация о платеже |
| `GET` | `/api/payments/{paymentId}/qr` | QR-код платежа |
| `GET` | `/api/payments/my` | Мои платежи |
| `GET` | `/api/payments/{paymentId}/status` | Статус платежа |
| `POST` | `/api/payments/{paymentId}/confirm` | Подтвердить платёж |
| `GET` | `/api/payments/pending` | Ожидающие платежи |

### Уведомления

| Метод | Эндпоинт | Описание |
|-------|----------|----------|
| `GET` | `/api/notifications` | Список уведомлений |
| `GET` | `/api/notifications/unread` | Непрочитанные уведомления |
| `PUT` | `/api/notifications/{id}/read` | Пометить прочитанным |
| `PUT` | `/api/notifications/read-all` | Пометить все прочитанными |

### Администрирование

| Метод | Эндпоинт | Описание |
|-------|----------|----------|
| `GET` | `/api/admin/users` | Список пользователей |
| `GET` | `/api/admin/users/{userId}` | Информация о пользователе |
| `PUT` | `/api/admin/users/{userId}/role` | Изменить роль |
| `POST` | `/api/admin/users/{userId}/ban` | Заблокировать |
| `POST` | `/api/admin/users/{userId}/unban` | Разблокировать |
| `GET` | `/api/admin/posts/flagged` | Помеченные посты |
| `POST` | `/api/admin/posts/{postId}/moderate` | Модерация поста |

### Здоровье и мониторинг

| Метод | Эндпоинт | Описание |
|-------|----------|----------|
| `GET` | `/api/health` | Проверка работоспособности |
| `GET` | `/api/health/websocket` | Статус WebSocket |

---

## 9. Развёртывание

### Продакшн-сборка

```bash
# Полный стек через Docker Compose
docker compose -f docker-compose.yml build
docker compose -f docker-compose.yml up -d

# Или отдельные сборки
cd frontend && npm run build:prod
cd ../backend && mvn clean package -DskipTests
```

### SSL-сертификаты

Для продакшена требуются валидные SSL-сертификаты для трёх доменов:
- `platform.local` — основной сайт
- `api.platform.local` — API
- `auth.platform.local` — Keycloak

Разместите их в `docker/nginx/ssl/`:
- `platform.crt` / `platform.key`
- `api.crt` / `api.key`
- `auth.crt` / `auth.key`

### Конфигурация для разных окружений

| Файл | Назначение |
|------|-----------|
| `backend/src/main/resources/application.yml` | Конфигурация Spring по умолчанию |
| `backend/src/main/resources/application-prod.yml` | Продакшн-переопределения |
| `.env` | Секреты Docker Compose |
| `frontend/.env` | Переменные сборки фронтенда |

### Чек-лист развёртывания

- [ ] SSL-сертификаты сгенерированы и смонтированы
- [ ] `.env` заполнен надёжными секретами (`MEDIA_ENCRYPTION_KEY`, `REDIS_PASSWORD`, `KEYCLOAK_SECRET`)
- [ ] Рейм Keycloak экспортирован и размещён в `docker/keycloak/realm-export.json`
- [ ] Cron-расписание резервного копирования настроено (сервис `backup` в Docker Compose)
- [ ] Стек мониторинга доступен (Grafana на порту 3001, Kibana на 5601)
- [ ] Ограничение частоты запросов настроено в `docker/nginx/conf.d/platform.conf`
- [ ] Лимиты ресурсов скорректированы в `docker-compose.yml` (`mem_limit`, `cpus`)
- [ ] `REACT_APP_ENVIRONMENT=production` установлен для сборки фронтенда

### Мониторинг и наблюдаемость

| Инструмент | Порт | Доступ |
|------------|------|--------|
| Grafana | 3001 | `http://localhost:3001` (admin/admin) |
| Prometheus | 9090 | `http://localhost:9090` |
| Kibana | 5601 | `http://localhost:5601` |
| Zipkin | 9411 | `http://localhost:9411` |
| Alertmanager | 9093 | `http://localhost:9093` |
| Swagger UI | 8081 | `http://localhost:8081/swagger-ui.html` |

### Резервное копирование и восстановление

Сервис `backup` выполняет ежедневный дамп PostgreSQL в 02:00. Резервные копии хранятся в `./backups/`.

```bash
# Ручной бэкап
docker compose exec postgres pg_dump -U platform_user platform_db > backup_$(date +%Y%m%d).sql

# Восстановление
cat backup.sql | docker compose exec -T postgres psql -U platform_user platform_db
```

### Остановка и очистка

```bash
# Остановка всех сервисов
docker compose down

# Остановка и удаление томов (уничтожает данные)
docker compose down -v

# Удаление неиспользуемых образов
docker image prune
```

---

## 10. Контрибьюция

### Рабочий процесс

1. **Форк** репозитория и клонирование
2. Создание **feature-ветки** от `main`: `git checkout -b feature/my-feature`
3. Внесение изменений с соблюдением стиля кода
4. Запуск **тестов**: `npm test` (фронтенд) и `mvn test` (бэкенд)
5. **Коммит** с описательным сообщением (на русском или английском)
6. **Push** в ваш форк и создание **Pull Request**

### Соглашения по коммитам

Формат: `<тип>: <описание>`

Типы:
- `feat` — новая функциональность
- `fix` — исправление ошибки
- `docs` — обновление документации
- `style` — форматирование (без изменения логики)
- `refactor` — рефакторинг кода
- `test` — добавление/обновление тестов
- `chore` — сборка, инфраструктура, зависимости

Примеры:
```
feat: добавлена функция экспорта архивов в PDF
fix: исправлена обработка ошибок WebSocket при реконнекте
docs: обновлен README с описанием API
test: добавлены тесты для PaymentController
```

### Стандарты кода

| Компонент | Стандарт |
|-----------|----------|
| **Фронтенд** | ESLint (конфигурация CRA) + Prettier; компоненты — функциональные с хуками |
| **Бэкенд** | Стандартные соглашения Spring Boot; Lombok для DTO/сущностей; JUnit 5 + Mockito для тестов |
| **Коммуникация** | Описательные PR с указанием связанных задач; код-ревью обязателен |
| **Язык** | Коммиты и документация — на русском или английском; весь UI — на русском |

### Добавление новой функциональности

#### Бэкенд
1. Создайте DTO в `model/dto/`
2. Создайте/обновите JPA-сущность в `model/`
3. Создайте репозиторий в `repository/`
4. Создайте сервис в `service/`
5. Создайте REST-контроллер в `controller/`
6. Добавьте миграцию Liquibase при необходимости
7. Напишите тесты

#### Фронтенд
1. Создайте компонент в `components/<Категория>/`
2. Добавьте хук если нужна управляемая логика
3. Подключите API через `services/api.js`
4. Добавьте маршрут/вкладку в `App.js`
5. Напишите тесты в `__tests__/`

---

## Лицензия

Проект «Путь Сердца» — закрытая платформа для сообщества. Использование и распространение без разрешения авторов запрещено.
