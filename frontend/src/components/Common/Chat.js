import React, { useState, useEffect, useRef, useCallback } from 'react';
import { FixedSizeList as List } from 'react-window';
import { AutoSizer } from 'react-virtualized-auto-sizer';
import { chatApi } from '../../services/api';
import { useWebSocket } from '../../hooks/useWebSocket';
import './Chat.css';

const Chat = ({ roomSlug = 'general', currentUser }) => {
    const [messages, setMessages] = useState([]);
    const [newMessage, setNewMessage] = useState('');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [users, setUsers] = useState([]);
    const [isTyping, setIsTyping] = useState(false);
    const [typingUsers, setTypingUsers] = useState([]);
    const messagesEndRef = useRef(null);
    const inputRef = useRef(null);
    const typingTimeoutRef = useRef(null);

    // Используем WebSocket хук
    const { isConnected, error: wsError, send } = useWebSocket(
        roomSlug,
        useCallback((data) => {
            switch (data.type) {
                case 'message':
                    setMessages(prev => [...prev, data.payload]);
                    break;
                case 'typing':
                    setTypingUsers(prev => {
                        const newUsers = data.payload.users.filter(u => u !== currentUser);
                        return newUsers.length > 0 ? newUsers : [];
                    });
                    break;
                case 'user_joined':
                    setUsers(prev => [...prev, data.payload.user]);
                    break;
                case 'user_left':
                    setUsers(prev => prev.filter(u => u.id !== data.payload.userId));
                    break;
                case 'users_list':
                    setUsers(data.payload.users);
                    break;
            }
        }, [currentUser])
    );

    useEffect(() => {
        const fetchChatData = async () => {
            try {
                setLoading(true);
                // Загружаем историю сообщений
                const history = await chatApi.getMessages(roomSlug, 0, 50);
                setMessages(history);

                // Загружаем список пользователей
                const usersList = await chatApi.getUsers(roomSlug);
                setUsers(usersList);
            } catch (err) {
                console.error('Failed to fetch chat data:', err);
                setError('Не удалось загрузить чат');
            } finally {
                setLoading(false);
            }
        };

        fetchChatData();
    }, [roomSlug]);

    useEffect(() => {
        scrollToBottom();
    }, [messages]);

    const clearError = () => {
        setError(null);
    };

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    };

    const handleSendMessage = async () => {
        if (!newMessage.trim() || !isConnected) return;

        const message = {
            id: Date.now().toString(),
            text: newMessage.trim(),
            author: currentUser,
            timestamp: new Date().toISOString(),
            room: roomSlug
        };

        try {
            // Отправляем через WebSocket
            const success = send({
                type: 'message',
                payload: message
            });

            if (success) {
                setMessages(prev => [...prev, message]);
                setNewMessage('');
                stopTyping();
            } else {
                // Fallback к REST API
                await chatApi.sendMessage(roomSlug, message);
                setMessages(prev => [...prev, message]);
                setNewMessage('');
                stopTyping();
            }
        } catch (err) {
            console.error('Failed to send message:', err);
            setError('Не удалось отправить сообщение');
        }
    };

    const handleTyping = () => {
        if (!isConnected) return;

        if (!isTyping) {
            setIsTyping(true);
            send({
                type: 'typing',
                payload: { user: currentUser, action: 'start' }
            });
        }

        if (typingTimeoutRef.current) {
            clearTimeout(typingTimeoutRef.current);
        }

        typingTimeoutRef.current = setTimeout(() => {
            stopTyping();
        }, 3000);
    };

    const stopTyping = () => {
        if (isTyping && isConnected) {
            setIsTyping(false);
            send({
                type: 'typing',
                payload: { user: currentUser, action: 'stop' }
            });
        }
    };

    const handleKeyPress = (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSendMessage();
        }
    };

    const formatTime = (timestamp) => {
        const date = new Date(timestamp);
        return date.toLocaleTimeString('ru-RU', {
            hour: '2-digit',
            minute: '2-digit'
        });
    };

    const formatDate = (timestamp) => {
        const date = new Date(timestamp);
        const today = new Date();

        if (date.toDateString() === today.toDateString()) {
            return 'Сегодня';
        }

        const yesterday = new Date(today);
        yesterday.setDate(yesterday.getDate() - 1);
        if (date.toDateString() === yesterday.toDateString()) {
            return 'Вчера';
        }

        return date.toLocaleDateString('ru-RU', {
            day: 'numeric',
            month: 'short'
        });
    };

    const MessageItem = ({ index, style }) => {
        const message = messages[index];
        const isOwn = message.author === currentUser;
        const showDate = index === 0 ||
            formatDate(message.timestamp) !== formatDate(messages[index - 1]?.timestamp);

        return (
            <div style={style}>
                {showDate && (
                    <div className="date-divider">
                        <span>{formatDate(message.timestamp)}</span>
                    </div>
                )}
                <div className={`message-item ${isOwn ? 'own' : 'other'}`}>
                    {!isOwn && (
                        <div className="message-avatar">
                            {message.author.charAt(0).toUpperCase()}
                        </div>
                    )}
                    <div className="message-content">
                        {!isOwn && (
                            <div className="message-author">{message.author}</div>
                        )}
                        <div className="message-bubble">
                            <div className="message-text">{message.text}</div>
                            <div className="message-time">{formatTime(message.timestamp)}</div>
                        </div>
                    </div>
                </div>
            </div>
        );
    };

    if (loading) {
        return (
            <div className="chat-container loading">
                <div className="loading-spinner"></div>
                <span>Загрузка чата...</span>
            </div>
        );
    }

    if (error) {
        return (
            <div className="chat-container error">
                <div className="error-icon">⚠️</div>
                <span>{error}</span>
            </div>
        );
    }

    return (
        <div className="chat-container">
            {/* Отображение ошибок */}
            {error && (
                <div className="error-message">
                    <span className="error-icon">⚠️</span>
                    <span>{error}</span>
                    <button className="error-close" onClick={clearError}>×</button>
                </div>
            )}

            <div className="chat-header">
                <div className="chat-info">
                    <h3>💬 Чат: {roomSlug}</h3>
                    <div className="chat-status">
                        <div className={`status-dot ${isConnected ? 'connected' : 'disconnected'}`}></div>
                        <span>{isConnected ? 'Подключено' : 'Отключено'}</span>
                        {wsError && <span className="status-error">• {wsError}</span>}
                    </div>
                </div>
                <div className="chat-users">
                    <div className="users-count">
                        <span className="users-icon">👥</span>
                        <span>{users.length} онлайн</span>
                    </div>
                    {typingUsers.length > 0 && (
                        <div className="typing-indicator">
                            <div className="typing-dots">
                                <span></span>
                                <span></span>
                                <span></span>
                            </div>
                            <span>{typingUsers.join(', ')} печатает...</span>
                        </div>
                    )}
                </div>
            </div>

            <div className="chat-messages" style={{ height: '400px' }}>
                <AutoSizer>
                    {({ height, width }) => (
                        <List
                            height={height}
                            width={width}
                            itemCount={messages.length}
                            itemSize={80}
                            overscanCount={10}
                        >
                            {MessageItem}
                        </List>
                    )}
                </AutoSizer>
                <div ref={messagesEndRef} />
            </div>

            <div className="chat-input-area">
                <div className="input-wrapper">
                    <textarea
                        ref={inputRef}
                        className="chat-input"
                        placeholder="Введите сообщение..."
                        value={newMessage}
                        onChange={(e) => {
                            setNewMessage(e.target.value);
                            handleTyping();
                        }}
                        onKeyPress={handleKeyPress}
                        rows="3"
                        disabled={!isConnected}
                    />
                    <button
                        className="send-button"
                        onClick={handleSendMessage}
                        disabled={!newMessage.trim() || !isConnected}
                        title={!isConnected ? 'Чат отключен' : 'Отправить сообщение'}
                    >
                        📤
                    </button>
                </div>
                <div className="input-hint">
                    <span>Нажмите Enter для отправки, Shift+Enter для новой строки</span>
                    {isTyping && <span className="typing-hint">Вы печатаете...</span>}
                </div>
            </div>

            <div className="chat-users-sidebar">
                <h4>Участники ({users.length})</h4>
                <div className="users-list">
                    {users.map(user => (
                        <div key={user.id} className="user-item">
                            <div className="user-avatar-small">
                                {user.name.charAt(0).toUpperCase()}
                            </div>
                            <div className="user-info">
                                <div className="user-name">{user.name}</div>
                                <div className="user-status">
                                    <div className="user-status-dot online"></div>
                                    <span>В сети</span>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
};

export default Chat;