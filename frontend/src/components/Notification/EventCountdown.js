import React, { useState, useEffect } from 'react';
import './EventCountdown.css';

const EventCountdown = ({ event, onClose, onClick }) => {
    const [timeLeft, setTimeLeft] = useState({});

    useEffect(() => {
        const calculateTimeLeft = () => {
            const eventDate = new Date(`${event.date}T${event.time || '00:00'}`);
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
    }, [event]);

    if (timeLeft.expired) {
        return null;
    }

    return (
        <div className="event-countdown" onClick={onClick}>
            <div className="countdown-content">
                <button className="countdown-close" onClick={(e) => { e.stopPropagation(); onClose(); }}>✕</button>
                <div className="countdown-icon">🎉</div>
                <div className="countdown-text">
                    <strong>Новое событие!</strong>
                    <span>{event.title}</span>
                </div>
                <div className="countdown-timer">
                    {timeLeft.days > 0 && <span>{timeLeft.days}д </span>}
                    {timeLeft.hours > 0 && <span>{timeLeft.hours}ч </span>}
                    <span>{timeLeft.minutes}м {timeLeft.seconds}с</span>
                </div>
                <div className="countdown-arrow">→</div>
            </div>
        </div>
    );
};

export default EventCountdown;