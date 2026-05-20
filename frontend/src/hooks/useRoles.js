import { useState, useEffect } from 'react';
import { getUserRoles } from '../keycloak';

const ROLE_PRIORITY = {
    'USER_0': 0,
    'USER_1': 1,
    'USER_2': 2,
    'CONSULTANT_PITANIE': 3,
    'CONSULTANT_SPORT': 3,
    'MODERATOR_ADMIN': 4,
    'DEVELOPER': 5,
    'VYACHESLAV': 6,
    'SUPER_ADMIN': 7
};

export const useRoles = () => {
    const [roles, setRoles] = useState([]);
    const [primaryRole, setPrimaryRole] = useState('USER_0');
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const loadRoles = () => {
            const userRoles = getUserRoles();
            setRoles(userRoles);

            if (userRoles.length > 0) {
                let highestRole = userRoles[0];
                for (const role of userRoles) {
                    if ((ROLE_PRIORITY[role] || 0) > (ROLE_PRIORITY[highestRole] || 0)) {
                        highestRole = role;
                    }
                }
                setPrimaryRole(highestRole);
            }
            setLoading(false);
        };

        loadRoles();
    }, []);

    const hasRole = (roleName) => {
        return roles.includes(roleName);
    };

    const hasAnyRole = (roleNames) => {
        return roleNames.some(role => roles.includes(role));
    };

    const isAtLeast = (minRole) => {
        const currentPriority = ROLE_PRIORITY[primaryRole] || 0;
        const minPriority = ROLE_PRIORITY[minRole] || 0;
        return currentPriority >= minPriority;
    };

    return { roles, primaryRole, loading, hasRole, hasAnyRole, isAtLeast };
};