import { storage } from '../utils/index.js';
import { navigation } from './navigation.js';
import { request } from '../api/core/request.js';

const TOKEN_STORAGE_KEY = 'token';
const USER_STORAGE_KEY = 'user';

export const AUTH_SESSION_CHANGE_EVENT = 'auth-session-change';

export interface AuthSessionUser {
    username?: string;
    nickname?: string;
    [key: string]: unknown;
}

export type AuthSessionClearReason = 'logout' | 'unauthorized' | 'manual';

export interface AuthSessionDetail {
    isAuthenticated: boolean;
    token: string | null;
    user: AuthSessionUser | null;
    reason?: AuthSessionClearReason;
}

let unauthorizedRedirectInFlight = false;

function emitAuthSessionChange(reason?: AuthSessionClearReason): void {
    const detail: AuthSessionDetail = {
        isAuthenticated: Boolean(getStoredToken() && getStoredUser()),
        token: getStoredToken(),
        user: getStoredUser(),
        reason
    };

    window.dispatchEvent(new CustomEvent<AuthSessionDetail>(AUTH_SESSION_CHANGE_EVENT, { detail }));
}

function getUnauthorizedRedirectQuery(): Record<string, string> | undefined {
    const currentPath = `${window.location.pathname}${window.location.search}`;
    if (!currentPath || currentPath === '/') {
        return undefined;
    }
    return { redirect: currentPath };
}

export function getStoredToken(): string | null {
    return storage.get<string | null>(TOKEN_STORAGE_KEY, null);
}

export function getStoredUser(): AuthSessionUser | null {
    return storage.get<AuthSessionUser | null>(USER_STORAGE_KEY, null);
}

export function setSession(token: string, user: AuthSessionUser): void {
    storage.set(TOKEN_STORAGE_KEY, token);
    storage.set(USER_STORAGE_KEY, user);
    unauthorizedRedirectInFlight = false;
    emitAuthSessionChange();
}

export function clearSession(reason: AuthSessionClearReason = 'manual'): void {
    storage.remove(TOKEN_STORAGE_KEY);
    storage.remove(USER_STORAGE_KEY);
    emitAuthSessionChange(reason);
}

export async function logout(): Promise<void> {
    const token = getStoredToken();

    if (token) {
        try {
            await request('/auth/logout', {
                method: 'POST'
            });
        } catch {
            // Ignore API errors - ensure local session is cleared regardless
        }
    }

    clearSession('logout');
}

export function handleUnauthorized(): void {
    if (unauthorizedRedirectInFlight) {
        return;
    }

    unauthorizedRedirectInFlight = true;
    clearSession('unauthorized');
    navigation.replace('home', getUnauthorizedRedirectQuery());
}
