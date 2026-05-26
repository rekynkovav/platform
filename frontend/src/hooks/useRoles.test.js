import { renderHook, act } from '@testing-library/react';
import { useRoles } from './useRoles';

// Mock keycloak
jest.mock('../keycloak', () => ({
    getUserRoles: jest.fn(() => ['USER_2', 'MODERATOR_ADMIN'])
}));

describe('useRoles', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    test('should return roles from keycloak', () => {
        const { result } = renderHook(() => useRoles());

        expect(result.current.roles).toContain('USER_2');
        expect(result.current.roles).toContain('MODERATOR_ADMIN');
    });

    test('should determine primary role correctly', () => {
        const { result } = renderHook(() => useRoles());

        // MODERATOR_ADMIN has higher priority than USER_2
        expect(result.current.primaryRole).toBe('MODERATOR_ADMIN');
    });

    test('hasRole should return true for existing role', () => {
        const { result } = renderHook(() => useRoles());

        expect(result.current.hasRole('USER_2')).toBe(true);
        expect(result.current.hasRole('SUPER_ADMIN')).toBe(false);
    });

    test('hasAnyRole should return true for any matching role', () => {
        const { result } = renderHook(() => useRoles());

        expect(result.current.hasAnyRole(['USER_1', 'USER_2'])).toBe(true);
        expect(result.current.hasAnyRole(['USER_0', 'USER_1'])).toBe(false);
    });

    test('isAtLeast should compare role priorities', () => {
        const { result } = renderHook(() => useRoles());

        // MODERATOR_ADMIN priority 4, USER_2 priority 2
        expect(result.current.isAtLeast('USER_2')).toBe(true);
        expect(result.current.isAtLeast('SUPER_ADMIN')).toBe(false);
    });
});