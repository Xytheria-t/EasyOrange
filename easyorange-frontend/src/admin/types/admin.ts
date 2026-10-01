import type { OrderStatus, ProductStatus } from '@/types';

export interface AdminUser {
    userId: string;
    username: string;
    nickname: string | null;
    avatar: string | null;
    email: string | null;
    phone: string | null;
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
    refundReason: string | null;
    refundTime: string | null;
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
    /** 订单状态 String 枚举 code（与后端 OrderStatus 一致） */
    status?: OrderStatus;
    paymentStatus?: string;
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

// ==================== Category Types ====================

/** 分类节点 —— 列表与树共用一个类型（树的层级体现在 children 上）。 */
export interface CategoryResponse {
    id: string;
    name: string;
    /** 一级分类为 null。 */
    parentId: string | null;
    parentName: string | null;
    level: number;
    sortOrder: number;
    status: number;
    productCount: number;
    createTime: string | null;
    /** 仅 /tree 接口非空，列表接口为空数组。 */
    children: CategoryResponse[];
}

export interface CategoryCreateRequest {
    name: string;
    parentId?: string;
    icon?: string;
    sortOrder?: number;
}

export interface CategoryUpdateRequest {
    name: string;
    /** 不传表示移到一级；与当前父分类不同即触发移动。 */
    parentId?: string;
    icon?: string;
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

/** 批量审核逐条结果：单次上限 50 条，失败原因按条返回，不是全有全无。 */
export interface BatchAuditResultResponse {
    total: number;
    success: number;
    failed: number;
    errors: string[];
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
    type: 'user' | 'product' | 'order';
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
    /** 商品状态语义码（ProductStatus.code：DRAFT / PENDING_REVIEW / REJECTED / ONLINE / OFFLINE / SOLD），后端 AuditLogResponse 声明为 String */
    beforeStatus: string;
    beforeStatusDesc: string;
    afterStatus: string;
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

// ==================== Dashboard Chart Types ====================

/** 知识库文档（RAG 摄入管线，管理端）— 列表接口不返回正文（content） */
export interface KnowledgeDoc {
    id: string;
    title: string;
    source: string;
    status: 'PENDING' | 'INDEXED' | 'FAILED';
    chunkCount: number;
    createTime: string;
}

export interface CreateKnowledgeDocRequest {
    title: string;
    content: string;
    source: string;
}

// ==================== Retrieval Eval Types ====================

/** 检索评测线（按 case_id 前缀判定）— KNOWLEDGE 真实 embedding 语义空间 / ASSET 合成向量语料 */
export type RetrievalEvalLine = 'KNOWLEDGE' | 'ASSET';

/** 一次评测批次（run_id）的汇总行 — hit@5 与 MRR 的分母都是本批用例数 */
export interface RetrievalEvalRun {
    runId: string;
    line: RetrievalEvalLine;
    lineLabel: string;
    caseCount: number;
    hitCount: number;
    hitRateAt5: number;
    hitRatePct: number;
    mrr: number;
    createdAt: string | null;
}

/** 单条检索用例的采样明细 — hitRank 为首个命中位次，未命中时为 null */
export interface RetrievalEvalCase {
    caseId: string;
    queryText: string | null;
    goldDocIds: string | null;
    hitAt5: boolean;
    reciprocalRank: number;
    hitRank: number | null;
}
