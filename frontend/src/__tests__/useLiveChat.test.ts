// Tests for hooks/live/useLiveChat.ts
// Covers: useLiveChat, useLiveReactions, useLiveQuestions

import { renderHook, act } from '@testing-library/react';
import '@testing-library/jest-dom';

// ── Mock context (must use mock-prefixed variables in factory) ─────────────────

const mockSendMessage = jest.fn().mockResolvedValue(undefined);
const mockSendReaction = jest.fn().mockResolvedValue(undefined);
const mockToggleChatPause = jest.fn();
const mockMarkChatAsRead = jest.fn();
const mockUseLiveSessionImpl = jest.fn();

jest.mock('../context/live/LiveSessionContext', () => ({
  useLiveSession: (...args: any[]) => mockUseLiveSessionImpl(...args),
}));

import { useLiveChat, useLiveReactions, useLiveQuestions } from '../hooks/live/useLiveChat';
import { useLiveSession } from '../context/live/LiveSessionContext';

// ── Helper data ────────────────────────────────────────────────────────────────

const makeMessage = (overrides: Record<string, any> = {}) => ({
  id: 'msg-1',
  broadcastId: 'bc-1',
  userId: 'user-1',
  username: 'Alice',
  userRole: 'USER_1',
  content: 'Hello!',
  type: 'message' as const,
  timestamp: new Date().toISOString(),
  ...overrides,
});

const defaultSessionState = {
  broadcast: { id: 'bc-1', title: 'Test', status: 'live', createdBy: 'user-1' },
  chatMessages: [] as any[],
  reactions: {} as Record<string, number>,
  isChatPaused: false,
  unreadCount: 0,
  isAdmin: false,
  isModerator: false,
};

function setupSessionState(overrides: Record<string, any> = {}) {
  mockUseLiveSessionImpl.mockReturnValue({
    state: { ...defaultSessionState, ...overrides },
    sendChatMessage: mockSendMessage,
    sendReaction: mockSendReaction,
    toggleChatPause: mockToggleChatPause,
    markChatAsRead: mockMarkChatAsRead,
  });
}

// ── useLiveChat ────────────────────────────────────────────────────────────────

