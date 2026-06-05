# Проект "Platform" — полный анализ

## Общая архитектура

- **Тип приложения**: одностраничное приложение (SPA) на React 18.
- **Сборка**: Create React App (`react-scripts` 5), Webpack 5.
- **Аутентификация**: Keycloak (OpenID Connect, PKCE S256).
- **Взаимодействие с сервером**: REST (axios), WebSocket (для чата и трансляций).
- **Управление состоянием**: React Context (AudioPlayerContext) + локальное состояние хуков.
- **Навигация**: ручное переключение вкладок через `activeTab`, React Router используется минимально.
- **Стилизация**: чистый CSS (CSS-переменные), только тёмная тема.
- **Тестирование**: отсутствует.

## Стек технологий (из `package.json`)

| Пакет | Версия | Назначение |
|-------|--------|------------|
| `react` | ^18.2.0 | UI-библиотека |
| `react-dom` | ^18.2.0 | Рендеринг в DOM |
| `react-router-dom` | ^6.20.0 | Маршрутизация (не используется активно) |
| `keycloak-js` | ^24.0.3 | Аутентификация |
| `axios` | ^1.7.2 | HTTP-клиент |
| `lodash` | ^4.17.21 | Утилиты (не используется в предоставленном коде) |
| `react-window` | ^1.8.10 | Виртуализация списков (не внедрена) |
| `react-scripts` | ^5.0.1 | Сборщик |
| `webpack` | ^5.88.2 | Сборщик (зависимость react-scripts) |
| `eslint` | ^8.57.0 | Линтер |
| `prettier` | ^3.2.5 | Форматтер |
| `source-map-explorer` | ^2.5.3 | Анализ бандла |

## Структура папок `frontend/src`

```
frontend/src/
├── index.css                     # Глобальные стили, тёмная тема, плеер
├── App.js                        # Корневой компонент + инициализация Keycloak
├── App.css                       # Стили хедера, страницы логина, анимации
├── keycloak.js                   # Работа с Keycloak (init, login, logout, getToken, getUserRoles)
├── constants/
│   └── roles.js                  # Роли, уровни, метки, иконки
├── hooks/
│   ├── usePermissions.js         # Хук проверки прав доступа
│   └── useWebSocket.js           # [УДАЛЁН] — пустой файл, импорты сломаны
├── context/
│   └── AudioPlayerContext.js     # Глобальный плеер (аудио/видео)
├── components/
│   ├── Navigation/
│   │   ├── NavTabs.js            # Панель вкладок
│   │   └── NavTabs.css           # Стили вкладок (разделенные кнопки, тройные меню)
│   ├── Feed/
│   │   ├── MainFeed.js           # Лента постов
│   │   └── MainFeed.css          # Стили ленты, встроенный плеер, модалка удаления
│   ├── Dashboard/
│   │   ├── Dashboard.js          # Дашборд с меню
│   │   ├── Dashboard.css         # Стили меню, подменю, модалок, запись аудио
│   │   └── initialData.js        # Начальные данные для 18 категорий (по 50 записей)
│   ├── live/
│   │   ├── LiveBroadcastView.js  # Прямые трансляции
│   │   └── LiveBroadcastView.css # Стили карточек трансляций, тёмная тема
│   ├── Chat/
│   │   ├── GeneralChat.js        # Общий чат
│   │   └── GeneralChat.css       # Стили чата (сообщения, ввод)
│   └── UserProfile/
│       ├── UserProfile.js        # Профиль и подписки
│       └── UserProfile.css       # Стили профиля, платежей, QR
└── services/
    └── api.js                    # API-клиент, все endpoint'ы (feed, broadcast, chat, payment, user, media, admin, dashboard, demo, events, feedback, notification)
```

Также в корне проекта находятся:

- `.env` — переменные окружения (БД, Keycloak, Media Encryption).
- `package-lock.json` — пустой файл (без зависимостей).
- `frontend/public/index.html` — HTML-шаблон с CSP и скриптом темы.
- `frontend/public/silent-check-sso.html` — страница для silent check SSO Keycloak.
- `frontend/node_modules/.bin/react-scripts` — исполняемый файл (не анализируется).
- `PROJECT_INFO.md` — этот файл.

## Подробный анализ каждого файла

### 1. `frontend/package.json`

- Зависимости актуальные, но `jest-worker` добавлен в `dependencies` случайно (должен быть в devDependencies или не нужен).
- `browserslist` корректный.
- Скрипты: `build:prod` использует переменную `REACT_APP_ENVIRONMENT=production`.

### 2. `frontend/public/index.html`

- Есть CSP (Content-Security-Policy), но он использует `'unsafe-inline'` для скриптов и стилей, что снижает безопасность.
- Скрипт темы из `localStorage` загружается до React, что хорошо.
- Нет PWA / Service Worker.

### 3. `frontend/public/silent-check-sso.html`

- Простая страница, отправляющая `parent.postMessage(location.href, location.origin)`.
- Используется для бесшовной проверки SSO в Keycloak.

### 4. `frontend/src/index.css`

