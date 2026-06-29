import React, { useState, useEffect, useRef } from 'react';
import { useAudioPlayer } from '../../context/AudioPlayerContext';
import './MediaRenderer.css';

/**
 * MediaRenderer — universal media display component for images, video, audio, and generic files.
 *
 * Resolves stream URLs from mediaIds, handles loading and error states, and renders
 * the appropriate player or preview. Supports fullscreen image view and integrates
 * with the global AudioPlayer context for audio/video playback.
 *
 * @param {string} mediaUrl - URL or mediaId of the media resource to display
 * @param {string} mediaType - Type of media: 'image', 'video', 'audio', or 'file'
 * @param {string} [fileName] - Display name of the file
 * @param {() => void} [onLoadError] - Callback fired when the media fails to load
 * @param {boolean} [compact=false] - Whether to render a compact/inline version
 * @returns {JSX.Element} The rendered media component (image, video, audio player, or file link)
 */
const MediaRenderer = ({ mediaUrl, mediaType, fileName, onLoadError, compact = false }) => {
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(null);
    const [showFullscreen, setShowFullscreen] = useState(false);
    const [streamUrl, setStreamUrl] = useState(mediaUrl);
    const { playTrack, isPlaying, currentTrack } = useAudioPlayer();

    const isAudio = mediaType === 'audio';
    const isVideo = mediaType === 'video';
    const isMedia = isAudio || isVideo;

    // Проверяем, играет ли этот трек
    const isThisPlaying = currentTrack?.id === mediaUrl && isPlaying;

    useEffect(() => {
        if (!mediaUrl) return;
        let cancelled = false;

        // Если это уже полный URL - используем как есть
        if (mediaUrl.startsWith('http') || mediaUrl.startsWith('/api/')) {
            setStreamUrl(mediaUrl);
            setIsLoading(false);
            return;
        }

        // Иначе запрашиваем стрим
        const fetchStream = async () => {
            try {
                const response = await fetch(`/api/media/stream/${mediaUrl}`);
                if (!response.ok) {
                    throw new Error(`HTTP ${response.status}`);
                }
                const data = await response.json();
                if (!cancelled) setStreamUrl(data.url);
            } catch (e) {
                if (!cancelled) setError('Не удалось загрузить медиа');
            } finally {
                if (!cancelled) setIsLoading(false);
            }
        };

        fetchStream();
        return () => { cancelled = true; };
    }, [mediaUrl]);

    const handleLoad = () => setIsLoading(false);

    const handleError = (e) => {
        setIsLoading(false);
        setError('Не удалось загрузить медиа');
        onLoadError?.();
    };

    const clearError = () => {
        setError(null);
    };

    const handlePlayClick = (e) => {
        e?.stopPropagation();

        const track = {
            id: mediaUrl,
            url: streamUrl,
            title: fileName || (isVideo ? 'Видео' : 'Аудио'),
            type: isVideo ? 'video' : 'audio'
        };

        playTrack(track);
    };

    const toggleFullscreen = () => {
        setShowFullscreen(!showFullscreen);
    };

    // ===== AUDIO - инлайн с возможностью глобального воспроизведения =====
    const renderAudio = () => {
        if (compact) {
            return (
                <div
                    className={`media-audio-compact ${isThisPlaying ? 'playing' : ''}`}
                    onClick={handlePlayClick}
                >
                    <div className="audio-play-btn">
                        {isThisPlaying ? '⏸️' : '▶️'}
                    </div>
                    <div className="audio-info">
                        <div className="audio-wave"></div>
                        <span className="audio-name">{fileName || 'Голосовое сообщение'}</span>
                    </div>
                </div>
            );
        }

        return (
            <div className="media-audio-container">
                {isLoading && (
                    <div className="media-loading">
                        <div className="spinner"></div>
                    </div>
                )}
                <div className="audio-player-inline">
                    <button
                        className={`inline-play-btn ${isThisPlaying ? 'playing' : ''}`}
                        onClick={handlePlayClick}
                    >
                        {isThisPlaying ? '⏸️' : '▶️'}
                    </button>
                    <div className="inline-audio-info">
                        <span className="inline-audio-title">{fileName || 'Голосовое'}</span>
                        <div className="audio-wave-mini">
                            {[...Array(8)].map((_, i) => (
                                <span key={i} className="wave-bar" style={{animationDelay: `${i * 0.1}s`}}></span>
                            ))}
                        </div>
                    </div>
                </div>
                {error && <div className="media-error">{error}</div>}
            </div>
        );
    };

    // ===== VIDEO - thumbnail с play button =====
    const renderVideo = () => {
        if (isLoading) {
            return (
                <div className="media-video-loading">
                    <div className="spinner"></div>
                </div>
            );
        }

        return (
            <div className="media-video-container" onClick={!compact ? handlePlayClick : undefined}>
                <video
                    src={streamUrl}
                    className="media-video-preview"
                    muted
                    preload="metadata"
                    onLoadedData={handleLoad}
                    onError={handleError}
                    poster={streamUrl ? (streamUrl.includes('?') ? streamUrl + '&t=thumb' : streamUrl + '?t=thumb') : undefined}
                />
                {!compact && (
                    <div className="video-overlay">
                        <div className="video-play-btn-large">▶️</div>
                    </div>
                )}
                {error && <div className="media-error">{error}</div>}
            </div>
        );
    };

    // ===== IMAGE - полный функционал =====
    const renderImage = () => (
        <div className="media-image-container">
            {isLoading && (
                <div className="media-loading">
                    <div className="spinner"></div>
                </div>
            )}
            <img
                src={streamUrl}
                alt={fileName || 'Изображение'}
                className={`media-image ${isLoading ? 'hidden' : ''}`}
                onLoad={handleLoad}
                onError={handleError}
                onClick={toggleFullscreen}
                loading="lazy"
            />
            {showFullscreen && (
                <div className="media-fullscreen" onClick={toggleFullscreen}>
                    <img src={streamUrl} alt={fileName} />
                    <button className="fullscreen-close" onClick={(e) => { e.stopPropagation(); toggleFullscreen(); }}>✕</button>
                </div>
            )}
        </div>
    );

    // ===== FILE - скачивание =====
    const renderFile = () => (
        <div className="media-file-container">
            <a
                href={streamUrl && !streamUrl.startsWith('javascript:') ? streamUrl : '#'}
                target="_blank"
                rel="noopener noreferrer"
                className="media-file-link"
            >
                <span className="file-icon">📎</span>
                <span className="file-name">{fileName || 'Файл'}</span>
            </a>
        </div>
    );

    if (error && !isMedia) {
        return (
            <div className="media-error-container">
                <span className="media-error-icon">⚠️</span>
                <span className="media-error-text">{error}</span>
            </div>
        );
    }

    switch (mediaType) {
        case 'image':
            return renderImage();
        case 'video':
            return renderVideo();
        case 'audio':
            return renderAudio();
        default:
            return renderFile();
    }
};

export default MediaRenderer;