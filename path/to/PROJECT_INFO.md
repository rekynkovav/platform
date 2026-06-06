# Проект "Platform" — оставшиеся замечания

## Общая архитектура

- **Тип приложения**: одностраничное приложение (SPA) на React 18.
- **Сборка**: Create React App (`react-scripts` 5), Webpack 5.
- **Аутентификация**: Keycloak (OpenID Connect, PKCE S256).
- **Взаимодействие с сервером**: REST (axios), WebSocket (восстановлен).
- **Управление состоянием**: React Context (AudioPlayerContext) + локальное состояние хуков.
- **Навигация**: React Router (полноценно внедрён).
- **Стилизация**: чистый CSS (CSS-переменные), только тёмная тема.
- **Тестирование**: добавлен базовый тест для `usePermissions.js`.

## Стек технологий (из `package.json`)

(без изменений)

## Структура папок `frontend/src`

(без изменений)

## Подробный анализ каждого файла (только неисправленные замечания)

### 1. `frontend/package.json`

- `jest-worker` перемещён в `devDependencies`. Исправлено.

### 2. `frontend/src/App.js`

- `handleLogin` теперь использует `login()`. Исправлено.
- Нет обработки истечения сессии – открытый вопрос (обработчик добавлен в `keycloak.js`).

### 3. `frontend/src/keycloak.js`

- `setInterval` очищается при повторной инициализации и при logout. Исправлено.
- Добавлен обработчик `onTokenExpired`. Исправлено.

### 4. `frontend/src/hooks/usePermissions.js`

- Добавлена проверка на пустой массив ролей. Исправлено.
- Добавлен unit-тест (`usePermissions.test.js`). Исправлено.

### 5. `frontend/src/hooks/useWebSocket.js`

- Восстановлен (полноценная реализация). Исправлено.

### 6. `frontend/src/context/AudioPlayerContext.js`

- `animationFrameRef` отменяется при паузе. Исправлено.
- Обработка ошибок при загрузке медиа добавлена. Исправлено.
- `hideTimeoutRef` очищается в `stopTrack`. Исправлено.
- Выбор `audioRef`/`videoRef` исправлен (условие `currentTrack?.type === 'video'`). Исправлено.

### 7. `frontend/src/components/Navigation/NavTabs.js`

- Условие для трансляций только `canStartBroadcast()`, без `hasLevel(4)`. Исправлено.
- Использует React Router. Исправлено.

### 8. `frontend/src/components/Feed/MainFeed.js`

- Опечатка в `getFileType` исправлена. Исправлено.
- Blob URL очищаются при отмене добавления. Исправлено.
- Добавлена пагинация. Исправлено.
- Дублирование логики записи аудио – остаётся (архитектурное).

### 9. `frontend/src/components/Dashboard/Dashboard.js`

- Blob URL очищаются при закрытии модального окна. Исправлено.
- Убран дублирующий `if (process.env.NODE_ENV === 'development')` в `saveMenuData`. Исправлено.
- Огромный компонент – остаётся (требует рефакторинга).

### 10. `frontend/src/components/live/LiveBroadcastView.js`

- `useWebSocket` восстановлен (хук существует). Исправлено.
- Интервал опроса увеличен до 30 секунд. Исправлено.
- Отображение `wsError` добавлено. Исправлено.

### 11. `frontend/src/components/Chat/GeneralChat.js`

- Импорт `useWebSocket` работает. Исправлено.
- Добавлена фильтрация дубликатов через `messageIdsRef`. Исправлено.
- Устаревшее замыкание `page` устранено через `pageRef`. Исправлено.
- `wsError` отображается на UI. Исправлено.

### 12. `frontend/src/components/UserProfile/UserProfile.js`

- `alert` заменён на `InfoModal`. Исправлено.
- Ошибка от `paymentApi.getPaymentQR` обрабатывается. Исправлено.
- Реквизиты остаются жёстко зашиты – архитектурное улучшение.

### 13. `frontend/src/services/api.js`

- Проверка `isTokenExpired()` перед `updateToken()`. Исправлено.
- Вызов `logout()` при 401. Исправлено.
- Удалён `demoApi`. Исправлено.

### 14. `.env`

- `MEDIA_ENCRYPTION_KEY` заполнен (`change-me-in-production`). Исправлено.

### 15. `frontend/src/components/Dashboard/initialData.js`

- Сокращён до 5 записей на категорию. Исправлено.

### 16. `frontend/src/hooks/usePermissions.js`

- Пустой массив обрабатывается. Исправлено.

## Заключение

Все критические проблемы, сломанные импорты, утечки ресурсов, некорректные проверки прав устранены. Добавлен первый unit-тест. Архитектурные улучшения (разделение больших компонентов, вынос модалок, TypeScript, полное покрытие тестами) остаются за рамками текущей задачи.
