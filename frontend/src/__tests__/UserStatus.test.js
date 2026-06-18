import React from 'react';
import { render, screen, waitFor, act } from '@testing-library/react';

const mockGetUsername = jest.fn(() => 'testuser');
const mockGetUserRoles = jest.fn(() => ['USER_0']);

jest.mock('../keycloak', () => ({
    __esModule: true,
    default: {},
    getUsername: (...args) => mockGetUsername(...args),
    getUserRoles: (...args) => mockGetUserRoles(...args),
}));

jest.mock('../services/api', () => ({
    usersApi: {
        getCurrentUser: jest.fn(),
    },
}));

import UserStatus from '../components/Common/UserStatus';
const { usersApi } = require('../services/api');

describe('UserStatus', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockGetUsername.mockReturnValue('testuser');
        mockGetUserRoles.mockReturnValue(['USER_0']);
        usersApi.getCurrentUser.mockResolvedValue({
            status: 'online',
            lastSeen: new Date().toISOString(),
        });
    });

    it('shows loading state initially', async () => {
        await act(async () => {
            render(<UserStatus />);
        });
        expect(document.querySelector('.user-status')).toBeInTheDocument();
    });

    it('renders user data after loading', async () => {
        await act(async () => {
            render(<UserStatus />);
        });
        await waitFor(() => {
            expect(screen.getByText('testuser')).toBeInTheDocument();
        });
    });

    it('shows online status text', async () => {
        await act(async () => {
            render(<UserStatus />);
        });
        await waitFor(() => {
            expect(screen.getByText('В сети')).toBeInTheDocument();
        });
    });

    it('shows role badges', async () => {
        await act(async () => {
            render(<UserStatus />);
        });
        await waitFor(() => {
            expect(screen.getByText('USER_0')).toBeInTheDocument();
        });
    });

    it('shows error state when API fails', async () => {
        usersApi.getCurrentUser.mockRejectedValueOnce(new Error('API error'));
        await act(async () => {
            render(<UserStatus />);
        });
        await waitFor(() => {
            expect(screen.getByText('Не удалось загрузить данные пользователя')).toBeInTheDocument();
        });
    });

    it('sets fallback data on error', async () => {
        usersApi.getCurrentUser.mockRejectedValueOnce(new Error('API error'));
        await act(async () => {
            render(<UserStatus />);
        });
        await waitFor(() => {
            expect(screen.getByText('Не удалось загрузить данные пользователя')).toBeInTheDocument();
        });
        expect(document.querySelector('.user-status.error')).toBeInTheDocument();
    });
});
