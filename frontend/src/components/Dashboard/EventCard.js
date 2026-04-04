// components/Dashboard/EventCard.js
import React from 'react';
import Timer from './Timer';
import './EventCard.css';

const EventCard = ({ event }) => {
    const { title, description, startTime, links = [], coverImage } = event;

    return (
        <div className="event-card">
            {coverImage && (
                <div className="event-cover">
                    <img src={coverImage} alt={title} />
                </div>
            )}

            <div className="event-timer-wrapper">
                <Timer targetTime={startTime} />
            </div>

            <div className="event-info">
                <h3 className="event-title">{title}</h3>
                <p className="event-description">{description}</p>

                {links.length > 0 && (
                    <div className="event-links">
                        <span className="links-label">🔗 Ссылки:</span>
                        <div className="links-list">
                            {links.map((link, idx) => (
                                <a
                                    key={idx}
                                    href={link.url}
                                    target="_blank"
                                    rel="noopener noreferrer"
                                    className="event-link"
                                >
                                    {link.name || 'Перейти'}
                                </a>
                            ))}
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
};

export default EventCard;