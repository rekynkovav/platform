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
        // Мгновенно закрываем всплывающее меню
        setHoveredMenu(null);
        setHoveredSubmenu(null);
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

            <ContentModal
                isOpen={showAddModal}
                onClose={() => {
                    setShowAddModal(false);
                    setSelectedMenuId(null);
                }}
                onSave={handleSaveContent}
            />
        </div>
    );
};

export default Dashboard;
