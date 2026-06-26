// Типы для live streaming системы
// Соответствуют будущим бэкенд-моделям для легкой миграции

export type BroadcastStatus =
  | 'idle'        // Неактивна
  | 'preparing'   // Подготовка к эфиру
  | 'live'        // В эфире
  | 'paused'      // На паузе
  | 'ended'       // Завершена
  | 'failed';     // Ошибка

export type StreamQuality = '1080p' | '720p' | '480p' | '360p' | 'auto';
export type StreamType = 'video' | 'audio';

export interface LiveBroadcast {
  // Основные поля (соответствуют бэкенд-модели LiveBroadcast)
  id: string;
  eventId?: string;           // Ссылка на Event (если есть)
  title: string;
  description?: string;

  // Трансляция
  streamKey: string;          // RTMP stream key (для админа)
  ingestUrl: string;          // RTMP ingest URL
  playbackUrl: string;        // HLS/DASH playback URL
  streamType: StreamType;     // 'video' или 'audio'

  // Статус и метаданные
  status: BroadcastStatus;
  viewerCount: number;
  maxViewers: number;
  startedAt?: string;         // ISO timestamp
  endedAt?: string;           // ISO timestamp
  duration?: number;          // Длительность в секундах

  // Статистика
  stats?: {
    bitrate: number;          // Кбит/с
    quality: StreamQuality;
    latency: number;          // Задержка в секундах
    bufferHealth: number;     // Здоровье буфера (0-100)
    droppedFrames: number;
  };

  // Настройки
  settings?: {
    isChatEnabled: boolean;
    isReactionsEnabled: boolean;
    isRecording: boolean;
    isPublic: boolean;
    allowModeratorControl?: boolean;
    maxViewers?: number;
    qualityOptions?: string[];
    allowedRoles?: string[];  // Роли с доступом
  };

  // Связь с существующей системой
  mediaFileId?: string;       // ID медиафайла для архивации
  linkedEventId?: string;     // ID привязанного события
  createdBy: string;          // ID пользователя
  createdAt: string;          // ISO timestamp
  updatedAt?: string;         // ISO timestamp

  // Demo metadata (not persisted)
  metadata?: {
    tags?: string[];
    category?: string;
    language?: string;
    thumbnail?: string;
  };
}

export interface LiveChatMessage {
  // Соответствует бэкенд-модели LiveChatMessage
  id: string;
  broadcastId: string;
  userId: string;
  username: string;
  userRole: string;
  avatar?: string;

  content: string;
  type: 'message' | 'reaction' | 'question' | 'system';
  metadata?: {
    emoji?: string;
    questionId?: string;
    pinned?: boolean;
    moderated?: boolean;
  };

  timestamp: string;          // ISO timestamp
  isOwn?: boolean;            // Фронтенд-поле
}

export interface ViewerStats {
  broadcastId: string;
  timestamp: string;
  viewerCount: number;
  countries: Record<string, number>;  // Страна → количество
  devices: Record<string, number>;    // Устройство → количество
  peakViewers: number;
  averageWatchTime: number;           // Среднее время просмотра в минутах
}

export interface LiveSession {
  // Состояние текущей live сессии (для контекста)
  broadcast: LiveBroadcast | null;
  isAdmin: boolean;
  isViewer: boolean;
  isModerator: boolean;

  // WebSocket соединение
  wsConnected: boolean;
  wsReconnecting: boolean;

  // Чат
  chatMessages: LiveChatMessage[];
  unreadCount: number;
  isChatPaused: boolean;

  // Воспроизведение
  playerState: {
    isPlaying: boolean;
    currentTime: number;
    volume: number;
    quality: StreamQuality;
    isFullscreen: boolean;
    isMuted: boolean;
  };

  // Взаимодействие
  reactions: Record<string, number>;  // Эмодзи → количество
  polls: any[];                       // Активные опросы
  questions: LiveChatMessage[];       // Вопросы к спикеру
}

export interface WebSocketEvent {
  type: string;
  payload: any;
  timestamp: string;
}

// События WebSocket (соответствуют бэкенд-событиям)
export type WSEventType =
  | 'BROADCAST_STATUS_CHANGED'
  | 'VIEWER_COUNT_UPDATED'
  | 'CHAT_MESSAGE'
  | 'REACTION_ADDED'
  | 'POLL_CREATED'
  | 'POLL_VOTE'
  | 'QUESTION_ASKED'
  | 'QUESTION_ANSWERED'
  | 'STREAM_STATS'
  | 'MODERATION_ACTION'
  | 'SYSTEM_MESSAGE';

export interface MockConfig {
  // Конфигурация мок-данных
  enableMockWebSocket: boolean;
  enableMockMedia: boolean;
  mockLatency: number;        // Задержка в мс
  mockErrorRate: number;      // Вероятность ошибки (0-1)
  generateFakeData: boolean;  // Генерация фейковых данных

  // Демо-данные
  demoBroadcasts: LiveBroadcast[];
  demoUsers: Array<{
    id: string;
    username: string;
    role: string;
    avatar?: string;
  }>;
}

// Типы для API (совместимы с будущим бэкендом)
export interface StartBroadcastRequest {
  eventId?: string;
  title: string;
  description?: string;
  streamType: StreamType;
  settings?: {
    isChatEnabled: boolean;
    isReactionsEnabled: boolean;
    isRecording: boolean;
    isPublic: boolean;
  };
}

export interface StartBroadcastResponse {
  broadcast: LiveBroadcast;
  streamKey: string;
  ingestUrl: string;
  playbackUrl: string;
}

export interface UpdateBroadcastRequest {
  status?: BroadcastStatus;
  title?: string;
  description?: string;
  settings?: Partial<LiveBroadcast['settings']>;
  viewerCount?: number;
  maxViewers?: number;
}

// Утилитарные типы
export type UnsubscribeFn = () => void;
export type EventCallback<T = any> = (data: T) => void;