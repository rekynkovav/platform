# План покрытия фронтенда тестами

## Цель
Обеспечить стабильность ключевых модулей приложения, предотвратить регрессии при рефакторинге и добавлении нового функционала.

## Инструменты
- **Jest** (встроен в CRA) — runner и assertion.
- **React Testing Library** — для тестирования компонентов.
- **Mock Service Worker (MSW)** — для перехвата HTTP-запросов (альтернатива — jest.mock axios).
- **jest-websocket-mock** — для тестирования WebSocket хуков.

## Приоритеты (по убыванию важности)

### 1. Хуки (логика без UI)
- `usePermissions` — уже есть базовый тест.
- `useWebSocket` — критичен, т.к. используется в чате и трансляциях.
- `useAudioRecorder` — дублируется в MainFeed и Dashboard.
- `usePostAudioControls` — управление плеером.

### 2. Сервисы (api.js)
- `api.js` — interceptor, обработка 401, обновление токена.
- Каждый endpoint (broadcastApi, chatApi, paymentApi и т.д.) — проверить, что вызывается правильный URL.

### 3. Компоненты с бизнес-логикой
- `GeneralChat` — отправка сообщений, загрузка истории, WebSocket.
- `LiveBroadcastView` — старт/стоп трансляции, WebSocket.
- `MainFeed` — пагинация, создание/редактирование/удаление поста.
- `Dashboard` — добавление/редактирование/удаление элементов меню.
- `UserProfile` — создание платежа, загрузка QR.

### 4. Компоненты навигации
- `NavTabs` — отображение вкладок в зависимости от прав.
- `App` — инициализация Keycloak, маршрутизация.

### 5. Контексты
- `AudioPlayerContext` — play/pause/stop, переключение треков.

## Структура тестов

```
frontend/src/__tests__/
├── hooks/
│   ├── usePermissions.test.js       (существует)
│   ├── useWebSocket.test.js
│   ├── useAudioRecorder.test.js
│   └── usePostAudioControls.test.js
├── services/
│   └── api.test.js
├── components/
│   ├── Chat/
│   │   └── GeneralChat.test.js
│   ├── Feed/
│   │   └── MainFeed.test.js
│   ├── Dashboard/
│   │   └── Dashboard.test.js
│   ├── live/
│   │   └── LiveBroadcastView.test.js
│   ├── Navigation/
│   │   └── NavTabs.test.js
│   └── UserProfile/
│       └── UserProfile.test.js
├── context/
│   └── AudioPlayerContext.test.js
└── App.test.js
```

## Примеры тестов (начальные)

### useWebSocket.test.js
- Проверить, что при монтировании устанавливается соединение.
- Проверить, что `send` отправляет JSON.
- Проверить, что при получении сообщения вызывается `onMessage`.
- Проверить, что при ошибке устанавливается `error`.

### api.test.js
- Проверить, что `broadcastApi.getActive()` вызывает GET `/api/broadcast/active`.
- Проверить, что при 401 вызывается `logout()`.
- Проверить, что `updateToken` вызывается только если `isTokenExpired()`.

### GeneralChat.test.js
- Проверить, что при загрузке отображается список сообщений.
- Проверить, что при отправке сообщения вызывается `chatApi.sendMessage`.
- Проверить, что при получении WebSocket-сообщения оно добавляется в список.
- Проверить, что ошибки отображаются.

### NavTabs.test.js
- Проверить, что для роли `USER_0` отображаются только базовые вкладки.
- Проверить, что для роли `MODERATOR_ADMIN` отображаются дополнительные вкладки (Dashboard, Трансляции, Админка).

## Запуск тестов
```bash
cd frontend
npm test -- --coverage
```

## План внедрения
1. Написать тесты для хуков (useWebSocket, useAudioRecorder).
2. Написать тесты для api.js.
3. Написать тесты для GeneralChat и LiveBroadcastView (наиболее критичные).
4. Постепенно покрыть остальные компоненты.
5. Добавить тесты для AudioPlayerContext.

## Ожидаемый результат
- Покрытие строк кода (statements) не менее 60%.
- Покрытие ветвлений (branches) не менее 50%.
- Все критические сценарии (отправка сообщения, создание платежа, старт трансляции) покрыты.
