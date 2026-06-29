import React, { useEffect, useState, useMemo, useCallback, useRef } from 'react';
import './Dashboard.css';
import '../common/Modal.css';
import { dashboardApi } from '../../services/api';
import useAudioRecorder from '../../hooks/useAudioRecorder';
import { usePermissions } from '../../hooks/usePermissions';
import ConfirmModal from '../common/ConfirmModal';
import AddContentModal from '../common/AddContentModal';
import {
    initialBroadcasts,
    initialBroadcastsGuests,
    initialExercises,
    initialRecommendations,
    initialHardening,
    initialAbout,
    initialNewbies,
    initialHeartVoice,
    initialReflection,
    initialInterviews,
    initialParticipants,
    initialMotivation,
    initialReviews,
    initialMusic,
    initialMovies,
    initialLiterature,
    initialAboutTimoshenko,
    initialChallenge
} from './initialData';

const SECTION_META = [
    { key: 'broadcasts', initial: initialBroadcasts },
    { key: 'broadcasts-guests', initial: initialBroadcastsGuests },
    { key: 'exercises', initial: initialExercises },
    { key: 'recommendations', initial: initialRecommendations },
    { key: 'hardening', initial: initialHardening },
    { key: 'about', initial: initialAbout },
    { key: 'newbies', initial: initialNewbies },
    { key: 'heart-voice', initial: initialHeartVoice },
    { key: 'reflection', initial: initialReflection },
    { key: 'interviews', initial: initialInterviews },
    { key: 'participants', initial: initialParticipants },
    { key: 'motivation', initial: initialMotivation },
    { key: 'reviews', initial: initialReviews },
    { key: 'music', initial: initialMusic },
    { key: 'movies', initial: initialMovies },
    { key: 'literature', initial: initialLiterature },
    { key: 'about-timoshenko', initial: initialAboutTimoshenko },
    { key: 'challenge', initial: initialChallenge }
];

/**
 * Dashboard — admin content management panel with 18 section tabs.
 *
 * Provides a tabbed interface for managing content sections (broadcasts, exercises,
 * recommendations, hardening, about, newbies, heart voice, reflection, interviews,
 * participants, motivation, reviews, music, movies, literature, about Timoshenko,
 * challenge). Supports CRUD operations, audio recording, file uploads, and
 * section-specific content management with permission-based access control.
 *
 * @returns {JSX.Element} The dashboard with section tabs, content list, and add/edit forms
 */
