import React, { useState, useEffect } from 'react';
import { useAudioPlayer } from './contexts/AudioPlayerContext';
import RoleSwitcher from './components/Admin/RoleSwitcher';
import NavTabs from './components/Navigation/NavTabs';
import Dashboard from './components/Dashboard/Dashboard';
import ScheduledEvents from './components/Scheduled/ScheduledEvents';
import ArchiveList from './components/Archive/ArchiveList';
import GeneralChat from './components/Chat/GeneralChat';
import NutritionChat from './components/Chat/NutritionChat';
import MainFeed from './components/Feed/MainFeed';
import NutritionFeed from './components/Feed/NutritionFeed';
import UserProfile from './components/UserProfile/UserProfile';
import Questions from './components/Help/Questions';
import HelpChat from './components/Help/HelpChat';
import EventsHub from './components/Events/EventsHub';
import NotificationBell from './components/Notification/NotificationBell';
import GlobalEventNotification from './components/Notification/GlobalEventNotification';
import ContactsList from './components/Contacts/ContactsList';
import './App.css';

/**
 * =============================================
 * App.js — ФИНАЛЬНАЯ ВЕРСИЯ (ПОРЦИЯ 100)
 * Полная чистка и унификация всего фронтенда
 * Проект «Путь Сердца» полностью готов к запуску
 * =============================================
 */

const App = () => {
    const [activeTab, setActiveTab] = useState('main');
    const { isMinimized } = useAudioPlayer();
    const demoRole = localStorage.getItem('demoRole') || 'USER_0';

    const handleTabChange = (tab) => {
        setActiveTab(tab);
    };

    const renderContent = () => {
        switch (activeTab) {
            case 'main': return <MainFeed />;
            case 'nutrition': return <NutritionFeed />;
            case 'dashboard': return <Dashboard />;
            case 'scheduled': return <ScheduledEvents />;
            case 'archive': return <ArchiveList />;
            case 'chat': return <GeneralChat />;
            case 'help': return <Questions />;
            case 'profile': return <UserProfile />;
            default: return <MainFeed />;
        }
    };

    return (
        <div className="app">
            {/* Header */}
            <header className="header">
                <div className="header-left">
                    <div className="header-info">
                        <h1 className="header-logo">Путь Сердца</h1>
                        <p className="header-founder">Вячеслав • Основатель</p>
                    </div>
                </div>

                <div className="header-right">
                    <NotificationBell />
                    <RoleSwitcher />
                </div>
            </header>

            {/* Навигация */}
            <NavTabs activeTab={activeTab} onTabChange={handleTabChange} />

            {/* Основной контент */}
            <main className={`main-content ${isMinimized ? 'player-minimized' : ''}`}>
                {renderContent()}
            </main>

            {/* Глобальное уведомление о ближайшем событии */}
            <GlobalEventNotification />

            {/* Плеер подключён через контекст в index.js */}
        </div>
    );
};

export default App;
