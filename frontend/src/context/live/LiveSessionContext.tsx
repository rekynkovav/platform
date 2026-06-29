// Контекст для управления состоянием live сессии
// Централизованное состояние для всех компонентов live streaming

import React, { createContext, useContext, useReducer, useEffect, useCallback } from 'react';
import {
  LiveSession,
  LiveBroadcast,
  LiveChatMessage,
  BroadcastStatus,
  WSEventType
} from '../../services/live/types';
import { liveWebSocket } from '../../services/live';
import { liveApi } from '../../services/live';
import { getUserRoles } from '../../keycloak';
import { ROLES, ROLE_LEVELS } from '../../constants/roles';

// Типы действий
type LiveSessionAction =
  | { type: 'SET_BROADCAST'; payload: LiveBroadcast | null }
  | { type: 'SET_STATUS'; payload: BroadcastStatus }
  | { type: 'SET_VIEWER_COUNT'; payload: number }
  | { type: 'ADD_CHAT_MESSAGE'; payload: LiveChatMessage }
  | { type: 'SET_CHAT_MESSAGES'; payload: LiveChatMessage[] }
  | { type: 'CLEAR_CHAT' }
  | { type: 'SET_PLAYER_STATE'; payload: Partial<LiveSession['playerState']> }
  | { type: 'SET_WS_CONNECTION'; payload: boolean }
  | { type: 'SET_RECONNECTING'; payload: boolean }
  | { type: 'SET_CHAT_PAUSED'; payload: boolean }
  | { type: 'ADD_REACTION'; payload: { emoji: string; count: number } }
  | { type: 'RESET_SESSION' }
  | { type: 'UPDATE_BROADCAST'; payload: Partial<LiveBroadcast> };

// Начальное состояние
const initialState: LiveSession = {
  broadcast: null,
  isAdmin: false,
  isViewer: false,
  isModerator: false,

  wsConnected: false,
  wsReconnecting: false,

  chatMessages: [],
  unreadCount: 0,
  isChatPaused: false,

  playerState: {
    isPlaying: false,
    currentTime: 0,
    volume: 1.0,
    quality: 'auto',
    isFullscreen: false,
    isMuted: false
  },

  reactions: {},
  polls: [],
  questions: []
};

