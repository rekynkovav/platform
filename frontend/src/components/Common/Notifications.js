import React, { useState, useEffect, useRef } from 'react';
import { chatApi, eventsApi, postsApi } from '../../services/api';
import './Notifications.css';

const Notifications = () => {
    const [notifications, setNotifications] = useState([]);
    const [unreadCount, setUnreadCount] = useState(0);
    const [isOpen, setIsOpen] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const notificationRef = useRef(null);

    const clearError = () => {
        setError(null);
    };

    // Типы уведомлений
    const notificationTypes = {
        chat: { icon: '💬', color: '#4F46E5', label: 'Чат' },
        event: { icon: '📅', color: '#10B981', label: 'Событие' },
        post: { icon: '📝', color: '#F59E0B', label: 'Пост' },
        system: { icon: '⚙️', color: '#6B7280', label: 'Система' },
        warning: { icon: '⚠️', color: '#EF4444', label: 'Предупреждение' },
        success: { icon: '✅', color: '#10B981', label: 'Успех' }
    };

    useEffect(() => {
        const fetchNotifications = async () => {
            try {
                setLoading(true);
                // В реальном приложении здесь будет вызов API
                // const data = await chatApi.getUnreadCount();
                // const notifications = await chatApi.getNotifications();

                // Мок-данные для демонстрации
                const mockNotifications = [
                    {
                        id: '1',
                        type: 'chat',
                        title: 'Новое сообщение',
                        message: 'Вас упомянули в общем чате',
                        timestamp: new Date(Date.now() - 300000).toISOString(),
                        read: false,
                        action: { type: 'navigate', path: '/chat' }
                    },
                    {
                        id: '2',
                        type: 'event',
                        title: 'Новое событие',
                        message: 'Завтра в 19:00 состоится вебинар по питанию',
                        timestamp: new Date(Date.now() - 1800000).toISOString(),
                        read: false,
                        action: { type: 'navigate', path: '/events' }
                    },
                    {
                        id: '3',
                        type: 'post',
                        title: 'Новый пост',
                        message: 'Пользователь опубликовал новый пост в ленте',
                        timestamp: new Date(Date.now() - 3600000).toISOString(),
                        read: true,
                        action: { type: 'navigate', path: '/feed' }
                    },
                    {
                        id: '4',
                        type: 'system',
                        title: 'Обновление системы',
                        message: 'Запланировано техническое обслуживание на 02:00',
                        timestamp: new Date(Date.now() - 7200000).toISOString(),
                        read: true,
                        action: { type: 'dismiss' }
                    },
                    {
                        id: '5',
                        type: 'warning',
                        title: 'Внимание',
                        message: 'Ваша подписка истекает через 3 дня',
                        timestamp: new Date(Date.now() - 86400000).toISOString(),
                        read: true,
                        action: { type: 'navigate', path: '/subscription' }
                    }
                ];

                setNotifications(mockNotifications);
                setUnreadCount(mockNotifications.filter(n => !n.read).length);
            } catch (err) {
                console.error('Failed to fetch notifications:', err);
                setError('Не удалось загрузить уведомления');
            } finally {
                setLoading(false);
            }
        };

        fetchNotifications();

        // Обновляем уведомления каждые 30 секунд
        const interval = setInterval(fetchNotifications, 30000);

        return () => clearInterval(interval);
    }, []);

    useEffect(() => {
        const handleClickOutside = (event) => {
            if (notificationRef.current && !notificationRef.current.contains(event.target)) {
                setIsOpen(false);
            }
        };

        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const markAsRead = async (id) => {
        try {
            // В реальном приложении здесь будет вызов API
            // await chatApi.markAsRead(id);

            setNotifications(prev =>
                prev.map(notif =>
                    notif.id === id ? { ...notif, read: true } : notif
                )
            );
            setUnreadCount(prev => Math.max(0, prev - 1));
        } catch (err) {
            console.error('Failed to mark notification as read:', err);
        }
    };

    const markAllAsRead = async () => {
        try {
            // В реальном приложении здесь будет вызов API
            // await chatApi.markAllAsRead();

            setNotifications(prev =>
                prev.map(notif => ({ ...notif, read: true }))
            );
            setUnreadCount(0);
        } catch (err) {
            console.error('Failed to mark all notifications as read:', err);
        }
    };

    const deleteNotification = async (id) => {
        try {
            // В реальном приложении здесь будет вызов API
            // await chatApi.deleteNotification(id);

            setNotifications(prev => prev.filter(notif => notif.id !== id));
            setUnreadCount(prev => {
                const deletedNotif = notifications.find(n => n.id === id);
                return deletedNotif && !deletedNotif.read ? Math.max(0, prev - 1) : prev;
            });
        } catch (err) {
            console.error('Failed to delete notification:', err);
        }
    };

    const handleNotificationClick = (notification) => {
        if (!notification.read) {
            markAsRead(notification.id);
        }

        // Обработка действия
        switch (notification.action?.type) {
            case 'navigate':
                window.location.href = notification.action.path;
                break;
            case 'dismiss':
                deleteNotification(notification.id);
                break;
            default:
                // Ничего не делаем
                break;
        }

        setIsOpen(false);
    };

    const formatTime = (timestamp) => {
        const date = new Date(timestamp);
        const now = new Date();
        const diffMs = now - date;
        const diffMins = Math.floor(diffMs / 60000);
        const diffHours = Math.floor(diffMs / 3600000);
        const diffDays = Math.floor(diffMs / 86400000);

        if (diffMins < 1) return 'Только что';
        if (diffMins < 60) return `${diffMins} мин назад`;
        if (diffHours < 24) return `${diffHours} ч назад`;
        if (diffDays < 7) return `${diffDays} дн назад`;

        return date.toLocaleDateString('ru-RU', {
            day: 'numeric',
            month: 'short'
        });
    };

    const NotificationItem = ({ notification }) => {
        const typeInfo = notificationTypes[notification.type] || notificationTypes.system;

        return (
            <div
                className={`notification-item ${notification.read ? 'read' : 'unread'}`}
                onClick={() => handleNotificationClick(notification)}
            >
                <div className="notification-icon" style={{ backgroundColor: typeInfo.color }}>
                    {typeInfo.icon}
                </div>
                <div className="notification-content">
                    <div className="notification-header">
                        <div className="notification-title">{notification.title}</div>
                        <div className="notification-time">{formatTime(notification.timestamp)}</div>
                    </div>
                    <div className="notification-message">{notification.message}</div>
                    <div className="notification-footer">
                        <span className="notification-type">{typeInfo.label}</span>
                        {!notification.read && <div className="unread-dot"></div>}
                    </div>
                </div>
                <button
                    className="notification-delete"
                    onClick={(e) => {
                        e.stopPropagation();
                        deleteNotification(notification.id);
                    }}
                    title="Удалить"
                >
                    ×
                </button>
            </div>
        );
    };

    return (
        <div className="notifications-container" ref={notificationRef}>
            <button
                className="notifications-button"
                onClick={() => setIsOpen(!isOpen)}
                aria-label="Уведомления"
            >
                <div className="bell-icon">🔔</div>
                {unreadCount > 0 && (
                    <div className="notification-badge">
                        {unreadCount > 9 ? '9+' : unreadCount}
                    </div>
                )}
            </button>

            {isOpen && (
                <div className="notifications-dropdown">
                    <div className="notifications-header">
                        <h3>Уведомления</h3>
                        <div className="notifications-actions">
                            {/* Отображение ошибок */}
                            {error && (
                                <div className="error-message">
                                    <span className="error-icon">⚠️</span>
                                    <span>{error}</span>
                                    <button className="error-close" onClick={clearError}>×</button>
                                </div>
                            )}
                            {unreadCount > 0 && (
                                <button
                                    className="mark-all-read"
                                    onClick={markAllAsRead}
                                >
                                    Прочитать все
                                </button>
                            )}
                            <button
                                className="close-dropdown"
                                onClick={() => setIsOpen(false)}
                            >
                                ×
                            </button>
                        </div>
                    </div>

                    {loading ? (
                        <div className="notifications-loading">
                            <div className="loading-spinner"></div>
                            <span>Загрузка уведомлений...</span>
                        </div>
                    ) : notifications.length === 0 ? (
                        <div className="no-notifications">
                            <div className="empty-icon">📭</div>
                            <span>Нет уведомлений</span>
                        </div>
                    ) : (
                        <div className="notifications-list">
                            {notifications.map(notification => (
                                <NotificationItem
                                    key={notification.id}
                                    notification={notification}
                                />
                            ))}
                        </div>
                    )}

                    <div className="notifications-footer">
                        <button
                            className="view-all"
                            onClick={() => {
                                window.location.href = '/notifications';
                                setIsOpen(false);
                            }}
                        >
                            Посмотреть все
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
};

export default Notifications;