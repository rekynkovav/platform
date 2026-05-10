// components/Dashboard/CommentItem.js
import React, { useState } from 'react';
import { commentsApi } from '../../services/api';
import { getUserId, getUserRoles } from '../../keycloak';
import './CommentItem.css';

const CommentItem = ({ comment, onDeleted }) => {
    const [isDeleting, setIsDeleting] = useState(false);
    const currentUserId = getUserId();
    const userRoles = getUserRoles();
    const isAdmin = userRoles.includes('MODERATOR_ADMIN') || userRoles.includes('SUPER_ADMIN') || userRoles.includes('VYACHESLAV');
    const canDelete = isAdmin || comment.authorId === currentUserId;

    const formatDate = (dateString) => {
        const date = new Date(dateString);
        const now = new Date();
        const diffMins = Math.floor((now - date) / 60000);

        if (diffMins < 1) return 'только что';
        if (diffMins < 60) return `${diffMins} мин назад`;
        if (diffMins < 1440) return `${Math.floor(diffMins / 60)} ч назад`;
        return date.toLocaleDateString('ru-RU', { day: 'numeric', month: 'short' });
    };

    const handleDelete = async () => {
        if (!window.confirm('Удалить комментарий?')) return;

        setIsDeleting(true);
        try {
            await commentsApi.delete(comment.postId, comment.id);
            if (onDeleted) onDeleted(comment.id);
        } catch (error) {
            console.error('Failed to delete comment:', error);
            alert('Ошибка при удалении');
        } finally {
            setIsDeleting(false);
        }
    };

    return (
        <div className={`comment-item ${isDeleting ? 'deleting' : ''}`}>
            <div className="comment-avatar">
                <span className="comment-avatar-icon">👤</span>
            </div>
            <div className="comment-content">
                <div className="comment-header">
                    <span className="comment-author">{comment.authorName || 'Пользователь'}</span>
                    <span className="comment-date">{formatDate(comment.createdAt)}</span>
                    {canDelete && (
                        <button
                            className="comment-delete"
                            onClick={handleDelete}
                            disabled={isDeleting}
                            title="Удалить"
                        >
                            🗑️
                        </button>
                    )}
                </div>
                <p className="comment-text">{comment.content}</p>
            </div>
        </div>
    );
};

export default CommentItem;