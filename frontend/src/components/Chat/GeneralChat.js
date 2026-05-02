import React, { useState, useEffect, useRef } from 'react';
import useChat from './useChat';
import ChatRoom from './ChatRoom';

const GeneralChat = ({ useApi = true }) => {
    // Доступные комнаты чата
    const availableRooms = [
        { id: 'user1', label: 'Новички' },
        { id: 'user2', label: 'Продвинутые' }
    ];

    // Названия комнат
    const roomTitles = {
        user1: 'Чат новичков',
        user2: 'Чат продвинутых'
    };

    const {
        messages,
        loading,
        currentRoom,
        canSwitch,
        currentUserAuthor,
        isRecording,
        recordingTime,
        uploading,
        changeRoom,
        sendMessage,
        editMessage,
        deleteMessage,
        uploadFile,
        uploadMedia,
        startRecording,
        stopRecording,
        getMediaUrl
    } = useChat('user1', {
        storageKeyPrefix: 'generalChatMessages',
        canSwitchRooms: true,
        availableRooms,
        useApi
    });

    // Обработка загрузки файла (с API)
    const handleFileUpload = async (file) => {
        if (useApi) {
            try {
                // Загружаем медиа на сервер
                const mediaResult = await uploadMedia(file);

                // Отправляем сообщение с mediaId
                await sendMessage('', mediaResult.mediaId);

                return mediaResult;
            } catch (error) {
                console.error('Failed to upload file:', error);
                throw error;
            }
        } else {
            // LocalStorage режим
            return uploadFile(file);
        }
    };

    if (loading) {
        return (
            <div className="loading-container" style={{
                display: 'flex',
                justifyContent: 'center',
                alignItems: 'center',
                height: '400px'
            }}>
                <div className="loading">Загрузка чата...</div>
            </div>
        );
    }

    return (
        <ChatRoom
            messages={messages}
            currentUserAuthor={currentUserAuthor}
            currentRoom={currentRoom}
            canSwitch={canSwitch}
            availableRooms={availableRooms}
            roomTitles={roomTitles}
            onRoomChange={changeRoom}
            onSendMessage={(content) => sendMessage(content)}
            onEditMessage={editMessage}
            onDeleteMessage={deleteMessage}
            onFileUpload={handleFileUpload}
            onStartRecording={startRecording}
            onStopRecording={stopRecording}
            isRecording={isRecording}
            recordingTime={recordingTime}
            uploading={uploading}
        />
    );
};

export default GeneralChat;