- Определяет только тёмную тему через `:root`. Светлая тема не реализована, хотя в `index.html` есть проверка на `light`.
- Содержит стили для глобального плеера, скроллбары, анимации.
- Дублирование с `App.css` (например, сброс `*` и стили `body`).

### 5. `frontend/src/App.css`

- Анимация `accessDeniedAnim` определена.
- Стили хедера и логина.
- Дублирует часть глобальных стилей `index.css`.

### 6. `frontend/src/keycloak.js`

- **Функции**: `initKeycloak`, `login`, `logout`, `getToken`, `getRefreshToken`, `updateToken`, `getUsername`, `getUserId`, `getUserRoles`, `isTokenExpired`.
- **Проблемы**:
  - `setInterval` для обновления токена никогда не очищается (утечка при размонтировании, хотя `App` — корневой компонент, это допустимо, но лучше использовать очистку).
  - Нет обработки ошибки при обновлении токена (только `console.error`).
  - `initKeycloak` вызывает `setInterval` каждый раз при повторном вызове (если вызвать несколько раз, будет несколько интервалов).
  - Внутренние переменные `userRoles` и `username` живут в модульной области видимости, что может приводить к состояниям гонки при параллельной инициализации.

### 7. `frontend/src/constants/roles.js`

- Хорошо определённые константы. Порядок уровней от 1 (USER_0) до 8 (SUPER_ADMIN).
- Нет `ROLE_LEVELS` для роли `DEVELOPER`? Она есть (5). Всё в порядке.

### 8. `frontend/src/hooks/usePermissions.js`

- Использует `getUserRoles()` из модуля `keycloak.js`.
- Возвращает набор методов проверки прав.
- **Проблемы**:
  - `getUserRoles()` может вернуть пустой массив до завершения инициализации Keycloak – `level` будет 1 (USER_0).
  - Методы `canViewAdmin`, `canUploadMedia` и т.д. используют `hasLevel`, который сравнивает с числом – это надёжно.
  - Нет кэширования: каждый вызов хука заново получает роли (но это нормально, так как роли не меняются).

### 9. `frontend/src/hooks/useWebSocket.js`

**Внимание!** Этот файл был **удалён** в коммите 47046df (`refactor: удалить неиспользуемый хук useWebSocket`). В текущей версии проекта его не должно быть. В предоставленных файлах он присутствует как пустой файл (без содержимого). Импорты из него сломаны.

### 10. `frontend/src/context/AudioPlayerContext.js`

- **Ответственность**: глобальный плеер для аудио и видео.
- **Проблемы**:
  - Логика выбора `audioRef` / `videoRef` запутанная: условие `currentTrack?.type !== 'video'` для аудио и наоборот. Есть риск, что плеер не будет работать для видео.
  - При остановке трека устанавливается `src = ''`, что может вызывать ошибки на iOS.
  - Нет обработки ошибок при загрузке медиа (событие `error` не слушается).
  - `animationFrameRef` используется для обновления `currentTime`, но при паузе не отменяется (отменяется при смене `currentTrack` или `isPlaying`). Может продолжать обновляться.
  - В `GlobalMediaPlayer` есть вложенный компонент с собственным контекстом, что усложняет отладку.
  - При `stopTrack` не очищаются таймеры `hideTimeoutRef`.

### 11. `frontend/src/App.js`

- **Инициализация Keycloak**: до 3 попыток с задержкой 2 секунды.
- **Навигация**: ручное переключение вкладок через `activeTab`.
- **Проверка прав**: для USER_0 доступны только `help` и `profile`; остальные блокируются с показом сообщения "Доступ ограничен".
- **Проблемы**:
  - `handleLogin` использует прямую ссылку на Keycloak URL, а не вызов `login()`. Это может не сработать при изменении конфигурации.
  - `handleTabChange` не проверяет, есть ли у пользователя права на вкладку `admin` или `broadcast-live` (проверка есть только на уровне USER_0). Дополнительные вкладки могут быть доступны, даже если прав нет.
  - Вкладка `dashboard` не определена в `renderContent` — при её выборе будет показан `MainFeed` (запасной вариант).
  - `accessDeniedTimerRef` не очищается при размонтировании (хотя App не размонтируется).
  - Нет обработки истечения сессии или перенаправления на логин.

### 12. `frontend/src/components/Navigation/NavTabs.js`

- Использует `usePermissions`. Базовые вкладки: `main`, `scheduled`, `chat`, `help`, `profile`.
- Дополнительные: `broadcast-live` (если `canStartBroadcast` или `hasLevel(4)`), `admin` (если `canViewAdmin`).
- **Проблемы**: условие `canStartBroadcast() || hasLevel(4)` избыточно, так как `canStartBroadcast` уже проверяет `hasLevel(6)`. `hasLevel(4)` может дать доступ к трансляциям некорректно (роль CONSULTANT имеет уровень 4, но не должна управлять трансляциями). Лучше полагаться на `canStartBroadcast()`.

### 13. `frontend/src/components/Navigation/NavTabs.css`

- Стили для навигационных вкладок, разделённых кнопок (например, чат с тремя частями), анимации.
- Использует CSS-переменные.
- Содержит много специфичных селекторов для разных режимов (`.nav-tab-half.nutrition`, `.nav-tab-third.chat-user1` и т.д.).

