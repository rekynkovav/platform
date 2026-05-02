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

    return (
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
                <div className="post-date">{formatDate(post.createdAt)}</div>
                {isAdmin && (
                    <button className="post-delete" onClick={handleDelete} title="Удалить">
                        🗑️
                    </button>
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
    );
};

export default PostCard;