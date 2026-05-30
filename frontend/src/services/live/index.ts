// Главный файл экспорта для live streaming системы
// Централизованный экспорт всех сервисов, типов и утилит

// Типы
export * from './types';

// Мок-сервисы (для демо, заменяются на реальные после миграции)
export { mockWebSocket, MockWebSocketService } from './mockWebSocket';
export { mockLiveApi, MockLiveApi } from './mockApi';
export { mockMediaService, MockMediaService } from './mockMedia';

// Контексты
export { LiveSessionProvider, useLiveSession, useBroadcast, useChatMessages, usePlayerState, useIsAdmin, useIsViewer, useConnectionStatus } from '../../context/live/LiveSessionContext';

// Хуки
export { useLiveBroadcast, useLiveBroadcasts, useBroadcastStats, useBroadcastAvailability } from '../../hooks/live/useLiveBroadcast';
export { useLiveChat, useLiveReactions, useLiveQuestions } from '../../hooks/live/useLiveChat';

// Утилиты
export {
  // Форматирование
  formatDuration,
  formatDate,
  formatRelativeTime,
  formatCountdown,
  formatNumber,
  formatBitrate,
  formatFileSize,

  // Валидация
  isValidStreamUrl,
  isValidStreamKey,
  isValidBroadcastTitle,
  isValidChatMessage,

  // Статусы
  getStatusColor,
  getStatusText,
  canJoinBroadcast,
  canControlBroadcast,

  // Качество
  getResolutionForQuality,
  getRecommendedQuality,
  getBitrateForQuality,

  // Чат
  formatMessageTime,
  getRoleColor,
  getRoleText,
  isSystemMessage,

  // Вспомогательные
  pluralize,
  generateId,
  delay,
  throttle,
  debounce,
  safeJsonParse,
  copyToClipboard,
  downloadFile,

  // Демо-данные
  generateDemoChatMessage,
  generateDemoBroadcast
} from '../../utils/live/helpers';

// Конфигурация
export const LIVE_CONFIG = {
  // Настройки WebSocket
  WS_RECONNECT_DELAY: 3000,
  WS_MAX_RECONNECT_ATTEMPTS: 5,
  WS_PING_INTERVAL: 30000,

  // Настройки чата
  CHAT_HISTORY_LIMIT: 200,
  CHAT_MESSAGE_LIMIT: 1000,
  CHAT_RATE_LIMIT: 5, // сообщений в секунду

  // Настройки плеера
  PLAYER_BUFFER_TARGET: 10,
  PLAYER_MAX_BUFFER: 30,
  PLAYER_QUALITY_SWITCH_THRESHOLD: 0.75,

  // Настройки трансляции
  BROADCAST_MAX_DURATION: 24 * 60 * 60, // 24 часа в секундах
  BROADCAST_AUTO_ARCHIVE_AFTER: 7 * 24 * 60 * 60, // 7 дней в секундах

  // Демо-режим
  DEMO_MODE_ENABLED: true,
  DEMO_FAKE_LATENCY: 500,
  DEMO_FAKE_ERROR_RATE: 0.05,

  // Пути к демо-файлам
  DEMO_MEDIA_PATHS: {
    VIDEO_1080P: '/demo/live-stream-1080p.m3u8',
    VIDEO_720P: '/demo/live-stream-720p.m3u8',
    VIDEO_480P: '/demo/live-stream-480p.m3u8',
    VIDEO_360P: '/demo/live-stream-360p.m3u8',
    AUDIO: '/demo/audio-stream.m3u8'
  }
} as const;

// Алиасы для будущей миграции
// После подключения реального бэкенда заменяем эти экспорты

// Реальный WebSocket (после миграции)
// export { realWebSocket as liveWebSocket } from './realWebSocket';
export { mockWebSocket as liveWebSocket } from './mockWebSocket';

// Реальный API (после миграции)
// export { realLiveApi as liveApi } from './realApi';
export { mockLiveApi as liveApi } from './mockApi';

// Реальный медиа сервис (после миграции)
// export { realMediaService as mediaService } from './realMedia';
export { mockMediaService as mediaService } from './mockMedia';

