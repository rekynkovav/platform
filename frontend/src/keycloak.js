import Keycloak from 'keycloak-js';

const DEBUG = process.env.NODE_ENV === 'development';
const log = (...args) => { if (DEBUG) // console.log('[KC]', ...args); };
const warn = (...args) => { if (DEBUG) console.warn('[KC]', ...args); };
const error = (...args) => console.error('[KC]', ...args);

const keycloakConfig = {
    url: process.env.REACT_APP_KEYCLOAK_URL || 'https://auth.platform.local/auth',
    realm: 'platform',
    clientId: 'platform-frontend'
};

const REDIRECT_URI = window.location.origin + '/';

/** @type {import("keycloak-js").default | null} */
let keycloakInstance = null;
let isInitialized = false;
let userRoles = [];
let username = '';
let tokenRefreshIntervalId = null;

export const initKeycloak = async () => {
    log('initKeycloak called, isInitialized:', isInitialized);
    log('Current URL:', window.location.href);
    log('Config:', keycloakConfig);

    if (isInitialized) {
        log('Already initialized, returning:', keycloakInstance?.authenticated);
        return keycloakInstance?.authenticated || false;
    }

    keycloakInstance = new Keycloak(keycloakConfig);
    log('New Keycloak instance created');

    // Monkey-patch: intercept login() to force correct redirectUri
    const originalLogin = keycloakInstance.login.bind(keycloakInstance);
    keycloakInstance.login = (options) => {
        log('login() called with options:', options);
        log('Using redirectUri:', REDIRECT_URI);
        return originalLogin({
            ...(options || {}),
            redirectUri: REDIRECT_URI
        });
    };

    try {
        log('Calling init() with onLoad: login-required');
        const authenticated = await keycloakInstance.init({
            onLoad: 'login-required',
            redirectUri: REDIRECT_URI,
            pkceMethod: 'S256',
            checkLoginIframe: false
        });

        log('init() returned:', authenticated);
        log('keycloak.authenticated:', keycloakInstance.authenticated);
        log('keycloak.token:', keycloakInstance.token ? 'present' : 'null');
        log('keycloak.tokenParsed:', keycloakInstance.tokenParsed);

        if (authenticated) {
            const token = keycloakInstance.tokenParsed;
            if (token?.realm_access?.roles) {
                userRoles = token.realm_access.roles;
            }
            username = token?.preferred_username || token?.email || 'User';
            log('Authenticated! User:', username, 'Roles:', userRoles);

            if (tokenRefreshIntervalId) {
                clearInterval(tokenRefreshIntervalId);
            }

            tokenRefreshIntervalId = setInterval(() => {
                keycloakInstance.updateToken(30).catch(err => {
                    error('Failed to refresh token:', err);
                });
            }, 60000);

            keycloakInstance.onAuthRefreshError = () => {
                error('Auth refresh error - session may be invalid');
                logout();
            };

            keycloakInstance.onTokenExpired = () => {
                warn('Token expired, attempting refresh');
                keycloakInstance?.updateToken(30)
                    .then((refreshed) => {
                        if (!refreshed) {
                            warn('Could not refresh, logging out');
                            logout();
                        } else {
                            log('Token refreshed successfully');
                            // Re-extract roles
                            const newRoles = keycloakInstance?.tokenParsed?.realm_access?.roles || [];
                            userRoles = newRoles;
                            username = keycloakInstance?.tokenParsed?.preferred_username || username;
                        }
                    })
                    .catch((err) => {
                        const isNetworkError = err?.message?.includes('Failed to fetch') ||
                                               err?.message?.includes('NetworkError') ||
                                               err?.message?.includes('timeout') ||
                                               err?.name === 'TypeError';
                        if (isNetworkError) {
                            warn('Network error during token refresh, will retry on next interval');
                            // Don't logout - interval will retry
                        } else {
                            error('Auth error during token refresh:', err);
                            logout();
                        }
                    });
            };
        } else {
            log('NOT authenticated - will show login screen');
        }

        // Clean URL from auth params after callback
        const hasAuthParams = window.location.search.includes('code=') ||
                              window.location.search.includes('state=') ||
                              window.location.hash.includes('code=') ||
                              window.location.hash.includes('state=');
        log('Has auth params in URL:', hasAuthParams);
        if (hasAuthParams) {
            log('Cleaning URL with replaceState');
            window.history.replaceState({}, document.title, window.location.pathname);
        }

        isInitialized = true;
        return authenticated;
    } catch (err) {
        error('init() FAILED:', err.message);
        keycloakInstance = null; // discard broken instance
        // Do NOT set isInitialized = true, allow retry
        return false;
    }
};

export const login = () => {
    log('login() exported called');
    keycloakInstance?.login({ redirectUri: REDIRECT_URI });
};

export const logout = () => {
    if (tokenRefreshIntervalId) {
        clearInterval(tokenRefreshIntervalId);
        tokenRefreshIntervalId = null;
    }
    // Reset in-memory state
    userRoles = [];
    username = '';
    const kc = keycloakInstance;
    isInitialized = false;
    keycloakInstance = null;
    // Clear storage
    if (process.env.NODE_ENV === 'development') {
        localStorage.clear();
    }
    sessionStorage.clear();
    // Redirect to Keycloak logout
    kc?.logout({ redirectUri: window.location.origin });
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
        } catch (err) {
            error('Failed to update token:', err);
            if (err instanceof Error && err.message.includes('expired')) {
                warn('Token expired, attempting to re-login');
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