describe('useLiveChat', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    setupSessionState({ chatMessages: [] });
  });

  it('returns initial state with empty messages', () => {
    const { result } = renderHook(() => useLiveChat('bc-1'));

    expect(result.current.messages).toEqual([]);
    expect(result.current.allMessages).toEqual([]);
    expect(result.current.isSending).toBe(false);
    expect(result.current.stats.totalMessages).toBe(0);
  });

  it('returns action functions', () => {
    const { result } = renderHook(() => useLiveChat('bc-1'));

    expect(typeof result.current.sendChatMessage).toBe('function');
    expect(typeof result.current.sendChatReaction).toBe('function');
    expect(typeof result.current.sendQuestion).toBe('function');
    expect(typeof result.current.setFilter).toBe('function');
    expect(typeof result.current.resetFilters).toBe('function');
    expect(typeof result.current.searchMessages).toBe('function');
    expect(typeof result.current.showQuestionsOnly).toBe('function');
    expect(typeof result.current.showAllMessages).toBe('function');
    expect(typeof result.current.getActiveUsers).toBe('function');
    expect(typeof result.current.exportChat).toBe('function');
    expect(typeof result.current.clearChat).toBe('function');
    expect(typeof result.current.pinMessage).toBe('function');
    expect(typeof result.current.deleteMessage).toBe('function');
    expect(typeof result.current.getUserMessages).toBe('function');
    expect(typeof result.current.isOwnMessage).toBe('function');
  });

  it('returns refs', () => {
    const { result } = renderHook(() => useLiveChat('bc-1'));
    expect(result.current.chatContainerRef).toBeDefined();
    expect(result.current.messagesEndRef).toBeDefined();
  });

  it('filters messages by broadcastId', () => {
    const messages = [
      makeMessage({ broadcastId: 'bc-1', content: 'msg for bc-1' }),
      makeMessage({ id: 'msg-2', broadcastId: 'bc-2', content: 'msg for bc-2' }),
    ];

    setupSessionState({ chatMessages: messages });

    const { result } = renderHook(() => useLiveChat('bc-1'));
    expect(result.current.allMessages).toHaveLength(1);
    expect(result.current.allMessages[0].content).toBe('msg for bc-1');
  });

  it('includes all messages when broadcastId is undefined', () => {
    const messages = [
      makeMessage({ broadcastId: 'bc-1' }),
      makeMessage({ id: 'msg-2', broadcastId: 'bc-2' }),
    ];

    setupSessionState({ chatMessages: messages });

    const { result } = renderHook(() => useLiveChat());
    expect(result.current.allMessages).toHaveLength(2);
  });

  it('filters by type', () => {
    const messages = [
      makeMessage({ type: 'message' }),
      makeMessage({ id: 'q-1', type: 'question' }),
    ];

    setupSessionState({ chatMessages: messages });

    const { result } = renderHook(() => useLiveChat('bc-1'));

    act(() => {
      result.current.setFilter({ type: 'question' });
    });

    expect(result.current.filteredMessages).toHaveLength(1);
    expect(result.current.filteredMessages[0].type).toBe('question');
  });

  it('filters by userId', () => {
    const messages = [
      makeMessage({ userId: 'user-1' }),
      makeMessage({ id: 'msg-2', userId: 'user-2' }),
    ];

    setupSessionState({ chatMessages: messages });

    const { result } = renderHook(() => useLiveChat('bc-1'));

    act(() => {
      result.current.setFilter({ userId: 'user-1' });
    });

    expect(result.current.filteredMessages).toHaveLength(1);
  });

  it('filters by search text', () => {
    const messages = [
      makeMessage({ content: 'Hello world' }),
      makeMessage({ id: 'msg-2', content: 'Goodbye world' }),
    ];

    setupSessionState({ chatMessages: messages });

    const { result } = renderHook(() => useLiveChat('bc-1'));

    act(() => {
      result.current.setFilter({ search: 'hello' });
    });

    expect(result.current.filteredMessages).toHaveLength(1);
    expect(result.current.filteredMessages[0].content).toBe('Hello world');
  });

  it('showQuestionsOnly filters to questions', () => {
    const messages = [
      makeMessage({ type: 'message' }),
      makeMessage({ id: 'q-1', type: 'question' }),
    ];

    setupSessionState({ chatMessages: messages });

    const { result } = renderHook(() => useLiveChat('bc-1'));

    act(() => {
      result.current.showQuestionsOnly();
    });

    expect(result.current.filteredMessages).toHaveLength(1);
    expect(result.current.filteredMessages[0].type).toBe('question');
  });

  it('resetFilters clears all filters', () => {
    const messages = [
      makeMessage({ type: 'message' }),
      makeMessage({ id: 'q-1', type: 'question' }),
    ];

    setupSessionState({ chatMessages: messages });

    const { result } = renderHook(() => useLiveChat('bc-1'));

    act(() => {
      result.current.setFilter({ type: 'question' });
    });
    expect(result.current.filteredMessages).toHaveLength(1);

    act(() => {
      result.current.resetFilters();
    });
    expect(result.current.filteredMessages).toHaveLength(2);
  });

  it('sendChatMessage calls context sendChatMessage', async () => {
    const { result } = renderHook(() => useLiveChat('bc-1'));

    await act(async () => {
      await result.current.sendChatMessage('Hi!');
    });

    expect(mockSendMessage).toHaveBeenCalledWith('Hi!', 'message');
  });

  it('sendChatMessage does not send empty content', async () => {
    const { result } = renderHook(() => useLiveChat('bc-1'));

    await act(async () => {
      await result.current.sendChatMessage('  ');
    });

    expect(mockSendMessage).not.toHaveBeenCalled();
  });

  it('sendChatReaction calls context sendReaction', async () => {
    const { result } = renderHook(() => useLiveChat('bc-1'));

    await act(async () => {
      await result.current.sendChatReaction('👍');
    });

    expect(mockSendReaction).toHaveBeenCalledWith('👍');
  });

  it('sendQuestion sends message with type question', async () => {
    const { result } = renderHook(() => useLiveChat('bc-1'));

    await act(async () => {
      await result.current.sendQuestion('What is this?');
    });

    expect(mockSendMessage).toHaveBeenCalledWith('What is this?', 'question');
  });

  it('getActiveUsers returns sorted user list', () => {
    const messages = [
      makeMessage({ userId: 'u1', username: 'Alice' }),
      makeMessage({ id: 'm2', userId: 'u1', username: 'Alice' }),
      makeMessage({ id: 'm3', userId: 'u2', username: 'Bob' }),
    ];

    setupSessionState({ chatMessages: messages });

    const { result } = renderHook(() => useLiveChat('bc-1'));

    const users = result.current.getActiveUsers();
    expect(users).toHaveLength(2);
    expect(users[0].userId).toBe('u1');
    expect(users[0].messageCount).toBe(2);
  });

  it('getUserMessages filters by userId', () => {
    const messages = [
      makeMessage({ userId: 'u1' }),
      makeMessage({ id: 'm2', userId: 'u2' }),
      makeMessage({ id: 'm3', userId: 'u1' }),
    ];

    setupSessionState({ chatMessages: messages });

    const { result } = renderHook(() => useLiveChat('bc-1'));

    const userMessages = result.current.getUserMessages('u1');
    expect(userMessages).toHaveLength(2);
  });

  it('canSend is true when broadcastId exists and not sending', () => {
    const { result } = renderHook(() => useLiveChat('bc-1'));
    expect(result.current.canSend).toBe(true);
  });

  it('canSend is false when no broadcastId', () => {
    const { result } = renderHook(() => useLiveChat());
    expect(result.current.canSend).toBe(false);
  });

  it('computes stats correctly', () => {
    const messages = [
      makeMessage({ userId: 'u1', type: 'message' }),
      makeMessage({ id: 'm2', userId: 'u2', type: 'question' }),
      makeMessage({ id: 'm3', userId: 'u1', type: 'message' }),
    ];

    setupSessionState({
      chatMessages: messages,
      reactions: { '👍': 5, '❤️': 3 },
    });

    const { result } = renderHook(() => useLiveChat('bc-1'));

    expect(result.current.stats.totalMessages).toBe(3);
    expect(result.current.stats.activeUsers).toBe(2);
    expect(result.current.stats.questionsCount).toBe(1);
    expect(result.current.stats.reactionsCount).toBe(8);
  });

  it('clearChat / pinMessage / deleteMessage are no-ops for non-admin', () => {
    setupSessionState({ isAdmin: false, isModerator: false });

    const { result } = renderHook(() => useLiveChat('bc-1'));

    expect(() => result.current.clearChat()).not.toThrow();
    expect(() => result.current.pinMessage('msg-1')).not.toThrow();
    expect(() => result.current.deleteMessage('msg-1')).not.toThrow();
  });

  it('isOwnMessage checks isOwn flag', () => {
    const { result } = renderHook(() => useLiveChat('bc-1'));

    expect(result.current.isOwnMessage(makeMessage({ isOwn: true }))).toBe(true);
    expect(result.current.isOwnMessage(makeMessage({ isOwn: false }))).toBe(false);
  });
});

