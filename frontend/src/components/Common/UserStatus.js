import React, { useState, useEffect } from 'react';
import { getUsername, getUserRoles } from '../../keycloak';
import { usersApi } from '../../services/api';

const UserStatus = () => {
    const [userData, setUserData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    const clearError = () => {
        setError(null);
    };

    useEffect(() => {
        const fetchUserData = async () => {
            try {
                setLoading(true);
                const username = getUsername();
                const roles = getUserRoles();

                // Получаем данные пользователя с бэкенда
                const userInfo = await usersApi.getCurrentUser();

                setUserData({
                    username,
                    roles,
                    ...userInfo
                });
            } catch (err) {
                console.error('Failed to fetch user data:', err);
                setError('Не удалось загрузить данные пользователя');
                // Fallback на локальные данные
                setUserData({
                    username: getUsername(),
                    roles: getUserRoles(),
                    status: 'offline',
                    lastSeen: new Date().toISOString()
                });
            } finally {
                setLoading(false);
            }
        };

        fetchUserData();
    }, []);

    const getStatusColor = (status) => {
        switch (status) {
            case 'online': return '#4CAF50';
            case 'away': return '#FF9800';
            case 'busy': return '#F44336';
            case 'offline': return '#9E9E9E';
            default: return '#9E9E9E';
        }
    };

    const getStatusText = (status) => {
        switch (status) {
            case 'online': return 'В сети';
            case 'away': return 'Отошёл';
            case 'busy': return 'Занят';
            case 'offline': return 'Не в сети';
            default: return 'Не в сети';
        }
    };

    const formatLastSeen = (timestamp) => {
        if (!timestamp) return 'Неизвестно';

        const date = new Date(timestamp);
        const now = new Date();
        const diffMs = now - date;
        const diffMins = Math.floor(diffMs / 60000);

        if (diffMins < 1) return 'Только что';
        if (diffMins < 60) return `${diffMins} мин назад`;
        if (diffMins < 1440) return `${Math.floor(diffMins / 60)} ч назад`;

        return date.toLocaleDateString('ru-RU');
    };

    if (loading) {
        return (
            <div className="user-status loading">
                <div className="status-dot"></div>
                <span>Загрузка...</span>
            </div>
        );
    }

    if (error) {
        return (
            <div className="user-status error">
                <div className="status-dot" style={{ backgroundColor: '#F44336' }}></div>
                <span>{error}</span>
                <button className="error-close" onClick={clearError}>×</button>
            </div>
        );
    }

    return (
        <div className="user-status">
            <div
                className="status-dot"
                style={{ backgroundColor: getStatusColor(userData?.status) }}
                title={getStatusText(userData?.status)}
            ></div>

            <div className="user-info">
                <div className="username">{userData?.username || 'Пользователь'}</div>
                <div className="status-details">
                    <span className="status-text">{getStatusText(userData?.status)}</span>
                    {userData?.status === 'offline' && userData?.lastSeen && (
                        <span className="last-seen">• {formatLastSeen(userData.lastSeen)}</span>
                    )}
                </div>
            </div>

            {userData?.roles && userData.roles.length > 0 && (
                <div className="user-roles">
                    {userData.roles.map((role, index) => (
                        <span key={index} className="role-badge">
                            {role}
                        </span>
                    ))}
                </div>
            )}
        </div>
    );
};

export default UserStatus;