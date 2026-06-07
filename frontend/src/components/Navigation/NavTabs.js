import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { usePermissions } from '../../hooks/usePermissions';
import './NavTabs.css';

const NavTabs = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const { canViewAdmin, canStartBroadcast, hasLevel } = usePermissions();

    // Базовые вкладки для всех
    const baseTabs = [
        { id: '', label: 'Главная', route: '/' },
        { id: 'scheduled', label: 'События', route: '/scheduled' },
        { id: 'chat', label: 'Чат', route: '/chat' },
        { id: 'help', label: 'Вопросы', route: '/help' },
        { id: 'profile', label: 'Профиль', route: '/profile' }
    ];

    // Дополнительные вкладки по правам
    const extraTabs = [];

    // Вкладка Dashboard — для пользователей с уровнем >= 4 (CONSULTANT и выше)
    if (hasLevel(4)) {
        extraTabs.push({ id: 'dashboard', label: 'Дашборд', route: '/dashboard' });
    }

    // Трансляции — только если можно их запускать (уровень 6 и выше)
    if (canStartBroadcast()) {
        extraTabs.push({ id: 'broadcast-live', label: 'Трансляции', route: '/broadcast-live' });
    }

    if (canViewAdmin()) {
        extraTabs.push({ id: 'admin', label: 'Админка', route: '/admin' });
    }

    const tabs = [...baseTabs, ...extraTabs];

    const isActive = (route) => {
        if (route === '/') return location.pathname === '/';
        return location.pathname.startsWith(route);
    };

    return (
        <nav className="nav-tabs">
            {tabs.map(tab => (
                <button
                    key={tab.id}
                    className={`nav-tab ${isActive(tab.route) ? 'active' : ''}`}
                    onClick={() => navigate(tab.route)}
                >
                    {tab.label}
                </button>
            ))}
        </nav>
    );
};

export default NavTabs;
