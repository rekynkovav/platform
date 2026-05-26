// Хук для работы с live чатом
// Управление сообщениями, реакциями и модерацией

import { useState, useEffect, useCallback, useRef } from 'react';
import { LiveChatMessage } from '../../services/live/types';
import { useLiveSession } from '../../contexts/live/LiveSessionContext';

export interface ChatFilters {
  type?: LiveChatMessage['type'];
  userId?: string;
  search?: string;
  showOnlyQuestions?: boolean;
  showOnlyPinned?: boolean;
}

export interface ChatStats {
  totalMessages: number;
  messagesPerMinute: number;
  activeUsers: number;
  questionsCount: number;
  reactionsCount: number;
}

export function useLiveChat(broadcastId?: string) {
  const {
    state: liveSession,
    sendChatMessage: sendMessage,
    sendReaction,
    toggleChatPause,
    markChatAsRead
  } = useLiveSession();

  const [filters, setFilters] = useState<ChatFilters>({});
  const [filteredMessages, setFilteredMessages] = useState<LiveChatMessage[]>([]);
  const [newMessage, setNewMessage] = useState('');
  const [isSending, setIsSending] = useState(false);
  const [stats, setStats] = useState<ChatStats>({
    totalMessages: 0,
    messagesPerMinute: 0,
    activeUsers: 0,
    questionsCount: 0,
    reactionsCount: 0
  });

  const chatContainerRef = useRef<HTMLDivElement>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // Все сообщения из сессии
  const allMessages = liveSession.chatMessages.filter(
    message => !broadcastId || message.broadcastId === broadcastId
  );

  // Применение фильтров
  useEffect(() => {
    let filtered = allMessages;

    if (filters.type) {
      filtered = filtered.filter(m => m.type === filters.type);
    }

    if (filters.userId) {
      filtered = filtered.filter(m => m.userId === filters.userId);
    }

    if (filters.search) {
      const searchLower = filters.search.toLowerCase();
      filtered = filtered.filter(m =>
        m.content.toLowerCase().includes(searchLower) ||
        m.username.toLowerCase().includes(searchLower)
      );
    }

    if (filters.showOnlyQuestions) {
      filtered = filtered.filter(m => m.type === 'question');
    }

    if (filters.showOnlyPinned) {
      filtered = filtered.filter(m => m.metadata?.pinned);
    }

    setFilteredMessages(filtered);
  }, [allMessages, filters]);

  // Расчет статистики
  useEffect(() => {
    if (allMessages.length === 0) {
      setStats({
        totalMessages: 0,
        messagesPerMinute: 0,
        activeUsers: 0,
        questionsCount: 0,
        reactionsCount: 0
      });
      return;
    }

    // Уникальные пользователи
    const uniqueUsers = new Set(allMessages.map(m => m.userId));

    // Сообщения за последние 5 минут
    const fiveMinutesAgo = new Date(Date.now() - 5 * 60 * 1000);
    const recentMessages = allMessages.filter(m =>
      new Date(m.timestamp) > fiveMinutesAgo
    );

    // Подсчет по типам
    const questionsCount = allMessages.filter(m => m.type === 'question').length;
    const reactionsCount = Object.values(liveSession.reactions).reduce((a, b) => a + b, 0);

    setStats({
      totalMessages: allMessages.length,
      messagesPerMinute: recentMessages.length / 5,
      activeUsers: uniqueUsers.size,
      questionsCount,
      reactionsCount
    });
  }, [allMessages, liveSession.reactions]);

  // Автопрокрутка к новым сообщениям
  useEffect(() => {
    if (messagesEndRef.current && !liveSession.isChatPaused) {
      messagesEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [allMessages.length, liveSession.isChatPaused]);

  // Отправка сообщения
  const sendChatMessage = useCallback(async (content?: string, type: LiveChatMessage['type'] = 'message') => {
    const messageContent = content || newMessage;
    if (!messageContent.trim() || !broadcastId) return;

    setIsSending(true);
    try {
      await sendMessage(messageContent, type);
      setNewMessage('');
    } catch (error) {
      console.error('[useLiveChat] Ошибка отправки сообщения:', error);
    } finally {
      setIsSending(false);
    }
  }, [broadcastId, newMessage, sendMessage]);

  // Отправка реакции
  const sendChatReaction = useCallback(async (emoji: string) => {
    if (!broadcastId) return;

    try {
      await sendReaction(emoji);
    } catch (error) {
      console.error('[useLiveChat] Ошибка отправки реакции:', error);
    }
  }, [broadcastId, sendReaction]);

  // Отправка вопроса
  const sendQuestion = useCallback(async (question: string) => {
    await sendChatMessage(question, 'question');
  }, [sendChatMessage]);

  // Установка фильтров
  const setFilter = useCallback((newFilters: Partial<ChatFilters>) => {
    setFilters(prev => ({ ...prev, ...newFilters }));
  }, []);

  // Сброс фильтров
  const resetFilters = useCallback(() => {
    setFilters({});
  }, []);

  // Поиск по сообщениям
  const searchMessages = useCallback((searchText: string) => {
    setFilter({ search: searchText });
  }, [setFilter]);

  // Показать только вопросы
  const showQuestionsOnly = useCallback(() => {
    setFilter({ showOnlyQuestions: true });
  }, [setFilter]);

  // Показать все сообщения
  const showAllMessages = useCallback(() => {
    resetFilters();
  }, [resetFilters]);

  // Получить активных пользователей
  const getActiveUsers = useCallback(() => {
    const userMap = new Map<string, { user: LiveChatMessage; count: number }>();

    allMessages.forEach(message => {
      const existing = userMap.get(message.userId);
      if (existing) {
        existing.count++;
      } else {
        userMap.set(message.userId, {
          user: message,
          count: 1
        });
      }
    });

    return Array.from(userMap.values())
      .sort((a, b) => b.count - a.count)
      .map(item => ({
        ...item.user,
        messageCount: item.count
      }));
  }, [allMessages]);

  // Экспорт чата
  const exportChat = useCallback(() => {
    const exportData = {
      broadcastId,
      exportDate: new Date().toISOString(),
      totalMessages: allMessages.length,
      messages: allMessages
    };

    const blob = new Blob([JSON.stringify(exportData, null, 2)], {
      type: 'application/json'
    });

    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `chat-export-${broadcastId || 'all'}-${Date.now()}.json`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }, [broadcastId, allMessages]);

  // Очистка чата (для админов)
  const clearChat = useCallback(() => {
    if (!liveSession.isAdmin && !liveSession.isModerator) {
      console.warn('[useLiveChat] Недостаточно прав для очистки чата');
      return;
    }

    // В реальной системе здесь был бы API вызов
    console.log('[useLiveChat] Чат очищен (мок)');
    // В мок-системе мы не можем очистить глобальное состояние
    // В реальной системе: await api.clearChat(broadcastId);
  }, [liveSession.isAdmin, liveSession.isModerator]);

  // Закрепление сообщения (для модераторов)
  const pinMessage = useCallback((messageId: string) => {
    if (!liveSession.isAdmin && !liveSession.isModerator) {
      console.warn('[useLiveChat] Недостаточно прав для закрепления сообщения');
      return;
    }

    console.log(`[useLiveChat] Сообщение ${messageId} закреплено (мок)`);
    // В реальной системе: await api.pinMessage(messageId, broadcastId);
  }, [liveSession.isAdmin, liveSession.isModerator]);

  // Удаление сообщения (для модераторов)
  const deleteMessage = useCallback((messageId: string) => {
    if (!liveSession.isAdmin && !liveSession.isModerator) {
      console.warn('[useLiveChat] Недостаточно прав для удаления сообщения');
      return;
    }

    console.log(`[useLiveChat] Сообщение ${messageId} удалено (мок)`);
    // В реальной системе: await api.deleteMessage(messageId, broadcastId);
  }, [liveSession.isAdmin, liveSession.isModerator]);

  // Получение сообщений пользователя
  const getUserMessages = useCallback((userId: string) => {
    return allMessages.filter(m => m.userId === userId);
  }, [allMessages]);

  // Проверка, является ли сообщение "своим"
  const isOwnMessage = useCallback((message: LiveChatMessage) => {
    return message.isOwn || message.userId === liveSession.broadcast?.createdBy;
  }, [liveSession.broadcast?.createdBy]);

  return {
    // Данные
    messages: filteredMessages,
    allMessages,
    newMessage,
    setNewMessage,
    filters,
    filteredMessages,
    stats,

    // Состояние
    isSending,
    isChatPaused: liveSession.isChatPaused,
    unreadCount: liveSession.unreadCount,
    hasUnread: liveSession.unreadCount > 0,
    isAdmin: liveSession.isAdmin,
    isModerator: liveSession.isModerator,

    // Действия
    sendChatMessage,
    sendChatReaction,
    sendQuestion,
    toggleChatPause,
    markChatAsRead,
    setFilter,
    resetFilters,
    searchMessages,
    showQuestionsOnly,
    showAllMessages,
    getActiveUsers,
    exportChat,
    clearChat,
    pinMessage,
    deleteMessage,
    getUserMessages,
    isOwnMessage,

    // Refs для UI
    chatContainerRef,
    messagesEndRef,

    // Утилиты
    canSend: !!broadcastId && !isSending,
    canModerate: liveSession.isAdmin || liveSession.isModerator,
    hasMessages: allMessages.length > 0,
    activeUsersCount: stats.activeUsers
  };
}

// Хук для работы с реакциями
export function useLiveReactions(broadcastId?: string) {
  const { state: liveSession, sendReaction } = useLiveSession();

  const [popularReactions, setPopularReactions] = useState<Array<{ emoji: string; count: number }>>([]);

  // Обновление популярных реакций
  useEffect(() => {
    const reactionsArray = Object.entries(liveSession.reactions)
      .map(([emoji, count]) => ({ emoji, count }))
      .sort((a, b) => b.count - a.count)
      .slice(0, 10); // Топ-10

    setPopularReactions(reactionsArray);
  }, [liveSession.reactions]);

  // Отправка реакции
  const addReaction = useCallback(async (emoji: string) => {
    if (!broadcastId) return;

    try {
      await sendReaction(emoji);
    } catch (error) {
      console.error('[useLiveReactions] Ошибка отправки реакции:', error);
    }
  }, [broadcastId, sendReaction]);

  // Получение количества для конкретной реакции
  const getReactionCount = useCallback((emoji: string) => {
    return liveSession.reactions[emoji] || 0;
  }, [liveSession.reactions]);

  // Проверка, отправлял ли пользователь реакцию
  const hasReacted = useCallback((emoji: string) => {
    // В мок-системе всегда false
    // В реальной системе здесь была бы проверка по userId
    return false;
  }, []);

  // Сброс всех реакций (для админов)
  const resetReactions = useCallback(() => {
    if (!liveSession.isAdmin && !liveSession.isModerator) {
      console.warn('[useLiveReactions] Недостаточно прав для сброса реакций');
      return;
    }

    console.log('[useLiveReactions] Реакции сброшены (мок)');
    // В реальной системе: await api.resetReactions(broadcastId);
  }, [liveSession.isAdmin, liveSession.isModerator]);

  return {
    // Данные
    reactions: liveSession.reactions,
    popularReactions,
    totalReactions: Object.values(liveSession.reactions).reduce((a, b) => a + b, 0),

    // Действия
    addReaction,
    getReactionCount,
    hasReacted,
    resetReactions,

    // Утилиты
    hasReactions: Object.keys(liveSession.reactions).length > 0,
    mostPopularReaction: popularReactions[0],
    canReset: liveSession.isAdmin || liveSession.isModerator
  };
}

// Хук для работы с вопросами
export function useLiveQuestions(broadcastId?: string) {
  const { messages, isOwnMessage } = useLiveChat(broadcastId);

  const questions = messages.filter(m => m.type === 'question');
  const unansweredQuestions = questions.filter(q => !q.metadata?.answered);
  const answeredQuestions = questions.filter(q => q.metadata?.answered);

  // Пометить вопрос как отвеченный
  const markAsAnswered = useCallback((questionId: string) => {
    console.log(`[useLiveQuestions] Вопрос ${questionId} помечен как отвеченный (мок)`);
    // В реальной системе: await api.markQuestionAsAnswered(questionId, broadcastId);
  }, []);

  // Ответить на вопрос
  const answerQuestion = useCallback(async (questionId: string, answer: string) => {
    console.log(`[useLiveQuestions] Ответ на вопрос ${questionId}: ${answer} (мок)`);
    // В реальной системе:
    // 1. await api.answerQuestion(questionId, answer, broadcastId);
    // 2. Отправить ответ в чат
  }, []);

  // Получить вопросы пользователя
  const getUserQuestions = useCallback((userId: string) => {
    return questions.filter(q => q.userId === userId);
  }, [questions]);

  return {
    // Данные
    questions,
    unansweredQuestions,
    answeredQuestions,

    // Статистика
    totalQuestions: questions.length,
    unansweredCount: unansweredQuestions.length,
    answeredCount: answeredQuestions.length,

    // Действия
    markAsAnswered,
    answerQuestion,
    getUserQuestions,

    // Утилиты
    hasQuestions: questions.length > 0,
    hasUnanswered: unansweredQuestions.length > 0,
    questionRate: questions.length / Math.max(messages.length, 1)
  };
}