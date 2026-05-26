// Утилиты для работы с live streaming
// Вспомогательные функции для форматирования, валидации и т.д.

import {
  LiveBroadcast,
  BroadcastStatus,
  LiveChatMessage,
  StreamQuality
} from '../../services/live/types';

// Форматирование времени
// ======================

/**
 * Форматирование длительности в секундах в читаемый формат
 */
export function formatDuration(seconds: number): string {
  if (!seconds || seconds < 0) return '00:00';

  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  const secs = Math.floor(seconds % 60);

  if (hours > 0) {
    return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  }

  return `${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
}

/**
 * Форматирование даты для отображения
 */
export function formatDate(date: string | Date, format: 'short' | 'long' | 'relative' = 'short'): string {
  const d = typeof date === 'string' ? new Date(date) : date;

  if (format === 'relative') {
    return formatRelativeTime(d);
  }

  if (format === 'long') {
    return d.toLocaleString('ru-RU', {
      day: 'numeric',
      month: 'long',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  // short format
  return d.toLocaleString('ru-RU', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
}

/**
 * Форматирование относительного времени
 */
export function formatRelativeTime(date: Date): string {
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffSec = Math.floor(diffMs / 1000);
  const diffMin = Math.floor(diffSec / 60);
  const diffHour = Math.floor(diffMin / 60);
  const diffDay = Math.floor(diffHour / 24);

  if (diffSec < 60) {
    return 'только что';
  } else if (diffMin < 60) {
    return `${diffMin} ${pluralize(diffMin, ['минуту', 'минуты', 'минут'])} назад`;
  } else if (diffHour < 24) {
    return `${diffHour} ${pluralize(diffHour, ['час', 'часа', 'часов'])} назад`;
  } else if (diffDay < 7) {
    return `${diffDay} ${pluralize(diffDay, ['день', 'дня', 'дней'])} назад`;
  } else {
    return formatDate(date, 'short');
  }
}

/**
 * Форматирование времени для таймера обратного отсчета
 */
export function formatCountdown(seconds: number): string {
  if (seconds <= 0) return '00:00:00';

  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  const secs = Math.floor(seconds % 60);

  return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
}

// Форматирование чисел
// ====================

/**
 * Форматирование больших чисел (зрителей, просмотров)
 */
export function formatNumber(num: number): string {
  if (num >= 1000000) {
    return `${(num / 1000000).toFixed(1).replace('.0', '')}M`;
  } else if (num >= 1000) {
    return `${(num / 1000).toFixed(1).replace('.0', '')}K`;
  }
  return num.toString();
}

/**
 * Форматирование битрейта
 */
export function formatBitrate(bitrate: number): string {
  if (bitrate >= 1000) {
    return `${(bitrate / 1000).toFixed(1)} Мбит/с`;
  }
  return `${bitrate.toFixed(0)} Кбит/с`;
}

/**
 * Форматирование размера файла
 */
export function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 Б';

  const k = 1024;
  const sizes = ['Б', 'КБ', 'МБ', 'ГБ', 'ТБ'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));

  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(2))} ${sizes[i]}`;
}

// Валидация
// =========

/**
 * Валидация URL трансляции
 */
export function isValidStreamUrl(url: string): boolean {
  if (!url) return false;

  try {
    const parsed = new URL(url);
    const validProtocols = ['rtmp:', 'rtmps:', 'http:', 'https:', 'webrtc:'];
    return validProtocols.includes(parsed.protocol);
  } catch {
    return false;
  }
}

/**
 * Валидация stream key
 */
export function isValidStreamKey(key: string): boolean {
  if (!key) return false;
  // Stream key должен быть не короче 4 символов и содержать только безопасные символы
  return key.length >= 4 && /^[a-zA-Z0-9_-]+$/.test(key);
}

/**
 * Валидация названия трансляции
 */
export function isValidBroadcastTitle(title: string): boolean {
  if (!title) return false;
  const trimmed = title.trim();
  return trimmed.length >= 3 && trimmed.length <= 100;
}

/**
 * Валидация сообщения чата
 */
export function isValidChatMessage(message: string): boolean {
  if (!message) return false;
  const trimmed = message.trim();
  return trimmed.length > 0 && trimmed.length <= 1000;
}

// Статусы и состояния
// ===================

/**
 * Получить цвет статуса трансляции
 */
export function getStatusColor(status: BroadcastStatus): string {
  const colors: Record<BroadcastStatus, string> = {
    'idle': 'gray',
    'preparing': 'blue',
    'live': 'green',
    'paused': 'yellow',
    'ended': 'red',
    'failed': 'red'
  };

  return colors[status] || 'gray';
}

/**
 * Получить текст статуса для отображения
 */
