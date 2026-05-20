// Mock медиа сервис для live streaming
// Имитирует HLS/DASH потоки и управление качеством
// Использует статичные демо-файлы для работы без реального медиасервера

import { StreamQuality } from './types';

export interface MediaStream {
  url: string;
  quality: StreamQuality;
  bitrate: number; // Кбит/с
  resolution?: { width: number; height: number };
  codec: string;
}

export interface PlayerStats {
  bitrate: number;
  bufferLength: number;
  droppedFrames: number;
  latency: number;
  networkActivity: boolean;
}

export class MockMediaService {
  private static instance: MockMediaService;
  private activeStreams: Map<string, MediaStream> = new Map();
  private playerStats: Map<string, PlayerStats> = new Map();
  private updateIntervals: Map<string, NodeJS.Timeout> = new Map();

  // Демо медиафайлы (должны существовать в public/demo/)
  private readonly DEMO_MEDIA = {
    video: {
      '1080p': { url: '/demo/live-stream-1080p.m3u8', bitrate: 5000, width: 1920, height: 1080 },
      '720p': { url: '/demo/live-stream-720p.m3u8', bitrate: 2500, width: 1280, height: 720 },
      '480p': { url: '/demo/live-stream-480p.m3u8', bitrate: 1000, width: 854, height: 480 },
      '360p': { url: '/demo/live-stream-360p.m3u8', bitrate: 500, width: 640, height: 360 }
    },
    audio: {
      'auto': { url: '/demo/audio-stream.m3u8', bitrate: 128 }
    }
  };

  private constructor() {
    this.initializeDemoFiles();
  }

  static getInstance(): MockMediaService {
    if (!MockMediaService.instance) {
      MockMediaService.instance = new MockMediaService();
    }
    return MockMediaService.instance;
  }

  // Публичное API
  // =============

  /**
   * Получить поток для трансляции
   */
  getStream(broadcastId: string, type: 'video' | 'audio' = 'video', quality: StreamQuality = 'auto'): MediaStream {
    const streamKey = `${broadcastId}-${type}-${quality}`;

    // Если поток уже активен, возвращаем его
    if (this.activeStreams.has(streamKey)) {
      return this.activeStreams.get(streamKey)!;
    }

    // Создаем новый поток
    let stream: MediaStream;

    if (type === 'audio') {
      stream = {
        url: this.DEMO_MEDIA.audio.auto.url,
        quality: 'auto',
        bitrate: 128,
        codec: 'aac'
      };
    } else {
      // Для видео определяем качество
      const targetQuality = quality === 'auto' ? '720p' : quality;
      const demo = this.DEMO_MEDIA.video[targetQuality as keyof typeof this.DEMO_MEDIA.video];

      stream = {
        url: demo.url,
        quality: targetQuality as StreamQuality,
        bitrate: demo.bitrate,
        resolution: demo.width ? { width: demo.width, height: demo.height! } : undefined,
        codec: 'h264'
      };
    }

    this.activeStreams.set(streamKey, stream);

    // Запускаем сбор статистики
    this.startStatsCollection(broadcastId, stream);

    console.log(`[MockMedia] Создан поток: ${streamKey}`, stream);
    return stream;
  }

  /**
   * Переключить качество потока
   */
  switchQuality(broadcastId: string, newQuality: StreamQuality): MediaStream {
    const currentStream = Array.from(this.activeStreams.entries())
      .find(([key]) => key.startsWith(broadcastId));

    if (!currentStream) {
      throw new Error(`Поток для трансляции ${broadcastId} не найден`);
    }

    const [currentKey, currentStreamData] = currentStream;
    const type = currentStreamData.resolution ? 'video' : 'audio';

    // Останавливаем старую статистику
    this.stopStatsCollection(broadcastId);

    // Удаляем старый поток
    this.activeStreams.delete(currentKey);

    // Создаем новый поток с новым качеством
    return this.getStream(broadcastId, type, newQuality);
  }

