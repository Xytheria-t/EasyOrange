import { routes, type RouteName } from './config.js';
import { router } from './index.js';
import { useAuthStore } from '../store/authStore.js';

export type QueryValue = string | number | boolean | null | undefined;

export interface NavigationOptions {
    replace?: boolean;
    query?: Record<string, QueryValue>;
}

function buildUrl(path: string, query?: Record<string, QueryValue>): string {
    const params = new URLSearchParams();

    Object.entries(query ?? {}).forEach(([key, value]) => {
        if (value === null || value === undefined || value === '') {
            return;
        }
        params.set(key, String(value));
    });

    const queryString = params.toString();
    return queryString ? `${path}?${queryString}` : path;
}

function isLoggedIn(): boolean {
    return Boolean(useAuthStore.getState().token);
}

function currentFullPath(): string {
    return `${window.location.pathname}${window.location.search}`;
}

export const navigation = {
    go(routeName: RouteName, options: NavigationOptions = {}): void {
        const route = routes[routeName];
        const targetUrl = buildUrl(route.path, options.query);

        if (route.requiresAuth && !isLoggedIn()) {
            const loginUrl = buildUrl('/login', {
                redirect: targetUrl
            });
            router.navigate(loginUrl);
            return;
        }

        if (options.replace) {
            router.navigate(targetUrl, { replace: true });
            return;
        }

        router.navigate(targetUrl);
    },

    replace(routeName: RouteName, query?: Record<string, QueryValue>): void {
        this.go(routeName, { replace: true, query });
    },

    loginRedirect(): void {
        const params = new URLSearchParams(window.location.search);
        const redirect = params.get('redirect');
        router.navigate(redirect || routes.products.path, { replace: true });
    },

    requireAuth(): boolean {
        if (isLoggedIn()) {
            return true;
        }

        const loginUrl = buildUrl('/login', {
            redirect: currentFullPath()
        });
        router.navigate(loginUrl, { replace: true });
        return false;
    },

    updateQuery(query: Record<string, QueryValue>, mode: 'push' | 'replace' = 'replace'): void {
        const params = new URLSearchParams(window.location.search);

        Object.entries(query).forEach(([key, value]) => {
            if (value === null || value === undefined || value === '') {
                params.delete(key);
                return;
            }
            params.set(key, String(value));
        });

        const queryString = params.toString();
        const url = queryString
            ? `${window.location.pathname}?${queryString}`
            : window.location.pathname;

        router.navigate(url, { replace: mode === 'replace' });
    }
};
