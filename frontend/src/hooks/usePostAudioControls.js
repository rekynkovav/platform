import { useCallback } from 'react';
import { useAudioPlayer } from '../context/AudioPlayerContext';

/**
 * Хук для управления воспроизведением аудио в посте.
 * Возвращает функции, соответствующие обработчикам из MainFeed.
 */
const usePostAudioControls = () => {
    const {
        playTrack,
        pauseTrack,
        currentTrack,
        isPlaying,
        stopTrack,
        volume,
        playbackRate,
        setVolume,
        setPlaybackRate,
        seek,
        currentTime,
        duration,
        formatTime
    } = useAudioPlayer();

    const handlePlayAudio = useCallback((post, activeAudioPostId, setActiveAudioPostId) => {
        if (activeAudioPostId === post.id && currentTrack?.id === post.id && isPlaying) {
            pauseTrack();
        } else if (activeAudioPostId === post.id && currentTrack?.id === post.id && !isPlaying) {
            playTrack({ id: post.id, title: post.title, url: post.url });
        } else {
            setActiveAudioPostId(post.id);
            playTrack({ id: post.id, title: post.title, url: post.url });
        }
    }, [currentTrack, isPlaying, playTrack, pauseTrack]);

    const handleSeek = useCallback((e, postId) => {
        const newTime = parseFloat(e.target.value);
        if (currentTrack?.id === postId) {
            seek(newTime);
        }
    }, [currentTrack, seek]);

    const handleVolumeChangeLocal = useCallback((e, postId) => {
        const newVolume = parseFloat(e.target.value);
        if (currentTrack?.id === postId) {
            setVolume(newVolume);
        }
    }, [currentTrack, setVolume]);

    const handleSpeedChangeLocal = useCallback((e, postId) => {
        const newSpeed = parseFloat(e.target.value);
        if (currentTrack?.id === postId) {
            setPlaybackRate(newSpeed);
        }
    }, [currentTrack, setPlaybackRate]);

    const getCurrentTimeForPost = useCallback((postId) => {
        if (currentTrack?.id === postId) {
            return currentTime;
        }
        return 0;
    }, [currentTrack, currentTime]);

    const getDurationForPost = useCallback((postId) => {
        if (currentTrack?.id === postId) {
            return duration;
        }
        return 0;
    }, [currentTrack, duration]);

    const isThisPlaying = useCallback((postId) => {
        return currentTrack?.id === postId && isPlaying;
    }, [currentTrack, isPlaying]);

    return {
        handlePlayAudio,
        handleSeek,
        handleVolumeChangeLocal,
        handleSpeedChangeLocal,
        getCurrentTimeForPost,
        getDurationForPost,
        isThisPlaying,
        stopTrack,
        volume,
        playbackRate,
        formatTime,
        currentTrack
    };
};

export default usePostAudioControls;
