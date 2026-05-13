import React from 'react';
import './PostCard.css';

const PostCard = ({ post, onDelete, onEdit }) => {
    const demoRole = localStorage.getItem('demoRole') || 'USER_0';
    const canEdit = ['MODERATOR_ADMIN', 'SUPER_ADMIN', 'VYACHESLAV', 'DEVELOPER'].includes(demoRole);

    const handleDelete = () => {
        if (window.confirm('Вы действительно хотите удалить этот пост?')) {
            onDelete && onDelete(post.id);
        }
    };

    return (
        <div className="post-card">
            <div className="post-header">
                <div className="post-author">
                    <span className="author-name">{post.author_name || post.author || 'Пользователь'}</span>
                    <span className="author-role">{post.author_role || ''}</span>
                </div>
                <div className="post-date">
                    {post.created_at ? new Date(post.created_at).toLocaleDateString('ru-RU') : 'Недавно'}
                </div>
            </div>

            <div className="post-content">
                <p>{post.content}</p>
                {post.media_url && (
                    <img
                        src={post.media_url}
                        alt="Медиа"
                        className="post-media"
                        loading="lazy"
                    />
                )}
            </div>

            <div className="post-footer">
                <div className="post-stats">
                    <span>❤️ {post.likes_count || 0}</span>
                    <span>💬 {post.comments_count || 0}</span>
                </div>

                {canEdit && (
                    <div className="post-actions">
                        {onEdit && <button onClick={() => onEdit(post)} className="edit-btn">Редактировать</button>}
                        <button onClick={handleDelete} className="delete-btn">Удалить</button>
                    </div>
                )}
            </div>
        </div>
    );
};

export default PostCard;