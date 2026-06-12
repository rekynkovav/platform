import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import RoleSwitcher from '../components/Admin/RoleSwitcher';
import { demoApi } from '../services/api';
import { getUserRoles } from '../keycloak';

// Моки
jest.mock('../services/api', () => ({
    demoApi: {
        getDemoRole: jest.fn(),
    },
}));

jest.mock('../keycloak', () => ({
    getUserRoles: jest.fn(),
}));

jest.mock('../constants/roles', () => ({
    ROLES: {
        USER_0: 'USER_0',
        USER_1: 'USER_1',
        USER_2: 'USER_2',
        MODERATOR_ADMIN: 'MODERATOR_ADMIN',
        CONSULTANT_PITANIE: 'CONSULTANT_PITANIE',
        CONSULTANT_SPORT: 'CONSULTANT_SPORT',
        DEVELOPER: 'DEVELOPER',
        VYACHESLAV: 'VYACHESLAV',
        SUPER_ADMIN: 'SUPER_ADMIN',
    },
    ROLE_LABELS: {
        USER_0: 'Пользователь',
        USER_1: 'Пользователь 1',
        USER_2: 'Пользователь 2',
        MODERATOR_ADMIN: 'Администратор',
        CONSULTANT_PITANIE: 'Консультант (питание)',
        CONSULTANT_SPORT: 'Консультант (спорт)',
        DEVELOPER: 'Разработчик',
        VYACHESLAV: 'Вячеслав',
        SUPER_ADMIN: 'Супер Админ',
    },
    ROLE_ICONS: {
        USER_0: '👤',
        USER_1: '👤',
        USER_2: '👤',
        MODERATOR_ADMIN: '🛡️',
        CONSULTANT_PITANIE: '🥗',
        CONSULTANT_SPORT: '🏋️',
        DEVELOPER: '💻',
        VYACHESLAV: '👑',
        SUPER_ADMIN: '⭐',
    },
}));

jest.mock('../components/Admin/RoleSwitcher.css', () => ({}));

describe('RoleSwitcher', () => {
    const mockOnRoleChange = jest.fn();

    beforeEach(() => {
        jest.clearAllMocks();
        delete process.env.REACT_APP_ENVIRONMENT;
        getUserRoles.mockReturnValue(['USER_0']);
        demoApi.getDemoRole.mockResolvedValue(null);
    });

    it('renders default role for production environment', () => {
        process.env.REACT_APP_ENVIRONMENT = 'production';
        render(<RoleSwitcher onRoleChange={mockOnRoleChange} />);
        expect(screen.getByText('Пользователь')).toBeInTheDocument();
    });

    it('renders role switcher header with current role', () => {
        process.env.REACT_APP_ENVIRONMENT = 'development';
        render(<RoleSwitcher onRoleChange={mockOnRoleChange} />);
        expect(screen.getByText('Пользователь')).toBeInTheDocument();
    });

    it('opens dropdown when clicking header', () => {
        process.env.REACT_APP_ENVIRONMENT = 'development';
        render(<RoleSwitcher onRoleChange={mockOnRoleChange} />);

        const header = screen.getByText('Пользователь').closest('.role-switcher-header');
        fireEvent.click(header);

        expect(screen.getByText('Выберите роль')).toBeInTheDocument();
        expect(screen.getByText('Администратор')).toBeInTheDocument();
        expect(screen.getByText('Супер Админ')).toBeInTheDocument();
    });

    it('loads demo role from API in development', async () => {
        process.env.REACT_APP_ENVIRONMENT = 'development';
        demoApi.getDemoRole.mockResolvedValue('MODERATOR_ADMIN');

        render(<RoleSwitcher onRoleChange={mockOnRoleChange} />);

        await waitFor(() => {
            expect(demoApi.getDemoRole).toHaveBeenCalled();
            expect(screen.getByText('Администратор')).toBeInTheDocument();
        });
    });

    it('does not allow role change in production', () => {
        process.env.REACT_APP_ENVIRONMENT = 'production';
        render(<RoleSwitcher onRoleChange={mockOnRoleChange} />);

        const header = screen.getByText('Пользователь').closest('.role-switcher-header');
        fireEvent.click(header);

        const adminItem = screen.getByText('Администратор');
        fireEvent.click(adminItem);

        expect(mockOnRoleChange).not.toHaveBeenCalled();
    });

    it('closes dropdown when clicking outside', () => {
        process.env.REACT_APP_ENVIRONMENT = 'development';
        render(<RoleSwitcher onRoleChange={mockOnRoleChange} />);

        const header = screen.getByText('Пользователь').closest('.role-switcher-header');
        fireEvent.click(header);
        expect(screen.getByText('Выберите роль')).toBeInTheDocument();

        fireEvent.mouseDown(document.body);

        expect(screen.queryByText('Выберите роль')).not.toBeInTheDocument();
    });

    it('shows check mark on active role', () => {
        process.env.REACT_APP_ENVIRONMENT = 'development';
        render(<RoleSwitcher onRoleChange={mockOnRoleChange} />);

        const header = screen.getByText('Пользователь').closest('.role-switcher-header');
        fireEvent.click(header);

        expect(screen.getByText('✓')).toBeInTheDocument();
    });

    it('uses sessionStorage role in development', () => {
        process.env.REACT_APP_ENVIRONMENT = 'development';
        sessionStorage.setItem('demoRole', 'SUPER_ADMIN');
        render(<RoleSwitcher onRoleChange={mockOnRoleChange} />);
        expect(screen.getByText('Супер Админ')).toBeInTheDocument();
        sessionStorage.removeItem('demoRole');
    });
});
