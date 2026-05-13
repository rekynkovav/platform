import React, { useState, useEffect } from 'react';
import { postsApi } from '../../services/api';
import PostCard from '../Dashboard/PostCard';
import './MainFeed.css';

const MainFeed = () => {
    const [posts, setPosts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    const loadFeed = async () => {
        try {
            setLoading(true);
            setError(null);
            const response = await postsApi.getFeed(20);
            setPosts(Array.isArray(response.data) ? response.data : []);
        } catch (err) {
            console.error('Failed to load main feed', err);
            setError('Не удалось загрузить ленту');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadFeed();
    }, []);

    if (loading) return <div className="loading">Загрузка ленты...</div>;
    if (error) return <div className="error-message">{error}</div>;

    return (
        <div className="main-feed">
            <h2>📢 Лента сообщества</h2>

            {posts.length === 0 ? (
                <p className="no-posts">Пока нет публикаций в ленте</p>
            ) : (
                <div className="posts-list">
                    {posts.map(post => (
                        <PostCard
                            key={post.id}
                            post={post}
                            onDelete={() => {}}
                            onEdit={() => {}}
                        />
                    ))}
                </div>
            )}
        </div>
    );
};

export default MainFeed;