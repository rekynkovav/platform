import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';

// ── Mock all dependencies ──────────────────────────────────────────────────────

jest.mock('../services/live', () => ({
  mediaService: {},
}));

jest.mock('../hooks/live/useLiveChat', () => ({
  useLiveChat: jest.fn(),
  useLiveReactions: jest.fn(),
  useLiveQuestions: jest.fn(),
}));

jest.mock('../context/live/LiveSessionContext', () => ({
  useLiveSession: jest.fn(),
}));

jest.mock('../utils/live/helpers', () => ({
  formatMessageTime: () => '12:00',
  getRoleColor: () => '#FFFFFF',
  getRoleText: () => 'Пользователь',
  isSystemMessage: () => false,
  isValidChatMessage: (msg: string) => !!msg && msg.trim().length > 0,
}));

// ── Imports (after mocks) ─────────────────────────────────────────────────────

import { LiveChat } from '../components/live/LiveChat';
import { useLiveChat, useLiveReactions, useLiveQuestions } from '../hooks/live/useLiveChat';
import { useLiveSession } from '../context/live/LiveSessionContext';

// ── Shared mock data ──────────────────────────────────────────────────────────

const mockSendMessage = jest.fn().mockResolvedValue(undefined);
const mockSendQuestion = jest.fn().mockResolvedValue(undefined);
const mockChatContainerRef = { current: document.createElement('div') };
const mockMessagesEndRef = { current: document.createElement('div') };

const defaultChatReturn: Record<string, any> = {
  messages: [],
  allMessages: [],
  newMessage: '',
  setNewMessage: jest.fn(),
  isSending: false,
  isChatPaused: false,
  unreadCount: 0,
  hasUnread: false,
  isAdmin: true,
  isModerator: false,
  sendChatMessage: mockSendMessage,
  sendChatReaction: jest.fn(),
  sendQuestion: mockSendQuestion,
  toggleChatPause: jest.fn(),
  markChatAsRead: jest.fn(),
  searchMessages: jest.fn(),
  showQuestionsOnly: jest.fn(),
  showAllMessages: jest.fn(),
  getActiveUsers: jest.fn().mockReturnValue([]),
  exportChat: jest.fn(),
  clearChat: jest.fn(),
  pinMessage: jest.fn(),
  deleteMessage: jest.fn(),
  getUserMessages: jest.fn(),
  isOwnMessage: jest.fn().mockReturnValue(false),
  chatContainerRef: mockChatContainerRef,
  messagesEndRef: mockMessagesEndRef,
};

const defaultReactionsReturn: Record<string, any> = {
  reactions: {},
  popularReactions: [],
  addReaction: jest.fn(),
  getReactionCount: jest.fn().mockReturnValue(0),
  hasReactions: false,
  mostPopularReaction: null,
  totalReactions: 0,
  hasReacted: jest.fn(),
  resetReactions: jest.fn(),
  canReset: false,
};

const defaultQuestionsReturn: Record<string, any> = {
  questions: [],
  unansweredQuestions: [],
  answeredQuestions: [],
  totalQuestions: 0,
  unansweredCount: 0,
  answeredCount: 0,
  answerQuestion: jest.fn(),
  markAsAnswered: jest.fn(),
  getUserQuestions: jest.fn(),
  hasQuestions: false,
  hasUnanswered: false,
  questionRate: 0,
};

const mockLiveSession = {
  state: {
    broadcast: { id: 'bc-1', title: 'Test', status: 'live', createdBy: 'user-1' },
    isAdmin: true,
    isViewer: false,
    isModerator: false,
    wsConnected: true,
    wsReconnecting: false,
    chatMessages: [],
    unreadCount: 0,
    isChatPaused: false,
    playerState: { isPlaying: true, currentTime: 0, volume: 1, quality: 'auto', isFullscreen: false, isMuted: false },
    reactions: {},
    polls: [],
    questions: [],
  },
  dispatch: jest.fn(),
  connectToBroadcast: jest.fn(),
  disconnectFromBroadcast: jest.fn(),
  sendChatMessage: jest.fn(),
  sendReaction: jest.fn(),
  startBroadcast: jest.fn(),
  stopBroadcast: jest.fn(),
  updateBroadcastStatus: jest.fn(),
  toggleChatPause: jest.fn(),
  markChatAsRead: jest.fn(),
};

