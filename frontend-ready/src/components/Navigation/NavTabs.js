// components/Navigation/NavTabs.js
import React from 'react';
import './NavTabs.css';

const tabs = [
    { id: 'dashboard', label: '📅 ЗАПЛАНИРОВАНО', icon: '📅' },
    { id: 'archive', label: '📁 АРХИВ', icon: '📁' },
    { id: 'chat', label: '💬 ЧАТ', icon: '💬' },
    { id: 'contacts', label: '👥 КОНТАКТЫ', icon: '👥' },
    { id: 'help', label: '🆘 ПОМОЩЬ!', icon: '🆘' }
];

const NavTabs = ({ activeTab, onTabChange }) => {
    return (
        <nav className="nav-tabs">
            {tabs.map(tab => (
                <button
                    key={tab.id}
                    className={`nav-tab ${activeTab === tab.id ? 'active' : ''}`}
                    onClick={() => onTabChange(tab.id)}
                >
                    <span className="nav-tab-icon">{tab.icon}</span>
                    <span className="nav-tab-label">{tab.label}</span>
                </button>
            ))}
        </nav>
    );
};

export default NavTabs;