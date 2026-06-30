import React, { useState } from 'react';
import { usePermissions } from '../../hooks/usePermissions';
import { CHAT_CONFIG } from './config';
import useChatRoom from './hooks/useChatRoom';
import useChatMedia from './hooks/useChatMedia';
import ChatHeader from './ChatHeader';
import ChatMessages from './ChatMessages';
import ChatInput from './ChatInput';
import '../../components/Chat/Chat.css';

/**
 * ChatPage — unified config-driven chat component.
 *
 * Replaces GeneralChat + NutritionChat + ChatRoom + useChat (786 lines).
 *
 * Usage:
 *   <ChatPage configKey="general" />   → /chat
 *   <ChatPage configKey="advanced" />  → /chat/advanced
 *   <ChatPage configKey="nutrition" /> → /chat/nutrition
 */
const ChatPage = ({ configKey }) => {
  const { hasLevel } = usePermissions();
  const config = CHAT_CONFIG[configKey];

  // Filter rooms by user permission level
  const availableRooms = config.rooms.filter((r) => hasLevel(r.minLevel));
  const canSwitch = config.showSwitcher && availableRooms.length > 1;

  const [activeSlug, setActiveSlug] = useState(
    () => (canSwitch ? config.defaultRoom : availableRooms[0]?.slug) || config.defaultRoom,
  );

  const {
    messages,
    loading,
    error,
    isConnected,
    username,
    sendMessage,
    editMessage,
    deleteMessage,
    clearError,
  } = useChatRoom(activeSlug);

  const {
    uploadFile,
    isRecording,
    recordingTime,
    uploading,
    startRecording,
    stopRecording,
  } = useChatMedia();

  const [editingMsg, setEditingMsg] = useState(null);

  const handleRoomSwitch = (slug) => {
    if (slug !== activeSlug) setActiveSlug(slug);
  };

  const handleEdit = (msg) => {
    setEditingMsg(msg);
  };

  const handleSaveEdit = async (content) => {
    if (!content.trim()) return;
    await editMessage(editingMsg.id, content);
    setEditingMsg(null);
  };

  return (
    <div className="chat-container">
      {/* Room switcher */}
      {canSwitch && (
        <div className="chat-room-switcher">
          {availableRooms.map((room) => (
            <button
              key={room.slug}
              className={`chat-room-btn ${activeSlug === room.slug ? 'active' : ''}`}
              onClick={() => handleRoomSwitch(room.slug)}
            >
              {room.label}
            </button>
          ))}
        </div>
      )}

      {/* Error bar */}
      {error && (
        <div className="error-message">
          <span>⚠️</span> {error}
          <button
            onClick={clearError}
            className="error-close-btn"
          >
            ×
          </button>
        </div>
      )}

      <ChatHeader slug={activeSlug} isConnected={isConnected} />

      <ChatMessages
        messages={messages}
        username={username}
        loading={loading}
        onEdit={handleEdit}
        onDelete={deleteMessage}
      />

      <ChatInput
        isConnected={isConnected}
        onSend={sendMessage}
        onUploadFile={uploadFile}
        onStartRecording={startRecording}
        onStopRecording={stopRecording}
        isRecording={isRecording}
        recordingTime={recordingTime}
        uploading={uploading}
      />

      {/* Edit modal */}
      {editingMsg && (
        <div className="modal-overlay" onClick={() => setEditingMsg(null)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <h3>Редактировать сообщение</h3>
            <EditForm
              initial={editingMsg.content}
              onSave={handleSaveEdit}
              onCancel={() => setEditingMsg(null)}
            />
          </div>
        </div>
      )}
    </div>
  );
};

/** Tiny inline edit form for message editing. */
const EditForm = ({ initial, onSave, onCancel }) => {
  const [value, setValue] = React.useState(initial);
  return (
    <div className="edit-message-form">
      <textarea
        value={value}
        onChange={(e) => setValue(e.target.value)}
        rows={3}
        autoFocus
      />
      <div className="edit-actions">
        <button onClick={() => onSave(value)}>Сохранить</button>
        <button onClick={onCancel}>Отмена</button>
      </div>
    </div>
  );
};

export default ChatPage;
