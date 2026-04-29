import { storage } from '../../utils/index.js';
import { navigation } from '../../routes/navigation.js';
import { request } from '../../api/core/request.js';
import { isSuccessCode } from '../../types/index.js';
import { useAuthStore } from '../../store/authStore.js';

const TOKEN_STORAGE_KEY = 'token';
const TOKEN_EXPIRES_KEY = 'token_expires';
const USER_STORAGE_KEY = 'user';

const TOKEN_EXPIRES_IN_MINUTES = 30;
const TOKEN_REFRESH_BEFORE_MINUTES = 5;

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
let refreshTimer: ReturnType<typeof setTimeout> | null = null;
let isRefreshing = false;
let refreshSubscribers: Array<(token: string) => void> = [];

export function subscribeTokenRefresh(callback: (token: string) => void): () => void {
    refreshSubscribers.push(callback);
    return () => {
        refreshSubscribers = refreshSubscribers.filter(cb => cb !== callback);
    };
}

function notifySubscribers(token: string): void {
    refreshSubscribers.forEach(cb => cb(token));
    refreshSubscribers = [];
}

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

export function getStoredTokenExpires(): number | null {
    return storage.get<number | null>(TOKEN_EXPIRES_KEY, null);
}

function scheduleTokenRefresh(expiresAt: number): void {
    if (refreshTimer) {
        clearTimeout(refreshTimer);
        refreshTimer = null;
    }

    const now = Date.now();
    const refreshTime = expiresAt - TOKEN_REFRESH_BEFORE_MINUTES * 60 * 1000;
    const delay = refreshTime - now;

    if (delay <= 0) {
        refreshAccessToken();
        return;
    }

    refreshTimer = setTimeout(() => {
        refreshAccessToken();
    }, delay);
}

function cancelScheduledRefresh(): void {
    if (refreshTimer) {
        clearTimeout(refreshTimer);
        refreshTimer = null;
    }
}

export async function refreshAccessToken(): Promise<string | null> {
    const token = getStoredToken();
    if (!token) {
        return null;
    }

    if (isRefreshing) {
        return new Promise(resolve => {
            subscribeTokenRefresh(resolve);
        });
    }

    isRefreshing = true;
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 8000);

    try {
        const response = await fetch('/api/auth/refresh', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            signal: controller.signal
        });

        if (!response.ok) {
            throw new Error('Refresh failed');
        }

        const data = await response.json() as { code: number; data?: string };

        if (!isSuccessCode(data.code) || !data.data) {
            throw new Error('Refresh response invalid');
        }

        const expiresAt = Date.now() + TOKEN_EXPIRES_IN_MINUTES * 60 * 1000;
        const user = getStoredUser() ?? { username: '', nickname: '' };
        setSession(data.data, user, expiresAt);
        notifySubscribers(data.data);
        syncTokenToStore(data.data);

        return data.data;
    } catch {
        handleUnauthorized();
        return null;
    } finally {
        clearTimeout(timeoutId);
        isRefreshing = false;
    }
}

export function setSession(token: string, user: AuthSessionUser, expiresAt?: number): void {
    storage.set(TOKEN_STORAGE_KEY, token);
    storage.set(USER_STORAGE_KEY, user);

    if (expiresAt) {
        storage.set(TOKEN_EXPIRES_KEY, expiresAt);
        scheduleTokenRefresh(expiresAt);
    } else {
        storage.remove(TOKEN_EXPIRES_KEY);
        cancelScheduledRefresh();
    }

    unauthorizedRedirectInFlight = false;
    emitAuthSessionChange();
}

export function clearSession(reason: AuthSessionClearReason = 'manual'): void {
    storage.remove(TOKEN_STORAGE_KEY);
    storage.remove(TOKEN_EXPIRES_KEY);
    storage.remove(USER_STORAGE_KEY);
    cancelScheduledRefresh();
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

export function initTokenRefresh(): void {
    const expiresAt = getStoredTokenExpires();
    if (expiresAt && expiresAt > Date.now()) {
        scheduleTokenRefresh(expiresAt);
    }
}

export function syncTokenToStore(token: string): void {
    useAuthStore.getState().setToken(token);
}
