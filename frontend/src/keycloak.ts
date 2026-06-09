import Keycloak, { KeycloakInstance, KeycloakTokenParsed } from 'keycloak-js';

interface KeycloakConfig {
  url: string;
  realm: string;
  clientId: string;
}

interface KeycloakInitOptions {
  onLoad: 'check-sso' | 'login-required';
  redirectUri: string;
  pkceMethod: 'S256';
}

const keycloakConfig: KeycloakConfig = {
  url: process.env.REACT_APP_KEYCLOAK_URL || 'http://localhost:8080/auth',
  realm: 'platform',
  clientId: 'platform-frontend'
};

const REDIRECT_URI = window.location.origin + '/';

let keycloakInstance: KeycloakInstance | null = null;
let isInitialized = false;
let userRoles: string[] = [];
let username = '';
let tokenRefreshIntervalId: ReturnType<typeof setInterval> | null = null;

export const initKeycloak = async (): Promise<boolean> => {
  if (isInitialized) return keycloakInstance?.authenticated || false;

  keycloakInstance = new Keycloak(keycloakConfig);

  // keycloak-js v24: init() with onLoad:'login-required' calls kc.login({}) internally.
  // The adapter's redirectUri(options) fallback chain is:
  //   options.redirectUri → kc.redirectUri → location.href
  // If kc.redirectUri is falsy at call time, location.href is used — on the callback page
  // that contains #state=...&code=..., producing a redirect loop.
  // Fix: intercept every login() call to force the correct redirectUri.
  const originalLogin = keycloakInstance.login.bind(keycloakInstance);
  (keycloakInstance as any).login = (options?: any) => {
    return originalLogin({
      ...(options || {}),
      redirectUri: REDIRECT_URI
    });
  };

  try {
    const authenticated = await keycloakInstance.init({
      onLoad: 'login-required',
      redirectUri: REDIRECT_URI,
      pkceMethod: 'S256'
    } as KeycloakInitOptions);

    if (authenticated) {
      const token: KeycloakTokenParsed | undefined = keycloakInstance.tokenParsed;
      if (token?.realm_access?.roles) {
        userRoles = token.realm_access.roles;
      }
      username = token?.preferred_username || token?.email || 'User';

      if (tokenRefreshIntervalId) {
        clearInterval(tokenRefreshIntervalId);
      }

      tokenRefreshIntervalId = setInterval(() => {
        keycloakInstance?.updateToken(30).catch(err => {
          console.error('Failed to refresh token', err);
        });
      }, 60000);

      keycloakInstance.onTokenExpired = () => {
        console.warn('Token expired, redirecting to login');
        logout();
      };
    }

    const hasAuthParams = window.location.search.includes('code=') ||
                          window.location.search.includes('state=') ||
                          window.location.hash.includes('code=') ||
                          window.location.hash.includes('state=');
    if (hasAuthParams) {
      window.history.replaceState({}, document.title, window.location.pathname);
    }

    isInitialized = true;
    return authenticated;
  } catch (error) {
    console.error('Keycloak init failed:', error);

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
  keycloakInstance?.login({ redirectUri: REDIRECT_URI });
};

export const logout = (): void => {
  if (tokenRefreshIntervalId) {
    clearInterval(tokenRefreshIntervalId);
    tokenRefreshIntervalId = null;
  }
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
