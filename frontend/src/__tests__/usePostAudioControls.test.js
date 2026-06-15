import { renderHook, act } from '@testing-library/react';
import usePostAudioControls from '../../hooks/usePostAudioControls';

// Мок для HTMLAudioElement
beforeEach(() => {
    window.HTMLMediaElement.prototype.play = jest.fn().mockResolvedValue();
    window.HTMLMediaElement.prototype.pause = jest.fn();
    window.HTMLMediaElement.prototype.load = jest.fn();
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
});

describe('usePostAudioControls', () => {
    it('should return default values', () => {
        const { result } = renderHook(() => usePostAudioControls());
        expect(result.current.volume).toBe(0.7);
        expect(result.current.playbackRate).toBe(1.0);
        expect(result.current.currentTrack).toBeNull();
        expect(result.current.isThisPlaying('any')).toBe(false);
    });

    it('handlePlayAudio should set activeAudioPostId and play', () => {
        const { result } = renderHook(() => usePostAudioControls());
        const setActiveId = jest.fn();
        act(() => {
            result.current.handlePlayAudio(
                { id: 'test-id', url: 'blob:test', type: 'audio' },
                null,
                setActiveId
            );
        });
        expect(setActiveId).toHaveBeenCalledWith('test-id');
        expect(result.current.isThisPlaying('test-id')).toBe(true);
    });

    it('stopTrack should clear currentTrack', () => {
        const { result } = renderHook(() => usePostAudioControls());
        const setActiveId = jest.fn();
        act(() => {
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

    it('handleSeek should update currentTime', () => {
        const { result } = renderHook(() => usePostAudioControls());
        const setActiveId = jest.fn();
        act(() => {
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

    it('handleVolumeChangeLocal should update volume', () => {
        const { result } = renderHook(() => usePostAudioControls());
        const event = { target: { value: '0.5' } };
        act(() => {
            result.current.handleVolumeChangeLocal(event, 'test-id');
        });
        expect(result.current.volume).toBe(0.5);
    });

    it('handleSpeedChangeLocal should update playbackRate', () => {
        const { result } = renderHook(() => usePostAudioControls());
        const event = { target: { value: '1.5' } };
        act(() => {
            result.current.handleSpeedChangeLocal(event, 'test-id');
        });
        expect(result.current.playbackRate).toBe(1.5);
    });

    it('formatTime should format seconds correctly', () => {
        const { result } = renderHook(() => usePostAudioControls());
        expect(result.current.formatTime(0)).toBe('0:00');
        expect(result.current.formatTime(65)).toBe('1:05');
        expect(result.current.formatTime(3661)).toBe('61:01');
        expect(result.current.formatTime(NaN)).toBe('0:00');
    });

    it('getCurrentTimeForPost and getDurationForPost should return 0 for unknown track', () => {
        const { result } = renderHook(() => usePostAudioControls());
        expect(result.current.getCurrentTimeForPost('unknown')).toBe(0);
        expect(result.current.getDurationForPost('unknown')).toBe(0);
    });
});
