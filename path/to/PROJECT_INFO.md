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

---

## Анализ папки docker

Ниже представлен анализ всех файлов, находящихся в папке `docker/`, включая поддиректории `nginx`, `postgres`, `keycloak`. Файлы были предоставлены как read-only в чате.

### Структура папки docker

```
docker/
├── start.sh                          # Скрипт запуска всего проекта
├── stop.sh                           # Скрипт остановки
├── clean.sh                          # Скрипт полной очистки
├── postgres/
│   ├── init.sql                      # SQL-скрипт инициализации БД (расширения, права)
│   ├── seed.sql                      # Пустой seed-файл (заглушка)
│   └── seed-data.sql                 # Seed-данные для вставки вручную после старта
├── nginx/
│   ├── nginx.conf                    # Главная конфигурация Nginx
│   ├── conf.d/
│   │   └── platform.conf             # Конфигурация виртуальных хостов (frontend, API, Keycloak)
│   └── ssl/
│       ├── generate-certs.sh         # Скрипт генерации самоподписанных сертификатов
│       ├── platform.crt              # SSL-сертификат для platform.local
│       ├── platform.key              # Приватный ключ для platform.local
│       ├── api.crt                   # SSL-сертификат для api.platform.local
│       ├── api.key                   # Приватный ключ для api.platform.local
│       ├── auth.crt                  # SSL-сертификат для auth.platform.local
│       └── auth.key                  # Приватный ключ для auth.platform.local
└── keycloak/
    ├── Dockerfile                    # Кастомный Dockerfile для Keycloak
    ├── realm-export.json             # Экспорт realm с 9 ролями и пользователями
    └── init-keycloak.sh              # Скрипт инициализации Keycloak (через API)
```

### 1. docker/start.sh

- **Назначение**: запуск всего проекта.
- **Проверки**:
  - Наличие Docker и Docker Compose (поддерживает как `docker compose` (v2), так и `docker-compose` (v1)).
  - Создание директории `docker/nginx/ssl` и генерация SSL-сертификатов, если они отсутствуют.
  - Добавление записей в `/etc/hosts` для доменов `platform.local`, `api.platform.local`, `auth.platform.local`.
- **Процесс**: останавливает старые контейнеры (`docker compose down`), затем запускает все сервисы в фоне (`docker compose up -d`).
- **Вывод**: печатает URL доступа и тестовые учётные данные.

### 2. docker/stop.sh

- Просто выполняет `docker-compose down`.

### 3. docker/clean.sh

- Останавливает контейнеры с удалением томов (`docker-compose down -v`), удаляет образы `platform_backend` и `platform_frontend`, а также очищает SSL-сертификаты.

### 4. docker/postgres/init.sql

- Создаёт расширения `uuid-ossp` и `pgcrypto`.
- Устанавливает кодировку UTF8.
- Настраивает права для пользователя `platform_user`.

### 5. docker/postgres/seed.sql

- Пустой файл, содержит только комментарий, поясняющий, что данные вставляются через Liquibase или вручную после старта backend.

### 6. docker/postgres/seed-data.sql

- Содержит ручной SQL для вставки тестовых данных (комнаты чата, пример события) после того, как Liquibase создаст таблицы.
- Использует `ON CONFLICT` для идемпотентности.

### 7. docker/nginx/nginx.conf

- Стандартная конфигурация Nginx: `worker_processes auto`, `events { worker_connections 1024; }`.
- Включает gzip-сжатие, логи, `client_max_body_size 100M`.
- Подключает конфигурации из `/etc/nginx/conf.d/*.conf` (где лежит `platform.conf`).

### 8. docker/nginx/conf.d/platform.conf

- **Три виртуальных хоста**:
  - HTTP → HTTPS редирект на всех трёх доменах.
  - `platform.local` — прокси на фронтенд (порт 80 контейнера `frontend`).
  - `api.platform.local` — прокси на бэкенд (порт 8081):
    - `/ws/` — WebSocket-прокси с поддержкой Upgrade и длинным таймаутом (86400 секунд).
    - `/api/` — REST API.
    - `/media/` — раздача статических файлов с кэшированием на 30 дней.
    - `/actuator/` — ограничен доступом с localhost.
  - `auth.platform.local` — прокси на Keycloak (порт 8080).
