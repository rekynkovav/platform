import React, { useState } from 'react';
import NavTabs from './components/Navigation/NavTabs';
import ScheduledEvents from './components/Scheduled/ScheduledEvents';
import ArchiveList from './components/Archive/ArchiveList';
import GeneralChat from './components/Chat/GeneralChat';
import ContactsList from './components/Contacts/ContactsList';
import HelpCenter from './components/Help/HelpCenter';
import './App.css';
import './components/Dashboard/Dashboard.css'; // Для мокового дашборда

// Заглушка вместо Keycloak (без сервера авторизации)
const keycloakMock = {
    authenticated: true,
    tokenParsed: {
        preferred_username: 'DemoUser',
        realm_access: { roles: ['user'] }
    },
    logout: () => { window.location.reload(); }
};

// Mock данные для Dashboard (когда нет API)
const mockNextEvent = {
    id: 1,
    title: 'Cuba & Eguinembé - Путь сердца',
    date: '2026-05-15T18:00:00',
    location: 'Москва',
    description: 'Танцевальный вечер'
};

const mockPosts = [
    {
        id: 1,
        title: 'Добро пожаловать на платформу!',
        content: 'Это демо-версия. Запустите бэкенд для полной работы.',
        author: 'System',
        createdAt: '2026-05-01T10:00:00Z',
        reactions: { like: 5, heart: 3 }
    }
];

// Простой Dashboard без API
const DashboardMock = ({ userRoles, keycloak }) => {
    const isAdmin = userRoles.includes('admin') || userRoles.includes('moderator');

    return (
        <div className="dashboard">
            <div className="dashboard-section event-section">
                <div className="event-card">
                    <div className="event-date">[DATE] 15 мая 2026</div>
                    <h3 className="event-title">{mockNextEvent.title}</h3>
                    <p className="event-location">[LOC] {mockNextEvent.location}</p>
                    <p className="event-description">{mockNextEvent.description}</p>
                </div>
            </div>

            {isAdmin && (
                <div className="dashboard-section admin-section">
                    <div className="admin-post-creator">
                        <h3>[NEW POST] Создать пост (демо)</h3>
                        <p><em>Запустите бэкенд для редактирования</em></p>
                    </div>
                </div>
            )}

            <div className="dashboard-section feed-section">
                <h2 className="section-title">[NEWS] ЛЕНТА НОВОСТЕЙ</h2>
                {mockPosts.map(post => (
                    <div key={post.id} className="post-card">
                        <h3 className="post-title">{post.title}</h3>
                        <p className="post-content">{post.content}</p>
                        <div className="post-meta">
                            <span>By {post.author}</span>
                            <span>Likes: {post.reactions.like}</span>
                            <span>Hearts: {post.reactions.heart}</span>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
};

function App() {
    const [activeTab, setActiveTab] = useState('dashboard');
    const userRoles = ['user'];

    const renderContent = () => {
        switch (activeTab) {
            case 'dashboard':
                return <DashboardMock userRoles={userRoles} keycloak={keycloakMock} />;
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
                    <button className="theme-toggle" onClick={() => {}}>DM</button>
                    <button className="header-logout" onClick={() => keycloakMock.logout()}>LOGOUT</button>
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