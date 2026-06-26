import React from 'react';
import { renderHook, act } from '@testing-library/react';
import usePostAudioControls from '../hooks/usePostAudioControls';
import { AudioPlayerProvider } from '../context/AudioPlayerContext';

// Мок для HTMLAudioElement
beforeEach(() => {
    window.HTMLMediaElement.prototype.play = jest.fn().mockImplementation(function() {
        return new Promise((resolve) => {
            setTimeout(() => {
                this.dispatchEvent(new Event('play'));
                resolve();
            }, 0);
        });
    });
    window.HTMLMediaElement.prototype.pause = jest.fn().mockImplementation(function() {
        this.dispatchEvent(new Event('pause'));
    });
    window.HTMLMediaElement.prototype.load = jest.fn();
    window.HTMLMediaElement.prototype.removeAttribute = jest.fn();
    Object.defineProperty(window.HTMLMediaElement.prototype, 'currentTime', {
        writable: true,
        value: 0,
    });
    Object.defineProperty(window.HTMLMediaElement.prototype, 'duration', {
        writable: true,
        value: 100,
    });
    Object.defineProperty(window.HTMLMediaElement.prototype, 'volume', {
        writable: true,
        value: 0.7,
    });
    Object.defineProperty(window.HTMLMediaElement.prototype, 'playbackRate', {
        writable: true,
        value: 1.0,
    });
    Object.defineProperty(window.HTMLMediaElement.prototype, 'src', {
        writable: true,
        value: '',
    });
});

const wrapper = ({ children }) => (
    <AudioPlayerProvider>{children}</AudioPlayerProvider>
);

describe('usePostAudioControls', () => {
    it('should return default values', () => {
        const { result } = renderHook(() => usePostAudioControls(), { wrapper });
        expect(result.current.volume).toBe(0.7);
        expect(result.current.playbackRate).toBe(1.0);
        expect(result.current.currentTrack).toBeNull();
        expect(result.current.isThisPlaying('any')).toBe(false);
    });

    it('handlePlayAudio should set activeAudioPostId and play', async () => {
        const { result } = renderHook(() => usePostAudioControls(), { wrapper });
        const setActiveId = jest.fn();

        await act(async () => {
            result.current.handlePlayAudio(
                { id: 'test-id', url: 'blob:test', type: 'audio' },
                null,
                setActiveId
            );
        });
        // Flush setTimeout in play() mock that dispatches 'play' event
        await act(async () => {
            await new Promise(resolve => setTimeout(resolve, 10));
        });

        expect(setActiveId).toHaveBeenCalledWith('test-id');
        expect(result.current.isThisPlaying('test-id')).toBe(true);
    });

    it('stopTrack should clear currentTrack', async () => {
        const { result } = renderHook(() => usePostAudioControls(), { wrapper });
        const setActiveId = jest.fn();

        await act(async () => {
            result.current.handlePlayAudio(
                { id: 'test-id', url: 'blob:test', type: 'audio' },
                null,
                setActiveId
            );
        });

        act(() => {
            result.current.stopTrack();
        });
        expect(result.current.currentTrack).toBeNull();
        expect(result.current.isThisPlaying('test-id')).toBe(false);
    });

    it('handleSeek should update currentTime', async () => {
        const { result } = renderHook(() => usePostAudioControls(), { wrapper });
        const setActiveId = jest.fn();

        await act(async () => {
            result.current.handlePlayAudio(
                { id: 'test-id', url: 'blob:test', type: 'audio' },
                null,
                setActiveId
            );
        });

        const event = { target: { value: '42' } };
        act(() => {
            result.current.handleSeek(event, 'test-id');
        });
        expect(result.current.getCurrentTimeForPost('test-id')).toBe(42);
    });

    it('handleVolumeChangeLocal should update volume', async () => {
        const { result } = renderHook(() => usePostAudioControls(), { wrapper });
        const setActiveId = jest.fn();

        await act(async () => {
            result.current.handlePlayAudio(
                { id: 'test-id', url: 'blob:test', type: 'audio' },
                null,
                setActiveId
            );
        });

        const event = { target: { value: '0.5' } };
        act(() => {
            result.current.handleVolumeChangeLocal(event, 'test-id');
        });
        expect(result.current.volume).toBe(0.5);
    });

    it('handleSpeedChangeLocal should update playbackRate', async () => {
        const { result } = renderHook(() => usePostAudioControls(), { wrapper });
        const setActiveId = jest.fn();

        await act(async () => {
            result.current.handlePlayAudio(
                { id: 'test-id', url: 'blob:test', type: 'audio' },
                null,
                setActiveId
            );
        });

        const event = { target: { value: '1.5' } };
        act(() => {
            result.current.handleSpeedChangeLocal(event, 'test-id');
        });
        expect(result.current.playbackRate).toBe(1.5);
    });

    it('formatTime should format seconds correctly', () => {
        const { result } = renderHook(() => usePostAudioControls(), { wrapper });
        expect(result.current.formatTime(0)).toBe('0:00');
        expect(result.current.formatTime(65)).toBe('1:05');
        expect(result.current.formatTime(3661)).toBe('61:01');
        expect(result.current.formatTime(NaN)).toBe('0:00');
    });

    it('getCurrentTimeForPost and getDurationForPost should return 0 for unknown track', () => {
        const { result } = renderHook(() => usePostAudioControls(), { wrapper });
        expect(result.current.getCurrentTimeForPost('unknown')).toBe(0);
        expect(result.current.getDurationForPost('unknown')).toBe(0);
    });
});
