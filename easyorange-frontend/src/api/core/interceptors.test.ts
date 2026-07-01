import { describe, expect, it, vi } from 'vitest';
import {
    addRequestInterceptor,
    addResponseInterceptor,
    applyRequestInterceptors,
    applyResponseInterceptors,
} from './interceptors';

describe('interceptors', () => {
    describe('request interceptors', () => {
        it('applies no interceptors when none registered', async () => {
            const config = { method: 'GET', headers: {} };
            const result = await applyRequestInterceptors(config);
            expect(result).toBe(config);
        });

        it('applies single request interceptor', async () => {
            const interceptor = vi.fn(config => ({
                ...config,
                headers: { ...config.headers, Authorization: 'Bearer token' },
            }));
            const remove = addRequestInterceptor(interceptor);

            const config = { method: 'GET', headers: {} };
            const result = await applyRequestInterceptors(config);

            expect(interceptor).toHaveBeenCalledTimes(1);
            expect(result.headers.Authorization).toBe('Bearer token');
            remove();
        });

        it('applies multiple request interceptors in order', async () => {
            const interceptor1 = vi.fn(config => ({
                ...config,
                headers: { ...config.headers, 'X-First': 'first' },
            }));
            const interceptor2 = vi.fn(config => ({
                ...config,
                headers: { ...config.headers, 'X-Second': 'second' },
            }));
            addRequestInterceptor(interceptor1);
            addRequestInterceptor(interceptor2);

            const config = { method: 'GET', headers: {} };
            const result = await applyRequestInterceptors(config);

            expect(interceptor1).toHaveBeenCalledBefore(interceptor2);
            expect(result.headers['X-First']).toBe('first');
            expect(result.headers['X-Second']).toBe('second');
        });

        it('supports async request interceptors', async () => {
            const interceptor = vi.fn(async config => ({
                ...config,
                headers: { ...config.headers, 'X-Async': 'async-value' },
            }));
            addRequestInterceptor(interceptor);

            const config = { method: 'GET', headers: {} };
            const result = await applyRequestInterceptors(config);

            expect(result.headers['X-Async']).toBe('async-value');
        });

        it('remove function works correctly', async () => {
            const interceptor = vi.fn(config => config);
            const remove = addRequestInterceptor(interceptor);
            remove();

            const config = { method: 'GET', headers: {} };
            await applyRequestInterceptors(config);

            expect(interceptor).not.toHaveBeenCalled();
        });
    });

    describe('response interceptors', () => {
        it('applies no interceptors when none registered', async () => {
            const response = new Response('ok', { status: 200 });
            const result = await applyResponseInterceptors(response);
            expect(result).toBe(response);
        });

        it('applies single response interceptor', async () => {
            const interceptor = vi.fn(res => res);
            const remove = addResponseInterceptor(interceptor);

            const response = new Response('ok', { status: 200 });
            const result = await applyResponseInterceptors(response);

            expect(interceptor).toHaveBeenCalledTimes(1);
            expect(result).toBe(response);
            remove();
        });

        it('applies multiple response interceptors in order', async () => {
            const calls: number[] = [];
            const interceptor1 = vi.fn((res: Response) => {
                calls.push(1);
                return res;
            });
            const interceptor2 = vi.fn((res: Response) => {
                calls.push(2);
                return res;
            });
            addResponseInterceptor(interceptor1);
            addResponseInterceptor(interceptor2);

            const response = new Response('ok', { status: 200 });
            await applyResponseInterceptors(response);

            expect(calls).toEqual([1, 2]);
        });

        it('supports async response interceptors', async () => {
            const interceptor = vi.fn(async res => {
                const clone = res.clone();
                const text = await clone.text();
                return new Response(text.toUpperCase(), res);
            });
            addResponseInterceptor(interceptor);

            const response = new Response('hello', { status: 200 });
            const result = await applyResponseInterceptors(response);

            const text = await result.text();
            expect(text).toBe('HELLO');
        });

        it('remove function works correctly', async () => {
            const interceptor = vi.fn(res => res);
            const remove = addResponseInterceptor(interceptor);
            remove();

            const response = new Response('ok', { status: 200 });
            await applyResponseInterceptors(response);

            expect(interceptor).not.toHaveBeenCalled();
        });
    });
});
