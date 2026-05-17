// components/Dashboard/Comments.js
import React, { useState, useEffect, useRef } from 'react';
import CommentItem from './CommentItem';
import CommentInput from './CommentInput';
import { commentsApi } from '../../services/api';
import './Comments.css';

const Comments = ({ postId, onCommentAdded }) => {
    const [comments, setComments] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const abortRef = useRef(null);

    useEffect(() => {
        // Отменяем предыдущий запрос при смене postId
        if (abortRef.current) abortRef.current.abort();
        const controller = new AbortController();
        abortRef.current = controller;

        loadComments(controller.signal);

        return () => {
            controller.abort();
        };
    }, [postId]);

    const loadComments = async (signal) => {
        try {
            setLoading(true);
            const response = await commentsApi.getByPost(postId, { signal });
            if (signal?.aborted) return;
            setComments(response.data);
            setError(null);
        } catch (err) {
            if (err.name === 'AbortError' || err.name === 'CanceledError') return;
            console.error('Failed to load comments:', err);
            setError('Не удалось загрузить комментарии');
        } finally {
            if (!signal?.aborted) setLoading(false);
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