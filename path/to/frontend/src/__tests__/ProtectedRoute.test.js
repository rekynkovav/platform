import React from 'react';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import ProtectedRoute from '../components/common/ProtectedRoute';
import { usePermissions } from '../hooks/usePermissions';

jest.mock('../hooks/usePermissions', () => ({
    usePermissions: jest.fn(),
}));

const Child = () => <div data-testid="child">Protected Content</div>;

describe('ProtectedRoute', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('renders children when user has required level', () => {
        usePermissions.mockReturnValue({
            hasLevel: jest.fn().mockReturnValue(true),
        });
        render(
            <MemoryRouter initialEntries={['/test']}>
                <ProtectedRoute requiredLevel={4}>
                    <Child />
                </ProtectedRoute>
            </MemoryRouter>
        );
        expect(screen.getByTestId('child')).toBeInTheDocument();
    });

    it('redirects to / when user does not have required level', () => {
        usePermissions.mockReturnValue({
            hasLevel: jest.fn().mockReturnValue(false),
        });
        render(
            <MemoryRouter initialEntries={['/test']}>
                <ProtectedRoute requiredLevel={6}>
                    <Child />
                </ProtectedRoute>
            </MemoryRouter>
        );
        expect(screen.queryByTestId('child')).not.toBeInTheDocument();
        // Ожидаем редирект на "/"
        expect(window.location.pathname).toBe('/');
    });
});