const Dashboard = () => {
    const [hoveredMenu, setHoveredMenu] = useState(null);
    const [hoveredSubmenu, setHoveredSubmenu] = useState(null);
    const [isOverCard, setIsOverCard] = useState(false);
    const [isOverDropdown, setIsOverDropdown] = useState(false);
    const [isOverSubmenu, setIsOverSubmenu] = useState(false);
    const [isOverSubmenuTrigger, setIsOverSubmenuTrigger] = useState(false);
    const isOverSubmenuRef = useRef(false);
    const isOverSubmenuTriggerRef = useRef(false);
    const hideTimeoutRef = useRef(null);
    const submenuShowTimeoutRef = useRef(null);
    const submenuHideTimeoutRef = useRef(null);
    const [menuPosition, setMenuPosition] = useState({ top: 0, left: 0 });
    const [submenuPosition, setSubmenuPosition] = useState({ top: 0, left: 0 });
    const [showAddModal, setShowAddModal] = useState(false);
    const [selectedMenuId, setSelectedMenuId] = useState(null);
    const [newItemTitle, setNewItemTitle] = useState('');
    const [newItemDescription, setNewItemDescription] = useState('');
    const [newItemMediaUrl, setNewItemMediaUrl] = useState('');
    const [newItemMediaType, setNewItemMediaType] = useState('');
    const blobUrlsRef = useRef([]);
    const [menuData, setMenuData] = useState({});
    const [error, setError] = useState(null);

    // Состояния для модального окна подтверждения удаления
    const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
    const [itemToDelete, setItemToDelete] = useState(null);
    const [editingItem, setEditingItem] = useState(null);

    // Хук записи аудио
    const {
        isRecording,
        recordingTime,
        audioBlobUrl,
        startRecording,
        stopRecording,
        resetRecording
    } = useAudioRecorder();

    const { hasLevel } = usePermissions();

    const isAdmin = hasLevel(6);
    const canEdit = hasLevel(6);
    const isSuperAdmin = hasLevel(8);

    const buildInitialData = useCallback(() => {
        const data = {};
        SECTION_META.forEach(meta => {
            data[meta.key] = meta.initial;
        });
        return data;
    }, []);

    useEffect(() => {
        const loadDashboardData = async () => {
            try {
                const data = await dashboardApi.getMenuData();
                setMenuData(data);
                setError(null);
            } catch (err) {
                if (process.env.NODE_ENV === 'development') {
                    try {
                        const savedData = process.env.NODE_ENV === 'development'
                            ? sessionStorage.getItem('dashboardMenuData')
                            : null;
                        if (savedData) {
                            const parsedData = JSON.parse(savedData);
                            let needsUpdate = false;

                            SECTION_META.forEach(meta => {
                                if (!parsedData[meta.key] || parsedData[meta.key].length < 5) {
                                    parsedData[meta.key] = meta.initial;
                                    needsUpdate = true;
                                }
                            });

                            if (needsUpdate) {
                                if (process.env.NODE_ENV === 'development') {
                                    sessionStorage.setItem('dashboardMenuData', JSON.stringify(parsedData));
                                }
                            }
                            setMenuData(parsedData);
                        } else {
                            const initialData = buildInitialData();
                            setMenuData(initialData);
                            if (process.env.NODE_ENV === 'development') {
                                sessionStorage.setItem('dashboardMenuData', JSON.stringify(initialData));
                            }
                        }
                        setError(null);
                    } catch (localError) {
                        setError('Не удалось загрузить данные дашборда');
                    }
                }
            }
        };
        loadDashboardData();
    }, [buildInitialData]);

    useEffect(() => {
        return () => {
            blobUrlsRef.current.forEach(url => URL.revokeObjectURL(url));
            blobUrlsRef.current = [];
        };
    }, []);

    const saveMenuData = useCallback(async (data) => {
        try {
            setMenuData(data);
            await dashboardApi.saveMenuData(data);
            setError(null);
        } catch (err) {
            if (process.env.NODE_ENV === 'development') {
                try {
                    sessionStorage.setItem('dashboardMenuData', JSON.stringify(data));
                } catch (localError) {
                    setError('Не удалось сохранить данные дашборда');
                }
            } else {
                setError('Не удалось сохранить данные дашборда');
            }
        }
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
        if (hideTimeoutRef.current) {
            clearTimeout(hideTimeoutRef.current);
            hideTimeoutRef.current = null;
        }
        const rect = event.currentTarget.getBoundingClientRect();
        const viewportWidth = window.innerWidth;
        const viewportHeight = window.innerHeight;

        const menuWidth = Math.max(400, Math.min(800, viewportWidth * 0.9));
        const menuHeight = Math.min(500, viewportHeight * 0.7);

        let top = rect.bottom + window.scrollY;
        let left = rect.left + window.scrollX;

        if (left + menuWidth > viewportWidth - 20) {
            left = viewportWidth - menuWidth - 20;
        }

        if (left < 20) {
            left = 20;
        }

        if (top + menuHeight > viewportHeight + window.scrollY - 20) {
            top = rect.top + window.scrollY - menuHeight - 10;
        }

        if (top < window.scrollY + 20) {
            top = window.scrollY + 20;
        }

        setMenuPosition({ top, left });
        setHoveredMenu(menuId);
        setHoveredSubmenu(null);
        setIsOverCard(true);
    }, []);

    const handleCardLeave = useCallback(() => {
        setIsOverCard(false);
    }, []);

    const handleDropdownEnter = useCallback(() => {
        if (hideTimeoutRef.current) {
            clearTimeout(hideTimeoutRef.current);
            hideTimeoutRef.current = null;
        }
        setIsOverDropdown(true);
    }, []);

    const handleDropdownLeave = useCallback(() => {
        setIsOverDropdown(false);
    }, []);

    useEffect(() => {
        if (!isOverCard && !isOverDropdown && !isOverSubmenu && !isOverSubmenuTrigger) {
            hideTimeoutRef.current = setTimeout(() => {
                setHoveredMenu(null);
                setHoveredSubmenu(null);
            }, 100);
            return () => {
                if (hideTimeoutRef.current) clearTimeout(hideTimeoutRef.current);
            };
        }
    }, [isOverCard, isOverDropdown, isOverSubmenu, isOverSubmenuTrigger]);

    const handleSubmenuEnter = useCallback((submenuId, event) => {
        if (hideTimeoutRef.current) clearTimeout(hideTimeoutRef.current);

        const rect = event.currentTarget.getBoundingClientRect();
        const viewportWidth = window.innerWidth;
        const viewportHeight = window.innerHeight;
        const submenuWidth = Math.max(400, Math.min(800, viewportWidth * 0.9));
        const submenuHeight = Math.min(500, viewportHeight * 0.7);

        let top = rect.top + window.scrollY;
        let left = rect.right + window.scrollX;

        if (left + submenuWidth > viewportWidth - 20) {
            left = rect.left + window.scrollX - submenuWidth;
        }
        if (left < 20) left = 20;
        if (top + submenuHeight > viewportHeight + window.scrollY - 20) {
            top = viewportHeight + window.scrollY - submenuHeight - 20;
        }
        if (top < window.scrollY + 20) {
            top = window.scrollY + 20;
        }

        setSubmenuPosition({ top, left });
        setHoveredSubmenu(submenuId);
    }, []);

    const handleSubmenuItemEnter = useCallback((submenuId, event) => {
        if (submenuHideTimeoutRef.current) {
            clearTimeout(submenuHideTimeoutRef.current);
            submenuHideTimeoutRef.current = null;
        }

        setIsOverSubmenuTrigger(true);
        isOverSubmenuTriggerRef.current = true;

        const rect = event.currentTarget.getBoundingClientRect();
        const scrollX = window.scrollX;
        const scrollY = window.scrollY;

        submenuShowTimeoutRef.current = setTimeout(() => {
            const viewportWidth = window.innerWidth;
            const viewportHeight = window.innerHeight;

            const submenuWidth = Math.max(400, Math.min(800, viewportWidth * 0.9));
            const submenuHeight = Math.min(500, viewportHeight * 0.7);

            let top = rect.top + scrollY;
            let left = rect.right + scrollX;

            if (left + submenuWidth > viewportWidth - 20) {
                left = rect.left + scrollX - submenuWidth;
            }
            if (left < 20) left = 20;
            if (top + submenuHeight > viewportHeight + scrollY - 20) {
                top = viewportHeight + scrollY - submenuHeight - 20;
            }
            if (top < scrollY + 20) top = scrollY + 20;

            setSubmenuPosition({ top, left });
            setHoveredSubmenu(submenuId);
        }, 1000);
    }, []);

    const handleSubmenuItemLeave = useCallback(() => {
        if (submenuShowTimeoutRef.current) {
            clearTimeout(submenuShowTimeoutRef.current);
            submenuShowTimeoutRef.current = null;
        }

        setIsOverSubmenuTrigger(false);
        isOverSubmenuTriggerRef.current = false;

        submenuHideTimeoutRef.current = setTimeout(() => {
            if (!isOverSubmenuRef.current) {
                setHoveredSubmenu(null);
            }
        }, 500);
    }, []);

    const handleSubmenuMouseEnter = useCallback(() => {
        if (submenuHideTimeoutRef.current) {
            clearTimeout(submenuHideTimeoutRef.current);
            submenuHideTimeoutRef.current = null;
        }
        setIsOverSubmenu(true);
        isOverSubmenuRef.current = true;
    }, []);

    const handleSubmenuMouseLeave = useCallback(() => {
        setIsOverSubmenu(false);
        isOverSubmenuRef.current = false;
        submenuHideTimeoutRef.current = setTimeout(() => {
            if (!isOverSubmenuTriggerRef.current) {
                setHoveredSubmenu(null);
            }
        }, 300);
    }, []);

    const handleAddContent = useCallback((menuId, submenuId = null) => {
        setSelectedMenuId(submenuId || menuId);
        setShowAddModal(true);
        setNewItemTitle('');
        setNewItemDescription('');
        setNewItemMediaUrl('');
        setNewItemMediaType('');
        resetRecording();
        setHoveredMenu(null);
        setHoveredSubmenu(null);
        if (hideTimeoutRef.current) {
            clearTimeout(hideTimeoutRef.current);
            hideTimeoutRef.current = null;
        }
    }, [resetRecording]);

    const cleanupBlobUrls = useCallback(() => {
        if (newItemMediaUrl && newItemMediaUrl.startsWith('blob:')) {
            URL.revokeObjectURL(newItemMediaUrl);
        }
        blobUrlsRef.current.forEach(url => {
            if (url && url.startsWith('blob:')) {
                URL.revokeObjectURL(url);
            }
        });
        blobUrlsRef.current = [];
    }, [newItemMediaUrl]);

    const handleSaveContent = useCallback(() => {
        if (!newItemTitle.trim()) return;

        try {
            const updatedData = { ...menuData };

            if (editingItem) {
                if (!updatedData[selectedMenuId]) {
                    updatedData[selectedMenuId] = [];
                }
                updatedData[selectedMenuId] = updatedData[selectedMenuId].map(item =>
                    item.id === editingItem.id
                        ? {
                            ...item,
                            title: newItemTitle,
                            description: newItemDescription,
                            mediaUrl: newItemMediaUrl,
                            mediaType: newItemMediaType,
                            editedAt: new Date().toISOString()
                        }
                        : item
                );
                setEditingItem(null);
            } else {
                const newItem = {
                    id: Date.now(),
                    title: newItemTitle,
                    description: newItemDescription,
                    mediaUrl: newItemMediaUrl,
                    mediaType: newItemMediaType
                };

                if (!updatedData[selectedMenuId]) {
                    updatedData[selectedMenuId] = [];
                }
                updatedData[selectedMenuId].push(newItem);
            }

            saveMenuData(updatedData);

            cleanupBlobUrls();
            setNewItemTitle('');
            setNewItemDescription('');
            setNewItemMediaUrl('');
            setNewItemMediaType('');
            resetRecording();
            setShowAddModal(false);
            setSelectedMenuId(null);
        } catch (err) {
            setError('Не удалось сохранить контент');
        }
    }, [newItemTitle, newItemDescription, newItemMediaUrl, newItemMediaType, menuData, selectedMenuId, editingItem, saveMenuData, cleanupBlobUrls, resetRecording]);

    const handleFileUpload = useCallback((e, type) => {
        try {
            const file = e.target.files[0];
            if (file) {
                const url = URL.createObjectURL(file);
                blobUrlsRef.current.push(url);
                setNewItemMediaUrl(url);
                setNewItemMediaType(type);
            }
        } catch (err) {
            setError('Не удалось загрузить файл');
        }
    }, []);

    const handleStartRecording = useCallback(async () => {
        try {
            await startRecording();
        } catch (err) {
            setError('Не удалось получить доступ к микрофону');
        }
    }, [startRecording]);

    const handleStopRecording = useCallback(() => {
        stopRecording();
    }, [stopRecording]);

    useEffect(() => {
        if (audioBlobUrl) {
            blobUrlsRef.current.push(audioBlobUrl);
            setNewItemMediaUrl(audioBlobUrl);
            setNewItemMediaType('audio');
        }
    }, [audioBlobUrl]);

    const formatRecordingTime = (seconds) => {
        const mins = Math.floor(seconds / 60);
        const secs = seconds % 60;
        return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    };

    const handleDeleteItem = useCallback((menuId, itemId, itemTitle) => {
        if (!canEdit) {
            alert('У вас нет прав на удаление');
            return;
        }
        setItemToDelete({ menuId, itemId, itemTitle });
        setShowDeleteConfirm(true);
        setHoveredMenu(null);
        setHoveredSubmenu(null);
        if (hideTimeoutRef.current) {
            clearTimeout(hideTimeoutRef.current);
            hideTimeoutRef.current = null;
        }
    }, [canEdit]);

    const executeDeleteItem = useCallback(() => {
        if (!itemToDelete) return;

        try {
            const updatedData = { ...menuData };
            if (updatedData[itemToDelete.menuId]) {
                updatedData[itemToDelete.menuId] = updatedData[itemToDelete.menuId].filter(
                    item => item.id !== itemToDelete.itemId
                );
                saveMenuData(updatedData);
            }

            setShowDeleteConfirm(false);
            setItemToDelete(null);
        } catch (err) {
            setError('Не удалось удалить элемент');
        }
    }, [itemToDelete, menuData, saveMenuData]);

    const handleEditItem = useCallback((menuId, item) => {
        if (!canEdit) {
            alert('У вас нет прав на редактирование');
            return;
        }
        setEditingItem(item);
        setSelectedMenuId(menuId);
        setNewItemTitle(item.title);
        setNewItemDescription(item.description || '');
        setNewItemMediaUrl(item.mediaUrl || '');
        setNewItemMediaType(item.mediaType || '');
        setShowAddModal(true);
        setHoveredMenu(null);
        setHoveredSubmenu(null);
        if (hideTimeoutRef.current) {
            clearTimeout(hideTimeoutRef.current);
            hideTimeoutRef.current = null;
        }
    }, [canEdit]);

    const handleDownloadBroadcast = useCallback((item) => {
        if (!isSuperAdmin) {
            alert('Только главный админ может скачивать эфиры');
            return;
        }

        if (item.mediaUrl) {
            const link = document.createElement('a');
            link.href = item.mediaUrl;
            link.download = `${item.title}.${item.mediaType === 'audio' ? 'mp3' : 'mp4'}`;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
        } else {
            alert('У этого эфира нет медиафайла для скачивания');
        }

        setHoveredMenu(null);
        setHoveredSubmenu(null);
        if (hideTimeoutRef.current) {
            clearTimeout(hideTimeoutRef.current);
            hideTimeoutRef.current = null;
        }
    }, [isSuperAdmin]);

    const handleItemClick = useCallback((item) => {
        if (item.description && item.description.trim()) {
            const url = item.description.trim();
            try {
                const parsed = new URL(url.startsWith('http') ? url : 'https://' + url);
                if (parsed.protocol === 'http:' || parsed.protocol === 'https:') {
                    window.open(parsed.href, '_blank', 'noopener,noreferrer');
                }
            } catch (e) {
            }
        }
    }, []);

    const cancelDeleteItem = useCallback(() => {
        setShowDeleteConfirm(false);
        setItemToDelete(null);
    }, []);

    const clearError = useCallback(() => {
        setError(null);
    }, []);

    const handleCloseAddModal = useCallback(() => {
        // Очищаем все blob URL, созданные при открытии модалки
        if (newItemMediaUrl && newItemMediaUrl.startsWith('blob:')) {
            URL.revokeObjectURL(newItemMediaUrl);
        }
        blobUrlsRef.current.forEach(url => {
            if (url && url.startsWith('blob:')) {
                URL.revokeObjectURL(url);
            }
        });
        blobUrlsRef.current = [];

        setShowAddModal(false);
        setSelectedMenuId(null);
        setEditingItem(null);
        setNewItemTitle('');
        setNewItemDescription('');
        setNewItemMediaUrl('');
        setNewItemMediaType('');
        resetRecording();
    }, [newItemMediaUrl, resetRecording]);

    return (
        <div className="dashboard">
            {error && (
                <div className="error-container">
                    <div className="error-alert">
                        <div className="error-alert-content">
                            <div className="error-alert-title">Ошибка</div>
                            <div className="error-alert-message">{error}</div>
                        </div>
                        <button className="error-alert-close" onClick={clearError}>✕</button>
                    </div>
                </div>
            )}
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
                                    className={`menu-dropdown-item ${item.hasSubmenu ? 'has-submenu' : ''}`}
                                    onClick={(e) => {
                                        if (!item.hasSubmenu) {
                                            handleItemClick(item);
                                        }
                                    }}
                                    onMouseEnter={(e) => {
                                        if (item.hasSubmenu) {
                                            handleSubmenuItemEnter('broadcasts-guests', e);
                                        }
                                    }}
                                    onMouseLeave={() => {
                                        if (item.hasSubmenu) {
                                            handleSubmenuItemLeave();
                                        }
                                    }}
                                    style={{ cursor: 'pointer' }}
                                >
                                    <div className="menu-item-content">
                                        <div>
                                            <div className="menu-item-title">
                                                {item.hasSubmenu && <span className="submenu-icon">📁 </span>}
                                                {item.title}
                                                {item.editedAt && <span className="edited-badge"> (изменено)</span>}
                                            </div>
                                            <div className="menu-item-description">{item.description}</div>
                                        </div>
                                        {canEdit && !item.hasSubmenu && (
                                            <div className="menu-item-actions">
                                                {isSuperAdmin && hoveredMenu === 'broadcasts' && (
                                                    <button
                                                        className="download-item-btn"
                                                        onClick={(e) => {
                                                            e.stopPropagation();
                                                            handleDownloadBroadcast(item);
                                                        }}
                                                        title="Скачать эфир"
                                                    >
                                                        ⬇️
                                                    </button>
                                                )}
                                                <button
                                                    className="edit-item-btn"
                                                    onClick={(e) => {
                                                        e.stopPropagation();
                                                        handleEditItem(hoveredMenu, item);
                                                    }}
                                                    title="Редактировать"
                                                >
                                                    ✏️
                                                </button>
                                                <button
                                                    className="delete-item-btn"
                                                    onClick={(e) => {
                                                        e.stopPropagation();
                                                        handleDeleteItem(hoveredMenu, item.id, item.title);
                                                    }}
                                                    title="Удалить"
                                                >
                                                    🗑️
                                                </button>
                                            </div>
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
                        onMouseEnter={handleSubmenuMouseEnter}
                        onMouseLeave={handleSubmenuMouseLeave}
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
                            <div
                                key={item.id}
                                className="menu-dropdown-item"
                                onClick={() => handleItemClick(item)}
                                style={{ cursor: 'pointer' }}
                            >
                                <div className="menu-item-content">
                                    <div>
                                        <div className="menu-item-title">
                                            {item.title}
                                            {item.editedAt && <span className="edited-badge"> (изменено)</span>}
                                        </div>
                                        <div className="menu-item-description">{item.description}</div>
                                    </div>
                                    {canEdit && (
                                        <div className="menu-item-actions">
                                            {isSuperAdmin && (
                                                <button
                                                    className="download-item-btn"
                                                    onClick={(e) => {
                                                        e.stopPropagation();
                                                        handleDownloadBroadcast(item);
                                                    }}
                                                    title="Скачать эфир"
                                                >
                                                    ⬇️
                                                </button>
                                            )}
                                            <button
                                                className="edit-item-btn"
                                                onClick={(e) => {
                                                    e.stopPropagation();
                                                    handleEditItem('broadcasts-guests', item);
                                                }}
                                                title="Редактировать"
                                            >
                                                ✏️
                                            </button>
                                            <button
                                                className="delete-item-btn"
                                                onClick={(e) => {
                                                    e.stopPropagation();
                                                    handleDeleteItem('broadcasts-guests', item.id, item.title);
                                                }}
                                                title="Удалить"
                                            >
                                                🗑️
                                            </button>
                                        </div>
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

            <ConfirmModal
                isOpen={showDeleteConfirm}
                title="Подтверждение удаления"
                message="Вы действительно хотите удалить эту запись?"
                itemLabel={itemToDelete?.itemTitle || ''}
                onConfirm={executeDeleteItem}
                onCancel={cancelDeleteItem}
            />

            <AddContentModal
                isOpen={showAddModal}
                onClose={handleCloseAddModal}
                editingItem={editingItem}
                selectedMenuId={selectedMenuId}
                newItemTitle={newItemTitle}
                setNewItemTitle={setNewItemTitle}
                newItemDescription={newItemDescription}
                setNewItemDescription={setNewItemDescription}
                newItemMediaUrl={newItemMediaUrl}
                setNewItemMediaUrl={setNewItemMediaUrl}
                newItemMediaType={newItemMediaType}
                setNewItemMediaType={setNewItemMediaType}
                isRecording={isRecording}
                recordingTime={recordingTime}
                handleStartRecording={handleStartRecording}
                handleStopRecording={handleStopRecording}
                handleFileUpload={handleFileUpload}
                handleSaveContent={handleSaveContent}
                formatRecordingTime={formatRecordingTime}
                resetRecording={resetRecording}
            />
        </div>
    );
};

export default Dashboard;
