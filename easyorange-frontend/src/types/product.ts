export type ProductStatus = 'DRAFT' | 'ONLINE' | 'SOLD' | 'OFFLINE' | 'PENDING_REVIEW' | 'REJECTED';

export interface Product {
    id: string;
    title: string;
    description: string;
    price: number;
    originalPrice: number | null;
    categoryId: string;
    categoryName: string;
    condition: number;
    conditionLevel: number;
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
    isHot?: boolean;
    discount?: number;
    viewCount?: number;
    category?: string;
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
    parentId: string | null;
    level?: number;
    sortOrder?: number;
    status?: number;
    children?: Category[];
    productCount?: number;
}

/** ES facet aggregation bucket（label：后端聚合出的展示名，分类为类目名、价格为区间文案） */
export interface FacetBucket {
    code: string;
    label?: string;
    count: number;
}

/** ES-powered product search result */
export interface ProductSearchResult {
    records: Product[];
    total: number;
    current: number;
    size: number;
    pages: number;
    facets: FacetBucket[];
    aiEnhancement?: AiEnhancement;
    /** 已开启 AI 增强但本次失败（后端 degraded 标记），UI 据此显示降级提示 */
    aiEnhancementDegraded?: boolean;
}

/** AI 智能导购增强数据 */
export interface AiEnhancement {
    intentExplanation: string;
    productTags: Record<string, string[]>;
    marketAnalysis: string;
    suggestedQuestions: string[];
}

/** Product search query parameters for ES search */
export interface ProductSearchParams {
    keyword?: string;
    categoryId?: string;
    status?: number;
    minPrice?: number;
    maxPrice?: number;
    conditionLevel?: number;
    sort?: 'default' | 'price_asc' | 'price_desc' | 'newest';
    /** 后端 /products/search 绑定的是 PageRequest.sortField（relevance 不传即可） */
    sortField?: 'relevance' | 'newest' | 'price_asc' | 'price_desc' | 'popular';
    pageNum?: number;
    pageSize?: number;
    aiEnhanced?: boolean;
}
