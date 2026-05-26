import React, { useState, useRef, useEffect } from 'react';
import MediaRenderer from './MediaRenderer';
import './Chat.css';

const ChatRoom = ({
    messages,
    currentUserAuthor,
    currentRoom,
    canSwitch,
    availableRooms,
    roomTitles = {},
    onRoomChange,
    onSendMessage,
    onEditMessage,
    onDeleteMessage,
    onFileUpload,
    onStartRecording,
    onStopRecording,
    isRecording,
    recordingTime,
    error: externalError,
    onClearError,
    uploading = false
}) => {
    const [newMessage, setNewMessage] = useState('');
    const [editingMessageId, setEditingMessageId] = useState(null);
    const [editingContent, setEditingContent] = useState('');
    const [selectedFile, setSelectedFile] = useState(null);
    const [filePreview, setFilePreview] = useState(null);
    const [mediaType, setMediaType] = useState(null);
    const [isUploading, setIsUploading] = useState(false);
    const [internalError, setInternalError] = useState(null);

    // Объединяем внутреннюю и внешнюю ошибки
    const error = externalError || internalError;

    const clearError = () => {
        setInternalError(null);
        onClearError?.();
    };

    const fileInputRef = useRef(null);
    const messagesEndRef = useRef(null);
    const mountedRef = useRef(true);

    useEffect(() => {
        return () => { mountedRef.current = false; };
    }, []);

    // Прокрутка к последнему сообщению
    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [messages]);

    // Определение типа медиа
    const getMediaTypeFromFile = (file) => {
        if (file.type.startsWith('image/')) return 'image';
        if (file.type.startsWith('video/')) return 'video';
        if (file.type.startsWith('audio/')) return 'audio';
        return 'file';
    };

    // Предпросмотр файла
    const handleFileSelect = (e) => {
        const file = e.target.files?.[0];
        if (!file) return;

        // Проверка размера (макс 50MB)
        const maxSize = 50 * 1024 * 1024;
        if (file.size > maxSize) {
            setInternalError('Файл слишком большой. Максимум 50MB');
            return;
        }

        setInternalError(null);
        setSelectedFile(file);
        setMediaType(getMediaTypeFromFile(file));

        // Предпросмотр для изображений и видео
        if (file.type.startsWith('image/') || file.type.startsWith('video/')) {
            const reader = new FileReader();
            reader.onload = (event) => {
                if (mountedRef.current) setFilePreview(event.target.result);
            };
            reader.readAsDataURL(file);
        } else if (file.type.startsWith('audio/')) {
            const reader = new FileReader();
            reader.onload = (event) => {
                if (mountedRef.current) setFilePreview(event.target.result);
            };
            reader.readAsDataURL(file);
        } else {
            setFilePreview(null);
        }
    };

    // Отмена выбора файла
    const handleCancelFile = () => {
        setSelectedFile(null);
        setFilePreview(null);
        setMediaType(null);
        setInternalError(null);
        if (fileInputRef.current) {
            fileInputRef.current.value = '';
        }
    };

    const handleClearError = () => {
        setInternalError(null);
        if (onClearError) onClearError();
    };

    // Отправка сообщения с файлом
    const handleSend = async () => {
        if (!newMessage.trim() && !selectedFile) return;

        let uploadedMediaId = null;

        // Если есть файл - загружаем сначала
        if (selectedFile) {
            setIsUploading(true);
            setInternalError(null);

            try {
                // Если предоставлен onFileUpload callback, используем его
                if (onFileUpload && typeof onFileUpload === 'function') {
                    uploadedMediaId = await onFileUpload(selectedFile);
                } else {
                    // TODO: Реализовать загрузку через mediaApi.upload
                    // const response = await mediaApi.upload(selectedFile);
                    // uploadedMediaId = response.id;
                    console.warn('Загрузка файлов через API пока не реализована');
                    setInternalError('Загрузка файлов временно недоступна');
                    setIsUploading(false);
                    return;
                }
            } catch (err) {
                console.error('Upload failed:', err);
                setInternalError('Не удалось загрузить файл');
                setIsUploading(false);
                return;
            }

            setIsUploading(false);
        }

        // Отправка сообщения (текст и/или медиа)
        if (onSendMessage) {
            try {
                let messageData;

                if (newMessage.trim() && uploadedMediaId) {
                    // Текст + медиа
                    messageData = { content: newMessage, mediaId: uploadedMediaId };
                } else if (newMessage.trim()) {
                    // Только текст
                    messageData = newMessage;
                } else if (uploadedMediaId) {
                    // Только медиа
                    messageData = { content: '', mediaId: uploadedMediaId };
                } else {
                    // Ничего не отправляем
                    return;
                }

                await onSendMessage(messageData);
            } catch (err) {
                console.error('Failed to send message:', err);
                setInternalError('Не удалось отправить сообщение');
                return;
            }
        }

        // Очищаем
        setNewMessage('');
        handleCancelFile();
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

    const handleSaveEdit = async (msgId) => {
        if (!editingContent.trim()) return;
        if (onEditMessage) {
            try {
                await onEditMessage(msgId, editingContent);
                setEditingMessageId(null);
                setEditingContent('');
            } catch (err) {
                console.error('Failed to edit message:', err);
                setInternalError('Не удалось отредактировать сообщение');
            }
        }
    };

    const handleCancelEdit = () => {
        setEditingMessageId(null);
        setEditingContent('');
    };

    const handleDeleteMessage = async (msgId) => {
        if (onDeleteMessage) {
            try {
                await onDeleteMessage(msgId);
            } catch (err) {
                console.error('Failed to delete message:', err);
                setInternalError('Не удалось удалить сообщение');
            }
        }
    };

    const getRoomTitle = () => {
        return roomTitles[currentRoom] || currentRoom;
    };

    // Рендер медиа в сообщении
    const renderMedia = (msg) => {
        // 1. Base64 (localStorage mode)
        if (msg.fileUrl) {
            return (
                <MediaRenderer
                    mediaUrl={msg.fileUrl}
                    mediaType={msg.fileType || msg.type}
                    fileName={msg.fileName}
                />
            );
        }

        // 2..mediaUrl (новый API)
        if (msg.mediaUrl) {
            return (
                <MediaRenderer
                    mediaUrl={msg.mediaUrl}
                    mediaType={msg.mediaType || 'file'}
                    fileName={msg.fileName}
                />
            );
        }

        // 3. Медиа по mediaId (нужно получить URL)
        if (msg.mediaId) {
            return (
                <MediaRenderer
                    mediaUrl={`/api/media/stream/${msg.mediaId}`}
                    mediaType={msg.mediaType || 'file'}
                    fileName={msg.fileName}
                />
            );
        }

        return null;
    };

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

            {/* Переключатель чатов */}
            {canSwitch && availableRooms && availableRooms.length > 0 && (
                <div className="chat-room-switcher">
                    {availableRooms.map(room => (
                        <button
                            key={room.id}
                            className={`chat-room-btn ${currentRoom === room.id ? 'active' : ''}`}
                            onClick={() => onRoomChange(room.id)}
                        >
                            {room.label}
                        </button>
                    ))}
                </div>
            )}

            <div className="chat-header">
                <h3 className="chat-title">{getRoomTitle()}</h3>
            </div>

            <div className="chat-messages">
                {messages.map(msg => (
                    <div
                        key={msg.id}
                        className={`chat-message ${msg.author === currentUserAuthor ? 'own' : ''} ${msg.pending ? 'pending' : ''}`}
                    >
                        <div className="message-avatar">
                            {msg.avatarUrl ? (
                                <img
                                    src={msg.avatarUrl}
                                    alt={`Аватар ${msg.author}`}
                                    className="avatar-image"
                                    onError={(e) => {
                                        e.target.style.display = 'none';
                                        e.target.parentElement.querySelector('.avatar-placeholder').style.display = 'flex';
                                    }}
                                />
                            ) : null}
                            <div className={`avatar-placeholder ${msg.avatarUrl ? 'hidden' : ''}`}>
                                <span className="avatar-icon">
                                    {msg.author === 'Система' ? '🤖' :
                                     msg.author === 'Админ' ? '🛡️' : '👤'}
                                </span>
                            </div>
                        </div>

                        <div className="message-content-wrapper">
                            <div className="message-header">
                                <strong>{msg.author}</strong>
                                <span className="timestamp">
                                    {new Date(msg.timestamp).toLocaleTimeString()}
                                    {msg.edited && <span className="edited-label"> (изменено)</span>}
                                </span>
                                {msg.pending && <span className="pending-label">Отправляется...</span>}
                                {msg.author === currentUserAuthor && !msg.pending && (
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
                                {msg.content && <p className="message-content">{msg.content}</p>}

                                {/* Медиа в сообщении */}
                                {(msg.fileUrl || msg.mediaUrl || msg.mediaId) && (
                                    <div className="message-media">
                                        {renderMedia(msg)}
                                    </div>
                                )}
                            </>
                        )}
                        </div>
                    </div>
                ))}
                <div ref={messagesEndRef} />
            </div>

            {/* Предпросмотр файла */}
            {selectedFile && (
                <div className="file-preview-container">
                    <div className="file-preview">
                        {mediaType === 'image' && filePreview && (
                            <img src={filePreview} alt="Preview" className="preview-image" />
                        )}
                        {mediaType === 'video' && filePreview && (
                            <video src={filePreview} className="preview-video" controls />
                        )}
                        {mediaType === 'audio' && filePreview && (
                            <audio src={filePreview} controls className="preview-audio" />
                        )}
                        {mediaType === 'file' && (
                            <div className="preview-file">
                                <span className="file-icon">📄</span>
                                <span className="file-name">{selectedFile.name}</span>
                            </div>
                        )}
                        {error && (
                            <div className="upload-error">
                                <span className="error-icon">⚠️</span>
                                <span>{error}</span>
                                <button className="error-close" onClick={handleClearError}>×</button>
                            </div>
                        )}
                    </div>
                    <button className="cancel-file-btn" onClick={handleCancelFile}>✕</button>
                </div>
            )}

            <div className="chat-input">
                <div className="chat-input-controls">
                    <label className="attach-file-btn" title="Прикрепить файл">
                        📎
                        <input
                            ref={fileInputRef}
                            type="file"
                            onChange={handleFileSelect}
                            style={{ display: 'none' }}
                            accept="image/*,video/*,audio/*,.pdf,.doc,.docx,.txt"
                        />
                    </label>
                    <button
                        className={`voice-record-btn ${isRecording ? 'recording' : ''}`}
                        onClick={isRecording ? onStopRecording : onStartRecording}
                        title={isRecording ? 'Остановить запись' : 'Записать голосовое'}
                        disabled={isUploading || !onStartRecording || !onStopRecording}
                    >
                        {isRecording ? '⏹️' : '🎤'}
                    </button>
                    {isRecording && (
                        <span className="recording-time">
                            {Math.floor(recordingTime / 60)}:{String(recordingTime % 60).padStart(2, '0')}
                        </span>
                    )}
                    {(uploading || isUploading) && (
                        <span className="uploading-indicator">
                            <span className="spinner-small"></span>
                            Загрузка...
                        </span>
                    )}
                </div>
                <textarea
                    value={newMessage}
                    onChange={(e) => setNewMessage(e.target.value)}
                    onKeyDown={handleKeyPress}
                    placeholder="Введите сообщение..."
                    rows="1"
                    disabled={isUploading}
                />
                <button
                    onClick={handleSend}
                    disabled={isUploading || (!newMessage.trim() && !selectedFile)}
                >
                    ➤
                </button>
            </div>
        </div>
    );
};

export default ChatRoom;