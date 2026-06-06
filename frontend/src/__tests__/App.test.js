import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import App from '../App';
import { initKeycloak, getUsername, logout, login } from '../keycloak';

// Моки
jest.mock('../keycloak', () => ({
    initKeycloak: jest.fn(),
    getUsername: jest.fn(),
    logout: jest.fn(),
    login: jest.fn(),
}));

jest.mock('../components/Navigation/NavTabs', () => () => <div data-testid="nav-tabs">NavTabs</div>);
jest.mock('../components/Feed/MainFeed', () => () => <div data-testid="main-feed">MainFeed</div>);
jest.mock('../components/Dashboard/Dashboard', () => () => <div data-testid="dashboard">Dashboard</div>);
jest.mock('../components/live/LiveBroadcastView', () => () => <div data-testid="live-broadcast">LiveBroadcastView</div>);
jest.mock('../components/Chat/GeneralChat', () => () => <div data-testid="general-chat">GeneralChat</div>);
jest.mock('../components/UserProfile/UserProfile', () => () => <div data-testid="user-profile">UserProfile</div>);
jest.mock('../context/AudioPlayerContext', () => ({
    AudioPlayerProvider: ({ children }) => <div data-testid="audio-player-provider">{children}</div>,
}));
jest.mock('../components/common/ProtectedRoute', () => ({ children }) => <div data-testid="protected-route">{children}</div>);

jest.mock('../App.css', () => ({}));

describe('App', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('renders loading screen while initializing', () => {
        initKeycloak.mockReturnValue(new Promise(() => {})); // never resolves
        render(<App />);
        expect(screen.getByText('Loading...')).toBeInTheDocument();
    });

    it('renders login screen when not authenticated', async () => {
        initKeycloak.mockResolvedValue(false);
        render(<App />);

        await waitFor(() => {
            expect(screen.getByText('Добро пожаловать в Платформу')).toBeInTheDocument();
            expect(screen.getByText('Войти через Keycloak')).toBeInTheDocument();
        });
    });

    it('renders main app when authenticated', async () => {
        initKeycloak.mockResolvedValue(true);
        getUsername.mockReturnValue('testuser');
        render(<App />);

        await waitFor(() => {
            expect(screen.getByText('Platform')).toBeInTheDocument();
            expect(screen.getByText('testuser')).toBeInTheDocument();
            expect(screen.getByTestId('nav-tabs')).toBeInTheDocument();
            expect(screen.getByTestId('main-feed')).toBeInTheDocument();
        });
    });

    it('calls login when login button is clicked', async () => {
        initKeycloak.mockResolvedValue(false);
        render(<App />);

        await waitFor(() => {
            expect(screen.getByText('Войти через Keycloak')).toBeInTheDocument();
        });

        screen.getByText('Войти через Keycloak').click();
        expect(login).toHaveBeenCalled();
    });

    it('calls logout when logout button is clicked', async () => {
        initKeycloak.mockResolvedValue(true);
        getUsername.mockReturnValue('testuser');
        render(<App />);

        await waitFor(() => {
            expect(screen.getByText('Logout')).toBeInTheDocument();
        });

        screen.getByText('Logout').click();
        expect(logout).toHaveBeenCalled();
    });

    it('renders dashboard route for authorized user', async () => {
        initKeycloak.mockResolvedValue(true);
        getUsername.mockReturnValue('testuser');
        window.history.pushState({}, 'Dashboard', '/dashboard');
        render(<App />);

        await waitFor(() => {
            expect(screen.getByTestId('dashboard')).toBeInTheDocument();
        });
    });

    it('renders chat route', async () => {
        initKeycloak.mockResolvedValue(true);
        getUsername.mockReturnValue('testuser');
        window.history.pushState({}, 'Chat', '/chat');
        render(<App />);

        await waitFor(() => {
            expect(screen.getByTestId('general-chat')).toBeInTheDocument();
        });
    });

    it('renders profile route', async () => {
        initKeycloak.mockResolvedValue(true);
        getUsername.mockReturnValue('testuser');
        window.history.pushState({}, 'Profile', '/profile');
        render(<App />);

        await waitFor(() => {
            expect(screen.getByTestId('user-profile')).toBeInTheDocument();
        });
    });
});
