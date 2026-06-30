// Хук для управления HLS плеером
// Инкапсулирует всю логику HLS: init, quality switching, error recovery, stats

import { useState, useEffect, useRef, useCallback } from 'react';
import Hls, { Level } from 'hls.js';
import { StreamQuality, LiveBroadcast } from '../../services/live/types';
import { mediaService } from '../../services/live';
import { getRecommendedQuality } from '../../utils/live/helpers';

export interface HlsPlayerState {
  currentQuality: StreamQuality;
  availableQualities: StreamQuality[];
  isLoading: boolean;
  error: string | null;
  stats: {
    bitrate: number;
    bufferLength: number;
    droppedFrames: number;
    latency: number;
    networkActivity: boolean;
  };
}

export interface UseHlsPlayerOptions {
  broadcast: LiveBroadcast;
  autoPlay?: boolean;
  muted?: boolean;
  onError?: (error: Error) => void;
  onReady?: () => void;
  onPlay?: () => void;
  onPause?: () => void;
}

/**
 * Хук для управления HLS воспроизведением.
 *
 * Инкапсулирует:
 * - Инициализацию HLS (hls.js или нативная поддержка Safari)
 * - Переключение качества через hls.currentLevel (без rebuild)
 * - Автоматическое переключение качества по ширине контейнера
 * - Восстановление ошибок (network → startLoad, media → recoverMediaError)
 * - Сбор статистики (битрейт, буфер, latency)
 * - Cleanup при размонтировании
 *
 * @example
 * ```tsx
 * const { state, switchQuality, videoRef, containerRef } = useHlsPlayer({
 *   broadcast,
 *   autoPlay: true,
 *   onError: (err) => console.error(err),
 * });
 * ```
 */
