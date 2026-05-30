import Keycloak, { KeycloakInstance, KeycloakTokenParsed } from 'keycloak-js';

interface KeycloakConfig {
  url: string;
  realm: string;
  clientId: string;
}

interface KeycloakInitOptions {
  onLoad: 'check-sso' | 'login-required';
  silentCheckSsoRedirectUri: string;
  pkceMethod: 'S256';
}

const keycloakConfig: KeycloakConfig = {
  url: process.env.REACT_APP_KEYCLOAK_URL || 'http://localhost:8080/auth',
  realm: 'platform',
  clientId: 'platform-frontend'
};

let keycloakInstance: KeycloakInstance | null = null;
let isInitialized = false;
let userRoles: string[] = [];
let username = '';

export const initKeycloak = async (): Promise<boolean> => {
  if (isInitialized) return keycloakInstance?.authenticated || false;

  keycloakInstance = new Keycloak(keycloakConfig);

  try {
    const authenticated = await keycloakInstance.init({
      onLoad: 'check-sso',
      silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html',
      pkceMethod: 'S256'
    } as KeycloakInitOptions);

    if (authenticated) {
      const token: KeycloakTokenParsed | undefined = keycloakInstance.tokenParsed;
      if (token?.realm_access?.roles) {
        userRoles = token.realm_access.roles;
      }
      username = token?.preferred_username || token?.email || 'User';

      // Обновление токена за 60 секунд до истечения
      setInterval(() => {
        keycloakInstance?.updateToken(30).catch(err => {
          console.error('Failed to refresh token', err);
        });
      }, 60000);
    }

    isInitialized = true;
    return authenticated;
  } catch (error) {
    console.error('Keycloak init failed:', error);

    // Более детальная обработка ошибок
    if (error instanceof Error) {
      if (error.message.includes('NetworkError') || error.message.includes('Failed to fetch')) {
        console.error('Network error: Keycloak server is unreachable');
      } else if (error.message.includes('timeout')) {
        console.error('Keycloak timeout: Server is not responding');
      } else if (error.message.includes('CORS')) {
        console.error('CORS error: Check Keycloak server configuration');
      }
    }

    isInitialized = true;
    return false;
  }
};

export const login = (): void => {
  keycloakInstance?.login();
};

export const logout = (): void => {
  if (process.env.NODE_ENV === 'development') {
    localStorage.clear();
  }
  sessionStorage.clear();
  keycloakInstance?.logout({ redirectUri: window.location.origin });
};

export const getToken = (): string | undefined => {
  return keycloakInstance?.token;
};

export const getRefreshToken = (): string | undefined => {
  return keycloakInstance?.refreshToken;
};

export const updateToken = async (): Promise<boolean> => {
  if (keycloakInstance) {
    try {
      return await keycloakInstance.updateToken(30);
    } catch (error) {
      console.error('Failed to update token:', error);
      // Если токен не удалось обновить, пробуем перелогиниться
      if (error instanceof Error && error.message.includes('expired')) {
        console.warn('Token expired, attempting to re-login');
        login();
      }
      return false;
    }
  }
  return false;
};

export const getUsername = (): string => {
  return username;
};

export const getUserId = (): string | undefined => {
  return keycloakInstance?.tokenParsed?.sub;
};

export const getUserRoles = (): string[] => {
  return userRoles;
};

export const isTokenExpired = (): boolean | undefined => {
  return keycloakInstance?.isTokenExpired();
};

export const getKeycloakInstance = (): KeycloakInstance | null => {
  return keycloakInstance;
};

export const isAuthenticated = (): boolean => {
  return keycloakInstance?.authenticated || false;
};

export const hasRole = (role: string): boolean => {
  return userRoles.includes(role);
};

export const hasAnyRole = (roles: string[]): boolean => {
  return roles.some(role => userRoles.includes(role));
};

export const hasAllRoles = (roles: string[]): boolean => {
  return roles.every(role => userRoles.includes(role));
};

export default {
  init: initKeycloak,
  logout,
  login,
  getToken,
  getRefreshToken,
  updateToken,
  getUsername,
  getUserId,
  getUserRoles,
  isTokenExpired,
  getKeycloakInstance,
  isAuthenticated,
  hasRole,
  hasAnyRole,
  hasAllRoles
};