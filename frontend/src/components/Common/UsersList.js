import React, { useState, useEffect, useMemo } from 'react';
import { FixedSizeList as List } from 'react-window';
import { AutoSizer } from 'react-virtualized-auto-sizer';
import { usersApi } from '../../services/api';
import UserStatus from './UserStatus';
import './UsersList.css';

const UsersList = ({ searchQuery = '', showOnlyOnline = false, limit = 50 }) => {
    const [users, setUsers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [selectedUser, setSelectedUser] = useState(null);

    useEffect(() => {
        const fetchUsers = async () => {
            try {
                setLoading(true);
                // В реальном приложении здесь будет вызов API
                // const data = await usersApi.getAll();

                // Мок-данные для демонстрации
                const mockUsers = Array.from({ length: 100 }, (_, i) => ({
                    id: `user-${i + 1}`,
                    username: `Пользователь ${i + 1}`,
                    status: ['online', 'away', 'busy', 'offline'][Math.floor(Math.random() * 4)],
                    lastSeen: new Date(Date.now() - Math.random() * 86400000 * 7).toISOString(),
                    roles: i === 0 ? ['SUPER_ADMIN'] :
                           i < 5 ? ['MODERATOR_ADMIN'] :
                           i < 10 ? ['DEVELOPER'] :
                           i < 20 ? ['USER_2'] :
                           i < 40 ? ['USER_1'] : ['USER_0'],
                    avatarColor: `hsl(${Math.random() * 360}, 70%, 60%)`
                }));

                setUsers(mockUsers);
            } catch (err) {
                console.error('Failed to fetch users:', err);
                setError('Не удалось загрузить список пользователей');
            } finally {
                setLoading(false);
            }
        };

        fetchUsers();

        // Обновляем статусы каждые 30 секунд
        const interval = setInterval(() => {
            setUsers(prev => prev.map(user => ({
                ...user,
                status: Math.random() > 0.1 ? user.status :
                       ['online', 'away', 'busy', 'offline'][Math.floor(Math.random() * 4)]
            })));
        }, 30000);

        return () => clearInterval(interval);
    }, []);

    const clearError = () => {
        setError(null);
    };

    const filteredUsers = useMemo(() => {
        let filtered = users;

        if (searchQuery) {
            const query = searchQuery.toLowerCase();
            filtered = filtered.filter(user =>
                user.username.toLowerCase().includes(query) ||
                user.roles.some(role => role.toLowerCase().includes(query))
            );
        }

        if (showOnlyOnline) {
            filtered = filtered.filter(user => user.status === 'online');
        }

        return filtered.slice(0, limit);
    }, [users, searchQuery, showOnlyOnline, limit]);

    const UserRow = ({ index, style }) => {
        const user = filteredUsers[index];
        const isSelected = selectedUser?.id === user.id;

        return (
            <div
                style={style}
                className={`user-row ${isSelected ? 'selected' : ''}`}
                onClick={() => setSelectedUser(user)}
            >
                <div className="user-avatar" style={{ backgroundColor: user.avatarColor }}>
                    {user.username.charAt(0)}
                </div>
                <div className="user-details">
                    <div className="user-name">{user.username}</div>
                    <div className="user-status-indicator">
                        <div
                            className="status-dot"
                            style={{
                                backgroundColor: getStatusColor(user.status),
                                animation: user.status === 'online' ? 'pulse 2s infinite' : 'none'
                            }}
                        />
                        <span className="status-text">{getStatusText(user.status)}</span>
                        {user.status === 'offline' && (
                            <span className="last-seen">• {formatLastSeen(user.lastSeen)}</span>
                        )}
                    </div>
                </div>
                <div className="user-roles">
                    {user.roles.slice(0, 2).map((role, idx) => (
                        <span key={idx} className="role-badge">
                            {role}
                        </span>
                    ))}
                    {user.roles.length > 2 && (
                        <span className="role-more">+{user.roles.length - 2}</span>
                    )}
                </div>
            </div>
        );
    };

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
            <div className="users-list loading">
                <div className="loading-spinner"></div>
                <span>Загрузка пользователей...</span>
            </div>
        );
    }

    if (error) {
        return (
            <div className="users-list error">
                <div className="error-icon">⚠️</div>
                <span>{error}</span>
            </div>
        );
    }

    return (
        <div className="users-list">
            {/* Отображение ошибок */}
            {error && (
                <div className="error-message">
                    <span className="error-icon">⚠️</span>
                    <span>{error}</span>
                    <button className="error-close" onClick={clearError}>×</button>
                </div>
            )}

            <div className="users-header">
                <h3>Пользователи ({filteredUsers.length})</h3>
                <div className="users-stats">
                    <span className="stat online">
                        <div className="stat-dot" style={{ backgroundColor: '#4CAF50' }}></div>
                        {filteredUsers.filter(u => u.status === 'online').length} онлайн
                    </span>
                    <span className="stat total">
                        <div className="stat-dot" style={{ backgroundColor: '#9E9E9E' }}></div>
                        {filteredUsers.length} всего
                    </span>
                </div>
            </div>

            <div className="users-container" style={{ height: '500px' }}>
                <AutoSizer>
                    {({ height, width }) => (
                        <List
                            height={height}
                            width={width}
                            itemCount={filteredUsers.length}
                            itemSize={70}
                        >
                            {UserRow}
                        </List>
                    )}
                </AutoSizer>
            </div>

            {selectedUser && (
                <div className="user-detail-modal">
                    <div className="modal-content">
                        <button
                            className="modal-close"
                            onClick={() => setSelectedUser(null)}
                        >
                            ×
                        </button>
                        <UserStatus user={selectedUser} />
                        <div className="user-detail-info">
                            <h4>Подробная информация</h4>
                            <div className="detail-item">
                                <strong>ID:</strong> {selectedUser.id}
                            </div>
                            <div className="detail-item">
                                <strong>Статус:</strong> {getStatusText(selectedUser.status)}
                            </div>
                            <div className="detail-item">
                                <strong>Последняя активность:</strong> {formatLastSeen(selectedUser.lastSeen)}
                            </div>
                            <div className="detail-item">
                                <strong>Роли:</strong>
                                <div className="detail-roles">
                                    {selectedUser.roles.map((role, idx) => (
                                        <span key={idx} className="detail-role-badge">
                                            {role}
                                        </span>
                                    ))}
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default UsersList;