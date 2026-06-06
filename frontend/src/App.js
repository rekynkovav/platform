import React, { useState, useEffect, useRef } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { initKeycloak, getUsername, getUserRoles, logout } from './keycloak';
import NavTabs from './components/Navigation/NavTabs';
import MainFeed from './components/Feed/MainFeed';
import Dashboard from './components/Dashboard/Dashboard';
import LiveBroadcastView from './components/live/LiveBroadcastView';
import GeneralChat from './components/Chat/GeneralChat';
import UserProfile from './components/UserProfile/UserProfile';
import { AudioPlayerProvider } from './context/AudioPlayerContext';
import { ROLES, ROLE_LEVELS } from './constants/roles';
import ProtectedRoute from './components/common/ProtectedRoute';
import './App.css';

function App() {
    const [authenticated, setAuthenticated] = useState(false);
    const [loading, setLoading] = useState(true);
    const [userRoles, setUserRoles] = useState([]);
    const [username, setUsername] = useState('User');

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

    return (
        <BrowserRouter>
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

                    <NavTabs />

                    <main className="main-content">
                        <Routes>
                            <Route path="/" element={<MainFeed />} />
                            <Route
                                path="/dashboard"
                                element={
                                    <ProtectedRoute requiredLevel={4}>
                                        <Dashboard />
                                    </ProtectedRoute>
                                }
                            />
                            <Route
                                path="/broadcast-live"
                                element={
                                    <ProtectedRoute requiredLevel={4}>
                                        <LiveBroadcastView />
                                    </ProtectedRoute>
                                }
                            />
                            <Route path="/chat" element={<GeneralChat />} />
                            <Route path="/help" element={<div className="help-section">Вопросы и помощь</div>} />
                            <Route path="/profile" element={<UserProfile />} />
                            <Route
                                path="/scheduled"
                                element={
                                    <ProtectedRoute requiredLevel={4}>
                                        <div className="scheduled-events">События</div>
                                    </ProtectedRoute>
                                }
                            />
                            <Route
                                path="/admin"
                                element={
                                    <ProtectedRoute requiredLevel={6}>
                                        <div className="admin-panel">Админка</div>
                                    </ProtectedRoute>
                                }
                            />
                            <Route path="*" element={<Navigate to="/" replace />} />
                        </Routes>
                    </main>
                </div>
            </AudioPlayerProvider>
        </BrowserRouter>
    );
}

export default App;
