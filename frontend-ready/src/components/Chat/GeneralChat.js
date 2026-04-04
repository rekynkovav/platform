// components/Chat/GeneralChat.js
import React, { useState, useEffect, useRef } from 'react';
import { chatApi } from '../../services/api';
import { getUsername, getUserId } from '../../keycloak';
import './GeneralChat.css';

const GeneralChat = () => {
    const [messages, setMessages] = useState([]);
    const [newMessage, setNewMessage] = useState('');
    const [loading, setLoading] = useState(true);
    const [sending, setSending] = useState(false);
    const [ws, setWs] = useState(null);
    const messagesEndRef = useRef(null);
    const currentUser = getUsername();
    const currentUserId = getUserId();

    useEffect(() => {
        loadMessages();
        connectWebSocket();

        return () => {
            if (ws) ws.close();
        };
    }, []);

    useEffect(() => {
        scrollToBottom();
    }, [messages]);

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    };

    const loadMessages = async () => {
        try {
            const response = await chatApi.getMessages();
            setMessages(response.data);
        } catch (err) {
            console.error('Failed to load messages:', err);
        } finally {
            setLoading(false);
        }
    };

    const connectWebSocket = () => {
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        const wsUrl = `${protocol}//${window.location.host}/ws/chat`;
        const socket = new WebSocket(wsUrl);

        socket.onopen = () => {
            console.log('WebSocket connected');
        };

        socket.onmessage = (event) => {
            const message = JSON.parse(event.data);
            setMessages(prev => [...prev, message]);
        };

        socket.onerror = (error) => {
            console.error('WebSocket error:', error);
        };

        socket.onclose = () => {
            console.log('WebSocket disconnected');
            setTimeout(connectWebSocket, 3000);
        };

        setWs(socket);
    };

    const sendMessage = async () => {
        if (!newMessage.trim() || sending) return;

        setSending(true);
        try {
            const response = await chatApi.sendMessage(newMessage.trim());
            setMessages(prev => [...prev, response.data]);
            setNewMessage('');
        } catch (err) {
            console.error('Failed to send message:', err);
            alert('Ошибка при отправке');
        } finally {
            setSending(false);
        }
    };

    const handleKeyPress = (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendMessage();
        }
    };

    const formatTime = (dateString) => {
        const date = new Date(dateString);
        return date.toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' });
    };

    return (
        <div className="general-chat">
            <div className="chat-header">
                <span className="chat-icon">💬</span>
                <span className="chat-title">Общий чат</span>
                <span className="chat-users">👥 {messages.length > 0 ? 'активен' : 'онлайн'}</span>
            </div>

            <div className="chat-messages">
                {loading ? (
                    <div className="chat-loading">Загрузка сообщений...</div>
                ) : messages.length === 0 ? (
                    <div className="chat-empty">
                        <p>💭 Пока нет сообщений</p>
                        <p className="empty-hint">Будьте первым, кто напишет в чат!</p>
                    </div>
                ) : (
                    messages.map((msg, idx) => (
                        <div
                            key={msg.id || idx}
                            className={`chat-message ${msg.userId === currentUserId ? 'own' : ''}`}
                        >
                            <div className="chat-message-avatar">
                                {msg.userName?.[0] || '👤'}
                            </div>
                            <div className="chat-message-content">
                                <div className="chat-message-header">
                                    <span className="chat-message-author">{msg.userName || 'User'}</span>
                                    <span className="chat-message-time">{formatTime(msg.createdAt)}</span>
                                </div>
                                <p className="chat-message-text">{msg.content}</p>
                            </div>
                        </div>
                    ))
                )}
                <div ref={messagesEndRef} />
            </div>

            <div className="chat-input-wrapper">
                <textarea
                    className="chat-input"
                    placeholder="✍️ Написать сообщение..."
                    value={newMessage}
                    onChange={(e) => setNewMessage(e.target.value)}
                    onKeyPress={handleKeyPress}
                    rows={2}
                    disabled={sending}
                />
                <button
                    className="chat-send"
                    onClick={sendMessage}
                    disabled={sending || !newMessage.trim()}
                >
                    {sending ? '⏳' : '📤'}
                </button>
            </div>
        </div>
    );
};

export default GeneralChat;