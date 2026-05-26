import { getUserRoles } from '../keycloak';
import { ROLES, ROLE_LEVELS } from '../constants/roles';

export const usePermissions = () => {
    const roles = getUserRoles();
    const highestRole = roles.length > 0 ? roles[0] : ROLES.USER_0;
    const level = ROLE_LEVELS[highestRole] || 1;

    const hasRole = (requiredRole) => {
        return roles.includes(requiredRole);
    };

    const hasAnyRole = (requiredRoles) => {
        return requiredRoles.some(r => roles.includes(r));
    };

    const hasLevel = (requiredLevel) => {
        return level >= requiredLevel;
    };

    const canViewAdmin = () => hasLevel(6);
    const canUploadMedia = () => hasLevel(4);
    const canStartBroadcast = () => hasLevel(6);
    const canModerate = () => hasLevel(6);
    const isConsultant = () => hasAnyRole([ROLES.CONSULTANT_PITANIE, ROLES.CONSULTANT_SPORT]);

    return {
        roles,
        level,
        hasRole,
        hasAnyRole,
        hasLevel,
        canViewAdmin,
        canUploadMedia,
        canStartBroadcast,
        canModerate,
        isConsultant
    };
};