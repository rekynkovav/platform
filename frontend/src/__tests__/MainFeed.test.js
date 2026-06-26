import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import MainFeed from '../components/Feed/MainFeed';
import { feedApi } from '../services/api';
import { usePermissions } from '../hooks/usePermissions';
import useAudioRecorder from '../hooks/useAudioRecorder';
import usePostAudioControls from '../hooks/usePostAudioControls';

jest.mock('../services/api', () => ({
    feedApi: {
        getMainFeed: jest.fn(),
        createPost: jest.fn(),
        updatePost: jest.fn(),
        deletePost: jest.fn(),
    },
}));
jest.mock('../hooks/usePermissions', () => ({ usePermissions: jest.fn() }));
jest.mock('../hooks/useAudioRecorder', () => jest.fn());
jest.mock('../hooks/usePostAudioControls', () => jest.fn());
jest.mock('../constants/fallbackPosts', () => []);
jest.mock('../components/Feed/MainFeed.css', () => ({}));
jest.mock('../components/common/ConfirmModal', () => {
    return function MockConfirmModal({ isOpen, title, message, onConfirm, onCancel }) {
        if (!isOpen) return null;
        return (
            <div data-testid="confirm-modal">
                <span>{title}</span>
                <span>{message}</span>
                <button data-testid="confirm-yes" onClick={onConfirm}>Да</button>
                <button data-testid="confirm-no" onClick={onCancel}>Нет</button>
            </div>
        );
    };
});

describe('MainFeed', () => {
    const mockPosts = [
        { id: 1, title: 'Post 1', content: 'Content 1', date: '2025-01-01', author: 'Admin', type: 'text' },
        { id: 2, title: 'Post 2', content: 'Content 2', date: '2025-01-02', author: 'Admin', type: 'text' },
    ];

    beforeEach(() => {
        jest.clearAllMocks();
        usePermissions.mockReturnValue({ hasLevel: jest.fn().mockReturnValue(true) });
        useAudioRecorder.mockReturnValue({
            isRecording: false, recordingTime: 0, audioBlobUrl: null,
            startRecording: jest.fn(), stopRecording: jest.fn(), resetRecording: jest.fn(),
        });
        usePostAudioControls.mockReturnValue({
            handlePlayAudio: jest.fn(), handleSeek: jest.fn(),
            handleVolumeChangeLocal: jest.fn(), handleSpeedChangeLocal: jest.fn(),
            getCurrentTimeForPost: jest.fn(() => 0), getDurationForPost: jest.fn(() => 100),
            isThisPlaying: jest.fn(() => false), stopTrack: jest.fn(),
            volume: 0.7, playbackRate: 1.0, formatTime: jest.fn(() => '0:00'), currentTrack: null,
        });
    });

    it('loads and displays posts', async () => {
        feedApi.getMainFeed.mockResolvedValue(mockPosts);
        render(<MainFeed />);
        await waitFor(() => {
            expect(screen.getByText('Post 1')).toBeInTheDocument();
            expect(screen.getByText('Post 2')).toBeInTheDocument();
        });
    });

    it('shows add post button for admin', async () => {
        feedApi.getMainFeed.mockResolvedValue([]);
        render(<MainFeed />);
        await waitFor(() => {
            expect(screen.getByText('➕ Добавить запись')).toBeInTheDocument();
        });
    });

    it('opens add post form when clicked', async () => {
        feedApi.getMainFeed.mockResolvedValue([]);
        render(<MainFeed />);
        await waitFor(() => {
            expect(screen.getByText('➕ Добавить запись')).toBeInTheDocument();
        });
        fireEvent.click(screen.getByText('➕ Добавить запись'));
        expect(screen.getByText('Новая запись')).toBeInTheDocument();
    });

    it('creates a post and adds it to the list', async () => {
        feedApi.getMainFeed.mockResolvedValue([]);
        feedApi.createPost.mockResolvedValue({ id: 3, title: 'New Post', content: 'New Content', date: '2025-01-03', author: 'Admin', type: 'text' });
        render(<MainFeed />);
        await waitFor(() => {
            expect(screen.getByText('➕ Добавить запись')).toBeInTheDocument();
        });
        fireEvent.click(screen.getByText('➕ Добавить запись'));
        const titleInput = screen.getByPlaceholderText('Заголовок');
        const contentTextarea = screen.getByPlaceholderText('Текст (для аудио/видео описание)');
        fireEvent.change(titleInput, { target: { value: 'New Post' } });
        fireEvent.change(contentTextarea, { target: { value: 'New Content' } });
        fireEvent.click(screen.getByText('Опубликовать'));
        await waitFor(() => {
            expect(feedApi.createPost).toHaveBeenCalled();
            expect(screen.getByText('New Post')).toBeInTheDocument();
        });
    });

    it('opens delete confirmation modal', async () => {
        feedApi.getMainFeed.mockResolvedValue(mockPosts);
        feedApi.deletePost.mockResolvedValue({});
        render(<MainFeed />);
        await waitFor(() => {
            expect(screen.getByText('Post 1')).toBeInTheDocument();
        });
        const deleteButtons = screen.getAllByTitle('Удалить запись');
        fireEvent.click(deleteButtons[0]);
        expect(screen.getByTestId('confirm-modal')).toBeInTheDocument();
        fireEvent.click(screen.getByTestId('confirm-yes'));
        await waitFor(() => {
            expect(feedApi.deletePost).toHaveBeenCalledWith(1);
            expect(screen.queryByText('Post 1')).not.toBeInTheDocument();
        });
    });

    it('shows error when load fails', async () => {
        feedApi.getMainFeed.mockRejectedValue(new Error('Network error'));
        render(<MainFeed />);
        await waitFor(() => {
            expect(screen.getByText('Не удалось загрузить ленту')).toBeInTheDocument();
        });
    });

    it('shows audio button for audio posts', async () => {
        const audioPost = { id: 3, title: 'Audio Post', content: 'Audio description', date: '2025-01-03', author: 'Admin', type: 'audio', url: 'blob:test' };
        feedApi.getMainFeed.mockResolvedValue([audioPost]);
        render(<MainFeed />);
        await waitFor(() => {
            expect(screen.getByText('▶️ Слушать аудио')).toBeInTheDocument();
        });
    });
});
