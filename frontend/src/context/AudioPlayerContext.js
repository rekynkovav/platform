import React, { createContext, useContext, useState, useRef, useEffect } from 'react';

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
    const [isScrolled, setIsScrolled] = useState(false);
    const audioRef = useRef(null);
    const videoRef = useRef(null);
    const hideTimeoutRef = useRef(null);
    let animationFrameRef = useRef(null);

    // Отслеживание скролла
    useEffect(() => {
        const handleScroll = () => {
            const scrolled = window.scrollY > 100;
            setIsScrolled(scrolled);
            if (scrolled && currentTrack) {
                setIsMinimized(true);
            }
        };

        window.addEventListener('scroll', handleScroll);
        return () => window.removeEventListener('scroll', handleScroll);
    }, [currentTrack]);

    const getMediaRef = () => {
        return currentTrack?.type === 'video' ? videoRef : audioRef;
    };

    useEffect(() => {
        const mediaRef = getMediaRef();
        if (mediaRef.current) {
            mediaRef.current.volume = volume;
            mediaRef.current.playbackRate = playbackRate;
        }
    }, [volume, playbackRate, currentTrack]);

    useEffect(() => {
        const mediaRef = getMediaRef();
        if (mediaRef.current) {
            const updateTime = () => {
                setCurrentTime(mediaRef.current.currentTime);
                animationFrameRef.current = requestAnimationFrame(updateTime);
            };

            const updateDuration = () => setDuration(mediaRef.current.duration);

            mediaRef.current.addEventListener('loadedmetadata', updateDuration);
            mediaRef.current.addEventListener('ended', () => {
                setIsPlaying(false);
                setCurrentTrack(null);
                setCurrentTime(0);
                setDuration(0);
                setIsMinimized(false);
            });
            mediaRef.current.addEventListener('play', () => setIsPlaying(true));
            mediaRef.current.addEventListener('pause', () => setIsPlaying(false));

            if (isPlaying) {
                animationFrameRef.current = requestAnimationFrame(updateTime);
            }

            return () => {
                if (mediaRef.current) {
                    mediaRef.current.removeEventListener('loadedmetadata', updateDuration);
                }
                if (animationFrameRef.current) {
                    cancelAnimationFrame(animationFrameRef.current);
                }
            };
        }
    }, [isPlaying, currentTrack]);

    const playTrack = (track) => {
        const mediaRef = getMediaRef();
        if (currentTrack?.id === track.id && mediaRef.current) {
            mediaRef.current.play();
        } else {
            setCurrentTrack(track);
            setCurrentTime(0);
            setDuration(0);
            setIsMinimized(false);
            setTimeout(() => {
                const newMediaRef = track.type === 'video' ? videoRef : audioRef;
                if (newMediaRef.current) {
                    newMediaRef.current.src = track.url;
                    newMediaRef.current.play();
                }
            }, 50);
        }
    };

    const pauseTrack = () => {
        const mediaRef = getMediaRef();
        if (mediaRef.current) {
            mediaRef.current.pause();
        }
    };

    const stopTrack = () => {
        const mediaRef = getMediaRef();
        if (mediaRef.current) {
            mediaRef.current.pause();
            mediaRef.current.src = '';
        }
        setCurrentTrack(null);
        setIsPlaying(false);
        setCurrentTime(0);
        setDuration(0);
        setIsMinimized(false);
    };

    const togglePlayPause = () => {
        const mediaRef = getMediaRef();
        if (isPlaying) {
            pauseTrack();
        } else {
            if (mediaRef.current && currentTrack) {
                mediaRef.current.play();
            }
        }
    };

    const seek = (time) => {
        const mediaRef = getMediaRef();
        setCurrentTime(time);
        if (mediaRef.current) {
            mediaRef.current.currentTime = time;
        }
    };

    const formatTime = (time) => {
        if (isNaN(time)) return '0:00';
        const mins = Math.floor(time / 60);
        const secs = Math.floor(time % 60);
        return `${mins}:${secs.toString().padStart(2, '0')}`;
    };

    const handleMouseEnter = () => {
        if (hideTimeoutRef.current) {
            clearTimeout(hideTimeoutRef.current);
            hideTimeoutRef.current = null;
        }
        setIsHovered(true);
    };

    const handleMouseLeave = () => {
        hideTimeoutRef.current = setTimeout(() => {
            setIsHovered(false);
        }, 500); // Задержка 500ms перед сворачиванием
    };

    return (
        <AudioPlayerContext.Provider value={{
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
            handleMouseLeave
        }}>
            {children}
            <audio ref={audioRef} style={{ display: 'none' }} />
            <video ref={videoRef} style={{ display: 'none' }} />
            {currentTrack && (
                <GlobalMediaPlayer />
            )}
        </AudioPlayerContext.Provider>
    );
};

// Глобальный компонент плеера
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
        handleMouseLeave
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

    if (isVideo && isMinimized) {
        return (
            <div
                className="global-video-player minimized"
                onMouseMove={handleVideoMouseMove}
                onMouseEnter={() => setShowControls(true)}
                onMouseLeave={() => setShowControls(false)}
            >
                <video
                    src={currentTrack.url}
                    autoPlay={isPlaying}
                    className="minimized-video"
                />
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
