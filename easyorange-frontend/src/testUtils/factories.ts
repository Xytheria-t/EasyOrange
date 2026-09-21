import type { AdminProduct, AdminProductQuery } from '@/admin/types/admin';
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

export function createMockAdminProduct(overrides: Partial<AdminProduct> = {}): AdminProduct {
    return {
        productId: '1',
        name: '测试商品',
        description: '描述',
        price: 100,
        originalPrice: 150,
        stock: 10,
        status: 'PENDING_REVIEW',
        statusDesc: '待审核',
        conditionLevel: 1,
        location: '北京',
        contactMethod: null,
        images: [],
        mainImage: null,
        categoryId: '1',
        categoryName: '电子产品',
        sellerId: '10',
        sellerName: '资产方小明',
        sellerAvatar: null,
        viewCount: 50,
        createTime: '2026-05-16 10:00:00',
        updateTime: '2026-05-16 10:00:00',
        ...overrides,
    };
}

export function createMockAdminProductQuery(overrides: Partial<AdminProductQuery> = {}): AdminProductQuery {
    return {
        pageNum: 1,
        pageSize: 20,
        ...overrides,
    };
}
