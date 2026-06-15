// Утилиты для live компонентов
// Вспомогательные функции, валидаторы, генераторы

import { LivePlayerProps } from './LivePlayer';
import { LiveChatProps } from './LiveChat';
import { BroadcastControlsProps } from './BroadcastControls';
import { LiveBroadcastViewProps } from './index';
import { LiveBroadcast, BroadcastStatus, StreamQuality } from '../../services/live/types';

// ==================== СТИЛИ ====================

/**
 * Получение стилей для LivePlayer
 */
export const getPlayerStyles = (): Record<string, string> => ({
  container: 'live-player-container',
  video: 'live-player-video',
  loading: 'live-player-loading',
  error: 'live-player-error',
  stats: 'live-player-stats',
  qualityControls: 'live-player-quality-controls',
  controls: 'live-player-controls',
  info: 'live-player-info'
});

/**
 * Получение стилей для LiveChat
 */
export const getChatStyles = (): Record<string, string> => ({
  container: 'live-chat-container',
  header: 'chat-header',
  filters: 'chat-filters',
  reactions: 'chat-reactions',
  content: 'chat-content',
  messages: 'messages-container',
  input: 'chat-input',
  stats: 'chat-stats'
});

/**
 * Получение стилей для BroadcastControls
 */
export const getControlsStyles = (): Record<string, string> => ({
  container: 'broadcast-controls',
  header: 'controls-header',
  section: 'controls-section',
  status: 'status-display',
  info: 'broadcast-info',
  controls: 'status-controls',
  editForm: 'edit-form',
  qualityControls: 'quality-controls',
  testFunctions: 'test-functions',
  stats: 'stats-panel',
  quickActions: 'quick-actions'
});

/**
 * Получение стилей для LiveBroadcastView
 */
export const getBroadcastViewStyles = (): Record<string, string> => ({
  container: 'live-broadcast-container',
  header: 'broadcast-header',
  navigation: 'broadcast-navigation',
  content: 'broadcast-content',
  loading: 'loading-container',
  error: 'error-container',
  noBroadcast: 'no-broadcast',
  cannotJoin: 'cannot-join',
  demoNotice: 'demo-notice'
});

// ==================== ВАЛИДАЦИЯ ====================

/**
 * Валидация свойств LivePlayer
 */
export const validatePlayerProps = (props: LivePlayerProps): { isValid: boolean; errors: string[] } => {
  const errors: string[] = [];

  if (!props.broadcast) {
    errors.push('Трансляция обязательна');
  }

  if (props.autoPlay && typeof props.autoPlay !== 'boolean') {
    errors.push('autoPlay должен быть boolean');
  }

  if (props.muted && typeof props.muted !== 'boolean') {
    errors.push('muted должен быть boolean');
  }

  if (props.controls && typeof props.controls !== 'boolean') {
    errors.push('controls должен быть boolean');
  }

  return {
    isValid: errors.length === 0,
    errors
  };
};

/**
 * Валидация свойств LiveChat
 */
export const validateChatProps = (props: LiveChatProps): { isValid: boolean; errors: string[] } => {
  const errors: string[] = [];

  if (!props.broadcastId) {
    errors.push('ID трансляции обязателен');
  }

  if (props.maxHeight && typeof props.maxHeight !== 'string') {
    errors.push('maxHeight должен быть строкой');
  }

  if (props.showReactions && typeof props.showReactions !== 'boolean') {
    errors.push('showReactions должен быть boolean');
  }

  if (props.showQuestions && typeof props.showQuestions !== 'boolean') {
    errors.push('showQuestions должен быть boolean');
  }

  if (props.showUserList && typeof props.showUserList !== 'boolean') {
    errors.push('showUserList должен быть boolean');
  }

  if (props.compact && typeof props.compact !== 'boolean') {
    errors.push('compact должен быть boolean');
  }

  return {
    isValid: errors.length === 0,
    errors
  };
};

/**
 * Валидация свойств BroadcastControls
 */
export const validateControlsProps = (props: BroadcastControlsProps): { isValid: boolean; errors: string[] } => {
  const errors: string[] = [];

  if (!props.broadcast) {
    errors.push('Трансляция обязательна');
  }

  if (props.showAdvanced && typeof props.showAdvanced !== 'boolean') {
    errors.push('showAdvanced должен быть boolean');
  }

  return {
    isValid: errors.length === 0,
    errors
  };
};

