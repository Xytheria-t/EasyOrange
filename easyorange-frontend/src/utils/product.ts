/**
 * @fileoverview 商品工具模块
 * @description 提供商品相关的工具函数
 */

import type { Product } from '@/types';
import { PRODUCT_STATUS_CODE } from '@/constants';

export function normalizeProduct(raw: Record<string, unknown>): Product {
    const status = typeof raw.status === 'number' ? raw.status : 1;
    const condition = typeof raw.condition === 'number' ? raw.condition : 0;
    return {
        id: raw.id as string,
        title: (raw.title as string) ?? '',
        description: (raw.description as string) ?? '',
        price: raw.price as number,
        originalPrice: (raw.originalPrice as number | null) ?? null,
        categoryId: raw.categoryId as number,
        categoryName: (raw.categoryName as string) ?? '',
        condition,
        conditionLevel: condition,
        status: PRODUCT_STATUS_CODE[status] ?? 'ONLINE',
        images: (raw.images as string[]) ?? [],
        location: (raw.location as string) ?? '',
        views: (raw.views as number) ?? 0,
        favorites: (raw.favorites as number) ?? 0,
        sellerId: raw.sellerId as string,
        sellerName: ((raw.sellerName as string) || (raw.username as string)) ?? '匿名用户',
        sellerAvatar: ((raw.sellerAvatar as string | null) || (raw.userAvatar as string | null)) ?? null,
        sellerRating: (raw.sellerRating as number) ?? 0,
        createTime: (raw.createTime as string) ?? '',
        updateTime: (raw.updateTime as string) ?? '',
        stock: raw.stock as number | undefined,
        contactMethod: (raw.contactMethod as string) ?? undefined,
    };
}

export function calculateDiscount(
    currentPrice: number,
    originalPrice?: number | null
): number | null {
    if (!originalPrice || originalPrice <= 0 || currentPrice >= originalPrice) {
        return null;
    }
    return Math.round((currentPrice / originalPrice) * 10) / 10;
}

export function getConditionNameFromString(condition: string): string {
    const conditionMap: Record<string, string> = {
        'NEW': '全新',
        'LIKE_NEW': '几乎全新',
        'GOOD': '良好',
        'FAIR': '一般',
        'POOR': '较差'
    };
    return conditionMap[condition] || condition;
}

