import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import Chat from '../components/Common/Chat';
import { chatApi } from '../services/api';
import { useWebSocket } from '../hooks/useWebSocket';

// Моки
jest.mock('../components/Common/Chat.css', () => ({}));
jest.mock('../services/api', () => ({
    chatApi: {
        getMessages: jest.fn(),
        sendMessage: jest.fn(),
        getUsers: jest.fn(),
    },
}));
jest.mock('../hooks/useWebSocket', () => ({
    useWebSocket: jest.fn(),
}));
jest.mock('react-window', () => ({
    FixedSizeList: ({ children, itemCount, ...props }) => (
        <div data-testid="virtualized-list" data-item-count={itemCount}>
            {itemCount > 0 && children({ index: 0, style: {} })}
        </div>
    ),
}));
jest.mock('react-virtualized-auto-sizer', () => ({
    AutoSizer: ({ children }) => children({ height: 400, width: 600 }),
}));

describe('Common/Chat', () => {
    const mockSend = jest.fn().mockReturnValue(true);

    beforeEach(() => {
        jest.clearAllMocks();
        useWebSocket.mockReturnValue({
            isConnected: true,
            error: null,
            send: mockSend,
        });
    });

    it('shows loading state initially', () => {
        chatApi.getMessages.mockReturnValue(new Promise(() => {})); // never resolves
        chatApi.getUsers.mockReturnValue(new Promise(() => {}));
        render(<Chat roomSlug="general" currentUser="Админ" />);
        expect(screen.getByText('Загрузка чата...')).toBeInTheDocument();
    });

    it('renders chat after loading', async () => {
        chatApi.getMessages.mockResolvedValue([]);
        chatApi.getUsers.mockResolvedValue([]);
        render(<Chat roomSlug="general" currentUser="Админ" />);
        await waitFor(() => {
            expect(screen.getByText(/💬 Чат: general/)).toBeInTheDocument();
        });
    });

    it('displays error when API fails', async () => {
        chatApi.getMessages.mockRejectedValue(new Error('Network error'));
        chatApi.getUsers.mockResolvedValue([]);
        render(<Chat roomSlug="general" currentUser="Админ" />);
        await waitFor(() => {
            expect(screen.getByText('Не удалось загрузить чат')).toBeInTheDocument();
        });
    });

    it('shows disconnected status', async () => {
        chatApi.getMessages.mockResolvedValue([]);
        chatApi.getUsers.mockResolvedValue([]);
        useWebSocket.mockReturnValue({
            isConnected: false,
            error: null,
            send: mockSend,
        });
        render(<Chat roomSlug="general" currentUser="Админ" />);
        await waitFor(() => {
            expect(screen.getByText('Отключено')).toBeInTheDocument();
        });
    });

    it('sends a message via WebSocket', async () => {
        chatApi.getMessages.mockResolvedValue([]);
        chatApi.getUsers.mockResolvedValue([]);
        render(<Chat roomSlug="general" currentUser="Админ" />);
        await waitFor(() => {
            expect(screen.getByText(/💬 Чат: general/)).toBeInTheDocument();
        });
        const textarea = screen.getByPlaceholderText('Введите сообщение...');
        fireEvent.change(textarea, { target: { value: 'Привет мир!' } });
        fireEvent.click(screen.getByText('📤'));
        expect(mockSend).toHaveBeenCalledWith(
            expect.objectContaining({
                type: 'message',
                payload: expect.objectContaining({
                    text: 'Привет мир!',
                    author: 'Админ',
                }),
            })
        );
    });

    it('does not send empty message', async () => {
        chatApi.getMessages.mockResolvedValue([]);
        chatApi.getUsers.mockResolvedValue([]);
        render(<Chat roomSlug="general" currentUser="Админ" />);
        await waitFor(() => {
            expect(screen.getByText(/💬 Чат: general/)).toBeInTheDocument();
        });
        fireEvent.click(screen.getByText('📤'));
        expect(mockSend).not.toHaveBeenCalled();
    });

    it('displays WebSocket error', async () => {
        chatApi.getMessages.mockResolvedValue([]);
        chatApi.getUsers.mockResolvedValue([]);
        useWebSocket.mockReturnValue({
            isConnected: true,
            error: 'Connection lost',
            send: mockSend,
        });
        render(<Chat roomSlug="general" currentUser="Админ" />);
        await waitFor(() => {
            expect(screen.getByText(/Connection lost/)).toBeInTheDocument();
        });
    });

    it('shows users list', async () => {
        chatApi.getMessages.mockResolvedValue([]);
        chatApi.getUsers.mockResolvedValue([
            { id: 'u1', name: 'Алексей' },
            { id: 'u2', name: 'Мария' },
        ]);
        render(<Chat roomSlug="general" currentUser="Админ" />);
        await waitFor(() => {
            expect(screen.getByText('Алексей')).toBeInTheDocument();
            expect(screen.getByText('Мария')).toBeInTheDocument();
        });
    });

    it('shows typing indicator when other users type', async () => {
        chatApi.getMessages.mockResolvedValue([]);
        chatApi.getUsers.mockResolvedValue([]);
        let capturedCallback;
        useWebSocket.mockImplementation((room, onMessage) => {
            capturedCallback = onMessage;
            return { isConnected: true, error: null, send: mockSend };
        });
        render(<Chat roomSlug="general" currentUser="Админ" />);
        await waitFor(() => {
            expect(screen.getByText(/💬 Чат: general/)).toBeInTheDocument();
        });
        // Simulate typing event via the captured callback
        act(() => {
            capturedCallback({
                type: 'typing',
                payload: { users: ['Алексей', 'Админ'] },
            });
        });
        // Алексей should appear, Админ (current user) should be filtered out
        expect(screen.getByText(/Алексей печатает/)).toBeInTheDocument();
    });

    it('renders virtualized message list', async () => {
        chatApi.getMessages.mockResolvedValue([
            { id: '1', text: 'Hello', author: 'User', timestamp: new Date().toISOString(), room: 'general' },
        ]);
        chatApi.getUsers.mockResolvedValue([]);
        render(<Chat roomSlug="general" currentUser="Админ" />);
        await waitFor(() => {
            expect(screen.getByTestId('virtualized-list')).toBeInTheDocument();
        });
    });

    it('shows connected status', async () => {
        chatApi.getMessages.mockResolvedValue([]);
        chatApi.getUsers.mockResolvedValue([]);
        render(<Chat roomSlug="general" currentUser="Админ" />);
        await waitFor(() => {
            expect(screen.getByText('Подключено')).toBeInTheDocument();
        });
    });
});
