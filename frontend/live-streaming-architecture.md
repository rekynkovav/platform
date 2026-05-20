# Архитектура Live Streaming системы

## Обзор
Полнофункциональная система live streaming с поддержкой видео/аудио трансляций, чата, управления качеством и демо-режимом.

## Структура проекта

### 1. Сервисы (`./src/services/live/`)
- **`mockWebSocket.ts`** - имитация WebSocket соединения для демо-режима
- **`mockApi.ts`** - мок API для работы с трансляциями
- **`mockMedia.ts`** - сервис для работы с HLS/DASH потоками
- **`types.ts`** - типы данных (LiveBroadcast, LiveChatMessage, BroadcastStatus и др.)

### 2. Контекст состояния (`./src/contexts/live/`)
- **`LiveSessionContext.tsx`** - централизованное управление состоянием live сессии
- Поддерживает: состояние трансляции, чат, воспроизведение, WebSocket соединение

### 3. Компоненты (`./src/components/live/`)
- **`LiveBroadcastView.tsx`** - главный компонент (объединяет плеер, чат, управление)
- **`LiveBroadcastViewSimple.js`** - упрощенная JS версия
- **`LivePlayer.tsx`** - HLS/DASH плеер с управлением качеством
- **`LiveChat.tsx`** - чат с поддержкой сообщений, реакций, вопросов
- **`BroadcastControls.tsx`** - управление трансляцией для админов
- **`DeviceTest.js`** - тестирование устройств (камера, микрофон)
- **`NetworkSpeedTest.js`** - тестирование скорости сети

### 4. Хуки (`./src/hooks/live/`)
- **`useLiveBroadcast.ts`** - управление трансляцией (загрузка, подключение, статус)
- **`useLiveChat.ts`** - работа с чатом (сообщения, фильтры, статистика)

### 5. Утилиты (`./src/utils/live/`)
- **`helpers.ts`** - форматирование, валидация, генерация демо-данных
- **`./src/components/live/utils.ts`** - утилиты для компонентов

### 6. Демо-файлы (`./public/demo/`)
- HLS плейлисты для разных качеств (1080p, 720p, 480p, 360p)
- Аудио поток
- Тестовая HTML страница для проверки медиафайлов

## Ключевые возможности

### 1. Поддержка форматов
- HLS/DASH потоки
- Нативная поддержка (Safari, iOS)
- hls.js для других браузеров
- Видео и аудио трансляции

### 2. Управление качеством
- Автоматическое переключение качества
- Ручной выбор качества (1080p, 720p, 480p, 360p, auto)
- Адаптация к ширине экрана

### 3. Чат система
- Сообщения в реальном времени
- Реакции (эмодзи)
- Вопросы к спикеру
- Модерация сообщений
- Поиск по истории чата

### 4. Статистика и мониторинг
- Битрейт потока
- Длина буфера
- Задержка трансляции
- Количество зрителей онлайн
- Счетчик упавших кадров

### 5. Демо-режим
- Полная имитация без реального бэкенда
- Генерация фейковых данных
- Имитация проблем с сетью
- Тестовые медиафайлы

### 6. Тестирование
- Проверка устройств (камера, микрофон)
- Тестирование скорости сети
- Проверка доступности медиафайлов

### 7. Адаптивный дизайн
- Разные layout'ы:
  - `player-first` - плеер первый, чат второй
  - `chat-first` - чат первый, плеер второй
  - `vertical` - вертикальное расположение
  - `horizontal` - горизонтальное расположение

## Типы данных

### LiveBroadcast
```typescript
interface LiveBroadcast {
  id: string;
  title: string;
  description?: string;
  status: BroadcastStatus; // 'idle' | 'preparing' | 'live' | 'paused' | 'ended' | 'failed'
  streamType: 'video' | 'audio';
  viewerCount: number;
  maxViewers: number;
  startedAt?: string;
  endedAt?: string;
  settings: {
    isPublic: boolean;
    isChatEnabled: boolean;
    isReactionsEnabled: boolean;
    isRecording: boolean;
    allowModeratorControl: boolean;
  };
}
```

### LiveChatMessage
```typescript
interface LiveChatMessage {
  id: string;
  broadcastId: string;
  userId: string;
  username: string;
  userRole: string;
  content: string;
  type: 'message' | 'reaction' | 'question' | 'system';
  timestamp: string;
}
```

## Интеграция с проектом

### Использование в App.js
```javascript
import LiveBroadcastView from './components/live/LiveBroadcastViewSimple';

// В рендере компонента
<LiveBroadcastView 
  broadcast={demoBroadcast}
  showChat={true}
  showControls={true}
  demoMode={true}
/>
```

### Ролевая модель
- **Админ** - полный контроль над трансляцией
- **Модератор** - управление чатом, пауза/возобновление
- **Зритель** - просмотр, чат, реакции

### Совместимость
- Использует существующую систему аутентификации
- Поддерживает демо-роли из localStorage
- Совместима с будущим бэкендом (типы соответствуют API)

## Демо-файлы

### HLS плейлисты
- `/demo/live-stream-1080p.m3u8` - 1920x1080, 5000 Кбит/с
- `/demo/live-stream-720p.m3u8` - 1280x720, 2500 Кбит/с  
- `/demo/live-stream-480p.m3u8` - 854x480, 1000 Кбит/с
- `/demo/live-stream-360p.m3u8` - 640x360, 500 Кбит/с
- `/demo/audio-stream.m3u8` - аудио поток, 128 Кбит/с

### Тестовая страница
- `/demo/test.html` - проверка доступности медиафайлов

## Миграция на реальный бэкенд

### 1. Замена мок-сервисов
- `mockWebSocket.ts` → реальный WebSocket сервер
- `mockApi.ts` → реальные API эндпоинты
- `mockMedia.ts` → реальный медиа сервер (Wowza, nginx-rtmp)

### 2. Настройка медиа сервера
- RTMP ingest для стриминга
- HLS/DASH выходные потоки
- Транскодирование в разные качества

### 3. Интеграция с бэкендом
- Аутентификация и авторизация
- Хранение трансляций в базе данных
- Статистика и аналитика

## Команды для разработки

### Запуск тестового сервера
```bash
# Проверка демо-файлов
open http://localhost:3000/demo/test.html

# Тестирование устройств
# Используйте компонент DeviceTest

# Тестирование скорости сети  
# Используйте компонент NetworkSpeedTest
```

### Генерация демо-данных
```javascript
import { generateDemoBroadcast } from './utils/live/helpers';

const demoBroadcast = generateDemoBroadcast({
  title: 'Моя тестовая трансляция',
  status: 'live'
});
```

## Примечания

1. **Безопасность**: В демо-режиме используются мок-данные, в продакшене необходима реальная аутентификация
2. **Производительность**: HLS.js может потреблять значительные ресурсы на слабых устройствах
3. **Совместимость**: Проверять поддержку HLS в целевых браузерах
4. **Масштабирование**: Для большого количества зрителей требуется CDN и масштабируемый медиа сервер

---

*Документ создан: 2026-05-20*
*Версия системы: 1.0.0*