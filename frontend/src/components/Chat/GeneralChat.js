import React, { useState, useEffect } from 'react';
import './Chat.css';

const GeneralChat = () => {
    const [messages, setMessages] = useState([]);
    const [newMessage, setNewMessage] = useState('');
    const [loading, setLoading] = useState(true);
    const [chatRoom, setChatRoom] = useState('user1'); // user1 или user2

    useEffect(() => {
        // Определяем комнату чата на основе роли
        const demoRole = localStorage.getItem('demoRole');
        if (demoRole === 'user2') {
            setChatRoom('user2');
        } else {
            setChatRoom('user1');
        }
    }, []);

    // Переключатель чата
    const handleChatRoomChange = (room) => {
        setChatRoom(room);
        localStorage.setItem('demoRole', room);
        // Обновляем сообщения для новой комнаты
        const storageKey = `generalChatMessages_${room}`;
        const savedMessages = localStorage.getItem(storageKey);
        if (savedMessages) {
            setMessages(JSON.parse(savedMessages));
        } else {
            const initialMessages = [
                {
                    id: 1,
                    author: 'Админ',
                    content: `Добро пожаловать в чат для ${room === 'user1' ? 'новичков' : 'продвинутых пользователей'}!`,
                    timestamp: new Date().toISOString()
                }
            ];
            setMessages(initialMessages);
            localStorage.setItem(storageKey, JSON.stringify(initialMessages));
        }
    };

    useEffect(() => {
        loadMessages();
    }, [chatRoom]);

    const loadMessages = () => {
        const storageKey = `generalChatMessages_${chatRoom}`;
        const savedMessages = localStorage.getItem(storageKey);

        if (savedMessages) {
            setMessages(JSON.parse(savedMessages));
        } else {
            const initialMessages = [
                {
                    id: 1,
                    author: 'Админ',
                    content: `Добро пожаловать в чат для ${chatRoom === 'user1' ? 'новичков' : 'продвинутых пользователей'}!`,
                    timestamp: new Date().toISOString()
                }
            ];
            setMessages(initialMessages);
            localStorage.setItem(storageKey, JSON.stringify(initialMessages));
        }
        setLoading(false);
    };

    const handleSend = () => {
        if (!newMessage.trim()) return;

        const demoRole = localStorage.getItem('demoRole');
        let author = 'Пользователь';

        if (demoRole === 'admin-moderator' || demoRole === 'admin-consultant' || demoRole === 'developer' || demoRole === 'owner') {
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

        const storageKey = `generalChatMessages_${chatRoom}`;
        localStorage.setItem(storageKey, JSON.stringify(updatedMessages));
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
            {/* Переключатель чатов */}
            <div className="chat-room-switcher">
                <button
                    className={`chat-room-btn ${chatRoom === 'user1' ? 'active' : ''}`}
                    onClick={() => handleChatRoomChange('user1')}
                >
                    👥 Новички
                </button>
                <button
                    className={`chat-room-btn ${chatRoom === 'user2' ? 'active' : ''}`}
                    onClick={() => handleChatRoomChange('user2')}
                >
                    ⭐ Продвинутые
                </button>
            </div>

            <div className="chat-header">
                <h3 className="chat-title">
                    {chatRoom === 'user1' ? 'Чат новичков' : 'Чат продвинутых'}
                </h3>
            </div>

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
                    onKeyDown={handleKeyPress}
                    placeholder="Введите сообщение..."
                    rows="1"
                />
                <button onClick={handleSend}>Отправить</button>
            </div>
        </div>
    );
};

export default GeneralChat;
