import React, { useEffect, useState, useMemo, useCallback, useRef } from 'react';
import ContentModal from '../Common/ContentModal';
import './Dashboard.css';

const Dashboard = () => {
    const [hoveredMenu, setHoveredMenu] = useState(null);
    const [hoveredSubmenu, setHoveredSubmenu] = useState(null);
    const [hideTimeout, setHideTimeout] = useState(null);
    const [isOverCard, setIsOverCard] = useState(false);
    const [isOverDropdown, setIsOverDropdown] = useState(false);
    const [menuPosition, setMenuPosition] = useState({ top: 0, left: 0 });
    const [submenuPosition, setSubmenuPosition] = useState({ top: 0, left: 0 });
    const [showAddModal, setShowAddModal] = useState(false);
    const [selectedMenuId, setSelectedMenuId] = useState(null);
    const [newItemTitle, setNewItemTitle] = useState('');
    const [newItemDescription, setNewItemDescription] = useState('');
    const [newItemMediaUrl, setNewItemMediaUrl] = useState('');
    const [newItemMediaType, setNewItemMediaType] = useState('');
    const [isRecording, setIsRecording] = useState(false);
    const [mediaRecorder, setMediaRecorder] = useState(null);
    const [recordingTime, setRecordingTime] = useState(0);
    const recordingTimerRef = useRef(null);
    const [menuData, setMenuData] = useState({});

    // Состояния для модального окна подтверждения удаления
    const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
    const [itemToDelete, setItemToDelete] = useState(null);

    const demoRole = localStorage.getItem('demoRole');
    const isAdmin = useMemo(() =>
        demoRole === 'admin' || demoRole === 'admin-moderator' ||
        demoRole === 'admin-consultant' || demoRole === 'admin-nutrition' ||
        demoRole === 'admin-sport' || demoRole === 'developer' ||
        demoRole === 'super-admin'
    , [demoRole]);

    useEffect(() => {
        const savedData = localStorage.getItem('dashboardMenuData');
        if (savedData) {
            setMenuData(JSON.parse(savedData));
        }
    }, []);

    const saveMenuData = useCallback((data) => {
        setMenuData(data);
        localStorage.setItem('dashboardMenuData', JSON.stringify(data));
    }, []);

    const navigationMenus = useMemo(() => [
        {
            id: 'broadcasts',
            title: 'Эфиры',
            items: [
                {
                    id: 'with-guests',
                    title: 'С Гостями',
                    description: 'Эфиры с приглашенными гостями',
                    hasSubmenu: true,
                    submenuItems: menuData['broadcasts-guests'] || []
                },
                ...(menuData['broadcasts'] || [])
            ]
        },
        { id: 'exercises', title: 'Упражнения', items: menuData['exercises'] || [] },
        { id: 'recommendations', title: 'Рекомендации', items: menuData['recommendations'] || [] },
        { id: 'hardening', title: 'Закаливание', items: menuData['hardening'] || [] },
        { id: 'about', title: 'О нас', items: menuData['about'] || [] },
        { id: 'newbies', title: 'Для новичков', items: menuData['newbies'] || [] },
        { id: 'heart-voice', title: 'Важные материалы\nголос сердца', items: menuData['heart-voice'] || [] },
        { id: 'reflection', title: 'Для размышления', items: menuData['reflection'] || [] },
        { id: 'interviews', title: 'Интервью с\nИнтересными личностями', items: menuData['interviews'] || [] },
        { id: 'participants', title: 'От участников проекта', items: menuData['participants'] || [] },
        { id: 'motivation', title: 'Мотивация', items: menuData['motivation'] || [] },
        { id: 'reviews', title: 'Отзывы', items: menuData['reviews'] || [] },
        { id: 'music', title: 'Музыка', items: menuData['music'] || [] },
        { id: 'movies', title: 'Фильмы', items: menuData['movies'] || [] },
        { id: 'literature', title: 'Литература', items: menuData['literature'] || [] },
        { id: 'about-timoshenko', title: 'О Тимошенко', items: menuData['about-timoshenko'] || [] },
        { id: 'challenge', title: 'Годовой челлендж\nФизических нагрузок', items: menuData['challenge'] || [] }
    ], [menuData]);

    const currentMenu = useMemo(() =>
        navigationMenus.find(m => m.id === hoveredMenu)
    , [navigationMenus, hoveredMenu]);

    const handleMenuEnter = useCallback((menuId, event) => {
        if (hideTimeout) {
            clearTimeout(hideTimeout);
            setHideTimeout(null);
        }
        const rect = event.currentTarget.getBoundingClientRect();
        setMenuPosition({
            top: rect.bottom + window.scrollY,
            left: rect.left + window.scrollX
        });
        setHoveredMenu(menuId);
        setHoveredSubmenu(null);
        setIsOverCard(true);
    }, [hideTimeout]);

    const handleCardLeave = useCallback(() => {
        setIsOverCard(false);
    }, []);

    const handleDropdownEnter = useCallback(() => {
        if (hideTimeout) {
            clearTimeout(hideTimeout);
            setHideTimeout(null);
        }
        setIsOverDropdown(true);
    }, [hideTimeout]);

    const handleDropdownLeave = useCallback(() => {
        setIsOverDropdown(false);
    }, []);

    // Эффект для управления видимостью меню
    useEffect(() => {
        if (!isOverCard && !isOverDropdown) {
            const timeout = setTimeout(() => {
                setHoveredMenu(null);
                setHoveredSubmenu(null);
            }, 100);
            setHideTimeout(timeout);
            return () => clearTimeout(timeout);
        }
    }, [isOverCard, isOverDropdown]);

    const handleSubmenuEnter = useCallback((submenuId, event) => {
        if (hideTimeout) clearTimeout(hideTimeout);
        const rect = event.currentTarget.getBoundingClientRect();
        setSubmenuPosition({
            top: rect.top + window.scrollY,
            left: rect.right + window.scrollX + 8
        });
        setHoveredSubmenu(submenuId);
    }, [hideTimeout]);

    const handleAddContent = useCallback((menuId, submenuId = null) => {
        setSelectedMenuId(submenuId || menuId);
        setShowAddModal(true);
        setNewItemTitle('');
        setNewItemDescription('');
        setNewItemMediaUrl('');
        setNewItemMediaType('');
        setIsRecording(false);
        setRecordingTime(0);
        // Мгновенно закрываем всплывающее меню
        setHoveredMenu(null);
        setHoveredSubmenu(null);
        if (hideTimeout) {
            clearTimeout(hideTimeout);
            setHideTimeout(null);
        }
    }, [hideTimeout]);

    const handleSaveContent = useCallback(() => {
        if (!newItemTitle.trim()) return;

        const newItem = {
            id: Date.now(),
            title: newItemTitle,
            description: newItemDescription,
            mediaUrl: newItemMediaUrl,
            mediaType: newItemMediaType
        };

        const updatedData = { ...menuData };
        if (!updatedData[selectedMenuId]) {
            updatedData[selectedMenuId] = [];
        }
        updatedData[selectedMenuId].push(newItem);
        saveMenuData(updatedData);

        setNewItemTitle('');
        setNewItemDescription('');
        setNewItemMediaUrl('');
        setNewItemMediaType('');
        setIsRecording(false);
        setRecordingTime(0);
        setShowAddModal(false);
        setSelectedMenuId(null);
    }, [newItemTitle, newItemDescription, newItemMediaUrl, newItemMediaType, menuData, selectedMenuId, saveMenuData]);

    const handleFileUpload = useCallback((e, type) => {
        const file = e.target.files[0];
        if (file) {
            const url = URL.createObjectURL(file);
            setNewItemMediaUrl(url);
            setNewItemMediaType(type);
        }
    }, []);

    const startRecording = useCallback(async () => {
        try {
            const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
            const recorder = new MediaRecorder(stream);
            const chunks = [];

            recorder.ondataavailable = (e) => {
                if (e.data.size > 0) {
                    chunks.push(e.data);
                }
            };

            recorder.onstop = () => {
                const audioBlob = new Blob(chunks, { type: 'audio/webm' });
                const audioUrl = URL.createObjectURL(audioBlob);
                setNewItemMediaUrl(audioUrl);
                setNewItemMediaType('audio');
                stream.getTracks().forEach(track => track.stop());
            };

            recorder.start();
            setMediaRecorder(recorder);
            setIsRecording(true);

            setRecordingTime(0);
            recordingTimerRef.current = setInterval(() => {
                setRecordingTime(prev => prev + 1);
            }, 1000);
        } catch (err) {
            console.error('Ошибка доступа к микрофону:', err);
            alert('Не удалось получить доступ к микрофону');
        }
    }, []);

    const stopRecording = useCallback(() => {
        if (mediaRecorder && isRecording) {
            mediaRecorder.stop();
            setIsRecording(false);
            if (recordingTimerRef.current) {
                clearInterval(recordingTimerRef.current);
            }
        }
    }, [mediaRecorder, isRecording]);

    const formatRecordingTime = (seconds) => {
        const mins = Math.floor(seconds / 60);
        const secs = seconds % 60;
        return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    };

    const handleDeleteItem = useCallback((menuId, itemId, itemTitle) => {
        setItemToDelete({ menuId, itemId, itemTitle });
        setShowDeleteConfirm(true);
        // Мгновенно закрываем всплывающее меню
        setHoveredMenu(null);
        setHoveredSubmenu(null);
        if (hideTimeout) {
            clearTimeout(hideTimeout);
            setHideTimeout(null);
        }
    }, [hideTimeout]);

    const executeDeleteItem = useCallback(() => {
        if (!itemToDelete) return;

        const updatedData = { ...menuData };
        if (updatedData[itemToDelete.menuId]) {
            updatedData[itemToDelete.menuId] = updatedData[itemToDelete.menuId].filter(
                item => item.id !== itemToDelete.itemId
            );
            saveMenuData(updatedData);
        }

        setShowDeleteConfirm(false);
        setItemToDelete(null);
    }, [itemToDelete, menuData, saveMenuData]);

    const cancelDeleteItem = useCallback(() => {
        setShowDeleteConfirm(false);
        setItemToDelete(null);
    }, []);

    return (
        <div className="dashboard">
            <div className="navigation-menus-section">
                <div className="navigation-menus-grid">
                    {navigationMenus.map((menu) => (
                        <div
                            key={menu.id}
                            className="navigation-menu-card"
                            onMouseEnter={(e) => handleMenuEnter(menu.id, e)}
                            onMouseLeave={handleCardLeave}
                        >
                            <div className="menu-card-header">
                                <span className="menu-card-title">{menu.title}</span>
                            </div>
                        </div>
                    ))}
                </div>

                {hoveredMenu && currentMenu && (
                    <div
                        className="menu-dropdown-positioned"
                        style={{ top: menuPosition.top, left: menuPosition.left }}
                        onMouseEnter={handleDropdownEnter}
                        onMouseLeave={handleDropdownLeave}
                    >
                        {isAdmin && (
                            <div className="dropdown-add-section">
                                <button
                                    className="dropdown-add-btn"
                                    onClick={() => handleAddContent(hoveredMenu)}
                                >
                                    Добавить
                                </button>
                            </div>
                        )}
                        {currentMenu.items.length > 0 ? (
                            currentMenu.items.map((item) => (
                                <div
                                    key={item.id}
                                    className="menu-dropdown-item"
                                    onMouseEnter={(e) => {
                                        if (item.hasSubmenu) {
                                            handleSubmenuEnter('broadcasts-guests', e);
                                        }
                                    }}
                                >
                                    <div className="menu-item-content">
                                        <div>
                                            <div className="menu-item-title">{item.title}</div>
                                            <div className="menu-item-description">{item.description}</div>
                                        </div>
                                        {isAdmin && !item.hasSubmenu && (
                                            <button
                                                className="delete-item-btn"
                                                onClick={() => handleDeleteItem(hoveredMenu, item.id, item.title)}
                                                title="Удалить"
                                            >
                                                ✕
                                            </button>
                                        )}
                                        {item.hasSubmenu && <span className="submenu-arrow">▶</span>}
                                    </div>
                                </div>
                            ))
                        ) : (
                            !isAdmin && <div className="empty-dropdown">Контент пока не добавлен</div>
                        )}
                    </div>
                )}

                {hoveredSubmenu && (
                    <div
                        className="menu-submenu-positioned"
                        style={{ top: submenuPosition.top, left: submenuPosition.left }}
                        onMouseEnter={handleDropdownEnter}
                        onMouseLeave={handleDropdownLeave}
                    >
                        {isAdmin && (
                            <div className="submenu-add-section">
                                <button
                                    className="submenu-add-btn"
                                    onClick={() => handleAddContent('broadcasts', 'broadcasts-guests')}
                                >
                                    Добавить эфир с гостем
                                </button>
                            </div>
                        )}
                        {(menuData['broadcasts-guests'] || []).map((item) => (
                            <div key={item.id} className="menu-dropdown-item">
                                <div className="menu-item-content">
                                    <div>
                                        <div className="menu-item-title">{item.title}</div>
                                        <div className="menu-item-description">{item.description}</div>
                                    </div>
                                    {isAdmin && (
                                        <button
                                            className="delete-item-btn"
                                            onClick={() => handleDeleteItem('broadcasts-guests', item.id, item.title)}
                                            title="Удалить"
                                        >
                                            ✕
                                        </button>
                                    )}
                                </div>
                            </div>
                        ))}
                        {(!menuData['broadcasts-guests'] || menuData['broadcasts-guests'].length === 0) && !isAdmin && (
                            <div className="empty-submenu">Пока нет эфиров</div>
                        )}
                    </div>
                )}
            </div>

            {/* Модальное окно подтверждения удаления */}
            {showDeleteConfirm && itemToDelete && (
                <div className="modal-overlay" onClick={cancelDeleteItem}>
                    <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                        <div className="modal-header">
                            <span className="modal-icon">🗑️</span>
                            <h3>Подтверждение удаления</h3>
                        </div>
                        <div className="modal-body">
                            <p>Вы действительно хотите удалить эту запись?</p>
                            <div className="item-preview">
                                <strong>{itemToDelete.itemTitle}</strong>
                            </div>
                            <p className="warning-text">Это действие невозможно отменить!</p>
                        </div>
                        <div className="modal-footer">
                            <button className="modal-btn cancel" onClick={cancelDeleteItem}>
                                ❌ Отмена
                            </button>
                            <button className="modal-btn confirm" onClick={executeDeleteItem}>
                                ✅ Да, удалить
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Встроенная модалка добавления контента с записью аудио */}
            {showAddModal && (
                <div className="modal-overlay" onClick={() => {
                    setShowAddModal(false);
                    setSelectedMenuId(null);
                    setNewItemTitle('');
                    setNewItemDescription('');
                    setNewItemMediaUrl('');
                    setNewItemMediaType('');
                    setIsRecording(false);
                    setRecordingTime(0);
                }}>
                    <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                        <h3>Добавить контент</h3>
                        <input
                            type="text"
                            placeholder="Название"
                            value={newItemTitle}
                            onChange={(e) => setNewItemTitle(e.target.value)}
                            className="modal-input"
                        />
                        <textarea
                            placeholder="Описание"
                            value={newItemDescription}
                            onChange={(e) => setNewItemDescription(e.target.value)}
                            className="modal-textarea"
                        />

                        <div className="media-controls-dashboard">
                            <div className="media-buttons-dashboard">
                                <label className="attach-button-dashboard">
                                    Фото
                                    <input
                                        type="file"
                                        accept="image/*"
                                        onChange={(e) => handleFileUpload(e, 'image')}
                                        style={{ display: 'none' }}
                                    />
                                </label>
                                <label className="attach-button-dashboard">
                                    Видео
                                    <input
                                        type="file"
                                        accept="video/*"
                                        onChange={(e) => handleFileUpload(e, 'video')}
                                        style={{ display: 'none' }}
                                    />
                                </label>
                                <label className="attach-button-dashboard">
                                    Аудио
                                    <input
                                        type="file"
                                        accept="audio/*"
                                        onChange={(e) => handleFileUpload(e, 'audio')}
                                        style={{ display: 'none' }}
                                    />
                                </label>
                                <button
                                    className={`mic-button-dashboard ${isRecording ? 'recording' : ''}`}
                                    onClick={isRecording ? stopRecording : startRecording}
                                >
                                    {isRecording ? '⏹️ Остановить' : '🎤 Записать голос'}
                                </button>
                            </div>

                            {isRecording && (
                                <div className="recording-indicator-dashboard">
                                    <span className="recording-dot">🔴</span>
                                    <span className="recording-time">{formatRecordingTime(recordingTime)}</span>
                                </div>
                            )}

                            {newItemMediaUrl && (
                                <div className="attached-file-dashboard">
                                    <span className="file-info">
                                        {newItemMediaType === 'image' && '📷 Фото прикреплено'}
                                        {newItemMediaType === 'video' && '🎬 Видео прикреплено'}
                                        {newItemMediaType === 'audio' && '🎵 Аудио прикреплено'}
                                    </span>
                                    <button
                                        className="remove-file-dashboard"
                                        onClick={() => {
                                            setNewItemMediaUrl('');
                                            setNewItemMediaType('');
                                        }}
                                    >
                                        ✕
                                    </button>
                                </div>
                            )}
                        </div>

                        <div className="modal-buttons">
                            <button className="modal-btn save" onClick={handleSaveContent}>
                                Сохранить
                            </button>
                            <button className="modal-btn cancel" onClick={() => {
                                setShowAddModal(false);
                                setSelectedMenuId(null);
                                setNewItemTitle('');
                                setNewItemDescription('');
                                setNewItemMediaUrl('');
                                setNewItemMediaType('');
                                setIsRecording(false);
                                setRecordingTime(0);
                            }}>
                                Отмена
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default Dashboard;
