import { beforeEach, describe, expect, it, vi } from 'vitest';
import { productApi } from './productApi';

const mockRequest = vi.fn();
vi.mock('./core/request', () => ({
    request: (...args: unknown[]) => mockRequest(...args),
}));

/** 后端把 long/Long 全局序列化成字符串，线上 total 就是 "70" —— 这里刻意按线上形状喂。 */
function pageOf(total: string) {
    return {
        code: 'A0000',
        message: '成功',
        data: { records: [], total, size: 20, current: 1, pages: 4 },
    };
}

describe('productApi', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('getProducts 请求路径与分页参数', () => {
        mockRequest.mockResolvedValue(pageOf('70'));
        productApi.getProducts({ pageNum: 2, pageSize: 20 });
        expect(mockRequest).toHaveBeenCalledWith('/products', {
            method: 'GET',
            params: { pageNum: 2, pageSize: 20 },
            skipAuth: true,
        });
    });

    // 不收敛时 total 是字符串：关系比较隐式转型侥幸能用，但任何 `+` 会静默变字符串拼接。
    it.each([
        ['getProducts', () => productApi.getProducts(), '/products'],
        ['getMyProducts', () => productApi.getMyProducts(), '/products/my'],
    ])('%s 把线上字符串 total 收敛成 number', async (_name, call, path) => {
        mockRequest.mockResolvedValue(pageOf('70'));

        const res = await (call() as Promise<{ data: { total: unknown } }>);

        expect(mockRequest).toHaveBeenCalledWith(path, expect.anything());
        expect(res.data.total).toBe(70);
        expect(typeof res.data.total).toBe('number');
    });

    it('total 缺失时收敛为 0 而不是 NaN', async () => {
        mockRequest.mockResolvedValue({
            code: 'A0000',
            message: '成功',
            data: { records: [], total: undefined, size: 20, current: 1, pages: 0 },
        });

        const res = await productApi.getProducts();

        expect(res.data?.total).toBe(0);
    });

    // 详情页路由没包 ProtectedRoute、按「匿名可逛」设计。少带 skipAuth 时匿名 GET 会 401，
    // 请求层的 handleUnauthorized 随即把用户弹去登录页——一条本该可匿名访问的公开读路径。
    it('getProductById 走匿名可读的公开路径', () => {
        mockRequest.mockResolvedValue({ code: 'A0000', message: '成功' });

        productApi.getProductById('123');

        expect(mockRequest).toHaveBeenCalledWith('/products/123', { skipAuth: true });
    });
});