// Редуктор
function liveSessionReducer(state: LiveSession, action: LiveSessionAction): LiveSession {
  switch (action.type) {
    case 'SET_BROADCAST':
      const broadcast = action.payload;
      // Read real roles from Keycloak JWT, not from sessionStorage
      const roles = getUserRoles();
      const SYSTEM_ROLES = ['offline_access', 'uma_authorization', 'default-roles-platform'];
      const appRoles = roles.filter((r: string) => !SYSTEM_ROLES.includes(r));
      // Find highest role by level
      let highestRoleLevel = 0;
      let highestRoleName = 'USER_0';
      for (const [roleName, roleLevel] of Object.entries(ROLE_LEVELS)) {
        if (appRoles.includes(roleName) && roleLevel > highestRoleLevel) {
          highestRoleLevel = roleLevel;
          highestRoleName = roleName;
        }
      }
      const isUserAdmin = [ROLES.SUPER_ADMIN, ROLES.VYACHESLAV, ROLES.MODERATOR_ADMIN].includes(highestRoleName as any);
      const isUserModerator = isUserAdmin || [ROLES.CONSULTANT_SPORT, ROLES.CONSULTANT_PITANIE, ROLES.DEVELOPER].includes(highestRoleName as any);

      return {
        ...state,
        broadcast,
        isAdmin: isUserAdmin,
        isViewer: !!broadcast,
        isModerator: isUserModerator,
        playerState: {
          ...state.playerState,
          isPlaying: !!broadcast && broadcast.status === 'live'
        }
      };

    case 'SET_STATUS':
      if (!state.broadcast) return state;
      return {
        ...state,
        broadcast: {
          ...state.broadcast,
          status: action.payload
        },
        playerState: {
          ...state.playerState,
          isPlaying: action.payload === 'live'
        }
      };

    case 'SET_VIEWER_COUNT':
      if (!state.broadcast) return state;
      const newViewerCount = action.payload;
      const newMaxViewers = Math.max(state.broadcast.maxViewers || 0, newViewerCount);

      return {
        ...state,
        broadcast: {
          ...state.broadcast,
          viewerCount: newViewerCount,
          maxViewers: newMaxViewers
        }
      };

    case 'ADD_CHAT_MESSAGE':
      const newMessage = action.payload;
      const isOwnMessage = newMessage.isOwn;
      const updatedMessages = [...state.chatMessages, newMessage];

      // Ограничиваем историю сообщений
      const trimmedMessages = updatedMessages.slice(-200);

      return {
        ...state,
        chatMessages: trimmedMessages,
        unreadCount: isOwnMessage || state.isChatPaused
          ? state.unreadCount
          : state.unreadCount + 1,

        // Если это вопрос, добавляем в отдельный список
        questions: newMessage.type === 'question'
          ? [...state.questions, newMessage]
          : state.questions
      };

    case 'SET_CHAT_MESSAGES':
      return {
        ...state,
        chatMessages: action.payload,
        unreadCount: 0
      };

    case 'CLEAR_CHAT':
      return {
        ...state,
        chatMessages: [],
        unreadCount: 0,
        questions: []
      };

    case 'SET_PLAYER_STATE':
      return {
        ...state,
        playerState: {
          ...state.playerState,
          ...action.payload
        }
      };

    case 'SET_WS_CONNECTION':
      return {
        ...state,
        wsConnected: action.payload,
        wsReconnecting: false
      };

    case 'SET_RECONNECTING':
      return {
        ...state,
        wsReconnecting: action.payload
      };

    case 'SET_CHAT_PAUSED':
      return {
        ...state,
        isChatPaused: action.payload,
        unreadCount: action.payload ? 0 : state.unreadCount
      };

    case 'ADD_REACTION':
      const { emoji, count } = action.payload;
      const currentCount = state.reactions[emoji] || 0;

      return {
        ...state,
        reactions: {
          ...state.reactions,
          [emoji]: currentCount + count
        }
      };

    case 'UPDATE_BROADCAST':
      if (!state.broadcast) return state;
      return {
        ...state,
        broadcast: {
          ...state.broadcast,
          ...action.payload,
          updatedAt: new Date().toISOString()
        }
      };

    case 'RESET_SESSION':
      return {
        ...initialState,
        // Сохраняем настройки пользователя
        isAdmin: state.isAdmin,
        isViewer: false,
        isModerator: state.isModerator
      };

    default:
      return state;
  }
}

// Создание контекста
const LiveSessionContext = createContext<{
  state: LiveSession;
  dispatch: React.Dispatch<LiveSessionAction>;
  connectToBroadcast: (broadcastId: string) => Promise<void>;
  disconnectFromBroadcast: () => void;
  sendChatMessage: (content: string, type?: LiveChatMessage['type']) => Promise<void>;
  sendReaction: (emoji: string) => Promise<void>;
  startBroadcast: (title: string, description?: string) => Promise<void>;
  stopBroadcast: () => Promise<void>;
  updateBroadcastStatus: (status: BroadcastStatus) => Promise<void>;
  toggleChatPause: () => void;
  markChatAsRead: () => void;
} | undefined>(undefined);

