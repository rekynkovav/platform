// components/Archive/ArchiveList.js (Трансляции)
import React, { useState, useEffect } from 'react';
import './ArchiveList.css';

const ArchiveList = () => {
    const [broadcasts, setBroadcasts] = useState([]);
    const [showCreateForm, setShowCreateForm] = useState(false);
    const [streamType, setStreamType] = useState('audio'); // 'audio' или 'video'
    const [newBroadcast, setNewBroadcast] = useState({
        title: '',
        description: '',
        date: '',
        time: '',
        type: 'audio'
    });

    // Состояния для модального окна подтверждения удаления
    const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
    const [broadcastToDelete, setBroadcastToDelete] = useState(null);

    // Состояния для редактирования
    const [showEditModal, setShowEditModal] = useState(false);
    const [editingBroadcast, setEditingBroadcast] = useState(null);
    const [editStreamType, setEditStreamType] = useState('audio');

    const demoRole = localStorage.getItem('demoRole');
    const isAdmin = demoRole === 'admin_moderator' || demoRole === 'developer' || demoRole === 'super_admin';
    const canEdit = demoRole === 'admin_moderator' || demoRole === 'developer' || demoRole === 'super_admin';

    // Отладка
    console.log('[ArchiveList] demoRole:', demoRole);
    console.log('[ArchiveList] isAdmin:', isAdmin);
    console.log('[ArchiveList] canEdit:', canEdit);

    useEffect(() => {
        loadBroadcasts();
    }, []);

    // Проверка, прошла ли трансляция
    const isBroadcastPast = (broadcast) => {
        const broadcastDateTime = new Date(`${broadcast.date}T${broadcast.time}`);
        const now = new Date();
        return broadcastDateTime < now;
    };

    // Перенос прошедших трансляций в Dashboard
    const moveCompletedBroadcastsToDashboard = (broadcastsList) => {
        const dashboardData = JSON.parse(localStorage.getItem('dashboardMenuData') || '{}');
        const currentBroadcasts = dashboardData['broadcasts'] || [];

        broadcastsList.forEach(broadcast => {
            if (isBroadcastPast(broadcast) && broadcast.status === 'scheduled') {
                // Обновляем статус трансляции
                broadcast.status = 'completed';

                // Проверяем, не добавлена ли уже эта трансляция в Dashboard
                const alreadyExists = currentBroadcasts.some(item => item.broadcastId === broadcast.id);

                if (!alreadyExists) {
                    // Добавляем в Dashboard
                    const dashboardItem = {
                        id: Date.now() + Math.random(),
                        broadcastId: broadcast.id,
                        title: broadcast.title,
                        description: broadcast.description,
                        mediaUrl: broadcast.mediaUrl || '',
                        mediaType: broadcast.type,
                        createdAt: new Date().toISOString()
                    };
                    currentBroadcasts.push(dashboardItem);
                }
            }
        });

        dashboardData['broadcasts'] = currentBroadcasts;
        localStorage.setItem('dashboardMenuData', JSON.stringify(dashboardData));
    };

    const loadBroadcasts = () => {
        const saved = localStorage.getItem('broadcasts');
        if (saved) {
            const broadcastsList = JSON.parse(saved);

            // Проверяем и переносим прошедшие трансляции
            moveCompletedBroadcastsToDashboard(broadcastsList);

            // Обновляем статусы
            const updatedBroadcasts = broadcastsList.map(b => ({
                ...b,
                status: isBroadcastPast(b) ? 'completed' : b.status
            }));

            setBroadcasts(updatedBroadcasts);
            localStorage.setItem('broadcasts', JSON.stringify(updatedBroadcasts));
        } else {
            const initial = [
                {
                    id: 1,
                    title: 'Прямой эфир - Основы питания',
                    description: 'Обсуждение принципов здорового питания',
                    date: '2026-04-10',
                    time: '18:00',
                    type: 'video',
                    status: 'scheduled'
                }
            ];
            setBroadcasts(initial);
            localStorage.setItem('broadcasts', JSON.stringify(initial));
        }
    };

    const handleCreateBroadcast = () => {
        if (!isAdmin) {
            alert('Только администратор может создавать трансляции');
            return;
        }

        if (!newBroadcast.title || !newBroadcast.date || !newBroadcast.time) {
            alert('Заполните название, дату и время');
            return;
        }

        const broadcast = {
            id: Date.now(),
            ...newBroadcast,
            type: streamType,
            status: 'scheduled',
            createdAt: new Date().toISOString()
        };

        const updated = [...broadcasts, broadcast];
        setBroadcasts(updated);
        localStorage.setItem('broadcasts', JSON.stringify(updated));

        // Добавляем в события
        const events = JSON.parse(localStorage.getItem('scheduledEvents') || '[]');
        const event = {
            id: Date.now() + 1,
            title: `📡 ${broadcast.title}`,
            date: broadcast.date,
            time: broadcast.time,
            description: `Прямая ${broadcast.type === 'audio' ? 'аудио' : 'видео'} трансляция: ${broadcast.description}`
        };
        events.push(event);
        localStorage.setItem('scheduledEvents', JSON.stringify(events));

        setNewBroadcast({ title: '', description: '', date: '', time: '', type: 'audio' });
        setShowCreateForm(false);
        alert('Трансляция создана и добавлена в События!');
    };

    const formatDate = (dateString) => {
        const date = new Date(dateString);
        return date.toLocaleDateString('ru-RU', {
            day: 'numeric',
            month: 'long',
            year: 'numeric'
        });
    };

    const confirmDeleteBroadcast = (broadcast) => {
        if (!canEdit) {
            alert('У вас нет прав на удаление');
            return;
        }
        setBroadcastToDelete(broadcast);
        setShowDeleteConfirm(true);
    };

    const handleEditBroadcast = (broadcast) => {
        if (!canEdit) {
            alert('У вас нет прав на редактирование');
            return;
        }
        setEditingBroadcast(broadcast);
        setNewBroadcast({
            title: broadcast.title,
            description: broadcast.description,
            date: broadcast.date,
            time: broadcast.time,
            type: broadcast.type
        });
        setEditStreamType(broadcast.type);
        setShowEditModal(true);
    };

    const handleSaveEdit = () => {
        if (!newBroadcast.title || !newBroadcast.date || !newBroadcast.time) {
            alert('Заполните название, дату и время');
            return;
        }

        const updated = broadcasts.map(b =>
            b.id === editingBroadcast.id
                ? { ...b, ...newBroadcast, type: editStreamType, edited: true }
                : b
        );

        setBroadcasts(updated);
        localStorage.setItem('broadcasts', JSON.stringify(updated));

        setNewBroadcast({ title: '', description: '', date: '', time: '', type: 'audio' });
        setEditingBroadcast(null);
        setShowEditModal(false);
    };

    const handleCancelEdit = () => {
        setNewBroadcast({ title: '', description: '', date: '', time: '', type: 'audio' });
        setEditingBroadcast(null);
        setShowEditModal(false);
    };

    const executeDeleteBroadcast = () => {
        if (!broadcastToDelete) return;

        const updated = broadcasts.filter(b => b.id !== broadcastToDelete.id);
        setBroadcasts(updated);
        localStorage.setItem('broadcasts', JSON.stringify(updated));

        setShowDeleteConfirm(false);
        setBroadcastToDelete(null);
    };

    const cancelDeleteBroadcast = () => {
        setShowDeleteConfirm(false);
        setBroadcastToDelete(null);
    };

    return (
        <div className="archive-list broadcasts-page">
            <div className="broadcasts-header">
                <h2>📡 Трансляции</h2>
                <p>Прямые аудио и видео эфиры</p>
            </div>

            {isAdmin && (
                <div className="create-broadcast-section">
                    <button
                        className="create-broadcast-btn"
                        onClick={() => setShowCreateForm(true)}
                    >
                        + Создать трансляцию
                    </button>
                </div>
            )}

            {showCreateForm && (
                <div className="modal-overlay" onClick={() => setShowCreateForm(false)}>
                    <div className="modal-content create-broadcast-modal" onClick={(e) => e.stopPropagation()}>
                        <h3>Новая трансляция</h3>

                        <div className="stream-type-toggle">
                            <span className="toggle-label">Тип трансляции:</span>
                            <div className="toggle-switch">
                                <button
                                    className={`toggle-option ${streamType === 'audio' ? 'active' : ''}`}
                                    onClick={() => setStreamType('audio')}
                                >
                                    🎙️ Аудио
                                </button>
                                <button
                                    className={`toggle-option ${streamType === 'video' ? 'active' : ''}`}
                                    onClick={() => setStreamType('video')}
                                >
                                    🎥 Видео
                                </button>
                            </div>
                        </div>

                        <input
                            type="text"
                            placeholder="Название трансляции"
                            value={newBroadcast.title}
                            onChange={(e) => setNewBroadcast({...newBroadcast, title: e.target.value})}
                        />

                        <textarea
                            placeholder="Описание трансляции"
                            value={newBroadcast.description}
                            onChange={(e) => setNewBroadcast({...newBroadcast, description: e.target.value})}
                            rows="3"
                        />

                        <div className="date-time-group">
                            <div className="date-field">
                                <label>📅 Дата</label>
                                <input
                                    type="date"
                                    value={newBroadcast.date}
                                    onChange={(e) => setNewBroadcast({...newBroadcast, date: e.target.value})}
                                />
                            </div>
                            <div className="time-field">
                                <label>⏰ Время</label>
                                <input
                                    type="time"
                                    value={newBroadcast.time}
                                    onChange={(e) => setNewBroadcast({...newBroadcast, time: e.target.value})}
                                />
                            </div>
                        </div>

                        <div className="modal-buttons-vertical">
                            <button className="modal-btn-create" onClick={handleCreateBroadcast}>
                                Создать трансляцию
                            </button>
                            <button className="modal-btn-cancel" onClick={() => setShowCreateForm(false)}>
                                Отмена
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {broadcasts.length === 0 ? (
                <div className="empty-state">
                    <p>📡 Нет запланированных трансляций</p>
                    {isAdmin && <p className="empty-hint">Создайте первую трансляцию</p>}
                </div>
            ) : (
                <div className="broadcasts-grid">
                    {broadcasts.map(broadcast => (
                        <div key={broadcast.id} className="broadcast-card">
                            <div className="broadcast-type-badge">
                                {broadcast.type === 'audio' ? '🎙️ Аудио' : '🎥 Видео'}
                            </div>
                            {canEdit && (
                                <div className="broadcast-actions">
                                    <button
                                        className="edit-broadcast-btn"
                                        onClick={() => handleEditBroadcast(broadcast)}
                                        title="Редактировать трансляцию"
                                    >
                                        ✏️
                                    </button>
                                    <button
                                        className="delete-broadcast-btn"
                                        onClick={() => confirmDeleteBroadcast(broadcast)}
                                        title="Удалить трансляцию"
                                    >
                                        🗑️
                                    </button>
                                </div>
                            )}
                            <h3 className="broadcast-title">
                                {broadcast.title}
                                {broadcast.edited && <span className="edited-badge-broadcast"> (изменено)</span>}
                            </h3>
                            <p className="broadcast-description">{broadcast.description}</p>
                            <div className="broadcast-datetime">
                                <span>📅 {formatDate(broadcast.date)}</span>
                                <span>⏰ {broadcast.time}</span>
                            </div>
                            <div className="broadcast-status">
                                {broadcast.status === 'scheduled' && '⏳ Запланирована'}
                                {broadcast.status === 'live' && '🔴 В эфире'}
                                {broadcast.status === 'ended' && '✅ Завершена'}
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {/* Модальное окно редактирования */}
            {showEditModal && editingBroadcast && (
                <div className="modal-overlay" onClick={handleCancelEdit}>
                    <div className="modal-content create-broadcast-modal" onClick={(e) => e.stopPropagation()}>
                        <div className="modal-header">
                            <span className="modal-icon">✏️</span>
                            <h3>Редактировать трансляцию</h3>
                        </div>

                        <div className="stream-type-toggle">
                            <span className="toggle-label">Тип трансляции:</span>
                            <div className="toggle-switch">
                                <button
                                    className={`toggle-option ${editStreamType === 'audio' ? 'active' : ''}`}
                                    onClick={() => setEditStreamType('audio')}
                                >
                                    🎙️ Аудио
                                </button>
                                <button
                                    className={`toggle-option ${editStreamType === 'video' ? 'active' : ''}`}
                                    onClick={() => setEditStreamType('video')}
                                >
                                    🎥 Видео
                                </button>
                            </div>
                        </div>

                        <input
                            type="text"
                            placeholder="Название трансляции"
                            value={newBroadcast.title}
                            onChange={(e) => setNewBroadcast({...newBroadcast, title: e.target.value})}
                        />

                        <textarea
                            placeholder="Описание трансляции"
                            value={newBroadcast.description}
                            onChange={(e) => setNewBroadcast({...newBroadcast, description: e.target.value})}
                            rows="3"
                        />

                        <div className="date-time-group">
                            <div className="date-field">
                                <label>📅 Дата</label>
                                <input
                                    type="date"
                                    value={newBroadcast.date}
                                    onChange={(e) => setNewBroadcast({...newBroadcast, date: e.target.value})}
                                />
                            </div>
                            <div className="time-field">
                                <label>⏰ Время</label>
                                <input
                                    type="time"
                                    value={newBroadcast.time}
                                    onChange={(e) => setNewBroadcast({...newBroadcast, time: e.target.value})}
                                />
                            </div>
                        </div>

                        <div className="modal-footer">
                            <button className="modal-btn cancel" onClick={handleCancelEdit}>
                                Отмена
                            </button>
                            <button className="modal-btn confirm" onClick={handleSaveEdit}>
                                Сохранить
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Модальное окно подтверждения удаления */}
            {showDeleteConfirm && broadcastToDelete && (
                <div className="modal-overlay" onClick={cancelDeleteBroadcast}>
                    <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                        <div className="modal-header">
                            <span className="modal-icon">🗑️</span>
                            <h3>Подтверждение удаления</h3>
                        </div>
                        <div className="modal-body">
                            <p>Вы действительно хотите удалить эту трансляцию?</p>
                            <div className="broadcast-preview">
                                <strong>{broadcastToDelete.title}</strong>
                                <small>{formatDate(broadcastToDelete.date)} в {broadcastToDelete.time}</small>
                            </div>
                            <p className="warning-text">Это действие невозможно отменить!</p>
                        </div>
                        <div className="modal-footer">
                            <button className="modal-btn cancel" onClick={cancelDeleteBroadcast}>
                                ❌ Отмена
                            </button>
                            <button className="modal-btn confirm" onClick={executeDeleteBroadcast}>
                                ✅ Да, удалить
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default ArchiveList;
