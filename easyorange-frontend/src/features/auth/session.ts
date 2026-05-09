import { storage } from '@/utils';
import { request } from '@/api/core/request';
import { useAuthStore } from '@/store/authStore';

const TOKEN_STORAGE_KEY = 'token';
const TOKEN_EXPIRES_KEY = 'token_expires';
const USER_STORAGE_KEY = 'user';

const TOKEN_EXPIRES_IN_MINUTES = 30;
const TOKEN_REFRESH_BEFORE_MINUTES = 5;
const LOGIN_GRACE_PERIOD_MS = 5000;

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

class TokenRefreshManager {
    private refreshTimer: ReturnType<typeof setTimeout> | null = null;
    private isRefreshing = false;
    private refreshSubscribers: Array<(token: string) => void> = [];
    private lastLoginTimestamp = 0;
    private unauthorizedRedirectInFlight = false;

    subscribe(callback: (token: string) => void): () => void {
        this.refreshSubscribers.push(callback);
        return () => {
            this.refreshSubscribers = this.refreshSubscribers.filter(cb => cb !== callback);
        };
    }

    notifySubscribers(token: string): void {
        this.refreshSubscribers.forEach(cb => cb(token));
        this.refreshSubscribers = [];
    }

    schedule(expiresAt: number, onRefresh: () => void): void {
        if (this.refreshTimer) {
            clearTimeout(this.refreshTimer);
            this.refreshTimer = null;
        }

        const now = Date.now();
        const refreshTime = expiresAt - TOKEN_REFRESH_BEFORE_MINUTES * 60 * 1000;
        const delay = refreshTime - now;

        if (delay <= 0) {
            onRefresh();
            return;
        }

        this.refreshTimer = setTimeout(onRefresh, delay);
    }

    cancel(): void {
        if (this.refreshTimer) {
            clearTimeout(this.refreshTimer);
            this.refreshTimer = null;
        }
    }

    getIsRefreshing(): boolean {
        return this.isRefreshing;
    }

    setIsRefreshing(value: boolean): void {
        this.isRefreshing = value;
    }

    getLastLoginTimestamp(): number {
        return this.lastLoginTimestamp;
    }

    setLastLoginTimestamp(timestamp: number): void {
        this.lastLoginTimestamp = timestamp;
    }

    isUnauthorizedRedirectInFlight(): boolean {
        return this.unauthorizedRedirectInFlight;
    }

    setUnauthorizedRedirectInFlight(value: boolean): void {
        this.unauthorizedRedirectInFlight = value;
    }

    isWithinLoginGracePeriod(): boolean {
        const timeSinceLogin = Date.now() - this.lastLoginTimestamp;
        return this.lastLoginTimestamp > 0 && timeSinceLogin < LOGIN_GRACE_PERIOD_MS;
    }
}

const tokenRefreshManager = new TokenRefreshManager();

function emitAuthSessionChange(reason?: AuthSessionClearReason): void {
    const detail: AuthSessionDetail = {
        isAuthenticated: Boolean(getStoredToken() && getStoredUser()),
        token: getStoredToken(),
        user: getStoredUser(),
        reason
    };

    window.dispatchEvent(new CustomEvent<AuthSessionDetail>(AUTH_SESSION_CHANGE_EVENT, { detail }));
}

export function getStoredToken(): string | null {
    const zustandToken = useAuthStore.getState().token;
    if (zustandToken) { return zustandToken; }
    return storage.get<string | null>(TOKEN_STORAGE_KEY, null);
}

export function getStoredUser(): AuthSessionUser | null {
    const zustandUser = useAuthStore.getState().user;
    if (zustandUser) { return zustandUser as unknown as AuthSessionUser; }
    return storage.get<AuthSessionUser | null>(USER_STORAGE_KEY, null);
}

export function getStoredTokenExpires(): number | null {
    return storage.get<number | null>(TOKEN_EXPIRES_KEY, null);
}

