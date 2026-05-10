// components/Scheduled/ScheduledEvents.js
import React, { useState, useEffect } from 'react';
import { eventsApi } from '../../services/api';
import { getUserRoles } from '../../keycloak';
import './ScheduledEvents.css';

const ScheduledEvents = () => {
    const [events, setEvents] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [isAdmin, setIsAdmin] = useState(false);
    const [showCreateForm, setShowCreateForm] = useState(false);
    const [newEvent, setNewEvent] = useState({
        title: '',
        description: '',
        startTime: '',
        links: []
    });

    useEffect(() => {
        const roles = getUserRoles();
        setIsAdmin(roles.includes('MODERATOR_ADMIN') || roles.includes('SUPER_ADMIN') || roles.includes('VYACHESLAV'));
        loadEvents();
    }, []);

    const loadEvents = async () => {
        try {
            setLoading(true);
            const response = await eventsApi.getAll();
            setEvents(response.data);
            setError(null);
        } catch (err) {
            console.error('Failed to load events:', err);
            setError('Не удалось загрузить события');
        } finally {
            setLoading(false);
        }
    };

    const handleCreateEvent = async () => {
        if (!newEvent.title || !newEvent.startTime) return;

        try {
            const response = await eventsApi.create(newEvent);
            setEvents(prev => [...prev, response.data]);
            setShowCreateForm(false);
            setNewEvent({ title: '', description: '', startTime: '', links: [] });
        } catch (err) {
            console.error('Failed to create event:', err);
            alert('Ошибка при создании события');
        }
    };

    const formatDate = (dateString) => {
        const date = new Date(dateString);
        return date.toLocaleString('ru-RU', {
            day: 'numeric',
            month: 'long',
            hour: '2-digit',
            minute: '2-digit'
        });
    };

    const isUpcoming = (dateString) => {
        return new Date(dateString) > new Date();
    };

    const upcomingEvents = events.filter(e => isUpcoming(e.startTime));
    const pastEvents = events.filter(e => !isUpcoming(e.startTime));

    return (
        <div className="scheduled-events">
            {isAdmin && (
                <div className="events-admin-bar">
                    <button
                        className="create-event-btn"
                        onClick={() => setShowCreateForm(!showCreateForm)}
                    >
                        {showCreateForm ? '✖ Отмена' : '+ Создать событие'}
                    </button>
                </div>
            )}

            {showCreateForm && (
                <div className="create-event-form">
                    <input
                        type="text"
                        placeholder="Название события"
                        value={newEvent.title}
                        onChange={(e) => setNewEvent({ ...newEvent, title: e.target.value })}
                    />
                    <textarea
                        placeholder="Описание"
                        value={newEvent.description}
                        onChange={(e) => setNewEvent({ ...newEvent, description: e.target.value })}
                        rows={3}
                    />
                    <input
                        type="datetime-local"
                        value={newEvent.startTime}
                        onChange={(e) => setNewEvent({ ...newEvent, startTime: e.target.value })}
                    />
                    <button onClick={handleCreateEvent}>Сохранить</button>
                </div>
            )}

            {loading ? (
                <div className="loading">Загрузка...</div>
            ) : error ? (
                <div className="error-message">{error}</div>
            ) : (
                <>
                    {upcomingEvents.length > 0 && (
                        <div className="events-section">
                            <h2 className="section-title">📅 Предстоящие события</h2>
                            <div className="events-list">
                                {upcomingEvents.map(event => (
                                    <div key={event.id} className="event-item">
                                        <div className="event-item-date">
                                            <span className="event-day">
                                                {new Date(event.startTime).getDate()}
                                            </span>
                                            <span className="event-month">
                                                {new Date(event.startTime).toLocaleString('ru-RU', { month: 'short' })}
                                            </span>
                                        </div>
                                        <div className="event-item-info">
                                            <h3 className="event-item-title">{event.title}</h3>
                                            <p className="event-item-description">{event.description}</p>
                                            <span className="event-item-time">
                                                🕒 {formatDate(event.startTime)}
                                            </span>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>
                    )}

                    {pastEvents.length > 0 && (
                        <div className="events-section">
                            <h2 className="section-title">📜 Прошедшие события</h2>
                            <div className="events-list past">
                                {pastEvents.map(event => (
                                    <div key={event.id} className="event-item past">
                                        <div className="event-item-date past">
                                            <span className="event-day">
                                                {new Date(event.startTime).getDate()}
                                            </span>
                                            <span className="event-month">
                                                {new Date(event.startTime).toLocaleString('ru-RU', { month: 'short' })}
                                            </span>
                                        </div>
                                        <div className="event-item-info">
                                            <h3 className="event-item-title">{event.title}</h3>
                                            <p className="event-item-description">{event.description}</p>
                                            <span className="event-item-time">
                                                🕒 {formatDate(event.startTime)}
                                            </span>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>
                    )}

                    {events.length === 0 && (
                        <div className="empty-state">
                            <p>📭 Нет запланированных событий</p>
                        </div>
                    )}
                </>
            )}
        </div>
    );
};

export default ScheduledEvents;