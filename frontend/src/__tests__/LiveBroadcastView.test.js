import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import LiveBroadcastView from '../components/live/LiveBroadcastView';
import { broadcastApi } from '../services/api';
import useWebSocket from '../hooks/useWebSocket';
import { usePermissions } from '../hooks/usePermissions';

// Моки
jest.mock('../services/api', () => ({
    broadcastApi: {
        getActive: jest.fn(),
        start: jest.fn(),
        end: jest.fn(),
    },
}));

jest.mock('../hooks/useWebSocket', () => jest.fn());

jest.mock('../hooks/usePermissions', () => ({
    usePermissions: jest.fn(),
}));

// Мок для ConfirmModal (простой заглушечный компонент)
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

jest.mock('../components/live/LiveBroadcastView.css', () => ({}));

describe('LiveBroadcastView', () => {
    const mockBroadcasts = [
        {
            id: '1',
            title: 'Test Broadcast',
            description: 'Test description',
            viewerCount: 5,
            startedAt: '2025-06-01T12:00:00Z',
            hlsPlaylistUrl: null,
        },
        {
            id: '2',
            title: 'Second Broadcast',
            description: 'Second desc',
            viewerCount: 10,
            startedAt: '2025-06-01T13:00:00Z',
            hlsPlaylistUrl: 'http://example.com/stream.m3u8',
        },
    ];

    beforeEach(() => {
        jest.clearAllMocks();
        usePermissions.mockReturnValue({
            hasLevel: jest.fn().mockReturnValue(true), // simulate admin
        });
        useWebSocket.mockReturnValue({
            isConnected: true,
            send: jest.fn(),
            error: null,
        });
    });

    it('renders header and loading state', () => {
        broadcastApi.getActive.mockResolvedValue(mockBroadcasts);
        render(<LiveBroadcastView />);
        expect(screen.getByText('Live Broadcasts')).toBeInTheDocument();
        expect(screen.getByText(/Start Broadcast/)).toBeInTheDocument();
    });

    it('loads and displays broadcasts', async () => {
        broadcastApi.getActive.mockResolvedValue(mockBroadcasts);
        render(<LiveBroadcastView />);

        await waitFor(() => {
            expect(screen.getByText('Test Broadcast')).toBeInTheDocument();
            expect(screen.getByText('Second Broadcast')).toBeInTheDocument();
            expect(screen.getByText('👥 5 viewers')).toBeInTheDocument();
            expect(screen.getByText('👥 10 viewers')).toBeInTheDocument();
        });
    });

    it('shows placeholder when no HLS URL', async () => {
        broadcastApi.getActive.mockResolvedValue(mockBroadcasts);
        render(<LiveBroadcastView />);

        await waitFor(() => {
            const placeholders = screen.getAllByText('📺 Live');
            expect(placeholders.length).toBe(1); // only first broadcast has no URL
        });
    });

    it('shows video element when HLS URL exists', async () => {
        broadcastApi.getActive.mockResolvedValue(mockBroadcasts);
        render(<LiveBroadcastView />);

        await waitFor(() => {
            const videos = document.querySelectorAll('video');
            expect(videos.length).toBe(1);
            expect(videos[0].src).toContain('example.com/stream.m3u8');
        });
    });

    it('calls startBroadcast when Start button is clicked', async () => {
        // prompt is not available in jsdom, we mock it
        global.prompt = jest.fn().mockReturnValue('New Broadcast');
        global.alert = jest.fn(); // prevent alert during test

        broadcastApi.getActive.mockResolvedValue([]);
        broadcastApi.start.mockResolvedValue({
            id: '3',
            title: 'New Broadcast',
            description: '',
            viewerCount: 0,
            startedAt: new Date().toISOString(),
        });

        render(<LiveBroadcastView />);

        const startBtn = screen.getByText('🎥 Start Broadcast');
        fireEvent.click(startBtn);

        await waitFor(() => {
            expect(broadcastApi.start).toHaveBeenCalledWith({
                title: 'New Broadcast',
                description: '',
                streamType: 'video',
            });
        });
    });

    it('shows end confirmation modal and executes end', async () => {
        broadcastApi.getActive.mockResolvedValue(mockBroadcasts);
        broadcastApi.end.mockResolvedValue({});

        render(<LiveBroadcastView />);

        await waitFor(() => {
            expect(screen.getByText('Test Broadcast')).toBeInTheDocument();
        });

        const endButtons = screen.getAllByText('End Broadcast');
        fireEvent.click(endButtons[0]);

        // модальное окно должно появиться
        expect(screen.getByTestId('confirm-modal')).toBeInTheDocument();
        expect(screen.getByText('Завершить трансляцию')).toBeInTheDocument();

        // нажимаем "Да"
        fireEvent.click(screen.getByTestId('confirm-yes'));

        await waitFor(() => {
            expect(broadcastApi.end).toHaveBeenCalledWith('1');
            // после завершения трансляция должна исчезнуть
            expect(screen.queryByText('Test Broadcast')).not.toBeInTheDocument();
        });
    });

    it('displays WebSocket error', () => {
        broadcastApi.getActive.mockResolvedValue([]);
        useWebSocket.mockReturnValue({
            isConnected: true,
            send: jest.fn(),
            error: 'Connection failed',
        });

        render(<LiveBroadcastView />);
        expect(screen.getByText(/WebSocket error: Connection failed/)).toBeInTheDocument();
    });

    it('displays API error on load', async () => {
        broadcastApi.getActive.mockRejectedValue(new Error('Network error'));

        render(<LiveBroadcastView />);

        await waitFor(() => {
            expect(
                screen.getByText('Не удалось загрузить трансляции. Проверьте подключение к серверу.')
            ).toBeInTheDocument();
        });
    });

    it('does not show Start button for non-admin', () => {
        usePermissions.mockReturnValue({
            hasLevel: jest.fn().mockReturnValue(false),
        });
        broadcastApi.getActive.mockResolvedValue([]);

        render(<LiveBroadcastView />);
        expect(screen.queryByText('🎥 Start Broadcast')).not.toBeInTheDocument();
    });

    it('shows "No active broadcasts" when empty', async () => {
        broadcastApi.getActive.mockResolvedValue([]);
        render(<LiveBroadcastView />);

        await waitFor(() => {
            expect(screen.getByText('No active broadcasts')).toBeInTheDocument();
        });
    });
});
