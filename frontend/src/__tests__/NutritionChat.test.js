import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import NutritionChat from '../components/Chat/NutritionChat';
import { useChat } from '../components/Chat/useChat';

// Моки
jest.mock('../components/Chat/useChat', () => ({
    useChat: jest.fn(),
}));
jest.mock('../components/Chat/ChatRoom', () => (props) => (
    <div data-testid="chat-room">
        <span data-testid="messages-count">{props.messages.length}</span>
        <span data-testid="room">{props.currentRoom}</span>
        <span data-testid="author">{props.currentUserAuthor}</span>
        <span data-testid="room-title">{props.roomTitles?.[props.currentRoom]}</span>
    </div>
));

describe('NutritionChat', () => {
    const mockUseChat = {
        messages: [],
        loading: false,
        currentRoom: 'nutrition',
        canSwitch: false,
        currentUserAuthor: 'Пользователь',
        isRecording: false,
        recordingTime: 0,
        uploading: false,
        error: null,
        sendMessage: jest.fn(),
        editMessage: jest.fn(),
        deleteMessage: jest.fn(),
        uploadFile: jest.fn(),
        startRecording: jest.fn(),
        stopRecording: jest.fn(),
        clearError: jest.fn(),
    };

    beforeEach(() => {
        jest.clearAllMocks();
        useChat.mockReturnValue(mockUseChat);
    });

    it('renders without crashing', () => {
        render(<NutritionChat />);
        expect(screen.getByTestId('chat-room')).toBeInTheDocument();
    });

    it('shows loading state when loading is true', () => {
        useChat.mockReturnValue({ ...mockUseChat, loading: true });
        render(<NutritionChat />);
        expect(screen.getByText('Загрузка чата...')).toBeInTheDocument();
    });

    it('passes correct props to ChatRoom', () => {
        render(<NutritionChat />);
        expect(screen.getByTestId('room')).toHaveTextContent('nutrition');
        expect(screen.getByTestId('room-title')).toHaveTextContent('Чат Питание');
    });

    it('calls useChat with correct config', () => {
        render(<NutritionChat />);
        expect(useChat).toHaveBeenCalledWith('nutrition', {
            storageKeyPrefix: 'nutritionChatMessages',
            canSwitchRooms: false,
            availableRooms: [],
            useApi: false,
        });
    });

    it('does not show ChatRoom when loading', () => {
        useChat.mockReturnValue({ ...mockUseChat, loading: true });
        render(<NutritionChat />);
        expect(screen.queryByTestId('chat-room')).not.toBeInTheDocument();
    });

    it('passes messages from useChat to ChatRoom', () => {
        const messages = [
            { id: '1', author: 'Система', content: 'Welcome' },
            { id: '2', author: 'Консультант', content: 'Hello' },
        ];
        useChat.mockReturnValue({ ...mockUseChat, messages });
        render(<NutritionChat />);
        expect(screen.getByTestId('messages-count')).toHaveTextContent('2');
    });

    it('renders with error state from useChat', () => {
        useChat.mockReturnValue({ ...mockUseChat, error: 'Network error' });
        render(<NutritionChat />);
        // ChatRoom should receive error prop
        expect(screen.getByTestId('chat-room')).toBeInTheDocument();
    });
});
