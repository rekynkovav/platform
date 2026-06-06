# Статус покрытия тестами фронтенда

## Выполненные тесты

| Компонент / хук / сервис | Файл теста | Статус |
|--------------------------|------------|--------|
| `usePermissions` | `usePermissions.test.js` | ✅ |
| `useWebSocket` | `useWebSocket.test.js` | ✅ |
| `useAudioRecorder` | `useAudioRecorder.test.js` | ✅ |
| `usePostAudioControls` | `usePostAudioControls.test.js` | ✅ |
| `api.js` (ApiClient + endpoints) | `api.test.js` | ✅ |
| `GeneralChat` | `GeneralChat.test.js` | ✅ |
| `LiveBroadcastView` | `LiveBroadcastView.test.js` | ✅ |
| `NavTabs` | `NavTabs.test.js` | ✅ |
| `MainFeed` | `MainFeed.test.js` | ✅ |
| `Dashboard` | `Dashboard.test.js` | ✅ |
| `UserProfile` | `UserProfile.test.js` | ✅ |
| `AudioPlayerContext` | `AudioPlayerContext.test.js` | ✅ |
| `App` | `App.test.js` | ✅ |
| `ConfirmModal` | `ConfirmModal.test.js` | ✅ |
| `AddContentModal` | `AddContentModal.test.js` | ✅ |
| `InfoModal` | `InfoModal.test.js` | ✅ |
| `ProtectedRoute` | `ProtectedRoute.test.js` | ✅ |

## Оставшиеся тесты (по желанию)

- `constants/roles.js` — можно написать тест для структуры и экспортов.
- `constants/fallbackPosts.js` — если используется.
- `Navigation/NavTabs.css` — стили, тесты не пишутся.
- `index.css`, `App.css` — стили, не тестируются.

## Запуск тестов

```bash
cd frontend
npm test -- --coverage
```

Всего тестов: **17 файлов**.
