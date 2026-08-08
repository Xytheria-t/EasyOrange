export interface RequestOptions extends Omit<RequestInit, 'body' | 'headers' | 'cache'> {
    method?: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';
    headers?: Record<string, string>;
    body?: unknown;
    params?: Record<string, unknown>;
    timeout?: number;
    retries?: number;
    signal?: AbortSignal;
    dedupe?: boolean;
    skipAuth?: boolean;
}
