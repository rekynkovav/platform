import React, { useState, useEffect } from 'react';
import './QuestionNotification.css';

const QuestionNotification = () => {
    const [notifications, setNotifications] = useState([]);

    useEffect(() => {
        const handleNewNotification = (e) => {
            const notif = e.detail;
            if (notif.type === 'new_question' || notif.type === 'new_answer') {
                setNotifications(prev => [notif, ...prev]);
            }
        };

        window.addEventListener('newNotification', handleNewNotification);
        return () => window.removeEventListener('newNotification', handleNewNotification);
    }, []);

    const removeNotification = (id) => {
        setNotifications(prev => prev.filter(n => n.id !== id));
    };

    if (notifications.length === 0) return null;

    return (
        <div className="question-notifications">
            {notifications.map(notification => (
                <div
                    key={notification.id}
                    className={`notification ${notification.type}`}
                    onClick={() => removeNotification(notification.id)}
                >
                    {notification.type === 'new_question' ? (
                        <>
                            <div className="notification-header">
                                <span className="notification-icon">❓</span>
                                <span className="notification-title">Новый вопрос</span>
                                <button className="notification-close" onClick={(e) => { e.stopPropagation(); removeNotification(notification.id); }}>×</button>
                            </div>
                            <div className="notification-body">
                                <p className="notification-text">{notification.message}</p>
                            </div>
                        </>
                    ) : (
                        <>
                            <div className="notification-header">
                                <span className="notification-icon">✅</span>
                                <span className="notification-title">Получен ответ</span>
                                <button className="notification-close" onClick={(e) => { e.stopPropagation(); removeNotification(notification.id); }}>×</button>
                            </div>
                            <div className="notification-body">
                                <p className="notification-text">{notification.message}</p>
                            </div>
                        </>
                    )}
                </div>
            ))}
        </div>
    );
};

export default QuestionNotification;
