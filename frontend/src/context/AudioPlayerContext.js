import React, { createContext, useContext, useState, useEffect } from 'react';

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
    const [isMinimized, setIsMinimized] = useState(false);

    const playTrack = (track) => {
        setCurrentTrack(track);
        setIsPlaying(true);
        setIsMinimized(false);
    };

    const pauseTrack = () => {
        setIsPlaying(false);
    };

    const togglePlay = () => {
        setIsPlaying(prev => !prev);
    };

    const toggleMinimize = () => {
        setIsMinimized(prev => !prev);
    };

    const stopTrack = () => {
        setCurrentTrack(null);
        setIsPlaying(false);
    };

    // Автоматическая пауза при смене трека
    useEffect(() => {
        if (currentTrack) {
            setIsPlaying(true);
        }
    }, [currentTrack]);

    const value = {
        currentTrack,
        isPlaying,
        isMinimized,
        playTrack,
        pauseTrack,
        togglePlay,
        toggleMinimize,
        stopTrack
    };

    return (
        <AudioPlayerContext.Provider value={value}>
            {children}
        </AudioPlayerContext.Provider>
    );
};

export default AudioPlayerContext;