export function getStatusText(status: BroadcastStatus): string {
  const texts: Record<BroadcastStatus, string> = {
    'idle': 'Неактивна',
    'preparing': 'Подготовка',
    'live': 'В эфире',
    'paused': 'На паузе',
    'ended': 'Завершена',
    'failed': 'Ошибка'
  };

  return texts[status] || 'Неизвестно';
}

/**
 * Проверка, можно ли присоединиться к трансляции
 */
export function canJoinBroadcast(broadcast: LiveBroadcast | null): boolean {
  if (!broadcast) return false;
  return broadcast.status === 'live' || broadcast.status === 'preparing';
}

/**
 * Проверка, можно ли управлять трансляцией
 */
export function canControlBroadcast(broadcast: LiveBroadcast | null, isAdmin: boolean): boolean {
  if (!broadcast) return false;
  return isAdmin || broadcast.status === 'preparing' || broadcast.status === 'live' || broadcast.status === 'paused';
}

// Качество потока
// ===============

/**
 * Получить разрешение для качества
 */
export function getResolutionForQuality(quality: StreamQuality): { width: number; height: number } | null {
  const resolutions: Record<StreamQuality, { width: number; height: number }> = {
    '1080p': { width: 1920, height: 1080 },
    '720p': { width: 1280, height: 720 },
    '480p': { width: 854, height: 480 },
    '360p': { width: 640, height: 360 },
    'auto': { width: 1280, height: 720 } // по умолчанию
  };

  return resolutions[quality] || null;
}

/**
 * Получить рекомендуемое качество на основе ширины экрана
 */
export function getRecommendedQuality(screenWidth: number): StreamQuality {
  if (screenWidth >= 1920) return '1080p';
  if (screenWidth >= 1280) return '720p';
  if (screenWidth >= 854) return '480p';
  return '360p';
}

/**
 * Получить битрейт для качества
 */
export function getBitrateForQuality(quality: StreamQuality): number {
  const bitrates: Record<StreamQuality, number> = {
    '1080p': 5000,
    '720p': 2500,
    '480p': 1000,
    '360p': 500,
    'auto': 2500
  };

  return bitrates[quality] || 2500;
}

// Сообщения чата
// ==============

/**
 * Форматирование времени сообщения
 */
export function formatMessageTime(timestamp: string): string {
  const date = new Date(timestamp);
  return date.toLocaleTimeString('ru-RU', {
    hour: '2-digit',
    minute: '2-digit'
  });
}

/**
 * Получить цвет роли пользователя
 */
export function getRoleColor(role: string): string {
  const colors: Record<string, string> = {
    'USER_0': 'gray',
    'USER_1': 'blue',
    'USER_2': 'green',
    'MODERATOR_ADMIN': 'orange',
    'SUPER_ADMIN': 'red',
    'VYACHESLAV': 'purple'
  };

  return colors[role] || 'gray';
}

/**
 * Получить текст роли для отображения
 */
export function getRoleText(role: string): string {
  const texts: Record<string, string> = {
    'USER_0': 'Гость',
    'USER_1': 'Пользователь',
    'USER_2': 'Пользователь+',
    'MODERATOR_ADMIN': 'Модератор',
    'SUPER_ADMIN': 'Администратор',
    'VYACHESLAV': 'Вячеслав'
  };

  return texts[role] || 'Пользователь';
}

/**
 * Проверка, является ли сообщение системным
 */
export function isSystemMessage(message: LiveChatMessage): boolean {
  return message.type === 'system' || message.userRole === 'SYSTEM';
}

// Вспомогательные функции
// =======================

/**
 * Склонение слов в зависимости от числа
 */
export function pluralize(count: number, words: [string, string, string]): string {
  const cases = [2, 0, 1, 1, 1, 2];
  const index = count % 100 > 4 && count % 100 < 20 ? 2 : cases[Math.min(count % 10, 5)];
  return words[index];
}

/**
 * Генерация случайного ID
 */