### 14. `frontend/src/components/Feed/MainFeed.js`

- **Большой компонент** (> 700 строк).
- **Функции**: загрузка постов, добавление/редактирование/удаление (только для админа), запись аудио, встроенный плеер.
- **Проблемы**:
  - Дублирование логики проверки прав через `sessionStorage` для development. Плохо: в продакшене `demoRole` не будет, и код будет использовать `getUserRoles()`, но структура `isAdmin` смешана.
  - Blob URL не очищаются после публикации (кроме случая вызова `handlePublishPost`, который очищает все blobUrlsRef, но если запись не опубликована, останутся).
  - Нет пагинации – загружаются все посты сразу.
  - Используется `forceUpdate` (счётчик) для принудительного рендера – антипаттерн.
  - Модальное окно подтверждения удаления находится внутри компонента, что усложняет тестирование.
  - Логика записи аудио дублируется с `Dashboard.js`.

### 15. `frontend/src/components/Feed/MainFeed.css`

- Стили для ленты, встроенного аудиоплеера, админ-панели, модального окна удаления.
- Использует CSS-переменные, анимации fadeIn, slideInRight.
- Частично дублирует стили `Dashboard.css` (например, `.inline-audio-player`).

### 16. `frontend/src/components/Dashboard/Dashboard.js`

- **Самый большой компонент** (> 1300 строк).
- **Функции**: загрузка меню, отображение 18 разделов, вложенные подменю, добавление/редактирование/удаление контента, запись аудио, загрузка файлов, отображение ошибок.
- **Проблемы**:
  - Полное дублирование логики записи аудио (MediaRecorder) из `MainFeed`.
  - Сложная система таймеров для показа/скрытия подменю (4 рефа, 8 состояний). Легко сломать.
  - Использует `sessionStorage` как fallback в development – в продакшене полагается на API, но код всё равно содержит логику синхронизации с sessionStorage, что может привести к расхождению.
  - Множество `useCallback` и `useMemo` без реальной оптимизации (например, `navigationMenus` пересоздаётся при изменении `menuData` – это нормально, но `useMemo` не даёт выгоды).
  - Модалки добавления и удаления встроены прямо в компонент – лучше вынести в отдельные компоненты.
  - В `initialData` много статических заглушек (50 эфиров и т.д.), они занимают огромную часть файла.
  - Код почти нечитаем – всё в одном монолите.

### 17. `frontend/src/components/Dashboard/Dashboard.css`

- Стили для меню, подменю, модальных окон, медиа-контролов, ошибок.
- Содержит сложные селекторы для позиционирования выпадающих меню, анимации slideDown.
- Использует CSS-переменные, адаптивную вёрстку.

### 18. `frontend/src/components/Dashboard/initialData.js`

- Экспортирует начальные данные для 18 категорий (каждая по 50 записей).
- Использует функцию `generateItems` для генерации массива объектов.
- **Проблемы**: Огромный размер (> 500 строк статических текстов). Хранит данные на клиенте, что негибко и усложняет поддержку.

### 19. `frontend/src/components/live/LiveBroadcastView.js`

- **Функции**: список активных трансляций, старт/стоп (админ), WebSocket для количества зрителей.
- **Проблемы**:
  - Использует `useWebSocket`, который удалён или пуст. При сборке возникнет ошибка импорта. **Требуется срочное исправление**.
  - Логика проверки прав через `sessionStorage` дублируется с `MainFeed`.
  - WebSocket подключается к каналу `broadcast-stats`, но серверный канал может быть другим.
  - Нет обработки ошибок WebSocket на клиенте (только в хуке, но хук удалён).
  - Интервал опроса `loadBroadcasts` каждые 10 секунд – слишком часто (можно увеличить до 30 с).

### 20. `frontend/src/components/live/LiveBroadcastView.css`

- Стили для страницы трансляций: карточки, превью, мета-информация, кнопки.
- Поддерживает тёмную тему через `@media (prefers-color-scheme: dark)`.
- Адаптивная вёрстка.

### 21. `frontend/src/components/Chat/GeneralChat.js`

- **Функции**: загрузка истории, отправка сообщений, WebSocket для получения новых.
- **Проблемы**:
  - Импортирует `useWebSocket` (пустой файл или удалён) – ошибка компиляции.
  - `loadMessages` использует `page` как число, но сервер не возвращает пагинацию – `hasMore` всегда `false`.
  - При получении нового сообщения через WebSocket не проверяется дубликат (может появиться дважды).
  - Нет обработки `wsError` – поле `error` из `useWebSocket` не используется.
  - `handleSend` отправляет сообщение сначала через REST, а затем через WebSocket – это нормально для синхронизации, но если WebSocket ещё не подключён, сообщение не дойдёт до других клиентов (только через REST). Лучше отправлять через REST, а WebSocket использовать только для получения.

### 22. `frontend/src/components/Chat/GeneralChat.css`

- Стили для чата: сообщения (свои/чужие), индикатор подключения, поле ввода.
- Использует CSS-переменные, анимацию fadeIn.
- Содержит селекторы `.chat-message.own` для стилизации своих сообщений.

