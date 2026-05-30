import { useChat } from './useChat';
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
        uploading,
        error,
        sendMessage,
        editMessage,
        deleteMessage,
        uploadFile,
        startRecording,
        stopRecording,
        clearError
    } = useChat('nutrition', {
        storageKeyPrefix: 'nutritionChatMessages',
        canSwitchRooms: false,
        availableRooms,
        useApi: false // Временно отключаем API до запуска бэкенда
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
            error={error}
            onClearError={clearError}
        />
    );
};

export default NutritionChat;