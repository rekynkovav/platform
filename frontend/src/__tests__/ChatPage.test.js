import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import ChatPage from '../features/chat/ChatPage';

// Mock all dependencies
jest.mock('../hooks/usePermissions', () => ({
  usePermissions: jest.fn(),
}));

jest.mock('../features/chat/hooks/useChatRoom', () => jest.fn());
jest.mock('../features/chat/hooks/useChatMedia', () => jest.fn());
jest.mock('../features/chat/config', () => ({
  CHAT_CONFIG: {
    general: {
      rooms: [
        { slug: 'user1', label: 'Основной', minLevel: 1 },
        { slug: 'user2', label: 'Продвинутый', minLevel: 3 },
      ],
      defaultRoom: 'user1',
      showSwitcher: true,
    },
    nutrition: {
      rooms: [{ slug: 'nutrition', label: 'Чат Питание', minLevel: 1 }],
      defaultRoom: 'nutrition',
      showSwitcher: false,
    },
  },
  ROOM_TITLES: {
    user1: 'Общий чат',
    user2: 'Чат продвинутых',
    nutrition: 'Чат Питание',
  },
}));

// Mock sub-components
jest.mock('../features/chat/ChatHeader', () => {
  return function MockChatHeader({ slug, isConnected }) {
    return (
      <div data-testid="chat-header">
        <span data-testid="header-slug">{slug}</span>
        <span data-testid="header-connected">{String(isConnected)}</span>
      </div>
    );
  };
});

jest.mock('../features/chat/ChatMessages', () => {
  return function MockChatMessages({ messages, username, loading, onEdit, onDelete }) {
    return (
      <div data-testid="chat-messages">
        <span data-testid="messages-count">{messages.length}</span>
        <span data-testid="messages-username">{username}</span>
        <span data-testid="messages-loading">{String(loading)}</span>
        {messages.map((msg) => (
          <div key={msg.id} data-testid={`msg-${msg.id}`}>
            {msg.content}
            <button data-testid={`edit-${msg.id}`} onClick={() => onEdit(msg)}>Edit</button>
            <button data-testid={`delete-${msg.id}`} onClick={() => onDelete(msg.id)}>Delete</button>
          </div>
        ))}
      </div>
    );
  };
});

jest.mock('../features/chat/ChatInput', () => {
  return function MockChatInput({ isConnected, onSend, isRecording, uploading }) {
    return (
      <div data-testid="chat-input">
        <span data-testid="input-connected">{String(isConnected)}</span>
        <button data-testid="input-send" onClick={() => onSend('test msg', null)}>Send</button>
      </div>
    );
  };
});

jest.mock('../components/Chat/Chat.css', () => ({}));

const useChatRoom = require('../features/chat/hooks/useChatRoom');
const useChatMedia = require('../features/chat/hooks/useChatMedia');
const { usePermissions } = require('../hooks/usePermissions');

const createPermissions = (level) => ({
  hasLevel: (n) => level >= n,
  level,
});

