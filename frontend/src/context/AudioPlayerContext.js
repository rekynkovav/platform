import React, { createContext, useContext, useState, useRef, useCallback, useEffect } from 'react';

const AudioPlayerContext = createContext();

export const useAudioPlayer = () => {
    const context = useContext(AudioPlayerContext);
    if (!context) {
        throw new Error('useAudioPlayer must be used within AudioPlayerProvider');
    }
    return context;
};

export const AudioPlayerProvider = ({ children }) => {
    const [currentTrack, setCurrentTrack] = useState(null);
    const [isPlaying, setIsPlaying] = useState(false);
    const [volume, setVolume] = useState(0.7);
    const [playbackRate, setPlaybackRate] = useState(1.0);
    const [currentTime, setCurrentTime] = useState(0);
    const [duration, setDuration] = useState(0);
    const [isHovered, setIsHovered] = useState(false);
    const [isMinimized, setIsMinimized] = useState(false);
    const [error, setError] = useState(null);
    const audioRef = useRef(null);
    const videoRef = useRef(null);
    const hideTimeoutRef = useRef(null);
    const animationFrameRef = useRef(null);
    const isPlayingRef = useRef(false);

    const getMediaRef = useCallback(() => {
        return currentTrack?.type === 'video' ? videoRef : audioRef;
    }, [currentTrack]);

    useEffect(() => {
        const mediaRef = getMediaRef();
        if (mediaRef.current) {
            mediaRef.current.volume = volume;
            mediaRef.current.playbackRate = playbackRate;
        }
    }, [volume, playbackRate, getMediaRef]);

    const setupMediaListeners = useCallback((mediaEl) => {
        if (!mediaEl) return () => {};

        const handleTimeUpdate = () => {
            // Не запускаем animation frame, если плеер на паузе
            if (!isPlayingRef.current) {
                setCurrentTime(mediaEl.currentTime);
                return;
            }
            if (animationFrameRef.current) cancelAnimationFrame(animationFrameRef.current);
            const loop = () => {
                setCurrentTime(mediaEl.currentTime);
                animationFrameRef.current = requestAnimationFrame(loop);
            };
            animationFrameRef.current = requestAnimationFrame(loop);
        };

        const handleLoadedMetadata = () => {
            setDuration(mediaEl.duration);
        };

        const handleEnded = () => {
            setIsPlaying(false);
            isPlayingRef.current = false;
            setCurrentTrack(null);
            setCurrentTime(0);
            setDuration(0);
            setError(null);
            if (animationFrameRef.current) {
                cancelAnimationFrame(animationFrameRef.current);
                animationFrameRef.current = null;
            }
        };

        const handlePlay = () => {
            setIsPlaying(true);
            isPlayingRef.current = true;
        };

        const handlePause = () => {
            setIsPlaying(false);
            isPlayingRef.current = false;
            // Отменяем animation frame при паузе
            if (animationFrameRef.current) {
                cancelAnimationFrame(animationFrameRef.current);
                animationFrameRef.current = null;
            }
        };

        const handleError = () => {
            setError('Ошибка воспроизведения медиа');
            stopTrackRef.current();
        };

        mediaEl.addEventListener('timeupdate', handleTimeUpdate);
        mediaEl.addEventListener('loadedmetadata', handleLoadedMetadata);
        mediaEl.addEventListener('ended', handleEnded);
        mediaEl.addEventListener('play', handlePlay);
        mediaEl.addEventListener('pause', handlePause);
        mediaEl.addEventListener('error', handleError);

        return () => {
            mediaEl.removeEventListener('timeupdate', handleTimeUpdate);
            mediaEl.removeEventListener('loadedmetadata', handleLoadedMetadata);
            mediaEl.removeEventListener('ended', handleEnded);
            mediaEl.removeEventListener('play', handlePlay);
            mediaEl.removeEventListener('pause', handlePause);
            mediaEl.removeEventListener('error', handleError);
            if (animationFrameRef.current) {
                cancelAnimationFrame(animationFrameRef.current);
                animationFrameRef.current = null;
            }
        };
    }, []);

    const stopTrackRef = useRef();

    const stopTrack = useCallback(() => {
        const mediaRef = getMediaRef();
        if (mediaRef.current) {
            mediaRef.current.pause();
            mediaRef.current.removeAttribute('src');
            mediaRef.current.load();
        }
        setCurrentTrack(null);
        setIsPlaying(false);
        isPlayingRef.current = false;
        setCurrentTime(0);
        setDuration(0);
        setError(null);
        if (animationFrameRef.current) {
            cancelAnimationFrame(animationFrameRef.current);
            animationFrameRef.current = null;
        }
        if (hideTimeoutRef.current) {
            clearTimeout(hideTimeoutRef.current);
            hideTimeoutRef.current = null;
        }
        setIsHovered(false);
        setIsMinimized(false);
    }, [getMediaRef]);

    stopTrackRef.current = stopTrack;

    useEffect(() => {
        const mediaRef = getMediaRef();
        if (!mediaRef.current) return;
        const cleanup = setupMediaListeners(mediaRef.current);
        if (currentTrack && mediaRef.current.src) {
            if (isPlaying) {
                mediaRef.current.play().catch(() => {});
            }
        }
        return cleanup;
    }, [currentTrack, isPlaying, getMediaRef, setupMediaListeners]);

    const playTrack = useCallback((track) => {
        setError(null);
        const mediaRef = track.type === 'video' ? videoRef : audioRef;
        if (!mediaRef.current) return;

        if (currentTrack?.id === track.id) {
            mediaRef.current.play().catch(() => {});
        } else {
            if (audioRef.current) {
                audioRef.current.pause();
                audioRef.current.removeAttribute('src');
                audioRef.current.load();
            }
            if (videoRef.current) {
                videoRef.current.pause();
                videoRef.current.removeAttribute('src');
                videoRef.current.load();
            }
            setCurrentTrack(track);
            setCurrentTime(0);
            setDuration(0);
            setError(null);

            mediaRef.current.src = track.url;
            mediaRef.current.load();
            mediaRef.current.play().catch(() => {
                setError('Не удалось воспроизвести медиа');
            });
        }
    }, [currentTrack]);

    const pauseTrack = useCallback(() => {
        const mediaRef = getMediaRef();
        if (mediaRef.current) {
            mediaRef.current.pause();
        }
    }, [getMediaRef]);

    const togglePlayPause = useCallback(() => {
        const mediaRef = getMediaRef();
        if (!mediaRef.current) return;
        if (isPlaying) {
            mediaRef.current.pause();
        } else {
            mediaRef.current.play().catch(() => {});
        }
    }, [isPlaying, getMediaRef]);

    const seek = useCallback((time) => {
        const mediaRef = getMediaRef();
        if (mediaRef.current) {
            mediaRef.current.currentTime = time;
            setCurrentTime(time);
        }
    }, [getMediaRef]);

    const formatTime = useCallback((time) => {
        if (isNaN(time) || !isFinite(time)) return '0:00';
        const mins = Math.floor(time / 60);
        const secs = Math.floor(time % 60);
        return `${mins}:${secs.toString().padStart(2, '0')}`;
    }, []);

    const handleMouseEnter = useCallback(() => {
        if (hideTimeoutRef.current) {
            clearTimeout(hideTimeoutRef.current);
            hideTimeoutRef.current = null;
        }
        setIsHovered(true);
    }, []);

    const handleMouseLeave = useCallback(() => {
        hideTimeoutRef.current = setTimeout(() => {
            setIsHovered(false);
        }, 500);
    }, []);

    useEffect(() => {
        return () => {
            if (hideTimeoutRef.current) {
                clearTimeout(hideTimeoutRef.current);
            }
            if (animationFrameRef.current) {
                cancelAnimationFrame(animationFrameRef.current);
            }
        };
    }, []);

    const value = {
        currentTrack,
        isPlaying,
        volume,
        playbackRate,
        currentTime,
        duration,
        isHovered,
        isMinimized,
        setIsMinimized,
        playTrack,
        pauseTrack,
        stopTrack,
        togglePlayPause,
        setVolume,
        setPlaybackRate,
        seek,
        formatTime,
        handleMouseEnter,
        handleMouseLeave,
        error
    };

    return (
        <AudioPlayerContext.Provider value={value}>
            {children}
            <audio ref={audioRef} style={{ display: 'none' }} />
            <video ref={videoRef} style={{ display: 'none' }} />
            {currentTrack && (
                <GlobalMediaPlayer />
            )}
        </AudioPlayerContext.Provider>
    );
};

