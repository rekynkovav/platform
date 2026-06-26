// Компонент чата для live трансляции
// Поддержка сообщений, реакций, вопросов, модерации

import React, { useState, useRef, useEffect, useCallback } from 'react';
import { LiveChatMessage } from '../../services/live/types';
import { useLiveChat, useLiveReactions, useLiveQuestions } from '../../hooks/live/useLiveChat';
import { useLiveSession } from '../../context/live/LiveSessionContext';
import {
  formatMessageTime,
  getRoleColor,
  getRoleText,
  isSystemMessage,
  isValidChatMessage
} from '../../utils/live/helpers';

export interface LiveChatProps {
  broadcastId: string;
  maxHeight?: string;
  showReactions?: boolean;
  showQuestions?: boolean;
  showUserList?: boolean;
  compact?: boolean;
}

/**
 * LiveChat — real-time chat component for live broadcasts.
 *
 * Supports text messages, emoji reactions, Q&A questions, user list display,
 * message moderation (pause/unpause, delete), and unread message tracking.
 * Integrates with the LiveSessionContext for session state and role-based
 * features (admin/moderator controls).
 *
 * @param {LiveChatProps} props - Component properties
 * @param {string} props.broadcastId - The ID of the associated broadcast
 * @param {string} [props.maxHeight='500px'] - Maximum height of the chat container
 * @param {boolean} [props.showReactions=true] - Whether to show the reactions panel
 * @param {boolean} [props.showQuestions=true] - Whether to show the Q&A questions panel
 * @param {boolean} [props.showUserList=false] - Whether to show the online user list
 * @param {boolean} [props.compact=false] - Whether to render in compact/mini mode
 * @returns {JSX.Element} The live chat interface with messages, input, reactions, and controls
 */
