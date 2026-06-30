// Tests for context/live/LiveSessionContext.tsx
// Covers: LiveSessionProvider, useLiveSession, helper hooks, reducer

import React from 'react';
import { render, screen } from '@testing-library/react';
import { renderHook, act } from '@testing-library/react';
import '@testing-library/jest-dom';

// ── Mocks ──────────────────────────────────────────────────────────────────────

const mockWsSubscribe = jest.fn().mockReturnValue(jest.fn());
const mockWsDisconnect = jest.fn();
const mockWsConnect = jest.fn().mockResolvedValue(true);

jest.mock('../services/live', () => ({
  liveWebSocket: {
    subscribe: (...args: any[]) => mockWsSubscribe(...args),
    disconnect: (...args: any[]) => mockWsDisconnect(...args),
    connect: (...args: any[]) => mockWsConnect(...args),
  },
  liveApi: {
    getBroadcast: jest.fn().mockResolvedValue({
      id: 'bc-1',
      title: 'Test',
      status: 'live',
      createdBy: 'user-1',
      viewerCount: 10,
      maxViewers: 20,
      streamKey: 'key',
      ingestUrl: 'rtmp://test',
      playbackUrl: 'http://test.m3u8',
      streamType: 'video',
      createdAt: new Date().toISOString(),
    }),
    getChatMessages: jest.fn().mockResolvedValue([]),
    sendChatMessage: jest.fn().mockResolvedValue({
      id: 'msg-1',
      broadcastId: 'bc-1',
      userId: 'user-1',
      username: 'test',
      userRole: 'USER_1',
      content: 'Hi',
      type: 'message',
      timestamp: new Date().toISOString(),
    }),
    sendReaction: jest.fn().mockResolvedValue(undefined),
    startBroadcast: jest.fn().mockResolvedValue({
      broadcast: {
        id: 'bc-new',
        title: 'New',
        status: 'preparing',
        createdBy: 'user-1',
        viewerCount: 0,
        maxViewers: 0,
        streamKey: 'new-key',
        ingestUrl: 'rtmp://test',
        playbackUrl: 'http://test.m3u8',
        streamType: 'video',
        createdAt: new Date().toISOString(),
      },
      streamKey: 'new-key',
      ingestUrl: 'rtmp://test',
      playbackUrl: 'http://test.m3u8',
    }),
    stopBroadcast: jest.fn().mockResolvedValue(undefined),
    updateBroadcast: jest.fn().mockResolvedValue(undefined),
  },
  mediaService: {},
}));

jest.mock('../keycloak', () => ({
  getUserRoles: jest.fn().mockReturnValue(['USER_1']),
  getToken: jest.fn().mockReturnValue('mock-token'),
  getUserId: jest.fn().mockReturnValue('user-1'),
  getUsername: jest.fn().mockReturnValue('testuser'),
}));

jest.mock('../constants/roles', () => ({
  ROLES: {
    SUPER_ADMIN: 'SUPER_ADMIN',
    VYACHESLAV: 'VYACHESLAV',
    MODERATOR_ADMIN: 'MODERATOR_ADMIN',
    DEVELOPER: 'DEVELOPER',
    CONSULTANT_PITANIE: 'CONSULTANT_PITANIE',
    CONSULTANT_SPORT: 'CONSULTANT_SPORT',
  },
  ROLE_LEVELS: {
    SUPER_ADMIN: 8,
    VYACHESLAV: 7,
    MODERATOR_ADMIN: 6,
    DEVELOPER: 5,
    CONSULTANT_PITANIE: 4,
    CONSULTANT_SPORT: 4,
    USER_1: 2,
  },
}));

// ── Imports after mocks ────────────────────────────────────────────────────────

import {
  LiveSessionProvider,
  useLiveSession,
  useBroadcast,
  useChatMessages,
  usePlayerState,
  useIsAdmin,
  useIsViewer,
  useConnectionStatus,
} from '../context/live/LiveSessionContext';

// ── Test wrapper ───────────────────────────────────────────────────────────────

function wrapper({ children }: { children: React.ReactNode }) {
  return <LiveSessionProvider>{children}</LiveSessionProvider>;
}

// ── Tests ──────────────────────────────────────────────────────────────────────

