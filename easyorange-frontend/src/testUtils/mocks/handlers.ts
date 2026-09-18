import { HttpResponse, http } from 'msw';

export const handlers = [
    // 商品列表
    http.get('/api/products', () => {
        return HttpResponse.json({
            code: 'A0000',
            message: 'success',
            data: {
                records: [],
                total: 0,
                current: 1,
                size: 20,
                pages: 0,
            },
            timestamp: Date.now(),
        });
    }),

    // 商品搜索
    http.get('/api/products/search', () => {
        return HttpResponse.json({
            code: 'A0000',
            message: 'success',
            data: {
                records: [],
                total: 0,
                current: 1,
                size: 20,
                pages: 0,
            },
            timestamp: Date.now(),
        });
    }),

    // 搜索建议
    http.get('/api/products/search/suggestions', () => {
        return HttpResponse.json({
            code: 'A0000',
            message: 'success',
            data: [],
            timestamp: Date.now(),
        });
    }),

    // 热搜
    http.get('/api/products/search/hot', () => {
        return HttpResponse.json({
            code: 'A0000',
            message: 'success',
            data: [],
            timestamp: Date.now(),
        });
    }),

    // Admin 商品列表
    http.get('/api/admin/products', () => {
        return HttpResponse.json({
            code: 'A0000',
            message: 'success',
            data: {
                records: [],
                total: 0,
                current: 1,
                size: 20,
                pages: 0,
            },
            timestamp: Date.now(),
        });
    }),

    // Admin 审核日志
    http.get('/api/admin/products/:id/audit-logs', () => {
        return HttpResponse.json({
            code: 'A0000',
            message: 'success',
            data: [],
            timestamp: Date.now(),
        });
    }),
];
