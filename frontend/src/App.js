import React, { useState, useEffect, useRef, Suspense } from 'react';
import { initKeycloak, getUsername, getUserRoles, logout, login as keycloakLogin } from './keycloak';
import NavTabs from './components/Navigation/NavTabs';
import GlobalEventNotification from './components/Notification/GlobalEventNotification';
import NotificationBell from './components/Notification/NotificationBell';
import RoleSwitcher from './components/Admin/RoleSwitcher';
import { AudioPlayerProvider } from './context/AudioPlayerContext';
import ErrorBoundary from './components/common/ErrorBoundary';
import './App.css';

// --- Lazy-loaded page components (code splitting) ---
const MainFeed = React.lazy(() => import('./components/Feed/MainFeed'));
const NutritionFeed = React.lazy(() => import('./components/Feed/NutritionFeed'));
const Dashboard = React.lazy(() => import('./components/Dashboard/Dashboard'));
const EventsHub = React.lazy(() => import('./components/Events/EventsHub'));
const ArchiveList = React.lazy(() => import('./components/Archive/ArchiveList'));
const LiveBroadcastView = React.lazy(() => import('./components/live/LiveBroadcastViewSimple'));
const GeneralChat = React.lazy(() => import('./components/Chat/GeneralChat'));
const NutritionChat = React.lazy(() => import('./components/Chat/NutritionChat'));
const Questions = React.lazy(() => import('./components/Help/Questions'));
const UserProfile = React.lazy(() => import('./components/UserProfile/UserProfile'));
const ScheduledEvents = React.lazy(() => import('./components/Scheduled/ScheduledEvents'));

/**
 * App — root application component.
 *
 * Orchestrates the entire frontend experience:
 * - Initializes Keycloak authentication on mount
 * - Manages top-level navigation tabs and route rendering
 * - Wraps children in an AudioPlayerProvider for global audio state
 * - Renders the login screen when unauthenticated, the main app when authenticated
 *
 * State:
 * @property {boolean} authenticated — whether the user has an active Keycloak session
 * @property {boolean} loading — true while Keycloak initialization is in flight
 * @property {string} activeTab — the currently selected navigation tab ID
 * @property {string[]} userRoles — list of realm roles from the Keycloak token
 * @property {string} username — display name of the authenticated user
 * @property {boolean} showAccessDenied — controls the "access denied" popup
 * @property {{x: number, y: number}} accessDeniedPosition — screen coordinates for the popup
 */
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
            })
            .catch((err) => {
                console.error('Keycloak init failed', err);
                setLoading(false);
            });
    }, []);

    /**
     * Initiates a Keycloak logout and clears local session storage.
     */
    const handleLogout = () => {
        logout();
    };

    /**
     * Triggers a Keycloak login redirect.
     */
    const handleLogin = () => {
        keycloakLogin();
    };

    /**
     * Shows a temporary "access denied" tooltip at the given screen coordinates.
     * Automatically hides after 2 seconds.
     * @param {number} x - screen X position
     * @param {number} y - screen Y position
     */
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

    /**
     * Resets the active tab to "main" when the logo is clicked.
     */
    const handleLogoClick = (e) => {
        setActiveTab('main');
    };

    /**
     * Switches to the "scheduled" tab when a global event notification is clicked.
     */
    const handleEventClick = () => {
        setActiveTab('scheduled');
    };

    /**
     * Updates the active tab in response to navigation bar clicks.
     * @param {string} tabId - the ID of the tab to switch to
     * @param {Event} event - the click event
     */
    const handleTabChange = (tabId, event) => {
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
            case 'chat':
                return <GeneralChat />;
            case 'chat-nutrition':
                return <NutritionChat />;
            case 'help':
                return <Questions />;
            case 'profile':
                return <UserProfile />;
            case 'events':
                return <EventsHub />;
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
                <ErrorBoundary key={activeTab}>
                    <Suspense fallback={<div className="loading-screen">Загрузка...</div>}>
                        {renderContent()}
                    </Suspense>
                </ErrorBoundary>
            </main>

            <GlobalEventNotification onEventClick={handleEventClick} />
        </div>
        </AudioPlayerProvider>
    );
}

export default App;
