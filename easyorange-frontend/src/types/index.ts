/**
 * @fileoverview 类型定义 - 精简版
 * @description 合并所有类型定义到单一文件
 */

// ============ 通用类型 ============
export type ToastType = 'success' | 'error' | 'info' | 'warning';
export type Unsubscribe = () => void;
export type NetworkChangeCallback = (online: boolean) => void;
export type DateFormat = 'date' | 'time' | 'datetime';
export type ConditionLevel = 1 | 2 | 3 | 4;

export interface DebounceOptions {
    immediate?: boolean;
}

export interface StorageItem<T = unknown> {
    value: T;
    expireAt?: number;
}

export interface PaginationParams {
    current?: number;
    page?: number;
    size?: number;
    pageSize?: number;
}

export interface PaginationResponse<T> {
    records: T[];
    content?: T[];
    total: number;
    size: number;
    current: number;
    pages?: number;
    totalPages?: number;
}

// ============ API 响应类型 ============
export const SUCCESS_CODES = ['A0000', 0, 200] as const;
export type ApiCode = string | number;
export type SuccessCode = typeof SUCCESS_CODES[number];

export const isSuccessCode = (code: ApiCode | null | undefined): code is SuccessCode => {
    return SUCCESS_CODES.includes(code as SuccessCode);
};

export interface Result<T = unknown> {
    code: ApiCode;
    message: string;
    data: T;
}

export interface PageParams {
    current?: number;
    size?: number;
}

export interface PageResult<T> {
    records: T[];
    total: number;
    size: number;
    current: number;
    pages: number;
}

export type PageResponse<T> = Result<PageResult<T>>;

// ============ 用户类型 ============
export type UserStatus = 'NORMAL' | 'DISABLED' | 'BANNED';
export type Gender = 'MALE' | 'FEMALE' | 'UNKNOWN';

export interface User {
    id: number;
    username: string;
    email: string;
    phone: string | null;
    studentId: string;
    realName: string;
    gender: Gender;
    status: UserStatus;
    createTime: string;
    lastLoginTime: string | null;
}

export interface LoginRequest {
    account: string;
    password: string;
    loginMethod?: 'PASSWORD' | 'SMS';
    clientType?: 'WEB';
    isRegister?: boolean;
}

export interface RegisterRequest {
    username: string;
    password: string;
    email: string;
    phone?: string;
    studentId: string;
    realName: string;
}

// 手机号注册请求（规划中）
export interface PhoneRegisterRequest {
    phone: string;
    verifyCode: string;
    password: string;
}

// 邮箱注册请求（规划中）
export interface EmailRegisterRequest {
    email: string;
    verifyCode: string;
    password: string;
}

// 手机号登录请求（规划中）
export interface PhoneLoginRequest {
    phone: string;
    verifyCode: string;
}

// 邮箱登录请求（规划中）
export interface EmailLoginRequest {
    email: string;
    password: string;
}

export interface LoginResponse {
    token: string;
    user: UserInfo;
}

export interface UserInfo {
    id: number;
    username: string;
    email: string;
    phone: string | null;
    studentId: string;
    realName: string;
    gender: Gender;
    status: UserStatus;
    createTime: string;
    lastLoginTime: string | null;
}

export interface UserStats {
    productCount: number;
    soldCount: number;
    boughtCount: number;
    favoriteCount: number;
    followerCount: number;
    followingCount: number;
    rating: number;
    reviewCount: number;
}

// ============ 商品类型 ============
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
    isHot?: boolean;
    discount?: number;
    viewCount?: number;
    category?: string;
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

// ============ 订单类型 ============
export type OrderStatus = 
    | 'PENDING_PAYMENT'
    | 'PAID'
    | 'SHIPPED'
    | 'DELIVERED'
    | 'COMPLETED'
    | 'CANCELLED'
    | 'REFUNDED';

export type PaymentMethod = 'WECHAT' | 'ALIPAY' | 'CAMPUS_CARD' | 'CASH';

export interface Order {
    id: number;
    orderNo: string;
    productId: number;
    productTitle: string;
    productImage: string;
    price: number;
    quantity: number;
    totalAmount: number;
    status: OrderStatus;
    paymentMethod: PaymentMethod | null;
    buyerId: number;
    buyerName: string;
    buyerAvatar: string | null;
    sellerId: number;
    sellerName: string;
    sellerAvatar: string | null;
    createTime: string;
    payTime: string | null;
    shipTime: string | null;
    completeTime: string | null;
    cancelTime: string | null;
    cancelReason: string | null;
}

export interface CreateOrderRequest {
    productId: number;
    quantity: number;
    paymentMethod: PaymentMethod;
    address?: OrderAddress;
    remark?: string;
}

export interface OrderQueryParams {
    status?: OrderStatus;
    role?: 'buyer' | 'seller';
    current?: number;
    size?: number;
}

// ============ 消息类型 ============
export type MessageType = 'SYSTEM' | 'ORDER' | 'CHAT' | 'ACTIVITY';
export type MessageStatus = 'UNREAD' | 'READ';

export interface Message {
    id: number;
    type: MessageType;
    title: string;
    content: string;
    status: MessageStatus;
    senderId: number | null;
    senderName: string | null;
    senderAvatar: string | null;
    receiverId: number;
    createTime: string;
    readTime: string | null;
    extra: Record<string, unknown> | null;
}

export interface ChatSession {
    id: number;
    targetUserId: number;
    targetUserName: string;
    targetUserAvatar: string | null;
    lastMessage: string;
    lastMessageTime: string;
    unreadCount: number;
}

export interface SendMessageRequest {
    receiverId: number;
    content: string;
    type?: 'TEXT' | 'IMAGE' | 'PRODUCT';
    productId?: number;
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

export interface OrderAddress {
    receiverName: string;
    receiverPhone: string;
    province: string;
    city: string;
    district: string;
    detail: string;
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

export interface OrderDetail {
    id: number;
    orderNo: string;
    productId: number;
    productTitle: string;
    productImage: string;
    price: number;
    quantity: number;
    totalAmount: number;
    status: string;
    paymentMethod: string | null;
    buyerId: number;
    buyerName: string;
    buyerAvatar: string | null;
    sellerId: number;
    sellerName: string;
    sellerAvatar: string | null;
    createTime: string;
    payTime: string | null;
    shipTime: string | null;
    completeTime: string | null;
    cancelTime: string | null;
    cancelReason: string | null;
    product?: {
        id: number;
        title: string;
        images: string[];
        condition: string;
    };
    address?: {
        receiverName: string;
        receiverPhone: string;
        province: string;
        city: string;
        district: string;
        detail: string;
    } | null;
}
