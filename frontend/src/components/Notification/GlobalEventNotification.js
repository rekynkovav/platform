import React, { useState, useEffect, useRef } from 'react';
import './GlobalEventNotification.css';

const GlobalEventNotification = ({ onEventClick }) => {
    const [activeEvent, setActiveEvent] = useState(null);
    const [timeLeft, setTimeLeft] = useState({});
    const [error, setError] = useState(null);
    const previousEventIdRef = useRef(null);

    const clearError = () => {
        setError(null);
    };

    useEffect(() => {
        const checkEvents = () => {
            try {
                const savedEvents = process.env.NODE_ENV === 'development'
                    ? sessionStorage.getItem('scheduledEvents')
                    : null;
                if (!savedEvents) {
                    setActiveEvent(null);
                    return;
                }

                const events = JSON.parse(savedEvents);
                const now = new Date();

            // Фильтруем только будущие события и сортируем по дате
            const upcomingEvents = events.filter(event => {
                const eventDate = new Date(`${event.date}T${event.time || '00:00'}`);
                return eventDate > now;
            }).sort((a, b) => {
                const dateA = new Date(`${a.date}T${a.time || '00:00'}`);
                const dateB = new Date(`${b.date}T${b.time || '00:00'}`);
                return dateA - dateB;
            });

            if (upcomingEvents.length > 0) {
                const nearestEvent = upcomingEvents[0];
                const eventDate = new Date(`${nearestEvent.date}T${nearestEvent.time || '00:00'}`);
                const daysDiff = (eventDate - now) / (1000 * 60 * 60 * 24);

                // Показываем уведомление только если событие в пределах 7 дней
                if (daysDiff <= 7 && daysDiff > 0) {
                    // Проверяем, изменилось ли событие (по ID или дате)
                    const currentEventId = `${nearestEvent.id || nearestEvent.date}_${nearestEvent.time}`;
                    if (previousEventIdRef.current !== currentEventId) {
                        previousEventIdRef.current = currentEventId;
                        setActiveEvent(nearestEvent);
                    } else if (!activeEvent || activeEvent.id !== nearestEvent.id) {
                        // Обновляем, если activeEvent не установлен или изменился
                        setActiveEvent(nearestEvent);
                    }
                } else {
                    setActiveEvent(null);
                    previousEventIdRef.current = null;
                }
            } else {
                setActiveEvent(null);
                previousEventIdRef.current = null;
            }
        } catch (error) {
            console.error('Ошибка при проверке событий:', error);
            setError('Не удалось загрузить события. Проверьте данные в sessionStorage.');
        }
        };

        checkEvents();

        // Проверяем каждую секунду для быстрой реакции на изменения
        const interval = setInterval(checkEvents, 1000);

        // Слушаем изменения в sessionStorage для немедленной реакции (только в development)
        const handleStorageChange = (e) => {
            if (process.env.NODE_ENV === 'development' && (e.key === 'scheduledEvents' || !e.key)) {
                checkEvents();
            }
        };

        window.addEventListener('storage', handleStorageChange);

        return () => {
            clearInterval(interval);
            window.removeEventListener('storage', handleStorageChange);
        };
    }, [activeEvent]);

    useEffect(() => {
        if (!activeEvent) {
            setTimeLeft({});
            return;
        }

        const calculateTimeLeft = () => {
            const eventDate = new Date(`${activeEvent.date}T${activeEvent.time || '00:00'}`);
            const now = new Date();
            const difference = eventDate - now;

            if (difference <= 0) {
                // Событие истекло — сбрасываем и ищем следующее
                setActiveEvent(null);
                previousEventIdRef.current = null;
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
            const newTimeLeft = calculateTimeLeft();
            setTimeLeft(newTimeLeft);
        }, 1000);

        return () => clearInterval(timer);
    }, [activeEvent]);

    if (!activeEvent || timeLeft.expired) return null;

    return (
        <div className="global-event-notification" onClick={() => onEventClick && onEventClick()}>
            {/* Отображение ошибок */}
            {error && (
                <div className="error-message">
                    <span className="error-icon">⚠️</span>
                    <span>{error}</span>
                    <button className="error-close" onClick={clearError}>×</button>
                </div>
            )}

            <div className="notification-bubble-glass">
                <div className="bubble-content-glass">
                    <div className="bubble-label-glass">До Ближайшего События</div>
                    <div className="bubble-title-glass">{activeEvent.title}</div>
                    <div className="bubble-timer-glass">
                        {timeLeft.days > 0 && <span>{timeLeft.days}д </span>}
                        {timeLeft.hours > 0 && <span>{timeLeft.hours}ч </span>}
                        <span>{timeLeft.minutes}м {timeLeft.seconds}с</span>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default GlobalEventNotification;