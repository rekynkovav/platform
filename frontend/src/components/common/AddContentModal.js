import React from 'react';

/**
 * AddContentModal — modal dialog for adding or editing content items in the Dashboard.
 *
 * Provides a form with title, description, media URL, media type selection,
 * inline audio recording with time display, and file upload support. Handles
 * blob URL cleanup on close and delegates save/cancel actions to parent callbacks.
 *
 * @param {boolean} isOpen - Whether the modal is currently visible
 * @param {() => void} onClose - Callback to close the modal (cleans up blob URLs)
 * @param {Object|null} editingItem - The item being edited, or null for new item creation
 * @param {string} selectedMenuId - ID of the currently selected menu section
 * @param {string} newItemTitle - Current value of the title input
 * @param {(value: string) => void} setNewItemTitle - Setter for the title input
 * @param {string} newItemDescription - Current value of the description input
 * @param {(value: string) => void} setNewItemDescription - Setter for the description input
 * @param {string} newItemMediaUrl - URL of the attached media file
 * @param {(value: string) => void} setNewItemMediaUrl - Setter for the media URL
 * @param {string} newItemMediaType - MIME type of the attached media
 * @param {(value: string) => void} setNewItemMediaType - Setter for the media type
 * @param {boolean} isRecording - Whether audio recording is currently active
 * @param {number} recordingTime - Current recording duration in seconds
 * @param {() => void} handleStartRecording - Callback to start audio recording
 * @param {() => void} handleStopRecording - Callback to stop audio recording
 * @param {(e: React.ChangeEvent<HTMLInputElement>) => void} handleFileUpload - Callback for file input change
 * @param {() => void} handleSaveContent - Callback to save the content item
 * @param {(seconds: number) => string} formatRecordingTime - Utility to format seconds as MM:SS
 * @param {() => void} resetRecording - Callback to reset recording state
 * @returns {JSX.Element|null} The modal overlay with content form, or null if closed
 */
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
