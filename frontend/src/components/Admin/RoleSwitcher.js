import React, { useState, useEffect, useRef } from 'react';
import './RoleSwitcher.css';

const RoleSwitcher = ({ onRoleChange }) => {
    const [currentRole, setCurrentRole] = useState(() => {
        return localStorage.getItem('demoRole') || 'user0';
    });
    const [isOpen, setIsOpen] = useState(false);
    const dropdownRef = useRef(null);

    const roles = [
        { id: 'user0', label: 'Пользователь 0', icon: '👁️', description: 'Просмотр сайта' },
        { id: 'user1', label: 'Пользователь 1', icon: '🌱', description: 'Новички + Чат 1' },
        { id: 'user2', label: 'Пользователь 2', icon: '⭐', description: 'Продвинутые + Чат 2' },
        { id: 'admin-moderator', label: 'Админ (модератор)', icon: '🛡️', description: 'Ответы + контент' },
        { id: 'admin-consultant', label: 'Админ (консультации)', icon: '💼', description: 'Консультации' },
        { id: 'admin-nutrition', label: 'Админ (питание)', icon: '🥗', description: 'Лента питания' },
        { id: 'admin-sport', label: 'Админ (спорт)', icon: '⚽', description: 'Спортивный контент' },
        { id: 'developer', label: 'Разработчик', icon: '💻', description: 'Разработка' },
        { id: 'vyacheslav', label: 'Вячеслав', icon: '👑', description: 'Владелец' },
        { id: 'super-admin', label: 'Главный Админ', icon: '🔱', description: 'Скачивание оригиналов' }
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
            const isAdmin = roleId.includes('admin') || roleId === 'developer' || roleId === 'vyacheslav' || roleId === 'super-admin';
            window.keycloakMock.roles = isAdmin ? ['admin', 'user'] : ['user'];
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
