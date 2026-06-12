import React, { useState } from 'react';
import './NavTabs.css';

/**
 * NavTabs — horizontal navigation bar with support for split (dropdown) tabs.
 *
 * Renders the main set of navigation tabs for the platform:
 * - "Лента Новостей" (News Feed) — splits into "Питание" / "Новости" on hover
 * - "Трансляции" (Broadcasts)
 * - "События" (Events)
 * - "Навигация" (Dashboard)
 * - "Чат" (Chat) — splits into "Основной" / "Питание" on hover
 * - "Помощь" (Help)
 *
 * @param {object} props
 * @param {string} props.activeTab — currently active tab ID
 * @param {(tabId: string, event: Event) => void} props.onTabChange — callback fired when a tab is clicked
 *
 * @component
 */
const NavTabs = ({ activeTab, onTabChange }) => {
    const [hoveredTab, setHoveredTab] = useState(null);

    const tabs = [
        { id: 'main', label: 'Лента Новостей', hasSplit: true, splitType: 'feed' },
        { id: 'archive', label: 'Трансляции' },
        { id: 'scheduled', label: 'События' },
        { id: 'dashboard', label: 'Навигация' },
        { id: 'chat', label: 'Чат', hasSplit: true, splitType: 'chat' },
        { id: 'help', label: 'Помощь' }
    ];

    /**
     * Renders the split (sub-tab) UI for tabs that have nested options.
     * Currently supports two split types:
     * - 'feed': renders "Питание" and "Новости" sub-tabs
     * - 'chat': renders "Основной" and "Питание" sub-tabs
     * @param {object} tab - the tab configuration object with a splitType field
     * @returns {JSX.Element|null} the split tab UI, or null if no split
     */
    const renderSplitTab = (tab) => {
        if (tab.splitType === 'feed') {
            return (
                <div className="nav-tab-split">
                    <button
                        className={`nav-tab-half nutrition ${activeTab === 'nutrition' ? 'active' : ''}`}
                        onClick={(e) => onTabChange('nutrition', e)}
                    >
                        Питание
                    </button>
                    <button
                        className={`nav-tab-half news ${activeTab === 'main' ? 'active' : ''}`}
                        onClick={(e) => onTabChange('main', e)}
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
                        onClick={(e) => onTabChange('chat', e)}
                    >
                        Основной
                    </button>
                    <button
                        className={`nav-tab-half chat-nutrition ${activeTab === 'chat-nutrition' ? 'active' : ''}`}
                        onClick={(e) => onTabChange('chat-nutrition', e)}
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
                         (tab.id === 'chat' && activeTab === 'chat-nutrition')) ? 'active' : ''
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
                                (tab.id === 'chat' && activeTab === 'chat-nutrition') ? 'active' : ''
                            }`}
                            onClick={(e) => onTabChange(tab.id, e)}
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
