import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import NutritionFeed from '../components/Feed/NutritionFeed';
import { usePermissions } from '../hooks/usePermissions';
import { useAudioPlayer } from '../context/AudioPlayerContext';
import { feedApi, dashboardApi } from '../services/api';
import { getUserRoles } from '../keycloak';

jest.mock('../hooks/usePermissions');
jest.mock('../context/AudioPlayerContext');
jest.mock('../services/api', () => ({
    feedApi: {
        getNutritionFeed: jest.fn(),
        createPost: jest.fn(),
        updatePost: jest.fn(),
        deletePost: jest.fn(),
    },
    dashboardApi: {
        getMenuData: jest.fn(),
        saveMenuData: jest.fn(),
    },
}));
jest.mock('../keycloak', () => ({
    getUserRoles: jest.fn(),
}));
jest.mock('../components/Common/ContentModal', () => {
    return function MockContentModal({ isOpen, onClose, onSave }) {
        if (!isOpen) return null;
        return (
            <div data-testid="content-modal">
                <button data-testid="modal-save" onClick={() => onSave({ title: 'Test', description: 'Desc', mediaType: 'text', mediaUrl: '' })}>
                    Save
                </button>
                <button data-testid="modal-close" onClick={onClose}>Close</button>
            </div>
        );
    };
});

// Mock CSS imports
jest.mock('../components/Feed/MainFeed.css', () => {});
jest.mock('../components/Feed/NutritionFeed.css', () => {});

const mockAudioPlayer = {
    playTrack: jest.fn(),
    pauseTrack: jest.fn(),
    stopTrack: jest.fn(),
    currentTrack: null,
    isPlaying: false,
    volume: 0.7,
    playbackRate: 1.0,
    setVolume: jest.fn(),
    setPlaybackRate: jest.fn(),
    seek: jest.fn(),
    currentTime: 0,
    duration: 100,
    formatTime: (t) => `${Math.floor(t / 60)}:${String(Math.floor(t % 60)).padStart(2, '0')}`,
};

const mockPermissions = {
    level: 1,
    roles: [],
    hasLevel: jest.fn().mockReturnValue(false),
    hasRole: jest.fn().mockReturnValue(false),
    hasAnyRole: jest.fn().mockReturnValue(false),
    isConsultant: jest.fn().mockReturnValue(false),
};

