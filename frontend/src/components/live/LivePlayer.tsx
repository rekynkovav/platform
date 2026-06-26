// Компонент live плеера
// Поддерживает HLS/DASH потоки, управление качеством, статистику

import React, { useCallback } from 'react';
import {
  StreamQuality,
  LiveBroadcast
} from '../../services/live/types';
import { useHlsPlayer } from '../../hooks/live/useHlsPlayer';
import { usePlayerState } from '../../context/live/LiveSessionContext';
import {
  formatDuration,
  formatBitrate,
  getResolutionForQuality,
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

/**
 * LivePlayer — HLS/DASH live streaming video player with quality control.
 *
 * Renders a live video stream using hls.js, supporting adaptive bitrate streaming,
 * manual quality selection, fullscreen mode, volume control, and playback statistics.
 * Integrates with the LiveSessionContext for player state management and auto-detects
 * the recommended quality based on network conditions.
 *
 * HLS lifecycle is managed by the useHlsPlayer hook (extracted for testability).
 *
 * @param {LivePlayerProps} props - Component properties
 * @param {LiveBroadcast} props.broadcast - The broadcast data containing stream URLs and settings
 * @param {boolean} [props.autoPlay=true] - Whether to start playback automatically
 * @param {boolean} [props.muted=false] - Whether to start muted
 * @param {boolean} [props.controls=true] - Whether to show built-in video controls
 * @param {(error: Error) => void} [props.onError] - Callback when a playback error occurs
 * @param {() => void} [props.onReady] - Callback when the player is ready to play
 * @param {() => void} [props.onPlay] - Callback when playback starts
 * @param {() => void} [props.onPause] - Callback when playback pauses
 * @param {() => void} [props.onEnded] - Callback when the stream ends
 * @returns {JSX.Element} The live video player with quality selector and stats overlay
 */
export const LivePlayer: React.FC<LivePlayerProps> = ({
  broadcast,
  autoPlay = true,
  muted = false,
  controls = true,
  onError,
  onReady,
  onPlay,
  onPause,
  onEnded,
}) => {
  const { playerState, setPlayerState } = usePlayerState();

  const {
    state,
    videoRef,
    containerRef,
    switchQuality,
  } = useHlsPlayer({
    broadcast,
    autoPlay,
    muted,
    onError,
    onReady,
  });

  // Обработчики событий видео
  const handlePlay = useCallback(() => {
    setPlayerState({ isPlaying: true });
    onPlay?.();
  }, [setPlayerState, onPlay]);

  const handlePause = useCallback(() => {
    setPlayerState({ isPlaying: false });
    onPause?.();
  }, [setPlayerState, onPause]);

  const handleEnded = useCallback(() => {
    onEnded?.();
  }, [onEnded]);

  const handleError = useCallback((e: React.SyntheticEvent<HTMLVideoElement>) => {
    const video = e.currentTarget;
    const errorMessage = video.error?.message || 'Неизвестная ошибка видео';
    onError?.(new Error(errorMessage));
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
  }, [setPlayerState, containerRef]);

  if (!broadcast) {
    return (
      <div className="live-player-container" ref={containerRef}>
        <div className="live-player-error">
          Трансляция не найдена
        </div>
      </div>
    );
  }

  const resolution = getResolutionForQuality(state.currentQuality);

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
      {state.isLoading && (
        <div className="live-player-loading">
          <div className="loading-spinner"></div>
          <div className="loading-text">Загрузка трансляции...</div>
        </div>
      )}

      {/* Ошибка */}
      {state.error && (
        <div className="live-player-error">
          <div className="error-icon">⚠️</div>
          <div className="error-text">{state.error}</div>
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
            <span className="stats-value">{state.currentQuality}</span>
            {resolution && (
              <span className="stats-value">
                {resolution.width}×{resolution.height}
              </span>
            )}
          </div>
          <div className="stats-row">
            <span className="stats-label">Битрейт:</span>
            <span className="stats-value">{formatBitrate(state.stats.bitrate)}</span>
          </div>
          <div className="stats-row">
            <span className="stats-label">Буфер:</span>
            <span className="stats-value">{state.stats.bufferLength.toFixed(1)}с</span>
          </div>
          <div className="stats-row">
            <span className="stats-label">Задержка:</span>
            <span className="stats-value">{state.stats.latency.toFixed(1)}с</span>
          </div>
        </div>
      )}

      {/* Управление качеством */}
      {state.availableQualities.length > 1 && (
        <div className="live-player-quality-controls">
          <div className="quality-label">Качество:</div>
          <div className="quality-buttons">
            {state.availableQualities.map(quality => (
              <button
                key={quality}
                className={`quality-button ${quality === state.currentQuality ? 'active' : ''}`}
                onClick={() => switchQuality(quality)}
                disabled={quality === state.currentQuality}
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

export default LivePlayer;
