export type ProductCondition = 'NEW' | 'LIKE_NEW' | 'GOOD' | 'FAIR' | 'POOR';

export type ProductStatus = 'ON_SALE' | 'SOLD' | 'OFF_SHELF' | 'RESERVED';

export interface Product {
    id: number;
    title: string;
    description: string;
    price: number;
    originalPrice: number | null;
    categoryId: number;
    categoryName: string;
    condition: ProductCondition;
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
}

export interface ProductDetail extends Product {
    seller: {
        id: number;
        username: string;
        avatar: string | null;
        rating: number;
        productCount: number;
        soldCount: number;
    };
    priceHistory: PriceHistoryItem[];
    similarProducts: Product[];
}

export interface PriceHistoryItem {
    date: string;
    price: number;
}

export interface ProductQueryParams {
    keyword?: string;
    categoryId?: number;
    priceMin?: number;
    priceMax?: number;
    conditions?: ProductCondition[];
    status?: ProductStatus;
    sellerId?: number;
    sort?: 'newest' | 'price_asc' | 'price_desc' | 'popular';
    current?: number;
    size?: number;
}

export interface CreateProductRequest {
    title: string;
    description: string;
    price: number;
    originalPrice?: number;
    categoryId: number;
    condition: ProductCondition;
    images: string[];
    location: string;
}

export interface UpdateProductRequest {
    title?: string;
    description?: string;
    price?: number;
    originalPrice?: number;
    categoryId?: number;
    condition?: ProductCondition;
    images?: string[];
    location?: string;
    status?: ProductStatus;
}

export interface Category {
    id: number;
    name: string;
    icon: string | null;
    parentId: number | null;
    children?: Category[];
    productCount: number;
}

export interface Favorite {
    id: number;
    productId: number;
    product: Product;
    createTime: string;
}
