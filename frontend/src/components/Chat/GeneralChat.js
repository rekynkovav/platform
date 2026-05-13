import React from 'react';
import useChat from './useChat';
import ChatRoom from './ChatRoom';

const GeneralChat = () => {
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
    } = useChat('general', {
        useApi: true
    });

    const handleFileUpload = async (file) => {
        try {
            const mediaResult = await uploadMedia(file);
            return mediaResult;
        } catch (error) {
            console.error('File upload failed:', error);
            throw error;
        }
    };

    if (loading) {
        return <div className="loading">Загрузка общего чата...</div>;
    }

    return (
        <ChatRoom
            messages={messages}
            currentUserAuthor={currentUserAuthor}
            currentRoom="general"
            canSwitch={false}
            availableRooms={[]}
            roomTitles={{ general: 'Общий чат' }}
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

export default GeneralChat;