// Mock WebSocket сервис для live streaming
// Имитирует реальное WebSocket соединение для демо
// Легко заменяется на реальный WebSocket после миграции

import {
  WebSocketEvent,
  WSEventType,
  LiveBroadcast,
  LiveChatMessage,
  EventCallback,
  UnsubscribeFn,
  MockConfig
} from './types';

export class MockWebSocketService {
  private static instance: MockWebSocketService;
  private listeners: Map<WSEventType, EventCallback[]> = new Map();
  private broadcastSessions: Map<string, MockBroadcastSession> = new Map();
  private isConnected = false;
  private reconnectTimer: NodeJS.Timeout | null = null;
  private messageQueue: WebSocketEvent[] = [];

  // Конфигурация
  private config: MockConfig = {
    enableMockWebSocket: true,
    enableMockMedia: true,
    mockLatency: 500,
    mockErrorRate: 0.05,
    generateFakeData: true,
    demoBroadcasts: [],
    demoUsers: this.generateDemoUsers()
  };

  private constructor() {
    this.initialize();
  }

  static getInstance(): MockWebSocketService {
    if (!MockWebSocketService.instance) {
      MockWebSocketService.instance = new MockWebSocketService();
    }
    return MockWebSocketService.instance;
  }

  // Публичное API (совместимо с реальным WebSocket)
  // ================================================

  connect(broadcastId: string): Promise<boolean> {
    console.log(`[MockWebSocket] Подключение к трансляции ${broadcastId}`);

    return new Promise((resolve) => {
      // Имитация задержки подключения
      setTimeout(() => {
        this.isConnected = true;

        // Создаем или получаем сессию трансляции
        let session = this.broadcastSessions.get(broadcastId);
        if (!session) {
          session = new MockBroadcastSession(broadcastId, this.config);
          this.broadcastSessions.set(broadcastId, session);
        }

        // Запускаем генерацию событий
        session.startEventGeneration((event) => {
          this.handleIncomingEvent(event);
        });

        // Обрабатываем очередь сообщений
        this.processMessageQueue();

        this.emitEvent('SYSTEM_MESSAGE', {
          message: `Подключено к трансляции ${broadcastId}`,
          broadcastId
        });

        console.log(`[MockWebSocket] Успешно подключено к ${broadcastId}`);
        resolve(true);
      }, this.config.mockLatency);
    });
  }

  disconnect(broadcastId?: string): void {
    if (broadcastId) {
      const session = this.broadcastSessions.get(broadcastId);
      if (session) {
        session.stopEventGeneration();
        this.broadcastSessions.delete(broadcastId);
      }
      console.log(`[MockWebSocket] Отключено от трансляции ${broadcastId}`);
    } else {
      // Отключение от всех трансляций
      this.broadcastSessions.forEach(session => session.stopEventGeneration());
      this.broadcastSessions.clear();
      console.log('[MockWebSocket] Отключено от всех трансляций');
    }

    this.isConnected = false;
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
  }

  send(eventType: WSEventType, payload: any, broadcastId: string): void {
    const event: WebSocketEvent = {
      type: eventType,
      payload: { ...payload, broadcastId },
      timestamp: new Date().toISOString()
    };

    // Имитация задержки сети
    setTimeout(() => {
      if (Math.random() < this.config.mockErrorRate) {
        console.warn(`[MockWebSocket] Ошибка отправки события ${eventType}`);
        return;
      }

      // Логируем отправку (в реальной системе здесь был бы реальный WebSocket)
      console.log(`[MockWebSocket] Отправлено: ${eventType}`, payload);

      // Для некоторых событий генерируем ответ
      this.handleOutgoingEvent(event);
    }, Math.random() * this.config.mockLatency);
  }

  subscribe(eventType: WSEventType, callback: EventCallback): UnsubscribeFn {
    if (!this.listeners.has(eventType)) {
      this.listeners.set(eventType, []);
    }

    const callbacks = this.listeners.get(eventType)!;
    callbacks.push(callback);

    console.log(`[MockWebSocket] Подписка на ${eventType}, всего подписчиков: ${callbacks.length}`);

    return () => {
      const index = callbacks.indexOf(callback);
      if (index > -1) {
        callbacks.splice(index, 1);
        console.log(`[MockWebSocket] Отписано от ${eventType}`);
      }
    };
  }

  getConnectionStatus(): boolean {
    return this.isConnected;
  }

  // Внутренние методы
  // =================

  private initialize(): void {
    console.log('[MockWebSocket] Инициализация мок-сервиса');

    // Загружаем демо-трансляции из sessionStorage или создаем по умолчанию (только в development)
    this.loadDemoBroadcasts();

    // Имитация периодического пинга
    setInterval(() => {
      if (this.isConnected) {
        this.emitEvent('SYSTEM_MESSAGE', {
          type: 'PING',
          timestamp: Date.now()
        });
      }
    }, 30000);
  }

