/**
 * Keycloak Authentication Module
 *
 * Provides a singleton wrapper around the Keycloak JS adapter for the platform.
 * Manages the full auth lifecycle — initialization, login, logout, token refresh,
 * role extraction, and session state.
 *
 * @module keycloak
 *
 * Key responsibilities:
 *  - Initializes the Keycloak client with PKCE (S256) on app mount
 *  - Extracts realm roles and username from the parsed JWT
 *  - Maintains automatic token refresh every 60 seconds
 *  - Handles auth errors (network failures, expired tokens, refresh errors)
 *  - Cleans OAuth callback parameters from the URL after login
 *  - Exposes helper functions for permission checks (hasRole, hasAnyRole, hasAllRoles)
 */
import Keycloak from 'keycloak-js';

const DEBUG = process.env.NODE_ENV === 'development';
const DEV_MOCK_AUTH = process.env.REACT_APP_DEV_MOCK_AUTH === 'true';
const log = (...args) => { if (DEBUG) console.log('[KC]', ...args); };
const warn = (...args) => { if (DEBUG) console.warn('[KC]', ...args); };
const error = (...args) => console.error('[KC]', ...args);

const keycloakConfig = {
    url: process.env.REACT_APP_KEYCLOAK_URL || (window.location.origin + '/auth'),
    realm: process.env.REACT_APP_KEYCLOAK_REALM || 'platform',
    clientId: process.env.REACT_APP_KEYCLOAK_CLIENT_ID || 'platform-frontend'
};

const REDIRECT_URI = window.location.origin + '/';

/** @type {import("keycloak-js").default | null} */
let keycloakInstance = null;
let isInitialized = false;
let userRoles = [];
let username = '';
let tokenRefreshIntervalId = null;

/**
 * Initializes the Keycloak client and authenticates the user.
 * - Creates a new Keycloak instance on first call (singleton)
 * - Monkey-patches login() to enforce the correct redirectUri
 * - Uses PKCE S256, disables checkLoginIframe (avoids CSP iframe errors)
 * - On success: extracts realm roles and username, starts periodic token refresh
 * - On failure: discards the broken instance without setting isInitialized (allows retry)
 *
 * @async
 * @function initKeycloak
 * @returns {Promise<boolean>} true if the user has an active session, false otherwise
 */
export const initKeycloak = async () => {
    log('initKeycloak called, isInitialized:', isInitialized);

    // DEV MOCK AUTH: skip Keycloak entirely — no redirect, no network
    // Read role from RoleSwitcher (sessionStorage) or default to SUPER_ADMIN for dev
    if (DEV_MOCK_AUTH) {
        warn('DEV MOCK AUTH: skipping Keycloak, using mock auth');
        const demoRole = sessionStorage.getItem('demoRole') || 'SUPER_ADMIN';
        userRoles = [demoRole];
        username = 'dev-user';
        keycloakInstance = {
            authenticated: true,
            token: 'mock-dev-token',
            tokenParsed: {
                realm_access: { roles: userRoles },
                preferred_username: username,
                sub: 'dev-user-id'
            },
            login: () => Promise.resolve(),
            logout: () => Promise.resolve(),
            updateToken: () => Promise.resolve(true),
            isTokenExpired: () => false,
        };
        isInitialized = true;
        return true;
    }

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
        } else {
            log('NOT authenticated');
            // DEV MOCK AUTH: auto-login when Keycloak is up but user isn't logged in
            if (DEV_MOCK_AUTH) {
                warn('DEV MOCK AUTH: auto-login with mock roles');
                const demoRole2 = sessionStorage.getItem('demoRole') || 'SUPER_ADMIN';
                userRoles = [demoRole2];
                username = 'dev-user';
                keycloakInstance.authenticated = true;
                keycloakInstance.token = 'mock-dev-token';
                keycloakInstance.tokenParsed = {
                    realm_access: { roles: userRoles },
                    preferred_username: username,
                    sub: 'dev-user-id'
                };
                isInitialized = true;
                return true;
            }
        }

        if (authenticated) {
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
                        } else {
                            error('Auth error during token refresh:', err);
                            logout();
                        }
                    });
            };
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

        // DEV MOCK AUTH: if Keycloak is unreachable, auto-authenticate with mock roles
        if (DEV_MOCK_AUTH) {
            warn('DEV MOCK AUTH: Keycloak unavailable, using mock authentication');
            const demoRole3 = sessionStorage.getItem('demoRole') || 'SUPER_ADMIN';
            userRoles = [demoRole3];
            username = 'dev-user';
            isInitialized = true;
            // Make the mock keycloakInstance look authenticated
            keycloakInstance = keycloakInstance || {};
            keycloakInstance.authenticated = true;
            keycloakInstance.token = 'mock-dev-token';
            keycloakInstance.tokenParsed = {
                realm_access: { roles: userRoles },
                preferred_username: username,
                sub: 'dev-user-id'
            };
            return true;
        }

        keycloakInstance = null; // discard broken instance
        // DO NOT set isInitialized = true, allow retry
        return false;
    }
};

/**
 * Redirects the browser to the Keycloak login page.
 * @function login
 */
export const login = () => {
    log('login() exported called');
    keycloakInstance?.login({ redirectUri: REDIRECT_URI });
};

/**
 * Logs the user out and clears all local/session storage.
 * - Clears the token refresh interval
 * - Resets in-memory state (userRoles, username, keycloakInstance)
 * - Redirects to Keycloak's logout endpoint
 */
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

/**
 * Attempts to refresh the Keycloak token, re-logging in if it has expired.
 * @async
 * @function updateToken
 * @returns {Promise<boolean>} true if the token was refreshed successfully, false otherwise
 */
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
