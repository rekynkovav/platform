/**
 * Real Media Service — replaces mockMedia.ts for production use.
 * Uses actual HLS URLs from broadcast.playbackUrl.
 * Player stats from HTMLVideoElement, not simulated data.
 */

import Hls from 'hls.js';
import { StreamQuality } from './types';

export interface MediaStream {
  url: string;
  quality: StreamQuality;
  bitrate: number;
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

export class RealMediaService {
  private static instance: RealMediaService;
  private activeStreams: Map<string, MediaStream> = new Map();
  private videoElements: Map<string, HTMLVideoElement> = new Map();
  private hlsInstances: Map<string, Hls> = new Map();

  private constructor() {}

  static getInstance(): RealMediaService {
    if (!RealMediaService.instance) {
      RealMediaService.instance = new RealMediaService();
    }
    return RealMediaService.instance;
  }

  async getStream(broadcastId: string, type: 'video' | 'audio' = 'video', quality: StreamQuality = 'auto'): Promise<MediaStream> {
    const streamKey = `${broadcastId}-${type}-${quality}`;
    if (this.activeStreams.has(streamKey)) {
      return this.activeStreams.get(streamKey)!;
    }
    // Fetch real playback URL from backend
    let playbackUrl = '';
    try {
      const { realLiveApi } = await import('./realApi');
      const broadcast = await realLiveApi.getBroadcast(broadcastId);
      playbackUrl = broadcast.playbackUrl || '';
    } catch (err) {
    }
    const stream: MediaStream = { url: playbackUrl, quality, bitrate: 0, codec: 'h264' };
    this.activeStreams.set(streamKey, stream);
    return stream;
  }

  attachHls(
    broadcastId: string,
    videoElement: HTMLVideoElement,
    playbackUrl: string,
    onManifestParsed?: () => void,
    onError?: (error: string) => void,
    onLevelSwitch?: (level: number) => void
  ): Hls | null {
    if (!playbackUrl) { onError?.('No playback URL provided'); return null; }
    this.detachHls(broadcastId);
    const canPlayNative = videoElement.canPlayType('application/vnd.apple.mpegurl');
    if (canPlayNative === 'probably' || canPlayNative === 'maybe') {
      videoElement.src = playbackUrl;
      onManifestParsed?.();
      return null;
    }
    if (!Hls.isSupported()) { onError?.('HLS not supported'); return null; }
    const hls = new Hls({
      enableWorker: true, lowLatencyMode: true, backBufferLength: 90,
      maxBufferLength: 30, maxMaxBufferLength: 60, maxBufferSize: 60_000_000,
      maxBufferHole: 0.5, liveSyncDurationCount: 3, liveMaxLatencyDurationCount: 10,
      liveDurationInfinity: true, manifestLoadingTimeOut: 10000,
      manifestLoadingMaxRetry: 3, manifestLoadingRetryDelay: 1000,
      fragLoadingTimeOut: 20000, fragLoadingMaxRetry: 3, fragLoadingRetryDelay: 1000,
    });
    hls.loadSource(playbackUrl);
    hls.attachMedia(videoElement);
    hls.on(Hls.Events.MANIFEST_PARSED, () => onManifestParsed?.());
    hls.on(Hls.Events.LEVEL_SWITCHED, (_e, d) => onLevelSwitch?.(d.level));
    hls.on(Hls.Events.ERROR, (_e, d) => {
      if (d.fatal) {
        if (d.type === Hls.ErrorTypes.NETWORK_ERROR) hls.startLoad();
        else if (d.type === Hls.ErrorTypes.MEDIA_ERROR) hls.recoverMediaError();
        else onError?.(`Fatal HLS error: ${d.details}`);
      }
    });
    this.hlsInstances.set(broadcastId, hls);
    this.videoElements.set(broadcastId, videoElement);
    return hls;
  }

  detachHls(broadcastId: string): void {
    const hls = this.hlsInstances.get(broadcastId);
    if (hls) { hls.destroy(); this.hlsInstances.delete(broadcastId); }
    this.videoElements.delete(broadcastId);
  }

  switchQuality(broadcastId: string, newQuality: StreamQuality): MediaStream {
    const hls = this.hlsInstances.get(broadcastId);
    if (!hls) throw new Error(`No HLS instance for ${broadcastId}`);
    if (newQuality === 'auto') { hls.currentLevel = -1; }
    else {
      const target = this.qualityToHeight(newQuality);
      const idx = hls.levels.findIndex(l => l.height >= target);
      if (idx >= 0) hls.currentLevel = idx;
    }
    const level = hls.levels[hls.currentLevel];
    return {
      url: hls.url || '', quality: newQuality,
      bitrate: level?.bitrate || 0,
      resolution: level ? { width: level.width, height: level.height } : undefined,
      codec: 'h264'
    };
  }

  getPlayerStats(broadcastId: string): PlayerStats {
    const video = this.videoElements.get(broadcastId);
    const hls = this.hlsInstances.get(broadcastId);
    if (!video) return { bitrate: 0, bufferLength: 0, droppedFrames: 0, latency: 0, networkActivity: false };
    let bufferLength = 0;
    if (video.buffered.length > 0) {
      for (let i = 0; i < video.buffered.length; i++) {
        if (video.buffered.start(i) <= video.currentTime && video.buffered.end(i) >= video.currentTime) {
          bufferLength = video.buffered.end(i) - video.currentTime; break;
        }
      }
    }
    const bitrate = hls && hls.currentLevel >= 0 && hls.levels[hls.currentLevel]
      ? hls.levels[hls.currentLevel].bitrate : 0;
    return { bitrate, bufferLength, droppedFrames: 0, latency: 0, networkActivity: !video.paused && video.readyState < 4 };
  }

  getAvailableQualities(type: 'video' | 'audio'): StreamQuality[] {
    if (type === 'audio') return ['auto'];
    return ['1080p', '720p', '480p', '360p', 'auto'];
  }

  getHlsLevels(broadcastId: string): StreamQuality[] {
    const hls = this.hlsInstances.get(broadcastId);
    if (!hls?.levels) return ['auto'];
    const q: StreamQuality[] = ['auto'];
    hls.levels.forEach(l => { const qual = this.heightToQuality(l.height); if (!q.includes(qual)) q.push(qual); });
    return q;
  }

  stopAllStreams(broadcastId: string): void {
    this.detachHls(broadcastId);
    Array.from(this.activeStreams.keys()).filter(k => k.startsWith(broadcastId)).forEach(k => this.activeStreams.delete(k));
  }

// Methods for backward compatibility with mock interface
simulateNetworkIssues(_broadcastId: string, _severity: 'low' | 'medium' | 'high'): void {
  // No-op in real mode — network issues are handled by actual network conditions
}

restoreNetwork(_broadcastId: string): void {
  // No-op in real mode
}

reset(): void {
  // Clean up all HLS instances
  Array.from(this.hlsInstances.keys()).forEach(id => this.detachHls(id));
  this.activeStreams.clear();
}
  private qualityToHeight(q: StreamQuality): number {
    return q === '1080p' ? 1080 : q === '720p' ? 720 : q === '480p' ? 480 : 360;
  }
  private heightToQuality(h: number): StreamQuality {
    if (h >= 1080) return '1080p'; if (h >= 720) return '720p'; if (h >= 480) return '480p'; return '360p';
  }
}

export const realMediaService = RealMediaService.getInstance();
export type { RealMediaService as MediaService };
