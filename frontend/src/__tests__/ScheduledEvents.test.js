import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import ScheduledEvents from '../components/Scheduled/ScheduledEvents';
import { api } from '../services/api';
import { usePermissions } from '../hooks/usePermissions';

// Моки
jest.mock('../services/api', () => ({
    api: {
        get: jest.fn(),
        post: jest.fn(),
        put: jest.fn(),
        delete: jest.fn(),
    },
}));

jest.mock('../hooks/usePermissions', () => ({
    usePermissions: jest.fn(),
}));

jest.mock('../components/Scheduled/ScheduledEvents.css', () => ({}));

// Helper: create a permissions object for a given role level
const createPermissions = (roleName, level) => ({
    roles: [roleName],
    level,
    hasRole: (r) => [roleName].includes(r),
    hasAnyRole: (rs) => rs.some(r => [roleName].includes(r)),
    hasLevel: (n) => level >= n,
    canViewAdmin: () => level >= 6,
    canUploadMedia: () => level >= 4,
    canStartBroadcast: () => level >= 6,
    canModerate: () => level >= 6,
    isConsultant: () => false,
});

describe('ScheduledEvents', () => {
    const mockEvents = [
        { id: 1, title: 'Test Event 1', date: '2025-07-01', time: '18:00', description: 'Event desc 1' },
        { id: 2, title: 'Test Event 2', date: '2025-07-15', time: '20:00', description: '' },
    ];

    beforeEach(() => {
        jest.clearAllMocks();
        delete process.env.REACT_APP_ENVIRONMENT;
        process.env.NODE_ENV = 'test';
        usePermissions.mockReturnValue(createPermissions('USER_0', 1));
        api.get.mockResolvedValue(mockEvents);
    });

    it('renders events list', async () => {
        render(<ScheduledEvents />);

        await waitFor(() => {
            expect(screen.getByText('Test Event 1')).toBeInTheDocument();
            expect(screen.getByText('Test Event 2')).toBeInTheDocument();
        });
    });

    it('shows no events message when empty', async () => {
        api.get.mockResolvedValue([]);
        render(<ScheduledEvents />);

        await waitFor(() => {
            expect(screen.getByText('📭 Нет запланированных событий')).toBeInTheDocument();
        });
    });

    it('renders add event button for admin in development', async () => {
        process.env.REACT_APP_ENVIRONMENT = 'development';
        usePermissions.mockReturnValue(createPermissions('SUPER_ADMIN', 8));

        render(<ScheduledEvents />);

        await waitFor(() => {
            expect(screen.getByText('+ Добавить событие')).toBeInTheDocument();
        });
    });

    it('hides add event button for regular user', async () => {
        render(<ScheduledEvents />);

        await waitFor(() => {
            expect(screen.queryByText('+ Добавить событие')).not.toBeInTheDocument();
        });
    });

    it('shows edit and delete buttons for admin', async () => {
        process.env.REACT_APP_ENVIRONMENT = 'development';
        usePermissions.mockReturnValue(createPermissions('SUPER_ADMIN', 8));

        render(<ScheduledEvents />);

        await waitFor(() => {
            expect(screen.getAllByTitle('Редактировать событие').length).toBe(2);
            expect(screen.getAllByTitle('Удалить событие').length).toBe(2);
        });
    });

    it('hides edit and delete buttons for regular user', async () => {
        render(<ScheduledEvents />);

        await waitFor(() => {
            expect(screen.queryByTitle('Редактировать событие')).not.toBeInTheDocument();
            expect(screen.queryByTitle('Удалить событие')).not.toBeInTheDocument();
        });
    });

    it('displays error message when API fails', async () => {
        api.get.mockRejectedValue(new Error('Network error'));
        render(<ScheduledEvents />);

        await waitFor(() => {
            expect(screen.getByText('Не удалось загрузить события')).toBeInTheDocument();
        });
    });

    it('shows event description when present', async () => {
        render(<ScheduledEvents />);

        await waitFor(() => {
            expect(screen.getByText('Event desc 1')).toBeInTheDocument();
        });
    });

    it('opens add event form when add button is clicked', async () => {
        process.env.REACT_APP_ENVIRONMENT = 'development';
        usePermissions.mockReturnValue(createPermissions('SUPER_ADMIN', 8));

        render(<ScheduledEvents />);

        await waitFor(() => {
            expect(screen.getByText('+ Добавить событие')).toBeInTheDocument();
        });

        fireEvent.click(screen.getByText('+ Добавить событие'));

        expect(screen.getByText('Новое событие')).toBeInTheDocument();
        expect(screen.getByPlaceholderText('Название события')).toBeInTheDocument();
    });
});
