// components/Dashboard/AdminPostCreator.js
import React, { useState, useRef } from 'react';
import { postsApi } from '../../services/api';
import './AdminPostCreator.css';

const AdminPostCreator = ({ onPostCreated }) => {
    const [content, setContent] = useState('');
    const [mediaFiles, setMediaFiles] = useState([]);
    const [mediaPreviews, setMediaPreviews] = useState([]);
    const [mediaType, setMediaType] = useState(null);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [uploadProgress, setUploadProgress] = useState(0);
    const fileInputRef = useRef(null);

    const handleFileSelect = (e) => {
        const files = Array.from(e.target.files);
        const newPreviews = [];
        const newFiles = [];

        files.forEach(file => {
            const type = file.type.startsWith('image/') ? 'image' :
                        file.type.startsWith('video/') ? 'video' : null;

            if (type) {
                newFiles.push(file);
                setMediaType(prev => prev || type);

                const url = URL.createObjectURL(file);
                newPreviews.push({ url, type, file });
            }
        });

        setMediaFiles(prev => [...prev, ...newFiles]);
        setMediaPreviews(prev => [...prev, ...newPreviews]);
    };

    const removeMedia = (index) => {
        URL.revokeObjectURL(mediaPreviews[index].url);
        setMediaPreviews(prev => prev.filter((_, i) => i !== index));
        setMediaFiles(prev => prev.filter((_, i) => i !== index));

        setMediaType(prev => {
            if (mediaPreviews.length <= 1) return null;
            return prev;
        });
    };

    const handleSubmit = async () => {
        if (!content.trim() && mediaFiles.length === 0) return;

        setIsSubmitting(true);
        setUploadProgress(0);

        try {
            let mediaUrl = null;
            let mediaTypeValue = null;

            // Загружаем медиа если есть
            if (mediaFiles.length > 0) {
                const formData = new FormData();
                mediaFiles.forEach(file => {
                    formData.append('media', file);
                });
                formData.append('type', mediaType);

                const uploadResponse = await postsApi.uploadMedia(formData, (progressEvent) => {
                    const percent = progressEvent.total
                        ? Math.round((progressEvent.loaded * 100) / progressEvent.total)
                        : 0;
                    setUploadProgress(percent);
                });
                mediaUrl = uploadResponse.data.url;
                mediaTypeValue = uploadResponse.data.type;
            }

            // Создаём пост
            const response = await postsApi.create({
                content: content.trim(),
                mediaUrl,
                mediaType: mediaTypeValue
            });

            // Очищаем форму
            setContent('');
            // Revoke blob URLs
            mediaPreviews.forEach(p => URL.revokeObjectURL(p.url));
            setMediaFiles([]);
            setMediaPreviews([]);
            setMediaType(null);
            setUploadProgress(0);

            // Уведомляем родителя
            onPostCreated(response.data);

        } catch (error) {
            console.error('Failed to create post:', error);
            alert('Ошибка при публикации. Попробуйте ещё раз.');
        } finally {
            setIsSubmitting(false);
        }
    };

    const triggerFileInput = () => {
        fileInputRef.current.click();
    };

    return (
        <div className="admin-post-creator">
            <div className="creator-header">
                <span className="creator-icon">✍️</span>
                <span className="creator-title">Создать публикацию</span>
                <span className="creator-badge">только для админов</span>
            </div>

            <textarea
                className="creator-input"
                placeholder="Напишите текст публикации..."
                value={content}
                onChange={(e) => setContent(e.target.value)}
                rows={3}
            />

            {/* Превью медиа */}
            {mediaPreviews.length > 0 && (
                <div className="creator-media-previews">
                    {mediaPreviews.map((preview, idx) => (
                        <div key={idx} className="creator-media-preview">
                            {preview.type === 'image' ? (
                                <img src={preview.url} alt="preview" />
                            ) : (
                                <video src={preview.url} />
                            )}
                            <button
                                className="creator-media-remove"
                                onClick={() => removeMedia(idx)}
                                title="Удалить"
                            >
                                ✕
                            </button>
                        </div>
                    ))}
                </div>
            )}

            {/* Прогресс загрузки */}
            {isSubmitting && uploadProgress > 0 && uploadProgress < 100 && (
                <div className="creator-progress">
                    <div className="creator-progress-bar" style={{ width: `${uploadProgress}%` }} />
                    <span className="creator-progress-text">{uploadProgress}%</span>
                </div>
            )}

            {/* Кнопки действий */}
            <div className="creator-actions">
                <div className="creator-media-buttons">
                    <button
                        type="button"
                        className="creator-media-btn"
                        onClick={triggerFileInput}
                        disabled={isSubmitting}
                        title="Добавить изображение или видео"
                    >
                        📎
                    </button>
                    <input
                        ref={fileInputRef}
                        type="file"
                        accept="image/*,video/*"
                        multiple
                        onChange={handleFileSelect}
                        style={{ display: 'none' }}
                    />
                </div>

                <button
                    className="creator-submit"
                    onClick={handleSubmit}
                    disabled={isSubmitting || (!content.trim() && mediaFiles.length === 0)}
                >
                    {isSubmitting ? 'Публикация...' : '📢 Опубликовать'}
                </button>
            </div>
        </div>
    );
};

export default AdminPostCreator;