export function generateId(prefix = 'id'): string {
  return `${prefix}-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
}

/**
 * Задержка (sleep)
 */
export function delay(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms));
}

/**
 * Ограничение частоты вызовов (throttle)
 */
export function throttle<T extends (...args: any[]) => any>(
  func: T,
  limit: number
): (...args: Parameters<T>) => void {
  let inThrottle: boolean;
  return function(this: any, ...args: Parameters<T>) {
    if (!inThrottle) {
      func.apply(this, args);
      inThrottle = true;
      setTimeout(() => (inThrottle = false), limit);
    }
  };
}

/**
 * Устранение дребезга (debounce)
 */
export function debounce<T extends (...args: any[]) => any>(
  func: T,
  wait: number
): (...args: Parameters<T>) => void {
  let timeout: NodeJS.Timeout;
  return function(this: any, ...args: Parameters<T>) {
    clearTimeout(timeout);
    timeout = setTimeout(() => func.apply(this, args), wait);
  };
}

/**
 * Безопасный парсинг JSON
 */
export function safeJsonParse<T>(json: string, fallback: T): T {
  try {
    return JSON.parse(json) as T;
  } catch {
    return fallback;
  }
}

/**
 * Копирование текста в буфер обмена
 */
export async function copyToClipboard(text: string): Promise<boolean> {
  try {
    await navigator.clipboard.writeText(text);
    return true;
  } catch (err) {
    console.error('Ошибка копирования в буфер обмена:', err);

    // Fallback для старых браузеров
    try {
      const textArea = document.createElement('textarea');
      textArea.value = text;
      textArea.style.position = 'fixed';
      textArea.style.left = '-999999px';
      textArea.style.top = '-999999px';
      document.body.appendChild(textArea);
      textArea.focus();
      textArea.select();
      document.execCommand('copy');
      document.body.removeChild(textArea);
      return true;
    } catch {
      return false;
    }
  }
}

/**
 * Скачивание файла
 */
export function downloadFile(content: string, filename: string, type = 'application/json'): void {
  const blob = new Blob([content], { type });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

// Генерация демо-данных
// =====================

/**
 * Генерация демо-сообщения чата
 */
export function generateDemoChatMessage(broadcastId: string, type: LiveChatMessage['type'] = 'message'): LiveChatMessage {
  const users = [
    { id: 'user-001', username: 'Анна Петрова', role: 'USER_1', avatar: '👩' },
    { id: 'user-002', username: 'Иван Сидоров', role: 'USER_2', avatar: '👨' },
    { id: 'user-003', username: 'Мария Иванова', role: 'USER_1', avatar: '👩‍💼' },
    { id: 'mod-001', username: 'Модератор', role: 'MODERATOR_ADMIN', avatar: '🛡️' }
  ];

  const messages = [
    'Отличная трансляция!',
    'Спасибо за полезную информацию',
    'Можно повторить последний слайд?',
    'Какой у вас опыт в этой теме?',
    'Есть вопросы по материалу',
    'Очень интересно, продолжайте!',
    'Какое оборудование вы используете?',
    'Можно ссылку на материалы?'
  ];

  const questions = [
    'Как начать работать в этой области?',
    'Какие навыки самые важные?',
    'Сколько времени нужно на обучение?',
    'Есть ли карьерный рост?'
  ];

  const user = users[Math.floor(Math.random() * users.length)];
  const content = type === 'question'
    ? questions[Math.floor(Math.random() * questions.length)]
    : messages[Math.floor(Math.random() * messages.length)];

  return {
    id: generateId('chat'),
    broadcastId,
    userId: user.id,
    username: user.username,
    userRole: user.role,
    avatar: user.avatar,
    content,
    type,
    timestamp: new Date().toISOString()
  };
}

/**
 * Генерация демо-трансляции
 */
export function generateDemoBroadcast(overrides: Partial<LiveBroadcast> = {}): LiveBroadcast {
  const now = new Date();
  const oneHourAgo = new Date(now.getTime() - 3600000);
  const twoHoursLater = new Date(now.getTime() + 7200000);

  const statuses: BroadcastStatus[] = ['idle', 'preparing', 'live', 'ended'];
  const status = overrides.status || statuses[Math.floor(Math.random() * statuses.length)];

  const base: LiveBroadcast = {
    id: generateId('broadcast'),
    title: 'Демо трансляция: ' + ['Основы программирования', 'Здоровый образ жизни', 'Финансовая грамотность', 'Искусственный интеллект'][Math.floor(Math.random() * 4)],
    description: 'Это демо трансляция для т��стирования функционала live streaming.',
    streamKey: `demo_${Math.random().toString(36).substr(2, 8)}`,
    ingestUrl: 'rtmp://demo-server/live/demo',
    playbackUrl: '/demo/live-stream-720p.m3u8',
    streamType: 'video',
    status,
    viewerCount: status === 'live' ? Math.floor(Math.random() * 100) + 50 : 0,
    maxViewers: status === 'live' ? Math.floor(Math.random() * 200) + 100 : 0,
    startedAt: status === 'live' || status === 'ended' ? oneHourAgo.toISOString() : undefined,
    endedAt: status === 'ended' ? now.toISOString() : undefined,
    duration: status === 'ended' ? 3600 : undefined,
    settings: {
      isChatEnabled: true,
      isReactionsEnabled: true,
      isRecording: true,
      isPublic: true,
      allowedRoles: ['USER_1', 'USER_2', 'MODERATOR_ADMIN', 'SUPER_ADMIN', 'VYACHESLAV']
    },
    createdBy: 'demo-admin',
    createdAt: oneHourAgo.toISOString(),
    updatedAt: now.toISOString()
  };

  return { ...base, ...overrides };
}