// ── useLiveReactions ───────────────────────────────────────────────────────────

describe('useLiveReactions', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    setupSessionState({ reactions: {} });
  });

  it('returns initial state with empty reactions', () => {
    const { result } = renderHook(() => useLiveReactions('bc-1'));

    expect(result.current.reactions).toEqual({});
    expect(result.current.popularReactions).toEqual([]);
    expect(result.current.totalReactions).toBe(0);
    expect(result.current.hasReactions).toBe(false);
    expect(result.current.mostPopularReaction).toBeUndefined();
  });

  it('returns action functions', () => {
    const { result } = renderHook(() => useLiveReactions('bc-1'));

    expect(typeof result.current.addReaction).toBe('function');
    expect(typeof result.current.getReactionCount).toBe('function');
    expect(typeof result.current.hasReacted).toBe('function');
    expect(typeof result.current.resetReactions).toBe('function');
  });

  it('computes totalReactions', () => {
    setupSessionState({ reactions: { '👍': 5, '❤️': 3 } });

    const { result } = renderHook(() => useLiveReactions('bc-1'));
    expect(result.current.totalReactions).toBe(8);
  });

  it('computes popularReactions sorted', () => {
    setupSessionState({
      reactions: { '❤️': 10, '👍': 5, '🔥': 15 },
    });

    const { result } = renderHook(() => useLiveReactions('bc-1'));
    expect(result.current.popularReactions[0].emoji).toBe('🔥');
    expect(result.current.popularReactions[0].count).toBe(15);
  });

  it('addReaction calls context sendReaction', async () => {
    const { result } = renderHook(() => useLiveReactions('bc-1'));

    await act(async () => {
      await result.current.addReaction('👍');
    });

    expect(mockSendReaction).toHaveBeenCalledWith('👍');
  });

  it('getReactionCount returns count for emoji', () => {
    setupSessionState({ reactions: { '👍': 5 } });

    const { result } = renderHook(() => useLiveReactions('bc-1'));
    expect(result.current.getReactionCount('👍')).toBe(5);
    expect(result.current.getReactionCount('❤️')).toBe(0);
  });

  it('hasReactions is true when reactions exist', () => {
    setupSessionState({ reactions: { '👍': 1 } });

    const { result } = renderHook(() => useLiveReactions('bc-1'));
    expect(result.current.hasReactions).toBe(true);
  });

  it('mostPopularReaction returns top reaction', () => {
    setupSessionState({ reactions: { '👍': 5, '❤️': 10 } });

    const { result } = renderHook(() => useLiveReactions('bc-1'));
    expect(result.current.mostPopularReaction?.emoji).toBe('❤️');
  });
});

