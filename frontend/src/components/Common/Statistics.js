import React, { useState, useEffect } from 'react';
import { eventsApi, postsApi, chatApi, usersApi } from '../../services/api';
import './Statistics.css';

const Statistics = ({ timeRange = 'day' }) => {
    const [stats, setStats] = useState({
        users: { total: 0, online: 0, new: 0 },
        posts: { total: 0, today: 0, popular: 0 },
        events: { total: 0, upcoming: 0, past: 0 },
        chat: { messages: 0, rooms: 0, active: 0 },
        performance: { uptime: 100, responseTime: 0, errors: 0 }
    });
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [selectedMetric, setSelectedMetric] = useState(null);

    useEffect(() => {
        const fetchStatistics = async () => {
            try {
                setLoading(true);

                // В реальном приложении здесь будут вызовы API
                // const usersData = await usersApi.getStats();
                // const postsData = await postsApi.getStats();
                // и т.д.

                // Мок-данные для демонстрации
                const mockStats = {
                    users: {
                        total: 1248,
                        online: 42,
                        new: 18,
                        growth: 12.5
                    },
                    posts: {
                        total: 5672,
                        today: 124,
                        popular: 23,
                        engagement: 4.8
                    },
                    events: {
                        total: 89,
                        upcoming: 5,
                        past: 84,
                        attendance: 76.3
                    },
                    chat: {
                        messages: 12345,
                        rooms: 8,
                        active: 3,
                        avgResponse: 2.4
                    },
                    performance: {
                        uptime: 99.8,
                        responseTime: 142,
                        errors: 12,
                        latency: 45
                    }
                };

                setStats(mockStats);
            } catch (err) {
                console.error('Failed to fetch statistics:', err);
                setError('Не удалось загрузить статистику');
            } finally {
                setLoading(false);
            }
        };

        fetchStatistics();

        // Обновляем статистику каждую минуту
        const interval = setInterval(fetchStatistics, 60000);

        return () => clearInterval(interval);
    }, [timeRange]);

    const clearError = () => {
        setError(null);
    };

    const getMetricColor = (value, type) => {
        if (type === 'uptime') {
            if (value >= 99.9) return '#10B981';
            if (value >= 99) return '#F59E0B';
            return '#EF4444';
        }
        if (type === 'responseTime') {
            if (value <= 100) return '#10B981';
            if (value <= 200) return '#F59E0B';
            return '#EF4444';
        }
        if (type === 'growth' || type === 'engagement' || type === 'attendance') {
            if (value > 0) return '#10B981';
            if (value < 0) return '#EF4444';
            return '#6B7280';
        }
        return '#6B7280';
    };

    const formatNumber = (num) => {
        if (num >= 1000000) return `${(num / 1000000).toFixed(1)}M`;
        if (num >= 1000) return `${(num / 1000).toFixed(1)}K`;
        return num.toString();
    };

    const formatPercentage = (num) => {
        return `${num.toFixed(1)}%`;
    };

    const formatTime = (ms) => {
        return `${ms}ms`;
    };

    const StatCard = ({ title, value, change, icon, color, type, onClick }) => {
        const isPositive = change > 0;
        const changeText = change !== undefined ? `${isPositive ? '+' : ''}${change}%` : null;

        return (
            <div
                className={`stat-card ${onClick ? 'clickable' : ''}`}
                style={{ borderLeftColor: color }}
                onClick={onClick}
            >
                <div className="stat-header">
                    <div className="stat-icon" style={{ backgroundColor: color }}>
                        {icon}
                    </div>
                    <div className="stat-title">{title}</div>
                </div>
                <div className="stat-value">
                    {type === 'percentage' ? formatPercentage(value) :
                     type === 'time' ? formatTime(value) :
                     formatNumber(value)}
                </div>
                {changeText && (
                    <div className={`stat-change ${isPositive ? 'positive' : 'negative'}`}>
                        {isPositive ? '↗' : '↘'} {changeText}
                    </div>
                )}
                <div className="stat-trend">
                    <div className="trend-line" style={{ width: `${Math.min(Math.abs(change || 0), 100)}%` }}></div>
                </div>
            </div>
        );
    };

    if (loading) {
        return (
            <div className="statistics loading">
                <div className="loading-spinner"></div>
                <span>Загрузка статистики...</span>
            </div>
        );
    }

    if (error) {
        return (
            <div className="statistics error">
                <div className="error-icon">⚠️</div>
                <span>{error}</span>
            </div>
        );
    }

    return (
        <div className="statistics">
            {/* Отображение ошибок */}
            {error && (
                <div className="error-message">
                    <span className="error-icon">⚠️</span>
                    <span>{error}</span>
                    <button className="error-close" onClick={clearError}>×</button>
                </div>
            )}

            <div className="stats-header">
                <h2>📊 Статистика платформы</h2>
                <div className="time-range-selector">
                    <button className={`time-btn ${timeRange === 'day' ? 'active' : ''}`}>День</button>
                    <button className={`time-btn ${timeRange === 'week' ? 'active' : ''}`}>Неделя</button>
                    <button className={`time-btn ${timeRange === 'month' ? 'active' : ''}`}>Месяц</button>
                </div>
            </div>

            <div className="stats-grid">
                <StatCard
                    title="Пользователи онлайн"
                    value={stats.users.online}
                    change={stats.users.growth}
                    icon="👥"
                    color="#4F46E5"
                    type="number"
                    onClick={() => setSelectedMetric('users')}
                />
                <StatCard
                    title="Новые посты"
                    value={stats.posts.today}
                    change={stats.posts.engagement}
                    icon="📝"
                    color="#10B981"
                    type="number"
                    onClick={() => setSelectedMetric('posts')}
                />
                <StatCard
                    title="Активные события"
                    value={stats.events.upcoming}
                    change={stats.events.attendance}
                    icon="📅"
                    color="#F59E0B"
                    type="number"
                    onClick={() => setSelectedMetric('events')}
                />
                <StatCard
                    title="Сообщений в чате"
                    value={stats.chat.messages}
                    change={stats.chat.avgResponse}
                    icon="💬"
                    color="#EC4899"
                    type="number"
                    onClick={() => setSelectedMetric('chat')}
                />
                <StatCard
                    title="Время отклика"
                    value={stats.performance.responseTime}
                    icon="⚡"
                    color={getMetricColor(stats.performance.responseTime, 'responseTime')}
                    type="time"
                />
                <StatCard
                    title="Аптайм"
                    value={stats.performance.uptime}
                    icon="🟢"
                    color={getMetricColor(stats.performance.uptime, 'uptime')}
                    type="percentage"
                />
            </div>

            <div className="detailed-stats">
                <div className="stat-section">
                    <h3>👥 Пользователи</h3>
                    <div className="stat-details">
                        <div className="detail-item">
                            <span className="detail-label">Всего пользователей:</span>
                            <span className="detail-value">{formatNumber(stats.users.total)}</span>
                        </div>
                        <div className="detail-item">
                            <span className="detail-label">Новых за период:</span>
                            <span className="detail-value">{stats.users.new}</span>
                        </div>
                        <div className="detail-item">
                            <span className="detail-label">Рост:</span>
                            <span className="detail-value" style={{ color: getMetricColor(stats.users.growth, 'growth') }}>
                                {stats.users.growth > 0 ? '+' : ''}{stats.users.growth}%
                            </span>
                        </div>
                    </div>
                </div>

                <div className="stat-section">
                    <h3>📝 Контент</h3>
                    <div className="stat-details">
                        <div className="detail-item">
                            <span className="detail-label">Всего постов:</span>
                            <span className="detail-value">{formatNumber(stats.posts.total)}</span>
                        </div>
                        <div className="detail-item">
                            <span className="detail-label">Популярных:</span>
                            <span className="detail-value">{stats.posts.popular}</span>
                        </div>
                        <div className="detail-item">
                            <span className="detail-label">Вовлеченность:</span>
                            <span className="detail-value" style={{ color: getMetricColor(stats.posts.engagement, 'engagement') }}>
                                {stats.posts.engagement}%
                            </span>
                        </div>
                    </div>
                </div>

                <div className="stat-section">
                    <h3>📅 События</h3>
                    <div className="stat-details">
                        <div className="detail-item">
                            <span className="detail-label">Всего событий:</span>
                            <span className="detail-value">{stats.events.total}</span>
                        </div>
                        <div className="detail-item">
                            <span className="detail-label">Прошедших:</span>
                            <span className="detail-value">{stats.events.past}</span>
                        </div>
                        <div className="detail-item">
                            <span className="detail-label">Посещаемость:</span>
                            <span className="detail-value" style={{ color: getMetricColor(stats.events.attendance, 'attendance') }}>
                                {stats.events.attendance}%
                            </span>
                        </div>
                    </div>
                </div>
            </div>

            {selectedMetric && (
                <div className="metric-detail-modal">
                    <div className="modal-content">
                        <button
                            className="modal-close"
                            onClick={() => setSelectedMetric(null)}
                        >
                            ×
                        </button>
                        <h3>Детальная статистика: {selectedMetric}</h3>
                        <div className="metric-chart">
                            {/* Здесь будет график */}
                            <div className="chart-placeholder">
                                📈 График для {selectedMetric}
                            </div>
                        </div>
                        <div className="metric-data">
                            <pre>{JSON.stringify(stats[selectedMetric], null, 2)}</pre>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default Statistics;