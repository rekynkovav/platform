import { renderHook } from '@testing-library/react';
import { usePermissions } from '../hooks/usePermissions';
import * as keycloak from '../keycloak';

jest.mock('../keycloak', () => ({
    getUserRoles: jest.fn(),
}));

describe('usePermissions', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('returns level 1 for empty roles', () => {
        keycloak.getUserRoles.mockReturnValue([]);
        const { result } = renderHook(() => usePermissions());
        expect(result.current.level).toBe(1);
        expect(result.current.roles).toEqual([]);
    });

    it('returns correct level for USER_0', () => {
        keycloak.getUserRoles.mockReturnValue(['USER_0']);
        const { result } = renderHook(() => usePermissions());
        expect(result.current.level).toBe(1);
    });

    it('returns correct level for MODERATOR_ADMIN', () => {
        keycloak.getUserRoles.mockReturnValue(['MODERATOR_ADMIN']);
        const { result } = renderHook(() => usePermissions());
        expect(result.current.level).toBe(6);
    });

    it('hasLevel returns true for required <= level', () => {
        keycloak.getUserRoles.mockReturnValue(['MODERATOR_ADMIN']);
        const { result } = renderHook(() => usePermissions());
        expect(result.current.hasLevel(4)).toBe(true);
        expect(result.current.hasLevel(6)).toBe(true);
        expect(result.current.hasLevel(7)).toBe(false);
    });

    it('canStartBroadcast returns true for MODERATOR_ADMIN', () => {
        keycloak.getUserRoles.mockReturnValue(['MODERATOR_ADMIN']);
        const { result } = renderHook(() => usePermissions());
        expect(result.current.canStartBroadcast()).toBe(true);
    });

    it('canStartBroadcast returns false for CONSULTANT_PITANIE', () => {
        keycloak.getUserRoles.mockReturnValue(['CONSULTANT_PITANIE']);
        const { result } = renderHook(() => usePermissions());
        expect(result.current.canStartBroadcast()).toBe(false);
    });

    it('isConsultant returns true for CONSULTANT_PITANIE', () => {
        keycloak.getUserRoles.mockReturnValue(['CONSULTANT_PITANIE']);
        const { result } = renderHook(() => usePermissions());
        expect(result.current.isConsultant()).toBe(true);
    });

    it('isConsultant returns false for USER_0', () => {
        keycloak.getUserRoles.mockReturnValue(['USER_0']);
        const { result } = renderHook(() => usePermissions());
        expect(result.current.isConsultant()).toBe(false);
    });
});
