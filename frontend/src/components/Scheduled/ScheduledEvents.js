import React, { useState, useEffect } from 'react';
import { eventsApi } from '../../services/api';
import { mockEventsApi } from '../../services/mockApi';
import './ScheduledEvents.css';

const ScheduledEvents = () => {
    const [events, setEvents] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [demoRole] = useState(() => localStorage.getItem('demoRole') || 'USER_0');

    const useMock = demoRole === 'USER_0' || !demoRole;
    const api = useMock ? mockEventsApi : eventsApi;

    const loadEvents = async () => {
        try {
            setLoading(true);
            setError(null);
            const response = await api.getAll();
            setEvents(Array.isArray(response.data) ? response.data : []);
        } catch (err) {
            console.error('Failed to load events', err);
            setError('Не удалось загрузить события');
            setEvents([]);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadEvents();
    }, [demoRole]);

    const isAdmin = ['MODERATOR_ADMIN', 'SUPER_ADMIN', 'VYACHESLAV', 'DEVELOPER'].includes(demoRole);

    if (loading) return <div className="loading">Загрузка событий...</div>;
    if (error) return <div className="error-message">{error}</div>;

    return (
        <div className="scheduled-events">
            <div className="broadcasts-header">
                <h2>📅 Запланированные события</h2>
                <p>Все предстоящие и прошедшие трансляции</p>
            </div>

            {isAdmin && (
                <div className="create-broadcast-section">
                    <button className="create-broadcast-btn">+ Создать новое событие</button>
                </div>
            )}

            <div className="broadcasts-grid">
                {events.length === 0 ? (
                    <p className="no-events">Пока нет запланированных событий</p>
                ) : (
                    events.map(event => (
                        <div key={event.id} className="broadcast-card">
                            <h3 className="broadcast-title">{event.title}</h3>
                            <div className="broadcast-datetime">
                                {new Date(event.startTime).toLocaleString('ru-RU')}
                            </div>
                            {event.description && (
                                <p className="broadcast-description">{event.description}</p>
                            )}
                            <div className="broadcast-status">{event.status || 'scheduled'}</div>
                        </div>
                    ))
                )}
            </div>
        </div>
    );
};

export default ScheduledEvents;