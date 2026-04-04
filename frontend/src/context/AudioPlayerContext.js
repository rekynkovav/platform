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
    const audioRef = useRef(null);
    const hideTimeoutRef = useRef(null);
    let animationFrameRef = useRef(null);

    useEffect(() => {
        if (audioRef.current) {
            audioRef.current.volume = volume;
            audioRef.current.playbackRate = playbackRate;
        }
    }, [volume, playbackRate]);

    useEffect(() => {
        if (audioRef.current) {
            const updateTime = () => {
                setCurrentTime(audioRef.current.currentTime);
                animationFrameRef.current = requestAnimationFrame(updateTime);
            };

            const updateDuration = () => setDuration(audioRef.current.duration);

            audioRef.current.addEventListener('loadedmetadata', updateDuration);
            audioRef.current.addEventListener('ended', () => {
                setIsPlaying(false);
                setCurrentTrack(null);
                setCurrentTime(0);
                setDuration(0);
            });
            audioRef.current.addEventListener('play', () => setIsPlaying(true));
            audioRef.current.addEventListener('pause', () => setIsPlaying(false));

            if (isPlaying) {
                animationFrameRef.current = requestAnimationFrame(updateTime);
            }

            return () => {
                if (audioRef.current) {
                    audioRef.current.removeEventListener('loadedmetadata', updateDuration);
                }
                if (animationFrameRef.current) {
                    cancelAnimationFrame(animationFrameRef.current);
                }
            };
        }
    }, [isPlaying]);

    const playTrack = (track) => {
        if (currentTrack?.id === track.id && audioRef.current) {
            audioRef.current.play();
        } else {
            setCurrentTrack(track);
            setCurrentTime(0);
            setDuration(0);
            setTimeout(() => {
                if (audioRef.current) {
                    audioRef.current.src = track.url;
                    audioRef.current.play();
                }
            }, 50);
        }
    };

    const pauseTrack = () => {
        if (audioRef.current) {
            audioRef.current.pause();
        }
    };

    const stopTrack = () => {
        if (audioRef.current) {
            audioRef.current.pause();
            audioRef.current.src = '';
        }
        setCurrentTrack(null);
        setIsPlaying(false);
        setCurrentTime(0);
        setDuration(0);
    };

    const togglePlayPause = () => {
        if (isPlaying) {
            pauseTrack();
        } else {
            if (audioRef.current && currentTrack) {
                audioRef.current.play();
            }
        }
    };

    const seek = (time) => {
        setCurrentTime(time);
        if (audioRef.current) {
            audioRef.current.currentTime = time;
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
            {currentTrack && (
                <GlobalAudioPlayer />
            )}
        </AudioPlayerContext.Provider>
    );
};

// Глобальный компонент плеера
const GlobalAudioPlayer = () => {
    const {
        currentTrack,
        isPlaying,
        volume,
        playbackRate,
        currentTime,
        duration,
        isHovered,
        togglePlayPause,
        stopTrack,
        setVolume,
        setPlaybackRate,
        seek,
        formatTime,
        handleMouseEnter,
        handleMouseLeave
    } = useAudioPlayer();

    const handleVolumeChange = (e) => {
        setVolume(parseFloat(e.target.value));
    };

    const handleSpeedChange = (e) => {
        setPlaybackRate(parseFloat(e.target.value));
    };

    const handleSeek = (e) => {
        seek(parseFloat(e.target.value));
    };

    return (
        <div
            className="global-audio-player"
            onMouseEnter={handleMouseEnter}
            onMouseLeave={handleMouseLeave}
        >
            {/* Всегда видимый свернутый режим */}
            <div className="player-minimized">
                <div className="minimized-wave">🎵</div>
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

            {/* Развернутый режим при наведении */}
            {isHovered && (
                <div className="player-expanded">
                    <div className="expanded-header">
                        <div className="expanded-title">🎵 {currentTrack?.title}</div>
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