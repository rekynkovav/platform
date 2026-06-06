import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import NavTabs from '../components/Navigation/NavTabs';
import { usePermissions } from '../hooks/usePermissions';

// Моки
jest.mock('../hooks/usePermissions', () => ({
    usePermissions: jest.fn(),
}));

jest.mock('../components/Navigation/NavTabs.css', () => ({}));

describe('NavTabs', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('renders base tabs for USER_0', () => {
        usePermissions.mockReturnValue({
            canViewAdmin: jest.fn().mockReturnValue(false),
            canStartBroadcast: jest.fn().mockReturnValue(false),
            hasLevel: jest.fn().mockReturnValue(false),
        });

        render(
            <MemoryRouter initialEntries={['/']}>
                <NavTabs />
            </MemoryRouter>
        );

        expect(screen.getByText('Главная')).toBeInTheDocument();
        expect(screen.getByText('События')).toBeInTheDocument();
        expect(screen.getByText('Чат')).toBeInTheDocument();
        expect(screen.getByText('Вопросы')).toBeInTheDocument();
        expect(screen.getByText('Профиль')).toBeInTheDocument();
        expect(screen.queryByText('Дашборд')).not.toBeInTheDocument();
        expect(screen.queryByText('Трансляции')).not.toBeInTheDocument();
        expect(screen.queryByText('Админка')).not.toBeInTheDocument();
    });

    it('renders extra tabs for MODERATOR_ADMIN', () => {
        usePermissions.mockReturnValue({
            canViewAdmin: jest.fn().mockReturnValue(true),
            canStartBroadcast: jest.fn().mockReturnValue(true),
            hasLevel: jest.fn().mockReturnValue(true),
        });

        render(
            <MemoryRouter initialEntries={['/']}>
                <NavTabs />
            </MemoryRouter>
        );

        expect(screen.getByText('Главная')).toBeInTheDocument();
        expect(screen.getByText('Дашборд')).toBeInTheDocument();
        expect(screen.getByText('Трансляции')).toBeInTheDocument();
        expect(screen.getByText('Админка')).toBeInTheDocument();
    });

    it('highlights active tab', () => {
        usePermissions.mockReturnValue({
            canViewAdmin: jest.fn().mockReturnValue(false),
            canStartBroadcast: jest.fn().mockReturnValue(false),
            hasLevel: jest.fn().mockReturnValue(false),
        });

        render(
            <MemoryRouter initialEntries={['/chat']}>
                <NavTabs />
            </MemoryRouter>
        );

        const chatTab = screen.getByText('Чат');
        expect(chatTab.classList.contains('active')).toBe(true);
    });

    it('navigates on click', () => {
        usePermissions.mockReturnValue({
            canViewAdmin: jest.fn().mockReturnValue(false),
            canStartBroadcast: jest.fn().mockReturnValue(false),
            hasLevel: jest.fn().mockReturnValue(false),
        });

        render(
            <MemoryRouter initialEntries={['/']}>
                <NavTabs />
            </MemoryRouter>
        );

        const profileTab = screen.getByText('Профиль');
        fireEvent.click(profileTab);
        expect(window.location.pathname).toBe('/profile');
    });
});
