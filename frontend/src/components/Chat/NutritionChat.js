import useChat from './useChat';
import ChatRoom from './ChatRoom';

const NutritionChat = () => {
    // Одна комната - без переключения
    const availableRooms = [];

    // Название комнаты
    const roomTitles = {
        nutrition: 'Чат Питание'
    };

    const {
        messages,
        loading,
        currentRoom,
        canSwitch,
        currentUserAuthor,
        isRecording,
        recordingTime,
        sendMessage,
        editMessage,
        deleteMessage,
        uploadFile,
        startRecording,
        stopRecording
    } = useChat('nutrition', {
        storageKeyPrefix: 'nutritionChatMessages',
        canSwitchRooms: false,
        availableRooms
    });

    if (loading) return <div className="loading">Загрузка чата...</div>;

    return (
        <ChatRoom
            messages={messages}
            currentUserAuthor={currentUserAuthor}
            currentRoom={currentRoom}
            canSwitch={canSwitch}
            availableRooms={availableRooms}
            roomTitles={roomTitles}
            onSendMessage={sendMessage}
            onEditMessage={editMessage}
            onDeleteMessage={deleteMessage}
            onFileUpload={uploadFile}
            onStartRecording={startRecording}
            onStopRecording={stopRecording}
            isRecording={isRecording}
            recordingTime={recordingTime}
        />
    );
};

export default NutritionChat;