import type { OrderStatus, ProductStatus } from '@/types';

export interface AdminUser {
    userId: string;
    username: string;
    nickname: string | null;
    avatar: string | null;
    email: string | null;
    phone: string | null;
    studentId: string | null;
    realName: string | null;
    userType: string | null;
    userTypeDesc: string | null;
    status: string | null;
    statusDesc: string | null;
    loginIp: string | null;
    loginDate: string | null;
    createTime: string | null;
    updateTime: string | null;
}

export interface DashboardStats {
    totalUsers: number;
    todayNewUsers: number;
    totalProducts: number;
    pendingProducts: number;
    totalOrders: number;
    todayOrders: number;
    totalRevenue: number;
    pendingReports: number;
}

export interface PendingReportItem {
    id: string;
    productId: string;
    productName: string | null;
    reason: string | null;
    reporterName: string | null;
    createTime: string | null;
}

export interface PendingItems {
    pendingReports: number;
    pendingOrders: number;
    pendingProducts: number;
    recentReports: PendingReportItem[];
}

export interface RecentUser {
    userId: string;
    username: string;
    nickname: string | null;
    avatar: string | null;
    email: string | null;
    phone: string | null;
    userType: string | null;
    userTypeDesc: string | null;
    status: string | null;
    statusDesc: string | null;
    createTime: string | null;
}

export interface RecentProduct {
    productId: string;
    name: string;
    price: number | null;
    mainImage: string | null;
    status: ProductStatus | null;
    statusDesc: string | null;
    sellerId: string | null;
    sellerName: string | null;
    categoryName: string | null;
    viewCount: number | null;
    createTime: string | null;
}

export interface AdminProduct {
    productId: string;
    name: string;
    description: string | null;
    price: number | null;
    originalPrice: number | null;
    stock: number | null;
    status: ProductStatus | null;
    statusDesc: string | null;
    conditionLevel: number | null;
    location: string | null;
    contactMethod: string | null;
    images: string[];
    mainImage: string | null;
    categoryId: string | null;
    categoryName: string | null;
    sellerId: string | null;
    sellerName: string | null;
    sellerAvatar: string | null;
    viewCount: number | null;
    createTime: string | null;
    updateTime: string | null;
}

export interface AdminUserQuery {
    pageNum: number;
    pageSize: number;
    keyword?: string;
    userType?: string;
    status?: string;
    startTime?: string;
    endTime?: string;
}

export interface AdminProductQuery {
    pageNum: number;
    pageSize: number;
    keyword?: string;
    categoryId?: string;
    status?: ProductStatus;
    sellerId?: string;
    startTime?: string;
    endTime?: string;
}

export interface UpdateStatusRequest {
    status: ProductStatus;
    reason?: string;
}

export interface UpdateUserStatusRequest {
    status: string;
}

export interface ActionResponse {
    isSuccess: boolean;
    message: string;
}

// ==================== Order Types ====================

export interface AdminOrderItem {
    itemId: string;
    productId: string;
    productName: string;
    productImage: string;
    unitPrice: number;
    quantity: number;
    subtotal: number;
}

export interface AdminOrder {
    orderId: string;
    orderNo: string;
    buyerId: string;
    buyerName: string;
    sellerId: string;
    sellerName: string;
    items: AdminOrderItem[];
    totalAmount: number;
    singleItem: boolean;
    status: OrderStatus;
    statusDesc: string;
    paymentStatus: string;
    paymentStatusDesc: string;
    createTime: string | null;
}

export interface AdminOrderDetail {
    orderId: string;
    orderNo: string;
    buyer: OrderParticipant;
    seller: OrderParticipant;
    items: AdminOrderDetailItem[];
    totalAmount: number;
    singleItem: boolean;
    status: OrderStatus;
    statusDesc: string;
    paymentStatus: string;
    paymentNo: string | null;
    paidAmount: number | null;
    refundedAmount: number | null;
    shippingAddress: ShippingAddress | null;
    remark: string | null;
    cancelReason: string | null;
    createTime: string | null;
    payTime: string | null;
    updateTime: string | null;
    cancelTime: string | null;
}

export interface AdminOrderDetailItem {
    itemId: string;
    productId: string;
    productName: string;
    productImage: string;
    unitPrice: number;
    quantity: number;
    subtotal: number;
}

export interface OrderParticipant {
    userId: string;
    nickname: string;
    avatar: string | null;
    phone: string | null;
}

export interface OrderProductInfo {
    productId: string;
    name: string;
    mainImage: string | null;
    price: number;
}