### 23. `frontend/src/components/UserProfile/UserProfile.js`

- **Функции**: отображение и редактирование профиля, история платежей, создание подписки, QR-код.
- **Проблемы**:
  - `alert` вместо модальных окон (плохой UX).
  - QR-код и реквизиты жёстко зашиты в коде – должны приходить с сервера.
  - При создании подписки вызывается `loadPayments()`, но `setCurrentPayment` устанавливается после `alert`, а `loadPayments` – асинхронно, порядок может сбиться.
  - Нет валидации полей при редактировании (email, phone).
  - Код не обрабатывает ошибки от `paymentApi.getPaymentQR` (например, если сервер вернул ошибку, QR не загрузится, но ошибка не отобразится).

### 24. `frontend/src/components/UserProfile/UserProfile.css`

- Стили для профиля: карточки, подпики, QR, банковские детали, аватар.
- Содержит множество специфичных классов (`.profile-avatar-container`, `.period-card`, `.bank-details-grid`).
- Адаптивная вёрстка, анимация fadeIn.

### 25. `frontend/src/services/api.js`

- **ApiClient**: обёртка над axios с JWT-перехватчиком.
- **Проблемы**:
  - `interceptors.request.use` вызывает `updateToken()` для каждого запроса, что может снизить производительность (лучше проверять истечение токена через `isTokenExpired()`).
  - При 401 очищает `sessionStorage` и перенаправляет на `/`, но не вызывает `logout()` Keycloak.
  - Использует `USE_MOCK` для пропуска токена – но в коде нет проверки `USE_MOCK` в response interceptor.
  - В конце файла экспорты `eventsApi`, `feedbackApi`, `notificationApi` смешаны с переменными, объявленными через `export const` – стилистически не единообразно.
- **Endpoints**:
  - `broadcastApi`, `chatApi`, `paymentApi`, `userApi`, `mediaApi`, `adminApi`, `dashboardApi`, `feedApi`, `demoApi`, `eventsApi`, `feedbackApi`, `notificationApi`.
  - В `demoApi` много методов, дублирующих функциональность основных API (например, `getDashboardMenuData` дублирует `dashboardApi.getMenuData`).

### 26. `.env`

- Содержит настройки для локальной разработки (БД, Keycloak, шифрование медиа).
- **Проблемы**: `MEDIA_ENCRYPTION_KEY` пустой (`MEDIA_ENCRYPTION_KEY=`), что может вызвать ошибки на бэкенде.

## Поток данных

1. Пользователь открывает приложение → `App.js` вызывает `initKeycloak()`.
2. После успешной аутентификации устанавливаются `userRoles` и `username`.
3. `NavTabs` использует `usePermissions` для отображения доступных вкладок.
4. Компоненты загружают данные через API (сервисы в `services/api.js`).
5. При ошибке – fallback на статические данные (`MainFeed`, `Dashboard`) или `sessionStorage` (`Dashboard`).
6. WebSocket используется для чата и трансляций (сейчас сломан из-за удаления хука).

## Безопасность

- CSP с `'unsafe-inline'` – уязвимость для XSS.
- Токен Keycloak обновляется каждые 60 секунд, но нет обработки ошибок обновления.
- Логика прав доступа дублируется на клиенте и может расходиться с серверной.
- Формы не валидируются, что может привести к отправке некорректных данных.
- Нет HTTP-Only кук для токена (он хранится в памяти браузера).
- QR-код содержит статические банковские реквизиты – безопасность не нарушена, но негибко.

## Производительность

- Каждый компонент загружает данные при монтировании – нет единого кэша.
- Нет виртуализации списков (react-window не используется).
- Blob URL создаются, но частично не очищаются.
- Dashboard создаёт множество таймеров, которые могут влиять на утечки памяти.
- Компоненты перерисовываются при любом изменении состояния, без мемоизации (кроме `Dashboard`, где она избыточна).

## Качество кода

- **Дублирование**: запись аудио (MainFeed, Dashboard), проверка прав (много раз, разные варианты), модальные окна.
- **Смешение ответственности**: `MainFeed` и `Dashboard` гигантские (700+ и 1300+ строк).
- **Магические строки**: сравнение ролей через `=== 'MODERATOR_ADMIN'` вместо `ROLES.MODERATOR_ADMIN`.
- **Нет TypeScript** – все ошибки типов выявляются только в runtime.
- **Нет тестов** – ни одного тестового файла.
- **Нет разделения на слои** – бизнес-логика, API-вызовы, UI-логика пермешаны.
- **Неиспользуемая функциональность**: react-router, react-window.
- **Удалённый хук useWebSocket** – сломал LiveBroadcastView и GeneralChat.
- **Пустой хук useWebSocket.js** – занимает место, импорты сломаны.
- **Огромный initialData.js** – статические данные на клиенте, увеличивают размер бандла.

## Рекомендации (без предложений по исправлению – только заметки)