- Для всех хостов используются самоподписанные SSL-сертификаты.

### 9. docker/nginx/ssl/generate-certs.sh

- Генерирует три самоподписанных сертификата (RSA 2048, срок 365 дней) для доменов `platform.local`, `api.platform.local`, `auth.platform.local`.
- Использует команду `openssl req -x509 -nodes ...`.

### 10. docker/ssl/*.crt / *.key

- Предварительно сгенерированные сертификаты (закодированы в PEM). Являются самоподписанными, предназначены только для разработки.

### 11. docker/keycloak/realm-export.json

- **Файл экспорта realm** для Keycloak.
- **Realm**: `platform`, отображаемое название `Platform`.
- **Роли** (9 штук): `user_0`, `user_1`, `user_2`, `moderator_admin`, `super_admin`, `consultant_pitanie`, `consultant_sport`, `developer`, `vyacheslav`.
- **Пользователи** (9 штук):
  - `super_admin` (пароль `admin123`) — роли `super_admin`, `vyacheslav`.
  - `moderator_admin` (пароль `moderator123`).
  - `user_2` (пароль `user2pass123`).
  - `user_1` (пароль `user1pass123`).
  - `user_0` (пароль `user0pass123`).
  - `consultant_pitanie` (пароль `pitanie123`).
  - `consultant_sport` (пароль `sport123`).
  - `developer` (пароль `dev123`).
  - `vyacheslav` (пароль `vyacheslav123`).
- **Клиенты**:
  - `platform-frontend` — public client, разрешённые редиректы: `https://platform.local/*`, `http://localhost:3000/*`.
  - `platform-backend` — confidential client с secret `backend-secret-2026`, service accounts, direct access grants.
- **Настройки**: `accessTokenLifespan=300` секунд, `ssoSessionMaxLifespan=604800` секунд.

### 12. docker/keycloak/Dockerfile

- Использует официальный образ `quay.io/keycloak/keycloak:24.0`.
- Копирует скрипт `init-keycloak.sh` и делает его исполняемым.
- Запускает Keycloak в dev-режиме (`kc.sh start-dev`).

**Примечание**: init-keycloak.sh не вызывается автоматически — он предназначен для ручного запуска после старта контейнера для настройки ролей и пользователей. Однако экспорт realm уже содержит все необходимые данные, поэтому при импорте через `--import-realm` этот скрипт может не потребоваться.

### 13. docker/keycloak/init-keycloak.sh

- **Bash-скрипт** для инициализации Keycloak через REST API.
- Получает admin token, создаёт realm `platform`, 9 ролей и тестовых пользователей, назначает роли, создаёт клиенты.
- Является альтернативой прямому импорту `realm-export.json`.

### Анализ безопасности и рекомендации

1. **SSL-сертификаты самоподписанные** — подходят только для локальной разработки. Для production необходимо заменить на сертификаты Let's Encrypt или коммерческие.
2. **CSP** в `index.html` уже настроен, но в Nginx дополнительно не блокируются заголовки безопасности (например, `X-Content-Type-Options`, `X-Frame-Options`). Рекомендуется добавить `add_header` в `platform.conf`.
3. **Secret `backend-secret-2026`** в `realm-export.json` — жёстко зашит, его следует изменить на production.
4. **Все пароли пользователей** (user0pass123, admin123 и т.д.) — тестовые и должны быть заменены.
5. **`init-keycloak.sh`** содержит `jq` и `curl` — для его выполнения требуется установленный `jq`. В Docker-образе Keycloak по умолчанию `jq` отсутствует, поэтому скрипт может не работать.
6. **Нет Healthcheck** в Dockerfile для сервисов (backend, keycloak). Добавление `HEALTHCHECK` улучшит мониторинг.
7. **Сети**: предполагается, что контейнеры общаются через общую Docker-сеть, но в файле `docker-compose.yml` (не предоставлен) должны быть определены правильные линки и переменные окружения.

### Выводы по папке docker

