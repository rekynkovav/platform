import React, { useState, useEffect } from 'react';
import './Chat.css';

const GeneralChat = () => {
    const [messages, setMessages] = useState([]);
    const [newMessage, setNewMessage] = useState('');
    const [loading, setLoading] = useState(true);
    const [chatRoom, setChatRoom] = useState('user1');
    const [canSwitchChats, setCanSwitchChats] = useState(false);
    const [editingMessageId, setEditingMessageId] = useState(null);
    const [editingContent, setEditingContent] = useState('');
    const [currentUserAuthor, setCurrentUserAuthor] = useState('');
    const [isRecording, setIsRecording] = useState(false);
    const [mediaRecorder, setMediaRecorder] = useState(null);
    const [recordingTime, setRecordingTime] = useState(0);
    const [recordingInterval, setRecordingInterval] = useState(null);

    useEffect(() => {
        const demoRole = localStorage.getItem('demoRole');

        // Определяем имя автора для текущего пользователя
        let author = 'Пользователь';
        if (demoRole === 'admin-moderator' || demoRole === 'developer' || demoRole === 'super-admin') {
            author = 'Админ';
        } else if (demoRole === 'user1') {
            author = 'Пользователь 1';
        } else if (demoRole === 'user2') {
            author = 'Пользователь 2';
        }
        setCurrentUserAuthor(author);

        // user1 не может переключать чаты
        if (demoRole === 'user1') {
            setChatRoom('user1');
            setCanSwitchChats(false);
        } else {
            // user2 и выше могут переключать
            setChatRoom('user1');
            setCanSwitchChats(true);
        }
    }, []);

    const handleChatRoomChange = (room) => {
        if (!canSwitchChats) return;
        setChatRoom(room);
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
                    content: `Добро пожаловать в чат ${chatRoom === 'user1' ? 'для новичков' : 'для продвинутых'}!`,
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

        if (demoRole === 'admin-moderator' || demoRole === 'developer' || demoRole === 'owner') {
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

    const handleEditMessage = (msg) => {
        if (msg.author !== currentUserAuthor) return;
        setEditingMessageId(msg.id);
        setEditingContent(msg.content);
    };

    const handleSaveEdit = (msgId) => {
        if (!editingContent.trim()) return;

        const updatedMessages = messages.map(msg =>
            msg.id === msgId
                ? { ...msg, content: editingContent, edited: true }
                : msg
        );
        setMessages(updatedMessages);

        const storageKey = `generalChatMessages_${chatRoom}`;
        localStorage.setItem(storageKey, JSON.stringify(updatedMessages));

        setEditingMessageId(null);
        setEditingContent('');
    };

    const handleCancelEdit = () => {
        setEditingMessageId(null);
        setEditingContent('');
    };

    const handleDeleteMessage = (msgId) => {
        // Проверяем, что пользователь может удалить только своё сообщение
        const messageToDelete = messages.find(msg => msg.id === msgId);
        if (!messageToDelete || messageToDelete.author !== currentUserAuthor) {
            alert('Вы можете удалять только свои сообщения');
            return;
        }

        if (!window.confirm('Удалить это сообщение?')) return;

        const updatedMessages = messages.filter(msg => msg.id !== msgId);
        setMessages(updatedMessages);

        const storageKey = `generalChatMessages_${chatRoom}`;
        localStorage.setItem(storageKey, JSON.stringify(updatedMessages));
    };

    const handleFileUpload = (e) => {
        const file = e.target.files[0];
        if (!file) return;

        const reader = new FileReader();
        reader.onload = (event) => {
            const demoRole = localStorage.getItem('demoRole');
            let author = currentUserAuthor;

            const message = {
                id: Date.now(),
                author: author,
                content: file.name,
                fileUrl: event.target.result,
                fileType: file.type,
                fileName: file.name,
                timestamp: new Date().toISOString(),
                type: 'file'
            };

            const updatedMessages = [...messages, message];
            setMessages(updatedMessages);

            const storageKey = `generalChatMessages_${chatRoom}`;
            localStorage.setItem(storageKey, JSON.stringify(updatedMessages));
        };
        reader.readAsDataURL(file);
    };

    const startRecording = async () => {
        try {
            const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
            const recorder = new MediaRecorder(stream);
            const chunks = [];

            recorder.ondataavailable = (e) => {
                chunks.push(e.data);
            };

            recorder.onstop = () => {
                const blob = new Blob(chunks, { type: 'audio/webm' });
                const reader = new FileReader();
                reader.onload = (event) => {
                    const message = {
                        id: Date.now(),
                        author: currentUserAuthor,
                        content: 'Голосовое сообщение',
                        fileUrl: event.target.result,
                        fileType: 'audio/webm',
                        timestamp: new Date().toISOString(),
                        type: 'voice'
                    };

                    const updatedMessages = [...messages, message];
                    setMessages(updatedMessages);

                    const storageKey = `generalChatMessages_${chatRoom}`;
                    localStorage.setItem(storageKey, JSON.stringify(updatedMessages));
                };
                reader.readAsDataURL(blob);

                stream.getTracks().forEach(track => track.stop());
            };

            recorder.start();
            setMediaRecorder(recorder);
            setIsRecording(true);
            setRecordingTime(0);

            const interval = setInterval(() => {
                setRecordingTime(prev => prev + 1);
            }, 1000);
            setRecordingInterval(interval);
        } catch (error) {
            console.error('Ошибка доступа к микрофону:', error);
            alert('Не удалось получить доступ к микрофону');
        }
    };

    const stopRecording = () => {
        if (mediaRecorder && mediaRecorder.state !== 'inactive') {
            mediaRecorder.stop();
            setIsRecording(false);
            if (recordingInterval) {
                clearInterval(recordingInterval);
                setRecordingInterval(null);
            }
        }
    };

    const formatRecordingTime = (seconds) => {
        const mins = Math.floor(seconds / 60);
        const secs = seconds % 60;
        return `${mins}:${secs.toString().padStart(2, '0')}`;
    };

    if (loading) return <div className="loading">Загрузка чата...</div>;

    return (
        <div className="chat-container">
            {/* Переключатель чатов - показываем только если разрешено */}
            {canSwitchChats && (
                <div className="chat-room-switcher">
                    <button
                        className={`chat-room-btn ${chatRoom === 'user1' ? 'active' : ''}`}
                        onClick={() => handleChatRoomChange('user1')}
                    >
                        Новички
                    </button>
                    <button
                        className={`chat-room-btn ${chatRoom === 'user2' ? 'active' : ''}`}
                        onClick={() => handleChatRoomChange('user2')}
                    >
                        Продвинутые
                    </button>
                </div>
            )}

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
                            <span className="timestamp">
                                {new Date(msg.timestamp).toLocaleTimeString()}
                                {msg.edited && <span className="edited-label"> (изменено)</span>}
                            </span>
                            {msg.author === currentUserAuthor && (
                                <div className="message-actions">
                                    <button
                                        className="edit-message-btn"
                                        onClick={() => handleEditMessage(msg)}
                                        title="Редактировать"
                                    >
                                        ✏️
                                    </button>
                                    <button
                                        className="delete-message-btn"
                                        onClick={() => handleDeleteMessage(msg.id)}
                                        title="Удалить"
                                    >
                                        🗑️
                                    </button>
                                </div>
                            )}
                        </div>
                        {editingMessageId === msg.id ? (
                            <div className="edit-message-form">
                                <textarea
                                    value={editingContent}
                                    onChange={(e) => setEditingContent(e.target.value)}
                                    rows="2"
                                />
                                <div className="edit-actions">
                                    <button onClick={() => handleSaveEdit(msg.id)}>Сохранить</button>
                                    <button onClick={handleCancelEdit}>Отмена</button>
                                </div>
                            </div>
                        ) : (
                            <>
                                <p>{msg.content}</p>
                                {msg.type === 'file' && msg.fileUrl && (
                                    <div className="message-file">
                                        <a href={msg.fileUrl} download={msg.fileName} className="file-download-link">
                                            📎 {msg.fileName}
                                        </a>
                                    </div>
                                )}
                                {msg.type === 'voice' && msg.fileUrl && (
                                    <div className="message-voice">
                                        <audio controls src={msg.fileUrl} />
                                    </div>
                                )}
                            </>
                        )}
                    </div>
                ))}
            </div>
            <div className="chat-input">
                <div className="chat-input-controls">
                    <label className="attach-file-btn" title="Прикрепить файл">
                        📎
                        <input
                            type="file"
                            onChange={handleFileUpload}
                            style={{ display: 'none' }}
                        />
                    </label>
                    <button
                        className={`voice-record-btn ${isRecording ? 'recording' : ''}`}
                        onClick={isRecording ? stopRecording : startRecording}
                        title={isRecording ? 'Остановить запись' : 'Записать голосовое сообщение'}
                    >
                        {isRecording ? '⏹️' : '🎤'}
                    </button>
                    {isRecording && (
                        <span className="recording-time">{formatRecordingTime(recordingTime)}</span>
                    )}
                </div>
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
