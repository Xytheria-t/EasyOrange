export type ProductStatus = 'DRAFT' | 'ONLINE' | 'SOLD' | 'OFFLINE' | 'PENDING_REVIEW' | 'REJECTED';

export interface Product {
    id: string;
    title: string;
    description: string;
    price: number;
    originalPrice: number | null;
    categoryId: string;
    categoryName: string;
    /** 成色等级（后端 condition；展示用 CONDITION_LABEL_MAP 换算文案） */
    condition: number;
    status: ProductStatus;
    images: string[];
    mainImageUrl?: string | null;
    location: string;
    views: number;
    sellerId: string;
    sellerName: string;
    sellerAvatar: string | null;
    sellerRating: number;
    createTime: string;
    updateTime: string;
    stock?: number;
    contactMethod?: string;
}

export interface ProductQueryParams {
    keyword?: string;
    categoryId?: string;
    priceMin?: number;
    priceMax?: number;
    conditions?: number[];
    status?: ProductStatus;
    sellerId?: string;
    sort?: 'newest' | 'price_asc' | 'price_desc' | 'popular';
    pageNum?: number;
    pageSize?: number;
    hasDiscount?: boolean;
}

/** AI 建议快照（拍照识别给出）— 仅用于统计字段级采纳率，不参与定价逻辑 */
export interface AiSuggestionSnapshot {
    title: string;
    description: string;
    price: number;
    categoryName: string;
    /** 成色等级 "1"~"4"（与后端 AutoListingResult 同形） */
    conditionLevel: string;
    location: string;
}

export interface CreateProductRequest {
    name: string;
    description: string;
    price: number;
    originalPrice?: number;
    categoryId: string;
    conditionLevel: number;
    stock?: number;
    location?: string;
    contactMethod?: string;
    imageUrls: string[];
    /** AI 建议快照（拍照识别给出）：后端留档后与最终值比对，出字段级采纳率 */
    aiSuggestion?: AiSuggestionSnapshot;
}

export interface UpdateProductRequest {
    name?: string;
    description?: string;
    price?: number;
    originalPrice?: number;
    categoryId?: string;
    conditionLevel: number;
    stock?: number;
    location?: string;
    contactMethod?: string;
    imageUrls?: string[];
}

export interface Category {
    id: string;
    name: string;
    icon: string | null;
    /** 一级分类为 null。 */
    parentId: string | null;
    level?: number;
    sortOrder?: number;
    /** 后端返回的是 0/1 数字（与 admin 侧一致），不是枚举名字符串。 */
    status?: number;
    productCount?: number;
}
