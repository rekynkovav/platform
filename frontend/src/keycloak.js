// keycloak.js - Упрощенная мок-версия для разработки
import { mockAuth } from './services/mockApi';

console.log('🔧 Запуск в МОК режиме');

export const initKeycloak = () => mockAuth.init();
export const login = () => console.log('Мок-логин');
export const logout = () => mockAuth.logout();
export const getToken = () => 'mock-token-123';
export const getRefreshToken = () => 'mock-refresh-token';
export const updateToken = async () => true;
export const getUsername = () => mockAuth.getUsername();
export const getUserId = () => 'mock-user-id';
export const getUserRoles = () => mockAuth.getUserRoles();

export default { init: initKeycloak, logout, login };