- Создать единый хук для записи аудио.
- Вынести модальные окна в отдельные компоненты.
- Разделить `Dashboard` на 3-4 компонента (меню, модалка, подменю).
- Заменить ручную навигацию на React Router.
- Перейти на TypeScript.
- Написать unit-тесты для хуков и сервисов.
- Добавить обработку ошибок через контекст уведомлений.
- Убрать `sessionStorage` из продакшен-кода.
- Удалить мёртвый код (`forceUpdate`, `jest-worker`).
- Починить импорты `useWebSocket` после удаления файла.
- Увеличить интервал опроса трансляций до 30 секунд.
- Реализовать пагинацию на списках (MainFeed, Chat, Dashboard).
- Оптимизировать `initialData.js` – загружать с сервера.
- Вынести банковские реквизиты в конфигурацию.

## Примечания по коммиту 47046df

- Удалён неиспользуемый хук `useWebSocket.js`. Однако он всё ещё импортируется в `LiveBroadcastView.js` и `GeneralChat.js`, что приведёт к ошибкам сборки. Требуется заменить вызовы удалённого хука на альтернативную реализацию или интегрировать WebSocket напрямую.

## Заключение

Проект находится в активной разработке, имеет много функциональных возможностей, но страдает от:

- дублирования кода,
- смешения ответственности,
- отсутствия тестов,
- незавершённого рефакторинга (удаление хука без обновления зависимостей).

Для перехода к стабильной production-версии требуется глубокий рефакторинг крупных компонентов, внедрение единого подхода к состоянию, TypeScript и тестирование.

---

## Анализ файлов backend (дополнение)

### Общая характеристика

Backend представляет собой Spring Boot 3.2.5 приложение. Кодовая база – около 200+ тысяч строк Java-кода (исходники + тесты). Проект является монолитным REST-сервером с встроенной WebSocket-поддержкой и гибридным кэшированием (Caffeine + Redis).

### Структура проекта

```
backend/
├── src/main/java/com/platform/
│   ├── PlatformApplication.java         # Точка входа
│   ├── aspect/                          # AOP аспекты (@Timed, @Counted)
│   ├── config/                          # ~25 классов конфигурации (Security, WebSocket, Cache, Async, Jackson, Retry, Resilience, RateLimiting, Metrics, CORS, ProductionGuard, EnvValidator)
│   ├── controller/                      # ~20 REST контроллеров
│   ├── dto/                             # CreateBroadcastRequest (импорт из другого пакета)
│   ├── exception/                       # 8 кастомных исключений + GlobalExceptionHandler
│   ├── filter/                          # IpRateLimitFilter,
│   ├── health/                          # Кастомные HealthIndicator (Redis, Database, Disk, AllServices)
│   ├── model/                           # ~20 JPA-сущностей + перечисления
│   ├── monitoring/                      # CircuitBreakerMonitor, MemoryMonitor, HotKeyMonitor, ConnectionPoolMonitor
│   ├── repository/                      # ~15 Spring Data JPA репозиториев
│   ├── security/                        # JwtValidationFilter, BruteForceProtectionFilter
│   ├── service/                         # ~40 сервисов (бизнес-логика, интеграции)
│   ├── util/                            # RoleUtils, SqlValidator, XssSanitizer, ValidationUtils, IdGenerator
│   ├── validation/                      # FileValidator
│   └── websocket/                       # ChatWebSocketHandler, BroadcastStatsWebSocketHandler, WebSocketHandshakeInterceptor, RedisWebSocketRegistry, RedisWebSocketSessionRegistry
├── src/main/resources/
│   ├── application.properties           # Основная конфигурация
│   ├── application.yml                  # Альтернативная YAML-конфигурация
│   ├── application-dev.yml             # Профиль разработки
│   ├── application-prod.yml            # Профиль продакшена
│   ├── application-staging.yml         # Профиль стейджинга
│   ├── application-test.yml            # Тестовый профиль
│   ├── logback-spring.xml              # Логирование (JSON)
│   ├── db/changelog/                    # 50+ Liquibase миграций (v1–v13)
│   └── static/                          # Статические ресурсы
├── src/test/java/com/platform/          # ~100+ тестовых классов
├── src/test/resources/                  # Тестовые конфиги
├── Dockerfile                           # Многоступенчатая сборка
├── docker/                              # Docker Compose, Keycloak настройки
├── pom.xml                              # Maven проект
└── *.md                                 # Документация
```

### Технологический стек backend

| Категория | Технология | Версия |
|-----------|-----------|--------|
| Фреймворк | Spring Boot | 3.2.5 |
| Язык | Java | 17 |
| Сборка | Maven | 3.9 |
| База данных | PostgreSQL (драйвер) | - |
| Миграции | Liquibase | - |
| Кэш | Caffeine + Spring Cache | 3.1.8 |
| Распределённый кэш | Redis (Lettuce) | - |
| ORM | Hibernate (через Spring Data JPA) | - |
| Аутентификация | Keycloak (OAuth2 JWT) + admin-client | 24.0.3 |
| API | Spring Web, REST, WebSocket | - |
| Документация API | Springdoc OpenAPI | 2.5.0 |
| Rate Limiting | bucket4j | 0.12.10 |
| Circuit Breaker | Resilience4j (spring-cloud-starter) | 3.1.1 / 2.1.0 |
| Retry | Spring Retry | - |
| Мониторинг | Micrometer (Prometheus, Zipkin/Brave) | - |
| Логирование | Logback + JSON | 0.1.5 |
| Шифрование | AES/GCM, AES/CBC (кастомная реализация) | - |
| Хранилище | AWS S3 SDK (Yandex Cloud) | 1.12.720 |
| QR-коды | ZXing | 3.5.3 |
| Тестирование | JUnit 5, Mockito, AssertJ, Spring Boot Test, Testcontainers, H2 | - |
| Анализ покрытия | JaCoCo | 0.8.11 |
| Видео | FFmpeg (вызовы через ProcessBuilder) | - |

