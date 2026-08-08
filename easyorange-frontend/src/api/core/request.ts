import { getStoredToken, handleUnauthorized, refreshAccessToken } from '@/features/auth/session';
import { type ApiCode, isSuccessCode, type RequestOptions, type Result } from '@/types';
import { buildQueryString, escapeHtml } from '@/utils/format';
import {
    addRequestInterceptor,
    addResponseInterceptor,
    applyRequestInterceptors,
    applyResponseInterceptors,
    type RequestConfig,
} from './interceptors';
import { requestManager } from './requestManager';

const API_BASE_URL = '/api';
const DEFAULT_TIMEOUT = 10000;
const DEFAULT_RETRIES = 2;
const RETRY_DELAY_BASE = 1000;
const CLIENT_TYPE = 'web';

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

const parseError = async (response: Response): Promise<ApiClientError> => {
    let message = `HTTP error! status: ${response.status}`;
    let details: unknown = null;

    try {
        const body = (await response.json()) as {
            message?: string;
            msg?: string;
            data?: unknown;
            errors?: unknown;
            code?: string | number;
        };

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

/** POST 等非幂等方法重试会重复副作用（如重复发消息），重试仅限幂等方法。 */
const isIdempotentMethod = (method: string): boolean =>
    method === 'GET' || method === 'PUT' || method === 'DELETE' || method === 'PATCH';

const buildQueryParams = (params: Record<string, unknown>): string => {
    const query = buildQueryString(params);
    return query ? `?${query}` : '';
};

const PUBLIC_ENDPOINTS = new Set(['/auth/login', '/auth/register', '/auth/password/reset', '/auth/sms-code']);

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
        signal,
        dedupe = true,
        skipAuth = false,
    } = options;

    const queryString = params ? buildQueryParams(params) : '';
    const url = `${API_BASE_URL}${endpoint}${queryString}`;
    const requestKey = requestManager.generateKey(endpoint, { method, body });

    if (dedupe && requestManager.isDuplicate(requestKey)) {
        throw new ApiClientError('重复请求已取消', 0);
    }

    let config: RequestConfig = {
        method,
        credentials: 'include' as const,
        headers: {
            'X-Client-Type': String(CLIENT_TYPE),
            ...headers,
        },
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
            config.headers.Authorization = `Bearer ${token}`;
        }
    }

    config = await applyRequestInterceptors(config);

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

                const processedResponse = await applyResponseInterceptors(response);

                if (!processedResponse.ok) {
                    if (processedResponse.status === 401 && shouldHandleUnauthorized(endpoint, skipAuth)) {
                        if (authRefreshAttempts < MAX_AUTH_REFRESH_ATTEMPTS) {
                            authRefreshAttempts++;
                            const newToken = await refreshAccessToken();
                            if (newToken) {
                                config.headers.Authorization = `Bearer ${newToken}`;
                                continue;
                            }
                        }
                        handleUnauthorized();
                    }
                    throw await parseError(processedResponse);
                }

                const data = (await processedResponse.json()) as Result<T>;

                if (!isSuccessCode(data.code)) {
                    throw new ApiClientError(data.message || '请求失败', data.code, data.data);
                }

                return data;
            } catch (error) {
                lastError = error as Error;

                const shouldRetry =
                    attempt < retries &&
                    isIdempotentMethod(method) &&
                    error instanceof ApiClientError &&
                    isRetryable(error.status) &&
                    !controller.signal.aborted;

                if (shouldRetry) {
                    attempt++;
                    const delay = RETRY_DELAY_BASE * 2 ** (attempt - 1);
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
    addRequestInterceptor,
    addResponseInterceptor,
    buildQueryParams,
    request,
    requestManager,
};
