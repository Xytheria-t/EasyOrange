import { routes, type RouteName } from './routeConfig.js';
import { storage } from '../utils/index.js';

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
    return Boolean(storage.get<string>('token'));
}

function currentFullPath(): string {
    return `${window.location.pathname}${window.location.search}`;
}

export const navigation = {
    go(routeName: RouteName, options: NavigationOptions = {}): void {
        const route = routes[routeName];
        const targetUrl = buildUrl(route.path, options.query);

        if (route.requiresAuth && !isLoggedIn()) {
            const loginUrl = buildUrl('/', {
                redirect: targetUrl
            });
            window.location.assign(loginUrl);
            return;
        }

        if (options.replace) {
            window.location.replace(targetUrl);
            return;
        }

        window.location.assign(targetUrl);
    },

    replace(routeName: RouteName, query?: Record<string, QueryValue>): void {
        this.go(routeName, { replace: true, query });
    },

    loginRedirect(): void {
        const params = new URLSearchParams(window.location.search);
        const redirect = params.get('redirect');
        window.location.replace(redirect || routes.products.path);
    },

    requireAuth(): boolean {
        if (isLoggedIn()) {
            return true;
        }

        const loginUrl = buildUrl('/', {
            redirect: currentFullPath()
        });
        window.location.replace(loginUrl);
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

        if (mode === 'push') {
            window.history.pushState({}, '', url);
            return;
        }

        window.history.replaceState({}, '', url);
    }
};
