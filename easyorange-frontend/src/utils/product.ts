/**
 * @fileoverview 商品工具模块
 * @description 提供商品相关的工具函数
 */

import type { Product, ProductStatus, RawProduct } from '@/types';

const VALID_STATUSES: readonly ProductStatus[] = ['DRAFT', 'PENDING_REVIEW', 'REJECTED', 'ONLINE', 'OFFLINE', 'SOLD'];

export function normalizeProduct(raw: RawProduct): Product {
    const status = VALID_STATUSES.includes(raw.status as ProductStatus) ? (raw.status as ProductStatus) : 'ONLINE';
    const condition = raw.condition ?? 0;
    return {
        id: raw.id,
        title: raw.title ?? '',
        description: raw.description ?? '',
        price: raw.price,
        originalPrice: raw.originalPrice ?? null,
        categoryId: raw.categoryId,
        categoryName: raw.categoryName ?? '',
        condition,
        conditionLevel: condition,
        status,
        images: raw.images ?? [],
        location: raw.location ?? '',
        views: raw.views ?? 0,
        favorites: raw.favorites ?? 0,
        sellerId: raw.sellerId,
        sellerName: raw.sellerName ?? raw.username ?? '匿名用户',
        sellerAvatar: raw.sellerAvatar ?? raw.userAvatar ?? null,
        sellerRating: raw.sellerRating ?? 0,
        createTime: raw.createTime ?? '',
        updateTime: raw.updateTime ?? '',
        stock: raw.stock,
        contactMethod: raw.contactMethod,
    };
}

export function calculateDiscount(currentPrice: number, originalPrice?: number | null): number | null {
    if (!originalPrice || originalPrice <= 0 || currentPrice >= originalPrice) {
        return null;
    }
    return Math.round((currentPrice / originalPrice) * 10) / 10;
}

export function getConditionNameFromString(condition: string): string {
    const conditionMap: Record<string, string> = {
        NEW: '全新',
        LIKE_NEW: '几乎全新',
        GOOD: '良好',
        FAIR: '一般',
        POOR: '较差',
    };
    return conditionMap[condition] || condition;
}
