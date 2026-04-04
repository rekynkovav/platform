import React, { useState, useEffect, useRef } from 'react';
import { initKeycloak, getUsername, getUserRoles, logout } from './keycloak';
import NavTabs from './components/Navigation/NavTabs';
import MainFeed from './components/Feed/MainFeed';
import Dashboard from './components/Dashboard/Dashboard';
import EventsHub from './components/Events/EventsHub';
import ArchiveList from './components/Archive/ArchiveList';
import GeneralChat from './components/Chat/GeneralChat';
import Questions from './components/Help/Questions';
import GlobalEventNotification from './components/Notification/GlobalEventNotification';
import RoleSwitcher from './components/Admin/RoleSwitcher';
import { useTheme } from './hooks/useTheme';
import './App.css';
import ScheduledEvents from "./components/Scheduled/ScheduledEvents";

// Глобальные переменные вне компонента
let globalInitStarted = false;
let globalInitPromise = null;
let globalAuthResult = null;
let globalAuthCompleted = false;

function App() {
    const [authenticated, setAuthenticated] = useState(globalAuthResult !== null ? globalAuthResult : false);
    const [loading, setLoading] = useState(!globalAuthCompleted);
    const [activeTab, setActiveTab] = useState('main');
    const [userRoles, setUserRoles] = useState([]);
    const [username, setUsername] = useState('User');
    const isMounted = useRef(true);
    const forceUpdate = useState(0)[1];
    const { theme, toggleTheme } = useTheme();

    // Проверяем демо-роль из localStorage (приоритет у переключателя)
    const demoRole = localStorage.getItem('demoRole');
    const effectiveRoles = demoRole === 'admin' ? ['admin', 'user'] : (demoRole === 'user' ? ['user'] : userRoles);

    useEffect(() => {
        console.log('🔧 useEffect started, globalInitStarted:', globalInitStarted);
        console.log('🔧 globalAuthCompleted:', globalAuthCompleted, 'globalAuthResult:', globalAuthResult);

        if (globalAuthCompleted) {
            console.log('🔧 Already completed, setting loading false');
            setLoading(false);
            setAuthenticated(globalAuthResult);
            if (globalAuthResult) {
                setUserRoles(getUserRoles());
                setUsername(getUsername());
            }
            forceUpdate(prev => prev + 1);
            return;
        }

        if (!globalInitStarted) {
            globalInitStarted = true;
            globalInitPromise = initKeycloak();
            console.log('🔧 initPromise created');
        }

        globalInitPromise
            .then((auth) => {
                console.log('🔧 Auth result:', auth);
                globalAuthResult = auth;
                globalAuthCompleted = true;

                if (isMounted.current) {
                    setAuthenticated(auth);
                    if (auth) {
                        const roles = getUserRoles();
                        const user = getUsername();
                        console.log('🔧 User roles:', roles);
                        console.log('🔧 Username:', user);
                        setUserRoles(roles);
                        setUsername(user);
                    }
                    console.log('🔧 Setting loading to false');
                    setLoading(false);
                    forceUpdate(prev => prev + 1);
                } else {
                    console.log('🔧 Component unmounted but saving globally');
                }
            })
            .catch((err) => {
                console.error('Keycloak init failed', err);
                globalAuthResult = false;
                globalAuthCompleted = true;
                if (isMounted.current) {
                    setLoading(false);
                    forceUpdate(prev => prev + 1);
                }
            });

        return () => {
            console.log('🔧 Cleanup: unmounting');
            isMounted.current = false;
        };
    }, []);

    const handleLogout = () => {
        globalInitStarted = false;
        globalInitPromise = null;
        globalAuthResult = null;
        globalAuthCompleted = false;
        logout();
    };

    const handleLogin = () => {
        const loginUrl = 'http://localhost/auth/realms/platform/protocol/openid-connect/auth?client_id=platform-frontend&redirect_uri=' + encodeURIComponent(window.location.origin) + '&response_type=code&scope=openid';
        window.location.href = loginUrl;
    };

    const handleLogoClick = () => {
        setActiveTab('main');
    };

    const handleEventClick = () => {
        setActiveTab('scheduled');
    };

    console.log('🔧 Rendering App, loading:', loading, 'authenticated:', authenticated);
    console.log('🔧 Effective roles:', effectiveRoles);

    if (loading) {
        console.log('🔧 Showing loading screen');
        return <div className="loading-screen">Загрузка...</div>;
    }

    if (!authenticated) {
        console.log('🔧 Showing login button');
        return (
            <div className="loading-screen">
                <div style={{ textAlign: 'center' }}>
                    <p style={{ marginBottom: '20px', fontSize: '18px' }}>🔐 Пожалуйста, войдите в систему</p>
                    <button
                        onClick={handleLogin}
                        style={{
                            padding: '12px 32px',
                            background: '#0d6efd',
                            color: 'white',
                            border: 'none',
                            borderRadius: '8px',
                            cursor: 'pointer',
                            fontSize: '16px',
                            fontWeight: '500'
                        }}
                    >
                        Войти через Keycloak
                    </button>
                </div>
            </div>
        );
    }

    console.log('🔧 Showing main app interface');

    const renderContent = () => {
        console.log('🔧 Rendering tab:', activeTab);
        switch (activeTab) {
            case 'main':
                return <MainFeed />;
            case 'dashboard':
                return <EventsHub />;
            case 'scheduled':
                return <ScheduledEvents />;
            case 'archive':
                return <ArchiveList />;
            case 'chat':
                return <GeneralChat />;
            case 'help':
                return <Questions />;
            default:
                return <MainFeed />;
        }
    };

    return (
        <div className="app">
            <header className="header">
                <div className="header-left">
                    <div className="header-info" onClick={handleLogoClick} style={{ cursor: 'pointer' }}>
                        <h1 className="header-logo">Глобальный Проект "Путь Сердца"</h1>
                        <span className="header-founder">Основатель - Вячеслав Тимошенко</span>
                    </div>
                </div>
                <div className="header-right">
                    <span className="header-username">{username}</span>
                    <RoleSwitcher />
                    <button className="theme-toggle" onClick={toggleTheme}>
                        {theme === 'light' ? '🌙' : '☀️'}
                    </button>
                    <button className="header-logout" onClick={handleLogout}>🚪</button>
                </div>
            </header>

            <NavTabs activeTab={activeTab} onTabChange={setActiveTab} />

            <main className="main-content">
                {renderContent()}
            </main>

            {/* Глобальное уведомление о событии для всех пользователей */}
            <GlobalEventNotification onEventClick={handleEventClick} />
        </div>
    );
}

export default App;