### Основные компоненты backend

#### Контроллеры (REST API)

| Контроллер | Путь | Описание |
|------------|------|----------|
| AdminController | `/api/admin` | Управление пользователями, постами, модерация, статистика |
| ArchiveController | `/api/archives` | CRUD для архивов записей эфиров |
| ChatController | `/api/chat` | Общий чат, админ-чат, приватные сообщения, вопросы/ответы |
| CommentController | `/api/posts/{postId}/comments` | Комментарии к постам |
| ContactController | `/api/contacts` | Контакты пользователей |
| DashboardController | `/api/dashboard` | Данные для меню дашборда (stub) |
| DemoController | `/api/demo` | Заглушки для 12 демо-эндпоинтов |
| EventController | `/api/events` | События/эфиры |
| EventMaterialController | `/api/events/materials` | Материалы разделов EventsHub |
| FeedController | `/api/feed` | Лента новостей (главная, nutrition) |
| HealthController | `/api/health` | Health checks (ffmpeg, WebSocket) |
| LiveBroadcastController | `/api/broadcast` | Создание, запуск, остановка трансляций; RTMP webhook |
| MediaController | `/api/media` | Загрузка, стриминг, скачивание медиа |
| NotificationController | `/api/notifications` | Уведомления |
| PaymentController | `/api/payments` | Платежи (создание, подтверждение админом, QR) |
| PostController | `/api/posts` | CRUD постов |
| ReactionController | `/api/posts/{postId}/reactions` | Лайки/реакции |
| UserController | `/api/users` | Профиль и его обновление |
| WebRTCController | `/api/webrtc` | WebRTC offer (заглушка) |

#### Сервисы (бизнес-логика)

**Основные сервисы:**
- `AdminService` — управление пользователями, модерация, статистика
- `ArchiveService` — архив записей
- `BroadcastArchiveService` — архивирование стримов (HLS → MP4 через FFmpeg, загрузка в S3)
- `ChatRoomService` — комнаты чата, права доступа
- `ChatService` — сообщения, вопросы, пагинация

**Сервисы безопасности и медиа:**
- `SecureMediaService` — загрузка, стриминг, удаление медиа с проверкой прав
- `EncryptionService` — AES-256 шифрование/дешифрование
- `WatermarkService` — невидимые водяные знаки через FFmpeg
- `HLSStreamingService` — HLS конвертация видео

**Сервисы инфраструктуры:**
- `LocalStorageService` / `S3StorageService` — хранилище (Local для dev, S3 для prod)
- `MediaStorageService` — загрузка медиа через StorageService
- `MediaQueueService` — очередь FFmpeg задач
- `MediaCleanupService` — очистка старых HLS сегментов

**Сервисы коммуникации:**
- `NotificationService` — уведомления (создание, отправка через WebSocket)
- `ResilienceService` — обёртка над Resilience4j
- `KeycloakService` — управление ролями через Keycloak admin API
- `AuditService` — логирование действий

**Сервисы оплаты:**
- `PaymentService` — создание платежей, подтверждение, истечение
- `QRCodeService` — генерация QR-кодов и данных для СБП

**Сервисы мониторинга:**
- `MetricsService` — Micrometer счётчики, таймеры, gauges
- `UploadLimitService` — лимиты загрузок (Redis)
- `UserSessionService` — управление сессиями (блокировка токенов)
- `StreamingFileService` — частичная загрузка файлов

**Прочие сервисы:**
- `UserService` — CRUD пользователей с кэшированием
- `PostService` — CRUD постов, лента
- `CommentService` — комментарии
- `ReactionService` — реакции (toggle)
- `EventService` — события
- `EventMaterialService` — материалы разделов
- `ContactService` — контакты
- `MessageArchiveService` — архивация старых сообщений

#### Модели (JPA-сущности)

| Сущность | Таблица | Описание |
|----------|---------|----------|
| User | users | Пользователи |
| Post | posts | Посты (категории: general, nutrition и т.д.) |
| Comment | comments | Комментарии к постам |
| Reaction | reactions | Лайки/реакции |
| Event | events | События/эфиры |
| Message | messages | Сообщения чата (партиционированы по месяцам) |
| Archive | archives | Архивы записей |
| LiveBroadcast | live_broadcasts | Прямые трансляции |
| Payment | payments | Платежи (6 статусов: PENDING → ... → REFUNDED) |
| MediaFile | media_files | Медиафайлы (зашифрованные) |
| AccessLog | access_logs | Логи доступа к медиа |
| Contact | contacts | Контакты пользователей |
| EventMaterial | event_materials | Материалы разделов EventsHub |
| ChatRoom | chat_rooms | Комнаты чата |
| ChatRoomPermission | chat_room_permissions | Права доступа к комнатам |
| Notification | notifications | Уведомления |

