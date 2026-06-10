import Keycloak from 'keycloak-js';

const keycloakConfig = {
    url: process.env.REACT_APP_KEYCLOAK_URL || 'http://localhost:8080/auth',
    realm: 'platform',
    clientId: 'platform-frontend'
};

const REDIRECT_URI = window.location.origin + '/';

let keycloakInstance = null;
let isInitialized = false;
let userRoles = [];
let username = '';
let tokenRefreshIntervalId = null;

export const initKeycloak = async () => {
    console.log('[KC] initKeycloak called, isInitialized:', isInitialized);
    console.log('[KC] Current URL:', window.location.href);
    console.log('[KC] Config:', keycloakConfig);

    if (isInitialized) {
        console.log('[KC] Already initialized, returning:', keycloakInstance?.authenticated);
        return keycloakInstance?.authenticated || false;
    }

    keycloakInstance = new Keycloak(keycloakConfig);
    console.log('[KC] New Keycloak instance created');

    // Monkey-patch: intercept login() to force correct redirectUri
    const originalLogin = keycloakInstance.login.bind(keycloakInstance);
    keycloakInstance.login = (options) => {
        console.log('[KC] login() called with options:', options);
        console.log('[KC] Using redirectUri:', REDIRECT_URI);
        return originalLogin({
            ...(options || {}),
            redirectUri: REDIRECT_URI
        });
    };

    try {
        console.log('[KC] Calling init() with onLoad: login-required');
        const authenticated = await keycloakInstance.init({
            onLoad: 'login-required',
            redirectUri: REDIRECT_URI,
            pkceMethod: 'S256',
            checkLoginIframe: false
        });

        console.log('[KC] init() returned:', authenticated);
        console.log('[KC] keycloak.authenticated:', keycloakInstance.authenticated);
        console.log('[KC] keycloak.token:', keycloakInstance.token ? 'present' : 'null');
        console.log('[KC] keycloak.tokenParsed:', keycloakInstance.tokenParsed);

        if (authenticated) {
            const token = keycloakInstance.tokenParsed;
            if (token?.realm_access?.roles) {
                userRoles = token.realm_access.roles;
            }
            username = token?.preferred_username || token?.email || 'User';
            console.log('[KC] Authenticated! User:', username, 'Roles:', userRoles);

            if (tokenRefreshIntervalId) {
                clearInterval(tokenRefreshIntervalId);
            }

            tokenRefreshIntervalId = setInterval(() => {
                keycloakInstance.updateToken(30).catch(err => {
                    console.error('[KC] Failed to refresh token:', err);
                });
            }, 60000);

            keycloakInstance.onTokenExpired = () => {
                console.warn('[KC] Token expired, redirecting to login');
                logout();
            };
        } else {
            console.log('[KC] NOT authenticated - will show login screen');
        }

        // Clean URL from auth params after callback
        const hasAuthParams = window.location.search.includes('code=') ||
                              window.location.search.includes('state=') ||
                              window.location.hash.includes('code=') ||
                              window.location.hash.includes('state=');
        console.log('[KC] Has auth params in URL:', hasAuthParams);
        if (hasAuthParams) {
            console.log('[KC] Cleaning URL with replaceState');
            window.history.replaceState({}, document.title, window.location.pathname);
        }

        isInitialized = true;
        return authenticated;
    } catch (error) {
        console.error('[KC] init() FAILED:', error);
        console.error('[KC] Error message:', error.message);
        console.error('[KC] Error stack:', error.stack);
        isInitialized = true;
        return false;
    }
};

export const login = () => {
    console.log('[KC] login() exported called');
    keycloakInstance?.login({ redirectUri: REDIRECT_URI });
};

export const logout = () => {
    console.log('[KC] logout() called');
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

export const getToken = () => keycloakInstance?.token;
export const getRefreshToken = () => keycloakInstance?.refreshToken;
export const getUsername = () => username;
export const getUserId = () => keycloakInstance?.tokenParsed?.sub;
export const getUserRoles = () => userRoles;
export const isTokenExpired = () => keycloakInstance?.isTokenExpired();
export const getKeycloakInstance = () => keycloakInstance;
export const isAuthenticated = () => keycloakInstance?.authenticated || false;
export const hasRole = (role) => userRoles.includes(role);
export const hasAnyRole = (roles) => roles.some(role => userRoles.includes(role));
export const hasAllRoles = (roles) => roles.every(role => userRoles.includes(role));

export const updateToken = async () => {
    if (keycloakInstance) {
        try {
            return await keycloakInstance.updateToken(30);
        } catch (error) {
            console.error('[KC] Failed to update token:', error);
            if (error instanceof Error && error.message.includes('expired')) {
                console.warn('[KC] Token expired, attempting to re-login');
                login();
            }
            return false;
        }
    }
    return false;
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
