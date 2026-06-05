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
- С