/**
 * Валидация свойств LiveBroadcastView
 */
export const validateBroadcastViewProps = (props: LiveBroadcastViewProps): { isValid: boolean; errors: string[] } => {
  const errors: string[] = [];

  if (!props.broadcastId && !props.broadcast) {
    errors.push('Требуется broadcastId или broadcast');
  }

  if (props.layout && !['player-first', 'chat-first', 'vertical', 'horizontal'].includes(props.layout)) {
    errors.push('Недопустимый layout. Допустимые значения: player-first, chat-first, vertical, horizontal');
  }

  if (props.showChat && typeof props.showChat !== 'boolean') {
    errors.push('showChat должен быть boolean');
  }

  if (props.showControls && typeof props.showControls !== 'boolean') {
    errors.push('showControls должен быть boolean');
  }

  if (props.showStats && typeof props.showStats !== 'boolean') {
    errors.push('showStats должен быть boolean');
  }

  if (props.autoPlay && typeof props.autoPlay !== 'boolean') {
    errors.push('autoPlay должен быть boolean');
  }

  if (props.demoMode && typeof props.demoMode !== 'boolean') {
    errors.push('demoMode должен быть boolean');
  }

  return {
    isValid: errors.length === 0,
    errors
  };
};

// ==================== ГЕНЕРАТОРЫ ====================

/**
 * Генерация уникального ID для плеера
 */
export const generatePlayerId = (broadcastId: string): string => {
  return `player-${broadcastId}-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
};

/**
 * Генерация уникального ID для чата
 */
export const generateChatId = (broadcastId: string): string => {
  return `chat-${broadcastId}-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
};

/**
 * Генерация уникального ID для управления
 */
export const generateControlsId = (broadcastId: string): string => {
  return `controls-${broadcastId}-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
};

/**
 * Генерация уникального ID для представления трансляции
 */
export const generateBroadcastViewId = (broadcastId: string): string => {
  return `broadcast-view-${broadcastId}-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
};

// ==================== КОНСТАНТЫ ====================

/**
 * Значения по умолчанию для LivePlayer
 */
export const PLAYER_DEFAULT_PROPS: Partial<LivePlayerProps> = {
  autoPlay: true,
  muted: false,
  controls: true
};

/**
 * Значения по умолчанию для LiveChat
 */
export const CHAT_DEFAULT_PROPS: Partial<LiveChatProps> = {
  maxHeight: '500px',
  showReactions: true,
  showQuestions: true,
  showUserList: false,
  compact: false
};

/**
 * Значения по умолчанию для BroadcastControls
 */
export const CONTROLS_DEFAULT_PROPS: Partial<BroadcastControlsProps> = {
  showAdvanced: false
};

/**
 * Значения по умолчанию для LiveBroadcastView
 */
export const BROADCAST_VIEW_DEFAULT_PROPS: Partial<LiveBroadcastViewProps> = {
  autoPlay: true,
  showChat: true,
  showControls: true,
  showStats: false,
  layout: 'player-first',
  demoMode: false
};

// ==================== ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ ====================

/**
 * Получение цвета для статуса трансляции
 */
export const getStatusColor = (status: BroadcastStatus): string => {
  const colors: Record<BroadcastStatus, string> = {
    'idle': '#666',
    'preparing': '#2196F3',
    'live': '#4CAF50',
    'paused': '#ff9800',
    'ended': '#f44336',
    'failed': '#9c27b0'
  };
  return colors[status] || '#666';
};

/**
 * Получение текста для статуса трансляции
 */
export const getStatusText = (status: BroadcastStatus): string => {
  const texts: Record<BroadcastStatus, string> = {
    'idle': 'Ожидание',
    'preparing': 'Подготовка',
    'live': 'В эфире',
    'paused': 'На паузе',
    'ended': 'Завершена',
    'failed': 'Ошибка'
  };
  return texts[status] || 'Неизвестно';
};

/**
 * Проверка возможности управления трансляцией
 */
export const canControlBroadcast = (broadcast: LiveBroadcast, isAdmin: boolean): boolean => {
  if (!broadcast) return false;
  return isAdmin || (broadcast.settings as any)?.allowModeratorControl;
};

/**
 * Проверка возможности присоединиться к трансляции
 */
