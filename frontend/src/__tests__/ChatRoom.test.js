import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import ChatRoom from '../components/Chat/ChatRoom';
import { mediaApi } from '../services/api';

// Моки
jest.mock('../components/Chat/Chat.css', () => ({}));
jest.mock('../components/Chat/MediaRenderer.css', () => ({}));
jest.mock('../components/Chat/MediaRenderer', () => (props) => (
    <div data-testid="media-renderer" data-url={props.mediaUrl} data-type={props.mediaType}>
        {props.fileName || 'media'}
    </div>
));
jest.mock('../services/api', () => ({
    mediaApi: {
        upload: jest.fn(),
        getStreamUrl: jest.fn(),
        delete: jest.fn(),
    },
}));

describe('ChatRoom', () => {
    const defaultProps = {
        messages: [],
        currentUserAuthor: 'Админ',
        currentRoom: 'general',
        canSwitch: false,
        availableRooms: [],
        roomTitles: {},
        onRoomChange: jest.fn(),
        onSendMessage: jest.fn(),
        onEditMessage: jest.fn(),
        onDeleteMessage: jest.fn(),
        onFileUpload: jest.fn(),
        onStartRecording: jest.fn(),
        onStopRecording: jest.fn(),
        isRecording: false,
        recordingTime: 0,
        error: null,
        onClearError: jest.fn(),
        uploading: false,
    };

    const sampleMessages = [
        {
            id: '1',
            author: 'Система',
            content: 'Добро пожаловать!',
            timestamp: new Date().toISOString(),
        },
        {
            id: '2',
            author: 'Админ',
            content: 'Тестовое сообщение',
            timestamp: new Date().toISOString(),
        },
    ];

    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('renders without crashing with empty messages', () => {
        render(<ChatRoom {...defaultProps} />);
        expect(screen.getByPlaceholderText('Введите сообщение...')).toBeInTheDocument();
        expect(screen.getByText('general')).toBeInTheDocument();
    });

    it('renders messages and room title', () => {
        render(
            <ChatRoom
                {...defaultProps}
                messages={sampleMessages}
                roomTitles={{ general: 'Общий чат' }}
            />
        );
        expect(screen.getByText('Общий чат')).toBeInTheDocument();
        expect(screen.getByText('Добро пожаловать!')).toBeInTheDocument();
        expect(screen.getByText('Тестовое сообщение')).toBeInTheDocument();
    });

    it('sends a text message on Enter key', async () => {
        const onSendMessage = jest.fn().mockResolvedValue();
        render(<ChatRoom {...defaultProps} onSendMessage={onSendMessage} />);
        const textarea = screen.getByPlaceholderText('Введите сообщение...');
        fireEvent.change(textarea, { target: { value: 'Новое сообщение' } });
        fireEvent.keyDown(textarea, { key: 'Enter', code: 'Enter' });
        await waitFor(() => {
            expect(onSendMessage).toHaveBeenCalledWith('Новое сообщение');
        });
    });

    it('does not send empty message', () => {
        const onSendMessage = jest.fn();
        render(<ChatRoom {...defaultProps} onSendMessage={onSendMessage} />);
        fireEvent.click(screen.getByText('➤'));
        expect(onSendMessage).not.toHaveBeenCalled();
    });

    it('displays error message and allows clearing', () => {
        const onClearError = jest.fn();
        render(
            <ChatRoom {...defaultProps} error="Ошибка загрузки" onClearError={onClearError} />
        );
        expect(screen.getByText('Ошибка загрузки')).toBeInTheDocument();
        fireEvent.click(screen.getByText('×'));
        expect(onClearError).toHaveBeenCalled();
    });

    it('shows room switcher when canSwitch is true', () => {
        const onRoomChange = jest.fn();
        render(
            <ChatRoom
                {...defaultProps}
                canSwitch={true}
                availableRooms={[
                    { id: 'general', label: 'Общий' },
                    { id: 'nutrition', label: 'Питание' },
                ]}
                onRoomChange={onRoomChange}
            />
        );
        expect(screen.getByText('Общий')).toBeInTheDocument();
        expect(screen.getByText('Питание')).toBeInTheDocument();
        fireEvent.click(screen.getByText('Питание'));
        expect(onRoomChange).toHaveBeenCalledWith('nutrition');
    });

    it('shows recording indicator when isRecording is true', () => {
        render(
            <ChatRoom
                {...defaultProps}
                isRecording={true}
                recordingTime={65}
            />
        );
        expect(screen.getByText('1:05')).toBeInTheDocument();
    });

    it('shows uploading indicator', () => {
        render(<ChatRoom {...defaultProps} uploading={true} />);
        expect(screen.getByText('Загрузка...')).toBeInTheDocument();
    });

    it('renders own messages with own class', () => {
        render(
            <ChatRoom
                {...defaultProps}
                messages={sampleMessages}
            />
        );
        const messages = document.querySelectorAll('.chat-message.own');
        expect(messages.length).toBe(1);
        expect(messages[0].textContent).toContain('Тестовое сообщение');
    });

    it('renders messages with media via MediaRenderer', () => {
        const mediaMessages = [
            {
                id: 'm1',
                author: 'Админ',
                content: 'Фото',
                mediaId: 'media-123',
                mediaType: 'image',
                fileName: 'photo.jpg',
                timestamp: new Date().toISOString(),
            },
        ];
        render(<ChatRoom {...defaultProps} messages={mediaMessages} />);
        expect(screen.getByTestId('media-renderer')).toHaveAttribute('data-url', '/api/media/stream/media-123');
        expect(screen.getByTestId('media-renderer')).toHaveAttribute('data-type', 'image');
    });

    it('shows edit form for own messages', () => {
        const ownMessages = [
            {
                id: 'own-1',
                author: 'Админ',
                content: 'Моё сообщение',
                timestamp: new Date().toISOString(),
            },
        ];
        render(<ChatRoom {...defaultProps} messages={ownMessages} />);
        fireEvent.click(screen.getByText('✏️'));
        expect(screen.getByText('Сохранить')).toBeInTheDocument();
        expect(screen.getByText('Отмена')).toBeInTheDocument();
    });

    it('handles editing and saving a message', async () => {
        const onEditMessage = jest.fn().mockResolvedValue();
        const ownMessages = [
            {
                id: 'own-1',
                author: 'Админ',
                content: 'Старый текст',
                timestamp: new Date().toISOString(),
            },
        ];
        render(
            <ChatRoom {...defaultProps} messages={ownMessages} onEditMessage={onEditMessage} />
        );
        fireEvent.click(screen.getByText('✏️'));
        // The edit textarea appears in the message list (before the input textarea)
        const textareas = screen.getAllByRole('textbox');
        const textarea = textareas[0]; // edit textarea (first in DOM)
        fireEvent.change(textarea, { target: { value: 'Новый текст' } });
        fireEvent.click(screen.getByText('Сохранить'));
        await waitFor(() => {
            expect(onEditMessage).toHaveBeenCalledWith('own-1', 'Новый текст');
        });
    });

    it('handles delete message', async () => {
        const onDeleteMessage = jest.fn().mockResolvedValue();
        const ownMessages = [
            {
                id: 'own-1',
                author: 'Админ',
                content: 'Удалить меня',
                timestamp: new Date().toISOString(),
            },
        ];
        render(
            <ChatRoom {...defaultProps} messages={ownMessages} onDeleteMessage={onDeleteMessage} />
        );
        fireEvent.click(screen.getByText('🗑️'));
        await waitFor(() => {
            expect(onDeleteMessage).toHaveBeenCalledWith('own-1');
        });
    });

    it('does not show edit/delete for other users messages', () => {
        render(<ChatRoom {...defaultProps} messages={sampleMessages} />);
        // "Добро пожаловать!" is from Система, not own
        const editBtns = screen.queryAllByText('✏️');
        const deleteBtns = screen.queryAllByText('🗑️');
        expect(editBtns.length).toBe(1); // only own message has edit
        expect(deleteBtns.length).toBe(1); // only own message has delete
    });
});