export const LiveChat: React.FC<LiveChatProps> = ({
  broadcastId,
  maxHeight = '500px',
  showReactions = true,
  showQuestions = true,
  showUserList = false,
  compact = false
}) => {
  const {
    messages,
    allMessages,
    newMessage,
    setNewMessage,
    isSending,
    isChatPaused,
    unreadCount,
    hasUnread,
    isAdmin,
    isModerator,
    sendChatMessage,
    sendChatReaction,
    sendQuestion,
    toggleChatPause,
    markChatAsRead,
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
    chatContainerRef,
    messagesEndRef
  } = useLiveChat(broadcastId);

  const {
    reactions,
    popularReactions,
    addReaction,
    getReactionCount,
    hasReactions,
    mostPopularReaction
  } = useLiveReactions(broadcastId);

  const {
    questions,
    unansweredQuestions,
    answeredQuestions,
    totalQuestions,
    unansweredCount,
    answerQuestion,
    markAsAnswered
  } = useLiveQuestions(broadcastId);

  const { state: liveSession } = useLiveSession();

  const [searchQuery, setSearchQuery] = useState('');
  const [showOnlyQuestions, setShowOnlyQuestions] = useState(false);
  const [showUserInfo, setShowUserInfo] = useState<string | null>(null);
  const [selectedMessage, setSelectedMessage] = useState<string | null>(null);
  const [showReactionPicker, setShowReactionPicker] = useState(false);
  const [activeTab, setActiveTab] = useState<'chat' | 'questions' | 'users'>('chat');

  const messageInputRef = useRef<HTMLTextAreaElement>(null);
  const searchInputRef = useRef<HTMLInputElement>(null);

  // Популярные реакции для быстрого доступа
  const quickReactions = ['👍', '👏', '❤️', '🔥', '🎉', '🤔', '😂', '😮'];

  // Обработка отправки сообщения
  const handleSendMessage = useCallback(async () => {
    if (!newMessage.trim() || isSending) return;

    await sendChatMessage();
    if (messageInputRef.current) {
      messageInputRef.current.focus();
    }
  }, [newMessage, isSending, sendChatMessage]);

  // Обработка отправки вопроса
  const handleSendQuestion = useCallback(async () => {
    if (!newMessage.trim() || isSending) return;

    await sendQuestion(newMessage);
    if (messageInputRef.current) {
      messageInputRef.current.focus();
    }
  }, [newMessage, isSending, sendQuestion]);

  // Обработка нажатия Enter (с Shift для новой строки)
  const handleKeyDown = useCallback((e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
  }, [handleSendMessage]);

  // Обработка поиска
  const handleSearch = useCallback(() => {
    searchMessages(searchQuery);
  }, [searchQuery, searchMessages]);

  // Обработка очистки поиска
  const handleClearSearch = useCallback(() => {
    setSearchQuery('');
    searchMessages('');
    if (searchInputRef.current) {
      searchInputRef.current.focus();
    }
  }, [searchMessages]);

  // Обработка отправки реакции
  const handleReactionClick = useCallback(async (emoji: string) => {
    await addReaction(emoji);
    setShowReactionPicker(false);
  }, [addReaction]);

  // Обработка ответа на вопрос
  const handleAnswerQuestion = useCallback(async (questionId: string) => {
    const answer = prompt('Введите ответ на вопрос:');
    if (answer && answer.trim()) {
      await answerQuestion(questionId, answer);
    }
  }, [answerQuestion]);

  // Автопрокрутка при новых сообщениях
  useEffect(() => {
    if (messagesEndRef.current && !isChatPaused) {
      messagesEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages.length, isChatPaused, messagesEndRef]);

  // Фокус на поле ввода при загрузке
  useEffect(() => {
    if (messageInputRef.current && !compact) {
      messageInputRef.current.focus();
    }
  }, [compact]);

  // Обработка клика вне пикера реакций
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (showReactionPicker && !(e.target as Element).closest('.reaction-picker')) {
        setShowReactionPicker(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [showReactionPicker]);

  // Рендер сообщения
  const renderMessage = useCallback((message: LiveChatMessage) => {
    const isOwn = isOwnMessage(message);
    const isSystem = isSystemMessage(message);
    const roleColor = getRoleColor(message.userRole);
    const roleText = getRoleText(message.userRole);
    const isQuestion = message.type === 'question';
    const isAnswered = (message.metadata as any)?.answered;
    const isPinned = message.metadata?.pinned;
    const isSelected = selectedMessage === message.id;

    return (
      <div
        key={message.id}
        className={`chat-message ${isOwn ? 'own' : ''} ${isSystem ? 'system' : ''} ${isQuestion ? 'question' : ''} ${isSelected ? 'selected' : ''}`}
        onClick={() => setSelectedMessage(message.id)}
      >
        {isPinned && <div className="message-pinned">📌 Закреплено</div>}

        <div className="message-header">
          <div className="message-avatar">{message.avatar || '👤'}</div>
          <div className="message-user">
            <span className="message-username" style={{ color: roleColor }}>
              {message.username}
            </span>
            <span className="message-role">{roleText}</span>
            {isQuestion && (
              <span className="message-type">
                {isAnswered ? '✅ Отвечено' : '❓ Вопрос'}
              </span>
            )}
          </div>
          <div className="message-time">
            {formatMessageTime(message.timestamp)}
          </div>
        </div>

        <div className="message-content">
          {message.content}
        </div>

        {showReactions && (
          <div className="message-reactions">
            {Object.entries(reactions).map(([emoji, count]) => (
              <button
                key={emoji}
                className="reaction-button"
                onClick={() => handleReactionClick(emoji)}
                title={`${count} реакций`}
              >
                {emoji} {count}
              </button>
            ))}
          </div>
        )}

        {(isAdmin || isModerator) && (
          <div className="message-actions">
            {isQuestion && !isAnswered && (
              <button
                className="action-button"
                onClick={() => markAsAnswered(message.id)}
                title="Пометить как отвеченный"
              >
                ✅
              </button>
            )}
            {!isPinned && (
              <button
                className="action-button"
                onClick={() => pinMessage(message.id)}
                title="Закрепить сообщение"
              >
                📌
              </button>
            )}
            <button
              className="action-button"
              onClick={() => deleteMessage(message.id)}
              title="Удалить сообщение"
            >
              🗑️
            </button>
          </div>
        )}
      </div>
    );
  }, [isOwnMessage, selectedMessage, reactions, showReactions, isAdmin, isModerator, handleReactionClick, markAsAnswered, pinMessage, deleteMessage]);

  // Рендер списка пользователей
  const renderUserList = useCallback(() => {
    const activeUsers = getActiveUsers();

    return (
      <div className="user-list">
        <div className="user-list-header">
          <h3>Активные пользователи ({activeUsers.length})</h3>
        </div>
        <div className="user-list-content">
          {activeUsers.map((user, index) => (
            <div
              key={user.userId}
              className="user-item"
              onClick={() => setShowUserInfo(user.userId)}
            >
              <div className="user-avatar">{user.avatar || '👤'}</div>
              <div className="user-info">
                <div className="user-name">{user.username}</div>
                <div className="user-stats">
                  <span className="user-role">{getRoleText(user.userRole)}</span>
                  <span className="user-messages">{user.messageCount} сообщений</span>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    );
  }, [getActiveUsers]);

  // Рендер списка вопросов
  const renderQuestionsList = useCallback(() => {
    return (
      <div className="questions-list">
        <div className="questions-header">
          <h3>Вопросы ({totalQuestions})</h3>
          <div className="questions-stats">
            <span className="stat unanswered">Неотвеченные: {unansweredCount}</span>
            <span className="stat answered">Отвеченные: {answeredQuestions.length}</span>
          </div>
        </div>

        {unansweredQuestions.length > 0 && (
          <div className="questions-section">
            <h4>Неотвеченные вопросы</h4>
            {unansweredQuestions.map(question => (
              <div key={question.id} className="question-item unanswered">
                <div className="question-content">{question.content}</div>
                <div className="question-meta">
                  <span className="question-user">{question.username}</span>
                  <span className="question-time">{formatMessageTime(question.timestamp)}</span>
                </div>
                {(isAdmin || isModerator) && (
                  <button
                    className="answer-button"
                    onClick={() => handleAnswerQuestion(question.id)}
                  >
                    Ответить
                  </button>
                )}
              </div>
            ))}
          </div>
        )}

        {answeredQuestions.length > 0 && (
          <div className="questions-section">
            <h4>Отвеченные вопросы</h4>
            {answeredQuestions.map(question => (
              <div key={question.id} className="question-item answered">
                <div className="question-content">{question.content}</div>
                <div className="question-meta">
                  <span className="question-user">{question.username}</span>
                  <span className="question-time">{formatMessageTime(question.timestamp)}</span>
                </div>
                <div className="question-answer">
                  ✅ Отвечено {question.metadata?.answeredAt && formatMessageTime(question.metadata.answeredAt)}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    );
  }, [totalQuestions, unansweredCount, unansweredQuestions, answeredQuestions, isAdmin, isModerator, handleAnswerQuestion]);

  return (
    <div className={`live-chat-container ${compact ? 'compact' : ''}`}>
      {/* Заголовок и управление */}
      <div className="chat-header">
        <div className="chat-title">
          <h3>Чат трансляции</h3>
          {hasUnread && (
            <span className="unread-badge">{unreadCount}</span>
          )}
        </div>

        <div className="chat-controls">
          <button
            className={`control-button ${isChatPaused ? 'active' : ''}`}
            onClick={toggleChatPause}
            title={isChatPaused ? 'Возобновить автоскролл' : 'Приостановить автоскролл'}
          >
            {isChatPaused ? '▶️' : '⏸️'}
          </button>

          {hasUnread && !isChatPaused && (
            <button
              className="control-button"
              onClick={markChatAsRead}
              title="Отметить как прочитанное"
            >
              👁️
            </button>
          )}

          <button
            className="control-button"
            onClick={exportChat}
            title="Экспорт чата"
          >
            💾
          </button>

          {(isAdmin || isModerator) && (
            <button
              className="control-button danger"
              onClick={clearChat}
              title="Очистить чат"
            >
              🗑️
            </button>
          )}
        </div>
      </div>

      {/* Поиск и фильтры */}
      {!compact && (
        <div className="chat-filters">
          <div className="search-box">
            <input
              ref={searchInputRef}
              type="text"
              placeholder="Поиск в чате..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
            />
            {searchQuery ? (
              <button className="search-clear" onClick={handleClearSearch}>
                ✕
              </button>
            ) : (
              <button className="search-button" onClick={handleSearch}>
                🔍
              </button>
            )}
          </div>

          <div className="filter-buttons">
            <button
              className={`filter-button ${showOnlyQuestions ? 'active' : ''}`}
              onClick={() => {
                setShowOnlyQuestions(!showOnlyQuestions);
                if (!showOnlyQuestions) {
                  showQuestionsOnly();
                } else {
                  showAllMessages();
                }
              }}
            >
              ❓ Только вопросы
            </button>

            {showQuestions && (
              <button
                className="filter-button"
                onClick={() => setActiveTab(activeTab === 'questions' ? 'chat' : 'questions')}
              >
                {activeTab === 'questions' ? '💬 Чат' : '❓ Вопросы'}
              </button>
            )}

            {showUserList && (
              <button
                className="filter-button"
                onClick={() => setActiveTab(activeTab === 'users' ? 'chat' : 'users')}
              >
                {activeTab === 'users' ? '💬 Чат' : '👥 Пользователи'}
              </button>
            )}
          </div>
        </div>
      )}

      {/* Реакции */}
      {showReactions && !compact && (
        <div className="chat-reactions">
          <div className="reactions-title">Реакции:</div>
          <div className="quick-reactions">
            {quickReactions.map(emoji => (
              <button
                key={emoji}
                className="quick-reaction"
                onClick={() => handleReactionClick(emoji)}
                title={`Отправить ${emoji}`}
              >
                {emoji} {getReactionCount(emoji) > 0 && `(${getReactionCount(emoji)})`}
              </button>
            ))}
          </div>
          <button
            className="more-reactions"
            onClick={() => setShowReactionPicker(!showReactionPicker)}
          >
            {showReactionPicker ? '✕' : '😀'}
          </button>

          {showReactionPicker && (
            <div className="reaction-picker">
              <div className="reaction-picker-grid">
                {['😀', '😂', '🥰', '😎', '🤔', '😮', '👏', '👍', '❤️', '🔥', '🎉', '🤯'].map(emoji => (
                  <button
                    key={emoji}
                    className="picker-reaction"
                    onClick={() => handleReactionClick(emoji)}
                  >
                    {emoji}
                  </button>
                ))}
              </div>
            </div>
          )}

          {mostPopularReaction && (
            <div className="popular-reaction">
              Самая популярная: {mostPopularReaction.emoji} ({mostPopularReaction.count})
            </div>
          )}
        </div>
      )}

      {/* Основное содержимое */}
      <div className="chat-content">
        {activeTab === 'chat' && (
          <div
            className="messages-container"
            ref={chatContainerRef}
            style={{ maxHeight }}
            onClick={markChatAsRead}
          >
            {messages.length === 0 ? (
              <div className="no-messages">
                {searchQuery ? 'Сообщения не найдены' : 'Чат пуст. Будьте первым!'}
              </div>
            ) : (
              messages.map(renderMessage)
            )}
            <div ref={messagesEndRef} />
          </div>
        )}

        {activeTab === 'questions' && renderQuestionsList()}
        {activeTab === 'users' && renderUserList()}
      </div>

      {/* Ввод сообщения */}
      {!compact && (
        <div className="chat-input">
          <textarea
            ref={messageInputRef}
            className="message-input"
            placeholder="Введите сообщение..."
            value={newMessage}
            onChange={(e) => setNewMessage(e.target.value)}
            onKeyDown={handleKeyDown}
            disabled={isSending}
            rows={3}
          />

          <div className="input-controls">
            <div className="input-buttons">
              <button
                className="send-button"
                onClick={handleSendMessage}
                disabled={!isValidChatMessage(newMessage) || isSending}
              >
                {isSending ? 'Отправка...' : 'Отправить'}
              </button>

              {showQuestions && (
                <button
                  className="question-button"
                  onClick={handleSendQuestion}
                  disabled={!isValidChatMessage(newMessage) || isSending}
                >
                  ❓ Задать вопрос
                </button>
              )}

              <button
                className="reaction-button"
                onClick={() => setShowReactionPicker(!showReactionPicker)}
              >
                😀
              </button>
            </div>

            <div className="input-info">
              <span className="char-count">
                {newMessage.length}/1000
              </span>
              {isSending && <span className="sending-indicator">⏳</span>}
            </div>
          </div>
        </div>
      )}

      {/* Статистика (только для разработки) */}
      {process.env.NODE_ENV === 'development' && !compact && (
        <div className="chat-stats">
          <div className="stat-item">Сообщений: {allMessages.length}</div>
          <div className="stat-item">Вопросов: {totalQuestions}</div>
          <div className="stat-item">Реакций: {Object.values(reactions as Record<string, number>).reduce((a: number, b: number) => a + b, 0)}</div>
          <div className="stat-item">Пользователей: {getActiveUsers().length}</div>
        </div>
      )}
    </div>
  );
};

// Стили компонента
const styles = `
.live-chat-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #1a1a1a;
  border-radius: 8px;
  overflow: hidden;
}

.live-chat-container.compact {
  max-width: 400px;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #2a2a2a;
  border-bottom: 1px solid #333;
}

.chat-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.chat-title h3 {
  margin: 0;
  color: white;
  font-size: 16px;
}

.unread-badge {
  background: #4CAF50;
  color: white;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: bold;
}

.chat-controls {
  display: flex;
  gap: 8px;
}

.control-button {
  background: #333;
  color: white;
  border: none;
  width: 32px;
  height: 32px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 14px;
}

.control-button:hover {
  background: #444;
}

.control-button.active {
  background: #4CAF50;
}

.control-button.danger {
  background: #f44336;
}

.control-button.danger:hover {
  background: #d32f2f;
}

.chat-filters {
  padding: 12px 16px;
  background: #2a2a2a;
  border-bottom: 1px solid #333;
}

.search-box {
  position: relative;
  margin-bottom: 10px;
}

.search-box input {
  width: 100%;
  padding: 8px 40px 8px 12px;
  background: #333;
  border: 1px solid #444;
  border-radius: 4px;
  color: white;
  font-size: 14px;
}

.search-box input:focus {
  outline: none;
  border-color: #4CAF50;
}

.search-button, .search-clear {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  color: #888;
  cursor: pointer;
  font-size: 16px;
}

.search-button:hover, .search-clear:hover {
  color: white;
}

.filter-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.filter-button {
  background: #333;
  color: white;
  border: none;
  padding: 6px 12px;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
}

.filter-button:hover {
  background: #444;
}

.filter-button.active {
  background: #4CAF50;
}

.chat-reactions {
  position: relative;
  padding: 12px 16px;
  background: #2a2a2a;
  border-bottom: 1px solid #333;
  display: flex;
  align-items: center;
  gap: 10px;
}

.reactions-title {
  color: #888;
  font-size: 14px;
}

.quick-reactions {
  display: flex;
  gap: 4px;
}

.quick-reaction {
  background: #333;
  color: white;
  border: none;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
}

.quick-reaction:hover {
  background: #444;
}

.more-reactions {
  background: #333;
  color: white;
  border: none;
  width: 32px;
  height: 32px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
}

.more-reactions:hover {
  background: #444;
}

.reaction-picker {
  position: absolute;
  bottom: 100%;
  right: 0;
  background: #2a2a2a;
  border: 1px solid #333;
  border-radius: 8px;
  padding: 8px;
  z-index: 100;
  box-shadow: 0 4px 12px rgba(0,0,0,0.3);
}

.reaction-picker-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 4px;
}

.picker-reaction {
  background: #333;
  color: white;
  border: none;
  width: 36px;
  height: 36px;
  border-radius: 4px;
  font-size: 18px;
  cursor: pointer;
}

.picker-reaction:hover {
  background: #444;
}

.popular-reaction {
  margin-left: auto;
  color: #4CAF50;
  font-size: 12px;
}

.chat-content {
  flex: 1;
  overflow: hidden;
}

.messages-container {
  height: 100%;
  overflow-y: auto;
  padding: 16px;
  scroll-behavior: smooth;
}

.messages-container::-webkit-scrollbar {
  width: 6px;
}

.messages-container::-webkit-scrollbar-track {
  background: #1a1a1a;
}

.messages-container::-webkit-scrollbar-thumb {
  background: #444;
  border-radius: 3px;
}

.messages-container::-webkit-scrollbar-thumb:hover {
  background: #555;
}

.no-messages {
  text-align: center;
  color: #888;
  padding: 40px 20px;
  font-size: 14px;
}

.chat-message {
  margin-bottom: 16px;
  padding: 12px;
  background: #2a2a2a;
  border-radius: 8px;
  border-left: 4px solid #333;
}

.chat-message.own {
  background: #1e3a1e;
  border-left-color: #4CAF50;
}

.chat-message.system {
  background: #2a1e3a;
  border-left-color: #9c27b0;
}

.chat-message.question {
  background: #3a2a1e;
  border-left-color: #ff9800;
}

.chat-message.selected {
  background: #2a3a3a;
  border-left-color: #00bcd4;
}

.message-pinned {
  background: #ff9800;
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 11px;
  margin-bottom: 8px;
  display: inline-block;
}

.message-header {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
  gap: 8px;
}

.message-avatar {
  width: 32px;
  height: 32px;
  background: #333;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}

.message-user {
  flex: 1;
}

.message-username {
  font-weight: bold;
  font-size: 14px;
  display: block;
}

.message-role, .message-type {
  font-size: 11px;
  color: #888;
  margin-right: 8px;
}

.message-time {
  font-size: 11px;
  color: #888;
}

.message-content {
  color: white;
  font-size: 14px;
  line-height: 1.4;
  word-break: break-word;
}

.message-reactions {
  display: flex;
  gap: 4px;
  margin-top: 8px;
  flex-wrap: wrap;
}

.reaction-button {
  background: #333;
  color: white;
  border: none;
  padding: 2px 6px;
  border-radius: 12px;
  font-size: 12px;
  cursor: pointer;
}

.reaction-button:hover {
  background: #444;
}

.message-actions {
  display: flex;
  gap: 4px;
  margin-top: 8px;
}

.action-button {
  background: #333;
  color: white;
  border: none;
  width: 24px;
  height: 24px;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
}

.action-button:hover {
  background: #444;
}

.user-list, .questions-list {
  height: 100%;
  overflow-y: auto;
  padding: 16px;
}

.user-list-header, .questions-header {
  margin-bottom: 16px;
}

.user-list-header h3, .questions-header h3 {
  margin: 0 0 8px 0;
  color: white;
  font-size: 16px;
}

.questions-stats {
  display: flex;
  gap: 16px;
  font-size: 12px;
}

.questions-stats .stat {
  padding: 4px 8px;
  border-radius: 4px;
}

.questions-stats .unanswered {
  background: #ff9800;
  color: white;
}

.questions-stats .answered {
  background: #4CAF50;
  color: white;
}

.user-item {
  display: flex;
  align-items: center;
  padding: 8px;
  background: #2a2a2a;
  border-radius: 8px;
  margin-bottom: 8px;
  cursor: pointer;
}

.user-item:hover {
  background: #333;
}

.user-info {
  flex: 1;
}

.user-name {
  font-weight: bold;
  color: white;
  font-size: 14px;
}

.user-stats {
  display: flex;
  gap: 8px;
  font-size: 11px;
  color: #888;
}

.questions-section {
  margin-bottom: 24px;
}

.questions-section h4 {
  margin: 0 0 12px 0;
  color: white;
  font-size: 14px;
  border-bottom: 1px solid #333;
  padding-bottom: 4px;
}

.question-item {
  padding: 12px;
  background: #2a2a2a;
  border-radius: 8px;
  margin-bottom: 8px;
  border-left: 4px solid #333;
}

.question-item.unanswered {
  border-left-color: #ff9800;
}

.question-item.answered {
  border-left-color: #4CAF50;
}

.question-content {
  color: white;
  font-size: 14px;
  margin-bottom: 8px;
}

.question-meta {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: #888;
}

.answer-button {
  background: #4CAF50;
  color: white;
  border: none;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  margin-top: 8px;
}

.answer-button:hover {
  background: #45a049;
}

.question-answer {
  margin-top: 8px;
  padding: 8px;
  background: #1e3a1e;
  border-radius: 4px;
  font-size: 12px;
  color: #4CAF50;
}

.chat-input {
  padding: 16px;
  background: #2a2a2a;
  border-top: 1px solid #333;
}

.message-input {
  width: 100%;
  padding: 12px;
  background: #333;
  border: 1px solid #444;
  border-radius: 4px;
  color: white;
  font-size: 14px;
  resize: vertical;
  min-height: 60px;
  max-height: 120px;
}

.message-input:focus {
  outline: none;
  border-color: #4CAF50;
}

.message-input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.input-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.input-buttons {
  display: flex;
  gap: 8px;
}

.send-button, .question-button, .reaction-button {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
}

.send-button {
  background: #4CAF50;
  color: white;
}

.send-button:hover:not(:disabled) {
  background: #45a049;
}

.send-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.question-button {
  background: #ff9800;
  color: white;
}

.question-button:hover:not(:disabled) {
  background: #f57c00;
}

.question-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.reaction-button {
  background: #333;
  color: white;
}

.reaction-button:hover {
  background: #444;
}

.input-info {
  display: flex;
  gap: 8px;
  align-items: center;
}

.char-count {
  font-size: 12px;
  color: #888;
}

.sending-indicator {
  font-size: 14px;
  color: #4CAF50;
}

.chat-stats {
  display: flex;
  justify-content: space-around;
  padding: 8px 16px;
  background: #2a2a2a;
  border-top: 1px solid #333;
  font-size: 12px;
  color: #888;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
}
`;

// Добавляем стили в документ
if (typeof document !== 'undefined') {
  const styleElement = document.createElement('style');
  styleElement.textContent = styles;
  document.head.appendChild(styleElement);
}

export default LiveChat;