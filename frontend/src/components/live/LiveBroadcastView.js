import React, { useState, useEffect, useCallback } from 'react';
import { broadcastApi } from '../../services/api';
import useWebSocket from '../../hooks/useWebSocket';
import { getUserRoles, getUsername } from '../../keycloak';
import './LiveBroadcastView.css';

const LiveBroadcastView = () => {
    const [broadcasts, setBroadcasts] = useState([]);
    const [selectedBroadcast, setSelectedBroadcast] = useState(null);
    const [loading, setLoading] = useState(true);
    const [isStarting, setIsStarting] = useState(false);
    const [error, setError] = useState(null);
    const userRoles = getUserRoles();
    const username = getUsername();
    const isAdmin = userRoles.some(r => ['SUPER_ADMIN', 'MODERATOR_ADMIN', 'VYACHESLAV'].includes(r));

    const { isConnected, send } = useWebSocket('broadcast-stats', (data) => {
        if (data.type === 'viewer_count') {
            setBroadcasts(prev => prev.map(b =>
                b.id === data.broadcastId ? { ...b, viewerCount: data.count } : b
            ));
        }
    });

    const clearError = () => {
        setError(null);
    };

    // Загрузка активных трансляций
    const loadBroadcasts = useCallback(async () => {
        try {
            setLoading(true);
            const data = await broadcastApi.getActive();
            setBroadcasts(data || []);
        } catch (error) {
            console.error('Failed to load broadcasts:', error);
            setError('Не удалось загрузить трансляции. Проверьте подключение к серверу.');
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        loadBroadcasts();
        const interval = setInterval(loadBroadcasts, 10000);
        return () => clearInterval(interval);
    }, [loadBroadcasts]);

    // Старт трансляции
    const startBroadcast = async () => {
        if (!isAdmin) {
            alert('Only admin can start broadcasts');
            return;
        }

        setIsStarting(true);
        try {
            const title = prompt('Enter broadcast title:');
            if (!title) return;

            const newBroadcast = await broadcastApi.start({
                title,
                description: prompt('Enter description:') || '',
                streamType: 'video'
            });

            setBroadcasts(prev => [...prev, newBroadcast]);
            send({ type: 'broadcast_started', broadcastId: newBroadcast.id });
        } catch (error) {
            console.error('Failed to start broadcast:', error);
            setError('Не удалось начать трансляцию. Проверьте подключение к серверу.');
        } finally {
            setIsStarting(false);
        }
    };

    // Завершение трансляции
    const endBroadcast = async (id) => {
        if (!isAdmin) return;

        if (confirm('End this broadcast?')) {
            try {
                await broadcastApi.end(id);
                setBroadcasts(prev => prev.filter(b => b.id !== id));
                send({ type: 'broadcast_ended', broadcastId: id });
            } catch (error) {
                console.error('Failed to end broadcast:', error);
                setError('Не удалось завершить трансляцию. Проверьте подключение к серверу.');
            }
        }
    };

    return (
        <div className="live-broadcast-view">
            <div className="broadcast-header">
                <h2>Live Broadcasts</h2>
                {isAdmin && (
                    <button onClick={startBroadcast} disabled={isStarting}>
                        {isStarting ? 'Starting...' : '🎥 Start Broadcast'}
                    </button>
                )}
            </div>

            {/* Отображение ошибок */}
            {error && (
                <div className="error-message">
                    <span className="error-icon">⚠️</span>
                    <span>{error}</span>
                    <button className="error-close" onClick={clearError}>×</button>
                </div>
            )}

            {loading ? (
                <div className="loading">Loading...</div>
            ) : broadcasts.length === 0 ? (
                <div className="no-broadcasts">No active broadcasts</div>
            ) : (
                <div className="broadcasts-grid">
                    {broadcasts.map(b => (
                        <div key={b.id} className="broadcast-card">
                            <div className="broadcast-preview">
                                {b.hlsPlaylistUrl ? (
                                    <video src={b.hlsPlaylistUrl} controls />
                                ) : (
                                    <div className="preview-placeholder">📺 Live</div>
                                )}
                            </div>
                            <div className="broadcast-info">
                                <h3>{b.title}</h3>
                                <p>{b.description}</p>
                                <div className="broadcast-meta">
                                    <span>👥 {b.viewerCount || 0} viewers</span>
                                    <span>🎬 Started: {new Date(b.startedAt).toLocaleTimeString()}</span>
                                </div>
                                {isAdmin && (
                                    <button onClick={() => endBroadcast(b.id)} className="end-btn">
                                        End Broadcast
                                    </button>
                                )}
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

export default LiveBroadcastView;