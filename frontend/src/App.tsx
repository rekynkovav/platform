import React, { useState, useEffect, useRef } from 'react';
import { initKeycloak, getUsername, getUserRoles, logout, login as keycloakLogin } from './keycloak';
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

interface AccessDeniedPosition {
  x: number;
  y: number;
}

function App() {
  const [authenticated, setAuthenticated] = useState(false);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('main');
  const [userRoles, setUserRoles] = useState<string[]>([]);
  const [username, setUsername] = useState('User');
  const [showAccessDenied, setShowAccessDenied] = useState(false);
  const [accessDeniedPosition, setAccessDeniedPosition] = useState<AccessDeniedPosition>({ x: 0, y: 0 });
  const [authError, setAuthError] = useState<string | null>(null);
  const accessDeniedTimerRef = useRef<NodeJS.Timeout | null>(null);

  const { hasLevel } = usePermissions();

  useEffect(() => {
    let cancelled = false;
    const initAuth = async (retryCount = 0) => {
      const MAX_RETRIES = 3;
      const RETRY_DELAY = 2000;

      try {
        const auth = await initKeycloak();
        if (!cancelled) {
          setAuthenticated(auth);
          if (auth) {
            setUserRoles(getUserRoles());
            setUsername(getUsername());
          }
        }
      } catch (error) {
        console.error('Auth init failed:', error);

        if (!cancelled && retryCount < MAX_RETRIES) {
          console.log(`Retrying... (${retryCount + 1}/${MAX_RETRIES})`);
          setTimeout(() => initAuth(retryCount + 1), RETRY_DELAY);
        } else if (!cancelled) {
          console.error('Max retries reached. Auth failed.');
          setAuthError('Не удалось подключиться к серверу аутентификации. Проверьте подключение к интернету.');
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };
    initAuth();
    return () => { cancelled = true; };
  }, []);

  const handleLogout = (): void => {
    logout();
  };

  const handleLogin = (): void => {
    keycloakLogin();
  };

  const showAccessDeniedMessage = (x: number, y: number): void => {
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

  const handleTabChange = (tabId: string, event?: React.MouseEvent<HTMLElement>): void => {
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
          {authError && (
            <div className="auth-error-message">
              <p style={{ color: '#dc3545', marginBottom: '10px' }}>{authError}</p>
              <p style={{ fontSize: '14px', color: '#6c757d' }}>
                Проверьте, что сервер Keycloak запущен по адресу: {process.env.REACT_APP_KEYCLOAK_URL || 'http://localhost:8080/auth'}
              </p>
            </div>
          )}
          <button className="login-btn" onClick={handleLogin}>
            Войти через Keycloak
          </button>
          <button
            className="retry-btn"
            onClick={() => window.location.reload()}
            style={{ marginTop: '10px', background: '#6c757d' }}
          >
            Попробовать снова
          </button>
        </div>
      </div>
    );
  }

  const renderContent = (): JSX.Element => {
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