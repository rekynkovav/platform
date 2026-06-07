import React from 'react';

const AddContentModal = ({
    isOpen,
    onClose,
    editingItem,
    selectedMenuId,
    newItemTitle,
    setNewItemTitle,
    newItemDescription,
    setNewItemDescription,
    newItemMediaUrl,
    setNewItemMediaUrl,
    newItemMediaType,
    setNewItemMediaType,
    isRecording,
    recordingTime,
    handleStartRecording,
    handleStopRecording,
    handleFileUpload,
    handleSaveContent,
    formatRecordingTime,
    resetRecording
}) => {
    if (!isOpen) return null;

    const handleOverlayClick = () => {
        if (newItemMediaUrl && newItemMediaUrl.startsWith('blob:')) {
            URL.revokeObjectURL(newItemMediaUrl);
        }
        onClose();
    };

    return (
        <div className="modal-overlay" onClick={handleOverlayClick}>
            <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                <h3>{editingItem ? 'Редактировать контент' : 'Добавить контент'}</h3>
                <input
                    type="text"
                    placeholder="Как будет отображаться"
                    value={newItemTitle}
                    onChange={(e) => setNewItemTitle(e.target.value)}
                    className="modal-input"
                />
                <textarea
                    placeholder="Сюда вставить ссылку"
                    value={newItemDescription}
                    onChange={(e) => setNewItemDescription(e.target.value)}
                    className="modal-textarea"
                />

                <div className="media-controls-dashboard">
                    <div className="media-buttons-dashboard">
                        {selectedMenuId === 'broadcasts' || selectedMenuId === 'broadcasts-guests' ? (
                            <>
                                <label className="attach-button-dashboard">
                                    Видео
                                    <input
                                        type="file"
                                        accept="video/*"
                                        onChange={(e) => handleFileUpload(e, 'video')}
                                        style={{ display: 'none' }}
                                    />
                                </label>
                                <label className="attach-button-dashboard">
                                    Аудио
                                    <input
                                        type="file"
                                        accept="audio/*"
                                        onChange={(e) => handleFileUpload(e, 'audio')}
                                        style={{ display: 'none' }}
                                    />
                                </label>
                                <button
                                    className={`mic-button-dashboard ${isRecording ? 'recording' : ''}`}
                                    onClick={isRecording ? handleStopRecording : handleStartRecording}
                                >
                                    {isRecording ? '⏹️ Остановить' : '🎤 Записать голос'}
                                </button>
                            </>
                        ) : selectedMenuId === 'music' ? (
                            <>
                                <label className="attach-button-dashboard">
                                    Аудио
                                    <input
                                        type="file"
                                        accept="audio/*"
                                        onChange={(e) => handleFileUpload(e, 'audio')}
                                        style={{ display: 'none' }}
                                    />
                                </label>
                                <button
                                    className={`mic-button-dashboard ${isRecording ? 'recording' : ''}`}
                                    onClick={isRecording ? handleStopRecording : handleStartRecording}
                                >
                                    {isRecording ? '⏹️ Остановить' : '🎤 Записать голос'}
                                </button>
                            </>
                        ) : selectedMenuId === 'movies' ? (
                            <label className="attach-button-dashboard">
                                Видео
                                <input
                                    type="file"
                                    accept="video/*"
                                    onChange={(e) => handleFileUpload(e, 'video')}
                                    style={{ display: 'none' }}
                                />
                            </label>
                        ) : (
                            <>
                                <label className="attach-button-dashboard">
                                    Фото
                                    <input
                                        type="file"
                                        accept="image/*"
                                        onChange={(e) => handleFileUpload(e, 'image')}
                                        style={{ display: 'none' }}
                                    />
                                </label>
                                <label className="attach-button-dashboard">
                                    Видео
                                    <input
                                        type="file"
                                        accept="video/*"
                                        onChange={(e) => handleFileUpload(e, 'video')}
                                        style={{ display: 'none' }}
                                    />
                                </label>
                                <label className="attach-button-dashboard">
                                    Аудио
                                    <input
                                        type="file"
                                        accept="audio/*"
                                        onChange={(e) => handleFileUpload(e, 'audio')}
                                        style={{ display: 'none' }}
                                    />
                                </label>
                                <button
                                    className={`mic-button-dashboard ${isRecording ? 'recording' : ''}`}
                                    onClick={isRecording ? handleStopRecording : handleStartRecording}
                                >
                                    {isRecording ? '⏹️ Остановить' : '🎤 Записать голос'}
                                </button>
                            </>
                        )}
                    </div>

                    {isRecording && (
                        <div className="recording-indicator-dashboard">
                            <span className="recording-dot">🔴</span>
                            <span className="recording-time">{formatRecordingTime(recordingTime)}</span>
                        </div>
                    )}

                    {newItemMediaUrl && (
                        <div className="attached-file-dashboard">
                            <span className="file-info">
                                {newItemMediaType === 'image' && '📷 Фото прикреплено'}
                                {newItemMediaType === 'video' && '🎬 Видео прикреплено'}
                                {newItemMediaType === 'audio' && '🎵 Аудио прикреплено'}
                            </span>
                            <button
                                className="remove-file-dashboard"
                                onClick={() => {
                                    if (newItemMediaUrl && newItemMediaUrl.startsWith('blob:')) {
                                        URL.revokeObjectURL(newItemMediaUrl);
                                    }
                                    setNewItemMediaUrl('');
                                    setNewItemMediaType('');
                                }}
                            >
                                ✕
                            </button>
                        </div>
                    )}
                </div>

                <div className="modal-buttons">
                    <button className="modal-btn save" onClick={handleSaveContent}>
                        Сохранить
                    </button>
                    <button className="modal-btn cancel" onClick={handleOverlayClick}>
                        Отмена
                    </button>
                </div>
            </div>
        </div>
    );
};

export default AddContentModal;
