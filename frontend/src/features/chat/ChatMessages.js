import React, { useRef, useEffect } from 'react';
import MediaRenderer from '../../components/Chat/MediaRenderer';

/**
 * ChatMessages — scrollable message list with avatars, edit/delete, media.
 */
const ChatMessages = ({ messages, username, loading, onEdit, onDelete }) => {
  const endRef = useRef(null);

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const renderMedia = (msg) => {
    if (msg.mediaId) {
      return (
        <MediaRenderer
          mediaUrl={`/api/media/stream/${msg.mediaId}`}
          mediaType={msg.mediaType || 'file'}
          fileName={msg.fileName}
        />
      );
    }
    if (msg.mediaUrl) {
      return (
        <MediaRenderer
          mediaUrl={msg.mediaUrl}
          mediaType={msg.mediaType || 'file'}
          fileName={msg.fileName}
        />
      );
    }
    if (msg.fileUrl) {
      return (
        <MediaRenderer
          mediaUrl={msg.fileUrl}
          mediaType={msg.fileType || msg.type}
          fileName={msg.fileName}
        />
      );
    }
    return null;
  };

  return (
    <div className="chat-messages">
      {loading && (
        <div style={{ textAlign: 'center', padding: '40px', color: 'var(--text-secondary)' }}>
          Загрузка...
        </div>
      )}

      {messages.map((msg, index) => (
        <div
          key={msg.id || `${msg.createdAt || msg.timestamp}-${index}`}
          className={`chat-message ${msg.author === username ? 'own' : ''} ${msg.pending ? 'pending' : ''}`}
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
                {msg.author === 'Система' ? '🤖' : msg.author === 'Админ' ? '🛡️' : '👤'}
              </span>
            </div>
          </div>

          <div className="message-content-wrapper">
            <div className="message-header">
              <strong>{msg.author}</strong>
              <span className="timestamp">
                {new Date(msg.timestamp || msg.createdAt).toLocaleTimeString('ru-RU', {
                  hour: '2-digit',
                  minute: '2-digit',
                })}
                {msg.edited && <span className="edited-label"> (изменено)</span>}
              </span>
              {msg.pending && <span className="pending-label">Отправляется...</span>}
              {msg.author === username && !msg.pending && (
                <div className="message-actions">
                  <button
                    className="edit-message-btn"
                    onClick={() => onEdit(msg)}
                    title="Редактировать"
                  >
                    ✏️
                  </button>
                  <button
                    className="delete-message-btn"
                    onClick={() => onDelete(msg.id)}
                    title="Удалить"
                  >
                    🗑️
                  </button>
                </div>
              )}
            </div>

            {msg.content && <p className="message-content">{msg.content}</p>}

            {(msg.fileUrl || msg.mediaUrl || msg.mediaId) && (
              <div className="message-media">{renderMedia(msg)}</div>
            )}
          </div>
        </div>
      ))}

      <div ref={endRef} />
    </div>
  );
};

export default ChatMessages;
