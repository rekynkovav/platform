// =============================================
// keycloak.js — Mock для демо-режима
// Полностью заменяет реальный Keycloak в разработке
// =============================================

const mockUser = {
    username: 'Тестовый Пользователь',
    roles: ['USER_2']
};

let isInitialized = false;

export const initKeycloak = async () => {
    await new Promise(resolve => setTimeout(resolve, 400)); // имитация загрузки
    isInitialized = true;
    console.log('%c🔑 Keycloak Mock initialized (demo mode)', 'color: #8b5cf6; font-weight: bold');
    return true;
};

export const getUsername = () => {
    return mockUser.username;
};

export const getUserRoles = () => {
    const demoRole = localStorage.getItem('demoRole');
    if (demoRole) {
        return [demoRole];
    }
    return mockUser.roles;
};

export const isAuthenticated = () => {
    return true; // в демо-режиме всегда авторизован
};

export const logout = () => {
    localStorage.removeItem('demoRole');
    window.location.reload();
};

export const getToken = () => {
    return 'mock-jwt-token-' + Date.now();
};

// Для совместимости со старым кодом
export const keycloak = {
    init: initKeycloak,
    authenticated: true,
    token: 'mock-jwt-token',
    loadUserInfo: () => Promise.resolve(mockUser)
};

export default {
    initKeycloak,
    getUsername,
    getUserRoles,
    isAuthenticated,
    logout,
    getToken,
    keycloak
};