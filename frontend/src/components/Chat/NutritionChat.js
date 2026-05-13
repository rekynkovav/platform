import React from 'react';
import useChat from './useChat';
import ChatRoom from './ChatRoom';

const NutritionChat = () => {
    const {
        messages,
        loading,
        currentUserAuthor,
        isRecording,
        recordingTime,
        uploading,
        sendMessage,
        editMessage,
        deleteMessage,
        uploadMedia,
        startRecording,
        stopRecording
    } = useChat('nutrition', {
        storageKeyPrefix: 'nutritionChatMessages',
        canSwitchRooms: false,
        useApi: true
    });

    const handleFileUpload = async (file) => {
        try {
            const mediaResult = await uploadMedia(file);
            await sendMessage('', mediaResult.mediaId || mediaResult.id);
            return mediaResult;
        } catch (error) {
            console.error('Failed to upload file in nutrition chat:', error);
            throw error;
        }
    };

    if (loading) {
        return <div className="loading">Загрузка чата питания...</div>;
    }

    return (
        <ChatRoom
            messages={messages}
            currentUserAuthor={currentUserAuthor}
            currentRoom="nutrition"
            canSwitch={false}
            availableRooms={[]}
            roomTitles={{ nutrition: 'Чат Питание' }}
            onSendMessage={sendMessage}
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

export default NutritionChat;