export interface ShippingAddress {
    receiverName: string;
    phone: string;
    detailAddress: string;
}

export interface AdminOrderQuery {
    pageNum: number;
    pageSize: number;
    orderNo?: string;
    buyerId?: string;
    sellerId?: string;
    status?: number;
    paymentStatus?: number;
    startTime?: string;
    endTime?: string;
}

export interface OrderInterventionRequest {
    reason: string;
}

export interface OrderStatsResponse {
    totalOrders: number;
    todayOrders: number;
    pendingPayment: number;
    toShip: number;
    toReceive: number;
    completed: number;
    cancelled: number;
    refunded: number;
    totalRevenue: number;
    todayRevenue: number;
}

// ==================== Report Types ====================

export interface AdminReport {
    reportId: string;
    productId: string;
    productName: string | null;
    productImage: string | null;
    reporterId: string;
    reporterName: string;
    reason: string;
    status: number;
    statusDesc: string;
    handleResult: string | null;
    handleRemark: string | null;
    createTime: string | null;
    handleTime: string | null;
}

export interface AdminReportQuery {
    pageNum: number;
    pageSize: number;
    status?: number;
    type?: number;
    keyword?: string;
    startTime?: string;
    endTime?: string;
}

export interface ReportHandleRequest {
    action: 'resolve' | 'dismiss';
    remark?: string;
}

export interface ReportStatsResponse {
    totalReports: number;
    pendingReports: number;
    resolvedReports: number;
    dismissedReports: number;
}

// ==================== Category Types ====================

export interface CategoryResponse {
    categoryId: string;
    name: string;
    parentId: string | null;
    parentName: string | null;
    level: number;
    sortOrder: number;
    status: number;
    productCount: number;
    createTime: string | null;
    updateTime: string | null;
}

export interface CategoryTreeResponse {
    categoryId: string;
    name: string;
    level: number;
    sortOrder: number;
    status: number;
    children: CategoryTreeResponse[];
}

export interface CategoryCreateRequest {
    name: string;
    parentId?: string;
    sortOrder?: number;
}

export interface CategoryUpdateRequest {
    name?: string;
    parentId?: string;
    sortOrder?: number;
    status?: number;
}

// ==================== Audit & User Operation Types ====================

export type AuditAction = 1 | 2 | 3;
export type AuditDimension = 'basic' | 'compliance' | 'image' | 'price';

export interface BatchAuditRequest {
    items: {
        productId: string;
        action: 1 | 2;
        reason?: string;
        dimensions?: AuditDimension[];
    }[];
}

export interface ProductAuditRequest {
    action: 1 | 2;
    reason?: string;
    dimensions?: AuditDimension[];
    remark?: string;
}

export interface TrendItem {
    month: string;
    users: number;
    products: number;
    orders: number;
}

export interface ActivityItem {
    time: string;
    text: string;
    type: 'user' | 'product' | 'order' | 'report';
}

export interface AuditLogResponse {
    id: string;
    productId: string;
    operatorId: string;
    operatorName: string;
    action: AuditAction;
    actionDesc: string;
    reason: string | null;
    dimensions: AuditDimension[];
    beforeStatus: number;
    beforeStatusDesc: string;
    afterStatus: number;
    afterStatusDesc: string;
    remark: string | null;
    createTime: string;
}

export interface UserRoleRequest {
    role: string;
}

export interface ResetPasswordRequest {
    newPassword: string;
}

export interface UserUnlockRequest {
    reason?: string;
}

// ==================== Rating Types ====================

export interface AdminRating {
    reviewId: string;
    productId: string;
    productName: string | null;
    userId: string;
    username: string | null;
    userAvatar: string | null;
    rating: number;
    content: string;
    replyContent: string | null;
    likes: number;
    status: number;
    createTime: string | null;
    updateTime: string | null;
}

export interface AdminRatingQuery {
    pageNum: number;
    pageSize: number;
    productId?: string;
    userId?: string;
    rating?: number;
    status?: number;
    keyword?: string;
    startTime?: string;
    endTime?: string;
}

export interface AdminRatingDeleteRequest {
    reason: string;
}

// ==================== Dashboard Chart Types ====================

export interface UserActivityItem {
    dayOfWeek: number;
    hour: number;
    count: number;
}

export interface TopProductItem {
    productId: string;
    name: string;
    viewCount: number;
    price: number;
    mainImage: string | null;
    status: number;
    statusDesc: string;
}
