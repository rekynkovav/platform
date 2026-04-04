// components/Dashboard/CommentInput.js
import React, { useState } from 'react';
import { commentsApi } from '../../services/api';
import './CommentInput.css';

const CommentInput = ({ postId, onCommentAdded }) => {
    const [content, setContent] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);

    const handleSubmit = async () => {
        if (!content.trim()) return;

        setIsSubmitting(true);
        try {
            const response = await commentsApi.create(postId, content.trim());
            setContent('');
            if (onCommentAdded) onCommentAdded(response.data);
        } catch (error) {
            console.error('Failed to add comment:', error);
            alert('Ошибка при отправке комментария');
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleKeyPress = (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSubmit();
        }
    };

    return (
        <div className="comment-input">
            <textarea
                className="comment-input-field"
                placeholder="✍️ Написать комментарий..."
                value={content}
                onChange={(e) => setContent(e.target.value)}
                onKeyPress={handleKeyPress}
                rows={2}
                disabled={isSubmitting}
            />
            <button
                className="comment-input-submit"
                onClick={handleSubmit}
                disabled={isSubmitting || !content.trim()}
            >
                {isSubmitting ? '⏳' : '📤'}
            </button>
        </div>
    );
};

export default CommentInput;