// ── useLiveQuestions ───────────────────────────────────────────────────────────

describe('useLiveQuestions', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    setupSessionState({ chatMessages: [] });
  });

  it('returns initial empty questions', () => {
    const { result } = renderHook(() => useLiveQuestions('bc-1'));

    expect(result.current.questions).toEqual([]);
    expect(result.current.totalQuestions).toBe(0);
    expect(result.current.hasQuestions).toBe(false);
  });

  it('returns action functions', () => {
    const { result } = renderHook(() => useLiveQuestions('bc-1'));

    expect(typeof result.current.markAsAnswered).toBe('function');
    expect(typeof result.current.answerQuestion).toBe('function');
    expect(typeof result.current.getUserQuestions).toBe('function');
  });

  it('filters questions from messages', () => {
    const messages = [
      makeMessage({ type: 'message' }),
      makeMessage({ id: 'q-1', type: 'question', content: 'Q1?' }),
      makeMessage({ id: 'q-2', type: 'question', content: 'Q2?' }),
    ];

    setupSessionState({ chatMessages: messages });

    const { result } = renderHook(() => useLiveQuestions('bc-1'));

    expect(result.current.totalQuestions).toBe(2);
    expect(result.current.hasQuestions).toBe(true);
  });

  it('computes unanswered and answered counts', () => {
    const messages = [
      makeMessage({ id: 'q-1', type: 'question', metadata: {} }),
      makeMessage({ id: 'q-2', type: 'question', metadata: { answered: true } }),
    ];

    setupSessionState({ chatMessages: messages });

    const { result } = renderHook(() => useLiveQuestions('bc-1'));

    expect(result.current.unansweredCount).toBe(1);
    expect(result.current.answeredCount).toBe(1);
    expect(result.current.hasUnanswered).toBe(true);
  });

  it('getUserQuestions filters by userId', () => {
    const messages = [
      makeMessage({ id: 'q-1', type: 'question', userId: 'u1' }),
      makeMessage({ id: 'q-2', type: 'question', userId: 'u2' }),
    ];

    setupSessionState({ chatMessages: messages });

    const { result } = renderHook(() => useLiveQuestions('bc-1'));

    const u1Questions = result.current.getUserQuestions('u1');
    expect(u1Questions).toHaveLength(1);
  });

  it('questionRate computes ratio', () => {
    const messages = [
      makeMessage({ type: 'message' }),
      makeMessage({ id: 'q-1', type: 'question' }),
    ];

    setupSessionState({ chatMessages: messages });

    const { result } = renderHook(() => useLiveQuestions('bc-1'));

    expect(result.current.questionRate).toBeCloseTo(0.5, 2);
  });
});
