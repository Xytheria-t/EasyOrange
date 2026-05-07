import { isSuccessCode, type ApiCode, type Result, type RequestOptions } from '@/types';
import { refreshAccessToken, getStoredToken, handleUnauthorized } from '@/features/auth/session';

const API_BASE_URL = '/api';
const DEFAULT_TIMEOUT = 10000;
const DEFAULT_RETRIES = 2;
const RETRY_DELAY_BASE = 1000;
const CACHE_TTL = 5 * 60 * 1000;
const CLIENT_TYPE = 'web';

interface PendingRequest {
    controller: AbortController;
    timestamp: number;
}

interface CacheItem<T = unknown> {
    data: T;
    expireAt: number;
}

interface RequestConfig extends RequestInit {
    headers: Record<string, string>;
}

type RequestInterceptor = (config: RequestConfig) => RequestConfig | Promise<RequestConfig>;
type ResponseInterceptor = (response: Response) => Response | Promise<Response>;

const requestManager = {
    pendingRequests: new Map<string, PendingRequest>(),
    dedupeWindow: 100,

    generateKey(endpoint: string, options: { method?: string; body?: unknown } = {}): string {
        const method = options.method || 'GET';
        const body = options.body ? JSON.stringify(options.body) : '';
        return `${method}:${endpoint}:${body}`;
    },

    isDuplicate(key: string): boolean {
        const pending = this.pendingRequests.get(key);
        if (!pending) {return false;}
        return Date.now() - pending.timestamp < this.dedupeWindow;
    },

    startTracking(key: string, controller: AbortController): void {
        const existing = this.pendingRequests.get(key);
        if (existing?.controller) {
            existing.controller.abort();
        }
        this.pendingRequests.set(key, { controller, timestamp: Date.now() });
    },

    stopTracking(key: string): void {
        this.pendingRequests.delete(key);
    },

    cancel(key: string, reason = '请求已取消'): void {
        const pending = this.pendingRequests.get(key);
        if (pending?.controller) {
            pending.controller.abort(reason);
            this.pendingRequests.delete(key);
        }
    },

    cancelAll(reason = '所有请求已取消'): void {
        this.pendingRequests.forEach((pending) => {
            pending.controller?.abort(reason);
        });
        this.pendingRequests.clear();
    },

    cancelByPattern(pattern: RegExp | string, reason = '请求已取消'): void {
        const regex = pattern instanceof RegExp ? pattern : new RegExp(pattern);
        this.pendingRequests.forEach((pending, key) => {
            if (regex.test(key)) {
                pending.controller?.abort(reason);
                this.pendingRequests.delete(key);
            }
        });
    }
};

const buildQueryParams = (params: Record<string, unknown>): string => {
    const filtered = Object.entries(params)
        .filter(([, v]) => v !== null && v !== undefined && v !== '');
    if (filtered.length === 0) {return '';}
    return `?${  new URLSearchParams(
        filtered.map(([k, v]) => [k, String(v)])
    ).toString()}`;
};

const requestCache = new Map<string, CacheItem>();

const getCacheKey = (endpoint: string, options?: { body?: unknown; params?: Record<string, unknown> }): string => {
    const body = options?.body ? JSON.stringify(options.body) : '';
    const params = options?.params ? JSON.stringify(options.params) : '';
    return `${endpoint}:${body}:${params}`;
};

const getFromCache = <T>(key: string): T | null => {
    const cached = requestCache.get(key);
    if (!cached) {return null;}
    if (Date.now() > cached.expireAt) {
        requestCache.delete(key);
        return null;
    }
    return cached.data as T;
};

const setToCache = <T>(key: string, data: T): void => {
    requestCache.set(key, { data, expireAt: Date.now() + CACHE_TTL });
};

