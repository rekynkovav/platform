import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import App from '../App';
import { initKeycloak, getUsername, logout, login, getUserRoles } from '../keycloak';

// Моки
jest.mock('../keycloak', () => ({
    initKeycloak: jest.fn(),
    getUsername: jest.fn(),
    getUserRoles: jest.fn(),
    logout: jest.fn(),
    login: jest.fn(),
}));

jest.mock('../components/Navigation/NavTabs', () => () => <div data-testid="nav-tabs">NavTabs</div>);
jest.mock('../components/Feed/MainFeed', () => () => <div data-testid="main-feed">MainFeed</div>);
jest.mock('../components/Feed/NutritionFeed', () => () => <div data-testid="nutrition-feed">NutritionFeed</div>);
jest.mock('../components/Dashboard/Dashboard', () => () => <div data-testid="dashboard">Dashboard</div>);
jest.mock('../components/live/LiveBroadcastViewSimple', () => () => <div data-testid="live-broadcast">LiveBroadcastView</div>);
jest.mock('../features/chat/ChatPage', () => (props) => <div data-testid={`chat-${props.configKey}`}>ChatPage</div>);
jest.mock('../hooks/usePermissions', () => ({
    usePermissions: () => ({ hasLevel: () => true }),
}));
jest.mock('../context/live/LiveSessionContext', () => ({
    LiveSessionProvider: ({ children }) => <div>{children}</div>,
}));
jest.mock('../components/Help/Questions', () => () => <div data-testid="questions">Questions</div>);
jest.mock('../components/UserProfile/UserProfile', () => () => <div data-testid="user-profile">UserProfile</div>);
jest.mock('../components/Events/EventsHub', () => () => <div data-testid="events-hub">EventsHub</div>);
jest.mock('../components/Archive/ArchiveList', () => () => <div data-testid="archive-list">ArchiveList</div>);
jest.mock('../components/Scheduled/ScheduledEvents', () => () => <div data-testid="scheduled-events">ScheduledEvents</div>);
jest.mock('../components/Notification/GlobalEventNotification', () => () => <div data-testid="global-event-notification" />);
jest.mock('../components/Notification/NotificationBell', () => () => <div data-testid="notification-bell" />);
jest.mock('../components/Admin/RoleSwitcher', () => () => <div data-testid="role-switcher" />);
jest.mock('../context/AudioPlayerContext', () => ({
    AudioPlayerProvider: ({ children }) => <div data-testid="audio-player-provider">{children}</div>,
}));

jest.mock('../App.css', () => ({}));
jest.mock('../components/common/ErrorBoundary', () => ({
    __esModule: true,
    default: ({ children }) => children,
}));

describe('App', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('renders loading screen while initializing', () => {
        initKeycloak.mockReturnValue(new Promise(() => {})); // never resolves
        render(
            <MemoryRouter>
                <App />
            </MemoryRouter>
        );
        expect(screen.getByText('Загрузка...')).toBeInTheDocument();
    });

    it('renders login screen when not authenticated', async () => {
        initKeycloak.mockResolvedValue(false);
        render(
            <MemoryRouter>
                <App />
            </MemoryRouter>
        );
        await waitFor(() => {
            expect(screen.getByText(/Пожалуйста, войдите в систему/)).toBeInTheDocument();
            expect(screen.getByText(/Войти через Keycloak/)).toBeInTheDocument();
        });
    });

    it('renders main app when authenticated', async () => {
        initKeycloak.mockResolvedValue(true);
        getUsername.mockReturnValue('testuser');
        getUserRoles.mockReturnValue(['USER_0']);
        render(
            <MemoryRouter>
                <App />
            </MemoryRouter>
        );
        await waitFor(() => {
            expect(screen.getByText('Путь Сердца')).toBeInTheDocument();
            expect(screen.getByTestId('nav-tabs')).toBeInTheDocument();
            expect(screen.getByTestId('main-feed')).toBeInTheDocument();
        });
    });

    it('calls login when login button is clicked', async () => {
        initKeycloak.mockResolvedValue(false);
        render(
            <MemoryRouter>
                <App />
            </MemoryRouter>
        );
        await waitFor(() => {
            expect(screen.getByText('Войти через Keycloak')).toBeInTheDocument();
        });
        screen.getByText('Войти через Keycloak').click();
        expect(login).toHaveBeenCalled();
    });

    it('navigates to profile when profile button is clicked', async () => {
        initKeycloak.mockResolvedValue(true);
        getUsername.mockReturnValue('testuser');
        getUserRoles.mockReturnValue(['USER_0']);
        render(
            <MemoryRouter initialEntries={['/profile']}>
                <App />
            </MemoryRouter>
        );
        await waitFor(() => {
            expect(screen.getByText('Путь Сердца')).toBeInTheDocument();
        });
        fireEvent.click(screen.getByTitle('Личный кабинет'));
        await waitFor(() => {
            expect(screen.getByTestId('user-profile')).toBeInTheDocument();
        });
    });

    it('renders correct route for /chat', async () => {
        initKeycloak.mockResolvedValue(true);
        getUsername.mockReturnValue('testuser');
        getUserRoles.mockReturnValue(['USER_0']);
        render(
            <MemoryRouter initialEntries={['/chat']}>
                <App />
            </MemoryRouter>
        );
        await waitFor(() => {
            expect(screen.getByTestId('chat-general')).toBeInTheDocument();
        });
    });

    it('renders correct route for /nutrition', async () => {
        initKeycloak.mockResolvedValue(true);
        getUsername.mockReturnValue('testuser');
        getUserRoles.mockReturnValue(['USER_0']);
        render(
            <MemoryRouter initialEntries={['/nutrition']}>
                <App />
            </MemoryRouter>
        );
        await waitFor(() => {
            expect(screen.getByTestId('nutrition-feed')).toBeInTheDocument();
        });
    });

    it('redirects unknown routes to /', async () => {
        initKeycloak.mockResolvedValue(true);
        getUsername.mockReturnValue('testuser');
        getUserRoles.mockReturnValue(['USER_0']);
        render(
            <MemoryRouter initialEntries={['/nonexistent']}>
                <App />
            </MemoryRouter>
        );
        await waitFor(() => {
            expect(screen.getByTestId('main-feed')).toBeInTheDocument();
        });
    });
});
