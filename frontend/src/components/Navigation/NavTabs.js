import React from 'react';
import './NavTabs.css';

const NavTabs = ({ activeTab, onTabChange }) => {
    const tabs = [
        { id: 'main', label: '🏠 Главная' },
        { id: 'dashboard', label: '📊 Навигация' },
        { id: 'scheduled', label: '📅 События' },
        { id: 'archive', label: '📚 Архив' },
        { id: 'chat', label: '💬 Чат' },
        { id: 'help', label: '🆘 Помощь' }
    ];

    return (
        <div className="nav-tabs">
            {tabs.map(tab => (
                <button
                    key={tab.id}
                    data-tab={tab.id}
                    className={`nav-tab ${activeTab === tab.id ? 'active' : ''}`}
                    onClick={() => onTabChange(tab.id)}
                >
                    {tab.label}
                </button>
            ))}
        </div>
    );
};

export default NavTabs;