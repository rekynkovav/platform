import { useState, useEffect } from 'react';
import { getUserRoles, getUsername } from '../keycloak';
import { ROLES, ROLE_LEVELS, ROLE_LABELS } from '../constants/roles';

// Только для DEVELOPMENT режима
const DEMO_ROLES = process.env.REACT_APP_ENVIRONMENT === 'development' ? [
    { id: ROLES.SUPER_ADMIN, name: ROLE_LABELS[ROLES.SUPER_ADMIN], level: ROLE_LEVELS[ROLES.SUPER_ADMIN] },
    { id: ROLES.VYACHESLAV, name: ROLE_LABELS[ROLES.VYACHESLAV], level: ROLE_LEVELS[ROLES.VYACHESLAV] },
    { id: ROLES.MODERATOR_ADMIN, name: ROLE_LABELS[ROLES.MODERATOR_ADMIN], level: ROLE_LEVELS[ROLES.MODERATOR_ADMIN] },
    { id: ROLES.DEVELOPER, name: ROLE_LABELS[ROLES.DEVELOPER], level: ROLE_LEVELS[ROLES.DEVELOPER] },
    { id: ROLES.USER_2, name: ROLE_LABELS[ROLES.USER_2], level: ROLE_LEVELS[ROLES.USER_2] },
    { id: ROLES.USER_1, name: ROLE_LABELS[ROLES.USER_1], level: ROLE_LEVELS[ROLES.USER_1] },
    { id: ROLES.USER_0, name: ROLE_LABELS[ROLES.USER_0], level: ROLE_LEVELS[ROLES.USER_0] }
] : [];

export const useRoles = () => {
    const [currentRole, setCurrentRole] = useState(() => {
        // В production — только реальные роли из Keycloak
        if (process.env.REACT_APP_ENVIRONMENT === 'production') {
            const roles = getUserRoles();
            return roles.length > 0 ? roles[0] : ROLES.USER_0;
        }
        // В development — можно использовать demoRole из sessionStorage (не localStorage!)
        return sessionStorage.getItem('demoRole') || ROLES.USER_0;
    });

    const availableRoles = process.env.REACT_APP_ENVIRONMENT === 'development' ? DEMO_ROLES : [];

    const switchRole = (roleId) => {
        if (process.env.REACT_APP_ENVIRONMENT !== 'development') return;
        if (DEMO_ROLES.some(r => r.id === roleId)) {
            setCurrentRole(roleId);
            sessionStorage.setItem('demoRole', roleId);
            window.location.reload();
        }
    };

    return { currentRole, availableRoles, switchRole };
};