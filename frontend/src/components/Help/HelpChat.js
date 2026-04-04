// components/Help/HelpChat.js
import React, { useState, useEffect, useRef } from 'react';
import { adminsApi } from '../../services/api';
import { getUsername } from '../../keycloak';
import './HelpChat.css';

const HelpChat = ({ recipient, isGroup, onBack }) => {
    const [messages, setMessages] = useState([]);
    const [newMessage, setNewMessage] = useState('');
    const [loading, setLoading] = useState(true);
    const [sending, setSending] = useState(false);
    const messagesEndRef = useRef(null);
    const currentUser = getUsername();

    useEffect(() => {
        loadMessages();
        // Здесь будет WebSocket подключение
        return () => {};
    }, [recipient]);

    useEffect(() => {
        scrollToBottom();
    }, [messages]);

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    };

    const loadMessages = async () => {
        try {
            setLoading(true);
            let response;
            if (isGroup) {
                response = await adminsApi.getGroupMessages();
            } else {
                response = await adminsApi.getMessages(recipient.id);
            }
            setMessages(response.data);
        } catch (err) {
            console.error('Failed to load messages:', err);
        } finally {
            setLoading(false);
        }
    };

    const sendMessage = async () => {
        if (!newMessage.trim() || sending) return;

        setSending(true);
        try {
            let response;
            if (isGroup) {
                response = await adminsApi.sendToAll(newMessage.trim());
            } else {
                response = await adminsApi.sendMessage(recipient.id, newMessage.trim());
            }
            setMessages(prev => [...prev, response.data]);
            setNewMessage('');
        } catch (err) {
            console.error('Failed to send message:', err);
            alert('Ошибка при отправке');
        } finally {
            setSending(false);
        }
    };

    const formatTime = (dateString) => {
        const date = new Date(dateString);
        return date.toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' });
    };

    return (
        <div className="help-chat">
            <div className="help-chat-header">
                <button className="help-chat-back" onClick={onBack}>
                    ← Назад
                </button>
                <div className="help-chat-recipient">
                    <span className="recipient-icon">{isGroup ? '👥' : '👤'}</span>
                    <span className="recipient-name">{recipient.name}</span>
                    {!isGroup && recipient.status === 'online' && (
                        <span className="recipient-status">🟢 Онлайн</span>
                    )}
                </div>
            </div>

            <div className="help-chat-messages">
                {loading ? (
                    <div className="chat-loading">Загрузка сообщений...</div>
                ) : messages.length === 0 ? (
                    <div className="chat-empty">
                        <p>💬 Напишите свой вопрос</p>
                        <p className="empty-hint">Администратор ответит в ближайшее время</p>
                    </div>
                ) : (
                    messages.map((msg, idx) => (
                        <div
                            key={msg.id || idx}
                            className={`help-chat-message ${msg.fromUser === currentUser ? 'own' : ''}`}
                        >
                            <div className="help-chat-message-content">
                                <div className="help-chat-message-header">
                                    <span className="message-author">{msg.fromUser}</span>
                                    <span className="message-time">{formatTime(msg.createdAt)}</span>
                                </div>
                                <p className="help-chat-message-text">{msg.content}</p>
                            </div>
                        </div>
                    ))
                )}
                <div ref={messagesEndRef} />
            </div>

            <div className="help-chat-input-wrapper">
                <textarea
                    className="help-chat-input"
                    placeholder="✍️ Написать сообщение..."
                    value={newMessage}
                    onChange={(e) => setNewMessage(e.target.value)}
                    rows={2}
                    disabled={sending}
                />
                <button
                    className="help-chat-send"
                    onClick={sendMessage}
                    disabled={sending || !newMessage.trim()}
                >
                    {sending ? '⏳' : '📤'}
                </button>
            </div>
        </div>
    );
};

export default HelpChat;