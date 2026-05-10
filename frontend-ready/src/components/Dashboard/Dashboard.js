// components/Dashboard/Dashboard.js
import React, { useState, useEffect } from 'react';
import EventCard from './EventCard';
import AdminPostCreator from './AdminPostCreator';
import PostFeed from './PostFeed';
import { eventsApi, postsApi } from '../../services/api';
import { getUserRoles } from '../../keycloak';
import './Dashboard.css';

const Dashboard = () => {
    const [nextEvent, setNextEvent] = useState(null);
    const [posts, setPosts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [isAdmin, setIsAdmin] = useState(false);

    useEffect(() => {
        const roles = getUserRoles();
        setIsAdmin(roles.includes('MODERATOR_ADMIN') || roles.includes('SUPER_ADMIN') || roles.includes('VYACHESLAV'));

        loadNextEvent();
        loadPosts();

        // Обновляем каждые 60 секунд
        const interval = setInterval(() => {
            loadNextEvent();
            loadPosts();
        }, 60000);

        return () => clearInterval(interval);
    }, []);

    const loadNextEvent = async () => {
        try {
            const response = await eventsApi.getNext();
            setNextEvent(response.data);
        } catch (err) {
            console.error('Failed to load next event:', err);
        }
    };

    const loadPosts = async () => {
        try {
            setLoading(true);
            const response = await postsApi.getFeed();
            setPosts(response.data);
            setError(null);
        } catch (err) {
            console.error('Failed to load posts:', err);
            setError('Не удалось загрузить ленту');
        } finally {
            setLoading(false);
        }
    };

    const handlePostCreated = (newPost) => {
        setPosts(prev => [newPost, ...prev]);
    };

    const handlePostUpdated = (updatedPost) => {
        setPosts(prev => prev.map(p => p.id === updatedPost.id ? updatedPost : p));
    };

    const handlePostDeleted = (postId) => {
        setPosts(prev => prev.filter(p => p.id !== postId));
    };

    return (
        <div className="dashboard">
            {/* Ближайшее событие */}
            {nextEvent && (
                <div className="dashboard-section event-section">
                    <EventCard event={nextEvent} />
                </div>
            )}

            {/* Админская панель создания постов */}
            {isAdmin && (
                <div className="dashboard-section admin-section">
                    <AdminPostCreator onPostCreated={handlePostCreated} />
                </div>
            )}

            {/* Лента постов с фоном */}
            <div className="dashboard-section feed-section">
                <div className="feed-background">
                    <div className="feed-background-text text-top">Cuba & Eguinembé</div>
                    <div className="feed-background-text text-bottom">Путь сердца!</div>
                </div>

                <h2 className="section-title">📰 ЛЕНТА НОВОСТЕЙ</h2>

                {error && <div className="error-message">{error}</div>}

                {loading && posts.length === 0 ? (
                    <div className="loading">Загрузка...</div>
                ) : (
                    <PostFeed
                        posts={posts}
                        isAdmin={isAdmin}
                        onPostUpdated={handlePostUpdated}
                        onPostDeleted={handlePostDeleted}
                    />
                )}
            </div>
        </div>
    );
};

export default Dashboard;