  /**
   * Получить статистику плеера
   */
  getPlayerStats(broadcastId: string): PlayerStats {
    const defaultStats: PlayerStats = {
      bitrate: 0,
      bufferLength: 0,
      droppedFrames: 0,
      latency: 0,
      networkActivity: false
    };

    return this.playerStats.get(broadcastId) || defaultStats;
  }

  /**
   * Имитировать проблемы с сетью
   */
  simulateNetworkIssues(broadcastId: string, severity: 'low' | 'medium' | 'high' = 'medium'): void {
    const stats = this.playerStats.get(broadcastId);
    if (!stats) return;

    const issues = {
      low: { bitrateMultiplier: 0.7, bufferReduction: 0.5, latencyIncrease: 1.5 },
      medium: { bitrateMultiplier: 0.4, bufferReduction: 0.2, latencyIncrease: 3.0 },
      high: { bitrateMultiplier: 0.1, bufferReduction: 0.05, latencyIncrease: 5.0 }
    };

    const issue = issues[severity];

    this.playerStats.set(broadcastId, {
      ...stats,
      bitrate: stats.bitrate * issue.bitrateMultiplier,
      bufferLength: stats.bufferLength * issue.bufferReduction,
      latency: stats.latency * issue.latencyIncrease,
      networkActivity: true
    });

    console.log(`[MockMedia] Имитация проблем с сетью: ${severity} для ${broadcastId}`);
  }

  /**
   * Восстановить нормальную работу сети
   */
  restoreNetwork(broadcastId: string): void {
    const stats = this.playerStats.get(broadcastId);
    if (!stats) return;

    this.playerStats.set(broadcastId, {
      ...stats,
      networkActivity: false
    });

    console.log(`[MockMedia] Сеть восстановлена для ${broadcastId}`);
  }

  /**
   * Остановить все потоки для трансляции
   */
  stopAllStreams(broadcastId: string): void {
    // Удаляем все потоки для этой трансляции
    Array.from(this.activeStreams.keys())
      .filter(key => key.startsWith(broadcastId))
      .forEach(key => {
        this.activeStreams.delete(key);
      });

    // Останавливаем сбор статистики
    this.stopStatsCollection(broadcastId);

    console.log(`[MockMedia] Все потоки остановлены для ${broadcastId}`);
  }

  /**
   * Получить доступные качества для типа потока
   */
  getAvailableQualities(type: 'video' | 'audio'): StreamQuality[] {
    if (type === 'audio') {
      return ['auto'];
    }

    return Object.keys(this.DEMO_MEDIA.video) as StreamQuality[];
  }

  // Внутренние методы
  // =================

  private initializeDemoFiles(): void {
    console.log('[MockMedia] Инициализация демо-медиафайлов');

    // Проверяем существование демо-файлов
    this.checkDemoFiles();
  }

  private checkDemoFiles(): void {
    // В реальной системе здесь была бы проверка существования файлов
    // Для демо просто логируем
    console.log('[MockMedia] Демо-файлы должны быть размещены в public/demo/');
    console.log('[MockMedia] Ожидаемые файлы:');
    Object.entries(this.DEMO_MEDIA.video).forEach(([quality, info]) => {
      console.log(`  - ${info.url} (${quality})`);
    });
    console.log(`  - ${this.DEMO_MEDIA.audio.auto.url} (audio)`);
  }

