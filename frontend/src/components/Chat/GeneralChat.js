import React, { useState, useEffect } from 'react';
import { mockChatApi } from '../../services/mockApi';
import './Chat.css';

const GeneralChat = () => {
    const [messages, setMessages] = useState([]);
    const [newMessage, setNewMessage] = useState('');
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        loadMessages();
    }, []);

    const loadMessages = async () => {
        try {
            const response = await mockChatApi.getMessages();
            setMessages(response.data);
        } catch (error) {
            console.error('Ошибка загрузки сообщений:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleSend = async () => {
        if (!newMessage.trim()) return;

        try {
            const response = await mockChatApi.sendMessage(newMessage);
            setMessages(prev => [...prev, response.data]);
            setNewMessage('');
        } catch (error) {
            console.error('Ошибка отправки:', error);
        }
    };

    if (loading) return <div className="loading">Загрузка чата...</div>;

    return (
        <div className="chat-container">
            <div className="chat-messages">
                {messages.map(msg => (
                    <div key={msg.id} className="chat-message">
                        <strong>{msg.author}</strong>
                        <span className="timestamp">{new Date(msg.timestamp).toLocaleTimeString()}</span>
                        <p>{msg.content}</p>
                    </div>
                ))}
            </div>
            <div className="chat-input">
                <input
                    type="text"
                    value={newMessage}
                    onChange={(e) => setNewMessage(e.target.value)}
                    onKeyPress={(e) => e.key === 'Enter' && handleSend()}
                    placeholder="Введите сообщение..."
                />
                <button onClick={handleSend}>Отправить</button>
            </div>
        </div>
    );
};

export default GeneralChat;