import React, { useState, useEffect, useRef } from 'react';
import { getUserRoles } from '../../keycloak';
import './ScheduledEvents.css';

const ScheduledEvents = () => {
    const [events, setEvents] = useState([]);
    const [newEvent, setNewEvent] = useState({
        title: '',
        date: '',
        time: '',
        description: ''
    });
    const [showAddForm, setShowAddForm] = useState(false);
    const [userRoles, setUserRoles] = useState([]);
    const [notificationEvent, setNotificationEvent] = useState(null);
    const [deleteNotification, setDeleteNotification] = useState(null);

    // Состояния для модального окна подтверждения удаления
    const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
    const [eventToDelete, setEventToDelete] = useState(null);

    // Состояния для редактирования
    const [editingEventId, setEditingEventId] = useState(null);
    const [showEditForm, setShowEditForm] = useState(false);

    const notificationTimeoutRef = useRef(null);

    const getCurrentDate = () => {
        const today = new Date();
        const year = today.getFullYear();
        const month = String(today.getMonth() + 1).padStart(2, '0');
        const day = String(today.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    };

    const getCurrentTime = () => {
        const now = new Date();
        const hours = String(now.getHours()).padStart(2, '0');
        const minutes = String(now.getMinutes()).padStart(2, '0');
        return `${hours}:${minutes}`;
    };

    useEffect(() => {
        const savedEvents = localStorage.getItem('scheduledEvents');
        if (savedEvents) {
            const parsed = JSON.parse(savedEvents);
            // Сортируем события по дате и времени (ближайшие сверху)
            const sorted = parsed.sort((a, b) => {
                const dateA = new Date(`${a.date}T${a.time || '00:00'}`);
                const dateB = new Date(`${b.date}T${b.time || '00:00'}`);
                return dateA - dateB;
            });
            setEvents(sorted);
        } else {
            const initialEvents = [
                { id: 1, title: "Открытие сезона", date: "2025-04-10", time: "15:00", description: "Открытие весеннего сезона. Первая встреча в новом формате." },
                { id: 2, title: "Практикум по дыханию", date: "2025-04-12", time: "11:00", description: "Практикум по дыхательным техникам." },
                { id: 3, title: "Лекция о силе духа", date: "2025-04-15", time: "18:30", description: "Лекция о силе духа." }
            ];
            setEvents(initialEvents);
            localStorage.setItem('scheduledEvents', JSON.stringify(initialEvents));
        }

        // Получаем роль из localStorage (от переключателя)
        const demoRole = localStorage.getItem('demoRole');
        let roles = [];

        if (demoRole === 'admin_moderator' || demoRole === 'developer' || demoRole === 'super_admin') {
            roles = ['admin', 'user'];
        } else if (demoRole === 'user') {
            roles = ['user'];
        } else {
            roles = getUserRoles();
        }

        setUserRoles(roles);

        setNewEvent(prev => ({
            ...prev,
            date: getCurrentDate(),
            time: getCurrentTime()
        }));
    }, []);

    // Cleanup notification timeout on unmount
    useEffect(() => {
        return () => {
            if (notificationTimeoutRef.current) {
                clearTimeout(notificationTimeoutRef.current);
            }
        };
    }, []);

    // Проверяем роль из localStorage напрямую
    const demoRole = localStorage.getItem('demoRole');
    const isAdmin = demoRole === 'admin_moderator' || demoRole === 'developer' || demoRole === 'super_admin';
    const canEdit = demoRole === 'admin_moderator' || demoRole === 'super_admin';

    const formatDate = (dateString) => {
        const date = new Date(dateString);
        const months = ['января', 'февраля', 'марта', 'апреля', 'мая', 'июня', 'июля', 'августа', 'сентября', 'октября', 'ноября', 'декабря'];
        return `${date.getDate()} ${months[date.getMonth()]} ${date.getFullYear()}`;
    };

    const handleAddEvent = () => {
        if (!isAdmin) {
            alert('Только администратор может добавлять события');
            return;
        }

        if (!newEvent.title || !newEvent.date) {
            alert('Заполните название и дату события');
            return;
        }

        if (editingEventId) {
            // Редактирование существующего события
            const updatedEvents = events.map(event =>
                event.id === editingEventId
                    ? { ...event, ...newEvent, edited: true }
                    : event
            );
            const sorted = updatedEvents.sort((a, b) => {
                const dateA = new Date(`${a.date}T${a.time || '00:00'}`);
                const dateB = new Date(`${b.date}T${b.time || '00:00'}`);
                return dateA - dateB;
            });
            setEvents(sorted);
            localStorage.setItem('scheduledEvents', JSON.stringify(sorted));
            setEditingEventId(null);
            setShowEditForm(false);
        } else {
            // Создание нового события
            const event = {
                id: Date.now(),
                ...newEvent,
            };

            const updatedEvents = [...events, event];
            const sorted = updatedEvents.sort((a, b) => {
                const dateA = new Date(`${a.date}T${a.time || '00:00'}`);
                const dateB = new Date(`${b.date}T${b.time || '00:00'}`);
                return dateA - dateB;
            });
            setEvents(sorted);
            localStorage.setItem('scheduledEvents', JSON.stringify(sorted));

            // Показываем уведомление
            setNotificationEvent(event);

            // Автоматически скрываем через 30 секунд
            notificationTimeoutRef.current = setTimeout(() => {
                setNotificationEvent(null);
            }, 30000);
        }

        setNewEvent({
            title: '',
            date: getCurrentDate(),
            time: getCurrentTime(),
            description: ''
        });
        setShowAddForm(false);
    };

    // Функция для открытия модального окна подтверждения удаления
    const confirmDeleteEvent = (event) => {
        if (!canEdit) {
            alert('У вас нет прав на удаление событий');
            return;
        }
        setEventToDelete(event);
        setShowDeleteConfirm(true);
    };

    // Функция для редактирования события
    const handleEditEvent = (event) => {
        if (!canEdit) {
            alert('У вас нет прав на редактирование событий');
            return;
        }
        setEditingEventId(event.id);
        setNewEvent({
            title: event.title,
            date: event.date,
            time: event.time,
            description: event.description || ''
        });
        setShowEditForm(true);
    };

    // Функция для отмены редактирования
    const handleCancelEdit = () => {
        setEditingEventId(null);
        setShowEditForm(false);
        setNewEvent({
            title: '',
            date: getCurrentDate(),
            time: getCurrentTime(),
            description: ''
        });
    };

    // Функция для фактического удаления
    const executeDeleteEvent = () => {
        if (!eventToDelete) return;

        const demoRole = localStorage.getItem('demoRole');
        const canDelete = demoRole === 'admin_moderator' || demoRole === 'developer' || demoRole === 'super_admin';
        if (!canDelete) {
            alert('Только администратор может удалять события!');
            setShowDeleteConfirm(false);
            setEventToDelete(null);
            return;
        }

        const updatedEvents = events.filter(event => event.id !== eventToDelete.id);
        setEvents(updatedEvents);
        localStorage.setItem('scheduledEvents', JSON.stringify(updatedEvents));

        // Показываем уведомление об удалении
        setDeleteNotification(eventToDelete?.title);
        notificationTimeoutRef.current = setTimeout(() => setDeleteNotification(null), 3000);

        setShowDeleteConfirm(false);
        setEventToDelete(null);
    };

    // Функция для отмены удаления
    const cancelDeleteEvent = () => {
        setShowDeleteConfirm(false);
        setEventToDelete(null);
        setDeleteNotification(`❌ Удаление отменено`);
        notificationTimeoutRef.current = setTimeout(() => setDeleteNotification(null), 2000);
    };

    const scrollToEventsTab = () => {
        const eventsTab = document.querySelector('.nav-tab[data-tab="scheduled"]');
        if (eventsTab) {
            eventsTab.click();
        }
        setNotificationEvent(null);
    };

    return (
        <div className="scheduled-events">
            {/* Модальное окно подтверждения удаления */}
            {showDeleteConfirm && eventToDelete && (
                <div className="modal-overlay" onClick={cancelDeleteEvent}>
                    <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                        <div className="modal-header">
                            <span className="modal-icon">🗑️</span>
                            <h3>Подтверждение удаления</h3>
                        </div>
                        <div className="modal-body">
                            <p>Вы действительно хотите удалить событие?</p>
                            <div className="event-preview">
                                <strong>{eventToDelete.title}</strong>
                                <div className="event-details-preview">
                                    <span>📅 {formatDate(eventToDelete.date)}</span>
                                    <span>⏰ {eventToDelete.time}</span>
                                </div>
                                {eventToDelete.description && (
                                    <small>{eventToDelete.description}</small>
                                )}
                            </div>
                            <p className="warning-text">Это действие невозможно отменить!</p>
                        </div>
                        <div className="modal-footer">
                            <button className="modal-btn cancel" onClick={cancelDeleteEvent}>
                                ❌ Отмена
                            </button>
                            <button className="modal-btn confirm" onClick={executeDeleteEvent}>
                                ✅ Да, удалить
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Уведомление об удалении */}
            {deleteNotification && (
                <div className="delete-notification">
                    <div className="notification-content">
                        <div className="notification-icon">{deleteNotification.includes('отменено') ? '❌' : '🗑️'}</div>
                        <div className="notification-text">
                            <strong>{deleteNotification.includes('отменено') ? 'Удаление отменено' : 'Событие удалено'}</strong>
                            <span>{deleteNotification.replace('❌ ', '').replace('🗑️ ', '')}</span>
                        </div>
                        <div className="notification-close" onClick={() => setDeleteNotification(null)}>✕</div>
                    </div>
                </div>
            )}

            {/* Уведомление о новом событии */}
            {notificationEvent && isAdmin && (
                <div className="event-countdown" onClick={scrollToEventsTab}>
                    <div className="countdown-content">
                        <button className="countdown-close" onClick={(e) => { e.stopPropagation(); setNotificationEvent(null); }}>✕</button>
                        <div className="countdown-icon">🎉</div>
                        <div className="countdown-text">
                            <strong>Ближайшее Событие</strong>
                            <span>{notificationEvent.title}</span>
                        </div>
                        <div className="countdown-timer">
                            <EventTimer date={notificationEvent.date} time={notificationEvent.time} />
                        </div>
                        <div className="countdown-arrow">→</div>
                    </div>
                </div>
            )}

            {/* Кнопка добавления события - только для админа */}
            {isAdmin && (
                <div className="admin-controls-events">
                    <button
                        className="add-event-button"
                        onClick={() => setShowAddForm(true)}
                    >
                        + Добавить событие
                    </button>
                </div>
            )}

            {/* Модальное окно формы добавления/редактирования события */}
            {isAdmin && (showAddForm || showEditForm) && (
                <div className="modal-overlay" onClick={() => {
                    setShowAddForm(false);
                    setShowEditForm(false);
                    handleCancelEdit();
                }}>
                    <div className="modal-content add-event-modal" onClick={(e) => e.stopPropagation()}>
                        <h3>{editingEventId ? 'Редактировать событие' : 'Новое событие'}</h3>
                        <input
                            type="text"
                            placeholder="Название события"
                            value={newEvent.title}
                            onChange={(e) => setNewEvent({...newEvent, title: e.target.value})}
                        />
                        <div className="date-time-group">
                            <div className="date-field">
                                <label>📅 Дата</label>
                                <input
                                    type="date"
                                    value={newEvent.date}
                                    onChange={(e) => setNewEvent({...newEvent, date: e.target.value})}
                                />
                            </div>
                            <div className="time-field">
                                <label>⏰ Время</label>
                                <input
                                    type="time"
                                    value={newEvent.time}
                                    onChange={(e) => setNewEvent({...newEvent, time: e.target.value})}
                                />
                            </div>
                        </div>
                        <textarea
                            placeholder="Описание события"
                            value={newEvent.description}
                            onChange={(e) => setNewEvent({...newEvent, description: e.target.value})}
                            rows="3"
                        />
                        <div className="modal-buttons-vertical">
                            <button className="modal-btn-create" onClick={handleAddEvent}>
                                {editingEventId ? 'Сохранить изменения' : 'Создать событие'}
                            </button>
                            <button className="modal-btn-cancel" onClick={() => {
                                setShowAddForm(false);
                                setShowEditForm(false);
                                handleCancelEdit();
                            }}>
                                Отмена
                            </button>
                        </div>
                    </div>
                </div>
            )}

            <div className="events-list">
                {events.length === 0 ? (
                    <div className="no-events">
                        <p>📭 Нет запланированных событий</p>
                        {isAdmin && <p className="hint">Нажмите "+ Добавить событие" чтобы создать первое событие</p>}
                    </div>
                ) : (
                    events.map((event) => (
                        <div key={event.id} className="event-card">
                            <div className="event-header">
                                <div className="event-title-section">
                                    <h3 className="event-title">
                                        {event.title}
                                        {event.edited && <span className="edited-badge"> (изменено)</span>}
                                    </h3>
                                    <span className="event-date">{formatDate(event.date)} {event.time}</span>
                                </div>
                                <div className="event-actions">
                                    {/* Кнопка редактирования - только для admin_moderator и super_admin */}
                                    {canEdit && (
                                        <button
                                            className="edit-event-btn"
                                            onClick={(e) => {
                                                e.stopPropagation();
                                                handleEditEvent(event);
                                            }}
                                            title="Редактировать событие"
                                        >
                                            ✏️
                                        </button>
                                    )}
                                    {/* Кнопка удаления - только для admin_moderator и super_admin */}
                                    {canEdit && (
                                        <button
                                            className="delete-event-btn"
                                            onClick={(e) => {
                                                e.stopPropagation();
                                                confirmDeleteEvent(event);
                                            }}
                                            title="Удалить событие"
                                        >
                                            🗑️
                                        </button>
                                    )}
                                </div>
                            </div>

                            <div className="event-info">
                                {event.description && (
                                    <p className="event-description">{event.description}</p>
                                )}
                            </div>
                        </div>
                    ))
                )}
            </div>
        </div>
    );
};

// Компонент таймера
const EventTimer = ({ date, time }) => {
    const [timeLeft, setTimeLeft] = useState({});

    useEffect(() => {
        const calculateTimeLeft = () => {
            const eventDate = new Date(`${date}T${time || '00:00'}`);
            const now = new Date();
            const difference = eventDate - now;

            if (difference <= 0) {
                return { expired: true };
            }

            const days = Math.floor(difference / (1000 * 60 * 60 * 24));
            const hours = Math.floor((difference % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
            const minutes = Math.floor((difference % (1000 * 60 * 60)) / (1000 * 60));
            const seconds = Math.floor((difference % (1000 * 60)) / 1000);

            return { days, hours, minutes, seconds, expired: false };
        };

        setTimeLeft(calculateTimeLeft());

        const timer = setInterval(() => {
            setTimeLeft(calculateTimeLeft());
        }, 1000);

        return () => clearInterval(timer);
    }, [date, time]);

    if (timeLeft.expired) {
        return <span>Событие началось!</span>;
    }

    return (
        <span>
            {timeLeft.days > 0 && `${timeLeft.days}д `}
            {timeLeft.hours > 0 && `${timeLeft.hours}ч `}
            {timeLeft.minutes}м {timeLeft.seconds}с
        </span>
    );
};

export default ScheduledEvents;