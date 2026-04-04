import React, { useState, useEffect } from 'react';
import Keycloak from 'keycloak-js';
import NavTabs from './components/Navigation/NavTabs';
import Dashboard from './components/Dashboard/Dashboard';
import ScheduledEvents from './components/Scheduled/ScheduledEvents';
import ArchiveList from './components/Archive/ArchiveList';
import GeneralChat from './components/Chat/GeneralChat';
import ContactsList from './components/Contacts/ContactsList';
import HelpCenter from './components/Help/HelpCenter';
import './App.css';

const keycloak = new Keycloak({
    url: 'http://localhost:8080',
    realm: 'platform',
    clientId: 'platform-frontend'
});

function App() {
    const [authenticated, setAuthenticated] = useState(false);
    const [loading, setLoading] = useState(true);
    const [activeTab, setActiveTab] = useState('dashboard');
    const [userRoles, setUserRoles] = useState([]);

    useEffect(() => {
        keycloak.init({ onLoad: 'login-required' })
            .then((auth) => {
                setAuthenticated(auth);
                const roles = keycloak.tokenParsed?.realm_access?.roles || [];
                setUserRoles(roles);
                setLoading(false);
            })
            .catch((err) => {
                console.error('Keycloak init failed', err);
                setLoading(false);
            });
    }, []);

    if (loading) {
        return <div className="loading-screen">Загрузка...</div>;
    }

    const renderContent = () => {
        switch (activeTab) {
            case 'dashboard':
                return <Dashboard userRoles={userRoles} keycloak={keycloak} />;
            case 'scheduled':
                return <ScheduledEvents userRoles={userRoles} keycloak={keycloak} />;
            case 'archive':
                return <ArchiveList />;
            case 'chat':
                return <GeneralChat keycloak={keycloak} />;
            case 'contacts':
                return <ContactsList keycloak={keycloak} />;
            case 'help':
                return <HelpCenter keycloak={keycloak} />;
            default:
                return <Dashboard userRoles={userRoles} keycloak={keycloak} />;
        }
    };

    return (
        <div className="app">
            <header className="header">
                <div className="header-left">
                    <h1 className="header-logo">PLATFORM</h1>
                </div>
                <div className="header-right">
                    <span className="header-username">{keycloak.tokenParsed?.preferred_username || 'User'}</span>
                    <button className="theme-toggle" onClick={() => {}}>🌙</button>
                    <button className="header-logout" onClick={() => keycloak.logout()}>🚪</button>
                </div>
            </header>

            <NavTabs activeTab={activeTab} onTabChange={setActiveTab} />

            <main className="main-content">
                {renderContent()}
            </main>
        </div>
    );
}

export default App;