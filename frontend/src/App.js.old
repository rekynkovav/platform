import React, { useState, useEffect, useRef } from 'react';
import { initKeycloak, getUsername, getUserRoles, logout } from './keycloak';
import NavTabs from './components/Navigation/NavTabs';
import MainFeed from './components/Feed/MainFeed';
import NutritionFeed from './components/Feed/NutritionFeed';
import Dashboard from './components/Dashboard/Dashboard';
import EventsHub from './components/Events/EventsHub';
import ArchiveList from './components/Archive/ArchiveList';
import LiveBroadcastView from './components/live/LiveBroadcastViewSimple';
import GeneralChat from './components/Chat/GeneralChat';
import NutritionChat from './components/Chat/NutritionChat';
import Questions from './components/Help/Questions';
import GlobalEventNotification from './components/Notification/GlobalEventNotification';
import NotificationBell from './components/Notification/NotificationBell';
import RoleSwitcher from './components/Admin/RoleSwitcher';
import UserProfile from './components/UserProfile/UserProfile';
import { AudioPlayerProvider } from './context/AudioPlayerContext';
import './App.css';
import ScheduledEvents from "./components/Scheduled/ScheduledEvents";
import './utils/chatLogViewer'; // Импортируем для доступа к логгеру в консоли

function App() {
    const [authenticated, setAuthenticated] = useState(false);
    const [loading, setLoading] = useState(true);
    const [activeTab, setActiveTab] = useState('main');
    const [userRoles, setUserRoles] = useState([]);
    const [username, setUsername] = useState('User');
    const [showAccessDenied, setShowAccessDenied] = useState(false);
    const [accessDeniedPosition, setAccessDeniedPosition] = useState({ x: 0, y: 0 });
    const initStartedRef = useRef(false);
    const accessDeniedTimerRef = useRef(null);

    const demoRole = localStorage.getItem('demoRole');
    const effectiveRoles = demoRole === 'SUPER_ADMIN' ? ['SUPER_ADMIN', 'USER_2'] : (demoRole === 'USER_0' ? ['USER_0'] : userRoles);

    useEffect(() => {
        if (initStartedRef.current) return;
        initStartedRef.current = true;

        initKeycloak()
            .then((auth) => {
                setAuthenticated(auth);
                if (auth) {
                    setUserRoles(getUserRoles());
                    setUsername(getUsername());
                }
                setLoading(false);

                // Для пользователя 0 устанавливаем вкладку help по умолчанию
                const demoRole = localStorage.getItem('demoRole');
                if (demoRole === 'USER_0') {
                    setActiveTab('help');
                }
            })
            .catch((err) => {
                console.error('Keycloak init failed', err);
                setLoading(false);
            });
    }, []);

    const handleLogout = () => {
        logout();
    };

    const handleLogin = () => {
        const loginUrl = 'http://localhost/auth/realms/platform/protocol/openid-connect/auth?client_id=platform-frontend&redirect_uri=' + encodeURIComponent(window.location.origin) + '&response_type=code&scope=openid';
        window.location.href = loginUrl;
    };

    const showAccessDeniedMessage = (x, y) => {
        // Сначала скрываем, если уже показано
        setShowAccessDenied(false);

        // Очищаем предыдущий таймер если есть
        if (accessDeniedTimerRef.current) {
            clearTimeout(accessDeniedTimerRef.current);
            accessDeniedTimerRef.current = null;
        }

        // Небольшая задержка для сброса анимации
        setTimeout(() => {
            setAccessDeniedPosition({ x, y });
            setShowAccessDenied(true);

            // Устанавливаем новый таймер
            accessDeniedTimerRef.current = setTimeout(() => {
                setShowAccessDenied(false);
                accessDeniedTimerRef.current = null;
            }, 2000);
        }, 10);
    };

    const handleLogoClick = (e) => {
        const demoRole = localStorage.getItem('demoRole');
        if (demoRole === 'USER_0') {
            const rect = e.currentTarget.getBoundingClientRect();
            showAccessDeniedMessage(rect.left + rect.width / 2, rect.bottom + 10);
            return;
        }
        setActiveTab('main');
    };

    const handleEventClick = () => {
        const demoRole = localStorage.getItem('demoRole');
        if (demoRole === 'USER_0') {
            showAccessDeniedMessage(window.innerWidth / 2, window.innerHeight / 2);
            return;
        }
        setActiveTab('scheduled');
    };

    const handleTabChange = (tabId, event) => {
        const demoRole = localStorage.getItem('demoRole');

        // Для пользователя 0 доступны только help и profile
        if (demoRole === 'USER_0' && tabId !== 'help' && tabId !== 'profile') {
            if (event) {
                const rect = event.currentTarget.getBoundingClientRect();
                showAccessDeniedMessage(rect.left + rect.width / 2, rect.bottom + 10);
            } else {
                showAccessDeniedMessage(window.innerWidth / 2, 100);
            }
            return;
        }

        setActiveTab(tabId);
    };

    if (loading) {
        return <div className="loading-screen">Загрузка...</div>;
    }

    if (!authenticated) {
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

    const renderContent = () => {
        switch (activeTab) {
            case 'main':
                return <MainFeed />;
            case 'nutrition':
                return <NutritionFeed />;
            case 'dashboard':
                return <Dashboard />;
            case 'scheduled':
                return <ScheduledEvents />;
            case 'archive':
                return <ArchiveList />;
            case 'broadcast-live':
                return <LiveBroadcastView />;
            case 'chat': {
                const demoRole = localStorage.getItem('demoRole');
                // USER_0 заблокирован в handleTabChange
                if (demoRole === 'USER_0') {
                    return <div className="access-denied-message">Доступ запрещён</div>;
                }
                // Все видят GeneralChat (с переключателем внутри)
                return <GeneralChat />;
            }
            case 'chat-nutrition': {
                const demoRole = localStorage.getItem('demoRole');
                if (demoRole === 'USER_0') {
                    return <div className="access-denied-message">Доступ запрещён</div>;
                }
                return <NutritionChat />;
            }
            case 'help':
                return <Questions />;
            case 'profile':
                return <UserProfile />;
            default:
                return <MainFeed />;
        }
    };

    return (
        <AudioPlayerProvider>
        <div className="app">
            <header className="header">
                <div className="header-spacer"></div>
                <div className="header-left">
                    <div className="header-info" onClick={handleLogoClick} style={{ cursor: 'pointer' }}>
                        <h1 className="header-logo">Путь Сердца</h1>
                        <span className="header-founder">Основатель - Вячеслав Тимошенко</span>
                    </div>
                </div>
                <div className="header-right">
                    <NotificationBell />
                    <RoleSwitcher />
                    <button className="header-logout" onClick={() => setActiveTab('profile')} title="Личный кабинет">👤</button>
                </div>
            </header>

            <NavTabs activeTab={activeTab} onTabChange={handleTabChange} />

            <main className="main-content">
                {showAccessDenied && (
                    <div
                        className="access-denied-message"
                        style={{
                            position: 'fixed',
                            left: `${accessDeniedPosition.x}px`,
                            top: `${accessDeniedPosition.y}px`,
                            transform: 'translateX(-50%)',
                            zIndex: 10000
                        }}
                    >
                        Доступ ограничен
                    </div>
                )}
                {renderContent()}
            </main>

            {/* Глобальное уведомление о событии для всех пользователей */}
            <GlobalEventNotification onEventClick={handleEventClick} />
        </div>
        </AudioPlayerProvider>
    );
}

export default App;
