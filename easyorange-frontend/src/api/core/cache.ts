const CACHE_TTL = 5 * 60 * 1000;

interface CacheItem<T = unknown> {
    data: T;
    expireAt: number;
}

const requestCache = new Map<string, CacheItem>();

export const getCacheKey = (endpoint: string, options?: { body?: unknown; params?: Record<string, unknown> }): string => {
    const body = options?.body ? JSON.stringify(options.body) : '';
    const params = options?.params ? JSON.stringify(options.params) : '';
    return `${endpoint}:${body}:${params}`;
};

export const getFromCache = <T>(key: string): T | null => {
    const cached = requestCache.get(key);
    if (!cached) {return null;}
    if (Date.now() > cached.expireAt) {
        requestCache.delete(key);
        return null;
    }
    return cached.data as T;
};

export const setToCache = <T>(key: string, data: T): void => {
    requestCache.set(key, { data, expireAt: Date.now() + CACHE_TTL });
};

export const clearCache = (pattern?: string): void => {
    if (!pattern) {
        requestCache.clear();
        return;
    }
    for (const key of requestCache.keys()) {
        if (key.startsWith(pattern)) {
            requestCache.delete(key);
        }
    }
};
