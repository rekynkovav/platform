import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import MediaRenderer from '../components/Chat/MediaRenderer';

// Моки
jest.mock('../components/Chat/MediaRenderer.css', () => ({}));
jest.mock('../context/AudioPlayerContext', () => ({
    useAudioPlayer: jest.fn(),
}));

const mockUseAudioPlayer = require('../context/AudioPlayerContext').useAudioPlayer;

describe('MediaRenderer', () => {
    const defaultAudioContext = {
        playTrack: jest.fn(),
        isPlaying: false,
        currentTrack: null,
    };

    beforeEach(() => {
        jest.clearAllMocks();
        mockUseAudioPlayer.mockReturnValue(defaultAudioContext);
    });

    it('renders image without crashing', () => {
        render(
            <MediaRenderer
                mediaUrl="/api/media/stream/img-1"
                mediaType="image"
                fileName="photo.jpg"
            />
        );
        const img = screen.getByAltText('photo.jpg');
        expect(img).toBeInTheDocument();
    });

    it('renders audio with play button', () => {
        render(
            <MediaRenderer
                mediaUrl="/api/media/stream/audio-1"
                mediaType="audio"
                fileName="voice.webm"
            />
        );
        expect(screen.getByText('▶️')).toBeInTheDocument();
        expect(screen.getByText('voice.webm')).toBeInTheDocument();
    });

    it('renders video with video element', () => {
        render(
            <MediaRenderer
                mediaUrl="/api/media/stream/video-1"
                mediaType="video"
                fileName="clip.mp4"
            />
        );
        const videoEl = document.querySelector('video');
        expect(videoEl).toBeInTheDocument();
    });

    it('renders file download link for unknown type', () => {
        render(
            <MediaRenderer
                mediaUrl="/api/media/stream/file-1"
                mediaType="file"
                fileName="document.pdf"
            />
        );
        expect(screen.getByText('document.pdf')).toBeInTheDocument();
        expect(screen.getByText('📎')).toBeInTheDocument();
    });

    it('calls playTrack when audio play button is clicked', () => {
        render(
            <MediaRenderer
                mediaUrl="/api/media/stream/audio-1"
                mediaType="audio"
                fileName="voice.webm"
            />
        );
        fireEvent.click(screen.getByText('▶️'));
        expect(defaultAudioContext.playTrack).toHaveBeenCalledWith(
            expect.objectContaining({
                id: '/api/media/stream/audio-1',
                title: 'voice.webm',
            })
        );
    });

    it('shows compact audio layout when compact is true', () => {
        render(
            <MediaRenderer
                mediaUrl="/api/media/stream/audio-1"
                mediaType="audio"
                fileName="voice.webm"
                compact={true}
            />
        );
        expect(screen.getByText('voice.webm')).toBeInTheDocument();
    });

    it('shows default file name when no fileName provided', () => {
        render(
            <MediaRenderer
                mediaUrl="/api/media/stream/file-1"
                mediaType="file"
            />
        );
        expect(screen.getByText('Файл')).toBeInTheDocument();
    });

    it('shows default audio name when no fileName provided', () => {
        render(
            <MediaRenderer
                mediaUrl="/api/media/stream/audio-1"
                mediaType="audio"
            />
        );
        expect(screen.getByText('Голосовое')).toBeInTheDocument();
    });

    it('shows playing state when currentTrack matches', () => {
        mockUseAudioPlayer.mockReturnValue({
            playTrack: jest.fn(),
            isPlaying: true,
            currentTrack: { id: '/api/media/stream/audio-1' },
        });
        render(
            <MediaRenderer
                mediaUrl="/api/media/stream/audio-1"
                mediaType="audio"
                fileName="voice.webm"
            />
        );
        expect(screen.getByText('⏸️')).toBeInTheDocument();
    });

    it('shows error state when image fails to load', () => {
        render(
            <MediaRenderer
                mediaUrl="/api/media/stream/img-1"
                mediaType="image"
                fileName="photo.jpg"
            />
        );
        const img = screen.getByAltText('photo.jpg');
        fireEvent.error(img);
        expect(screen.getByText('Не удалось загрузить медиа')).toBeInTheDocument();
    });

    it('calls onLoadError callback when media fails', () => {
        const onLoadError = jest.fn();
        render(
            <MediaRenderer
                mediaUrl="/api/media/stream/img-1"
                mediaType="image"
                fileName="photo.jpg"
                onLoadError={onLoadError}
            />
        );
        const img = screen.getByAltText('photo.jpg');
        fireEvent.error(img);
        expect(onLoadError).toHaveBeenCalled();
    });

    it('toggles fullscreen for images', () => {
        render(
            <MediaRenderer
                mediaUrl="/api/media/stream/img-1"
                mediaType="image"
                fileName="photo.jpg"
            />
        );
        const img = screen.getByAltText('photo.jpg');
        fireEvent.click(img);
        // Fullscreen overlay should appear
        expect(document.querySelector('.media-fullscreen')).toBeInTheDocument();
    });
});