describe('LiveSessionContext', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  // ── useLiveSession ─────────────────────────────────────────────────────────

  describe('useLiveSession', () => {
    it('throws when used outside provider', () => {
      const spy = jest.spyOn(console, 'error').mockImplementation(() => {});

      expect(() => {
        renderHook(() => useLiveSession());
      }).toThrow('useLiveSession must be used within a LiveSessionProvider');

      spy.mockRestore();
    });

    it('provides initial state', () => {
      const { result } = renderHook(() => useLiveSession(), { wrapper });

      expect(result.current.state.broadcast).toBeNull();
      expect(result.current.state.wsConnected).toBe(false);
      expect(result.current.state.chatMessages).toEqual([]);
      expect(result.current.state.isAdmin).toBe(false);
      expect(result.current.state.isViewer).toBe(false);
      expect(result.current.state.isModerator).toBe(false);
    });

    it('provides action functions', () => {
      const { result } = renderHook(() => useLiveSession(), { wrapper });

      expect(typeof result.current.connectToBroadcast).toBe('function');
      expect(typeof result.current.disconnectFromBroadcast).toBe('function');
      expect(typeof result.current.sendChatMessage).toBe('function');
      expect(typeof result.current.sendReaction).toBe('function');
      expect(typeof result.current.startBroadcast).toBe('function');
      expect(typeof result.current.stopBroadcast).toBe('function');
      expect(typeof result.current.updateBroadcastStatus).toBe('function');
      expect(typeof result.current.toggleChatPause).toBe('function');
      expect(typeof result.current.markChatAsRead).toBe('function');
      expect(typeof result.current.dispatch).toBe('function');
    });

    it('connectToBroadcast loads broadcast and chat messages', async () => {
      const { result } = renderHook(() => useLiveSession(), { wrapper });

      await act(async () => {
        await result.current.connectToBroadcast('bc-1');
      });

      expect(result.current.state.broadcast).toBeTruthy();
      expect(result.current.state.broadcast?.id).toBe('bc-1');
      expect(result.current.state.wsConnected).toBe(true);
      expect(result.current.state.wsReconnecting).toBe(false);
    });

    it('sendChatMessage dispatches ADD_CHAT_MESSAGE', async () => {
      const { result } = renderHook(() => useLiveSession(), { wrapper });

      await act(async () => {
        await result.current.connectToBroadcast('bc-1');
      });

      await act(async () => {
        await result.current.sendChatMessage('Hello!');
      });

      expect(result.current.state.chatMessages).toHaveLength(1);
      expect(result.current.state.chatMessages[0].content).toBe('Hi');
      expect(result.current.state.chatMessages[0].isOwn).toBe(true);
    });

    it('sendChatMessage does nothing without broadcast', async () => {
      const { result } = renderHook(() => useLiveSession(), { wrapper });

      await act(async () => {
        await result.current.sendChatMessage('Hello!');
      });

      expect(result.current.state.chatMessages).toHaveLength(0);
    });

    it('sendReaction dispatches ADD_REACTION', async () => {
      const { result } = renderHook(() => useLiveSession(), { wrapper });

      await act(async () => {
        await result.current.connectToBroadcast('bc-1');
      });

      await act(async () => {
        await result.current.sendReaction('👍');
      });

      expect(result.current.state.reactions['👍']).toBe(1);
    });

    it('toggleChatPause toggles pause state', () => {
      const { result } = renderHook(() => useLiveSession(), { wrapper });

      expect(result.current.state.isChatPaused).toBe(false);

      act(() => {
        result.current.toggleChatPause();
      });

      expect(result.current.state.isChatPaused).toBe(true);

      act(() => {
        result.current.toggleChatPause();
      });

      expect(result.current.state.isChatPaused).toBe(false);
    });

    it('markChatAsRead resets paused state', () => {
      const { result } = renderHook(() => useLiveSession(), { wrapper });

      act(() => {
        result.current.toggleChatPause();
      });
      expect(result.current.state.isChatPaused).toBe(true);

      act(() => {
        result.current.markChatAsRead();
      });

      expect(result.current.state.isChatPaused).toBe(false);
    });
  });

  // ── Helper hooks ───────────────────────────────────────────────────────────

  describe('useBroadcast', () => {
    it('returns broadcast from state', () => {
      const { result } = renderHook(() => useBroadcast(), { wrapper });
      expect(result.current).toBeNull();
    });
  });

  describe('useChatMessages', () => {
    it('returns chat messages from state', () => {
      const { result } = renderHook(() => useChatMessages(), { wrapper });
      expect(result.current).toEqual([]);
    });
  });

  describe('usePlayerState', () => {
    it('returns playerState and setPlayerState', () => {
      const { result } = renderHook(() => usePlayerState(), { wrapper });

      expect(result.current.playerState).toBeDefined();
      expect(result.current.playerState.isPlaying).toBe(false);
      expect(typeof result.current.setPlayerState).toBe('function');
    });

    it('setPlayerState updates player state', () => {
      const { result } = renderHook(() => usePlayerState(), { wrapper });

      act(() => {
        result.current.setPlayerState({ volume: 0.5 });
      });

      expect(result.current.playerState.volume).toBe(0.5);
    });
  });

  describe('useIsAdmin', () => {
    it('returns false for regular user', () => {
      const { result } = renderHook(() => useIsAdmin(), { wrapper });
      expect(result.current).toBe(false);
    });
  });

  describe('useIsViewer', () => {
    it('returns false without broadcast', () => {
      const { result } = renderHook(() => useIsViewer(), { wrapper });
      expect(result.current).toBe(false);
    });
  });

  describe('useConnectionStatus', () => {
    it('returns wsConnected and wsReconnecting', () => {
      const { result } = renderHook(() => useConnectionStatus(), { wrapper });
      expect(result.current.wsConnected).toBe(false);
      expect(result.current.wsReconnecting).toBe(false);
    });
  });

  // ── Reducer edge cases ─────────────────────────────────────────────────────

  describe('reducer', () => {
    it('SET_STATUS changes broadcast status', async () => {
      const { result } = renderHook(() => useLiveSession(), { wrapper });

      await act(async () => {
        await result.current.connectToBroadcast('bc-1');
      });

      act(() => {
        result.current.dispatch({ type: 'SET_STATUS', payload: 'ended' });
      });

      expect(result.current.state.broadcast?.status).toBe('ended');
      expect(result.current.state.playerState.isPlaying).toBe(false);
    });

    it('SET_STATUS with live updates isPlaying', async () => {
      const { result } = renderHook(() => useLiveSession(), { wrapper });

      await act(async () => {
        await result.current.connectToBroadcast('bc-1');
      });

      act(() => {
        result.current.dispatch({ type: 'SET_STATUS', payload: 'live' });
      });

      expect(result.current.state.playerState.isPlaying).toBe(true);
    });

    it('SET_STATUS ignores if no broadcast', () => {
      const { result } = renderHook(() => useLiveSession(), { wrapper });

      act(() => {
        result.current.dispatch({ type: 'SET_STATUS', payload: 'live' });
      });

      expect(result.current.state.broadcast).toBeNull();
    });

    it('SET_VIEWER_COUNT updates viewer count', async () => {
      const { result } = renderHook(() => useLiveSession(), { wrapper });

      await act(async () => {
        await result.current.connectToBroadcast('bc-1');
      });

      act(() => {
        result.current.dispatch({ type: 'SET_VIEWER_COUNT', payload: 50 });
      });

      expect(result.current.state.broadcast?.viewerCount).toBe(50);
    });

    it('SET_VIEWER_COUNT updates maxViewers', async () => {
      const { result } = renderHook(() => useLiveSession(), { wrapper });

      await act(async () => {
        await result.current.connectToBroadcast('bc-1');
      });

      act(() => {
        result.current.dispatch({ type: 'SET_VIEWER_COUNT', payload: 100 });
      });

      expect(result.current.state.broadcast?.maxViewers).toBe(100);
    });

    it('ADD_CHAT_MESSAGE increments unread count', async () => {
      const { result } = renderHook(() => useLiveSession(), { wrapper });

      await act(async () => {
        await result.current.connectToBroadcast('bc-1');
      });

      act(() => {
        result.current.dispatch({
          type: 'ADD_CHAT_MESSAGE',
          payload: {
            id: 'msg-x',
            broadcastId: 'bc-1',
            userId: 'u2',
            username: 'Other',
            userRole: 'USER',
            content: 'Hey',
            type: 'message',
            timestamp: new Date().toISOString(),
            isOwn: false,
          },
        });
      });

      expect(result.current.state.unreadCount).toBe(1);
    });

    it('CLEAR_CHAT resets messages', async () => {
      const { result } = renderHook(() => useLiveSession(), { wrapper });

      await act(async () => {
        await result.current.connectToBroadcast('bc-1');
      });

      act(() => {
        result.current.dispatch({ type: 'CLEAR_CHAT' });
      });

      expect(result.current.state.chatMessages).toEqual([]);
      expect(result.current.state.unreadCount).toBe(0);
    });

    it('SET_PLAYER_STATE updates player state', () => {
      const { result } = renderHook(() => useLiveSession(), { wrapper });

      act(() => {
        result.current.dispatch({
          type: 'SET_PLAYER_STATE',
          payload: { volume: 0.3, isMuted: true },
        });
      });

      expect(result.current.state.playerState.volume).toBe(0.3);
      expect(result.current.state.playerState.isMuted).toBe(true);
    });

    it('SET_WS_CONNECTION updates connection state', () => {
      const { result } = renderHook(() => useLiveSession(), { wrapper });

      act(() => {
        result.current.dispatch({ type: 'SET_WS_CONNECTION', payload: true });
      });

      expect(result.current.state.wsConnected).toBe(true);
      expect(result.current.state.wsReconnecting).toBe(false);
    });

    it('SET_RECONNECTING updates reconnecting state', () => {
      const { result } = renderHook(() => useLiveSession(), { wrapper });

      act(() => {
        result.current.dispatch({ type: 'SET_RECONNECTING', payload: true });
      });

      expect(result.current.state.wsReconnecting).toBe(true);
    });

    it('SET_CHAT_PAUSED toggles and resets unread', async () => {
      const { result } = renderHook(() => useLiveSession(), { wrapper });

      await act(async () => {
        await result.current.connectToBroadcast('bc-1');
      });

      act(() => {
        result.current.dispatch({
          type: 'ADD_CHAT_MESSAGE',
          payload: {
            id: 'msg-x',
            broadcastId: 'bc-1',
            userId: 'u2',
            username: 'Other',
            userRole: 'USER',
            content: 'Hey',
            type: 'message',
            timestamp: new Date().toISOString(),
            isOwn: false,
          },
        });
      });

      expect(result.current.state.unreadCount).toBe(1);

      act(() => {
        result.current.dispatch({ type: 'SET_CHAT_PAUSED', payload: true });
      });

      expect(result.current.state.isChatPaused).toBe(true);
      expect(result.current.state.unreadCount).toBe(0);
    });

    it('RESET_SESSION clears state but preserves roles', async () => {
      const { result } = renderHook(() => useLiveSession(), { wrapper });

      await act(async () => {
        await result.current.connectToBroadcast('bc-1');
      });

      act(() => {
        result.current.dispatch({ type: 'RESET_SESSION' });
      });

      expect(result.current.state.broadcast).toBeNull();
      expect(result.current.state.chatMessages).toEqual([]);
    });

    it('UPDATE_BROADCAST updates broadcast fields', async () => {
      const { result } = renderHook(() => useLiveSession(), { wrapper });

      await act(async () => {
        await result.current.connectToBroadcast('bc-1');
      });

      act(() => {
        result.current.dispatch({
          type: 'UPDATE_BROADCAST',
          payload: { title: 'Updated Title' },
        });
      });

      expect(result.current.state.broadcast?.title).toBe('Updated Title');
    });

    it('ADD_REACTION accumulates counts', async () => {
      const { result } = renderHook(() => useLiveSession(), { wrapper });

      await act(async () => {
        await result.current.connectToBroadcast('bc-1');
      });

      act(() => {
        result.current.dispatch({ type: 'ADD_REACTION', payload: { emoji: '👍', count: 1 } });
      });
      act(() => {
        result.current.dispatch({ type: 'ADD_REACTION', payload: { emoji: '👍', count: 1 } });
      });

      expect(result.current.state.reactions['👍']).toBe(2);
    });

    it('SET_CHAT_MESSAGES replaces messages and resets unread', () => {
      const { result } = renderHook(() => useLiveSession(), { wrapper });

      const messages = [
        { id: 'm1', broadcastId: 'bc-1', userId: 'u1', username: 'A', userRole: 'USER', content: 'Hi', type: 'message' as const, timestamp: '' },
      ];

      act(() => {
        result.current.dispatch({ type: 'SET_CHAT_MESSAGES', payload: messages });
      });

      expect(result.current.state.chatMessages).toHaveLength(1);
      expect(result.current.state.unreadCount).toBe(0);
    });
  });
});