// Типы для TypeScript
export type {
  // Из типов
  LiveBroadcast,
  LiveChatMessage,
  BroadcastStatus,
  StreamQuality,
  LiveSession,

  // Из WebSocket
  MockWebSocketService as LiveWebSocketService,

  // Из API
  MockLiveApi as LiveApiService,

  // Из медиа
  MockMediaService as MediaService
};

// Утилита для проверки готовности системы
export function isLiveSystemReady(): boolean {
  // Проверяем, что все необходимые зависимости доступны
  const checks = {
    sessionStorage: typeof sessionStorage !== 'undefined',
    webSocket: typeof WebSocket !== 'undefined',
    mediaSource: typeof MediaSource !== 'undefined',
    fetch: typeof fetch !== 'undefined'
  };

  const allReady = Object.values(checks).every(Boolean);

  if (!allReady) {
    console.warn('[LiveSystem] Некоторые зависимости недоступны:', checks);
  }

  return allReady;
}

// Инициализация системы
export async function initializeLiveSystem(options?: {
  enableMock?: boolean;
  demoMode?: boolean;
  config?: Partial<typeof LIVE_CONFIG>;
}): Promise<boolean> {
  console.log('[LiveSystem] Инициализация live streaming системы...');

  const config = {
    ...LIVE_CONFIG,
    ...options?.config
  };

  // Проверяем готовность системы
  if (!isLiveSystemReady()) {
    console.error('[LiveSystem] Система не готова. Проверьте зависимости.');
    return false;
  }

  // Настройка демо-режима
  if (options?.demoMode !== false) {
    console.log('[LiveSystem] Демо-режим включен');

    // Инициализируем демо-данные
    try {
      // Загружаем или создаем демо-трансляции
      const saved = process.env.NODE_ENV === 'development'
          ? sessionStorage.getItem('demoLiveBroadcasts')
          : null;
      if (!saved) {
        const demoBroadcasts = [
          generateDemoBroadcast({ id: 'demo-live-001', status: 'live', title: 'Прямой эфир: Основы здорового питания' }),
          generateDemoBroadcast({ id: 'demo-live-002', status: 'scheduled', title: 'Аудио трансляция: Медитация' })
        ];
        if (process.env.NODE_ENV === 'development') {
            sessionStorage.setItem('demoLiveBroadcasts', JSON.stringify(demoBroadcasts));
        }
      }
    } catch (error) {
      console.warn('[LiveSystem] Ошибка инициализации демо-данных:', error);
    }
  }

  // Инициализация WebSocket (мок)
  if (options?.enableMock !== false) {
    console.log('[LiveSystem] Мок-сервисы инициализированы');
  }

  console.log('[LiveSystem] Инициализация завершена успешно');
  return true;
}

// Очистка системы
export function cleanupLiveSystem(): void {
  console.log('[LiveSystem] Очистка live streaming системы...');

  // Останавливаем все активные соединения
  try {
    // В реальной системе здесь была бы очистка WebSocket соединений
    // mockWebSocket.disconnect();

    // Очищаем локальное хранилище (только демо-данные)
    if (process.env.NODE_ENV === 'development') {
        sessionStorage.removeItem('demoLiveBroadcasts');
    }

    console.log('[LiveSystem] Очистка завершена');
  } catch (error) {
    console.error('[LiveSystem] Ошибка очистки:', error);
  }
}

// Экспорт по умолчанию для удобного импорта
export default {
  // Сервисы
  liveWebSocket,
  liveApi,
  mediaService,

  // Контексты и провайдеры
  LiveSessionProvider,

  // Хуки
  useLiveSession,
  useLiveBroadcast,
  useLiveChat,
  useLiveReactions,
  useLiveQuestions,

  // Утилиты
  formatDuration,
  formatDate,
  getStatusColor,
  getStatusText,

  // Конфигурация
  LIVE_CONFIG,

  // Функции управления
  isLiveSystemReady,
  initializeLiveSystem,
  cleanupLiveSystem,

  // Типы
  types: {
    LiveBroadcast,
    LiveChatMessage,
    BroadcastStatus,
    StreamQuality
  }
};