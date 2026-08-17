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
    location: string;
    views: number;
    favorites: number;
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

export interface FavoriteProduct {
    id: string;
    sellerId: string;
    username: string;
    userAvatar: string | null;
    categoryId: string;
    categoryName: string;
    title: string;
    description: string;
    price: number;
    originalPrice: number | null;
    stock: number;
    status: number;
    statusDesc: string | null;
    views: number;
    condition: number;
    conditionDesc: string | null;
    location: string;
    contactMethod: string | null;
    images: string[];
    mainImageUrl: string | null;
    createTime: string;
    updateTime: string;
}

export interface Favorite {
    id: string;
    productId: string;
    priceSnapshot: number | null;
    product: FavoriteProduct;
    createTime: string;
}

/** ES facet aggregation bucket */
export interface FacetBucket {
    code: string;
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
    pageNum?: number;
    pageSize?: number;
    aiEnhanced?: boolean;
}