const clearCache = (pattern?: string): void => {
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

const requestInterceptors: RequestInterceptor[] = [];
const responseInterceptors: ResponseInterceptor[] = [];

const addRequestInterceptor = (interceptor: RequestInterceptor): (() => void) => {
    requestInterceptors.push(interceptor);
    return () => {
        const index = requestInterceptors.indexOf(interceptor);
        if (index > -1) {requestInterceptors.splice(index, 1);}
    };
};

const addResponseInterceptor = (interceptor: ResponseInterceptor): (() => void) => {
    responseInterceptors.push(interceptor);
    return () => {
        const index = responseInterceptors.indexOf(interceptor);
        if (index > -1) {responseInterceptors.splice(index, 1);}
    };
};

class ApiClientError extends Error {
    status: ApiCode;
    details: unknown;

    constructor(message: string, status: ApiCode = 0, details?: unknown) {
        super(message);
        this.name = 'ApiClientError';
        this.status = status;
        this.details = details;
    }
}

const escapeHtml = (str: string): string => {
    const htmlEscapes: Record<string, string> = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;'
    };
    return str.replace(/[&<>"']/g, (ch) => htmlEscapes[ch]);
};

const parseError = async (response: Response): Promise<ApiClientError> => {
    let message = `HTTP error! status: ${response.status}`;
    let details: unknown = null;

    try {
        const body = await response.json() as { message?: string; msg?: string; data?: unknown; errors?: unknown; code?: string | number };
        
        if (body?.message || body?.msg) {
            message = escapeHtml(String(body.message ?? body.msg));
        }
        details = body?.data ?? body?.errors ?? null;
    } catch {
        try {
            await response.text();
        } catch {
            // ignore
        }
    }

    return new ApiClientError(message, response.status, details);
};

const isRetryable = (status: ApiCode): boolean => {
    if (typeof status !== 'number') {
        return false;
    }
    return status === 0 || status === 408 || status === 429 || status >= 500;
};

const PUBLIC_ENDPOINTS = new Set(['/auth/login', '/auth/logout', '/auth/register', '/auth/password-reset', '/auth/sms-code']);

const shouldHandleUnauthorized = (endpoint: string, skipAuth: boolean): boolean => {
    if (skipAuth) {
        return false;
    }

    const normalizedPath = endpoint.split('?')[0].toLowerCase();
    return !PUBLIC_ENDPOINTS.has(normalizedPath);
};

async function request<T = unknown>(endpoint: string, options: RequestOptions = {}): Promise<Result<T>> {
    const {
        method = 'GET',
        headers = {},
        body,
        params,
        timeout = DEFAULT_TIMEOUT,
        retries = DEFAULT_RETRIES,
        cache = false,
        signal,
        dedupe = true,
        skipAuth = false
    } = options;

    const queryString = params ? buildQueryParams(params) : '';
    const url = `${API_BASE_URL}${endpoint}${queryString}`;
    const cacheKey = getCacheKey(endpoint, { body, params });
    const requestKey = requestManager.generateKey(endpoint, { method, body });

    if (cache && method === 'GET') {
        const cachedData = getFromCache<Result<T>>(cacheKey);
        if (cachedData) {
            return cachedData;
        }
    }

    if (dedupe && requestManager.isDuplicate(requestKey)) {
        throw new ApiClientError('重复请求已取消', 0);
    }

    let config: RequestConfig = {
        method,
        headers: {
            'X-Client-Type': String(CLIENT_TYPE),
            ...headers
        }
    };

    if (body && method !== 'GET') {
        if (body instanceof FormData) {
            config.body = body;
        } else {
            config.body = JSON.stringify(body);
            if (!config.headers['Content-Type']) {
                config.headers['Content-Type'] = 'application/json';
            }
        }
    }

    if (!skipAuth) {
        const token = getStoredToken();
        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`;
        }
    }

    for (const interceptor of requestInterceptors) {
        config = await interceptor(config);
    }

    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort('请求超时'), timeout);

    let combinedSignal: AbortSignal = controller.signal;
    if (signal) {
        if (signal.aborted) {
            clearTimeout(timeoutId);
            throw new ApiClientError('请求已取消', 0);
        }

        const combinedController = new AbortController();
        const abortHandler = () => combinedController.abort();
        signal.addEventListener('abort', abortHandler);
        controller.signal.addEventListener('abort', abortHandler);
        combinedSignal = combinedController.signal;
    }

    config.signal = combinedSignal;

    if (dedupe) {
        requestManager.startTracking(requestKey, controller);
    }

    let lastError: Error | null = null;
    let attempt = 0;
    let authRefreshAttempts = 0;
    const MAX_AUTH_REFRESH_ATTEMPTS = 1;

    try {
        while (attempt <= retries) {
            try {
                const response = await fetch(url, config);

                let processedResponse = response;
                for (const interceptor of responseInterceptors) {
                    processedResponse = await interceptor(processedResponse);
                }

                if (!processedResponse.ok) {
                    if (processedResponse.status === 401 && shouldHandleUnauthorized(endpoint, skipAuth)) {
                        if (authRefreshAttempts < MAX_AUTH_REFRESH_ATTEMPTS) {
                            authRefreshAttempts++;
                            const newToken = await refreshAccessToken();
                            if (newToken) {
                                config.headers['Authorization'] = `Bearer ${newToken}`;
                                continue;
                            }
                        }
                        handleUnauthorized();
                    }
                    throw await parseError(processedResponse);
                }

                const data = await processedResponse.json() as Result<T>;

                if (cache && method === 'GET' && data && isSuccessCode(data.code)) {
                    setToCache(cacheKey, data);
                }

                if (!isSuccessCode(data.code)) {
                    throw new ApiClientError(data.message || '请求失败', data.code, data.data);
                }

                return data;

            } catch (error) {
                lastError = error as Error;

                const shouldRetry = attempt < retries &&
                    error instanceof ApiClientError &&
                    isRetryable(error.status) &&
                    !controller.signal.aborted;

                if (shouldRetry) {
                    attempt++;
                    const delay = RETRY_DELAY_BASE * Math.pow(2, attempt - 1);
                    await new Promise(resolve => setTimeout(resolve, delay));
                } else {
                    break;
                }
            }
        }

        if (lastError instanceof ApiClientError) {
            throw lastError;
        }

        if (lastError?.name === 'AbortError' || controller.signal.aborted) {
            throw new ApiClientError('请求超时或已取消', 0);
        }

        throw new ApiClientError(lastError?.message ?? '网络请求失败', 0);
    } finally {
        clearTimeout(timeoutId);
        if (dedupe) {
            requestManager.stopTracking(requestKey);
        }
    }
}

export {
    API_BASE_URL,
    ApiClientError,
    request,
    clearCache,
    addRequestInterceptor,
    addResponseInterceptor,
    buildQueryParams,
    requestManager
};
