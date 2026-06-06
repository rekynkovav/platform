import React, { useState, useEffect, useRef, useCallback } from 'react';
import { chatApi } from '../../services/api';
import useWebSocket from '../../hooks/useWebSocket';
import { getUsername } from '../../keycloak';
import './GeneralChat.css';

const GeneralChat = () => {
    const [messages, setMessages] = useState([]);
    const [newMessage, setNewMessage] = useState('');
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [hasMore, setHasMore] = useState(true);
    const [error, setError] = useState(null);
    const messagesEndRef = useRef(null);
    const username = getUsername();
    const roomSlug = 'general';

    // Множество идентификаторов уже полученных сообщений для исключения дубликатов
    const messageIdsRef = useRef(new Set());

    const { isConnected, send, error: wsError } = useWebSocket(roomSlug, (wsMessage) => {
        // Проверяем, не было ли уже такое сообщение
        if (wsMessage.id && !messageIdsRef.current.has(wsMessage.id)) {
            messageIdsRef.current.add(wsMessage.id);
            setMessages(prev => [...prev, wsMessage]);
        } else if (!wsMessage.id) {
            // Если id отсутствует, добавляем без проверки (риск дублирования)
            setMessages(prev => [...prev, wsMessage]);
        }
    });

    const clearError = () => {
        setError(null);
    };

    // Загрузка истории
    const loadMessages = useCallback(async (reset = false) => {
        try {
            setLoading(true);
            const currentPage = reset ? 0 : page;
            const response = await chatApi.getMessages(roomSlug, currentPage, 50);

            if (reset) {
                // Очищаем множество id перед новой загрузкой
                messageIdsRef.current = new Set(
                    (response.data || []).map(msg => msg.id).filter(Boolean)
                );
                setMessages(response.data || []);
                setPage(1);
            } else {
                const newMsgs = response.data || [];
                // Фильтруем только те, которых ещё нет
                const filtered = newMsgs.filter(msg => msg.id && !messageIdsRef.current.has(msg.id));
                filtered.forEach(msg => messageIdsRef.current.add(msg.id));
                setMessages(prev => [...filtered, ...prev]);
                setPage(currentPage + 1);
            }
            // Backend возвращает простой список, без пагинации
            setHasMore(false);
        } catch (error) {
            console.error('Failed to load messages:', error);
            setError('Не удалось загрузить сообщения');
        } finally {
            setLoading(false);
        }
    }, [page, roomSlug]);

    useEffect(() => {
        loadMessages(true);
    }, []);

    // Автоскролл при новых сообщениях
    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [messages]);

    // Отправка сообщения
    const handleSend = async () => {
        if (!newMessage.trim()) return;
        if (!isConnected) {
            alert('WebSocket disconnected. Please refresh the page.');
            return;
        }

        try {
            const response = await chatApi.sendMessage(roomSlug, {
                content: newMessage,
                userId: username
            });

            // Добавляем id в множество для предотвращения дублирования,
            // если сервер пришлёт это же сообщение через WebSocket.
            if (response.id) {
                messageIdsRef.current.add(response.id);
            }

            send({
                id: response.id,
                content: newMessage,
                author: username,
                createdAt: new Date().toISOString()
            });

            setNewMessage('');
        } catch (error) {
            console.error('Failed to send message:', error);
            setError('Не удалось отправить сообщение');
        }
    };

    const handleKeyDown = (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSend();
        }
    };

    return (
        <div className="general-chat">
            <div className="chat-header">
                <h3>Общий чат</h3>
                <div className="connection-status">
                    {isConnected ? '🟢 Connected' : '🔴 Disconnected'}
                </div>
            </div>

            {error && (
                <div className="chat-error">
                    <span className="error-icon">⚠️</span>
                    <span>{error}</span>
                    <button className="error-close" onClick={() => setError(null)}>×</button>
                </div>
            )}

            {wsError && (
                <div className="chat-error">
                    <span className="error-icon">⚠️</span>
                    <span>WebSocket error: {wsError}</span>
                    <button className="error-close" onClick={() => setError(null)}>×</button>
                </div>
            )}

            <div className="messages-container">
                {loading && <div className="loading">Loading...</div>}
                {hasMore && !loading && (
                    <button onClick={() => loadMessages()} className="load-more">
                        Load older messages
                    </button>
                )}
                {messages.map(msg => (
                    <div key={msg.id || Math.random()} className={`message ${msg.author === username ? 'own' : 'other'}`}>
                        <div className="message-author">{msg.author}</div>
                        <div className="message-content">{msg.content}</div>
                        <div className="message-time">
                            {new Date(msg.createdAt).toLocaleTimeString()}
                        </div>
                    </div>
                ))}
                <div ref={messagesEndRef} />
            </div>

            <div className="message-input">
                <textarea
                    value={newMessage}
                    onChange={(e) => setNewMessage(e.target.value)}
                    onKeyDown={handleKeyDown}
                    placeholder="Type a message..."
                    rows="3"
                />
                <button onClick={handleSend} disabled={!isConnected || !newMessage.trim()}>
                    Send
                </button>
            </div>
        </div>
    );
};

export default GeneralChat;