export const canJoinBroadcast = (broadcast: LiveBroadcast | null): boolean => {
  if (!broadcast) return false;
  return ['live', 'paused'].includes(broadcast.status);
};

/**
 * Получение разрешения для качества
 */
export const getResolutionForQuality = (quality: StreamQuality): { width: number; height: number } | null => {
  const resolutions: Record<StreamQuality, { width: number; height: number }> = {
    '1080p': { width: 1920, height: 1080 },
    '720p': { width: 1280, height: 720 },
    '480p': { width: 854, height: 480 },
    '360p': { width: 640, height: 360 },
    'auto': { width: 0, height: 0 }
  };
  return resolutions[quality] || null;
};

/**
 * Получение рекомендуемого качества на основе ширины экрана
 */
export const getRecommendedQuality = (width: number): StreamQuality => {
  if (width >= 1920) return '1080p';
  if (width >= 1280) return '720p';
  if (width >= 854) return '480p';
  return '360p';
};

/**
 * Форматирование длительности
 */
export const formatDuration = (seconds: number): string => {
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  const secs = Math.floor(seconds % 60);

  if (hours > 0) {
    return `${hours}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  }
  return `${minutes}:${secs.toString().padStart(2, '0')}`;
};

/**
 * Форматирование битрейта
 */
export const formatBitrate = (bitrate: number): string => {
  if (bitrate >= 1000000) {
    return `${(bitrate / 1000000).toFixed(1)} Mbps`;
  }
  if (bitrate >= 1000) {
    return `${(bitrate / 1000).toFixed(1)} Kbps`;
  }
  return `${bitrate} bps`;
};

/**
 * Форматирование времени сообщения
 */
export const formatMessageTime = (timestamp: string | Date): string => {
  const date = new Date(timestamp);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMins = Math.floor(diffMs / 60000);

  if (diffMins < 1) return 'только что';
  if (diffMins < 60) return `${diffMins} мин назад`;

  const diffHours = Math.floor(diffMins / 60);
  if (diffHours < 24) return `${diffHours} ч назад`;

  return date.toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' });
};

/**
 * Получение цвета роли пользователя
 */
export const getRoleColor = (role: string): string => {
  const colors: Record<string, string> = {
    'admin': '#f44336',
    'moderator': '#ff9800',
    'speaker': '#2196F3',
    'vip': '#9c27b0',
    'user': '#4CAF50'
  };
  return colors[role] || '#666';
};

/**
 * Получение текста роли пользователя
 */
export const getRoleText = (role: string): string => {
  const texts: Record<string, string> = {
    'admin': 'Админ',
    'moderator': 'Модератор',
    'speaker': 'Спикер',
    'vip': 'VIP',
    'user': 'Пользователь'
  };
  return texts[role] || 'Гость';
};

/**
 * Проверка, является ли сообщение системным
 */
export const isSystemMessage = (message: any): boolean => {
  return message.type === 'system' || message.userRole === 'system';
};

/**
 * Проверка валидности сообщения чата
 */
export const isValidChatMessage = (message: string): boolean => {
  if (!message || typeof message !== 'string') return false;
  const trimmed = message.trim();
  return trimmed.length > 0 && trimmed.length <= 1000;
};

/**
 * Генерация демо трансляции
 */
export const generateDemoBroadcast = (overrides: Partial<LiveBroadcast> = {}): LiveBroadcast => {
  const defaultBroadcast: LiveBroadcast = {
    id: 'demo-broadcast-' + Date.now(),
    title: 'Демо трансляция: Live Streaming System',
    description: 'Тестовая трансляция для демонстрации возможностей live streaming системы',
    status: 'live',
    streamKey: 'demo-stream-key',
    ingestUrl: 'rtmp://demo-server/live/demo',
    playbackUrl: '/demo/live-stream-720p.m3u8',
    streamType: 'video',
    createdBy: 'demo-admin',
    viewerCount: Math.floor(Math.random() * 100) + 50,
    maxViewers: 1000,
    startedAt: new Date(Date.now() - 3600000).toISOString(), // 1 час назад
    endedAt: null,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    settings: {
      isPublic: true,
      isChatEnabled: true,
      isReactionsEnabled: true,
      isRecording: true,
      allowModeratorControl: true,
      maxViewers: 1000,
      qualityOptions: ['1080p', '720p', '480p', '360p', 'auto']
    },
    metadata: {
      tags: ['demo', 'test', 'live-streaming'],
      category: 'Технологии',
      language: 'ru',
      thumbnail: '/demo/thumbnail.jpg'
    }
  };

  return { ...defaultBroadcast, ...overrides };
};

/**
 * Создание мок сообщения чата
 */
export const createMockChatMessage = (overrides: any = {}) => {
  const roles = ['user', 'user', 'user', 'moderator', 'vip'];
  const avatars = ['👤', '👨', '👩', '👨‍💻', '👩‍💻', '🎤', '🎮', '📱'];
  const messages = [
    'Привет всем! Как дела?',
    'Отличная трансляция!',
    'Спасибо за полезную информацию',
    'Есть вопросы по теме?',
    'Можно подробнее про архитектуру?',
    'Какие технологии используете?',
    'Как настроить HLS поток?',
    'Есть ли документация?',
    'Спасибо за демо!',
    'Когда следующая трансляция?'
  ];

  const role = roles[Math.floor(Math.random() * roles.length)];
  const avatar = avatars[Math.floor(Math.random() * avatars.length)];
  const content = messages[Math.floor(Math.random() * messages.length)];

  return {
    id: `msg-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
    broadcastId: 'demo-broadcast',
    userId: `user-${Math.floor(Math.random() * 1000)}`,
    username: `User${Math.floor(Math.random() * 1000)}`,
    userRole: role,
    avatar: avatar,
    content: content,
    type: 'message',
    timestamp: new Date().toISOString(),
    metadata: {},
    ...overrides
  };
};

