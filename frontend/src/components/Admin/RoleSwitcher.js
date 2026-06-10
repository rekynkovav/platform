import React, { useState, useEffect, useRef } from 'react';
import './RoleSwitcher.css';
import { ROLES, ROLE_LABELS, ROLE_ICONS } from '../../constants/roles';
import { demoApi } from '../../services/api';
import { getUserRoles } from '../../keycloak';

const isDevelopment = process.env.REACT_APP_ENVIRONMENT === 'development';

const RoleSwitcher = ({ onRoleChange }) => {
    const [currentRole, setCurrentRole] = useState(() => {
        // Только для development
        if (process.env.REACT_APP_ENVIRONMENT === 'development') {
            return sessionStorage.getItem('demoRole') || 'USER_0';
        }
        // В production — реальные роли из Keycloak
        const roles = getUserRoles();
        return roles.length > 0 ? roles[0] : 'USER_0';
    });

    useEffect(() => {
        const loadDemoRole = async () => {
            try {
                const role = await demoApi.getDemoRole();
                if (role) {
                    setCurrentRole(role);
                }
            } catch (err) {
                console.error('Failed to load demo role:', err);
                const savedRole = sessionStorage.getItem('demoRole');
                if (savedRole) {
                    setCurrentRole(savedRole);
                }
            }
        };

        if (process.env.REACT_APP_ENVIRONMENT === 'development') {
            loadDemoRole();
        }
    }, []);
    const [isOpen, setIsOpen] = useState(false);
    const dropdownRef = useRef(null);

    const roles = [
        { id: ROLES.USER_0, label: ROLE_LABELS[ROLES.USER_0], icon: ROLE_ICONS[ROLES.USER_0], description: 'Просмотр сайта' },
        { id: ROLES.USER_1, label: ROLE_LABELS[ROLES.USER_1], icon: ROLE_ICONS[ROLES.USER_1], description: 'Чат 1' },
        { id: ROLES.USER_2, label: ROLE_LABELS[ROLES.USER_2], icon: ROLE_ICONS[ROLES.USER_2], description: 'Чат 2' },
        { id: ROLES.MODERATOR_ADMIN, label: ROLE_LABELS[ROLES.MODERATOR_ADMIN], icon: ROLE_ICONS[ROLES.MODERATOR_ADMIN], description: 'Ответы + контент' },
        { id: ROLES.CONSULTANT_PITANIE, label: ROLE_LABELS[ROLES.CONSULTANT_PITANIE], icon: ROLE_ICONS[ROLES.CONSULTANT_PITANIE], description: 'Лента питания' },
        { id: ROLES.CONSULTANT_SPORT, label: ROLE_LABELS[ROLES.CONSULTANT_SPORT], icon: ROLE_ICONS[ROLES.CONSULTANT_SPORT], description: 'Спортивный контент' },
        { id: ROLES.DEVELOPER, label: ROLE_LABELS[ROLES.DEVELOPER], icon: ROLE_ICONS[ROLES.DEVELOPER], description: 'Разработка' },
        { id: ROLES.VYACHESLAV, label: ROLE_LABELS[ROLES.VYACHESLAV], icon: ROLE_ICONS[ROLES.VYACHESLAV], description: 'Владелец' },
        { id: ROLES.SUPER_ADMIN, label: ROLE_LABELS[ROLES.SUPER_ADMIN], icon: ROLE_ICONS[ROLES.SUPER_ADMIN], description: 'Полный доступ' }
    ];

    useEffect(() => {
        const handleClickOutside = (event) => {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
                setIsOpen(false);
            }
        };

        if (isOpen) {
            document.addEventListener('mousedown', handleClickOutside);
        }

        return () => {
            document.removeEventListener('mousedown', handleClickOutside);
        };
    }, [isOpen]);

    const handleRoleChange = (roleId) => {
        if (process.env.REACT_APP_ENVIRONMENT !== 'development') return;

        setCurrentRole(roleId);
        sessionStorage.setItem('demoRole', roleId);

        if (onRoleChange) onRoleChange(roleId);
        setIsOpen(false);
        window.location.reload();
    };

    const getCurrentRoleData = () => {
        return roles.find(r => r.id === currentRole) || roles[0];
    };

    const roleData = getCurrentRoleData();

    return (
        <div className="role-switcher-container" ref={dropdownRef}>
            <div className="role-switcher-header" onClick={() => setIsOpen(!isOpen)}>
                <span className="role-icon">{roleData.icon}</span>
                <span className="role-text">{roleData.label}</span>
                <span className="role-arrow">{isOpen ? '▲' : '▼'}</span>
            </div>

            {isOpen && (
                <div className="role-dropdown">
                    <div className="role-dropdown-header">
                        <h4>Выберите роль</h4>
                        <p>Для демонстрации функционала</p>
                    </div>
                    <div className="role-list">
                        {roles.map(role => (
                            <div
                                key={role.id}
                                className={`role-item ${currentRole === role.id ? 'active' : ''}`}
                                onClick={() => handleRoleChange(role.id)}
                            >
                                <span className="role-item-icon">{role.icon}</span>
                                <div className="role-item-content">
                                    <div className="role-item-label">{role.label}</div>
                                    <div className="role-item-description">{role.description}</div>
                                </div>
                                {currentRole === role.id && <span className="role-check">✓</span>}
                            </div>
                        ))}
                    </div>
                </div>
            )}
        </div>
    );
};

export default RoleSwitcher;
