import React, { useState, useEffect } from 'react';
import { eventsApi } from '../../services/api';
import './GlobalEventNotification.css';

const GlobalEventNotification = ({ onEventClick }) => {
    const [nextEvent, setNextEvent] = useState(null);
    const [visible, setVisible] = useState(false);

    useEffect(() => {
        const fetchNextEvent = async () => {
            try {
                const response = await eventsApi.getNext();
                if (response?.data) {
                    setNextEvent(response.data);
                    setVisible(true);
                }
            } catch (err) {
                // Тихо игнорируем — уведомление просто не покажется
                console.error('Failed to load next event for notification', err);
            }
        };

        fetchNextEvent();
    }, []);

    if (!visible || !nextEvent) return null;

    return (
        <div className="global-event-notification" onClick={onEventClick}>
            <div className="notification-content">
                <span className="notification-icon">📅</span>
                <div className="notification-text">
                    <strong>Ближайшее событие:</strong>
                    <span>{nextEvent.title}</span>
                </div>
                <button
                    className="notification-close"
                    onClick={(e) => {
                        e.stopPropagation();
                        setVisible(false);
                    }}
                >
                    ✕
                </button>
            </div>
        </div>
    );
};

export default GlobalEventNotification;