describe('ChatPage', () => {
  const defaultChatRoomReturn = {
    messages: [],
    loading: false,
    error: null,
    isConnected: true,
    username: 'testuser',
    sendMessage: jest.fn(),
    editMessage: jest.fn(),
    deleteMessage: jest.fn(),
    clearError: jest.fn(),
  };

  const defaultChatMediaReturn = {
    uploadFile: jest.fn(),
    isRecording: false,
    recordingTime: 0,
    uploading: false,
    startRecording: jest.fn(),
    stopRecording: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
    useChatRoom.mockReturnValue({ ...defaultChatRoomReturn });
    useChatMedia.mockReturnValue({ ...defaultChatMediaReturn });
    usePermissions.mockReturnValue(createPermissions(1));
  });

  // --- Rendering ---

  it('renders chat-container', () => {
    const { container } = render(<ChatPage configKey="general" />);
    expect(container.querySelector('.chat-container')).toBeInTheDocument();
  });

  it('renders ChatHeader, ChatMessages, and ChatInput', () => {
    render(<ChatPage configKey="general" />);
    expect(screen.getByTestId('chat-header')).toBeInTheDocument();
    expect(screen.getByTestId('chat-messages')).toBeInTheDocument();
    expect(screen.getByTestId('chat-input')).toBeInTheDocument();
  });

  // --- Room switcher ---

  it('shows room switcher when user has access to multiple rooms', () => {
    usePermissions.mockReturnValue(createPermissions(5)); // can access user1 (1) and user2 (3)
    render(<ChatPage configKey="general" />);
    expect(screen.getByText('Основной')).toBeInTheDocument();
    expect(screen.getByText('Продвинутый')).toBeInTheDocument();
  });

  it('does NOT show room switcher when user has access to only one room', () => {
    usePermissions.mockReturnValue(createPermissions(1)); // only user1
    render(<ChatPage configKey="general" />);
    expect(screen.queryByText('Основной')).not.toBeInTheDocument();
    expect(screen.queryByText('Продвинутый')).not.toBeInTheDocument();
  });

  it('does NOT show room switcher when config has showSwitcher=false', () => {
    usePermissions.mockReturnValue(createPermissions(5));
    render(<ChatPage configKey="nutrition" />);
    expect(screen.queryByText('Чат Питание')).not.toBeInTheDocument();
  });

  it('switches active room when room button clicked', () => {
    usePermissions.mockReturnValue(createPermissions(5));
    render(<ChatPage configKey="general" />);
    fireEvent.click(screen.getByText('Продвинутый'));
    expect(screen.getByTestId('header-slug')).toHaveTextContent('user2');
  });

  it('does not re-render when clicking the already active room', () => {
    usePermissions.mockReturnValue(createPermissions(5));
    render(<ChatPage configKey="general" />);
    // Default is user1
    expect(screen.getByTestId('header-slug')).toHaveTextContent('user1');
    fireEvent.click(screen.getByText('Основной'));
    // Still user1
    expect(screen.getByTestId('header-slug')).toHaveTextContent('user1');
  });

  // --- Error bar ---

  it('shows error bar when useChatRoom returns an error', () => {
    useChatRoom.mockReturnValue({
      ...defaultChatRoomReturn,
      error: 'WebSocket failed',
    });
    render(<ChatPage configKey="general" />);
    expect(screen.getByText('WebSocket failed')).toBeInTheDocument();
  });

  it('clears error when close button clicked', () => {
    useChatRoom.mockReturnValue({
      ...defaultChatRoomReturn,
      error: 'Some error',
    });
    render(<ChatPage configKey="general" />);
    fireEvent.click(screen.getByText('×'));
    expect(defaultChatRoomReturn.clearError).toHaveBeenCalled();
  });

  it('does NOT show error bar when no error', () => {
    useChatRoom.mockReturnValue({
      ...defaultChatRoomReturn,
      error: null,
    });
    render(<ChatPage configKey="general" />);
    expect(screen.queryByText('⚠️')).not.toBeInTheDocument();
  });

  // --- Props pass-through ---

  it('passes correct props to ChatHeader', () => {
    useChatRoom.mockReturnValue({
      ...defaultChatRoomReturn,
      isConnected: false,
    });
    render(<ChatPage configKey="general" />);
    expect(screen.getByTestId('header-slug')).toHaveTextContent('user1');
    expect(screen.getByTestId('header-connected')).toHaveTextContent('false');
  });

  it('passes messages and username to ChatMessages', () => {
    useChatRoom.mockReturnValue({
      ...defaultChatRoomReturn,
      messages: [{ id: 1, content: 'Hi' }],
      username: 'john',
    });
    render(<ChatPage configKey="general" />);
    expect(screen.getByTestId('messages-count')).toHaveTextContent('1');
    expect(screen.getByTestId('messages-username')).toHaveTextContent('john');
  });

  it('passes loading state to ChatMessages', () => {
    useChatRoom.mockReturnValue({
      ...defaultChatRoomReturn,
      loading: true,
    });
    render(<ChatPage configKey="general" />);
    expect(screen.getByTestId('messages-loading')).toHaveTextContent('true');
  });

  // --- Nutrition config ---

  it('uses nutrition room slug when configKey is nutrition', () => {
    render(<ChatPage configKey="nutrition" />);
    expect(useChatRoom).toHaveBeenCalledWith('nutrition');
    expect(screen.getByTestId('header-slug')).toHaveTextContent('nutrition');
  });

  // --- Edit modal ---

  it('opens edit modal when onEdit is triggered from ChatMessages', () => {
    useChatRoom.mockReturnValue({
      ...defaultChatRoomReturn,
      messages: [{ id: 1, content: 'Edit me', author: 'testuser', timestamp: '2025-01-01' }],
    });
    render(<ChatPage configKey="general" />);
    fireEvent.click(screen.getByTestId('edit-1'));
    expect(screen.getByText('Редактировать сообщение')).toBeInTheDocument();
  });

  it('closes edit modal when cancel clicked', () => {
    useChatRoom.mockReturnValue({
      ...defaultChatRoomReturn,
      messages: [{ id: 1, content: 'Edit me', author: 'testuser', timestamp: '2025-01-01' }],
    });
    render(<ChatPage configKey="general" />);
    fireEvent.click(screen.getByTestId('edit-1'));
    expect(screen.getByText('Редактировать сообщение')).toBeInTheDocument();
    fireEvent.click(screen.getByText('Отмена'));
    expect(screen.queryByText('Редактировать сообщение')).not.toBeInTheDocument();
  });

  it('closes edit modal when overlay clicked', () => {
    useChatRoom.mockReturnValue({
      ...defaultChatRoomReturn,
      messages: [{ id: 1, content: 'Edit me', author: 'testuser', timestamp: '2025-01-01' }],
    });
    render(<ChatPage configKey="general" />);
    fireEvent.click(screen.getByTestId('edit-1'));
    const overlay = screen.getByText('Редактировать сообщение').closest('.modal-overlay');
    fireEvent.click(overlay);
    expect(screen.queryByText('Редактировать сообщение')).not.toBeInTheDocument();
  });
});
