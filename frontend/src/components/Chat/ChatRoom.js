import React, { useState, useRef, useEffect } from 'react';
import MediaRenderer from './MediaRenderer';
import './Chat.css';

const ChatRoom = ({
    messages,
    currentUserAuthor,
    currentRoom,
    canSwitch = false,
    availableRooms = [],
    roomTitles = {},
    onSendMessage,
    onEditMessage,
    onDeleteMessage,
    onFileUpload,
    onStartRecording,
    onStopRecording,
    isRecording,
    recordingTime,
    uploading
}) => {
    const [inputValue, setInputValue] = useState('');
    const [selectedFile, setSelectedFile] = useState(null);
    const fileInputRef = useRef(null);
    const messagesEndRef = useRef(null);

    // Автоскролл к последнему сообщению
    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    };

    useEffect(() => {
        scrollToBottom();
    }, [messages]);

    const handleSend = async () => {
        if ((!inputValue.trim() && !selectedFile) || uploading) return;

        let mediaId = null;

        if (selectedFile && onFileUpload) {
            try {
                const result = await onFileUpload(selectedFile);
                mediaId = result.mediaId || result.id;
            } catch (error) {
                console.error('File upload failed', error);
                return;
            }
        }

        await onSendMessage(inputValue.trim(), mediaId);
        setInputValue('');
        setSelectedFile(null);
    };

    const handleKeyPress = (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSend();
        }
    };

    const handleFileSelect = (e) => {
        const file = e.target.files[0];
        if (file) setSelectedFile(file);
    };

    return (
        <div className="chat-container">
            <div className="chat-header">
                <h2>{roomTitles[currentRoom] || 'Чат'}</h2>
            </div>

            <div className="chat-messages">
                {messages.map((msg) => (
                    <div
                        key={msg.id}
                        className={`chat-message ${msg.author === currentUserAuthor ? 'own' : ''}`}
                    >
                        <strong>{msg.author}</strong>
                        <div className="chat-message-text">
                            {msg.content}
                            {msg.mediaId && (
                                <MediaRenderer
                                    mediaUrl={msg.mediaId}
                                    mediaType={msg.mediaType || 'file'}
                                    fileName={msg.fileName}
                                />
                            )}
                        </div>
                        <span className="timestamp">
                            {new Date(msg.timestamp).toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' })}
                        </span>
                    </div>
                ))}
                <div ref={messagesEndRef} />
            </div>

            <div className="chat-input">
                <input
                    type="file"
                    ref={fileInputRef}
                    style={{ display: 'none' }}
                    onChange={handleFileSelect}
                />
                <button onClick={() => fileInputRef.current.click()} disabled={uploading}>
                    📎
                </button>

                <textarea
                    value={inputValue}
                    onChange={(e) => setInputValue(e.target.value)}
                    onKeyPress={handleKeyPress}
                    placeholder="Напишите сообщение..."
                    rows="1"
                />

                <button onClick={handleSend} disabled={uploading || (!inputValue.trim() && !selectedFile)}>
                    {uploading ? '⏳' : '➤'}
                </button>
            </div>
        </div>
    );
};

export default ChatRoom;