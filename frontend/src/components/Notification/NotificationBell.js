import React, { useState, useEffect } from 'react';
import './NotificationBell.css';

const NotificationBell = () => {
    const [count, setCount] = useState(0);

    useEffect(() => {
        // TODO: В будущем — реальный запрос к API уведомлений
        // Пока демо-заглушка
        setCount(3);
    }, []);

    const handleClick = () => {
        // TODO: открыть панель уведомлений
        alert('🛎️ Уведомления (в следующих порциях будет полноценная панель)');
    };

    return (
        <button
            className="notification-bell"
            onClick={handleClick}
            title="Уведомления"
        >
            🛎️
            {count > 0 && (
                <span className="notification-count">
                    {count}
                </span>
            )}
        </button>
    );
};

export default NotificationBell;