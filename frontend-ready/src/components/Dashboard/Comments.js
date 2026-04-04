// components/Dashboard/Comments.js
import React, { useState, useEffect } from 'react';
import CommentItem from './CommentItem';
import CommentInput from './CommentInput';
import { commentsApi } from '../../services/api';
import './Comments.css';

const Comments = ({ postId, onCommentAdded }) => {
    const [comments, setComments] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        loadComments();
    }, [postId]);

    const loadComments = async () => {
        try {
            setLoading(true);
            const response = await commentsApi.getByPost(postId);
            setComments(response.data);
            setError(null);
        } catch (err) {
            console.error('Failed to load comments:', err);
            setError('Не удалось загрузить комментарии');
        } finally {
            setLoading(false);
        }
    };

    const handleCommentAdded = (newComment) => {
        setComments(prev => [newComment, ...prev]);
        if (onCommentAdded) onCommentAdded();
    };

    const handleCommentDeleted = (commentId) => {
        setComments(prev => prev.filter(c => c.id !== commentId));
    };

    return (
        <div className="comments">
            <div className="comments-header">
                <span className="comments-title">💬 КОММЕНТАРИИ</span>
                <span className="comments-count">{comments.length}</span>
            </div>

            <CommentInput postId={postId} onCommentAdded={handleCommentAdded} />

            {loading && comments.length === 0 ? (
                <div className="comments-loading">Загрузка комментариев...</div>
            ) : error ? (
                <div className="comments-error">{error}</div>
            ) : comments.length === 0 ? (
                <div className="comments-empty">
                    <p>Будьте первым, кто оставит комментарий!</p>
                </div>
            ) : (
                <div className="comments-list">
                    {comments.map(comment => (
                        <CommentItem
                            key={comment.id}
                            comment={comment}
                            onDeleted={handleCommentDeleted}
                        />
                    ))}
                </div>
            )}
        </div>
    );
};

export default Comments;