export function useHlsPlayer({
  broadcast,
  autoPlay = true,
  muted = false,
  onError,
  onReady,
}: UseHlsPlayerOptions) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const hlsRef = useRef<Hls | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  const [state, setState] = useState<HlsPlayerState>({
    currentQuality: 'auto',
    availableQualities: [],
    isLoading: true,
    error: null,
    stats: {
      bitrate: 0,
      bufferLength: 0,
      droppedFrames: 0,
      latency: 0,
      networkActivity: false,
    },
  });

  // Ref для callback-ов чтобы не триггерить re-init HLS
  const onErrorRef = useRef(onError);
  const onReadyRef = useRef(onReady);
  onErrorRef.current = onError;
  onReadyRef.current = onReady;

  // Обновление доступных качеств из HLS levels
  const updateAvailableQualities = useCallback((levels: Level[]) => {
    if (!levels || levels.length === 0) {
      const defaults: StreamQuality[] = ['1080p', '720p', '480p', '360p', 'auto'];
      setState(prev => ({ ...prev, availableQualities: defaults }));
      return;
    }

    const qualities = levels.map((level: Level) => {
      if (level.height >= 1080) return '1080p' as StreamQuality;
      if (level.height >= 720) return '720p' as StreamQuality;
      if (level.height >= 480) return '480p' as StreamQuality;
      return '360p' as StreamQuality;
    });

    const unique = Array.from(new Set<StreamQuality>([...qualities, 'auto']));
    setState(prev => ({ ...prev, availableQualities: unique }));
  }, []);

  // Определение качества из HLS level
  const getQualityFromLevel = useCallback((level: Level): StreamQuality => {
    if (level.height >= 1080) return '1080p';
    if (level.height >= 720) return '720p';
    if (level.height >= 480) return '480p';
    return '360p';
  }, []);

  // Инициализация HLS
  const initHls = useCallback((streamUrl: string) => {
    if (!videoRef.current) return;

    // Очищаем предыдущий инстанс
    if (hlsRef.current) {
      hlsRef.current.destroy();
      hlsRef.current = null;
    }

    const video = videoRef.current;
    const canPlayNative = video.canPlayType('application/vnd.apple.mpegurl');

    if (canPlayNative === 'probably' || canPlayNative === 'maybe') {
      // Safari / iOS — нативная поддержка
      video.src = streamUrl;
      setState(prev => ({ ...prev, isLoading: false }));
      onReadyRef.current?.();
      return;
    }

    if (Hls.isSupported()) {
      const hls = new Hls({
        enableWorker: true,
        lowLatencyMode: true,
        backBufferLength: 90,
        maxBufferLength: 30,
        maxMaxBufferLength: 60,
        maxBufferSize: 60 * 1000 * 1000,
        maxBufferHole: 0.5,
        maxFragLookUpTolerance: 0.25,
        liveSyncDurationCount: 3,
        liveMaxLatencyDurationCount: 10,
        liveDurationInfinity: true,
        enableWebVTT: true,
        enableCEA708Captions: true,
        captionsTextTrack1Label: 'English',
        captionsTextTrack1LanguageCode: 'en',
        manifestLoadingTimeOut: 10000,
        manifestLoadingMaxRetry: 3,
        manifestLoadingRetryDelay: 1000,
        manifestLoadingMaxRetryTimeout: 64000,
        levelLoadingTimeOut: 10000,
        levelLoadingMaxRetry: 3,
        levelLoadingRetryDelay: 1000,
        levelLoadingMaxRetryTimeout: 64000,
        fragLoadingTimeOut: 20000,
        fragLoadingMaxRetry: 3,
        fragLoadingRetryDelay: 1000,
        fragLoadingMaxRetryTimeout: 64000,
      });

      hls.loadSource(streamUrl);
      hls.attachMedia(video);

      hls.on(Hls.Events.MANIFEST_PARSED, () => {
        setState(prev => ({ ...prev, isLoading: false }));
        onReadyRef.current?.();
      });

      hls.on(Hls.Events.LEVEL_LOADED, () => {
        updateAvailableQualities(hls.levels);
      });

      hls.on(Hls.Events.LEVEL_SWITCHED, (_event, data) => {
        if (hls.levels[data.level]) {
          const quality = getQualityFromLevel(hls.levels[data.level]);
          setState(prev => ({ ...prev, currentQuality: quality }));
        }
      });

      hls.on(Hls.Events.ERROR, (_event, data) => {
        if (data.fatal) {
          switch (data.type) {
            case Hls.ErrorTypes.NETWORK_ERROR:
              hls.startLoad();
              break;
            case Hls.ErrorTypes.MEDIA_ERROR:
              hls.recoverMediaError();
              break;
            default:
              hls.destroy();
              initHls(streamUrl);
              break;
          }
        }

        const errorMsg = `Ошибка воспроизведения: ${data.details}`;
        setState(prev => ({ ...prev, error: errorMsg }));
        onErrorRef.current?.(new Error(data.details));
      });

      hlsRef.current = hls;
    } else {
      const errorMsg = 'Браузер не поддерживает HLS.';
      setState(prev => ({ ...prev, error: errorMsg, isLoading: false }));
      onErrorRef.current?.(new Error('HLS not supported'));
    }
  }, [updateAvailableQualities, getQualityFromLevel]);

  // Переключение качества — через hls.currentLevel (без rebuild)
  const switchQuality = useCallback((quality: StreamQuality) => {
    setState(prev => {
      if (!hlsRef.current || quality === prev.currentQuality) return prev;

      try {
        if (quality === 'auto') {
          hlsRef.current.currentLevel = -1;
        } else {
          const targetHeight = quality === '1080p' ? 1080
            : quality === '720p' ? 720
            : quality === '480p' ? 480
            : 360;
          const idx = hlsRef.current.levels.findIndex((l: Level) => l.height >= targetHeight);
          if (idx >= 0) {
            hlsRef.current.currentLevel = idx;
          }
        }
        return { ...prev, currentQuality: quality };
      } catch (err) {
        const errorMsg = err instanceof Error ? err.message : 'Ошибка переключения качества';
        return { ...prev, error: errorMsg };
      }
    });
  }, []);

  // Автоматическое переключение качества по ширине контейнера
  const autoSwitchQuality = useCallback(() => {
    if (!containerRef.current) return;

    const width = containerRef.current.clientWidth;
    const recommended = getRecommendedQuality(width);

    setState(prev => {
      if (recommended !== prev.currentQuality && prev.availableQualities.includes(recommended)) {
        // Вызываем switchQuality через ref чтобы избежать замкнутости
        if (hlsRef.current) {
          try {
            if (recommended === 'auto') {
              hlsRef.current.currentLevel = -1;
            } else {
              const targetHeight = recommended === '1080p' ? 1080
                : recommended === '720p' ? 720
                : recommended === '480p' ? 480
                : 360;
              const idx = hlsRef.current.levels.findIndex((l: Level) => l.height >= targetHeight);
              if (idx >= 0) {
                hlsRef.current.currentLevel = idx;
              }
            }
            return { ...prev, currentQuality: recommended };
          } catch {
            return prev;
          }
        }
      }
      return prev;
    });
  }, []);

  // Обновление статистики
  const updateStats = useCallback(() => {
    if (!videoRef.current || !broadcast) return;
    const playerStats = mediaService.getPlayerStats(broadcast.id);
    setState(prev => ({ ...prev, stats: playerStats }));
  }, [broadcast]);

  // Основной useEffect — init HLS, ResizeObserver, stats interval
  useEffect(() => {
    if (!broadcast) return;

    const loadStream = async () => {
      try {
        const stream = await mediaService.getStream(
          broadcast.id,
          broadcast.streamType,
          'auto'
        );
        if (stream?.url) {
          initHls(stream.url);
        }
      } catch (err) {
        setState(prev => ({
          ...prev,
          error: err instanceof Error ? err.message : 'Ошибка получения потока',
          isLoading: false,
        }));
      }
    };

    loadStream();

    // Доступные качества
    const qualities = mediaService.getAvailableQualities(broadcast.streamType);
    setState(prev => ({ ...prev, availableQualities: qualities }));

    // ResizeObserver для auto quality switch
    let resizeObserver: ResizeObserver | null = null;
    if (containerRef.current && typeof ResizeObserver !== 'undefined') {
      resizeObserver = new ResizeObserver(autoSwitchQuality);
      resizeObserver.observe(containerRef.current);
    }

    // Stats interval
    const statsInterval = setInterval(updateStats, 1000);

    return () => {
      if (hlsRef.current) {
        hlsRef.current.destroy();
        hlsRef.current = null;
      }
      resizeObserver?.disconnect();
      clearInterval(statsInterval);
    };
  }, [broadcast?.id]); // Only re-init on broadcast change

  // Cleanup при unmount
  useEffect(() => {
    return () => {
      if (hlsRef.current && typeof hlsRef.current.destroy === 'function') {
        hlsRef.current.destroy();
        hlsRef.current = null;
      }
    };
  }, []);

  return {
    state,
    videoRef,
    containerRef,
    switchQuality,
    autoSwitchQuality,
    hlsRef,
  };
}
