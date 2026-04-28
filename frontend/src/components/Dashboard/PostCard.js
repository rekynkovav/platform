// components/Dashboard/PostCard.js
import React, { useState } from 'react';
import Comments from './Comments';
import { reactionsApi, postsApi } from '../../services/api';
import './PostCard.css';

const PostCard = ({ post, isAdmin, onPostUpdated, onPostDeleted, index }) => {
    const [showComments, setShowComments] = useState(false);
    const [reactions, setReactions] = useState({
        likes: post.likesCount || 0,
        comments: post.commentsCount || 0,
        shares: post.sharesCount || 0,
        userLiked: post.userLiked || false
    });
    const [isReacting, setIsReacting] = useState(false);

    // Состояния для редактирования
    const [showEditModal, setShowEditModal] = useState(false);
    const [editContent, setEditContent] = useState('');
    const [editMediaUrl, setEditMediaUrl] = useState('');
    const [editMediaType, setEditMediaType] = useState('');

    // Проверка прав на редактирование и удаление
    const demoRole = localStorage.getItem('demoRole');
    const canEdit = demoRole === 'admin-moderator' || demoRole === 'developer' || demoRole === 'super-admin';

    // Отладка
    console.log('[PostCard] demoRole:', demoRole);
    console.log('[PostCard] canEdit:', canEdit);

    const formatDate = (dateString) => {
        const date = new Date(dateString);
        const now = new Date();
        const diffMs = now - date;
        const diffMins = Math.floor(diffMs / 60000);
        const diffHours = Math.floor(diffMs / 3600000);
        const diffDays = Math.floor(diffMs / 86400000);

        if (diffMins < 1) return 'только что';
        if (diffMins < 60) return `${diffMins} мин назад`;
        if (diffHours < 24) return `${diffHours} ч назад`;
        if (diffDays < 7) return `${diffDays} д назад`;
        return date.toLocaleDateString('ru-RU', { day: 'numeric', month: 'short' });
    };

    const handleLike = async () => {
        if (isReacting) return;
        setIsReacting(true);

        try {
            const response = await reactionsApi.toggle(post.id, 'like');
            setReactions(prev => ({
                ...prev,
                likes: response.data.likesCount,
                userLiked: response.data.userLiked
            }));
        } catch (error) {
            console.error('Failed to toggle like:', error);
        } finally {
            setIsReacting(false);
        }
    };

    const handleShare = () => {
        const url = `${window.location.origin}/post/${post.id}`;
        navigator.clipboard.writeText(url);
        alert('Ссылка скопирована!');

        // Обновляем счётчик шаров
        setReactions(prev => ({ ...prev, shares: prev.shares + 1 }));
    };

    const handleCommentAdded = () => {
        setReactions(prev => ({ ...prev, comments: prev.comments + 1 }));
        if (onPostUpdated) {
            onPostUpdated({ ...post, commentsCount: reactions.comments + 1 });
        }
    };

    const handleDelete = async () => {
        if (!canEdit) {
            alert('У вас нет прав на удаление');
            return;
        }
        if (window.confirm('Удалить эту публикацию?')) {
            try {
                await postsApi.delete(post.id);
                if (onPostDeleted) onPostDeleted(post.id);
            } catch (error) {
                console.error('Failed to delete post:', error);
                alert('Ошибка при удалении');
            }
        }
    };

    const handleEdit = () => {
        if (!canEdit) {
            alert('У вас нет прав на редактирование');
            return;
        }
        setEditContent(post.content || '');
        setEditMediaUrl(post.mediaUrl || '');
        setEditMediaType(post.mediaType || '');
        setShowEditModal(true);
    };

    const handleSaveEdit = async () => {
        if (!editContent.trim()) {
            alert('Содержимое поста не может быть пустым');
            return;
        }

        try {
            const updatedPost = {
                ...post,
                content: editContent,
                mediaUrl: editMediaUrl,
                mediaType: editMediaType,
                edited: true
            };

            await postsApi.update(post.id, updatedPost);

            if (onPostUpdated) {
                onPostUpdated(updatedPost);
            }

            setShowEditModal(false);
        } catch (error) {
            console.error('Failed to update post:', error);
            alert('Ошибка при обновлении поста');
        }
    };

    const handleCancelEdit = () => {
        setShowEditModal(false);
        setEditContent('');
        setEditMediaUrl('');
        setEditMediaType('');
    };

    const handleFileUpload = (e) => {
        const file = e.target.files[0];
        if (file) {
            const url = URL.createObjectURL(file);
            setEditMediaUrl(url);

            if (file.type.startsWith('image/')) {
                setEditMediaType('image');
            } else if (file.type.startsWith('video/')) {
                setEditMediaType('video');
            }
        }
    };

    return (
        <>
            <div className="post-card fade-in" style={{ animationDelay: `${index * 0.05}s` }}>
                <div className="post-header">
                    <div className="post-author">
                        <span className="post-author-avatar">👤</span>
                        <div className="post-author-info">
                            <span className="post-author-name">{post.authorName || 'Admin'}</span>
                            <span className="post-author-role">
                                {post.authorRole === 'admin' ? 'Администратор' : 'Модератор'}
                            </span>
                        </div>
                    </div>
                    <div className="post-date">
                        {formatDate(post.createdAt)}
                        {post.edited && <span className="edited-badge-post"> (изменено)</span>}
                    </div>
                    {canEdit && (
                        <div className="post-actions-header">
                            <button className="post-edit" onClick={handleEdit} title="Редактировать">
                                ✏️
                            </button>
                            <button className="post-delete" onClick={handleDelete} title="Удалить">
                                🗑️
                            </button>
                        </div>
                    )}
                </div>

                <div className="post-content">
                    <p className="post-text">{post.content}</p>

                    {post.mediaUrl && (
                        <div className="post-media">
                            {post.mediaType === 'image' ? (
                                <img src={post.mediaUrl} alt="post media" loading="lazy" />
                            ) : post.mediaType === 'video' ? (
                                <video src={post.mediaUrl} controls preload="metadata" />
                            ) : null}
                        </div>
                    )}
                </div>

                <div className="post-actions">
                    <button
                        className={`post-action like ${reactions.userLiked ? 'active' : ''}`}
                        onClick={handleLike}
                        disabled={isReacting}
                    >
                        ❤️ <span>{reactions.likes}</span>
                    </button>
                    <button
                        className="post-action comment"
                        onClick={() => setShowComments(!showComments)}
                    >
                        💬 <span>{reactions.comments}</span>
                    </button>
                    <button className="post-action share" onClick={handleShare}>
                        📤 <span>{reactions.shares}</span>
                    </button>
                </div>

                {showComments && (
                    <Comments
                        postId={post.id}
                        onCommentAdded={handleCommentAdded}
                    />
                )}
            </div>

            {/* Модальное окно редактирования */}
            {showEditModal && (
                <div className="modal-overlay" onClick={handleCancelEdit}>
                    <div className="modal-content edit-post-modal" onClick={(e) => e.stopPropagation()}>
                        <div className="modal-header">
                            <span className="modal-icon">✏️</span>
                            <h3>Редактировать публикацию</h3>
                        </div>
                        <div className="modal-body">
                            <textarea
                                className="edit-post-textarea"
                                placeholder="Содержимое публикации..."
                                value={editContent}
                                onChange={(e) => setEditContent(e.target.value)}
                                rows="6"
                            />

                            <div className="media-upload-section">
                                <label className="upload-media-btn">
                                    📎 Прикрепить медиа
                                    <input
                                        type="file"
                                        accept="image/*,video/*"
                                        onChange={handleFileUpload}
                                        style={{ display: 'none' }}
                                    />
                                </label>

                                {editMediaUrl && (
                                    <div className="attached-media-preview">
                                        <span className="media-info">
                                            {editMediaType === 'image' ? '🖼️ Изображение' : '🎬 Видео'}
                                        </span>
                                        <button
                                            className="remove-media-btn"
                                            onClick={() => {
                                                setEditMediaUrl('');
                                                setEditMediaType('');
                                            }}
                                        >
                                            ✕
                                        </button>
                                    </div>
                                )}
                            </div>
                        </div>
                        <div className="modal-footer">
                            <button className="modal-btn cancel" onClick={handleCancelEdit}>
                                Отмена
                            </button>
                            <button className="modal-btn confirm" onClick={handleSaveEdit}>
                                Сохранить
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </>
    );
};

export default PostCard;