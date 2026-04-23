import React, { useState, useEffect, useRef, useMemo, useCallback } from 'react';
import { getUserRoles } from '../../keycloak';
import { useAudioPlayer } from '../../context/AudioPlayerContext';
import ContentModal from '../Common/ContentModal';
import './MainFeed.css';
import './NutritionFeed.css';

const NutritionFeed = () => {
    const [posts, setPosts] = useState([]);
    const [newPost, setNewPost] = useState({ type: 'text', title: '', content: '', url: '' });
    const [showAddForm, setShowAddForm] = useState(false);
    const [userRoles, setUserRoles] = useState([]);
    const [notification, setNotification] = useState(null);
    const feedEndRef = useRef(null);
    const addFormRef = useRef(null);
    const [activeAudioPostId, setActiveAudioPostId] = useState(null);
    const [, forceUpdate] = useState(0);

    // Состояния для модального окна подтверждения удаления
    const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
    const [postToDelete, setPostToDelete] = useState(null);

    // Состояние для ContentModal
    const [showContentModal, setShowContentModal] = useState(false);

    // Состояния для навигационных меню
    const [hoveredMenu, setHoveredMenu] = useState(null);
    const [hideTimeout, setHideTimeout] = useState(null);
    const [isOverCard, setIsOverCard] = useState(false);
    const [isOverDropdown, setIsOverDropdown] = useState(false);
    const [menuPosition, setMenuPosition] = useState({ top: 0, left: 0 });
    const [showAddModal, setShowAddModal] = useState(false);
    const [selectedMenuId, setSelectedMenuId] = useState(null);
    const [menuData, setMenuData] = useState({});
    const [itemToDelete, setItemToDelete] = useState(null);
    const [showDeleteItemConfirm, setShowDeleteItemConfirm] = useState(false);

    const {
        playTrack,
        pauseTrack,
        currentTrack,
        isPlaying,
        stopTrack,
        volume,
        playbackRate,
        setVolume,
        setPlaybackRate,
        seek,
        currentTime,
        duration,
        formatTime
    } = useAudioPlayer();

    // Функция для обновления ролей из localStorage
    const updateRoles = () => {
        const demoRole = localStorage.getItem('demoRole');
        let roles = [];

        if (demoRole === 'admin-moderator' || demoRole === 'admin-consultant' || demoRole === 'admin-nutrition' || demoRole === 'developer' || demoRole === 'super-admin') {
            roles = ['admin', 'user'];
        } else if (demoRole === 'user1' || demoRole === 'user2') {
            roles = ['user'];
        } else {
            roles = getUserRoles();
        }

        setUserRoles(roles);
        return roles;
    };

    // Синхронизируем activeAudioPostId с currentTrack
    useEffect(() => {
        if (currentTrack) {
            setActiveAudioPostId(currentTrack.id);
        } else if (!currentTrack && activeAudioPostId) {
            const timer = setTimeout(() => {
                setActiveAudioPostId(null);
            }, 500);
            return () => clearTimeout(timer);
        }
    }, [currentTrack]);

    // Загрузка постов и ролей
    useEffect(() => {
        const savedPosts = localStorage.getItem('nutritionFeedPosts');
        if (savedPosts) {
            setPosts(JSON.parse(savedPosts));
        } else {
            const initialPosts = [
                {
                    id: 1,
                    type: 'text',
                    title: '🥗 Основы здорового питания',
                    content: 'Правильное питание - основа здоровья. Узнайте о принципах сбалансированного рациона.',
                    date: '2025-03-15',
                    author: 'Admin'
                },
                {
                    id: 2,
                    type: 'text',
                    title: '🍎 Польза фруктов',
                    content: 'Фрукты - источник витаминов и минералов. Включайте их в свой ежедневный рацион.',
                    date: '2025-03-16',
                    author: 'Admin'
                }
            ];
            setPosts(initialPosts);
            localStorage.setItem('nutritionFeedPosts', JSON.stringify(initialPosts));
        }

        updateRoles();
    }, []);

    useEffect(() => {
        if (feedEndRef.current) {
            feedEndRef.current.scrollIntoView({ behavior: 'auto' });
        }
    }, [posts]);

    useEffect(() => {
        if (showAddForm && addFormRef.current) {
            setTimeout(() => {
                addFormRef.current.scrollIntoView({ behavior: 'smooth', block: 'center' });
            }, 100);
        }
    }, [showAddForm]);

    // Функция для определения типа файла
    const getFileType = (file) => {
        if (file.type.startsWith('audio/')) return 'audio';
        if (file.type.startsWith('video/')) return 'video';
        return 'text';
    };

    // Обработка загрузки файла
    const handleFileUpload = (e) => {
        const file = e.target.files[0];
        if (!file) return;

        const fileType = getFileType(file);
        const url = URL.createObjectURL(file);

        setNewPost({
            ...newPost,
            type: fileType,
            url: url,
            title: newPost.title || file.name.replace(/\.[^/.]+$/, '')
        });
    };

    const handleAddPost = () => {
        const demoRole = localStorage.getItem('demoRole');
        const isAdmin = demoRole === 'admin-moderator' || demoRole === 'admin-nutrition' || demoRole === 'developer' || demoRole === 'super-admin';

        if (!isAdmin) {
            alert('Только администратор питания может добавлять записи');
            return;
        }

        setShowContentModal(true);
    };

    const handleSavePost = (data) => {
        const post = {
            id: Date.now(),
            type: data.mediaType || 'text',
            title: data.title,
            content: data.description,
            url: data.mediaUrl || '',
            date: new Date().toISOString().split('T')[0],
            author: 'Admin'
        };

        const updatedPosts = [...posts, post];
        setPosts(updatedPosts);
        localStorage.setItem('nutritionFeedPosts', JSON.stringify(updatedPosts));

        setShowContentModal(false);

        setTimeout(() => {
            if (feedEndRef.current) {
                feedEndRef.current.scrollIntoView({ behavior: 'smooth' });
            }
        }, 100);
    };

    // Функция для открытия модального окна подтверждения
    const confirmDeletePost = (post) => {
        setPostToDelete(post);
        setShowDeleteConfirm(true);
    };

    // Функция для фактического удаления
    const executeDeletePost = () => {
        if (!postToDelete) return;

        const demoRole = localStorage.getItem('demoRole');
        const isAdmin = demoRole === 'admin-moderator' || demoRole === 'admin-nutrition' || demoRole === 'developer' || demoRole === 'super-admin';

        if (!isAdmin) {
            alert('Только администратор питания может удалять записи!');
            setShowDeleteConfirm(false);
            setPostToDelete(null);
            return;
        }

        const updatedPosts = posts.filter(post => post.id !== postToDelete.id);
        setPosts(updatedPosts);
        localStorage.setItem('nutritionFeedPosts', JSON.stringify(updatedPosts));

        if (currentTrack?.id === postToDelete.id) {
            stopTrack();
        }

        setShowDeleteConfirm(false);
        setPostToDelete(null);
        forceUpdate(prev => prev + 1);
    };

    // Функция для отмены удаления
    const cancelDeletePost = () => {
        setShowDeleteConfirm(false);
        setPostToDelete(null);
    };

    const handlePlayAudio = (post) => {
        if (activeAudioPostId === post.id && currentTrack?.id === post.id && isPlaying) {
            pauseTrack();
        } else if (activeAudioPostId === post.id && currentTrack?.id === post.id && !isPlaying) {
            playTrack({ id: post.id, title: post.title, url: post.url });
        } else {
            setActiveAudioPostId(post.id);
            playTrack({ id: post.id, title: post.title, url: post.url });
        }
    };

    const handleSeek = (e, postId) => {
        const newTime = parseFloat(e.target.value);
        if (currentTrack?.id === postId) {
            seek(newTime);
        }
    };

    const handleVolumeChangeLocal = (e, postId) => {
        const newVolume = parseFloat(e.target.value);
        if (currentTrack?.id === postId) {
            setVolume(newVolume);
        }
    };

    const handleSpeedChangeLocal = (e, postId) => {
        const newSpeed = parseFloat(e.target.value);
        if (currentTrack?.id === postId) {
            setPlaybackRate(newSpeed);
        }
    };

    const getCurrentTimeForPost = (postId) => {
        if (currentTrack?.id === postId) {
            return currentTime;
        }
        return 0;
    };

    const getDurationForPost = (postId) => {
        if (currentTrack?.id === postId) {
            return duration;
        }
        return 0;
    };

    const isThisPlaying = (postId) => {
        return currentTrack?.id === postId && isPlaying;
    };

    const demoRole = localStorage.getItem('demoRole');
    const isAdmin = demoRole === 'admin-nutrition' || demoRole === 'developer' || demoRole === 'super-admin';

    // Загрузка данных меню
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

    const nutritionMenus = useMemo(() => [
        { id: 'nutrition', title: 'Питание', items: menuData['nutrition'] || [] },
        { id: 'recipes', title: 'Рецепты', items: menuData['recipes'] || [] },
        { id: 'living-kitchen', title: 'Живая кухня', items: menuData['living-kitchen'] || [] }
    ], [menuData]);

    const currentMenu = useMemo(() =>
        nutritionMenus.find(m => m.id === hoveredMenu)
    , [nutritionMenus, hoveredMenu]);

    const handleMenuEnter = useCallback((menuId, event) => {
        if (hideTimeout) {
            clearTimeout(hideTimeout);
            setHideTimeout(null);
        }
        const rect = event.currentTarget.getBoundingClientRect();
        setMenuPosition({
            top: rect.top + window.scrollY,
            left: rect.left + window.scrollX - 320
        });
        setHoveredMenu(menuId);
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

    useEffect(() => {
        if (!isOverCard && !isOverDropdown) {
            const timeout = setTimeout(() => {
                setHoveredMenu(null);
            }, 100);
            setHideTimeout(timeout);
            return () => clearTimeout(timeout);
        }
    }, [isOverCard, isOverDropdown]);

    const handleAddContent = useCallback((menuId) => {
        setSelectedMenuId(menuId);
        setShowAddModal(true);
        setHoveredMenu(null);
        if (hideTimeout) {
            clearTimeout(hideTimeout);
            setHideTimeout(null);
        }
    }, [hideTimeout]);

    const handleSaveContent = useCallback((data) => {
        const newItem = {
            id: Date.now(),
            title: data.title,
            description: data.description,
            mediaUrl: data.mediaUrl,
            mediaType: data.mediaType
        };

        const updatedData = { ...menuData };
        if (!updatedData[selectedMenuId]) {
            updatedData[selectedMenuId] = [];
        }
        updatedData[selectedMenuId].push(newItem);
        saveMenuData(updatedData);

        setShowAddModal(false);
        setSelectedMenuId(null);
    }, [menuData, selectedMenuId, saveMenuData]);

    const handleDeleteItem = useCallback((menuId, itemId, itemTitle) => {
        setItemToDelete({ menuId, itemId, itemTitle });
        setShowDeleteItemConfirm(true);
        setHoveredMenu(null);
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

        setShowDeleteItemConfirm(false);
        setItemToDelete(null);
    }, [itemToDelete, menuData, saveMenuData]);

    const cancelDeleteItem = useCallback(() => {
        setShowDeleteItemConfirm(false);
        setItemToDelete(null);
    }, []);

    return (
        <div className="nutrition-feed-container">
            {/* Модальное окно подтверждения удаления элемента меню */}
            {showDeleteItemConfirm && itemToDelete && (
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

            {/* Боковая панель с меню */}
            <div className="nutrition-sidebar">
                {nutritionMenus.map((menu) => (
                    <div
                        key={menu.id}
                        className="nutrition-menu-card"
                        onMouseEnter={(e) => handleMenuEnter(menu.id, e)}
                        onMouseLeave={handleCardLeave}
                    >
                        <span className="nutrition-menu-title">{menu.title}</span>
                    </div>
                ))}

                {hoveredMenu && currentMenu && (
                    <div
                        className="nutrition-menu-dropdown"
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
                                <div key={item.id} className="menu-dropdown-item">
                                    <div className="menu-item-content">
                                        <div>
                                            <div className="menu-item-title">{item.title}</div>
                                            <div className="menu-item-description">{item.description}</div>
                                        </div>
                                        {isAdmin && (
                                            <button
                                                className="delete-item-btn"
                                                onClick={() => handleDeleteItem(hoveredMenu, item.id, item.title)}
                                                title="Удалить"
                                            >
                                                ✕
                                            </button>
                                        )}
                                    </div>
                                </div>
                            ))
                        ) : (
                            !isAdmin && <div className="empty-dropdown">Контент пока не добавлен</div>
                        )}
                    </div>
                )}
            </div>

            {/* Основная лента */}
            <div className="main-feed">
            {/* Модальное окно подтверждения удаления */}
            {showDeleteConfirm && postToDelete && (
                <div className="modal-overlay" onClick={cancelDeletePost}>
                    <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                        <div className="modal-header">
                            <span className="modal-icon">🗑️</span>
                            <h3>Подтверждение удаления</h3>
                        </div>
                        <div className="modal-body">
                            <p>Вы действительно хотите удалить публикацию?</p>
                            <div className="post-preview">
                                <strong>{postToDelete.title}</strong>
                                <small>{postToDelete.date}</small>
                            </div>
                            <p className="warning-text">Это действие невозможно отменить!</p>
                        </div>
                        <div className="modal-footer">
                            <button className="modal-btn cancel" onClick={cancelDeletePost}>
                                ❌ Отмена
                            </button>
                            <button className="modal-btn confirm" onClick={executeDeletePost}>
                                ✅ Да, удалить
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {notification && (
                <div className={`event-notification ${notification.includes('удалена') ? 'delete-notification' : ''}`}>
                    <div className="notification-content">
                        <div className="notification-text">
                            <strong>{notification.includes('удалена') ? 'Запись удалена' : 'Новая запись!'}</strong>
                            <span>{notification}</span>
                        </div>
                        <div className="notification-close" onClick={() => setNotification(null)}>✕</div>
                    </div>
                </div>
            )}

            <div className="feed-container">
                <div className="feed-list">
                    {posts.map((post) => (
                        <div key={post.id} className="feed-post">
                            <div className="post-header">
                                <div className="post-title">{post.title}</div>
                                {isAdmin && (
                                    <button
                                        className="delete-post"
                                        onClick={() => confirmDeletePost(post)}
                                        title="Удалить запись"
                                    >
                                        🗑️
                                    </button>
                                )}
                            </div>
                            <div className="post-date">{post.date}</div>
                            <div className="post-content">{post.content}</div>

                            {post.type === 'audio' && (
                                <div className="post-audio">
                                    {activeAudioPostId === post.id ? (
                                        <div className="inline-audio-player">
                                            <div className="inline-audio-header">
                                                <span className="inline-audio-title">🎵 {post.title}</span>
                                                <button className="inline-stop-btn" onClick={() => stopTrack()}>
                                                    ✕
                                                </button>
                                            </div>
                                            <div className="inline-progress">
                                                <span>{formatTime(getCurrentTimeForPost(post.id))}</span>
                                                <input
                                                    type="range"
                                                    className="inline-seek"
                                                    min="0"
                                                    max={getDurationForPost(post.id) || 100}
                                                    value={getCurrentTimeForPost(post.id)}
                                                    onChange={(e) => handleSeek(e, post.id)}
                                                />
                                                <span>{formatTime(getDurationForPost(post.id))}</span>
                                            </div>
                                            <div className="inline-controls">
                                                <button className="inline-play" onClick={() => handlePlayAudio(post)}>
                                                    {isThisPlaying(post.id) ? '⏸️' : '▶️'}
                                                </button>
                                                <div className="inline-volume">
                                                    <span>🔊</span>
                                                    <input
                                                        type="range"
                                                        min="0"
                                                        max="1"
                                                        step="0.01"
                                                        value={volume}
                                                        onChange={(e) => handleVolumeChangeLocal(e, post.id)}
                                                    />
                                                </div>
                                                <div className="inline-speed">
                                                    <span>⚡</span>
                                                    <select value={playbackRate} onChange={(e) => handleSpeedChangeLocal(e, post.id)}>
                                                        <option value="0.5">0.5x</option>
                                                        <option value="0.75">0.75x</option>
                                                        <option value="1.0">1.0x</option>
                                                        <option value="1.25">1.25x</option>
                                                        <option value="1.5">1.5x</option>
                                                        <option value="2.0">2.0x</option>
                                                    </select>
                                                </div>
                                            </div>
                                        </div>
                                    ) : (
                                        <button className="play-audio-btn" onClick={() => handlePlayAudio(post)}>
                                            ▶️ Слушать аудио
                                        </button>
                                    )}
                                </div>
                            )}

                            {post.type === 'video' && post.url && (
                                <div className="post-video">
                                    <video controls width="100%">
                                        <source src={post.url} type="video/mp4" />
                                    </video>
                                </div>
                            )}
                        </div>
                    ))}
                    <div ref={feedEndRef} className="feed-end-marker" />
                </div>

                {isAdmin && (
                    <div className="admin-section" ref={addFormRef}>
                        <div className="admin-panel">
                            <button className="add-post-btn add-event-style" onClick={handleAddPost}>
                                + Добавить запись
                            </button>
                        </div>
                    </div>
                )}
            </div>

            <ContentModal
                isOpen={showContentModal}
                onClose={() => setShowContentModal(false)}
                onSave={handleSavePost}
            />

            {/* Модальное окно для добавления контента в меню */}
            <ContentModal
                isOpen={showAddModal}
                onClose={() => {
                    setShowAddModal(false);
                    setSelectedMenuId(null);
                }}
                onSave={handleSaveContent}
            />
            </div>
        </div>
    );
};

export default NutritionFeed;
