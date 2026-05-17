import React, { useState } from 'react';
import './NavTabs.css';

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
            const demoRole = localStorage.getItem('demoRole');
            const canSwitchChats = demoRole !== 'USER_0';

            // Все видят Основной + Питание
            return (
                <div className="nav-tab-split chat-split">
                    <button
                        className={`nav-tab-half chat-main ${activeTab === 'chat' ? 'active' : ''}`}
                        onClick={(e) => onTabChange('chat', e)}
                    >
                        Основной
                    </button>
                    {canSwitchChats && (
                        <button
                            className={`nav-tab-half chat-nutrition ${activeTab === 'chat-nutrition' ? 'active' : ''}`}
                            onClick={(e) => onTabChange('chat-nutrition', e)}
                        >
                            Питание
                        </button>
                    )}
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
