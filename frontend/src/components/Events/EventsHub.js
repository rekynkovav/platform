import React, { useState, useEffect } from 'react';
import { eventsApi } from '../../services/api';
import './EventsHub.css';

const EventsHub = () => {
    const [events, setEvents] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    const loadUpcomingEvents = async () => {
        try {
            setLoading(true);
            setError(null);
            const response = await eventsApi.getUpcoming();
            setEvents(Array.isArray(response.data) ? response.data : []);
        } catch (err) {
            console.error('Failed to load upcoming events', err);
            setError('Не удалось загрузить предстоящие события');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadUpcomingEvents();
    }, []);

    if (loading) return <div className="loading">Загрузка предстоящих событий...</div>;
    if (error) return <div className="error-message">{error}</div>;

    return (
        <div className="events-hub">
            <h2>📅 Предстоящие события</h2>

            {events.length === 0 ? (
                <p className="no-events">В ближайшее время событий нет</p>
            ) : (
                <div className="events-list">
                    {events.map(event => (
                        <div key={event.id} className="event-card">
                            <h3>{event.title}</h3>
                            <div className="event-date">
                                {new Date(event.startTime).toLocaleString('ru-RU', {
                                    day: 'numeric',
                                    month: 'long',
                                    year: 'numeric',
                                    hour: '2-digit',
                                    minute: '2-digit'
                                })}
                            </div>
                            {event.description && (
                                <p className="event-description">{event.description}</p>
                            )}
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

export default EventsHub;