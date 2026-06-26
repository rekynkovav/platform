import { useState, useEffect } from 'react';
import { getUserRoles, getUsername } from '../keycloak';
import { ROLES, ROLE_LEVELS, ROLE_LABELS } from '../constants/roles';

// Для DEVELOPMENT режима — все роли доступны для переключения
const DEMO_ROLES = process.env.REACT_APP_ENVIRONMENT === 'development' ? Object.values(ROLES).map(roleId => ({
    id: roleId,
    name: ROLE_LABELS[roleId] || roleId,
    level: ROLE_LEVELS[roleId] || 1
})) : [];

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