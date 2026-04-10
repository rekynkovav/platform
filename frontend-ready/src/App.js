import React, { useState } from 'react';
import NavTabs from './components/Navigation/NavTabs';
import Dashboard from './components/Dashboard/Dashboard';
import ScheduledEvents from './components/Scheduled/ScheduledEvents';
import ArchiveList from './components/Archive/ArchiveList';
import GeneralChat from './components/Chat/GeneralChat';
import ContactsList from './components/Contacts/ContactsList';
import HelpCenter from './components/Help/HelpCenter';
import './App.css';

// Заглушка вместо Keycloak (без сервера авторизации)
const keycloakMock = {
    authenticated: true,
    tokenParsed: {
        preferred_username: 'DemoUser',
        realm_access: { roles: ['user'] }
    },
    logout: () => { window.location.reload(); }
};

function App() {
    const [activeTab, setActiveTab] = useState('dashboard');
    const userRoles = ['user'];

    const renderContent = () => {
        switch (activeTab) {
            case 'dashboard':
                return <Dashboard userRoles={userRoles} keycloak={keycloakMock} />;
            case 'scheduled':
                return <ScheduledEvents userRoles={userRoles} keycloak={keycloakMock} />;
            case 'archive':
                return <ArchiveList />;
            case 'chat':
                return <GeneralChat keycloak={keycloakMock} />;
            case 'contacts':
                return <ContactsList keycloak={keycloakMock} />;
            case 'help':
                return <HelpCenter keycloak={keycloakMock} />;
            default:
                return <Dashboard userRoles={userRoles} keycloak={keycloakMock} />;
        }
    };

    return (
        <div className="app">
            <header className="header">
                <div className="header-left">
                    <h1 className="header-logo">PLATFORM</h1>
                </div>
                <div className="header-right">
                    <span className="header-username">{keycloakMock.tokenParsed?.preferred_username || 'User'}</span>
                    <button className="theme-toggle" onClick={() => {}}>🌙</button>
                    <button className="header-logout" onClick={() => keycloakMock.logout()}>🚪</button>
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