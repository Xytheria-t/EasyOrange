/**
 * @fileoverview 类型定义 - 精简版
 * @description 合并所有类型定义到单一文件
 */

// ============ API 响应类型 ============
const SUCCESS_CODES = ['A0000', '0', 0, 200] as const;
export type ApiCode = string | number;
type SuccessCode = typeof SUCCESS_CODES[number];

export const isSuccessCode = (code: ApiCode | null | undefined): code is SuccessCode => {
    return SUCCESS_CODES.includes(code as SuccessCode);
};

export interface Result<T = unknown> {
    code: ApiCode;
    message: string;
    data: T;
    timestamp: number;
}

export interface PageResult<T> {
    records: T[];
    total: number;
    size: number;
    current: number;
    pages: number;
}

// ============ 用户类型 ============
export interface User {
    userId: number;
    username: string;
    nickname?: string;
    email: string;
    phone: string | null;
    studentId: string | null;
    realName: string | null;
    avatar: string | null;
    status: number;
    createTime: string;
    updateTime: string;
}

export interface LoginRequest {
    account: string;
    password: string;
    loginMethod?: 'password' | 'sms';
    clientType?: 'WEB';
    isRegister?: boolean;
}

export interface RegisterRequest {
    username: string;
    password: string;
    phone?: string;
    email?: string;
}

export interface LoginResponse {
    token: string;
    refreshToken: string;
    user: User;
}

// ============ 商品类型 ============
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

export const CONDITION_LABEL_MAP: Record<number, string> = {
    1: '全新',
    2: '几乎全新',
    3: '轻微使用',
    4: '明显使用',
};

export const STATUS_LABEL_MAP: Record<ProductStatus, string> = {
    DRAFT: '草稿',
    ONLINE: '在售',
    SOLD: '已售出',
    OFFLINE: '已下架',
};

export const PRODUCT_STATUS_CODE: Record<number, ProductStatus> = {
    0: 'DRAFT',
    1: 'ONLINE',
    2: 'SOLD',
    3: 'OFFLINE',
};

// ============ 订单类型 ============
export type OrderStatus =
    | 'PENDING_PAYMENT'
    | 'PAID'
    | 'SHIPPED'
    | 'COMPLETED'
    | 'CANCELLED'
    | 'REFUNDED';

const ORDER_STATUS_CODE: Record<number, OrderStatus> = {
    0: 'PENDING_PAYMENT',
    1: 'PAID',
    2: 'SHIPPED',
    3: 'COMPLETED',
    4: 'CANCELLED',
    5: 'REFUNDED',
};

const ORDER_STATUS_LABEL: Record<OrderStatus, string> = {
    PENDING_PAYMENT: '待付款',
    PAID: '待发货',
    SHIPPED: '已发货',
    COMPLETED: '已完成',
    CANCELLED: '已取消',
    REFUNDED: '已退款',
};

export const getOrderStatusLabel = (status: number | OrderStatus): string => {
    if (typeof status === 'number') {
        const key = ORDER_STATUS_CODE[status];
        return key ? ORDER_STATUS_LABEL[key] : '未知状态';
    }
    return ORDER_STATUS_LABEL[status] ?? '未知状态';
};

export const getOrderStatusFromCode = (code: number): OrderStatus => {
    return ORDER_STATUS_CODE[code] ?? 'PENDING_PAYMENT';
};

export type PaymentMethod = 'WECHAT' | 'ALIPAY' | 'CAMPUS_CARD' | 'CASH';

export interface Order {
    id: number;
    orderNo: string;
    buyerId: number;
    buyerUsername: string;
    sellerId: number;
    sellerUsername: string;
    productId: number;
    productTitle: string;
    productImage: string;
    amount: number;
    status: number;
    statusDesc: string;
    address: string;
    phone: string;
    quantity: number;
    remark: string | null;
    createTime: string;
    updateTime: string;
}

export interface CreateOrderRequest {
    productId: number;
    quantity?: number;
    paymentMethod?: PaymentMethod;
    address?: string;
    phone?: string;
    remark?: string;
}

export interface OrderQueryParams {
    orderNo?: string;
    status?: number | OrderStatus;
    buyerId?: number;
    sellerId?: number;
    productId?: number;
    role?: 'buyer' | 'seller';
    pageNum?: number;
    pageSize?: number;
    current?: number;
    size?: number;
    sortField?: string;
    sortDirection?: 'asc' | 'desc';
}

// ============ 消息类型 ============
export interface ChatSession {
    id: number;
    targetUserId: number;
    targetUserName: string;
    targetUserAvatar: string | null;
    lastMessage: string;
    lastMessageTime: string;
    unreadCount: number;
}

export interface ChatMessage {
    id: number;
    senderId: number;
    receiverId: number;
    content: string;
    type: 'TEXT' | 'IMAGE' | 'PRODUCT';
    createTime: string;
    readTime: string | null;
}

export interface RequestOptions extends Omit<RequestInit, 'body' | 'headers' | 'cache'> {
    method?: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';
    headers?: Record<string, string>;
    body?: unknown;
    params?: Record<string, unknown>;
    timeout?: number;
    retries?: number;
    cache?: boolean;
    signal?: AbortSignal;
    dedupe?: boolean;
    skipAuth?: boolean;
}

export interface OrderDetail extends Order {
    product?: {
        id: number;
        title: string;
        images: string[];
        condition: string;
    };
}