#### DTO (Data Transfer Objects)

- `UserDto`, `PostDto`, `CommentDto`, `EventDto`, `ArchiveDto`, `PaymentDto`, `MessageDto`, `ChatRoomDto`, `QuestionDto`, `ReactionDto`, `ContactDto`, `MediaFileDto`, `LiveBroadcastDto`, `AdminStatsDto`, `EventMaterialDto`, `CreateBroadcastRequest`

#### Репозитории (Spring Data JPA)

Все репозитории используют кастомные запросы (`@Query`), модифицирующие методы (`@Modifying`), агрегации (`COUNT`, `SUM`). Примеры:

- `MessageRepository` — сложные запросы для пагинации, курсора, приватных сообщений, подсчёта новых
- `PostRepository` — лента с фильтром по категории
- `LiveBroadcastRepository` — инкремент/декремент зрителей
- `ArchiveRepository` — поиск по `startTime`
- `PaymentRepository` — поиск истёкших платежей, ожидающих подтверждения
- `MediaFileRepository` — инкремент просмотров/скачиваний
- `NotificationRepository` — непрочитанные уведомления

#### Конфигурации

| Конфигурация | Назначение |
|--------------|-----------|
| SecurityConfig | OAuth2 resource server, CORS, CSRF, JWT converter |
| WebSocketConfig | Регистрация WebSocket хендлеров |
| CacheConfig | Redis cache manager с разными TTL для разных кэшей |
| AsyncConfig | Thread pool для @Async (task, ffmpeg, email) |
| JacksonConfig | ObjectMapper с JavaTimeModule, безопасные настройки |
| CorsConfig | CORS для `/api/**` |
| PlatformCircuitBreakerConfig | Circuit breaker для БД и внешних API |
| ResilienceConfig | Keycloak CB, retry, bulkhead, time limiter |
| RetryConfig | RetryTemplate для БД и внешних API |
| MetricsConfig | Включение AOP для @Timed |
| RateLimitingConfig | Redis template для bucket4j |
| ProductionGuard | Проверка конфигурации при продакшен-профиле |
| EnvValidator | Валидация переменных окружения при старте |

#### Обработка ошибок

`GlobalExceptionHandler` обрабатывает:

| Исключение | HTTP статус |
|-----------|-------------|
| ResourceNotFoundException | 404 |
| AccessDeniedException | 403 |
| BusinessException | 400 |
| MethodArgumentNotValidException | 400 |
| ConstraintViolationException | 400 |
| HttpMessageNotReadableException | 400 |
| MethodArgumentTypeMismatchException | 400 |
| OptimisticLockingFailureException | 409 |
| Остальные Exception | 500 |

Также объявлены кастомные исключения: `BadRequestException`, `ConflictException`, `UnauthorizedException`, `ValidationException`.

#### WebSocket

| Компонент | Назначение |
|-----------|-----------|
| ChatWebSocketHandler | Обработка чат-сообщений (rate limiting, broadcast, retry) |
| BroadcastStatsWebSocketHandler | Подсчёт зрителей, отправка статистики |
| WebSocketHandshakeInterceptor | Проверка JWT при handshake |
| RedisWebSocketRegistry | Регистрация сессий в Redis для кластеризации |
| RedisWebSocketSessionRegistry | Хранение сессий, отправка сообщений через Redis |

#### Фильтры

| Фильтр | Назначение |
|--------|-----------|
| IpRateLimitFilter | Rate limiting по IP (Caffeine, разный лимит для auth/chat/payments) |
| JwtValidationFilter | Валидация JWT, проверка срока действия |
| BruteForceProtectionFilter | Защита логина от перебора (Redis, 5 попыток → блокировка 15 мин) |

#### Мониторинг

| Компонент | Назначение |
|-----------|-----------|
| CircuitBreakerMonitor | Логирование состояния circuit breaker каждые 10 сек |
| MemoryMonitor | Логирование памяти, GC каждую минуту |
| HotKeyMonitor | Отслеживание «горячих» ключей кэша |
| ConnectionPoolMonitor | Статистика HikariCP пула |

#### Тестирование

Проект содержит **более 100 тестовых классов** (около 800 тестов). Покрытие включает:

- **Unit-тесты сервисов** – все основные сервисы покрыты (ChatService, UserService, PaymentService, AdminService, CommentService, ReactionService, ArchiveService, EventMaterialService, BroadcastArchiveService, LiveBroadcastService, NotificationService, ContactService и др.)
- **Unit-тесты контроллеров** – все контроллеры (через MockMvc standaloneSetup, ручной SecurityContextFilter)
- **DataJpaTest** – репозитории (H2, PostgreSQL режим)
- **Тесты конфигураций** – Security, Cors, Cache, Async, Jackson, CircuitBreaker, Retry, Resilience, WebSocket
- **Тесты моделей и DTO** – Builder, сериализация
- **Тесты утилит** – RoleUtils, SqlValidator, XssSanitizer, ValidationUtils
- **Тесты фильтров** – IpRateLimitFilter, BruteForce
- **Тесты HealthIndicators** – Redis, Database, Disk, AllServices
- **Тесты WebSocket** – ChatWebSocketHandler, BroadcastStatsWebSocketHandler, RedisWebSocketRegistry, HandshakeInterceptor
- **Тесты исключений** – все кастомные исключения, ErrorResponse
- **Интеграционные тесты** – LiveBroadcastIntegrationTest (Testcontainers)

