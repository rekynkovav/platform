// components/Dashboard/PostFeed.js
import React from 'react';
import PostCard from './PostCard';
import './PostFeed.css';

const PostFeed = ({ posts, isAdmin, onPostUpdated, onPostDeleted }) => {
    if (!posts || posts.length === 0) {
        return (
            <div className="post-feed-empty">
                <p>📭 Пока нет публикаций</p>
                <p className="empty-hint">Следите за обновлениями!</p>
            </div>
        );
    }

    return (
        <div className="post-feed">
            {posts.map((post, index) => (
                <PostCard
                    key={post.id}
                    post={post}
                    isAdmin={isAdmin}
                    onPostUpdated={onPostUpdated}
                    onPostDeleted={onPostDeleted}
                    index={index}
                />
            ))}
        </div>
    );
};

export default PostFeed;