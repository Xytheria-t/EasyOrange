export type ProductStatus = 'DRAFT' | 'ONLINE' | 'SOLD' | 'OFFLINE';

export interface Product {
    id: number;
    title: string;
    description: string;
    price: number;
    originalPrice: number | null;
    categoryId: number;
    categoryName: string;
    condition: number;
    conditionLevel: number;
    status: ProductStatus;
    images: string[];
    location: string;
    views: number;
    favorites: number;
    sellerId: number;
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
    categoryId?: number;
    priceMin?: number;
    priceMax?: number;
    conditions?: number[];
    status?: ProductStatus;
    sellerId?: number;
    sort?: 'newest' | 'price_asc' | 'price_desc' | 'popular';
    pageNum?: number;
    pageSize?: number;
}

export interface CreateProductRequest {
    name: string;
    description: string;
    price: number;
    originalPrice?: number;
    categoryId: number;
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
    categoryId?: number;
    conditionLevel?: number;
    stock?: number;
    location?: string;
    contactMethod?: string;
    imageUrls?: string[];
}

export interface Category {
    id: number;
    name: string;
    icon: string | null;
    parentId: number | null;
    level?: number;
    sortOrder?: number;
    status?: number;
    children?: Category[];
    productCount?: number;
}

export interface FavoriteProduct {
    id: number;
    sellerId: number;
    username: string;
    userAvatar: string | null;
    categoryId: number;
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
    id: number;
    productId: number;
    product: FavoriteProduct;
    createTime: string;
}