  private loadDemoBroadcasts(): void {
    try {
      const saved = process.env.NODE_ENV === 'development'
        ? sessionStorage.getItem('demoLiveBroadcasts')
        : null;
      if (saved) {
        this.config.demoBroadcasts = JSON.parse(saved);
        console.log('[MockWebSocket] Загружены демо-трансляции:', this.config.demoBroadcasts.length);
      } else {
        this.config.demoBroadcasts = this.generateDemoBroadcasts();
        this.saveDemoBroadcasts();
      }
    } catch (error) {
      console.error('[MockWebSocket] Ошибка загрузки демо-трансляций:', error);
      this.config.demoBroadcasts = this.generateDemoBroadcasts();
    }
  }

  private saveDemoBroadcasts(): void {
    if (process.env.NODE_ENV === 'development') {
      sessionStorage.setItem('demoLiveBroadcasts', JSON.stringify(this.config.demoBroadcasts));
    }
  }

  private generateDemoBroadcasts(): LiveBroadcast[] {
    const now = new Date();
    const oneHourAgo = new Date(now.getTime() - 3600000);
    const twoHoursLater = new Date(now.getTime() + 7200000);

    return [
      {
        id: 'demo-live-001',
        eventId: 'event-001',
        title: 'Прямой эфир: Основы здорового питания',
        description: 'Обсуждение принципов правильного питания и здорового образа жизни',
        streamKey: 'live_demo_001_key',
        ingestUrl: 'rtmp://demo-server/live/demo-001',
        playbackUrl: '/demo/live-stream-720p.m3u8',
        streamType: 'video',
        status: 'live',
        viewerCount: 87,
        maxViewers: 120,
        startedAt: oneHourAgo.toISOString(),
        duration: 3600,
        stats: {
          bitrate: 2500,
          quality: '720p',
          latency: 3.2,
          bufferHealth: 95,
          droppedFrames: 12
        },
        settings: {
          isChatEnabled: true,
          isReactionsEnabled: true,
          isRecording: true,
          isPublic: true,
          allowedRoles: ['USER_1', 'USER_2', 'MODERATOR_ADMIN', 'SUPER_ADMIN', 'VYACHESLAV']
        },
        createdBy: 'admin-001',
        createdAt: oneHourAgo.toISOString(),
        updatedAt: now.toISOString()
      },
      {
        id: 'demo-live-002',
        title: 'Аудио трансляция: Медитация и релаксация',
        description: 'Сессия guided meditation для снятия стресса',
        streamKey: 'live_demo_002_key',
        ingestUrl: 'rtmp://demo-server/live/demo-002',
        playbackUrl: '/demo/audio-stream.m3u8',
        streamType: 'audio',
        status: 'idle',
        viewerCount: 0,
        maxViewers: 0,
        startedAt: twoHoursLater.toISOString(),
        settings: {
          isChatEnabled: true,
          isReactionsEnabled: false,
          isRecording: true,
          isPublic: true
        },
        createdBy: 'moderator-001',
        createdAt: now.toISOString()
      }
    ];
  }

  private generateDemoUsers() {
    return [
      { id: 'user-001', username: 'Анна Петрова', role: 'USER_1', avatar: '👩' },
      { id: 'user-002', username: 'Иван Сидоров', role: 'USER_2', avatar: '👨' },
      { id: 'user-003', username: 'Мария Иванова', role: 'USER_1', avatar: '👩‍💼' },
      { id: 'user-004', username: 'Алексей Козлов', role: 'USER_2', avatar: '👨‍💻' },
      { id: 'user-005', username: 'Елена Смирнова', role: 'USER_1', avatar: '👩‍🏫' },
      { id: 'mod-001', username: 'Модератор', role: 'MODERATOR_ADMIN', avatar: '🛡️' },
      { id: 'admin-001', username: 'Администратор', role: 'SUPER_ADMIN', avatar: '👑' }
    ];
  }

  private handleIncomingEvent(event: WebSocketEvent): void {
    // Добавляем в очередь для обработки
    this.messageQueue.push(event);
    this.processMessageQueue();
  }

  private processMessageQueue(): void {
    if (!this.isConnected || this.messageQueue.length === 0) {
      return;
    }

    // Обрабатываем сообщения с имитацией задержки
    const processNext = () => {
      if (this.messageQueue.length === 0) return;

      const event = this.messageQueue.shift()!;
      this.emitEvent(event.type as WSEventType, event.payload);

      // Рекурсивно обрабатываем следующее сообщение
      setTimeout(processNext, Math.random() * 100);
    };

    processNext();
  }

