import React, { useState, useEffect, useRef, Suspense, useCallback } from 'react';
import { Routes, Route, useNavigate, Navigate } from 'react-router-dom';
import { initKeycloak, getUsername, getUserRoles, logout, login as keycloakLogin } from './keycloak';
import NavTabs from './components/Navigation/NavTabs';
import GlobalEventNotification from './components/Notification/GlobalEventNotification';
import NotificationBell from './components/Notification/NotificationBell';
import RoleSwitcher from './components/Admin/RoleSwitcher';
import { AudioPlayerProvider } from './context/AudioPlayerContext';
import { LiveSessionProvider } from './context/live/LiveSessionContext';
import ErrorBoundary from './components/common/ErrorBoundary';
import './App.css';

// --- Lazy-loaded page components (code splitting) ---
const MainFeed = React.lazy(() => import('./components/Feed/MainFeed'));
const NutritionFeed = React.lazy(() => import('./components/Feed/NutritionFeed'));
const Dashboard = React.lazy(() => import('./components/Dashboard/Dashboard'));
const EventsHub = React.lazy(() => import('./components/Events/EventsHub'));
const ArchiveList = React.lazy(() => import('./components/Archive/ArchiveList'));
const LivePage = React.lazy(() => import('./components/live/LivePage'));
const GeneralChat = React.lazy(() => import('./components/Chat/GeneralChat'));
const GeneralChatUser2 = React.lazy(() => import('./components/Chat/GeneralChat').then(m => ({ default: () => <m.default roomSlug="user2" /> })));
const NutritionChat = React.lazy(() => import('./components/Chat/NutritionChat'));
const Questions = React.lazy(() => import('./components/Help/Questions'));
const UserProfile = React.lazy(() => import('./components/UserProfile/UserProfile'));
const ScheduledEvents = React.lazy(() => import('./components/Scheduled/ScheduledEvents'));

// Preload all lazy components by triggering their import promises.
// React.lazy() wraps a dynamic import — calling the same import() again
// returns the cached module, so the component is ready before the user
// navigates to it.
const _preloads = [
    () => import('./components/Feed/MainFeed'),
    () => import('./components/Feed/NutritionFeed'),
    () => import('./components/Dashboard/Dashboard'),
    () => import('./components/Events/EventsHub'),
    () => import('./components/Archive/ArchiveList'),
    () => import('./components/live/LivePage'),
    () => import('./components/Chat/GeneralChat'),
    () => import('./components/Chat/NutritionChat'),
    () => import('./components/Help/Questions'),
    () => import('./components/UserProfile/UserProfile'),
    () => import('./components/Scheduled/ScheduledEvents'),
];
const preloadComponents = () => _preloads.forEach(fn => fn());

/**
 * App — root application component.
 *
 * Orchestrates the entire frontend experience:
 * - Initializes Keycloak authentication on mount
 * - Uses React Router for URL-based navigation
 * - Wraps children in an AudioPlayerProvider for global audio state
 * - Renders the login screen when unauthenticated, the main app when authenticated
 *
 * Routes:
 * /              → MainFeed
 * /nutrition     → NutritionFeed
 * /dashboard     → Dashboard
 * /events        → EventsHub
 * /archive       → ArchiveList
 * /live          → LiveBroadcastView
 * /chat          → GeneralChat
 * /chat/nutrition → NutritionChat
 * /help          → Questions
 * /profile       → UserProfile
 * /scheduled     → ScheduledEvents
 */
function App() {
    const [authenticated, setAuthenticated] = useState(false);
    const [loading, setLoading] = useState(true);
    const [userRoles, setUserRoles] = useState([]);
    const [username, setUsername] = useState('User');
    const [showAccessDenied, setShowAccessDenied] = useState(false);
    const [accessDeniedPosition, setAccessDeniedPosition] = useState({ x: 0, y: 0 });
    const initStartedRef = useRef(false);
    const accessDeniedTimerRef = useRef(null);
    const navigate = useNavigate();

    useEffect(() => {
        if (initStartedRef.current) return;
        initStartedRef.current = true;

        initKeycloak()
            .then((auth) => {
                setAuthenticated(auth);
                if (auth) {
                    setUserRoles(getUserRoles());
                    setUsername(getUsername());
                    // Preload all lazy components to prevent flash on tab switch
                    preloadComponents();
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
     * Navigates to home page when the logo is clicked.
     */
    const handleLogoClick = () => {
        navigate('/');
    };

    /**
     * Navigates to scheduled events when a global event notification is clicked.
     */
    const handleEventClick = () => {
        navigate('/scheduled');
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

    return (
        <AudioPlayerProvider>
        <LiveSessionProvider>
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
                    <button className="header-profile-btn" onClick={() => navigate('/profile')} title="Личный кабинет">
                        <span className="header-profile-icon">👤</span>
                        <span className="header-profile-label">Кабинет</span>
                    </button>
                </div>
            </header>

            <NavTabs />

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
                <ErrorBoundary>
                    <Suspense fallback={<div className="tab-loading"><div className="tab-spinner" /></div>}>
                        <Routes>
                            <Route path="/" element={<MainFeed />} />
                            <Route path="/nutrition" element={<NutritionFeed />} />
                            <Route path="/dashboard" element={<Dashboard />} />
                            <Route path="/events" element={<EventsHub />} />
                            <Route path="/archive" element={<ArchiveList />} />
                            <Route path="/live" element={<LivePage />} />
                            <Route path="/chat" element={<GeneralChat />} />
                            <Route path="/chat/user2" element={<GeneralChatUser2 />} />
                            <Route path="/chat/nutrition" element={<NutritionChat />} />
                            <Route path="/help" element={<Questions />} />
                            <Route path="/profile" element={<UserProfile />} />
                            <Route path="/scheduled" element={<ScheduledEvents />} />
                            <Route path="*" element={<Navigate to="/" replace />} />
                        </Routes>
                    </Suspense>
                </ErrorBoundary>
            </main>

            <GlobalEventNotification onEventClick={handleEventClick} />
        </div>
        </LiveSessionProvider>
        </AudioPlayerProvider>
    );
}

export default App;