  private startStatsCollection(broadcastId: string, stream: MediaStream): void {
    // Останавливаем предыдущий интервал, если есть
    this.stopStatsCollection(broadcastId);

    // Начальная статистика
    const initialStats: PlayerStats = {
      bitrate: stream.bitrate,
      bufferLength: 5 + Math.random() * 10, // 5-15 секунд
      droppedFrames: 0,
      latency: 2 + Math.random() * 3, // 2-5 секунд
      networkActivity: false
    };

    this.playerStats.set(broadcastId, initialStats);

    // Интервал обновления статистики
    const interval = setInterval(() => {
      const currentStats = this.playerStats.get(broadcastId);
      if (!currentStats) return;

      // Генерация реалистичных изменений статистики
      const newStats: PlayerStats = {
        bitrate: this.calculateBitrate(currentStats.bitrate, stream.bitrate),
        bufferLength: this.calculateBuffer(currentStats.bufferLength),
        droppedFrames: currentStats.droppedFrames + (Math.random() > 0.95 ? 1 : 0),
        latency: this.calculateLatency(currentStats.latency),
        networkActivity: currentStats.networkActivity
      };

      this.playerStats.set(broadcastId, newStats);

      // Периодически имитируем небольшие проблемы с сетью
      if (Math.random() > 0.98 && !currentStats.networkActivity) {
        this.simulateNetworkIssues(broadcastId, 'low');
        setTimeout(() => this.restoreNetwork(broadcastId), 5000);
      }

    }, 1000); // Обновление каждую секунду

    this.updateIntervals.set(broadcastId, interval);
  }

  private stopStatsCollection(broadcastId: string): void {
    const interval = this.updateIntervals.get(broadcastId);
    if (interval) {
      clearInterval(interval);
      this.updateIntervals.delete(broadcastId);
    }

    this.playerStats.delete(broadcastId);
  }

  private calculateBitrate(current: number, target: number): number {
    // Плавные изменения битрейта вокруг целевого значения
    const variation = target * 0.1; // ±10%
    const change = (Math.random() * 2 - 1) * variation;
    const newBitrate = current + change;

    // Ограничиваем разумными пределами
    return Math.max(target * 0.5, Math.min(target * 1.5, newBitrate));
  }

  private calculateBuffer(current: number): number {
    // Буфер обычно колеблется
    const change = Math.random() * 2 - 1; // -1 to +1
    const newBuffer = current + change;

    // Ограничиваем разумными пределами
    return Math.max(0, Math.min(30, newBuffer));
  }

  private calculateLatency(current: number): number {
    // Задержка немного меняется
    const change = Math.random() * 0.5 - 0.25; // -0.25 to +0.25
    const newLatency = current + change;

    // Ограничиваем разумными пределами
    return Math.max(1, Math.min(10, newLatency));
  }

  // Утилиты для разработки
  // ======================

  /**
   * Создать демо HLS плейлист (для тестирования)
   */
  createDemoPlaylist(quality: StreamQuality = '720p'): string {
    const demo = this.DEMO_MEDIA.video[quality as keyof typeof this.DEMO_MEDIA.video];
    if (!demo) {
      throw new Error(`Неизвестное качество: ${quality}`);
    }

    // Простой HLS плейлист
    return `#EXTM3U
#EXT-X-VERSION:3
#EXT-X-TARGETDURATION:6
#EXT-X-MEDIA-SEQUENCE:1
#EXT-X-PLAYLIST-TYPE:EVENT
#EXTINF:6.0,
segment1.ts
#EXTINF:6.0,
segment2.ts
#EXTINF:6.0,
segment3.ts
#EXTINF:6.0,
segment4.ts
#EXTINF:6.0,
segment5.ts
#EXT-X-ENDLIST`;
  }

  /**
   * Сбросить все состояния
   */
  reset(): void {
    this.activeStreams.clear();
    this.playerStats.clear();

    this.updateIntervals.forEach(interval => clearInterval(interval));
    this.updateIntervals.clear();

    console.log('[MockMedia] Все состояния сброшены');
  }

  /**
   * Получить информацию о всех активных потоках
   */
  getActiveStreamsInfo(): Array<{ key: string; stream: MediaStream }> {
    return Array.from(this.activeStreams.entries()).map(([key, stream]) => ({ key, stream }));
  }
}

// Экспорт синглтона
export const mockMediaService = MockMediaService.getInstance();

// Экспорт типа для TypeScript
export type { MockMediaService as MediaService };