  private handleOutgoingEvent(event: WebSocketEvent): void {
    // Обработка исходящих событий (например, сообщения чата)
    switch (event.type) {
      case 'CHAT_MESSAGE':
        // Имитируем получение сообщения от других пользователей
        setTimeout(() => {
          const responses = [
            'Отличный вопрос!',
            'Согласен с вами!',
            'Интересная мысль',
            'Спасибо за комментарий!'
          ];

          if (Math.random() > 0.7) {
            const randomUser = this.config.demoUsers[
              Math.floor(Math.random() * this.config.demoUsers.length)
            ];

            this.emitEvent('CHAT_MESSAGE', {
              id: `msg-${Date.now()}`,
              broadcastId: event.payload.broadcastId,
              userId: randomUser.id,
              username: randomUser.username,
              userRole: randomUser.role,
              avatar: randomUser.avatar,
              content: responses[Math.floor(Math.random() * responses.length)],
              type: 'message',
              timestamp: new Date().toISOString()
            });
          }
        }, 1000 + Math.random() * 2000);
        break;

      case 'REACTION_ADDED':
        // Распространяем реакцию другим зрителям
        this.emitEvent('REACTION_ADDED', event.payload);
        break;
    }
  }

  private emitEvent(eventType: WSEventType, payload: any): void {
    const callbacks = this.listeners.get(eventType);
    if (callbacks) {
      callbacks.forEach(callback => {
        try {
          callback(payload);
        } catch (error) {
          console.error(`[MockWebSocket] Ошибка в callback для ${eventType}:`, error);
        }
      });
    }
  }

  // Публичные утилиты
  getDemoBroadcasts(): LiveBroadcast[] {
    return [...this.config.demoBroadcasts];
  }

  updateDemoBroadcast(broadcastId: string, updates: Partial<LiveBroadcast>): void {
    const index = this.config.demoBroadcasts.findIndex(b => b.id === broadcastId);
    if (index !== -1) {
      this.config.demoBroadcasts[index] = {
        ...this.config.demoBroadcasts[index],
        ...updates,
        updatedAt: new Date().toISOString()
      };
      this.saveDemoBroadcasts();

      // Уведомляем об изменении
      this.emitEvent('BROADCAST_STATUS_CHANGED', this.config.demoBroadcasts[index]);
    }
  }

  addDemoBroadcast(broadcast: LiveBroadcast): void {
    this.config.demoBroadcasts.push(broadcast);
    this.saveDemoBroadcasts();
  }
}

// Вспомогательный класс для управления отдельной трансляцией
class MockBroadcastSession {
  private eventInterval: NodeJS.Timeout | null = null;
  private isGenerating = false;

  constructor(
    private broadcastId: string,
    private config: MockConfig
  ) {}

  startEventGeneration(onEvent: (event: WebSocketEvent) => void): void {
    if (this.isGenerating) return;
    this.isGenerating = true;

    console.log(`[MockBroadcastSession] Запуск генерации событий для ${this.broadcastId}`);

    // Генерация обновлений количества зрителей
    this.eventInterval = setInterval(() => {
      if (Math.random() > 0.3) {
        const viewerChange = Math.floor(Math.random() * 5) - 2; // -2 to +2
        onEvent({
          type: 'VIEWER_COUNT_UPDATED',
          payload: {
            broadcastId: this.broadcastId,
            viewerChange,
            timestamp: new Date().toISOString()
          },
          timestamp: new Date().toISOString()
        });
      }

      // Генерация случайных реакций
      if (Math.random() > 0.8) {
        const emojis = ['❤️', '👍', '👏', '🔥', '🎉'];
        const emoji = emojis[Math.floor(Math.random() * emojis.length)];
        onEvent({
          type: 'REACTION_ADDED',
          payload: {
            broadcastId: this.broadcastId,
            emoji,
            count: Math.floor(Math.random() * 5) + 1,
            timestamp: new Date().toISOString()
          },
          timestamp: new Date().toISOString()
        });
      }

      // Генерация статистики потока
      if (Math.random() > 0.9) {
        onEvent({
          type: 'STREAM_STATS',
          payload: {
            broadcastId: this.broadcastId,
            bitrate: 2000 + Math.random() * 1000,
            latency: 2 + Math.random() * 3,
            bufferHealth: 80 + Math.random() * 20,
            timestamp: new Date().toISOString()
          },
          timestamp: new Date().toISOString()
        });
      }
    }, 3000); // Каждые 3 секунды
  }

  stopEventGeneration(): void {
    if (this.eventInterval) {
      clearInterval(this.eventInterval);
      this.eventInterval = null;
    }
    this.isGenerating = false;
    console.log(`[MockBroadcastSession] Остановка генерации событий для ${this.broadcastId}`);
  }
}

// Экспорт синглтона для использования
export const mockWebSocket = MockWebSocketService.getInstance();

// Экспорт типа для TypeScript
export type { MockWebSocketService as LiveWebSocketService };