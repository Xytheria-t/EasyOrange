import { beforeEach, describe, expect, it, vi } from 'vitest';
import { favoriteApi } from './favoriteApi';

const mockRequest = vi.fn();
vi.mock('./core/request', () => ({
    request: (...args: unknown[]) => mockRequest(...args),
}));

describe('favoriteApi', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('getCount calls request with correct URL and method', async () => {
        mockRequest.mockResolvedValue({ data: 3 });

        await favoriteApi.getCount();

        expect(mockRequest).toHaveBeenCalledWith('/favorites/count', { method: 'GET' });
    });

    // 后端全局 Long → String（JS 精度安全），计数在线上是 "3"：类型声明是 number，必须收敛
    it('getCount coerces string count to number', async () => {
        mockRequest.mockResolvedValue({ data: '42' });

        await expect(favoriteApi.getCount()).resolves.toBe(42);
    });

    it('getCount falls back to 0 when data is null', async () => {
        mockRequest.mockResolvedValue({ data: null });

        await expect(favoriteApi.getCount()).resolves.toBe(0);
    });
});
