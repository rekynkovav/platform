import React, { useState, useEffect, useRef } from 'react';
import './NotificationBell.css';

const NotificationBell = () => {
    const [notifications, setNotifications] = useState([]);
    const [isOpen, setIsOpen] = useState(false);
    const [unreadCount, setUnreadCount] = useState(0);
    const dropdownRef = useRef(null);

    useEffect(() => {
        loadNotifications();
        checkUpcomingEvents();
        checkQuestionNotifications();

        const eventCheckInterval = setInterval(checkUpcomingEvents, 60000);
        const questionCheckInterval = setInterval(checkQuestionNotifications, 2000);
        window.addEventListener('storage', loadNotifications);
        window.addEventListener('newAdminReply', handleNewAdminReply);

        return () => {
            clearInterval(eventCheckInterval);
            clearInterval(questionCheckInterval);
            window.removeEventListener('storage', loadNotifications);
            window.removeEventListener('newAdminReply', handleNewAdminReply);
        };
    }, []);

    useEffect(() => {
        const handleClickOutside = (event) => {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
                setIsOpen(false);
            }
        };

        if (isOpen) {
            document.addEventListener('mousedown', handleClickOutside);
        }

        return () => {
            document.removeEventListener('mousedown', handleClickOutside);
        };
    }, [isOpen]);

    const loadNotifications = () => {
        const saved = localStorage.getItem('userNotifications');
        if (saved) {
            const parsed = JSON.parse(saved);
            setNotifications(parsed);
            setUnreadCount(parsed.filter(n => !n.read).length);
        }
    };

    const saveNotifications = (notifs) => {
        localStorage.setItem('userNotifications', JSON.stringify(notifs));
        setNotifications(notifs);
        setUnreadCount(notifs.filter(n => !n.read).length);
    };

    const checkUpcomingEvents = () => {
        const savedEvents = localStorage.getItem('scheduledEvents');
        if (!savedEvents) return;

        const events = JSON.parse(savedEvents);
        const now = new Date();
        const saved = localStorage.getItem('userNotifications');
        const existingNotifs = saved ? JSON.parse(saved) : [];

        // Получаем список уже показанных уведомлений о событиях
        const shownEventNotifications = localStorage.getItem('shownEventNotifications');
        const shownNotifs = shownEventNotifications ? JSON.parse(shownEventNotifications) : [];

        events.forEach(event => {
            const eventDate = new Date(`${event.date}T${event.time || '00:00'}`);
            const timeDiff = eventDate - now;
            const hoursLeft = timeDiff / (1000 * 60 * 60);

            if (hoursLeft <= 24 && hoursLeft > 0) {
                const notificationId = `event-24h-${event.id}`;

                // Проверяем, было ли уже показано это уведомление
                const alreadyShown = shownNotifs.includes(notificationId);
                const exists = existingNotifs.some(n => n.id === notificationId);

                if (!exists && !alreadyShown) {
                    const newNotif = {
                        id: notificationId,
                        type: 'event',
                        title: 'Скоро событие!',
                        message: `"${event.title}" начнется через ${Math.floor(hoursLeft)} ч`,
                        timestamp: new Date().toISOString(),
                        read: false,
                        eventId: event.id
                    };
                    const updated = [newNotif, ...existingNotifs];
                    saveNotifications(updated);

                    // Добавляем ID в список показанных уведомлений
                    const updatedShown = [...shownNotifs, notificationId];
                    localStorage.setItem('shownEventNotifications', JSON.stringify(updatedShown));
                }
            }
        });

        // Очищаем старые записи о показанных уведомлениях (старше 7 дней)
        cleanupShownNotifications();
    };

    const handleNewAdminReply = (event) => {
        const { questionId, questionTitle, adminName } = event.detail;

        const newNotif = {
            id: `admin-reply-${questionId}-${Date.now()}`,
            type: 'admin-reply',
            title: 'Ответ администратора',
            message: `${adminName} ответил на "${questionTitle}"`,
            timestamp: new Date().toISOString(),
            read: false,
            questionId
        };

        const updated = [newNotif, ...notifications];
        saveNotifications(updated);
    };

    const cleanupShownNotifications = () => {
        const shownEventNotifications = localStorage.getItem('shownEventNotifications');
        if (!shownEventNotifications) return;

        const shownNotifs = JSON.parse(shownEventNotifications);
        const now = new Date();
        const sevenDaysAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);

        // Получаем все события
        const savedEvents = localStorage.getItem('scheduledEvents');
        if (!savedEvents) {
            // Если событий нет, очищаем все показанные уведомления
            localStorage.setItem('shownEventNotifications', JSON.stringify([]));
            return;
        }

        const events = JSON.parse(savedEvents);
        const eventIds = events.map(e => `event-24h-${e.id}`);

        // Оставляем только те уведомления, которые относятся к существующим событиям
        // и событие еще не прошло более 7 дней назад
        const cleanedNotifs = shownNotifs.filter(notifId => {
            if (!eventIds.includes(notifId)) return false;

            const eventId = notifId.replace('event-24h-', '');
            const event = events.find(e => e.id === parseInt(eventId));
            if (!event) return false;

            const eventDate = new Date(`${event.date}T${event.time || '00:00'}`);
            return eventDate > sevenDaysAgo;
        });

        localStorage.setItem('shownEventNotifications', JSON.stringify(cleanedNotifs));
    };

    const checkQuestionNotifications = () => {
        const demoRole = localStorage.getItem('demoRole');

        // Определяем роли
        const isGeneralAdmin = demoRole === 'admin-moderator' || demoRole === 'admin-consultant' || demoRole === 'developer';
        const isNutritionAdmin = demoRole === 'admin-nutrition';
        const isVyacheslav = demoRole === 'vyacheslav';
        const isSuperAdmin = demoRole === 'super-admin';
        const isUser = demoRole === 'user0' || demoRole === 'user1' || demoRole === 'user2';

        const saved = localStorage.getItem('userNotifications');
        let existingNotifs = saved ? JSON.parse(saved) : [];

        // Загружаем все вопросы из единого хранилища
        const allQuestionsKey = 'allQuestions';
        const allQuestions = JSON.parse(localStorage.getItem(allQuestionsKey) || '[]');

        // Для админов: создаем уведомления на основе неотвеченных вопросов
        if (isGeneralAdmin || isNutritionAdmin || isVyacheslav || isSuperAdmin) {
            let myUnansweredQuestions = [];

            if (isNutritionAdmin) {
                // Админ питания видит только вопросы с recipientId = 'admin-nutrition'
                myUnansweredQuestions = allQuestions.filter(q =>
                    q.recipientId === 'admin-nutrition' && !q.isAnswered && !q.archived
                );
            } else if (isVyacheslav) {
                // Вячеслав видит личные (recipientId = 'vyacheslav') и общие (recipientId пустой)
                myUnansweredQuestions = allQuestions.filter(q =>
                    (q.recipientId === 'vyacheslav' || !q.recipientId || q.recipientId === '') &&
                    !q.isAnswered && !q.archived
                );
            } else if (isGeneralAdmin || isSuperAdmin) {
                // Общие админы видят только вопросы с пустым recipientId
                myUnansweredQuestions = allQuestions.filter(q =>
                    (!q.recipientId || q.recipientId === '') && !q.isAnswered && !q.archived
                );
            }

            // Создаем Set ID неотвеченных вопросов
            const unansweredIds = new Set(myUnansweredQuestions.map(q => `question_${q.id}`));

            // Удаляем уведомления о вопросах, которые уже отвечены
            existingNotifs = existingNotifs.filter(n => {
                if (n.type === 'new-question') {
                    return unansweredIds.has(n.id);
                }
                return true; // Оставляем другие типы уведомлений
            });

            // Добавляем уведомления для новых неотвеченных вопросов
            myUnansweredQuestions.forEach(q => {
                const notifId = `question_${q.id}`;
                const exists = existingNotifs.some(n => n.id === notifId);

                if (!exists) {
                    const modeLabel = q.mode === 'general' ? 'Общий' : q.mode === 'nutrition' ? 'Питание' : 'Вячеславу';
                    const newNotif = {
                        id: notifId,
                        type: 'new-question',
                        title: `Новый вопрос (${modeLabel})`,
                        message: `${q.author}: ${q.question.substring(0, 50)}${q.question.length > 50 ? '...' : ''}`,
                        timestamp: new Date().toISOString(),
                        read: false,
                        questionId: q.id,
                        mode: q.mode
                    };
                    existingNotifs.unshift(newNotif);
                }
            });
        }

        // Для пользователей: проверяем ответы на их вопросы
        if (isUser) {
            const currentUser = localStorage.getItem('username') || demoRole;
            const currentRoleId = demoRole; // ID текущей роли пользователя

            // Получаем единый список уже показанного контента
            const shownContentKey = `shownContent_${currentRoleId}`;
            const shownContent = JSON.parse(localStorage.getItem(shownContentKey) || '{"answers":[],"posts":[],"nutritionPosts":[],"events":[],"streams":[]}');

            // 1. Проверяем ответы на вопросы (для всех пользователей: 0, 1, 2)
            const myAnsweredQuestions = allQuestions.filter(q =>
                q.authorId === currentRoleId &&
                q.isAnswered &&
                !q.archivedByUser
            );

            myAnsweredQuestions.forEach(q => {
                const notifId = `answer_${q.id}`;
                const exists = existingNotifs.some(n => n.id === notifId);
                const alreadyShown = shownContent.answers.includes(q.id);

                if (!exists && !alreadyShown) {
                    const modeLabel = q.mode === 'general' ? 'Общий' : q.mode === 'nutrition' ? 'Питание' : 'Вячеславу';
                    existingNotifs.unshift({
                        id: notifId,
                        type: 'new-answer',
                        title: `Получен ответ (${modeLabel})`,
                        message: `${q.answeredBy} ответил на ваш вопрос`,
                        timestamp: new Date().toISOString(),
                        read: false,
                        questionId: q.id,
                        mode: q.mode
                    });

                    shownContent.answers.push(q.id);
                }
            });

            // 2. Для пользователей 1, 2 и Вячеслава: проверяем весь новый контент
            if (demoRole === 'user1' || demoRole === 'user2' || demoRole === 'vyacheslav') {

                // Проверяем новые посты в основной ленте
                const mainPosts = JSON.parse(localStorage.getItem('mainFeedPosts') || '[]');
                mainPosts.forEach(post => {
                    const alreadyShown = shownContent.posts.includes(post.id);
                    const notifId = `post_${post.id}`;
                    const exists = existingNotifs.some(n => n.id === notifId);

                    if (!exists && !alreadyShown) {
                        existingNotifs.unshift({
                            id: notifId,
                            type: 'new-post',
                            title: 'Новая запись',
                            message: post.title || 'Добавлена новая запись в ленту',
                            timestamp: new Date().toISOString(),
                            read: false
                        });
                        shownContent.posts.push(post.id);
                    }
                });

                // Проверяем новые посты в ленте питания
                const nutritionPosts = JSON.parse(localStorage.getItem('nutritionFeedPosts') || '[]');
                nutritionPosts.forEach(post => {
                    const alreadyShown = shownContent.nutritionPosts.includes(post.id);
                    const notifId = `nutrition_post_${post.id}`;
                    const exists = existingNotifs.some(n => n.id === notifId);

                    if (!exists && !alreadyShown) {
                        existingNotifs.unshift({
                            id: notifId,
                            type: 'new-nutrition-post',
                            title: 'Новая запись (Питание)',
                            message: post.title || 'Добавлена новая запись в ленту питания',
                            timestamp: new Date().toISOString(),
                            read: false
                        });
                        shownContent.nutritionPosts.push(post.id);
                    }
                });

                // Проверяем новые события
                const events = JSON.parse(localStorage.getItem('scheduledEvents') || '[]');
                events.forEach(event => {
                    const alreadyShown = shownContent.events.includes(event.id);
                    const notifId = `event_${event.id}`;
                    const exists = existingNotifs.some(n => n.id === notifId);

                    if (!exists && !alreadyShown) {
                        existingNotifs.unshift({
                            id: notifId,
                            type: 'new-event',
                            title: 'Новое событие',
                            message: event.title || 'Добавлено новое событие',
                            timestamp: new Date().toISOString(),
                            read: false
                        });
                        shownContent.events.push(event.id);
                    }
                });

                // TODO: Добавить проверку трансляций когда будет известно где они хранятся
                // const streams = JSON.parse(localStorage.getItem('streams') || '[]');
                // streams.forEach(stream => { ... });
            }

            // Сохраняем обновленный список показанного контента
            localStorage.setItem(shownContentKey, JSON.stringify(shownContent));
        }

        // Сохраняем обновленные уведомления
        if (JSON.stringify(existingNotifs) !== saved) {
            saveNotifications(existingNotifs);
        }
    };

    const markAsRead = (id) => {
        const updated = notifications.map(n =>
            n.id === id ? { ...n, read: true } : n
        );
        saveNotifications(updated);
    };

    const markAllAsRead = () => {
        const updated = notifications.map(n => ({ ...n, read: true }));
        saveNotifications(updated);
    };

    const deleteNotification = (id) => {
        const updated = notifications.filter(n => n.id !== id);
        saveNotifications(updated);
    };

    const clearAll = () => {
        saveNotifications([]);
    };

    const getNotificationIcon = (type) => {
        switch (type) {
            case 'event': return '📅';
            case 'payment': return '💳';
            case 'admin-reply': return '💬';
            case 'new-question': return '💬';
            case 'new-answer': return '💬';
            default: return '🔔';
        }
    };

    const formatTimestamp = (timestamp) => {
        const date = new Date(timestamp);
        const now = new Date();
        const diffMs = now - date;
        const diffMins = Math.floor(diffMs / 60000);
        const diffHours = Math.floor(diffMs / 3600000);
        const diffDays = Math.floor(diffMs / 86400000);

        if (diffMins < 1) return 'только что';
        if (diffMins < 60) return `${diffMins} мин назад`;
        if (diffHours < 24) return `${diffHours} ч назад`;
        if (diffDays < 7) return `${diffDays} дн назад`;

        return date.toLocaleDateString('ru-RU', { day: 'numeric', month: 'short' });
    };

    const handleNotificationClick = (notification) => {
        markAsRead(notification.id);

        if (notification.type === 'event' && notification.eventId) {
            const eventsTab = document.querySelector('[data-tab="scheduled"]');
            if (eventsTab) eventsTab.click();
        } else if (notification.type === 'admin-reply' && notification.questionId) {
            const helpTab = document.querySelector('[data-tab="help"]');
            if (helpTab) helpTab.click();
        } else if ((notification.type === 'new-question' || notification.type === 'new-answer') && notification.questionId) {
            const helpTab = document.querySelector('[data-tab="help"]');
            if (helpTab) helpTab.click();
        }
    };

    return (
        <div className="notification-bell-container" ref={dropdownRef}>
            <button
                className={`notification-bell-button ${unreadCount > 0 ? 'has-unread' : ''}`}
                onClick={() => {
                    setIsOpen(!isOpen);
                    // Автоматически отмечаем все как прочитанные при открытии
                    if (!isOpen && unreadCount > 0) {
                        markAllAsRead();
                    }
                }}
                title="Уведомления"
            >
                🔔
                {unreadCount > 0 && (
                    <span className="notification-badge">{unreadCount > 99 ? '99+' : unreadCount}</span>
                )}
            </button>

            {isOpen && (
                <div className="notification-dropdown">
                    <div className="notification-header">
                        <h3>Уведомления</h3>
                        {notifications.length > 0 && (
                            <div className="notification-actions">
                                <button
                                    className="clear-all"
                                    onClick={clearAll}
                                    title="Очистить все"
                                >
                                    🗑️
                                </button>
                            </div>
                        )}
                    </div>

                    <div className="notification-list">
                        {notifications.length === 0 ? (
                            <div className="no-notifications">
                                <span className="no-notif-icon">🔕</span>
                                <p>Нет уведомлений</p>
                            </div>
                        ) : (
                            notifications.map(notification => (
                                <div
                                    key={notification.id}
                                    className={`notification-item ${!notification.read ? 'unread' : ''}`}
                                    onClick={() => handleNotificationClick(notification)}
                                >
                                    <div className="notification-content">
                                        <div className="notification-title">{notification.title}</div>
                                        <div className="notification-message">{notification.message}</div>
                                        <div className="notification-time">{formatTimestamp(notification.timestamp)}</div>
                                    </div>
                                    <button
                                        className="notification-delete"
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            deleteNotification(notification.id);
                                        }}
                                        title="Удалить"
                                    >
                                        ✕
                                    </button>
                                </div>
                            ))
                        )}
                    </div>
                </div>
            )}
        </div>
    );
};

export default NotificationBell;
