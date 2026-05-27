import Keycloak from 'keycloak-js';

const keycloakConfig = {
    url: process.env.REACT_APP_KEYCLOAK_URL || 'http://localhost:8080/auth',
    realm: 'platform',
    clientId: 'platform-frontend'
};

let keycloakInstance = null;
let isInitialized = false;
let userRoles = [];
let username = '';

export const initKeycloak = async () => {
    if (isInitialized) return keycloakInstance?.authenticated || false;

    keycloakInstance = new Keycloak(keycloakConfig);

    try {
        const authenticated = await keycloakInstance.init({
            onLoad: 'check-sso',
            silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html',
            pkceMethod: 'S256'
        });

        if (authenticated) {
            const token = keycloakInstance.tokenParsed;
            if (token?.realm_access?.roles) {
                userRoles = token.realm_access.roles;
            }
            username = token?.preferred_username || token?.email || 'User';

            // Обновление токена за 60 секунд до истечения
            setInterval(() => {
                keycloakInstance.updateToken(30).catch(err => {
                    console.error('Failed to refresh token', err);
                });
            }, 60000);
        }

        isInitialized = true;
        return authenticated;
    } catch (error) {
        console.error('Keycloak init failed:', error);
        isInitialized = true;
        return false;
    }
};

export const login = () => keycloakInstance?.login();
export const logout = () => {
    if (process.env.NODE_ENV === 'development') {
      localStorage.clear();
    }
    sessionStorage.clear();
    keycloakInstance?.logout({ redirectUri: window.location.origin });
};
export const getToken = () => keycloakInstance?.token;
export const getRefreshToken = () => keycloakInstance?.refreshToken;
export const updateToken = async () => {
    if (keycloakInstance) {
        return keycloakInstance.updateToken(30);
    }
    return false;
};
export const getUsername = () => username;
export const getUserId = () => keycloakInstance?.tokenParsed?.sub;
export const getUserRoles = () => userRoles;
export const isTokenExpired = () => keycloakInstance?.isTokenExpired();

export default { init: initKeycloak, logout, login };