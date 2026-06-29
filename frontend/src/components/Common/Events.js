import React, { useState, useEffect, useMemo } from 'react';
import { FixedSizeList as List } from 'react-window';
import { AutoSizer } from 'react-virtualized-auto-sizer';
import { eventsApi } from '../../services/api';
import './Events.css';

/**
 * Events — displays a filterable, virtualized list of events with registration support.
 *
 * Shows upcoming, ongoing, past, and registered events in a list or calendar view.
 * Includes event details modal, participant tracking, and registration/unregistration
 * actions. Uses mock data for demonstration purposes.
 *
 * @param {string} [filter='upcoming'] - Event filter: 'upcoming', 'ongoing', 'past', or 'registered'
 * @param {number} [limit=20] - Maximum number of events to display
 * @returns {JSX.Element} The events list with filters, stats, and detail modal
 */
const Events = ({ filter = 'upcoming', limit = 20 }) => {
    const [events, setEvents] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [selectedEvent, setSelectedEvent] = useState(null);
    const [viewMode, setViewMode] = useState('list'); // 'list' или 'calendar'

    useEffect(() => {
        const fetchEvents = async () => {
            try {
                setLoading(true);
                // В реальном приложении здесь будет вызов API
                // const data = await eventsApi.getAll();

                // Мок-данные для демонстрации
                const mockEvents = Array.from({ length: 50 }, (_, i) => {
                    const now = new Date();
                    const startDate = new Date(now);
                    startDate.setDate(startDate.getDate() + Math.floor(Math.random() * 30) - 15);
                    startDate.setHours(9 + Math.floor(Math.random() * 8), 0, 0, 0);

                    const endDate = new Date(startDate);
                    endDate.setHours(startDate.getHours() + Math.floor(Math.random() * 4) + 1);

                    const types = ['webinar', 'meeting', 'workshop', 'conference', 'training'];
                    const statuses = ['upcoming', 'ongoing', 'completed', 'cancelled'];
                    const categories = ['nutrition', 'fitness', 'wellness', 'education', 'community'];

                    return {
                        id: `event-${i + 1}`,
                        title: `Событие ${i + 1}: ${['Вебинар по питанию', 'Встреча сообщества', 'Тренинг по фитнесу', 'Конференция по здоровью'][i % 4]}`,
                        description: `Подробное описание события ${i + 1}. Это важное мероприятие для всех участников сообщества.`,
                        startDate: startDate.toISOString(),
                        endDate: endDate.toISOString(),
                        location: i % 3 === 0 ? 'Онлайн' : ['Москва', 'Санкт-Петербург', 'Новосибирск'][i % 3],
                        type: types[i % types.length],
                        status: statuses[i % statuses.length],
                        category: categories[i % categories.length],
                        organizer: `Организатор ${Math.floor(i / 10) + 1}`,
                        maxParticipants: 50 + Math.floor(Math.random() * 100),
                        currentParticipants: Math.floor(Math.random() * 50),
                        isRegistered: Math.random() > 0.7,
                        color: `hsl(${Math.random() * 360}, 70%, 60%)`
                    };
                });

                setEvents(mockEvents);
            } catch (err) {
                setError('Не удалось загрузить события');
            } finally {
                setLoading(false);
            }
        };

        fetchEvents();

        // Обновляем статусы событий каждую минуту
        const interval = setInterval(() => {
            setEvents(prev => prev.map(event => {
                const now = new Date();
                const start = new Date(event.startDate);
                const end = new Date(event.endDate);

                let status = event.status;
                if (now < start) status = 'upcoming';
                else if (now >= start && now <= end) status = 'ongoing';
                else if (now > end && event.status !== 'cancelled') status = 'completed';

                return { ...event, status };
            }));
        }, 60000);

        return () => clearInterval(interval);
    }, []);

    const filteredEvents = useMemo(() => {
        let filtered = events;

        if (filter === 'upcoming') {
            filtered = filtered.filter(e => e.status === 'upcoming');
        } else if (filter === 'ongoing') {
            filtered = filtered.filter(e => e.status === 'ongoing');
        } else if (filter === 'past') {
            filtered = filtered.filter(e => e.status === 'completed');
        } else if (filter === 'registered') {
            filtered = filtered.filter(e => e.isRegistered);
        }

        // Сортируем по дате
        filtered.sort((a, b) => new Date(a.startDate) - new Date(b.startDate));

        return filtered.slice(0, limit);
    }, [events, filter, limit]);

    const clearError = () => {
        setError(null);
    };

    const getStatusColor = (status) => {
        switch (status) {
            case 'upcoming': return '#4F46E5';
            case 'ongoing': return '#10B981';
            case 'completed': return '#6B7280';
            case 'cancelled': return '#EF4444';
            default: return '#6B7280';
        }
    };

    const getStatusText = (status) => {
        switch (status) {
            case 'upcoming': return 'Предстоящее';
            case 'ongoing': return 'Идет сейчас';
            case 'completed': return 'Завершено';
            case 'cancelled': return 'Отменено';
            default: return 'Неизвестно';
        }
    };

    const getTypeIcon = (type) => {
        switch (type) {
            case 'webinar': return '🎓';
            case 'meeting': return '👥';
            case 'workshop': return '🔧';
            case 'conference': return '🏛️';
            case 'training': return '💪';
            default: return '📅';
        }
    };

    const formatDate = (dateString) => {
        const date = new Date(dateString);
        return date.toLocaleDateString('ru-RU', {
            day: 'numeric',
            month: 'short',
            year: 'numeric'
        });
    };

    const formatTime = (dateString) => {
        const date = new Date(dateString);
        return date.toLocaleTimeString('ru-RU', {
            hour: '2-digit',
            minute: '2-digit'
        });
    };

    const formatDateTime = (startDate, endDate) => {
        const start = new Date(startDate);
        const end = new Date(endDate);
        const sameDay = start.toDateString() === end.toDateString();

        if (sameDay) {
            return `${formatDate(startDate)}, ${formatTime(startDate)} - ${formatTime(endDate)}`;
        } else {
            return `${formatDate(startDate)} ${formatTime(startDate)} - ${formatDate(endDate)} ${formatTime(endDate)}`;
        }
    };

    const handleRegister = async (eventId) => {
        try {
            // В реальном приложении здесь будет вызов API
            // await eventsApi.register(eventId);

            setEvents(prev =>
                prev.map(event =>
                    event.id === eventId
                        ? {
                            ...event,
                            isRegistered: true,
                            currentParticipants: event.currentParticipants + 1
                        }
                        : event
                )
            );
        } catch (err) {
            alert('Не удалось зарегистрироваться на событие');
        }
    };

    const handleUnregister = async (eventId) => {
        try {
            // В реальном приложении здесь будет вызов API
            // await eventsApi.unregister(eventId);

            setEvents(prev =>
                prev.map(event =>
                    event.id === eventId
                        ? {
                            ...event,
                            isRegistered: false,
                            currentParticipants: Math.max(0, event.currentParticipants - 1)
                        }
                        : event
                )
            );
        } catch (err) {
            alert('Не удалось отменить регистрацию');
        }
    };

    const EventItem = ({ index, style }) => {
        const event = filteredEvents[index];
        const isSelected = selectedEvent?.id === event.id;

        return (
            <div
                style={style}
                className={`event-item ${isSelected ? 'selected' : ''}`}
                onClick={() => setSelectedEvent(event)}
            >
                <div className="event-header">
                    <div className="event-type-icon">
                        {getTypeIcon(event.type)}
                    </div>
                    <div className="event-title">{event.title}</div>
                    <div
                        className="event-status"
                        style={{ backgroundColor: getStatusColor(event.status) }}
                    >
                        {getStatusText(event.status)}
                    </div>
                </div>

                <div className="event-details">
                    <div className="event-time">
                        <span className="detail-icon">🕒</span>
                        <span>{formatDateTime(event.startDate, event.endDate)}</span>
                    </div>
                    <div className="event-location">
                        <span className="detail-icon">📍</span>
                        <span>{event.location}</span>
                    </div>
                    <div className="event-category">
                        <span className="detail-icon">🏷️</span>
                        <span>{event.category}</span>
                    </div>
                </div>

                <div className="event-participants">
                    <div className="participants-count">
                        <span className="detail-icon">👥</span>
                        <span>{event.currentParticipants}/{event.maxParticipants}</span>
                    </div>
                    <div className="event-actions">
                        {event.status === 'upcoming' && (
                            event.isRegistered ? (
                                <button
                                    className="btn-unregister"
                                    onClick={(e) => {
                                        e.stopPropagation();
                                        handleUnregister(event.id);
                                    }}
                                >
                                    Отменить регистрацию
                                </button>
                            ) : (
                                <button
                                    className="btn-register"
                                    onClick={(e) => {
                                        e.stopPropagation();
                                        handleRegister(event.id);
                                    }}
                                    disabled={event.currentParticipants >= event.maxParticipants}
                                >
                                    {event.currentParticipants >= event.maxParticipants ? 'Мест нет' : 'Зарегистрироваться'}
                                </button>
                            )
                        )}
                    </div>
                </div>
            </div>
        );
    };

    if (loading) {
        return (
            <div className="events-container loading">
                <div className="loading-spinner"></div>
                <span>Загрузка событий...</span>
            </div>
        );
    }

    if (error) {
        return (
            <div className="events-container error">
                <div className="error-icon">⚠️</div>
                <span>{error}</span>
            </div>
        );
    }

    return (
        <div className="events-container">
            {/* Отображение ошибок */}
            {error && (
                <div className="error-message">
                    <span className="error-icon">⚠️</span>
                    <span>{error}</span>
                    <button className="error-close" onClick={clearError}>×</button>
                </div>
            )}

            <div className="events-header">
                <h2>📅 События</h2>
                <div className="events-controls">
                    <div className="filter-buttons">
                        <button
                            className={`filter-btn ${filter === 'upcoming' ? 'active' : ''}`}
                            onClick={() => setSelectedEvent(null)}
                        >
                            Предстоящие
                        </button>
                        <button
                            className={`filter-btn ${filter === 'ongoing' ? 'active' : ''}`}
                            onClick={() => setSelectedEvent(null)}
                        >
                            Сейчас
                        </button>
                        <button
                            className={`filter-btn ${filter === 'past' ? 'active' : ''}`}
                            onClick={() => setSelectedEvent(null)}
                        >
                            Прошедшие
                        </button>
                        <button
                            className={`filter-btn ${filter === 'registered' ? 'active' : ''}`}
                            onClick={() => setSelectedEvent(null)}
                        >
                            Мои
                        </button>
                    </div>
                    <div className="view-toggle">
                        <button
                            className={`view-btn ${viewMode === 'list' ? 'active' : ''}`}
                            onClick={() => setViewMode('list')}
                            title="Список"
                        >
                            📋
                        </button>
                        <button
                            className={`view-btn ${viewMode === 'calendar' ? 'active' : ''}`}
                            onClick={() => setViewMode('calendar')}
                            title="Календарь"
                        >
                            📅
                        </button>
                    </div>
                </div>
            </div>

            <div className="events-stats">
                <div className="stat-item">
                    <div className="stat-value">{filteredEvents.length}</div>
                    <div className="stat-label">Всего событий</div>
                </div>
                <div className="stat-item">
                    <div className="stat-value">
                        {filteredEvents.filter(e => e.status === 'upcoming').length}
                    </div>
                    <div className="stat-label">Предстоящих</div>
                </div>
                <div className="stat-item">
                    <div className="stat-value">
                        {filteredEvents.filter(e => e.isRegistered).length}
                    </div>
                    <div className="stat-label">Мои регистрации</div>
                </div>
            </div>

            {viewMode === 'list' ? (
                <div className="events-list" style={{ height: '500px' }}>
                    <AutoSizer>
                        {({ height, width }) => (
                            <List
                                height={height}
                                width={width}
                                itemCount={filteredEvents.length}
                                itemSize={140}
                            >
                                {EventItem}
                            </List>
                        )}
                    </AutoSizer>
                </div>
            ) : (
                <div className="events-calendar">
                    <div className="calendar-placeholder">
                        📅 Календарь событий
                        <p>Здесь будет отображение событий в календарном виде</p>
                    </div>
                </div>
            )}

            {selectedEvent && (
                <div className="event-detail-modal">
                    <div className="modal-content">
                        <button
                            className="modal-close"
                            onClick={() => setSelectedEvent(null)}
                        >
                            ×
                        </button>
                        <div className="event-detail-header">
                            <div className="event-detail-type">
                                {getTypeIcon(selectedEvent.type)} {selectedEvent.type}
                            </div>
                            <h3>{selectedEvent.title}</h3>
                            <div
                                className="event-detail-status"
                                style={{ backgroundColor: getStatusColor(selectedEvent.status) }}
                            >
                                {getStatusText(selectedEvent.status)}
                            </div>
                        </div>

                        <div className="event-detail-body">
                            <div className="detail-section">
                                <h4>📝 Описание</h4>
                                <p>{selectedEvent.description}</p>
                            </div>

                            <div className="detail-grid">
                                <div className="detail-item">
                                    <div className="detail-label">🕒 Время</div>
                                    <div className="detail-value">
                                        {formatDateTime(selectedEvent.startDate, selectedEvent.endDate)}
                                    </div>
                                </div>
                                <div className="detail-item">
                                    <div className="detail-label">📍 Место</div>
                                    <div className="detail-value">{selectedEvent.location}</div>
                                </div>
                                <div className="detail-item">
                                    <div className="detail-label">🏷️ Категория</div>
                                    <div className="detail-value">{selectedEvent.category}</div>
                                </div>
                                <div className="detail-item">
                                    <div className="detail-label">👨‍💼 Организатор</div>
                                    <div className="detail-value">{selectedEvent.organizer}</div>
                                </div>
                            </div>

                            <div className="detail-section">
                                <h4>👥 Участники</h4>
                                <div className="participants-progress">
                                    <div className="progress-bar">
                                        <div
                                            className="progress-fill"
                                            style={{
                                                width: `${(selectedEvent.currentParticipants / selectedEvent.maxParticipants) * 100}%`
                                            }}
                                        ></div>
                                    </div>
                                    <div className="progress-text">
                                        {selectedEvent.currentParticipants} из {selectedEvent.maxParticipants} мест занято
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div className="event-detail-footer">
                            {selectedEvent.status === 'upcoming' && (
                                selectedEvent.isRegistered ? (
                                    <button
                                        className="btn-unregister-large"
                                        onClick={() => handleUnregister(selectedEvent.id)}
                                    >
                                        Отменить регистрацию
                                    </button>
                                ) : (
                                    <button
                                        className="btn-register-large"
                                        onClick={() => handleRegister(selectedEvent.id)}
                                        disabled={selectedEvent.currentParticipants >= selectedEvent.maxParticipants}
                                    >
                                        {selectedEvent.currentParticipants >= selectedEvent.maxParticipants
                                            ? 'Мест нет'
                                            : 'Зарегистрироваться на событие'}
                                    </button>
                                )
                            )}
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default Events;