import type { PageResult, Product } from '@/types';

export function createMockProduct(overrides: Partial<Product> = {}): Product {
    return {
        id: '1',
        title: '测试商品',
        description: '这是一个测试商品描述',
        price: 100,
        originalPrice: 150,
        categoryId: '1',
        categoryName: '电子产品',
        condition: 1,
        conditionLevel: 1,
        status: 'ONLINE',
        images: ['https://example.com/image.jpg'],
        location: '北京',
        views: 50,
        sellerId: 'user1',
        sellerName: '资产方小明',
        sellerAvatar: null,
        sellerRating: 4.5,
        createTime: '2026-05-16 10:00:00',
        updateTime: '2026-05-16 10:00:00',
        ...overrides,
    };
}

export function createMockProductPage(overrides: Partial<PageResult<Product>> = {}): PageResult<Product> {
    return {
        records: [createMockProduct()],
        total: 1,
        current: 1,
        size: 20,
        pages: 1,
        ...overrides,
    };
}