Docker-инфраструктура проекта включает полноценную среду для разработки с Nginx, Postgres, Keycloak и самоподписанными сертификатами. Конфигурация ориентирована на локальный запуск через `start.sh`. Для production требуется замена сертификатов, смена паролей/секретов, настройка бэкапов, а также добавление healthchecks и заголовков безопасности в Nginx. Экспорт realm позволяет быстро развернуть Keycloak с 9 ролями и тестовыми пользователями. Seed-данные для Postgres вставляются после старта backend через Liquibase и дополнительный SQL-скрипт.

---

## Анализ папки grafana

Ниже представлен анализ файлов, находящихся в папке `grafana/`. Файлы были предоставлены как read-only в чате.

### Структура папки grafana

```
grafana/
├── dashboards/
│   ├── business.json                # Дашборд бизнес-метрик
│   └── business-metrics.json        # Упрощённый дашборд бизнес-метрик
```

### 1. grafana/dashboards/business.json

**Назначение**: основной дашборд для мониторинга бизнес-метрик платформы.

**Панели**:

| ID | Название | Тип | Описание |
|----|----------|-----|----------|
| 1 | Active Users | stat | Отображает количество активных пользователей (WebSocket-соединения + HTTP-сессии) |
| 2 | Messages per Minute | graph | График скорости сообщений в чате и уведомлений |
| 3 | Payments (24h) | stat | Сумма платежей и выручка за последние 24 часа |
| 4 | User Registration Rate | graph | Скорость регистрации новых пользователей |
| 5 | API Response Times | graph | Перцентили времени ответа API (p95, p99) |
| 6 | Error Rate | graph | Процент ошибок 4xx и 5xx |
| 7 | Database Performance | stat | Скорость операций с БД (вставки, обновления, запросы) |
| 8 | Cache Hit Rate | graph | Процент попаданий в кэш |

**Источники данных**: Prometheus (все метрики).

**Переменные шаблонизации**:
- `environment` — окружение (из лейбла `environment`).
- `service` — сервис (из лейбла `service`).

**Аннотации**: отслеживание деплоев через метрику `deployment_event_total`.

**Ссылки**: на технический и инфраструктурный дашборды.

**Проблемы**:
- Некоторые метрики, используемые в панелях (например, `websocket_connections_total`, `chat_messages_total`, `notifications_sent_total`, `payments_total`, `payments_amount_total`, `user_registrations_total`, `http_request_duration_seconds_bucket`, `hibernate_entity_inserts_total`, `hibernate_entity_updates_total`, `hibernate_queries_total`, `cache_gets_total`, `deployment_event_total`), не экспортируются текущим кодом backend. Это означает, что дашборд будет отображать пустые панели до тех пор, пока не будут добавлены соответствующие метрики.
- Метрика `broadcast_status` (используется в `business-metrics.json`) не экспортируется.
- Дашборд использует `sum(increase(...[24h]))` для платежей, но в коде backend нет счётчика `payments_total` или `payments_amount_total`.
- Метрика `http_requests_total` с фильтром по статусу не экспортируется (используется `http.server.requests` от Micrometer, но с другим именем).
- Метрики Hibernate (`hibernate_*`) не экспортируются по умолчанию — требуется дополнительная конфигурация.

### 2. grafana/dashboards/business-metrics.json

**Назначение**: упрощённый дашборд с четырьмя панелями.

**Панели**:

| Название | Тип | Метрика |
|----------|-----|---------|
| Active Users | stat | `sum(websocket_connections_total)` |
| Messages per Minute | graph | `rate(chat_messages_total[1m])` |
| Payments (24h) | stat | `sum(increase(payments_total[24h]))` |
| Active Broadcasts | stat | `count(broadcast_status == 1)` |

**Проблемы**:
- Те же проблемы с отсутствующими метриками, что и в `business.json`.
- Метрика `broadcast_status` не экспортируется.

### Общие замечания по папке grafana

