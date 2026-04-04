// .js
import Keycloak from 'keycloak-js';

const keycloakConfig = {
    url: process.env.REACT_APP_KEYCLOAK_URL || 'https://auth.platform.com',
    realm: 'platform',
    clientId: 'platform-frontend'
};

const keycloak = new Keycloak(keycloakConfig);

let initPromise = null;

export const initKeycloak = () => {
    if (!initPromise) {
        initPromise = keycloak.init({
            onLoad: 'check-sso',
            checkLoginIframe: false,
            pkceMethod: 'S256',
            silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html'
        }).then((authenticated) => {
            console.log('Keycloak initialized, authenticated:', authenticated);
            return authenticated;
        }).catch((err) => {
            console.error('Keycloak init failed:', err);
            return false;
        });
    }
    return initPromise;
};

export const login = () => {
    keycloak.login({
        redirectUri: window.location.origin
    });
};

export const logout = () => {
    keycloak.logout({
        redirectUri: window.location.origin
    });
};

export const getToken = () => {
    return keycloak.token;
};

export const getRefreshToken = () => {
    return keycloak.refreshToken;
};

export const updateToken = (minValidity = 30) => {
    return keycloak.updateToken(minValidity);
};

export const getUsername = () => {
    return keycloak.tokenParsed?.preferred_username ||
           keycloak.tokenParsed?.email ||
           'User';
};

export const getUserId = () => {
    return keycloak.subject;
};

export const getUserRoles = () => {
    return keycloak.tokenParsed?.realm_access?.roles || [];
};

export default keycloak;