/**
 * Создание мок реакции
 */
export const createMockReaction = (overrides: any = {}) => {
  const emojis = ['👍', '👏', '❤️', '🔥', '🎉', '🤔', '😂', '😮'];

  return {
    id: `reaction-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
    broadcastId: 'demo-broadcast',
    userId: `user-${Math.floor(Math.random() * 1000)}`,
    emoji: emojis[Math.floor(Math.random() * emojis.length)],
    count: Math.floor(Math.random() * 10) + 1,
    timestamp: new Date().toISOString(),
    ...overrides
  };
};

/**
 * Создание мок вопроса
 */
export const createMockQuestion = (overrides: any = {}) => {
  const questions = [
    'Как настроить live streaming?',
    'Какие форматы поддерживаются?',
    'Есть ли ограничения по зрителям?',
    'Как работает чат?',
    'Можно ли записывать трансляции?',
    'Как настроить качество видео?',
    'Есть ли мобильное приложение?',
    'Как интегрировать с моим сайтом?'
  ];

  return {
    id: `question-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
    broadcastId: 'demo-broadcast',
    userId: `user-${Math.floor(Math.random() * 1000)}`,
    username: `User${Math.floor(Math.random() * 1000)}`,
    content: questions[Math.floor(Math.random() * questions.length)],
    type: 'question',
    timestamp: new Date().toISOString(),
    metadata: {
      answered: Math.random() > 0.5,
      answeredAt: Math.random() > 0.5 ? new Date().toISOString() : null
    },
    ...overrides
  };
};

// ==================== ЭКСПОРТ ====================

export default {
  // Стили
  getPlayerStyles,
  getChatStyles,
  getControlsStyles,
  getBroadcastViewStyles,

  // Валидация
  validatePlayerProps,
  validateChatProps,
  validateControlsProps,
  validateBroadcastViewProps,

  // Генераторы
  generatePlayerId,
  generateChatId,
  generateControlsId,
  generateBroadcastViewId,

  // Константы
  PLAYER_DEFAULT_PROPS,
  CHAT_DEFAULT_PROPS,
  CONTROLS_DEFAULT_PROPS,
  BROADCAST_VIEW_DEFAULT_PROPS,

  // Вспомогательные функции
  getStatusColor,
  getStatusText,
  canControlBroadcast,
  canJoinBroadcast,
  getResolutionForQuality,
  getRecommendedQuality,
  formatDuration,
  formatBitrate,
  formatMessageTime,
  getRoleColor,
  getRoleText,
  isSystemMessage,
  isValidChatMessage,
  generateDemoBroadcast,
  createMockChatMessage,
  createMockReaction,
  createMockQuestion
};