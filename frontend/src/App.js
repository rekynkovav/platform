import React, { useState, useEffect, useRef } from 'react';
import { initKeycloak, getUsername, getUserRoles, logout } from './keycloak';
import NavTabs from './components/Navigation/NavTabs';
import MainFeed from './components/Feed/MainFeed';
import Dashboard from './components/Dashboard/Dashboard';
import LiveBroadcastView from './components/live/LiveBroadcastView';
import GeneralChat from './components/Chat/GeneralChat';
import UserProfile from './components/UserProfile/UserProfile';
import { AudioPlayerProvider } from './context/AudioPlayerContext';
import { usePermissions } from './hooks/usePermissions';
import { ROLES, ROLE_LEVELS } from './constants/roles';
import './App.css';

function App() {
    const [authenticated, setAuthenticated] = useState(false);
    const [loading, setLoading] = useState(true);
    const [activeTab, setActiveTab] = useState('main');
    const [userRoles, setUserRoles] = useState([]);
    const [username, setUsername] = useState('User');
    const [showAccessDenied, setShowAccessDenied] = useState(false);
    const [accessDeniedPosition, setAccessDeniedPosition] = useState({ x: 0, y: 0 });
    const accessDeniedTimerRef = useRef(null);

    const { hasLevel } = usePermissions();

    useEffect(() => {
        const initAuth = async (retryCount = 0) => {
            const MAX_RETRIES = 3;
            const RETRY_DELAY = 2000;

            try {
                const auth = await initKeycloak();
                setAuthenticated(auth);
                if (auth) {
                    setUserRoles(getUserRoles());
                    setUsername(getUsername());
                }
            } catch (error) {
                console.error('Auth init failed:', error);

                if (retryCount < MAX_RETRIES) {
                    console.log(`Retrying... (${retryCount + 1}/${MAX_RETRIES})`);
                    setTimeout(() => initAuth(retryCount + 1), RETRY_DELAY);
                } else {
                    console.error('Max retries reached. Auth failed.');
                }
            } finally {
                setLoading(false);
            }
        };
        initAuth();
    }, []);

    const handleLogout = () => {
        logout();
    };

    const handleLogin = () => {
        const keycloakUrl = process.env.REACT_APP_KEYCLOAK_URL || 'http://localhost:8080/auth';
        const loginUrl = `${keycloakUrl}/realms/platform/protocol/openid-connect/auth?client_id=platform-frontend&redirect_uri=${encodeURIComponent(window.location.origin)}&response_type=code&scope=openid`;
        window.location.href = loginUrl;
    };

    const showAccessDeniedMessage = (x, y) => {
        setShowAccessDenied(false);

        if (accessDeniedTimerRef.current) {
            clearTimeout(accessDeniedTimerRef.current);
            accessDeniedTimerRef.current = null;
        }

        setTimeout(() => {
            setAccessDeniedPosition({ x, y });
            setShowAccessDenied(true);

            accessDeniedTimerRef.current = setTimeout(() => {
                setShowAccessDenied(false);
                accessDeniedTimerRef.current = null;
            }, 2000);
        }, 10);
    };

    const handleTabChange = (tabId, event) => {
        const userRole = userRoles.length > 0 ? userRoles[0] : ROLES.USER_0;
        const userLevel = ROLE_LEVELS[userRole] || 1;

        // USER_0 доступны только help и profile
        if (userLevel === 1 && tabId !== 'help' && tabId !== 'profile') {
            if (event) {
                const rect = event.currentTarget.getBoundingClientRect();
                showAccessDeniedMessage(rect.left + rect.width / 2, rect.bottom + 10);
            }
            return;
        }

        setActiveTab(tabId);
    };

    if (loading) {
        return <div className="loading-screen">Loading...</div>;
    }

    if (!authenticated) {
        return (
            <div className="loading-screen">
                <div className="login-container">
                    <div style={{ fontSize: '64px', marginBottom: '20px' }}>🔐</div>
                    <p>Добро пожаловать в Платформу</p>
                    <button className="login-btn" onClick={handleLogin}>
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
            case 'dashboard':
                return <Dashboard />;
            case 'broadcast-live':
                return <LiveBroadcastView />;
            case 'chat':
                return <GeneralChat />;
            case 'help':
                return <div className="help-section">Вопросы и помощь</div>;
            case 'profile':
                return <UserProfile />;
            case 'scheduled':
                return <div className="scheduled-events">События</div>;
            case 'admin':
                return <div className="admin-panel">Админка</div>;
            default:
                return <MainFeed />;
        }
    };

    return (
        <AudioPlayerProvider>
            <div className="app">
                <header className="header">
                    <div className="header-left">
                        <h1 className="header-logo">Platform</h1>
                    </div>
                    <div className="header-right">
                        <span className="username">{username}</span>
                        <button onClick={handleLogout} className="logout-btn">Logout</button>
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
            </div>
        </AudioPlayerProvider>
    );
}

export default App;
