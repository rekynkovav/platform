import Keycloak from 'keycloak-js';

const keycloakConfig = {
  url: process.env.REACT_APP_KEYCLOAK_URL || 'http://localhost:8180',
  realm: process.env.REACT_APP_KEYCLOAK_REALM || 'platform',
  clientId: process.env.REACT_APP_KEYCLOAK_CLIENT || 'platform-frontend',
};

let keycloak: Keycloak | null = null;

export const initKeycloak = async (): Promise<boolean> => {
  if (keycloak) {
    return keycloak.authenticated || false;
  }

  keycloak = new Keycloak(keycloakConfig);
  
  try {
    const authenticated = await keycloak.init({
      onLoad: 'login-required',
      checkLoginIframe: false,
    });
    return authenticated;
  } catch (error) {
    console.error('Keycloak init failed:', error);
    return false;
  }
};

export const getUsername = (): string => {
  return keycloak?.tokenParsed?.preferred_username || 'User';
};

export const getUserRoles = (): string[] => {
  return keycloak?.tokenParsed?.realm_access?.roles || [];
};

export const getToken = (): string | undefined => {
  return keycloak?.token;
};

export const login = (): void => {
  keycloak?.login();
};

export const logout = (): void => {
  keycloak?.logout();
};

export const isAuthenticated = (): boolean => {
  return keycloak?.authenticated || false;
};
