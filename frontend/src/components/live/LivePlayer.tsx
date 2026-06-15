// Компонент live плеера
// Поддерживает HLS/DASH потоки, управление качеством, статистику

import React, { useState, useEffect, useRef, useCallback } from 'react';
import Hls from 'hls.js';
import {
  StreamQuality,
  LiveBroadcast
} from '../../services/live/types';
import { mediaService } from '../../services/live';
import { usePlayerState } from '../../context/live/LiveSessionContext';
import {
  formatDuration,
  formatBitrate,
  getResolutionForQuality,
  getRecommendedQuality
} from '../../utils/live/helpers';

export interface LivePlayerProps {
  broadcast: LiveBroadcast;
  autoPlay?: boolean;
  muted?: boolean;
  controls?: boolean;
  onError?: (error: Error) => void;
  onReady?: () => void;
  onPlay?: () => void;
  onPause?: () => void;
  onEnded?: () => void;
}

export const LivePlayer: React.FC<LivePlayerProps> = ({
  broadcast,
  autoPlay = true,
  muted = false,
  controls = true,
  onError,
  onReady,
  onPlay,
  onPause,
  onEnded
}) => {
  const videoRef = useRef<HTMLVideoElement>(null);
  const hlsRef = useRef<Hls | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  const { playerState, setPlayerState } = usePlayerState();
  const [currentQuality, setCurrentQuality] = useState<StreamQuality>('auto');
  const [availableQualities, setAvailableQualities] = useState<StreamQuality[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [stats, setStats] = useState({
    bitrate: 0,
    bufferLength: 0,
    droppedFrames: 0,
    latency: 0,
    networkActivity: false
  });

  // Получение потока
  const getStream = useCallback(() => {
    if (!broadcast) return null;

    try {
      return mediaService.getStream(
        broadcast.id,
        broadcast.streamType,
        currentQuality
      );
    } catch (err) {
      console.error('[LivePlayer] Ошибка получения потока:', err);
      setError(err instanceof Error ? err.message : 'Ошибка получения потока');
      return null;
    }
  }, [broadcast, currentQuality]);

  // Инициализация HLS
  const initHls = useCallback((streamUrl: string) => {
    if (!videoRef.current) return;

    // Очищаем предыдущий HLS инстанс
    if (hlsRef.current) {
      hlsRef.current.destroy();
      hlsRef.current = null;
    }

    // Проверяем нативную поддержку HLS
    const video = videoRef.current;
    const canPlayNative = video.canPlayType('application/vnd.apple.mpegurl');

    if (canPlayNative === 'probably' || canPlayNative === 'maybe') {
      // Используем нативную поддержку (Safari, iOS)
      video.src = streamUrl;
      if (onReady) onReady();
      return;
    }

    // Используем hls.js для других браузеров
    if (Hls.isSupported()) {
      const hls = new Hls({
        enableWorker: true,
        lowLatencyMode: true,
        backBufferLength: 90,
        maxBufferLength: 30,
        maxMaxBufferLength: 60,
        maxBufferSize: 60 * 1000 * 1000, // 60MB
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
        fragLoadingMaxRetryTimeout: 64000
      });

      hls.loadSource(streamUrl);
      hls.attachMedia(video);

      hls.on(Hls.Events.MANIFEST_PARSED, () => {
        // console.log('[LivePlayer] HLS манифест загружен');
        setIsLoading(false);
        if (onReady) onReady();

        // Автовоспроизведение если разрешено
        if (autoPlay && !muted) {
          video.play().catch(console.error);
        }
      });

      hls.on(Hls.Events.LEVEL_LOADED, (event, data) => {
        // console.log('[LivePlayer] Уровень качества загружен:', data.level);
        updateAvailableQualities(hls.levels);
      });

      hls.on(Hls.Events.LEVEL_SWITCHED, (event, data) => {
        // console.log('[LivePlayer] Качество переключено:', data.level);
        if (hls.levels[data.level]) {
          const quality = getQualityFromLevel(hls.levels[data.level]);
          setCurrentQuality(quality);
        }
      });

      hls.on(Hls.Events.ERROR, (event, data) => {
        console.error('[LivePlayer] HLS ошибка:', data);

        if (data.fatal) {
          switch (data.type) {
            case Hls.ErrorTypes.NETWORK_ERROR:
              console.error('[LivePlayer] Сетевая ошибка, пытаемся переподключиться...');
              hls.startLoad();
              break;
            case Hls.ErrorTypes.MEDIA_ERROR:
              console.error('[LivePlayer] Медиа ошибка, пытаемся восстановить...');
              hls.recoverMediaError();
              break;
            default:
              console.error('[LivePlayer] Фатальная ошибка, перезапускаем HLS...');
              hls.destroy();
              initHls(streamUrl);
              break;
          }
        }

        setError(`Ошибка воспроизведения: ${data.details}`);
        if (onError) onError(new Error(data.details));
      });

      hlsRef.current = hls;
    } else {
      setError('Браузер не поддерживает HLS. Пожалуйста, используйте современный браузер.');
      if (onError) onError(new Error('HLS not supported'));
    }
  }, [autoPlay, muted, onError, onReady]);

  // Обновление доступных качеств
  const updateAvailableQualities = useCallback((levels: any[]) => {
    if (!levels || levels.length === 0) {
      const defaultQualities: StreamQuality[] = ['1080p', '720p', '480p', '360p', 'auto'];
      setAvailableQualities(defaultQualities);
      return;
    }

    const qualities = levels.map(level => {
      if (level.height >= 1080) return '1080p';
      if (level.height >= 720) return '720p';
      if (level.height >= 480) return '480p';
      return '360p';
    });

    // Добавляем уникальные качества
    const uniqueQualities = Array.from(new Set([...qualities, 'auto'] as StreamQuality[]));
    setAvailableQualities(uniqueQualities);
  }, []);

  // Получение качества из уровня HLS
  const getQualityFromLevel = useCallback((level: any): StreamQuality => {
    if (level.height >= 1080) return '1080p';
    if (level.height >= 720) return '720p';
    if (level.height >= 480) return '480p';
    return '360p';
  }, []);

  // Переключение качества
  const switchQuality = useCallback((quality: StreamQuality) => {
    if (!hlsRef.current || quality === currentQuality) return;

    try {
      const stream = mediaService.switchQuality(broadcast.id, quality);
      setCurrentQuality(quality);

      // Перезагружаем HLS с новым потоком
      if (hlsRef.current) {
        hlsRef.current.destroy();
      }
      initHls(stream.url);

      // console.log(`[LivePlayer] Качество переключено на: ${quality}`);
    } catch (err) {
      console.error('[LivePlayer] Ошибка переключения качества:', err);
      setError(err instanceof Error ? err.message : 'Ошибка переключения качества');
    }
  }, [broadcast.id, currentQuality, initHls]);

  // Автоматическое переключение качества на основе ширины экрана
  const autoSwitchQuality = useCallback(() => {
    if (!containerRef.current) return;

    const width = containerRef.current.clientWidth;
    const recommendedQuality = getRecommendedQuality(width);

    if (recommendedQuality !== currentQuality && availableQualities.includes(recommendedQuality)) {
      switchQuality(recommendedQuality);
    }
  }, [currentQuality, availableQualities, switchQuality]);

  // Обновление статистики
  const updateStats = useCallback(() => {
    if (!videoRef.current) return;

    const video = videoRef.current;
    const playerStats = mediaService.getPlayerStats(broadcast.id);

    setStats(playerStats);

    // Обновляем состояние плеера
    setPlayerState({
      isPlaying: !video.paused,
      currentTime: video.currentTime,
      volume: video.volume,
      isMuted: video.muted
    });
  }, [broadcast.id, setPlayerState]);

  // Обработчики событий видео
  const handlePlay = useCallback(() => {
    setPlayerState({ isPlaying: true });
    if (onPlay) onPlay();
  }, [setPlayerState, onPlay]);

  const handlePause = useCallback(() => {
    setPlayerState({ isPlaying: false });
    if (onPause) onPause();
  }, [setPlayerState, onPause]);

  const handleEnded = useCallback(() => {
    if (onEnded) onEnded();
  }, [onEnded]);

  const handleError = useCallback((e: React.SyntheticEvent<HTMLVideoElement>) => {
    const video = e.currentTarget;
    console.error('[LivePlayer] Видео ошибка:', video.error);

    const errorMessage = video.error?.message || 'Неизвестная ошибка видео';
    setError(`Ошибка видео: ${errorMessage}`);

    if (onError) onError(new Error(errorMessage));
  }, [onError]);

  // Полноэкранный режим
  const toggleFullscreen = useCallback(() => {
    if (!containerRef.current) return;

    if (!document.fullscreenElement) {
      containerRef.current.requestFullscreen().catch(err => {
        console.error('[LivePlayer] Ошибка входа в полноэкранный режим:', err);
      });
      setPlayerState({ isFullscreen: true });
    } else {
      document.exitFullscreen();
      setPlayerState({ isFullscreen: false });
    }
  }, [setPlayerState]);

  // Инициализация плеера
  useEffect(() => {
    if (!broadcast) return;

    const stream = getStream();
    if (!stream) return;

    // console.log('[LivePlayer] Инициализация плеера для трансляции:', broadcast.id);
    // console.log('[LivePlayer] URL потока:', stream.url);

    initHls(stream.url);

    // Получаем доступные качества
    const qualities = mediaService.getAvailableQualities(broadcast.streamType);
    setAvailableQualities(qualities);

    // Автоматическое переключение качества при изменении размера
    const resizeObserver = new ResizeObserver(autoSwitchQuality);
    if (containerRef.current) {
      resizeObserver.observe(containerRef.current);
    }

    // Обновление статистики каждую секунду
    const statsInterval = setInterval(updateStats, 1000);

    return () => {
      // Очистка
      if (hlsRef.current) {
        hlsRef.current.destroy();
        hlsRef.current = null;
      }

      resizeObserver.disconnect();
      clearInterval(statsInterval);

      mediaService.stopAllStreams(broadcast.id);
    };
  }, [broadcast, getStream, initHls, autoSwitchQuality, updateStats]);

  // Обработка изменения качества
  useEffect(() => {
    if (broadcast && currentQuality) {
      autoSwitchQuality();
    }
  }, [broadcast, currentQuality, autoSwitchQuality]);

  if (!broadcast) {
    return (
      <div className="live-player-container" ref={containerRef}>
        <div className="live-player-error">
          Трансляция не найдена
        </div>
      </div>
    );
  }

  const resolution = getResolutionForQuality(currentQuality);

  return (
    <div className="live-player-container" ref={containerRef}>
      {/* Видео элемент */}
      <video
        ref={videoRef}
        className="live-player-video"
        controls={controls}
        autoPlay={autoPlay}
        muted={muted}
        playsInline
        onPlay={handlePlay}
        onPause={handlePause}
        onEnded={handleEnded}
        onError={handleError}
      />

      {/* Загрузка */}
      {isLoading && (
        <div className="live-player-loading">
          <div className="loading-spinner"></div>
          <div className="loading-text">Загрузка трансляции...</div>
        </div>
      )}

      {/* Ошибка */}
      {error && (
        <div className="live-player-error">
          <div className="error-icon">⚠️</div>
          <div className="error-text">{error}</div>
          <button
            className="error-retry"
            onClick={() => window.location.reload()}
          >
            Перезагрузить
          </button>
        </div>
      )}

      {/* Статистика (только для разработки) */}
      {process.env.NODE_ENV === 'development' && (
        <div className="live-player-stats">
          <div className="stats-row">
            <span className="stats-label">Качество:</span>
            <span className="stats-value">{currentQuality}</span>
            {resolution && (
              <span className="stats-value">
                {resolution.width}×{resolution.height}
              </span>
            )}
          </div>
          <div className="stats-row">
            <span className="stats-label">Битрейт:</span>
            <span className="stats-value">{formatBitrate(stats.bitrate)}</span>
          </div>
          <div className="stats-row">
            <span className="stats-label">Буфер:</span>
            <span className="stats-value">{stats.bufferLength.toFixed(1)}с</span>
          </div>
          <div className="stats-row">
            <span className="stats-label">Задержка:</span>
            <span className="stats-value">{stats.latency.toFixed(1)}с</span>
          </div>
        </div>
      )}

      {/* Управление качеством */}
      {availableQualities.length > 1 && (
        <div className="live-player-quality-controls">
          <div className="quality-label">Качество:</div>
          <div className="quality-buttons">
            {availableQualities.map(quality => (
              <button
                key={quality}
                className={`quality-button ${quality === currentQuality ? 'active' : ''}`}
                onClick={() => switchQuality(quality)}
                disabled={quality === currentQuality}
              >
                {quality === 'auto' ? 'Авто' : quality}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Управление плеером */}
      <div className="live-player-controls">
        <button
          className="control-button"
          onClick={() => {
            if (videoRef.current) {
              if (playerState.isPlaying) {
                videoRef.current.pause();
              } else {
                videoRef.current.play();
              }
            }
          }}
        >
          {playerState.isPlaying ? '⏸️' : '▶️'}
        </button>

        <button
          className="control-button"
          onClick={() => {
            if (videoRef.current) {
              videoRef.current.muted = !videoRef.current.muted;
              setPlayerState({ isMuted: videoRef.current.muted });
            }
          }}
        >
          {playerState.isMuted ? '🔇' : '🔊'}
        </button>

        <button
          className="control-button"
          onClick={toggleFullscreen}
        >
          {playerState.isFullscreen ? '⤢' : '⤡'}
        </button>

        <div className="time-display">
          {formatDuration(videoRef.current?.currentTime || 0)}
        </div>
      </div>

      {/* Информация о трансляции */}
      <div className="live-player-info">
        <div className="broadcast-title">{broadcast.title}</div>
        {broadcast.description && (
          <div className="broadcast-description">{broadcast.description}</div>
        )}
        <div className="broadcast-stats">
          <span className="stat-item">👁️ {broadcast.viewerCount}</span>
          <span className="stat-item">📊 {broadcast.maxViewers}</span>
          {broadcast.startedAt && (
            <span className="stat-item">
              🕐 {formatDuration((Date.now() - new Date(broadcast.startedAt).getTime()) / 1000)}
            </span>
          )}
        </div>
      </div>
    </div>
  );
};

// Стили компонента (можно вынести в отдельный CSS файл)
const styles = `
.live-player-container {
  position: relative;
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  background: #000;
  border-radius: 8px;
  overflow: hidden;
}

.live-player-video {
  width: 100%;
  height: auto;
  display: block;
  background: #000;
}

.live-player-loading {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.8);
  color: white;
  z-index: 10;
}

.loading-spinner {
  width: 50px;
  height: 50px;
  border: 4px solid rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  border-top-color: #4CAF50;
  animation: spin 1s ease-in-out infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-text {
  margin-top: 15px;
  font-size: 16px;
}

.live-player-error {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.9);
  color: white;
  padding: 20px;
  text-align: center;
  z-index: 20;
}

.error-icon {
  font-size: 48px;
  margin-bottom: 15px;
}

.error-text {
  font-size: 18px;
  margin-bottom: 20px;
  max-width: 80%;
}

.error-retry {
  background: #4CAF50;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
}

.error-retry:hover {
  background: #45a049;
}

.live-player-stats {
  position: absolute;
  top: 10px;
  right: 10px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 10px;
  border-radius: 4px;
  font-size: 12px;
  z-index: 5;
}

.stats-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
}

.stats-label {
  font-weight: bold;
  margin-right: 10px;
}

.stats-value {
  color: #4CAF50;
}

.live-player-quality-controls {
  position: absolute;
  bottom: 60px;
  right: 10px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 10px;
  border-radius: 4px;
  z-index: 5;
}

.quality-label {
  margin-bottom: 5px;
  font-size: 12px;
  opacity: 0.8;
}

.quality-buttons {
  display: flex;
  gap: 5px;
}

.quality-button {
  background: rgba(255, 255, 255, 0.2);
  color: white;
  border: none;
  padding: 4px 8px;
  border-radius: 3px;
  font-size: 11px;
  cursor: pointer;
}

.quality-button:hover {
  background: rgba(255, 255, 255, 0.3);
}

.quality-button.active {
  background: #4CAF50;
}

.quality-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.live-player-controls {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.8));
  padding: 10px;
  display: flex;
  align-items: center;
  gap: 10px;
  z-index: 5;
}

.control-button {
  background: rgba(255, 255, 255, 0.2);
  color: white;
  border: none;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 18px;
}

.control-button:hover {
  background: rgba(255, 255, 255, 0.3);
}

.time-display {
  color: white;
  font-size: 14px;
  margin-left: auto;
  opacity: 0.8;
}

.live-player-info {
  position: absolute;
  top: 10px;
  left: 10px;
  right: 10px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 10px;
  border-radius: 4px;
  z-index: 5;
}

.broadcast-title {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 5px;
}

.broadcast-description {
  font-size: 14px;
  opacity: 0.9;
  margin-bottom: 10px;
}

.broadcast-stats {
  display: flex;
  gap: 15px;
  font-size: 12px;
  opacity: 0.8;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 5px;
}
`;

// Добавляем стили в документ
if (typeof document !== 'undefined') {
  const styleElement = document.createElement('style');
  styleElement.textContent = styles;
  document.head.appendChild(styleElement);
}

export default LivePlayer;