// components/Dashboard/Timer.js
import React, { useState, useEffect } from 'react';
import './Timer.css';

const Timer = ({ targetTime }) => {
    const [timeLeft, setTimeLeft] = useState({
        days: 0,
        hours: 0,
        minutes: 0,
        seconds: 0,
        isExpired: false
    });

    useEffect(() => {
        const calculateTimeLeft = () => {
            const now = new Date().getTime();
            const target = new Date(targetTime).getTime();
            const difference = target - now;

            if (difference <= 0) {
                setTimeLeft({ isExpired: true });
                return;
            }

            setTimeLeft({
                days: Math.floor(difference / (1000 * 60 * 60 * 24)),
                hours: Math.floor((difference % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60)),
                minutes: Math.floor((difference % (1000 * 60 * 60)) / (1000 * 60)),
                seconds: Math.floor((difference % (1000 * 60)) / 1000),
                isExpired: false
            });
        };

        calculateTimeLeft();
        const interval = setInterval(calculateTimeLeft, 1000);

        return () => clearInterval(interval);
    }, [targetTime]);

    if (timeLeft.isExpired) {
        return (
            <div className="timer expired">
                <div className="timer-value">СОБЫТИЕ НАЧАЛОСЬ!</div>
                <div className="timer-label">Присоединяйтесь сейчас</div>
            </div>
        );
    }

    return (
        <div className="timer">
            <div className="timer-value">
                {timeLeft.days > 0 && (
                    <span className="timer-unit">
                        {String(timeLeft.days).padStart(2, '0')}
                        <span className="timer-unit-label">д</span>
                    </span>
                )}
                <span className="timer-unit">
                    {String(timeLeft.hours).padStart(2, '0')}
                    <span className="timer-unit-label">ч</span>
                </span>
                <span className="timer-unit">
                    {String(timeLeft.minutes).padStart(2, '0')}
                    <span className="timer-unit-label">м</span>
                </span>
                <span className="timer-unit">
                    {String(timeLeft.seconds).padStart(2, '0')}
                    <span className="timer-unit-label">с</span>
                </span>
            </div>
            <div className="timer-label">ДО НАЧАЛА СОБЫТИЯ</div>
        </div>
    );
};

export default Timer;