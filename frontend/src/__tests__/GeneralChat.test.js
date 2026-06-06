import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import GeneralChat from '../components/Chat/GeneralChat';
import { chatApi } from '../services/api';
import useWebSocket from '../hooks/useWebSocket';
import { getUsername } from '../keycloak';

// Моки
jest.mock('../services/api', () => ({
    chatApi: {
        getMessages: jest.fn(),
        sendMessage: jest.fn(),
    },
}));

jest.mock('../hooks/useWebSocket', () => jest.fn());

jest.mock('../keycloak', () => ({
    getUsername: jest.fn(),
}));

// Мок для класса CSS (пропускаем импорт стилей)
jest.mock('../components/Chat/GeneralChat.css', () => ({}));

describe('GeneralChat', () => {
    const mockMessages = [
        { id: '1', author: 'user1', content: 'Hello', createdAt: '2025-01-01T12:00:00Z' },
        { id: '2', author: 'user2', content: 'Hi!', createdAt: '2025-01-01T12:01:00Z' },
    ];

    beforeEach(() => {
        jest.clearAllMocks();
        getUsername.mockReturnValue('testuser');
        useWebSocket.mockReturnValue({
            isConnected: true,
            send: jest.fn(),
            error: null,
        });
    });

    it('renders chat header and connection status', () => {
        chatApi.getMessages.mockResolvedValue({ data: [] });
        render(<GeneralChat />);
        expect(screen.getByText('Общий чат')).toBeInTheDocument();
        expect(screen.getByText('🟢 Connected')).toBeInTheDocument();
    });

    it('loads and displays messages', async () => {
        chatApi.getMessages.mockResolvedValue({ data: mockMessages });
        render(<GeneralChat />);

        await waitFor(() => {
            expect(screen.getByText('Hello')).toBeInTheDocument();
            expect(screen.getByText('Hi!')).toBeInTheDocument();
        });
    });

    it('sends a message and adds it to the list', async () => {
        chatApi.getMessages.mockResolvedValue({ data: [] });
        chatApi.sendMessage.mockResolvedValue({ id: '3' });
        const sendMock = jest.fn();
        useWebSocket.mockReturnValue({
            isConnected: true,
            send: sendMock,
            error: null,
        });

        render(<GeneralChat />);

        const input = screen.getByPlaceholderText('Type a message...');
        fireEvent.change(input, { target: { value: 'Test message' } });
        fireEvent.click(screen.getByText('Send'));

        await waitFor(() => {
            expect(chatApi.sendMessage).toHaveBeenCalledWith('general', {
                content: 'Test message',
                userId: 'testuser',
            });
            expect(sendMock).toHaveBeenCalledWith({
                id: '3',
                content: 'Test message',
                author: 'testuser',
                createdAt: expect.any(String),
            });
            expect(input.value).toBe('');
        });
    });

    it('displays error when API fails', async () => {
        chatApi.getMessages.mockRejectedValue(new Error('Network error'));
        render(<GeneralChat />);

        await waitFor(() => {
            expect(screen.getByText('Не удалось загрузить сообщения')).toBeInTheDocument();
        });
    });

    it('displays WebSocket error', () => {
        chatApi.getMessages.mockResolvedValue({ data: [] });
        useWebSocket.mockReturnValue({
            isConnected: true,
            send: jest.fn(),
            error: 'Connection lost',
        });

        render(<GeneralChat />);

        expect(screen.getByText(/WebSocket error: Connection lost/)).toBeInTheDocument();
    });

    it('shows disconnected status when WebSocket is disconnected', () => {
        chatApi.getMessages.mockResolvedValue({ data: [] });
        useWebSocket.mockReturnValue({
            isConnected: false,
            send: jest.fn(),
            error: null,
        });

        render(<GeneralChat />);

        expect(screen.getByText('🔴 Disconnected')).toBeInTheDocument();
    });
});