const GlobalMediaPlayer = () => {
    const {
        currentTrack,
        isPlaying,
        volume,
        playbackRate,
        currentTime,
        duration,
        isHovered,
        isMinimized,
        setIsMinimized,
        togglePlayPause,
        stopTrack,
        setVolume,
        setPlaybackRate,
        seek,
        formatTime,
        handleMouseEnter,
        handleMouseLeave,
        error
    } = useAudioPlayer();

    const [showControls, setShowControls] = useState(true);
    const controlsTimeoutRef = useRef(null);

    const handleVolumeChange = (e) => {
        setVolume(parseFloat(e.target.value));
    };

    const handleSpeedChange = (e) => {
        setPlaybackRate(parseFloat(e.target.value));
    };

    const handleSeek = (e) => {
        seek(parseFloat(e.target.value));
    };

    const handleVideoMouseMove = () => {
        setShowControls(true);
        if (controlsTimeoutRef.current) {
            clearTimeout(controlsTimeoutRef.current);
        }
        controlsTimeoutRef.current = setTimeout(() => {
            if (isMinimized) {
                setShowControls(false);
            }
        }, 2000);
    };

    const isVideo = currentTrack?.type === 'video';

    useEffect(() => {
        return () => {
            if (controlsTimeoutRef.current) {
                clearTimeout(controlsTimeoutRef.current);
            }
        };
    }, []);

    if (error) {
        return (
            <div className="global-audio-player error">
                <span className="error-text">⚠️ {error}</span>
                <button className="minimized-close" onClick={() => stopTrack()}>✕</button>
            </div>
        );
    }

    if (isVideo && isMinimized) {
        return (
            <div
                className="global-video-player minimized"
                onMouseMove={handleVideoMouseMove}
                onMouseEnter={() => setShowControls(true)}
                onMouseLeave={() => setShowControls(false)}
            >
                {showControls && (
                    <div className="minimized-video-controls">
                        <button className="video-control-btn" onClick={togglePlayPause}>
                            {isPlaying ? '⏸️' : '▶️'}
                        </button>
                        <button className="video-control-btn" onClick={() => setIsMinimized(false)} title="Развернуть">
                            ⬆️
                        </button>
                        <button className="video-control-btn close" onClick={stopTrack}>
                            ✕
                        </button>
                    </div>
                )}
                <div className="video-return-dot" onClick={() => setIsMinimized(false)} title="Вернуться к видео">
                    •
                </div>
            </div>
        );
    }

    return (
        <div
            className={`global-audio-player ${isVideo ? 'video-mode' : ''}`}
            onMouseEnter={handleMouseEnter}
            onMouseLeave={handleMouseLeave}
        >
            <div className="player-minimized">
                <div className="minimized-wave">{isVideo ? '🎬' : '🎵'}</div>
                <div className="minimized-info">
                    <div className="minimized-title">{currentTrack?.title}</div>
                    <div className="minimized-time">{formatTime(currentTime)}</div>
                </div>
                <button className="minimized-play" onClick={(e) => { e.stopPropagation(); togglePlayPause(); }}>
                    {isPlaying ? '⏸️' : '▶️'}
                </button>
                <button className="minimized-close" onClick={(e) => { e.stopPropagation(); stopTrack(); }}>
                    ✕
                </button>
            </div>

            {isHovered && (
                <div className="player-expanded">
                    <div className="expanded-header">
                        <div className="expanded-title">{isVideo ? '🎬' : '🎵'} {currentTrack?.title}</div>
                    </div>
                    <div className="expanded-progress">
                        <span>{formatTime(currentTime)}</span>
                        <input
                            type="range"
                            className="expanded-seek"
                            min="0"
                            max={duration || 100}
                            value={currentTime}
                            onChange={handleSeek}
                        />
                        <span>{formatTime(duration)}</span>
                    </div>
                    <div className="expanded-controls">
                        <button className="expanded-play" onClick={togglePlayPause}>
                            {isPlaying ? '⏸️' : '▶️'}
                        </button>
                        <button className="expanded-stop" onClick={stopTrack}>⏹️</button>
                        <div className="expanded-volume">
                            <span>🔊</span>
                            <input
                                type="range"
                                min="0"
                                max="1"
                                step="0.01"
                                value={volume}
                                onChange={handleVolumeChange}
                            />
                        </div>
                        <div className="expanded-speed">
                            <span>⚡</span>
                            <select value={playbackRate} onChange={handleSpeedChange}>
                                <option value="0.5">0.5x</option>
                                <option value="0.75">0.75x</option>
                                <option value="1.0">1.0x</option>
                                <option value="1.25">1.25x</option>
                                <option value="1.5">1.5x</option>
                                <option value="2.0">2.0x</option>
                            </select>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};
