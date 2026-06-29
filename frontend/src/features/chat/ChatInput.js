import React, { useState, useRef } from 'react';

/**
 * ChatInput — textarea + attach/voice/send controls.
 *
 * Handles file preview, voice recording UI, and message dispatch.
 */
const ChatInput = ({
  isConnected,
  onSend,
  onUploadFile,
  onStartRecording,
  onStopRecording,
  isRecording,
  recordingTime,
  uploading,
}) => {
  const [text, setText] = useState('');
  const [selectedFile, setSelectedFile] = useState(null);
  const [filePreview, setFilePreview] = useState(null);
  const [mediaType, setMediaType] = useState(null);
  const [isUploading, setIsUploading] = useState(false);
  const [error, setError] = useState(null);
  const fileInputRef = useRef(null);

  const getMediaType = (file) => {
    if (file.type.startsWith('image/')) return 'image';
    if (file.type.startsWith('video/')) return 'video';
    if (file.type.startsWith('audio/')) return 'audio';
    return 'file';
  };

  const handleFileSelect = (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (file.size > 50 * 1024 * 1024) {
      setError('Файл слишком большой. Максимум 50MB');
      return;
    }
    setError(null);
    setSelectedFile(file);
    setMediaType(getMediaType(file));

    if (file.type.startsWith('image/') || file.type.startsWith('video/') || file.type.startsWith('audio/')) {
      const reader = new FileReader();
      reader.onload = (ev) => setFilePreview(ev.target.result);
      reader.readAsDataURL(file);
    } else {
      setFilePreview(null);
    }
  };

  const cancelFile = () => {
    setSelectedFile(null);
    setFilePreview(null);
    setMediaType(null);
    setError(null);
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const handleSend = async () => {
    if (!text.trim() && !selectedFile) return;

    let mediaId = null;
    if (selectedFile) {
      setIsUploading(true);
      try {
        const result = await onUploadFile(selectedFile);
        mediaId = result.mediaId;
      } catch {
        setError('Не удалось загрузить файл');
        setIsUploading(false);
        return;
      }
      setIsUploading(false);
    }

    await onSend(text, mediaId);
    setText('');
    cancelFile();
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleVoice = async () => {
    if (isRecording) {
      onStopRecording();
    } else {
      const blob = await onStartRecording();
      if (blob) {
        setIsUploading(true);
        try {
          const result = await onUploadFile(new File([blob], 'voice.webm', { type: 'audio/webm' }));
          await onSend('Голосовое сообщение', result.mediaId);
        } catch {
          setError('Не удалось загрузить голосовое');
        }
        setIsUploading(false);
      }
    }
  };

  const busy = uploading || isUploading;

  return (
    <>
      {/* File preview */}
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
                <span>⚠️</span> {error}
                <button onClick={() => setError(null)} style={{ marginLeft: 'auto', background: 'none', border: 'none', cursor: 'pointer' }}>
                  ×
                </button>
              </div>
            )}
          </div>
          <button className="cancel-file-btn" onClick={cancelFile}>✕</button>
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
            onClick={handleVoice}
            title={isRecording ? 'Остановить запись' : 'Записать голосовое'}
            disabled={busy || !onStartRecording || !onStopRecording}
          >
            {isRecording ? '⏹️' : '🎤'}
          </button>
          {isRecording && (
            <span className="recording-time">
              {Math.floor(recordingTime / 60)}:{String(recordingTime % 60).padStart(2, '0')}
            </span>
          )}
          {busy && (
            <span className="uploading-indicator">
              <span className="spinner-small" /> Загрузка...
            </span>
          )}
        </div>
        <textarea
          value={text}
          onChange={(e) => setText(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder={isConnected ? 'Введите сообщение...' : 'Отключён...'}
          rows="1"
          disabled={!isConnected || busy}
        />
        <button onClick={handleSend} disabled={busy || (!text.trim() && !selectedFile)}>
          ➤
        </button>
      </div>

      {error && !selectedFile && (
        <div className="error-message" style={{ padding: '8px 12px' }}>
          <span>⚠️</span> {error}
          <button onClick={() => setError(null)} style={{ marginLeft: 'auto', background: 'none', border: 'none', cursor: 'pointer' }}>
            ×
          </button>
        </div>
      )}
    </>
  );
};

export default ChatInput;
