import React, { useState, useEffect } from 'react';
import './RoleSwitcher.css';

const RoleSwitcher = ({ onRoleChange }) => {
    const [isAdmin, setIsAdmin] = useState(() => {
        return localStorage.getItem('demoRole') === 'admin';
    });

    const toggleRole = () => {
        const newRole = !isAdmin;
        setIsAdmin(newRole);
        localStorage.setItem('demoRole', newRole ? 'admin' : 'user');

        if (window.keycloakMock) {
            window.keycloakMock.roles = newRole ? ['admin', 'user'] : ['user'];
        }

        if (onRoleChange) {
            onRoleChange(newRole);
        }

        window.location.reload();
    };

    return (
        <div className="role-switcher-header" onClick={toggleRole}>
            <span className="role-icon">{isAdmin ? '👑' : '👤'}</span>
            <span className="role-text">{isAdmin ? 'Админ' : 'Пользователь'}</span>
        </div>
    );
};

export default RoleSwitcher;