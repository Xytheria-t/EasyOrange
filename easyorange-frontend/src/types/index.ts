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
export const SUCCESS_CODES = ['A0000', '0', 0, 200] as const;
export type ApiCode = string | number;
export type SuccessCode = typeof SUCCESS_CODES[number];

export const isSuccessCode = (code: ApiCode | null | undefined): code is SuccessCode => {
    return SUCCESS_CODES.includes(code as SuccessCode);
};

export const RETRYABLE_STATUS = [0, 408, 429] as const;

export const ERROR_CODE_MAP: Record<string, string> = {
    '1': '操作失败',
    '1000': '业务异常',
    '1001': '参数错误',
    '1002': '未授权',
    '1003': '禁止访问',
    '1004': '资源不存在',
    '1005': '内部服务器错误',
    '1006': '服务不可用',
    '1007': '参数校验失败',
    '1008': '请求方法不允许',
    '2001': '文件大小超过限制',
    '2002': '无效的文件类型',
    '2003': '文件上传失败',
    'B1001': '用户不存在',
    'B1002': '账户已被禁用',
    'B1003': '账户已被锁定',
    'B1004': '用户名已存在',
    'B1005': '邮箱已被注册',
    'B1006': '手机号已被注册',
    'B1007': '密码错误',
    'B1008': '验证码无效或已过期',
    'B1009': '验证码发送过于频繁',
    'B1010': '验证码验证次数过多，请重新获取',
    'B1011': '账号或密码错误',
    'B1012': '学号已被注册',
    'B2001': '商品不存在',
    'B2002': '商品已下架',
    'B2003': '商品库存不足',
    'B2004': '商品已售出',
    'B2005': '非商品所有者',
    'B2006': '商品已审核',
    'B5001': '文件上传失败',
    'B5002': '文件删除失败',
    'B5003': '文件不存在',
    'B5004': '文件类型不允许',
    'B5005': '文件大小超出限制',
    'B5006': '文件名无效',
    'B7001': '消息不存在',
    'B7002': '非消息接收者',
    'B7003': '消息模板不存在',
    'B7004': '模板编码已存在',
    'B7005': '消息模板已禁用',
    'B7006': '模板渲染失败',
    'B7007': '模板变量缺失',
};

export const getErrorMessage = (code: ApiCode, fallback?: string): string => {
    const codeStr = String(code);
    return ERROR_CODE_MAP[codeStr] ?? fallback ?? '请求失败';
};

export interface Result<T = unknown> {
    code: ApiCode;
    message: string;
    data: T;
    timestamp: number;
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

export interface ApiResponse<T = unknown> {
    success: boolean;
    code: ApiCode;
    message: string;
    data: T;
    timestamp?: number;
}

// ============ 用户类型 ============
export type UserStatusType = 'NORMAL' | 'DISABLED' | 'LOCKED';
export type GenderType = 'MALE' | 'FEMALE' | 'UNKNOWN';

export const USER_STATUS_CODE: Record<number, UserStatusType> = {
    0: 'NORMAL',
    1: 'DISABLED',
    2: 'LOCKED',
};

export const USER_STATUS_LABEL: Record<UserStatusType, string> = {
    NORMAL: '正常',
    DISABLED: '禁用',
    LOCKED: '锁定',
};

export const getUserStatusLabel = (status: number | UserStatusType): string => {
    if (typeof status === 'number') {
        const key = USER_STATUS_CODE[status];
        return key ? USER_STATUS_LABEL[key] : '未知状态';
    }
    return USER_STATUS_LABEL[status] ?? '未知状态';
};

export const GENDER_CODE: Record<number, GenderType> = {
    0: 'FEMALE',
    1: 'MALE',
    2: 'UNKNOWN',
};

export const GENDER_LABEL: Record<GenderType, string> = {
    MALE: '男',
    FEMALE: '女',
    UNKNOWN: '未知',
};

export const getGenderLabel = (code: number | GenderType): string => {
    if (typeof code === 'number') {
        const key = GENDER_CODE[code];
        return key ? GENDER_LABEL[key] : '未知';
    }
    return GENDER_LABEL[code] ?? '未知';
};

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
    refreshToken: string;
    user: User;
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

export const CONDITION_LEVEL_MAP: Record<number, ProductCondition> = {
    1: 'NEW',
    2: 'LIKE_NEW',
    3: 'GOOD',
    4: 'FAIR',
};

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

export const getProductStatusFromCode = (code: number): ProductStatus => {
    return PRODUCT_STATUS_CODE[code] ?? 'ONLINE';
};

export const getProductStatusLabel = (status: number | ProductStatus): string => {
    if (typeof status === 'number') {
        const key = PRODUCT_STATUS_CODE[status];
        return key ? STATUS_LABEL_MAP[key] : '未知状态';
    }
    return STATUS_LABEL_MAP[status] ?? '未知状态';
};

// ============ 订单类型 ============
export type OrderStatus = 
    | 'PENDING_PAYMENT'
    | 'PAID'
    | 'SHIPPED'
    | 'COMPLETED'
    | 'CANCELLED'
    | 'REFUNDED';

export const ORDER_STATUS_CODE: Record<number, OrderStatus> = {
    0: 'PENDING_PAYMENT',
    1: 'PAID',
    2: 'SHIPPED',
    3: 'COMPLETED',
    4: 'CANCELLED',
    5: 'REFUNDED',
};

export const ORDER_STATUS_LABEL: Record<OrderStatus, string> = {
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
export type MessageTypeType = 'SYSTEM' | 'CHAT' | 'ORDER' | 'PAYMENT' | 'ACTIVITY';
export type MessageStatusType = 'UNREAD' | 'READ';

export const MESSAGE_TYPE_CODE: Record<number, MessageTypeType> = {
    1: 'SYSTEM',
    2: 'CHAT',
    3: 'ORDER',
    4: 'PAYMENT',
    5: 'ACTIVITY',
};

export const MESSAGE_TYPE_LABEL: Record<MessageTypeType, string> = {
    SYSTEM: '系统通知',
    CHAT: '聊天消息',
    ORDER: '订单消息',
    PAYMENT: '支付消息',
    ACTIVITY: '活动通知',
};

export const getMessageTypeLabel = (code: number | MessageTypeType): string => {
    if (typeof code === 'number') {
        const key = MESSAGE_TYPE_CODE[code];
        return key ? MESSAGE_TYPE_LABEL[key] : '未知类型';
    }
    return MESSAGE_TYPE_LABEL[code] ?? '未知类型';
};

export const MESSAGE_STATUS_CODE: Record<number, MessageStatusType> = {
    0: 'UNREAD',
    1: 'READ',
};

export const MESSAGE_STATUS_LABEL: Record<MessageStatusType, string> = {
    UNREAD: '未读',
    READ: '已读',
};

export const getMessageStatusLabel = (code: number | MessageStatusType): string => {
    if (typeof code === 'number') {
        const key = MESSAGE_STATUS_CODE[code];
        return key ? MESSAGE_STATUS_LABEL[key] : '未知状态';
    }
    return MESSAGE_STATUS_LABEL[code] ?? '未知状态';
};

export interface Message {
    id: number;
    type: number;
    typeDesc?: string;
    title: string;
    content: string;
    isRead: number;
    readDesc?: string;
    senderId: number | null;
    senderName: string | null;
    senderAvatar: string | null;
    receiverId: number;
    receiverName?: string;
    businessId?: number;
    createTime: string;
    updateTime?: string;
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

export interface OrderDetail extends Order {
    product?: {
        id: number;
        title: string;
        images: string[];
        condition: string;
    };
}