function syncAllStores(token: string, user: AuthSessionUser, expiresAt?: number, refreshToken?: string): void {
    storage.set(TOKEN_STORAGE_KEY, token);
    storage.set(USER_STORAGE_KEY, user);

    const finalRefreshToken = refreshToken ?? useAuthStore.getState().refreshToken ?? '';
    useAuthStore.getState().login(
        user as unknown as import('../../types/index.js').User,
        token,
        finalRefreshToken
    );

    if (expiresAt) {
        storage.set(TOKEN_EXPIRES_KEY, expiresAt);
        tokenRefreshManager.schedule(expiresAt, refreshAccessToken);
    } else {
        storage.remove(TOKEN_EXPIRES_KEY);
        tokenRefreshManager.cancel();
    }
}

function clearAllStores(reason: AuthSessionClearReason = 'manual'): void {
    storage.remove(TOKEN_STORAGE_KEY);
    storage.remove(TOKEN_EXPIRES_KEY);
    storage.remove(USER_STORAGE_KEY);
    tokenRefreshManager.cancel();
    emitAuthSessionChange(reason);
}

export async function refreshAccessToken(): Promise<string | null> {
    const token = getStoredToken();
    if (!token) {
        return null;
    }

    if (tokenRefreshManager.getIsRefreshing()) {
        return new Promise(resolve => {
            tokenRefreshManager.subscribe(resolve);
        });
    }

    tokenRefreshManager.setIsRefreshing(true);

    try {
        const refreshToken = useAuthStore.getState().refreshToken;
        const response = await request<string>('/auth/refresh', {
            method: 'POST',
            body: { refreshToken },
            skipAuth: true,
            timeout: 8000,
            retries: 0,
            dedupe: false
        });

        if (!response.data) {
            throw new Error('Refresh response invalid');
        }

        const expiresAt = Date.now() + TOKEN_EXPIRES_IN_MINUTES * 60 * 1000;
        const user = getStoredUser();
        if (user) {
            syncAllStores(response.data, user, expiresAt);
        } else {
            syncAllStores(response.data, { username: '', nickname: '' }, expiresAt);
        }
        tokenRefreshManager.notifySubscribers(response.data);

        return response.data;
    } catch {
        clearAllStores('unauthorized');
        useAuthStore.setState({
            user: null,
            token: null,
            refreshToken: null,
            isAuthenticated: false,
        });
        return null;
    } finally {
        tokenRefreshManager.setIsRefreshing(false);
    }
}

export function setSession(token: string, user: AuthSessionUser, expiresAt?: number, refreshToken?: string): void {
    syncAllStores(token, user, expiresAt, refreshToken);
    tokenRefreshManager.setLastLoginTimestamp(Date.now());
    tokenRefreshManager.setUnauthorizedRedirectInFlight(false);
    emitAuthSessionChange();
}

export function clearSession(reason: AuthSessionClearReason = 'manual'): void {
    clearAllStores(reason);
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

    useAuthStore.getState().logout();
    clearSession('logout');
}

export function handleUnauthorized(): void {
    if (tokenRefreshManager.isUnauthorizedRedirectInFlight()) {
        return;
    }

    if (tokenRefreshManager.isWithinLoginGracePeriod()) {
        return;
    }

    tokenRefreshManager.setUnauthorizedRedirectInFlight(true);
    useAuthStore.getState().logout();
    clearSession('unauthorized');

    const currentPath = `${window.location.pathname}${window.location.search}`;
    const redirectQuery = currentPath && currentPath !== '/' ? { redirect: currentPath } : undefined;
    const loginPath = redirectQuery
        ? `/login?redirect=${encodeURIComponent(redirectQuery.redirect)}`
        : '/login';
    window.location.href = loginPath;
}

export function initTokenRefresh(): void {
    const expiresAt = getStoredTokenExpires();
    if (expiresAt && expiresAt > Date.now()) {
        tokenRefreshManager.schedule(expiresAt, refreshAccessToken);
    }
}

export function syncTokenToStore(token: string): void {
    useAuthStore.getState().setToken(token);
}

export { tokenRefreshManager };
