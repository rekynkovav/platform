import React, { useState, useEffect } from 'react';
import './GlobalEventNotification.css';

const GlobalEventNotification = ({ onEventClick }) => {
    const [activeEvent, setActiveEvent] = useState(null);
    const [timeLeft, setTimeLeft] = useState({});

    useEffect(() => {
        const checkEvents = () => {
            const savedEvents = localStorage.getItem('scheduledEvents');
            if (savedEvents) {
                const events = JSON.parse(savedEvents);
                const now = new Date();

                const upcomingEvents = events.filter(event => {
                    const eventDate = new Date(`${event.date}T${event.time || '00:00'}`);
                    return eventDate > now;
                }).sort((a, b) => new Date(`${a.date}T${a.time}`) - new Date(`${b.date}T${b.time}`));

                if (upcomingEvents.length > 0) {
                    const nearestEvent = upcomingEvents[0];
                    const eventDate = new Date(`${nearestEvent.date}T${nearestEvent.time || '00:00'}`);
                    const daysDiff = (eventDate - now) / (1000 * 60 * 60 * 24);

                    if (daysDiff <= 7 && daysDiff > 0) {
                        setActiveEvent(nearestEvent);
                    } else if (daysDiff <= 0) {
                        setActiveEvent(null);
                    }
                } else {
                    setActiveEvent(null);
                }
            }
        };

        checkEvents();
        const interval = setInterval(checkEvents, 5000);
        window.addEventListener('storage', checkEvents);

        return () => {
            clearInterval(interval);
            window.removeEventListener('storage', checkEvents);
        };
    }, []);

    useEffect(() => {
        if (!activeEvent) return;

        const calculateTimeLeft = () => {
            const eventDate = new Date(`${activeEvent.date}T${activeEvent.time || '00:00'}`);
            const now = new Date();
            const difference = eventDate - now;

            if (difference <= 0) {
                setActiveEvent(null);
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
            if (newTimeLeft.expired) {
                setActiveEvent(null);
                clearInterval(timer);
            }
        }, 1000);

        return () => clearInterval(timer);
    }, [activeEvent]);

    if (!activeEvent || timeLeft.expired) return null;

    return (
        <div className="global-event-notification" onClick={() => onEventClick && onEventClick()}>
            <div className="notification-bubble-glass">
                <div className="bubble-content-glass">
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