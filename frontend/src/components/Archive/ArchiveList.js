// components/Archive/ArchiveList.js
import React, { useState, useEffect } from 'react';
import { archiveApi } from '../../services/api';
import './ArchiveList.css';

const ArchiveList = () => {
    const [archives, setArchives] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [selectedArchive, setSelectedArchive] = useState(null);

    useEffect(() => {
        loadArchives();
    }, []);

    const loadArchives = async () => {
        try {
            setLoading(true);
            const response = await archiveApi.getAll();
            setArchives(response.data);
            setError(null);
        } catch (err) {
            console.error('Failed to load archives:', err);
            setError('Не удалось загрузить архив');
        } finally {
            setLoading(false);
        }
    };

    const formatDate = (dateString) => {
        const date = new Date(dateString);
        return date.toLocaleDateString('ru-RU', {
            day: 'numeric',
            month: 'long',
            year: 'numeric'
        });
    };

    const getDuration = (start, end) => {
        const duration = new Date(end) - new Date(start);
        const hours = Math.floor(duration / 3600000);
        const minutes = Math.floor((duration % 3600000) / 60000);
        return hours > 0 ? `${hours}ч ${minutes}м` : `${minutes}м`;
    };

    return (
        <div className="archive-list">
            {loading ? (
                <div className="loading">Загрузка...</div>
            ) : error ? (
                <div className="error-message">{error}</div>
            ) : archives.length === 0 ? (
                <div className="empty-state">
                    <p>📀 Архив пуст</p>
                    <p className="empty-hint">Здесь будут сохранены прошедшие эфиры</p>
                </div>
            ) : (
                <>
                    <div className="archive-stats">
                        <span>📀 Всего записей: {archives.length}</span>
                    </div>
                    <div className="archives-grid">
                        {archives.map(archive => (
                            <div key={archive.id} className="archive-card">
                                <div className="archive-card-thumbnail">
                                    {archive.thumbnail ? (
                                        <img src={archive.thumbnail} alt={archive.title} />
                                    ) : (
                                        <div className="archive-thumbnail-placeholder">
                                            🎬
                                        </div>
                                    )}
                                    <span className="archive-duration">
                                        {getDuration(archive.startTime, archive.endTime)}
                                    </span>
                                </div>
                                <div className="archive-card-info">
                                    <h3 className="archive-card-title">{archive.title}</h3>
                                    <p className="archive-card-date">
                                        📅 {formatDate(archive.startTime)}
                                    </p>
                                    <p className="archive-card-views">
                                        👁️ {archive.viewsCount || 0} просмотров
                                    </p>
                                    <button
                                        className="archive-card-play"
                                        onClick={() => setSelectedArchive(archive)}
                                    >
                                        ▶ Смотреть запись
                                    </button>
                                </div>
                            </div>
                        ))}
                    </div>
                </>
            )}

            {selectedArchive && (
                <div className="archive-modal" onClick={() => setSelectedArchive(null)}>
                    <div className="archive-modal-content" onClick={e => e.stopPropagation()}>
                        <button className="archive-modal-close" onClick={() => setSelectedArchive(null)}>
                            ✖
                        </button>
                        <h3>{selectedArchive.title}</h3>
                        <div className="archive-modal-video">
                            {selectedArchive.videoUrl ? (
                                <video src={selectedArchive.videoUrl} controls autoPlay />
                            ) : (
                                <div className="video-placeholder">
                                    🎥 Видеозапись недоступна
                                </div>
                            )}
                        </div>
                        <p className="archive-modal-description">{selectedArchive.description}</p>
                    </div>
                </div>
            )}
        </div>
    );
};

export default ArchiveList;