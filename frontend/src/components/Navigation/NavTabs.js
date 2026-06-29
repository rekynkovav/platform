import React, { useState, useRef, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import './NavTabs.css';

const PATH_TAB_MAP = {
    '/': 'main',
    '/nutrition': 'nutrition',
    '/dashboard': 'dashboard',
    '/scheduled': 'scheduled',
    '/archive': 'archive',
    '/live': 'broadcast-live',
    '/chat': 'chat',
    '/chat/nutrition': 'chat-nutrition',
    '/help': 'help',
    '/profile': 'profile',
    '/events': 'events',
};

const NavTabs = () => {
    const [hoveredTab, setHoveredTab] = useState(null);
    const navigate = useNavigate();
    const location = useLocation();
    const navRef = useRef(null);
    const activeTab = PATH_TAB_MAP[location.pathname] || 'main';

    const tabs = [
        { id: 'main', label: 'Лента Новостей', hasSplit: true, splitType: 'feed' },
        { id: 'archive', label: 'Трансляции' },
        { id: 'scheduled', label: 'События' },
        { id: 'dashboard', label: 'Навигация' },
        { id: 'chat', label: 'Чат', hasSplit: true, splitType: 'chat' },
        { id: 'help', label: 'Помощь' },
    ];

    useEffect(() => {
        const handleClickOutside = (e) => {
            if (navRef.current && !navRef.current.contains(e.target)) {
                setHoveredTab(null);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const isTabActive = (tab) => {
        return activeTab === tab.id ||
            (tab.id === 'main' && activeTab === 'nutrition') ||
            (tab.id === 'chat' && activeTab === 'chat-nutrition');
    };

    const renderSplitTab = (tab) => {
        if (tab.splitType === 'feed') {
            return (
                <div className="nav-tab-split">
                    <button
                        className={`nav-tab-half nutrition ${activeTab === 'nutrition' ? 'active' : ''}`}
                        onClick={(e) => { e.stopPropagation(); navigate('/nutrition'); setHoveredTab(null); }}
                    >
                        Питание
                    </button>
                    <button
                        className={`nav-tab-half news ${activeTab === 'main' ? 'active' : ''}`}
                        onClick={(e) => { e.stopPropagation(); navigate('/'); setHoveredTab(null); }}
                    >
                        Новости
                    </button>
                </div>
            );
        } else if (tab.splitType === 'chat') {
            return (
                <div className="nav-tab-split">
                    <button
                        className={`nav-tab-half chat-user1 ${activeTab === 'chat' ? 'active' : ''}`}
                        onClick={(e) => { e.stopPropagation(); navigate('/chat'); setHoveredTab(null); }}
                    >
                        Основной
                    </button>
                    <button
                        className={`nav-tab-half chat-nutrition ${activeTab === 'chat-nutrition' ? 'active' : ''}`}
                        onClick={(e) => { e.stopPropagation(); navigate('/chat/nutrition'); setHoveredTab(null); }}
                    >
                        Питание
                    </button>
                </div>
            );
        }
    };

    return (
        <div className="nav-tabs" ref={navRef}>
            {tabs.map(tab => (
                <div
                    key={tab.id}
                    className={`nav-tab-wrapper ${isTabActive(tab) ? 'active' : ''}`}
                    onMouseEnter={() => tab.hasSplit && setHoveredTab(tab.id)}
                    onMouseLeave={() => setHoveredTab(null)}
                >
                    {tab.hasSplit && hoveredTab === tab.id ? (
                        renderSplitTab(tab)
                    ) : (
                        <button
                            className={`nav-tab ${isTabActive(tab) ? 'active' : ''}`}
                            onClick={() => navigate(tab.id === 'main' ? '/' : '/' + tab.id)}
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
