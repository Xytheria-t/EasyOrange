/**
 * @fileoverview 商品工具模块
 * @description 提供商品相关的工具函数
 */

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

export function isHotProduct(viewCount: number): boolean {
    return viewCount >= 100;
}