Используемые тестовые библиотеки: JUnit 5, Mockito, AssertJ, Spring Boot Test, @DataJpaTest, @WebMvcTest, Testcontainers, H2.

### База данных (Liquibase миграции)

Миграции разбиты на версии v1–v13:

- **v1**: 10 таблиц (users, posts, comments, reactions, events, messages, archives, live_broadcasts, notifications, indexes)
- **v2**: начальные пользователи/роли
- **v3**: media_files и payments
- **v4**: media поля в messages
- **v5**: chat_rooms, room_id в messages, room permissions
- **v6**: дефолтные комнаты и права, индексы, версионность
- **v7**: партиционирование messages по месяцам
- **v8**: поля архивации в live_broadcasts
- **v9**: очистка и начальные данные
- **v10**: access_logs
- **v11**: contacts
- **v12**: event_materials
- **v13**: исправление схемы notifications

### Потенциальные проблемы backend

1. **Конфликт конфигураций** — `bucket4j.enabled` включён в `application.properties`, но отключён в `application.yml`. Приоритет YAML выше, поэтому bucket4j может быть неактивен.

2. **Размер multipart** — в `application.properties` сначала установлено `max-file-size=100MB`, затем переопределено на `10KB` для чата. Это может привести к путанице.

3. **`enable_lazy_load_no_trans=true`** — может скрыть проблемы с ленивой загрузкой, но снижает производительность.

4. **Избыточность тестов** — многие тесты проверяют только то, что метод не выбрасывает исключение, без проверки логики.

5. **Большое количество тестовых классов** (> 100) замедляет сборку.

6. **Отсутствие обработки `ValidationException`** — кастомное исключение не перехватывается `GlobalExceptionHandler`, может упасть с 500.

7. **Смешение Builder и конструкторов** — единообразие не соблюдено.

8. **`hibernate.cache.use_second_level_cache=false`** настройка в `application.properties`, хотя есть Spring Cache (Caffeine). Это нормально, но может ввести в заблуждение.

9. **Тестовая конфигурация `TestJpaConfig`** исключает множество автоконфигураций, что может скрыть проблемы.

10. **Большое количество тестов** может быть регрессионным бременем.

### Анализ Dockerfile (backend)

**Файл:** `backend/Dockerfile`

**Многоступенчатая сборка:**
- **Stage 1 (builder):** Образ `maven:3.9-eclipse-temurin-21`, рабочая директория `/app`. Выполняется `mvn dependency:go-offline`, затем копируются исходники из `backend/src/` и собирается jar (`mvn clean package -DskipTests`).
- **Stage 2 (runtime):** Образ `eclipse-temurin:17-jre-alpine`, рабочая директория `/app`. Устанавливается `ffmpeg` (необходим для HLS и водяных знаков). Создаётся пользователь `platform`, каталог для логов, затем jar копируется из builder. Порт 8081. Запуск: `java $JAVA_OPTS -jar app.jar`.

**Анализ:**
- Размер образа минимизирован за счёт двух стадий.
- Версия JDK в builder (21) не совпадает с runtime (17). Это может вызвать конфликт, если collector использует API 21. Но Maven собирает с target 17 (см. pom.xml), поэтому проблем быть не должно.
- `ffmpeg` устанавливается в runtime, что необходимо для HLS и водяных знаков.
- Используется непривилегированный пользователь (`platform`), что соответствует лучшим практикам безопасности.
- Отсутствует `HEALTHCHECK`.
- Файл не содержит инструкций для мультиплатформенной сборки (например, `--platform`).

**Рекомендации:**
- Добавить `HEALTHCHECK` для проверки работоспособности контейнера.
- Указать явно `--platform=linux/amd64` при сборке для совместимости.
- Объединить установку пакетов в одну команду для уменьшения числа слоёв.
- Проверить, что версия Maven (3.9) совместима с Java 21.

### Заключение по backend

Backend представляет собой зрелое корпоративное приложение с богатой бизнес-логикой, реализованной в виде микросервисоподобных сервисов. Проект отличается высокой детализацией: отдельные сервисы для шифрования, HLS, водяных знаков, кэширования, rate limiting, circuit breaker. Тестирование покрывает практически все слои приложения, включая конфигурации и WebSocket. Некоторые проблемы (конфликт конфигов, избыточность тестов) не критичны и могут быть исправлены по мере развития проекта.

Dockerfile реализован грамотно с учётом многоступенчатой сборки и безопасности, но может быть улучшен добавлением `HEALTHCHECK` и уточнением платформы.
