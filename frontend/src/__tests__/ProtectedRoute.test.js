import React from 'react';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import ProtectedRoute from '../components/common/ProtectedRoute';
import { usePermissions } from '../hooks/usePermissions';

// Моки
jest.mock('../hooks/usePermissions', () => ({
    usePermissions: jest.fn(),
}));

describe('ProtectedRoute', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('renders children when level is sufficient', () => {
        usePermissions.mockReturnValue({ level: 5 });
        render(
            <MemoryRouter>
                <ProtectedRoute requiredLevel={3}>
                    <div data-testid="protected-content">Protected Content</div>
                </ProtectedRoute>
            </MemoryRouter>
        );

        expect(screen.getByTestId('protected-content')).toBeInTheDocument();
        expect(screen.getByText('Protected Content')).toBeInTheDocument();
    });

    it('renders children when level equals requiredLevel', () => {
        usePermissions.mockReturnValue({ level: 3 });
        render(
            <MemoryRouter>
                <ProtectedRoute requiredLevel={3}>
                    <div data-testid="protected-content">Protected Content</div>
                </ProtectedRoute>
            </MemoryRouter>
        );

        expect(screen.getByTestId('protected-content')).toBeInTheDocument();
    });

    it('redirects to "/" when level is insufficient', () => {
        usePermissions.mockReturnValue({ level: 1 });
        render(
            <MemoryRouter initialEntries={['/admin']}>
                <ProtectedRoute requiredLevel={5}>
                    <div data-testid="protected-content">Protected Content</div>
                </ProtectedRoute>
            </MemoryRouter>
        );

        expect(screen.queryByTestId('protected-content')).not.toBeInTheDocument();
        // Should redirect to "/" - check that Navigate rendered
        expect(window.location.pathname).toBe('/');
    });

    it('uses default requiredLevel of 1', () => {
        usePermissions.mockReturnValue({ level: 1 });
        render(
            <MemoryRouter>
                <ProtectedRoute>
                    <div data-testid="protected-content">Protected Content</div>
                </ProtectedRoute>
            </MemoryRouter>
        );

        expect(screen.getByTestId('protected-content')).toBeInTheDocument();
    });

    it('does not render children when level is 0', () => {
        usePermissions.mockReturnValue({ level: 0 });
        render(
            <MemoryRouter initialEntries={['/restricted']}>
                <ProtectedRoute requiredLevel={1}>
                    <div data-testid="protected-content">Protected Content</div>
                </ProtectedRoute>
            </MemoryRouter>
        );

        expect(screen.queryByTestId('protected-content')).not.toBeInTheDocument();
    });
});
