import React, { useState, useEffect } from 'react';
import './EventCountdown.css';

const EventCountdown = ({ event, onClose }) => {
    const [timeLeft, setTimeLeft] = useState({});

    useEffect(() => {
        const calculate = () => {
            const eventDate = new Date(event.timestamp || event.createdAt);
            const now = new Date();
            const diff = eventDate - now;
            if (diff <= 0) {
                onClose();
                return { expired: true };
            }
            const days = Math.floor(diff / (1000 * 60 * 60 * 24));
            const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
            const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
            const seconds = Math.floor((diff % (1000 * 60)) / 1000);
            return { days, hours, minutes, seconds, expired: false };
        };

        const timer = setInterval(() => setTimeLeft(calculate()), 1000);
        setTimeLeft(calculate());

        return () => clearInterval(timer);
    }, [event, onClose]);

    if (timeLeft.expired) return null;

    return (
        <div className="event-countdown" onClick={() => {}}>
            <div className="countdown-content">
                <button className="countdown-close" onClick={onClose}>✕</button>
                <div className="countdown-icon">🎉</div>
                <div className="countdown-text">
                    <strong>Скоро событие!</strong>
                    <span>{event.title}</span>
                </div>
                <div className="countdown-timer">
                    {timeLeft.days > 0 && <span>{timeLeft.days}д </span>}
                    {timeLeft.hours > 0 && <span>{timeLeft.hours}ч </span>}
                    <span>{timeLeft.minutes}м {timeLeft.seconds}с</span>
                </div>
            </div>
        </div>
    );
};

export default EventCountdown;