import React, { useState, useEffect } from 'react';
import { postsApi } from '../../services/api';
import PostCard from '../Dashboard/PostCard';
import './NutritionFeed.css';

const NutritionFeed = () => {
    const [posts, setPosts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    const loadNutritionFeed = async () => {
        try {
            setLoading(true);
            setError(null);
            // Пока используем общую ленту (в будущем можно добавить фильтр по типу "nutrition")
            const response = await postsApi.getFeed(20);
            setPosts(Array.isArray(response.data) ? response.data : []);
        } catch (err) {
            console.error('Failed to load nutrition feed', err);
            setError('Не удалось загрузить ленту питания');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadNutritionFeed();
    }, []);

    if (loading) return <div className="loading">Загрузка ленты питания...</div>;
    if (error) return <div className="error-message">{error}</div>;

    return (
        <div className="nutrition-feed">
            <h2>🍎 Лента питания</h2>

            {posts.length === 0 ? (
                <p className="no-posts">Пока нет публикаций в ленте питания</p>
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

export default NutritionFeed;