// Провайдер
export function LiveSessionProvider({ children }: { children: React.ReactNode }) {
  const [state, dispatch] = useReducer(liveSessionReducer, initialState);
  const wsSubscriptions: (() => void)[] = [];

  // Инициализация WebSocket подписок
  useEffect(() => {
    const setupWebSocketSubscriptions = () => {
      // Подписка на события WebSocket
      const subscriptions = [
        liveWebSocket.subscribe('VIEWER_COUNT_UPDATED', (data) => {
          if (state.broadcast?.id === data.broadcastId) {
            dispatch({
              type: 'SET_VIEWER_COUNT',
              payload: Math.max(0, data.viewerCount || 0)
            });
          }
        }),

        liveWebSocket.subscribe('CHAT_MESSAGE', (message: LiveChatMessage) => {
          if (state.broadcast?.id === message.broadcastId) {
            dispatch({ type: 'ADD_CHAT_MESSAGE', payload: message });
          }
        }),

        liveWebSocket.subscribe('REACTION_ADDED', (data) => {
          if (state.broadcast?.id === data.broadcastId) {
            dispatch({
              type: 'ADD_REACTION',
              payload: { emoji: data.emoji, count: data.count }
            });
          }
        }),

        liveWebSocket.subscribe('BROADCAST_STATUS_CHANGED', (broadcast: LiveBroadcast) => {
          if (state.broadcast?.id === broadcast.id) {
            dispatch({ type: 'SET_BROADCAST', payload: broadcast });
          }
        }),

        liveWebSocket.subscribe('SYSTEM_MESSAGE', (data) => {
        })
      ];

      return subscriptions;
    };

    const subscriptions = setupWebSocketSubscriptions();
    wsSubscriptions.push(...subscriptions);

    return () => {
      // Отписка при размонтировании
      subscriptions.forEach(unsubscribe => unsubscribe());
      wsSubscriptions.length = 0;
    };
  }, [state.broadcast?.id]);

  // Подключение к трансляции
  const connectToBroadcast = useCallback(async (broadcastId: string) => {
    try {
      dispatch({ type: 'SET_RECONNECTING', payload: true });

      // Загружаем данные трансляции
      const broadcast = await liveApi.getBroadcast(broadcastId);
      dispatch({ type: 'SET_BROADCAST', payload: broadcast });

      // Загружаем историю чата
      const chatMessages = await liveApi.getChatMessages(broadcastId);
      dispatch({ type: 'SET_CHAT_MESSAGES', payload: chatMessages });

      // Подключаем WebSocket
      const connected = await liveWebSocket.connect(broadcastId);
      dispatch({ type: 'SET_WS_CONNECTION', payload: connected });


    } catch (error) {
      dispatch({ type: 'SET_WS_CONNECTION', payload: false });
    } finally {
      dispatch({ type: 'SET_RECONNECTING', payload: false });
    }
  }, []);

  // Отключение от трансляции
  const disconnectFromBroadcast = useCallback(() => {
    if (state.broadcast) {

      // Отключаем WebSocket
      liveWebSocket.disconnect(state.broadcast.id);

    }

    dispatch({ type: 'RESET_SESSION' });
  }, [state.broadcast]);

  // Отправка сообщения в чат
  const sendChatMessage = useCallback(async (content: string, type: LiveChatMessage['type'] = 'message') => {
    if (!state.broadcast || !content.trim()) return;

    try {
      // Отправляем через REST API — сервер рассылает через WS другим клиентам
      // Локальный dispatch НЕ нужен — WS subscription добавит сообщение
      const message = await liveApi.sendChatMessage(state.broadcast.id, content, type);

      // Optimistic: добавляем сразу, чтобы не ждать WS broadcast
      // Помечаем как isOwn чтобы WS subscription пропустило дубликат
      dispatch({ type: 'ADD_CHAT_MESSAGE', payload: { ...message, isOwn: true } });

    } catch (error) {
    }
  }, [state.broadcast]);

  // Отправка реакции
  const sendReaction = useCallback(async (emoji: string) => {
    if (!state.broadcast) return;

    try {
      await liveApi.sendReaction(state.broadcast.id, emoji);

      // Optimistic: добавляем сразу
      dispatch({ type: 'ADD_REACTION', payload: { emoji, count: 1 } });

    } catch (error) {
    }
  }, [state.broadcast]);

  // Начало трансляции (для админов)
  const startBroadcast = useCallback(async (title: string, description?: string) => {
    try {
      const response = await liveApi.startBroadcast({
        title,
        description,
        streamType: 'video',
        settings: {
          isChatEnabled: true,
          isReactionsEnabled: true,
          isRecording: true,
          isPublic: true
        }
      });

      // Подключаемся к созданной трансляции
      await connectToBroadcast(response.broadcast.id);

      // Меняем статус на "live" после небольшой задержки (имитация подготовки)
      setTimeout(async () => {
        await liveApi.updateBroadcast(response.broadcast.id, { status: 'live' });
        dispatch({ type: 'SET_STATUS', payload: 'live' });
      }, 2000);


    } catch (error) {
      throw error;
    }
  }, [connectToBroadcast]);

  // Остановка трансляции
  const stopBroadcast = useCallback(async () => {
    if (!state.broadcast) return;

    try {
      await liveApi.stopBroadcast(state.broadcast.id);
      dispatch({ type: 'SET_STATUS', payload: 'ended' });

      // Отключаемся через 3 секунды
      setTimeout(() => {
        disconnectFromBroadcast();
      }, 3000);


    } catch (error) {
    }
  }, [state.broadcast, disconnectFromBroadcast]);

  // Обновление статуса трансляции
  const updateBroadcastStatus = useCallback(async (status: BroadcastStatus) => {
    if (!state.broadcast) return;

    try {
      await liveApi.updateBroadcast(state.broadcast.id, { status });
      dispatch({ type: 'SET_STATUS', payload: status });
    } catch (error) {
    }
  }, [state.broadcast]);

  // Пауза/возобновление чата
  const toggleChatPause = useCallback(() => {
    dispatch({ type: 'SET_CHAT_PAUSED', payload: !state.isChatPaused });
  }, [state.isChatPaused]);

  // Отметка чата как прочитанного
  const markChatAsRead = useCallback(() => {
    dispatch({ type: 'SET_CHAT_PAUSED', payload: false });
  }, []);

  // Автоматическое отключение при размонтировании
  useEffect(() => {
    return () => {
      disconnectFromBroadcast();
    };
  }, [disconnectFromBroadcast]);

  // Значение контекста
  const contextValue = {
    state,
    dispatch,
    connectToBroadcast,
    disconnectFromBroadcast,
    sendChatMessage,
    sendReaction,
    startBroadcast,
    stopBroadcast,
    updateBroadcastStatus,
    toggleChatPause,
    markChatAsRead
  };

  return (
    <LiveSessionContext.Provider value={contextValue}>
      {children}
    </LiveSessionContext.Provider>
  );
}

// Хук для использования контекста
export function useLiveSession() {
  const context = useContext(LiveSessionContext);
  if (context === undefined) {
    throw new Error('useLiveSession must be used within a LiveSessionProvider');
  }
  return context;
}

// Вспомогательные хуки
export function useBroadcast() {
  const { state } = useLiveSession();
  return state.broadcast;
}

export function useChatMessages() {
  const { state } = useLiveSession();
  return state.chatMessages;
}

export function usePlayerState() {
  const { state, dispatch } = useLiveSession();

  const setPlayerState = useCallback((updates: Partial<LiveSession['playerState']>) => {
    dispatch({ type: 'SET_PLAYER_STATE', payload: updates });
  }, [dispatch]);

  return {
    playerState: state.playerState,
    setPlayerState
  };
}

export function useIsAdmin() {
  const { state } = useLiveSession();
  return state.isAdmin;
}

export function useIsViewer() {
  const { state } = useLiveSession();
  return state.isViewer;
}

export function useConnectionStatus() {
  const { state } = useLiveSession();
  return {
    wsConnected: state.wsConnected,
    wsReconnecting: state.wsReconnecting
  };
}