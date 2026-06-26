import React, { useState, useRef, useEffect } from 'react';
import './ContentModal.css';

/**
 * ContentModal — modal dialog for creating or editing content entries with media attachments.
 *
 * Provides a form with title, description, file upload (image/video/audio), and
 * inline audio recording. Handles blob URL lifecycle and cleans up resources on close.
 *
 * @param {boolean} isOpen - Whether the modal is currently visible
 * @param {() => void} onClose - Callback to close the modal
 * @param {(data: { title: string, description: string, mediaUrl: string, mediaType: string }) => void} onSave - Callback with the saved content data
 * @param {string} [initialTitle=''] - Pre-filled title (for editing)
 * @param {string} [initialDescription=''] - Pre-filled description (for editing)
 * @returns {JSX.Element|null} The modal overlay with form, or null if closed
 */
const ContentModal = ({ isOpen, onClose, onSave, initialTitle = '', initialDescription = '' }) => {
    const [title, setTitle] = useState(initialTitle);
    const [description, setDescription] = useState(initialDescription);
    const [mediaUrl, setMediaUrl] = useState('');
    const [mediaType, setMediaType] = useState('');
    const [isRecording, setIsRecording] = useState(false);
    const [mediaRecorder, setMediaRecorder] = useState(null);
    const [recordingTime, setRecordingTime] = useState(0);
    const [error, setError] = useState(null);
    const recordingTimerRef = useRef(null);
    const blobUrlsRef = useRef([]);

    useEffect(() => {
        setTitle(initialTitle);
        setDescription(initialDescription);
    }, [initialTitle, initialDescription]);

    useEffect(() => {
        if (!isOpen) {
            // Останавливаем запись при закрытии
            if (isRecording && mediaRecorder) {
                mediaRecorder.stop();
                setIsRecording(false);
            }
            if (recordingTimerRef.current) {
                clearInterval(recordingTimerRef.current);
                recordingTimerRef.current = null;
            }
            blobUrlsRef.current.forEach(url => URL.revokeObjectURL(url));
            blobUrlsRef.current = [];
            setTitle('');
            setDescription('');
            setMediaUrl('');
            setMediaType('');
            setRecordingTime(0);
        }
    }, [isOpen, isRecording, mediaRecorder]);

    // Cleanup при размонтировании
    useEffect(() => {
        return () => {
            if (recordingTimerRef.current) {
                clearInterval(recordingTimerRef.current);
            }
            blobUrlsRef.current.forEach(url => URL.revokeObjectURL(url));
            blobUrlsRef.current = [];
        };
    }, []);

    const clearError = () => {
        setError(null);
    };

    const handleFileUpload = (e) => {
        const file = e.target.files[0];
        if (file) {
            const url = URL.createObjectURL(file);
            blobUrlsRef.current.push(url);
            setMediaUrl(url);

            if (file.type.startsWith('image/')) {
                setMediaType('image');
            } else if (file.type.startsWith('video/')) {
                setMediaType('video');
            } else if (file.type.startsWith('audio/')) {
                setMediaType('audio');
            }
        }
    };

    const startRecording = async () => {
        try {
            const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
            const recorder = new MediaRecorder(stream);
            const chunks = [];

            recorder.ondataavailable = (e) => {
                if (e.data.size > 0) {
                    chunks.push(e.data);
                }
            };

            recorder.onstop = () => {
                const audioBlob = new Blob(chunks, { type: 'audio/webm' });
                const audioUrl = URL.createObjectURL(audioBlob);
                blobUrlsRef.current.push(audioUrl);
                setMediaUrl(audioUrl);
                setMediaType('audio');
                stream.getTracks().forEach(track => track.stop());
            };

            recorder.start();
            setMediaRecorder(recorder);
            setIsRecording(true);

            setRecordingTime(0);
            recordingTimerRef.current = setInterval(() => {
                setRecordingTime(prev => prev + 1);
            }, 1000);
        } catch (err) {
            console.error('Ошибка доступа к микрофону:', err);
            setError('Не удалось получить доступ к микрофону');
        }
    };

    const stopRecording = () => {
        if (mediaRecorder && isRecording) {
            mediaRecorder.stop();
            setIsRecording(false);
            if (recordingTimerRef.current) {
                clearInterval(recordingTimerRef.current);
            }
        }
    };

    const formatRecordingTime = (seconds) => {
        const mins = Math.floor(seconds / 60);
        const secs = seconds % 60;
        return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    };

    const handleSave = () => {
        if (!title.trim()) {
            alert('Заполните заголовок');
            return;
        }

        onSave({
            title,
            description,
            mediaUrl,
            mediaType
        });

        setTitle('');
        setDescription('');
        setMediaUrl('');
        setMediaType('');
        // Revoke blob URLs after save
        blobUrlsRef.current.forEach(url => URL.revokeObjectURL(url));
        blobUrlsRef.current = [];
    };

    if (!isOpen) return null;

    return (
        <div className="content-modal-overlay" onClick={onClose}>
            <div className="content-modal" onClick={(e) => e.stopPropagation()}>
                <h3>Добавить контент</h3>

                {/* Отображение ошибок */}
                {error && (
                    <div className="error-message">
                        <span className="error-icon">⚠️</span>
                        <span>{error}</span>
                        <button className="error-close" onClick={clearError}>×</button>
                    </div>
                )}

                <input
                    type="text"
                    placeholder="Название"
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                    className="content-modal-input"
                />

                <textarea
                    placeholder="Описание"
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    className="content-modal-textarea"
                />

                <div className="content-modal-media-controls">
                    <div className="content-modal-media-buttons">
                        <label className="content-modal-attach-button">
                            📎 Прикрепить файл
                            <input
                                type="file"
                                accept="image/*,video/*,audio/*"
                                onChange={handleFileUpload}
                                style={{ display: 'none' }}
                            />
                        </label>

                        <button
                            className={`content-modal-mic-button ${isRecording ? 'recording' : ''}`}
                            onClick={isRecording ? stopRecording : startRecording}
                        >
                            {isRecording ? '⏹️ Остановить' : '🎤 Записать Аудио'}
                        </button>
                    </div>

                    {isRecording && (
                        <div className="content-modal-recording-indicator">
                            <span className="content-modal-recording-dot">🔴</span>
                            <span className="content-modal-recording-time">{formatRecordingTime(recordingTime)}</span>
                        </div>
                    )}

                    {mediaUrl && (
                        <div className="content-modal-attached-file">
                            <span className="content-modal-file-info">
                                {mediaType === 'image' && '📷 Фото прикреплено'}
                                {mediaType === 'video' && '🎬 Видео прикреплено'}
                                {mediaType === 'audio' && '🎵 Аудио прикреплено'}
                            </span>
                            <button
                                className="content-modal-remove-file"
                                onClick={() => {
                                    if (mediaUrl && mediaUrl.startsWith('blob:')) URL.revokeObjectURL(mediaUrl);
                                    setMediaUrl('');
                                    setMediaType('');
                                }}
                            >
                                ✕
                            </button>
                        </div>
                    )}
                </div>

                <div className="content-modal-buttons">
                    <button className="content-modal-btn save" onClick={handleSave}>
                        Сохранить
                    </button>
                    <button className="content-modal-btn cancel" onClick={onClose}>
                        Отмена
                    </button>
                </div>
            </div>
        </div>
    );
};

export default ContentModal;
