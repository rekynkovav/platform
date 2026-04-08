import React, { useState, useEffect } from 'react';
import './Chat.css';

const NutritionChat = () => {
    const [messages, setMessages] = useState([]);
    const [newMessage, setNewMessage] = useState('');
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        loadMessages();
    }, []);

    const loadMessages = () => {
        const savedMessages = localStorage.getItem('nutritionChatMessages');
        if (savedMessages) {
            setMessages(JSON.parse(savedMessages));
        } else {
            const initialMessages = [
                {
                    id: 1,
                    author: 'Админ',
                    content: 'Добро пожаловать в чат по питанию! Здесь можно обсудить рецепты, диеты и здоровое питание.',
                    timestamp: new Date().toISOString()
                }
            ];
            setMessages(initialMessages);
            localStorage.setItem('nutritionChatMessages', JSON.stringify(initialMessages));
        }
        setLoading(false);
    };

    const handleSend = () => {
        if (!newMessage.trim()) return;

        const demoRole = localStorage.getItem('demoRole');
        let author = 'Пользователь';

        if (demoRole === 'admin-moderator' || demoRole === 'admin-nutrition' || demoRole === 'developer' || demoRole === 'vyacheslav') {
            author = 'Админ';
        } else if (demoRole === 'user1') {
            author = 'Пользователь 1';
        } else if (demoRole === 'user2') {
            author = 'Пользователь 2';
        }

        const message = {
            id: Date.now(),
            author: author,
            content: newMessage,
            timestamp: new Date().toISOString()
        };

        const updatedMessages = [...messages, message];
        setMessages(updatedMessages);
        localStorage.setItem('nutritionChatMessages', JSON.stringify(updatedMessages));
        setNewMessage('');
    };

    const handleKeyPress = (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSend();
        }
    };

    if (loading) return <div className="loading">Загрузка чата...</div>;

    return (
        <div className="chat-container">
            <div className="chat-messages">
                {messages.map(msg => (
                    <div key={msg.id} className="chat-message">
                        <div className="message-header">
                            <strong>{msg.author}</strong>
                            <span className="timestamp">{new Date(msg.timestamp).toLocaleTimeString()}</span>
                        </div>
                        <p>{msg.content}</p>
                    </div>
                ))}
            </div>
            <div className="chat-input">
                <textarea
                    value={newMessage}
                    onChange={(e) => setNewMessage(e.target.value)}
                    onKeyPress={handleKeyPress}
                    placeholder="Введите сообщение..."
                    rows="1"
                />
                <button onClick={handleSend}>Отправить</button>
            </div>
        </div>
    );
};

export default NutritionChat;