describe('NutritionFeed', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        usePermissions.mockReturnValue(mockPermissions);
        useAudioPlayer.mockReturnValue(mockAudioPlayer);
        getUserRoles.mockReturnValue([]);
        feedApi.getNutritionFeed.mockResolvedValue([
            { id: 1, type: 'text', title: 'Test Post', content: 'Test content', date: '2025-01-01', author: 'Admin' },
        ]);
        dashboardApi.getMenuData.mockResolvedValue({
            nutrition: [],
            recipes: [],
            'living-kitchen': [],
        });
    });

    it('renders without crashing', async () => {
        render(<NutritionFeed />);
        await waitFor(() => {
            expect(screen.getByText('Test Post')).toBeInTheDocument();
        });
    });

    it('displays posts from API', async () => {
        render(<NutritionFeed />);
        await waitFor(() => {
            expect(screen.getByText('Test Post')).toBeInTheDocument();
            expect(screen.getByText('Test content')).toBeInTheDocument();
        });
    });

    it('shows error message when feed API fails', async () => {
        feedApi.getNutritionFeed.mockRejectedValue(new Error('API error'));
        render(<NutritionFeed />);

        await waitFor(() => {
            expect(screen.getByText('Не удалось загрузить ленту питания')).toBeInTheDocument();
        });
    });

    it('falls back to demo posts on API error', async () => {
        feedApi.getNutritionFeed.mockRejectedValue(new Error('API error'));
        render(<NutritionFeed />);

        await waitFor(() => {
            expect(screen.getByText(/Основы здорового питания/)).toBeInTheDocument();
        });
    });

    it('shows add post button for nutrition consultant', async () => {
        usePermissions.mockReturnValue({
            ...mockPermissions,
            hasLevel: jest.fn((l) => l <= 6),
            hasRole: jest.fn((r) => r === 'CONSULTANT_PITANIE'),
            hasAnyRole: jest.fn(() => false),
            isConsultant: jest.fn(() => true),
        });

        render(<NutritionFeed />);

        await waitFor(() => {
            expect(screen.getByText(/Добавить запись/)).toBeInTheDocument();
        });
    });

    it('hides add post button for non-consultant', async () => {
        usePermissions.mockReturnValue({
            ...mockPermissions,
            hasRole: jest.fn(() => false),
            isConsultant: jest.fn(() => false),
        });

        render(<NutritionFeed />);

        await waitFor(() => {
            expect(screen.queryByText(/Добавить запись/)).not.toBeInTheDocument();
        });
    });

    it('shows edit and delete buttons for consultant', async () => {
        usePermissions.mockReturnValue({
            ...mockPermissions,
            hasLevel: jest.fn((l) => l <= 6),
            hasRole: jest.fn((r) => r === 'CONSULTANT_PITANIE'),
            hasAnyRole: jest.fn(() => false),
            isConsultant: jest.fn(() => true),
        });

        render(<NutritionFeed />);

        await waitFor(() => {
            expect(screen.getByText('✏️')).toBeInTheDocument();
            expect(screen.getByText('🗑️')).toBeInTheDocument();
        });
    });

    it('handles audio post play button', async () => {
        feedApi.getNutritionFeed.mockResolvedValue([
            { id: 2, type: 'audio', title: 'Audio Post', content: 'Audio content', url: 'http://test.com/audio.mp3', date: '2025-01-01', author: 'Admin' },
        ]);

        render(<NutritionFeed />);

        await waitFor(() => {
            expect(screen.getByText(/Слушать аудио/)).toBeInTheDocument();
        });

        fireEvent.click(screen.getByText(/Слушать аудио/));

        expect(mockAudioPlayer.playTrack).toHaveBeenCalledWith({
            id: 2,
            title: 'Audio Post',
            url: 'http://test.com/audio.mp3',
        });
    });

    it('renders video post with video element', async () => {
        feedApi.getNutritionFeed.mockResolvedValue([
            { id: 3, type: 'video', title: 'Video Post', content: 'Video content', url: 'http://test.com/video.mp4', date: '2025-01-01', author: 'Admin' },
        ]);

        render(<NutritionFeed />);

        await waitFor(() => {
            expect(screen.getByText('Video Post')).toBeInTheDocument();
        });

        // Video element should be present
        const videoElement = document.querySelector('video');
        expect(videoElement).toBeInTheDocument();
    });

    it('shows nutrition menu sidebar', async () => {
        render(<NutritionFeed />);

        await waitFor(() => {
            expect(screen.getByText('Питание')).toBeInTheDocument();
            expect(screen.getByText('Рецепты')).toBeInTheDocument();
            expect(screen.getByText('Живая кухня')).toBeInTheDocument();
        });
    });

    it('opens delete confirmation modal on delete click', async () => {
        usePermissions.mockReturnValue({
            ...mockPermissions,
            hasLevel: jest.fn((l) => l <= 6),
            hasRole: jest.fn((r) => r === 'CONSULTANT_PITANIE'),
            hasAnyRole: jest.fn(() => false),
            isConsultant: jest.fn(() => true),
        });

        render(<NutritionFeed />);

        await waitFor(() => {
            expect(screen.getByText('🗑️')).toBeInTheDocument();
        });

        fireEvent.click(screen.getByText('🗑️'));

        await waitFor(() => {
            expect(screen.getByText('Подтверждение удаления')).toBeInTheDocument();
            expect(screen.getByText(/Вы действительно хотите удалить публикацию/)).toBeInTheDocument();
        });
    });

    it('shows error message component when error is set', async () => {
        feedApi.getNutritionFeed.mockRejectedValue(new Error('fail'));

        render(<NutritionFeed />);

        await waitFor(() => {
            const errorEl = screen.getByText('Не удалось загрузить ленту питания');
            expect(errorEl).toBeInTheDocument();
        });

        // Click close button on error
        const closeButton = screen.getByText('×');
        fireEvent.click(closeButton);

        // Error should be gone
        expect(screen.queryByText('Не удалось загрузить ленту питания')).not.toBeInTheDocument();
    });
});