1. **Отсутствующие метрики**: большинство метрик, используемых в дашбордах, не экспортируются текущим кодом backend. Для их появления необходимо:
   - Добавить кастомные счётчики Micrometer для `websocket_connections_total`, `chat_messages_total`, `notifications_sent_total`, `payments_total`, `payments_amount_total`, `user_registrations_total`, `broadcast_status`.
   - Настроить экспорт метрик HTTP-запросов с разбивкой по статусам (сейчас используется `http.server.requests` от Micrometer, но имя метрики другое).
   - Включить экспорт метрик Hibernate (через `spring.jpa.properties.hibernate.generate_statistics=true` и Micrometer).
   - Настроить экспорт метрик кэша (Caffeine не экспортирует метрики в Micrometer по умолчанию — требуется дополнительная конфигурация).

2. **Дублирование**: `business-metrics.json` является упрощённой копией `business.json`. Рекомендуется оставить только один дашборд.

3. **Отсутствие дашбордов для инфраструктуры**: в коде есть ссылки на технический и инфраструктурный дашборды, но сами файлы отсутствуют.

4. **Формат**: оба файла используют старый формат дашбордов Grafana (без `__inputs` и `__requires`). Для импорта в современную Grafana может потребоваться обновление.

### Рекомендации

1. Добавить недостающие метрики в backend (см. список выше).
2. Удалить дублирующийся дашборд `business-metrics.json`.
3. Создать дашборды для инфраструктуры (CPU, память, диск, сеть) и технических метрик (JVM, пул соединений, Redis).
4. Обновить формат дашбордов до актуальной версии Grafana.
5. Добавить алерты на основе метрик (например, при превышении порога ошибок или времени ответа).

---

## Анализ папки load-test

Ниже представлен анализ файлов, находящихся в папке `load-test/`. Файлы были предоставлены как read-only в чате.

### Структура папки load-test

```
load-test/
├── api-test.js                       # Тест API с использованием k6 (HTTP)
├── chat-test.js                      # Тест WebSocket с использованием k6
├── k6-config.js                      # Конфигурация для k6 (сценарии, пороговые значения, вывод)
└── test-users.json                   # Тестовые пользователи (генерируются скриптом)
```

### 1. load-test/api-test.js

**Назначение**: нагрузочный тест REST API.

**Сценарий**:
- Использует модуль `k6/http`, `k6/check`, `k6/sleep`.
- Загружает тестовых пользователей из `test-users.json` (SharedArray).
- Этапы (`stages`):
  1. 30 секунд, ramp up до 50 виртуальных пользователей (VUs).
  2. 1 минута, ramp up до 200 VUs.
  3. 30 секунд, ramp down до 0 VUs.
- **Пороговые значения**:
  - `http_req_duration`: p(95) < 500 мс.
  - `http_req_failed`: rate < 0.01 (менее 1% ошибок).

**Действия каждого VU**:
1. Авторизация (POST `/auth/login`) – получение токена.
2. Получение профиля (GET `/users/me`).
3. Получение списка постов (GET `/posts?page=0&size=10`).
4. Отправка сообщения в чат (POST `/chat/messages`, с вероятностью 30%).
5. Получение активных трансляций (GET `/broadcasts/active`).
- Между запросами пауза 1 секунда.

**Проблемы**:
- Эндпоинт `/auth/login` не существует в текущем коде backend (аутентификация через Keycloak, а не через REST). Тест будет падать с 404.
- Путь `/posts` не существует (реальный эндпоинт — `/api/posts/feed` или `/api/feed/main`). Аналогично другие пути могут не совпадать.
- Тест не использует префикс `/api`, хотя все API эндпоинты backend имеют этот префикс.
- `SharedArray` и `open('./test-users.json')` предполагают наличие файла с тестовыми пользователями, но его может не быть.

### 2. load-test/chat-test.js

**Назначение**: нагрузочный тест WebSocket.

**Сценарий**:
- Использует модули `k6/http`, `k6/ws`, `k6/check`, `k6/sleep`.
- Этапы:
  1. 30 секунд, ramp up до 100 VUs.
  2. 1 минута, ramp up до 1000 VUs.
  3. 30 секунд, ramp down до 0 VUs.
- **Пороговые значения**:
  - `http_req_duration`: p(95) < 500 мс (не относится к WebSocket).
  - `ws_connecting`: p(95) < 100 мс.
  - `ws_msgs_received`: count > 0.

