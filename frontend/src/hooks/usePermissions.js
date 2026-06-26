import { getUserRoles } from '../keycloak';
import { ROLES, ROLE_LEVELS } from '../constants/roles';

export const usePermissions = () => {
    const roles = getUserRoles();
    const SYSTEM_ROLES = ['offline_access', 'uma_authorization', 'default-roles-platform'];
    const appRoles = roles.filter(r => !SYSTEM_ROLES.includes(r));
    // Find highest role by ROLE_LEVELS (not by array index)
    let highestRole = ROLES.USER_0;
    let highestLevel = 0;
    for (const [roleName, roleLevel] of Object.entries(ROLE_LEVELS)) {
        if (appRoles.includes(roleName) && roleLevel > highestLevel) {
            highestLevel = roleLevel;
            highestRole = roleName;
        }
    }
    const level = highestLevel || 1;

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
