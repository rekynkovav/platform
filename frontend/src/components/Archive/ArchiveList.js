import React, { useState, useEffect } from 'react';
import { archiveApi } from '../../services/api';
import { mockArchiveApi } from '../../services/mockApi';
import './ArchiveList.css';

const ArchiveList = () => {
    const [archives, setArchives] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [demoRole] = useState(() => localStorage.getItem('demoRole') || 'USER_0');

    const useMock = demoRole === 'USER_0' || !demoRole;
    const api = useMock ? mockArchiveApi : archiveApi;

    const loadArchives = async () => {
        try {
            setLoading(true);
            setError(null);
            const response = await api.getAll();
            setArchives(Array.isArray(response.data) ? response.data : []);
        } catch (err) {
            console.error('Failed to load archives', err);
            setError('Не удалось загрузить архив');
            setArchives([]);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadArchives();
    }, [demoRole]);

    const isAdmin = ['MODERATOR_ADMIN', 'SUPER_ADMIN', 'VYACHESLAV', 'DEVELOPER'].includes(demoRole);

    if (loading) return <div className="loading">Загрузка архива...</div>;
    if (error) return <div className="error-message">{error}</div>;

    return (
        <div className="archive-list">
            <div className="broadcasts-header">
                <h2>📼 Архив трансляций</h2>
                <p>Все завершённые события и записи</p>
            </div>

            {isAdmin && (
                <div className="create-broadcast-section">
                    <button className="create-broadcast-btn">+ Добавить в архив</button>
                </div>
            )}

            <div className="broadcasts-grid">
                {archives.length === 0 ? (
                    <p className="no-events">Архив пока пуст</p>
                ) : (
                    archives.map(archive => (
                        <div key={archive.id} className="broadcast-card">
                            <h3 className="broadcast-title">{archive.title}</h3>
                            {archive.description && (
                                <p className="broadcast-description">{archive.description}</p>
                            )}
                            <div className="broadcast-datetime">
                                {new Date(archive.startTime || archive.date).toLocaleString('ru-RU')}
                            </div>
                            <div className="broadcast-status">завершено</div>
                        </div>
                    ))
                )}
            </div>
        </div>
    );
};

export default ArchiveList;