import React from 'react';
import { usePermissions } from '../../hooks/usePermissions';
import './NavTabs.css';

const NavTabs = ({ activeTab, onTabChange }) => {
    const { canViewAdmin, canStartBroadcast, hasLevel, level } = usePermissions();

    // Базовые вкладки для всех
    const baseTabs = [
        { id: 'main', label: 'Главная' },
        { id: 'scheduled', label: 'События' },
        { id: 'chat', label: 'Чат' },
        { id: 'help', label: 'Вопросы' },
        { id: 'profile', label: 'Профиль' }
    ];

    // Дополнительные вкладки по правам
    const extraTabs = [];

    // Вкладка Dashboard — для пользователей с уровнем >= 4 (CONSULTANT и выше)
    if (hasLevel(4)) {
        extraTabs.push({ id: 'dashboard', label: 'Дашборд' });
    }

    if (canStartBroadcast() || hasLevel(4)) {
        extraTabs.push({ id: 'broadcast-live', label: 'Трансляции' });
    }

    if (canViewAdmin()) {
        extraTabs.push({ id: 'admin', label: 'Админка' });
    }

    const tabs = [...baseTabs, ...extraTabs];

    return (
        <nav className="nav-tabs">
            {tabs.map(tab => (
                <button
                    key={tab.id}
                    className={`nav-tab ${activeTab === tab.id ? 'active' : ''}`}
                    onClick={(e) => onTabChange(tab.id, e)}
                >
                    {tab.label}
                </button>
            ))}
        </nav>
    );
};

export default NavTabs;
