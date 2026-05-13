import React, { useState, useEffect } from 'react';
import { useAudioPlayer } from '../../contexts/AudioPlayerContext';
import './MediaRenderer.css';

const MediaRenderer = ({ mediaUrl, mediaType, fileName }) => {
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(null);
    const [streamUrl, setStreamUrl] = useState(mediaUrl);
    const { playTrack, isPlaying, currentTrack } = useAudioPlayer();

    const isAudio = mediaType === 'audio';
    const isVideo = mediaType === 'video';
    const isThisPlaying = currentTrack?.url === streamUrl && isPlaying;

    useEffect(() => {
        if (!mediaUrl) return;

        const fetchStreamUrl = async () => {
            try {
                if (mediaUrl.startsWith('http') || mediaUrl.startsWith('/api/media')) {
                    setStreamUrl(mediaUrl);
                    setIsLoading(false);
                    return;
                }

                const response = await fetch(`/api/media/stream/${mediaUrl}`);
                const data = await response.json();
                setStreamUrl(data.url);
            } catch (e) {
                console.error('Failed to get stream URL:', e);
                setError('Не удалось загрузить медиа');
            } finally {
                setIsLoading(false);
            }
        };

        fetchStreamUrl();
    }, [mediaUrl]);

    const handlePlayClick = () => {
        if (!streamUrl) return;

        const track = {
            id: mediaUrl,
            url: streamUrl,
            title: fileName || (isVideo ? 'Видео' : 'Аудио'),
            type: isVideo ? 'video' : 'audio'
        };

        playTrack(track);
    };

    const renderContent = () => {
        if (isLoading) {
            return <div className="media-loading"><div className="spinner"></div></div>;
        }

        if (error) {
            return (
                <div className="media-error-container">
                    <span className="media-error-icon">⚠️</span>
                    <span className="media-error-text">{error}</span>
                </div>
            );
        }

        if (isAudio) {
            return (
                <div className={`media-audio-compact ${isThisPlaying ? 'playing' : ''}`} onClick={handlePlayClick}>
                    <div className="audio-play-btn">{isThisPlaying ? '⏸️' : '▶️'}</div>
                    <div className="audio-info">
                        <div className="audio-wave"></div>
                        <span className="audio-name">{fileName || 'Аудио'}</span>
                    </div>
                </div>
            );
        }

        if (isVideo) {
            return (
                <div className="media-video-container" onClick={handlePlayClick}>
                    <video
                        src={streamUrl}
                        className="media-video-preview"
                        muted
                        preload="metadata"
                    />
                    <div className="video-overlay">
                        <div className="video-play-btn-large">▶️</div>
                    </div>
                </div>
            );
        }

        if (mediaType === 'image') {
            return (
                <img
                    src={streamUrl}
                    alt={fileName || 'Изображение'}
                    className="media-image"
                    loading="lazy"
                />
            );
        }

        return (
            <a href={streamUrl} target="_blank" rel="noopener noreferrer" className="media-file-link">
                <span className="file-icon">📎</span>
                <span className="file-name">{fileName || 'Файл'}</span>
            </a>
        );
    };

    return <div className="media-renderer">{renderContent()}</div>;
};

export default MediaRenderer;