import React from 'react';
import { render, screen, fireEvent, act } from '@testing-library/react';
import { AudioPlayerProvider, useAudioPlayer } from '../context/AudioPlayerContext';

// Тестовый компонент, использующий контекст
const TestComponent = () => {
    const {
        currentTrack,
        isPlaying,
        volume,
        playbackRate,
        currentTime,
        duration,
        error,
        playTrack,
        pauseTrack,
        stopTrack,
        togglePlayPause,
        setVolume,
        setPlaybackRate,
        seek,
        formatTime,
    } = useAudioPlayer();

    return (
        <div>
            <div data-testid="currentTrack">{currentTrack?.title || 'none'}</div>
            <div data-testid="isPlaying">{isPlaying ? 'playing' : 'paused'}</div>
            <div data-testid="volume">{volume}</div>
            <div data-testid="playbackRate">{playbackRate}</div>
            <div data-testid="currentTime">{currentTime}</div>
            <div data-testid="duration">{duration}</div>
            <div data-testid="error">{error || 'no error'}</div>
            <button data-testid="playTrack" onClick={() => playTrack({ id: '1', title: 'Test Track', url: 'blob:test', type: 'audio' })}>Play</button>
            <button data-testid="pauseTrack" onClick={pauseTrack}>Pause</button>
            <button data-testid="stopTrack" onClick={stopTrack}>Stop</button>
            <button data-testid="togglePlayPause" onClick={togglePlayPause}>Toggle</button>
            <button data-testid="setVolume" onClick={() => setVolume(0.5)}>Set Volume</button>
            <button data-testid="setPlaybackRate" onClick={() => setPlaybackRate(1.5)}>Set Rate</button>
            <button data-testid="seek" onClick={() => seek(30)}>Seek</button>
            <div data-testid="formatTime">{formatTime(125)}</div>
        </div>
    );
};

// Мок для HTMLMediaElement
beforeEach(() => {
    // Создаём мок для audio и video элементов
    window.HTMLMediaElement.prototype.play = jest.fn().mockResolvedValue();
    window.HTMLMediaElement.prototype.pause = jest.fn();
    window.HTMLMediaElement.prototype.load = jest.fn();
    window.HTMLMediaElement.prototype.addEventListeners = jest.fn();
    window.HTMLMediaElement.prototype.removeEventListener = jest.fn();
    // Устанавливаем начальные значения
    Object.defineProperty(window.HTMLMediaElement.prototype, 'currentTime', {
        writable: true,
        value: 0,
    });
    Object.defineProperty(window.HTMLMediaElement.prototype, 'duration', {
        writable: true,
        value: 0,
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

describe('AudioPlayerContext', () => {
    it('renders with default values', () => {
        render(
            <AudioPlayerProvider>
                <TestComponent />
            </AudioPlayerProvider>
        );

        expect(screen.getByTestId('currentTrack').textContent).toBe('none');
        expect(screen.getByTestId('isPlaying').textContent).toBe('paused');
        expect(screen.getByTestId('volume').textContent).toBe('0.7');
        expect(screen.getByTestId('playbackRate').textContent).toBe('1');
        expect(screen.getByTestId('currentTime').textContent).toBe('0');
        expect(screen.getByTestId('duration').textContent).toBe('0');
        expect(screen.getByTestId('error').textContent).toBe('no error');
    });

    it('playTrack sets currentTrack and starts playing', () => {
        render(
            <AudioPlayerProvider>
                <TestComponent />
            </AudioPlayerProvider>
        );

        fireEvent.click(screen.getByTestId('playTrack'));

        expect(screen.getByTestId('currentTrack').textContent).toBe('Test Track');
        expect(screen.getByTestId('isPlaying').textContent).toBe('playing');
    });

    it('pauseTrack pauses playback', () => {
        render(
            <AudioPlayerProvider>
                <TestComponent />
            </AudioPlayerProvider>
        );

        fireEvent.click(screen.getByTestId('playTrack'));
        fireEvent.click(screen.getByTestId('pauseTrack'));

        expect(screen.getByTestId('isPlaying').textContent).toBe('paused');
    });

    it('stopTrack clears track and resets state', () => {
        render(
            <AudioPlayerProvider>
                <TestComponent />
            </AudioPlayerProvider>
        );

        fireEvent.click(screen.getByTestId('playTrack'));
        fireEvent.click(screen.getByTestId('stopTrack'));

        expect(screen.getByTestId('currentTrack').textContent).toBe('none');
        expect(screen.getByTestId('isPlaying').textContent).toBe('paused');
        expect(screen.getByTestId('currentTime').textContent).toBe('0');
        expect(screen.getByTestId('duration').textContent).toBe('0');
        expect(screen.getByTestId('error').textContent).toBe('no error');
    });

    it('togglePlayPause toggles between play and pause', () => {
        render(
            <AudioPlayerProvider>
                <TestComponent />
            </AudioPlayerProvider>
        );

        fireEvent.click(screen.getByTestId('playTrack'));
        expect(screen.getByTestId('isPlaying').textContent).toBe('playing');

        fireEvent.click(screen.getByTestId('togglePlayPause'));
        expect(screen.getByTestId('isPlaying').textContent).toBe('paused');

        fireEvent.click(screen.getByTestId('togglePlayPause'));
        expect(screen.getByTestId('isPlaying').textContent).toBe('playing');
    });

    it('setVolume updates volume', () => {
        render(
            <AudioPlayerProvider>
                <TestComponent />
            </AudioPlayerProvider>
        );

        fireEvent.click(screen.getByTestId('setVolume'));
        expect(screen.getByTestId('volume').textContent).toBe('0.5');
    });

    it('setPlaybackRate updates playbackRate', () => {
        render(
            <AudioPlayerProvider>
                <TestComponent />
            </AudioPlayerProvider>
        );

        fireEvent.click(screen.getByTestId('setPlaybackRate'));
        expect(screen.getByTestId('playbackRate').textContent).toBe('1.5');
    });

    it('seek updates currentTime', () => {
        render(
            <AudioPlayerProvider>
                <TestComponent />
            </AudioPlayerProvider>
        );

        fireEvent.click(screen.getByTestId('playTrack'));
        fireEvent.click(screen.getByTestId('seek'));

        expect(screen.getByTestId('currentTime').textContent).toBe('30');
    });

    it('formatTime returns correct format', () => {
        render(
            <AudioPlayerProvider>
                <TestComponent />
            </AudioPlayerProvider>
        );

        expect(screen.getByTestId('formatTime').textContent).toBe('2:05');
    });

    it('handles media error and sets error state', () => {
        // Мок для play, который выбрасывает ошибку
        window.HTMLMediaElement.prototype.play = jest.fn().mockRejectedValue(new Error('Playback failed'));

        render(
            <AudioPlayerProvider>
                <TestComponent />
            </AudioPlayerProvider>
        );

        fireEvent.click(screen.getByTestId('playTrack'));

        // После неудачного play должен установиться error
        expect(screen.getByTestId('error').textContent).toBe('Не удалось воспроизвести медиа');
    });
});
