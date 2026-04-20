/**
 * @fileoverview 商品列表页测试数据
 * @description 提供开发环境使用的模拟商品数据
 */

import type { ProductListItem } from './types.js';

interface MockCategory {
    id: number;
    name: string;
    icon: string;
    parentId: null;
    productCount: number;
}

interface MockCondition {
    level: number;
    name: string;
    icon: string;
}

export const MOCK_CATEGORIES: MockCategory[] = [
    { id: 1, name: '图书教材', icon: '📚', parentId: null, productCount: 0 },
    { id: 2, name: '电子产品', icon: '💻', parentId: null, productCount: 0 },
    { id: 3, name: '服装鞋包', icon: '👔', parentId: null, productCount: 0 },
    { id: 4, name: '生活用品', icon: '🏠', parentId: null, productCount: 0 },
    { id: 5, name: '运动户外', icon: '⚽', parentId: null, productCount: 0 },
    { id: 6, name: '美妆护肤', icon: '💄', parentId: null, productCount: 0 },
    { id: 7, name: '交通工具', icon: '🚲', parentId: null, productCount: 0 },
    { id: 8, name: '其他', icon: '📦', parentId: null, productCount: 0 },
];

export const MOCK_CONDITIONS: MockCondition[] = [
    { level: 1, name: '全新', icon: '✨' },
    { level: 2, name: '几乎全新', icon: '🌟' },
    { level: 3, name: '轻微使用', icon: '💫' },
    { level: 4, name: '明显使用', icon: '⭐' },
];

const MOCK_PRODUCT_TITLES = [
    '大学英语四级词汇书', 'MacBook Pro 2020', '耐克运动鞋', '宜家书桌',
    '高等数学教材', 'AirPods Pro', 'Adidas运动T恤', '小米空气净化器',
    '机械键盘', '显示器支架', '蓝牙音箱', '电动牙刷', '咖啡机', '登山背包', '瑜伽垫', '台灯'
];

const MOCK_LOCATIONS = ['图书馆', '食堂', '宿舍楼下', '教学楼'];

export function generateMockProducts(count: number): ProductListItem[] {
    return Array.from({ length: count }, (_, i) => {
        const category = MOCK_CATEGORIES[Math.floor(Math.random() * MOCK_CATEGORIES.length)];
        const condition = MOCK_CONDITIONS[Math.floor(Math.random() * MOCK_CONDITIONS.length)];
        const price = Math.floor(Math.random() * 1000) + 10;
        const originalPrice = Math.random() > 0.5 ? price + Math.floor(Math.random() * 500) : null;

        return {
            id: i + 1,
            name: MOCK_PRODUCT_TITLES[i % MOCK_PRODUCT_TITLES.length],
            description: `这是一件优质的二手商品，${condition.name}状态，性价比极高。`,
            price,
            originalPrice,
            categoryId: category.id,
            categoryName: category.name,
            categoryIcon: category.icon,
            conditionLevel: condition.level,
            conditionName: condition.name,
            conditionIcon: condition.icon,
            location: MOCK_LOCATIONS[Math.floor(Math.random() * MOCK_LOCATIONS.length)],
            images: [
                `https://picsum.photos/seed/${i}a/400/400`,
                `https://picsum.photos/seed/${i}b/400/400`
            ],
            sellerId: Math.floor(Math.random() * 10) + 1,
            sellerName: `用户${Math.floor(Math.random() * 100)}`,
            sellerAvatar: `https://api.dicebear.com/7.x/avataaars/svg?seed=${i}`,
            viewCount: Math.floor(Math.random() * 500),
            createTime: new Date(Date.now() - Math.random() * 7 * 24 * 60 * 60 * 1000).toISOString(),
        } as ProductListItem;
    });
}
