import React, { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import './NavTabs.css';

/**
 * Path-to-tab mapping for determining which tab is active.
 */
const PATH_TAB_MAP = {
    '/': 'main',
    '/nutrition': 'nutrition',
    '/dashboard': 'dashboard',
    '/scheduled': 'scheduled',
    '/archive': 'archive',
    '/live': 'broadcast-live',
    '/chat': 'chat',
    '/chat/nutrition': 'chat-nutrition',
    '/chat/user2': 'chat-user2',
    '/help': 'help',
    '/profile': 'profile',
    '/events': 'events',
};

/**
 * NavTabs — horizontal navigation bar with support for split (dropdown) tabs.
 *
 * Uses React Router (useNavigate + useLocation) for URL-based navigation.
 * Renders the main set of navigation tabs for the platform:
 * - "Лента Новостей" (News Feed) — splits into "Питание" / "Новости" on hover
 * - "Трансляции" (Broadcasts)
 * - "События" (Events)
 * - "Навигация" (Dashboard)
 * - "Чат" (Chat) — splits into "Основной" / "Питание" on hover
 * - "Помощь" (Help)
 *
 * @component
 */
const NavTabs = () => {
    const [hoveredTab, setHoveredTab] = useState(null);
    const navigate = useNavigate();
    const location = useLocation();

    // Determine active tab from current URL path
    const activeTab = PATH_TAB_MAP[location.pathname] || 'main';

    const tabs = [
        { id: 'main', label: 'Лента Новостей', hasSplit: true, splitType: 'feed' },
        { id: 'archive', label: 'Трансляции' },
        { id: 'scheduled', label: 'События' },
        { id: 'dashboard', label: 'Навигация' },
        { id: 'chat', label: 'Чат', hasSplit: true, splitType: 'chat' },
        { id: 'help', label: 'Помощь' },
    ];

    /**
     * Handles tab click — navigates to the corresponding route.
     * @param {string} tabId - the tab ID to navigate to
     */
    const handleTabClick = (tabId) => {
        // Маппинг tabId → URL путь (некоторые ID не совпадают с маршрутами)
        const pathMap = {
            'chat-nutrition': '/chat/nutrition',
            'chat-user2': '/chat/user2',
        };
        const path = pathMap[tabId] || ('/' + (tabId === 'main' ? '' : tabId));
        navigate(path);
    };

    /**
     * Renders the split (sub-tab) UI for tabs that have nested options.
     */
    const renderSplitTab = (tab) => {
        if (tab.splitType === 'feed') {
            return (
                <div className="nav-tab-split">
                    <button
                        className={`nav-tab-half nutrition ${activeTab === 'nutrition' ? 'active' : ''}`}
                        onClick={() => handleTabClick('nutrition')}
                    >
                        Питание
                    </button>
                    <button
                        className={`nav-tab-half news ${activeTab === 'main' ? 'active' : ''}`}
                        onClick={() => handleTabClick('main')}
                    >
                        Новости
                    </button>
                </div>
            );
        } else if (tab.splitType === 'chat') {
            return (
                <div className="nav-tab-split chat-split">
                    <button
                        className={`nav-tab-half chat-main ${activeTab === 'chat' ? 'active' : ''}`}
                        onClick={() => handleTabClick('chat')}
                    >
                        Основной
                    </button>
                    <button
                        className={`nav-tab-half chat-user2 ${activeTab === 'chat-user2' ? 'active' : ''}`}
                        onClick={() => handleTabClick('chat-user2')}
                    >
                        Продвинутый
                    </button>
                    <button
                        className={`nav-tab-half chat-nutrition ${activeTab === 'chat-nutrition' ? 'active' : ''}`}
                        onClick={() => handleTabClick('chat-nutrition')}
                    >
                        Питание
                    </button>
                </div>
            );
        }
    };

    return (
        <div className="nav-tabs">
            {tabs.map(tab => (
                <div
                    key={tab.id}
                    className={`nav-tab-wrapper ${
                        (activeTab === tab.id ||
                         (tab.id === 'main' && activeTab === 'nutrition') ||
                         (tab.id === 'chat' && (activeTab === 'chat-nutrition' || activeTab === 'chat-user2'))) ? 'active' : ''
                    }`}
                    onMouseEnter={() => tab.hasSplit && setHoveredTab(tab.id)}
                    onMouseLeave={() => setHoveredTab(null)}
                >
                    {tab.hasSplit && hoveredTab === tab.id ? (
                        renderSplitTab(tab)
                    ) : (
                        <button
                            data-tab={tab.id}
                            className={`nav-tab ${
                                activeTab === tab.id ||
                                (tab.id === 'main' && activeTab === 'nutrition') ||
                                (tab.id === 'chat' && (activeTab === 'chat-nutrition' || activeTab === 'chat-user2')) ? 'active' : ''
                                }`}
                            onClick={() => handleTabClick(tab.id)}
                        >
                            {tab.label}
                        </button>
                    )}
                </div>
            ))}
        </div>
    );
};

export default NavTabs;