// ── Helper to set mock returns ─────────────────────────────────────────────────

function setupMocks(overrides: {
  chat?: Record<string, any>;
  reactions?: Record<string, any>;
  questions?: Record<string, any>;
} = {}) {
  (useLiveChat as jest.Mock).mockReturnValue({ ...defaultChatReturn, ...overrides.chat });
  (useLiveReactions as jest.Mock).mockReturnValue({ ...defaultReactionsReturn, ...overrides.reactions });
  (useLiveQuestions as jest.Mock).mockReturnValue({ ...defaultQuestionsReturn, ...overrides.questions });
  (useLiveSession as jest.Mock).mockReturnValue(mockLiveSession);
}

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('LiveChat', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    setupMocks();
  });

  // ── 1. Renders without crashing ─────────────────────────────────────────────

  it('renders without crashing with chat header', () => {
    render(<LiveChat broadcastId="bc-1" />);
    expect(screen.getByText('Чат трансляции')).toBeInTheDocument();
  });

  // ── 2. Shows empty state when no messages ────────────────────────────────────

  it('shows empty state when there are no messages', () => {
    render(<LiveChat broadcastId="bc-1" />);
    expect(screen.getByText('Чат пуст. Будьте первым!')).toBeInTheDocument();
  });

  // ── 3. Shows message input in non-compact mode ───────────────────────────────

  it('shows message input in non-compact mode', () => {
    render(<LiveChat broadcastId="bc-1" />);
    expect(screen.getByPlaceholderText('Введите сообщение...')).toBeInTheDocument();
    expect(screen.getByText('Отправить')).toBeInTheDocument();
  });

  // ── 4. Hides message input in compact mode ───────────────────────────────────

  it('hides message input in compact mode', () => {
    render(<LiveChat broadcastId="bc-1" compact />);
    expect(screen.queryByPlaceholderText('Введите сообщение...')).not.toBeInTheDocument();
  });

  // ── 5. Renders chat messages ────────────────────────────────────────────────

  it('renders chat messages when present', () => {
    const messages = [
      {
        id: 'msg-1',
        broadcastId: 'bc-1',
        userId: 'user-1',
        username: 'Алиса',
        userRole: 'user',
        content: 'Привет всем!',
        type: 'message' as const,
        timestamp: '2026-01-15T12:00:00Z',
      },
      {
        id: 'msg-2',
        broadcastId: 'bc-1',
        userId: 'user-2',
        username: 'Борис',
        userRole: 'moderator',
        content: 'Добро пожаловать!',
        type: 'message' as const,
        timestamp: '2026-01-15T12:01:00Z',
      },
    ];

    setupMocks({ chat: { messages, allMessages: messages } });

    render(<LiveChat broadcastId="bc-1" />);

    expect(screen.getByText('Привет всем!')).toBeInTheDocument();
    expect(screen.getByText('Добро пожаловать!')).toBeInTheDocument();
    expect(screen.getByText('Алиса')).toBeInTheDocument();
    expect(screen.getByText('Борис')).toBeInTheDocument();
  });

  // ── 6. Sends message on Enter key ───────────────────────────────────────────
  it('calls sendChatMessage when Enter is pressed', async () => {
    let capturedValue = 'Тестовое сообщение';

    const mockSetNewMessage = jest.fn((val: string) => { capturedValue = val; });
    const mockSend = jest.fn().mockResolvedValue(undefined);

    (useLiveChat as jest.Mock).mockImplementation(() => ({
      ...defaultChatReturn,
      get newMessage() { return capturedValue; },
      setNewMessage: mockSetNewMessage,
      sendChatMessage: mockSend,
    }));

    render(<LiveChat broadcastId="bc-1" />);

    const input = screen.getByPlaceholderText('Введите сообщение...');
    fireEvent.change(input, { target: { value: 'Тестовое сообщение' } });
    fireEvent.keyDown(input, { key: 'Enter' });

    await waitFor(() => {
      expect(mockSend).toHaveBeenCalled();
    });
  });

  // ── 7. Shows question button when showQuestions is true ───────────────────────

  it('shows question button when showQuestions is true', () => {
    render(<LiveChat broadcastId="bc-1" showQuestions />);
    expect(screen.getByText('❓ Задать вопрос')).toBeInTheDocument();
  });

  // ── 8. Hides question button when showQuestions is false ──────────────────────

  it('hides question button when showQuestions is false', () => {
    render(<LiveChat broadcastId="bc-1" showQuestions={false} />);
    expect(screen.queryByText('❓ Задать вопрос')).not.toBeInTheDocument();
  });

  // ── 9. Renders reactions section when showReactions is true ──────────────────

  it('renders reactions section when showReactions is true', () => {
    render(<LiveChat broadcastId="bc-1" showReactions />);
    expect(screen.getByText('Реакции:')).toBeInTheDocument();
    expect(screen.getByText('👍')).toBeInTheDocument();
    expect(screen.getByText('❤️')).toBeInTheDocument();
    expect(screen.getByText('🔥')).toBeInTheDocument();
  });

  // ── 10. Hides reactions section when showReactions is false ──────────────────

  it('hides reactions section when showReactions is false', () => {
    render(<LiveChat broadcastId="bc-1" showReactions={false} />);
    expect(screen.queryByText('Реакции:')).not.toBeInTheDocument();
  });

  // ── 11. Shows unread badge when there are unread messages ────────────────────

  it('shows unread badge when there are unread messages', () => {
    setupMocks({ chat: { unreadCount: 5, hasUnread: true } });

    render(<LiveChat broadcastId="bc-1" />);
    expect(screen.getByText('5')).toBeInTheDocument();
  });

  // ── 12. Shows questions tab and statistics ───────────────────────────────────

  it('shows questions tab and statistics when questions exist', () => {
    const questions = [
      {
        id: 'q-1',
        broadcastId: 'bc-1',
        userId: 'user-3',
        username: 'Мария',
        userRole: 'user',
        content: 'Какой формат записи?',
        type: 'question' as const,
        timestamp: '2026-01-15T12:05:00Z',
      },
    ];

    setupMocks({
      chat: { messages: questions, allMessages: questions },
      questions: {
        questions,
        unansweredQuestions: questions,
        answeredQuestions: [],
        totalQuestions: 1,
        unansweredCount: 1,
        hasQuestions: true,
        hasUnanswered: true,
      },
    });

    render(<LiveChat broadcastId="bc-1" />);

    // Click on questions tab
    const questionsTabBtn = screen.getByText('❓ Вопросы');
    fireEvent.click(questionsTabBtn);

    expect(screen.getByText('Вопросы (1)')).toBeInTheDocument();
    expect(screen.getByText('Неотвеченные: 1')).toBeInTheDocument();
  });

  // ── 13. Search input is visible ─────────────────────────────────────────────

  it('displays search input in non-compact mode', () => {
    render(<LiveChat broadcastId="bc-1" />);
    expect(screen.getByPlaceholderText('Поиск в чате...')).toBeInTheDocument();
  });

  // ── 14. Export button is rendered ────────────────────────────────────────────

  it('renders export button in chat header', () => {
    render(<LiveChat broadcastId="bc-1" />);
    expect(screen.getByTitle('Экспорт чата')).toBeInTheDocument();
  });

  // ── 15. Hides clear chat for non-admin users ────────────────────────────────

  it('hides clear chat button for non-admin users', () => {
    setupMocks({ chat: { isAdmin: false, isModerator: false } });

    render(<LiveChat broadcastId="bc-1" />);
    expect(screen.queryByTitle('Очистить чат')).not.toBeInTheDocument();
  });
});
