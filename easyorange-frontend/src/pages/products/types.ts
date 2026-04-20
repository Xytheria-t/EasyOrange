/**
 * @fileoverview 商品列表页类型定义
 *
 * @design_note
 * ProductListItem 是针对列表页优化的去规范化视图模型，与后端 Product 实体不同：
 * - 列表页需要展示更多卖家信息（avatar、phone、wechat）
 * - 列表页使用数值等级 conditionLevel 便于筛选
 * - 列表页不需要完整的 ProductCondition 嵌套对象
 *
 * 保持分离可以避免列表接口返回不必要的敏感字段，提升性能。
 */

export type SortOption = 'newest' | 'price_asc' | 'price_desc' | 'popular';
export type QuickFilterOption = 'all' | 'hot' | 'discount' | 'newArrival';
export type ViewMode = 'grid' | 'list';

export interface ProductCondition {
    level: number;
    name: string;
    icon: string;
}

export interface ProductListItem {
    id: number;
    name: string;
    description: string;
    price: number;
    originalPrice: number | null;
    categoryId: number;
    categoryName: string;
    categoryIcon: string | null;
    conditionLevel: number;
    conditionName: string;
    conditionIcon: string;
    location: string;
    images: string[];
    sellerId: number;
    sellerName: string;
    sellerAvatar: string | null;
    sellerPhone?: string;
    sellerWechat?: string;
    contactMethod?: string;
    viewCount: number;
    createTime: string;
}

export interface BrowseHistoryItem {
    id: number;
    name: string;
    price: number;
    image: string | null;
}

export interface CompareItem {
    id: number;
    name: string;
    price: number;
    originalPrice: number | null;
    image: string | null;
    conditionName: string;
    conditionIcon: string;
    categoryName: string;
    location: string;
    viewCount: number;
}

export interface ProductFilters {
    keyword: string;
    categoryId: number | null;
    priceMin: number | null;
    priceMax: number | null;
    conditions: number[];
    sort: SortOption;
    quickFilter: QuickFilterOption;
}

export interface QueryParams {
    page: number;
    size: number;
    keyword?: string;
    categoryId?: number;
    minPrice?: number;
    maxPrice?: number;
    sortBy?: string;
    sortOrder?: string;
}

export interface PriceStatisticsData {
    highestPrice: number;
    lowestPrice: number;
    averagePrice: number;
    history: Array<{ newPrice: number }>;
}

export interface FilterTag {
    label: string;
    key: string;
}