**Действия каждого VU**:
- Подключается к `ws://localhost:8081/ws/chat`.
- При открытии соединения отправляет JSON-сообщение с полями `roomSlug: 'general'`, `userId: user_${__VU}`, `content: 'Test message'`.
- Ждёт сообщения на `message` (логгирует его).
- Через 10 секунд закрывает соединение.
- Между VU пауза 1 секунда.

**Проблемы**:
- WebSocket хендлер на бэкенде ожидает handshake с JWT (через `WebSocketHandshakeInterceptor`), а `k6/ws` не предоставляет заголовки или query‑параметры по умолчанию. Тест будет отклонён на этапе handshake.
- Путь `/ws/chat` совпадает с реальным (регистрируется в `WebSocketConfig`), но без JWT handshake не пройдёт.
- Формат сообщения (поле `userId` строкой) не соответствует ожидаемому `ChatMessagePayload` (там тоже `userId` как String), но в тесте нет токена, поэтому сообщение не будет отправлено.
- Количество VU (1000) может быть чрезмерным для тестовой среды.

### 3. load-test/k6-config.js

**Назначение**: общая конфигурация для k6.

**Содержит**:
- `summaryTrendStats`: среднее, мин, медиана, макс, p(90), p(95), p(99).
- **Сценарии**:
  - `smoke`: 10 VU, 1 минута.
  - `load`: ramp up до 100 → 500 → 1000 → 500 → 0 за 3 минуты.
  - `stress`: ramp up до 1000 → 3000 → 5000 за 3.5 минуты.
  - `soak`: 100 запросов/сек, длительность 1 час.
- **Пороговые значения**:
  - По умолчанию для `http_req_duration{type:api}` p(95)<500, p(99)<1000.
  - Для статики p(95)<200.
  - `http_req_failed` < 0.01.
  - `ws_connecting` p(95)<100.
  - `ws_msgs_received` count>0.
- **Теги** для группировки метрик.
- **Вывод**:
  - JSON, CSV, InfluxDB (для Grafana).
- **Настройки HTTP**: таймаут 30 секунд, максимум 5 редиректов.
- **Настройки WebSocket**: таймаут 10 секунд, максимальный размер payload 10 КБ.
- **Утилиты**: `randomString`, `randomEmail`, `randomUser`, `randomSleep`, `checkResponse`.

**Проблемы**:
- Используются переменные окружения `${ENV}`, `${DATE}`, `${INFLUXDB_*}` – они должны быть установлены перед запуском.
- Вывод в InfluxDB требует работающей инфраструктуры.
- Порог `http_req_duration{type:api}` не будет работать, так как в тестах не устанавливается тег `type`.

### Анализ безопасности и рекомендации

1. **Нет JWT аутентификации** в тестах: `api-test.js` пытается вызвать `/auth/login`, которого нет. Необходимо заменить на получение токена через Keycloak API (например, прямой запрос к `/realms/platform/protocol/openid-connect/token` с типом `password`).
2. **WebSocket handshake**: в `chat-test.js` нужно добавить токен в query‑параметр `token` или в заголовки (k6/ws поддерживает заголовки, но не через стандартный `ws.connect` с объектом params).
3. **Префикс `/api`**: все API эндпоинты следует вызывать с префиксом `/api`.
4. **Тестовые пользователи**: `test-users.json` должен содержать валидные учётные данные (username/password). В конфигурации Keycloak есть тестовые пользователи (user0, user1 и т.д.).
5. **Пороговые значения**: для `api-test.js` следует установить реалистичные пороги, исходя из производительности backend (например, p(95)<1000 мс для первого запуска).
6. **Интеграция с CI**: скрипты можно запускать в CI-пайплайне, предварительно развернув тестовое окружение (Docker Compose).

### Выводы по папке load-test

Нагрузочные тесты написаны на k6 и включают как HTTP, так и WebSocket сценарии. Основные проблемы связаны с несоответствием путей и отсутствием JWT аутентификации. После исправления этих несоответствий тесты могут быть использованы для оценки производительности backend. Конфигурационный файл `k6-config.js` предоставляет гибкие сценарии (smoke, load, stress, soak) и настройки вывода, что позволяет интегрировать тесты в CI/CD и мониторинг.
