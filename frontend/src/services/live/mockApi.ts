// Mock API сервис для live streaming
// Имитирует REST API бэкенда для управления трансляциями
// Легко заменяется на реальный API после миграции

import {
  LiveBroadcast,
  StartBroadcastRequest,
  StartBroadcastResponse,
  UpdateBroadcastRequest,
  LiveChatMessage,
  ViewerStats,
  BroadcastStatus
} from './types';
import { mockWebSocket } from './mockWebSocket';

export class MockLiveApi {
  private static instance: MockLiveApi;
  private broadcasts: Map<string, LiveBroadcast> = new Map();
  private chatMessages: Map<string, LiveChatMessage[]> = new Map();
  private viewerStats: Map<string, ViewerStats[]> = new Map();

  private constructor() {
    this.initializeDemoData();
  }

  static getInstance(): MockLiveApi {
    if (!MockLiveApi.instance) {
      MockLiveApi.instance = new MockLiveApi();
    }
    return MockLiveApi.instance;
  }

  // Публичное API (совместимо с реальным бэкенд-API)
  // ================================================

  // Управление трансляциями
  // -----------------------

  async startBroadcast(request: StartBroadcastRequest): Promise<StartBroadcastResponse> {
    console.log('[MockLiveApi] Запрос на начало трансляции:', request);

    return new Promise((resolve) => {
      setTimeout(() => {
        const broadcastId = `broadcast-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
        const now = new Date().toISOString();

        const broadcast: LiveBroadcast = {
          id: broadcastId,
          title: request.title,
          description: request.description,
          streamKey: `live_${broadcastId}_key`,
          ingestUrl: `rtmp://demo-server/live/${broadcastId}`,
          playbackUrl: `/demo/live-${request.streamType === 'audio' ? 'audio' : 'video'}.m3u8`,
          streamType: request.streamType,
          status: 'preparing',
          viewerCount: 0,
          maxViewers: 0,
          startedAt: now,
          settings: {
            isChatEnabled: request.settings?.isChatEnabled ?? true,
            isReactionsEnabled: request.settings?.isReactionsEnabled ?? true,
            isRecording: request.settings?.isRecording ?? true,
            isPublic: request.settings?.isPublic ?? true,
            allowedRoles: ['USER_1', 'USER_2', 'MODERATOR_ADMIN', 'SUPER_ADMIN', 'VYACHESLAV']
          },
          createdBy: this.getCurrentUserId(),
          createdAt: now
        };

        this.broadcasts.set(broadcastId, broadcast);
        this.saveToStorage();

        // Обновляем демо-трансляции в WebSocket сервисе
        mockWebSocket.addDemoBroadcast(broadcast);

        console.log('[MockLiveApi] Трансляция создана:', broadcastId);

        resolve({
          broadcast,
          streamKey: broadcast.streamKey,
          ingestUrl: broadcast.ingestUrl,
          playbackUrl: broadcast.playbackUrl
        });
      }, 800); // Имитация задержки API
    });
  }

  async getBroadcast(broadcastId: string): Promise<LiveBroadcast> {
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        const broadcast = this.broadcasts.get(broadcastId);
        if (broadcast) {
          resolve(broadcast);
        } else {
          reject(new Error(`Трансляция ${broadcastId} не найдена`));
        }
      }, 300);
    });
  }

  async updateBroadcast(
    broadcastId: string,
    updates: UpdateBroadcastRequest
  ): Promise<LiveBroadcast> {
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        const broadcast = this.broadcasts.get(broadcastId);
        if (!broadcast) {
          reject(new Error(`Трансляция ${broadcastId} не найдена`));
          return;
        }

        const updatedBroadcast: any = {
          ...broadcast,
          ...updates,
          updatedAt: new Date().toISOString()
        };

        // Специальная логика для изменения статуса
        if (updates.status) {
          if (updates.status === 'live' && !broadcast.startedAt) {
            updatedBroadcast.startedAt = new Date().toISOString();
          } else if (updates.status === 'ended' && !broadcast.endedAt) {
            updatedBroadcast.endedAt = new Date().toISOString();
            updatedBroadcast.duration = Math.floor(
              (new Date(updatedBroadcast.endedAt).getTime() -
               new Date(updatedBroadcast.startedAt!).getTime()) / 1000
            );
          }
        }

        
        this.saveToStorage();

        // Обновляем в WebSocket сервисе
        mockWebSocket.updateDemoBroadcast(broadcastId, updatedBroadcast);

        console.log('[MockLiveApi] Трансляция обновлена:', broadcastId, updates);

        resolve(updatedBroadcast);
      }, 500);
    });
  }

  async stopBroadcast(broadcastId: string): Promise<LiveBroadcast> {
    return this.updateBroadcast(broadcastId, { status: 'ended' });
  }

  async deleteBroadcast(broadcastId: string): Promise<void> {
    return new Promise((resolve) => {
      setTimeout(() => {
        this.broadcasts.delete(broadcastId);
        this.chatMessages.delete(broadcastId);
        this.viewerStats.delete(broadcastId);
        this.saveToStorage();

        // Удаляем из WebSocket сервиса
        const demoBroadcasts = mockWebSocket.getDemoBroadcasts();
        const updated = demoBroadcasts.filter(b => b.id !== broadcastId);
        if (process.env.NODE_ENV === 'development') {
            sessionStorage.setItem('demoLiveBroadcasts', JSON.stringify(updated));
        }

        console.log('[MockLiveApi] Трансляция удалена:', broadcastId);
        resolve();
      }, 400);
    });
  }

  async getActiveBroadcasts(): Promise<LiveBroadcast[]> {
    return new Promise((resolve) => {
      setTimeout(() => {
        const active = Array.from(this.broadcasts.values()).filter(
          b => b.status === 'live' || b.status === 'preparing'
        );
        resolve(active);
      }, 200);
    });
  }

  async getAllBroadcasts(): Promise<LiveBroadcast[]> {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve(Array.from(this.broadcasts.values()));
      }, 300);
    });
  }

  // Чат
  // ---

  async getChatMessages(broadcastId: string, limit = 50): Promise<LiveChatMessage[]> {
    return new Promise((resolve) => {
      setTimeout(() => {
        const messages = this.chatMessages.get(broadcastId) || [];
        resolve(messages.slice(-limit));
      }, 200);
    });
  }

  async sendChatMessage(
    broadcastId: string,
    content: string,
    type: LiveChatMessage['type'] = 'message'
  ): Promise<LiveChatMessage> {
    return new Promise((resolve) => {
      setTimeout(() => {
        const message: LiveChatMessage = {
          id: `chat-${Date.now()}-${Math.random().toString(36).substr(2, 6)}`,
          broadcastId,
          userId: this.getCurrentUserId(),
          username: this.getCurrentUsername(),
          userRole: this.getCurrentUserRole(),
          avatar: this.getCurrentUserAvatar(),
          content,
          type,
          timestamp: new Date().toISOString(),
          isOwn: true
        };

        const messages = this.chatMessages.get(broadcastId) || [];
        messages.push(message);
        this.chatMessages.set(broadcastId, messages);
        this.saveChatToStorage(broadcastId, messages);

        console.log('[MockLiveApi] Сообщение чата отправлено:', content.substring(0, 50));

        resolve(message);
      }, 300);
    });
  }

  async sendReaction(broadcastId: string, emoji: string): Promise<void> {
    return new Promise((resolve) => {
      setTimeout(() => {
        console.log('[MockLiveApi] Реакция отправлена:', emoji);
        resolve();
      }, 200);
    });
  }

  // Статистика
  // ----------

  async getViewerStats(broadcastId: string): Promise<ViewerStats[]> {
    return new Promise((resolve) => {
      setTimeout(() => {
        const stats = this.viewerStats.get(broadcastId) || this.generateMockStats(broadcastId);
        resolve(stats);
      }, 400);
    });
  }

  async getBroadcastStats(broadcastId: string): Promise<any> {
    return new Promise((resolve) => {
      setTimeout(() => {
        const broadcast = this.broadcasts.get(broadcastId);
        if (!broadcast) {
          resolve(null);
          return;
        }

        const stats = {
          broadcastId,
          totalViewers: broadcast.maxViewers,
          averageViewers: Math.floor(broadcast.viewerCount * 0.7),
          peakViewers: broadcast.maxViewers,
          averageWatchTime: broadcast.duration ? Math.floor(broadcast.duration * 0.6) : 0,
          totalMessages: (this.chatMessages.get(broadcastId) || []).length,
          totalReactions: Math.floor(Math.random() * 100) + 50,
          engagementRate: Math.random() * 0.3 + 0.4 // 40-70%
        };

        resolve(stats);
      }, 500);
    });
  }

  // Интеграция с существующей системой
  // ----------------------------------

  async linkToEvent(broadcastId: string, eventId: string): Promise<LiveBroadcast> {
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        const broadcast = this.broadcasts.get(broadcastId);
        if (!broadcast) {
          reject(new Error(`Трансляция ${broadcastId} не найдена`));
          return;
        }

        const updatedBroadcast: any = {
          ...broadcast,
          eventId,
          updatedAt: new Date().toISOString()
        };

        
        this.saveToStorage();

        console.log('[MockLiveApi] Трансляция привязана к событию:', broadcastId, eventId);
        resolve(updatedBroadcast);
      }, 400);
    });
  }

  async archiveBroadcast(broadcastId: string): Promise<{ mediaFileId: string }> {
    return new Promise((resolve) => {
      setTimeout(() => {
        const mediaFileId = `archive-${broadcastId}-${Date.now()}`;
        console.log('[MockLiveApi] Трансляция архивирована:', broadcastId, mediaFileId);
        resolve({ mediaFileId });
      }, 1000);
    });
  }

  // Вспомогательные методы
  // ----------------------

  private initializeDemoData(): void {
    this.loadFromStorage();

    // Если нет данных, создаем демо-трансляции
    if (this.broadcasts.size === 0) {
      const demoBroadcasts = mockWebSocket.getDemoBroadcasts();
      demoBroadcasts.forEach(broadcast => {
        this.broadcasts.set(broadcast.id, broadcast);
      });
      this.saveToStorage();
    }

    console.log('[MockLiveApi] Инициализировано трансляций:', this.broadcasts.size);
  }

  private loadFromStorage(): void {
    try {
      const saved = process.env.NODE_ENV === 'development'
          ? sessionStorage.getItem('mockLiveBroadcasts')
          : null;
      if (saved) {
        const broadcasts: LiveBroadcast[] = JSON.parse(saved);
        broadcasts.forEach(broadcast => {
          this.broadcasts.set(broadcast.id, broadcast);
        });
      }
    } catch (error) {
      console.error('[MockLiveApi] Ошибка загрузки из sessionStorage:', error);
    }
  }

  private saveToStorage(): void {
    const broadcasts = Array.from(this.broadcasts.values());
    if (process.env.NODE_ENV === 'development') {
        sessionStorage.setItem('mockLiveBroadcasts', JSON.stringify(broadcasts));
    }
  }

  private saveChatToStorage(broadcastId: string, messages: LiveChatMessage[]): void {
    if (process.env.NODE_ENV === 'development') {
        sessionStorage.setItem(`mockLiveChat_${broadcastId}`, JSON.stringify(messages));
    }
  }

  private generateMockStats(broadcastId: string): ViewerStats[] {
    const stats: ViewerStats[] = [];
    const now = new Date();
    const broadcast = this.broadcasts.get(broadcastId);

    if (!broadcast || !broadcast.startedAt) {
      return stats;
    }

    const startTime = new Date(broadcast.startedAt);
    const duration = broadcast.duration || 3600; // 1 час по умолчанию

    // Генерируем статистику каждые 5 минут
    for (let i = 0; i < duration; i += 300) {
      const timestamp = new Date(startTime.getTime() + i * 1000);
      if (timestamp > now) break;

      const viewerCount = Math.floor(
        broadcast.viewerCount * (0.7 + Math.random() * 0.6) // 70-130% от текущего
      );

      stats.push({
        broadcastId,
        timestamp: timestamp.toISOString(),
        viewerCount,
        countries: { 'RU': Math.floor(viewerCount * 0.8), 'UA': Math.floor(viewerCount * 0.2) },
        devices: { 'desktop': Math.floor(viewerCount * 0.6), 'mobile': Math.floor(viewerCount * 0.4) },
        peakViewers: Math.max(...stats.map(s => s.viewerCount), viewerCount),
        averageWatchTime: Math.floor(Math.random() * 30) + 10 // 10-40 минут
      });
    }

    this.viewerStats.set(broadcastId, stats);
    return stats;
  }

  private getCurrentUserId(): string {
    // В реальной системе здесь был бы JWT или сессия
    const demoRole = process.env.NODE_ENV === 'development'
        ? sessionStorage.getItem('demoRole') || 'USER_1'
        : 'USER_1';
    return `${demoRole.toLowerCase()}-${Date.now()}`;
  }

  private getCurrentUsername(): string {
    const demoRole = process.env.NODE_ENV === 'development'
        ? sessionStorage.getItem('demoRole') || 'USER_1'
        : 'USER_1';
    const names: Record<string, string> = {
      'USER_0': 'Гость',
      'USER_1': 'Анна Петрова',
      'USER_2': 'Иван Сидоров',
      'MODERATOR_ADMIN': 'Модератор',
      'SUPER_ADMIN': 'Администратор',
      'VYACHESLAV': 'Вячеслав'
    };
    return names[demoRole] || 'Пользователь';
  }

  private getCurrentUserRole(): string {
    return process.env.NODE_ENV === 'development'
      ? sessionStorage.getItem('demoRole') || 'USER_1'
      : 'USER_1';
  }

  private getCurrentUserAvatar(): string {
    const role = this.getCurrentUserRole();
    const avatars: Record<string, string> = {
      'USER_0': '👤',
      'USER_1': '👩',
      'USER_2': '👨',
      'MODERATOR_ADMIN': '🛡️',
      'SUPER_ADMIN': '👑',
      'VYACHESLAV': '🌟'
    };
    return avatars[role] || '👤';
  }

  // Утилиты для разработки
  // ----------------------

  resetDemoData(): void {
    this.broadcasts.clear();
    this.chatMessages.clear();
    this.viewerStats.clear();
    if (process.env.NODE_ENV === 'development') {
      sessionStorage.removeItem('mockLiveBroadcasts');
    }
    this.initializeDemoData();
    console.log('[MockLiveApi] Демо-данные сброшены');
  }

  simulateViewerJoin(broadcastId: string, count = 1): void {
    const broadcast = this.broadcasts.get(broadcastId);
    if (broadcast) {
      const newViewerCount = broadcast.viewerCount + count;
      const newMaxViewers = Math.max(broadcast.maxViewers, newViewerCount);

      this.updateBroadcast(broadcastId, {
        viewerCount: newViewerCount as any,
        maxViewers: newMaxViewers
      }).catch(console.error);
    }
  }

  simulateViewerLeave(broadcastId: string, count = 1): void {
    const broadcast = this.broadcasts.get(broadcastId);
    if (broadcast) {
      const newViewerCount = Math.max(0, broadcast.viewerCount - count);
      this.updateBroadcast(broadcastId, {
        viewerCount: newViewerCount as any,
      }).catch(console.error);
    }
  }
}

// Экспорт синглтона для использования
export const mockLiveApi = MockLiveApi.getInstance();

// Экспорт типа для TypeScript
export type { MockLiveApi as LiveApiService };