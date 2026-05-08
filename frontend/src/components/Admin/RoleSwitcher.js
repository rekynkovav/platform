import React, { useState, useEffect, useRef } from 'react';
import './RoleSwitcher.css';

const RoleSwitcher = ({ onRoleChange }) => {
    const [currentRole, setCurrentRole] = useState(() => {
        return localStorage.getItem('demoRole') || 'ROLE_USER';
    });
    const [isOpen, setIsOpen] = useState(false);
    const dropdownRef = useRef(null);

    const roles = [
        { id: 'ROLE_USER', label: 'Пользователь', icon: '👤', description: 'Базовый доступ' },
        { id: 'ROLE_PREMIUM', label: 'Премиум', icon: '💎', description: 'Расширенные возможности' },
        { id: 'ROLE_ADVANCED', label: 'Продвинутый', icon: '🚀', description: 'Продвинутый доступ' },
        { id: 'ROLE_CREATOR', label: 'Создатель', icon: '🎨', description: 'Загрузка контента' },
        { id: 'ROLE_ADMIN', label: 'Администратор', icon: '🛡️', description: 'Управление контентом' },
        { id: 'ROLE_MODERATOR', label: 'Модератор', icon: '👮', description: 'Модерация' },
        { id: 'ROLE_SPORT_ADMIN', label: 'Спортивный админ', icon: '⚽', description: 'Спортивный контент' },
        { id: 'ROLE_NUTRITION_ADMIN', label: 'Админ питания', icon: '🥗', description: 'Питание и диеты' },
        { id: 'ROLE_CONSULTANT_ADMIN', label: 'Консультант', icon: '💬', description: 'Консультации' },
        { id: 'ROLE_OWNER', label: 'Владелец', icon: '👑', description: 'Вячеслав Тимошенко' },
        { id: 'ROLE_SUPER_ADMIN', label: 'Супер Админ', icon: '🔱', description: 'Полный доступ' }
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
        setCurrentRole(roleId);
        localStorage.setItem('demoRole', roleId);

        if (window.keycloakMock) {
            window.keycloakMock.roles = [roleId];
        }

        if (onRoleChange) {
            onRoleChange(roleId);
        }

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
