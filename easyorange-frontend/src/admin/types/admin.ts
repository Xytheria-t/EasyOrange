export type UserType = string;
export type UserStatus = string;
export type ProductStatus = number;

export interface AdminUser {
  userId: number;
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
  id: number;
  productId: number;
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
  userId: number;
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
  productId: number;
  name: string;
  price: number | null;
  mainImage: string | null;
  status: number | null;
  statusDesc: string | null;
  sellerId: number | null;
  sellerName: string | null;
  categoryName: string | null;
  viewCount: number | null;
  createTime: string | null;
}

export interface AdminProduct {
  productId: number;
  name: string;
  description: string | null;
  price: number | null;
  originalPrice: number | null;
  stock: number | null;
  status: number | null;
  statusDesc: string | null;
  conditionLevel: number | null;
  location: string | null;
  contactMethod: string | null;
  images: string[];
  mainImage: string | null;
  categoryId: number | null;
  categoryName: string | null;
  sellerId: number | null;
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
  categoryId?: number;
  status?: number;
  sellerId?: number;
  startTime?: string;
  endTime?: string;
}

export interface UpdateStatusRequest {
  status: number;
  reason?: string;
}

export interface PageData<T> {
  records: T[];
  total: number;
  current: number;
  size: number;
  pages: number;
}

export interface ActionResponse {
  success: boolean;
  message: string;
}

// ==================== Order Types ====================

export interface AdminOrder {
  orderId: number;
  orderNo: string;
  buyerId: number;
  buyerName: string;
  sellerId: number;
  sellerName: string;
  productId: number;
  productName: string;
  amount: number;
  status: number;
  statusDesc: string;
  paymentStatus: number;
  paymentStatusDesc: string;
  createTime: string | null;
}

export interface AdminOrderDetail {
  orderId: number;
  orderNo: string;
  buyer: OrderParticipant;
  seller: OrderParticipant;
  product: OrderProductInfo;
  amount: number;
  status: number;
  statusDesc: string;
  paymentStatus: number;
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

export interface OrderParticipant {
  userId: number;
  nickname: string;
  avatar: string | null;
  phone: string | null;
}

export interface OrderProductInfo {
  productId: number;
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
  buyerId?: number;
  sellerId?: number;
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
  reportId: number;
  productId: number;
  productName: string | null;
  productImage: string | null;
  reporterId: number;
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
  categoryId: number;
  name: string;
  parentId: number | null;
  parentName: string | null;
  level: number;
  sortOrder: number;
  status: number;
  productCount: number;
  createTime: string | null;
  updateTime: string | null;
}

export interface CategoryTreeResponse {
  categoryId: number;
  name: string;
  level: number;
  sortOrder: number;
  status: number;
  children: CategoryTreeResponse[];
}

export interface CategoryCreateRequest {
  name: string;
  parentId?: number;
  sortOrder?: number;
}

export interface CategoryUpdateRequest {
  name?: string;
  parentId?: number;
  sortOrder?: number;
  status?: number;
}

// ==================== Audit & User Operation Types ====================

export type AuditAction = 1 | 2 | 3;
export type AuditDimension = 'basic' | 'compliance' | 'image' | 'price';

export interface BatchAuditRequest {
  items: {
    productId: number;
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
  id: number;
  productId: number;
  operatorId: number;
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

// ==================== AI Review Types ====================

export interface AiReviewResult {
  suggestedAction: boolean;
  suggestedActionDesc: string;
  confidenceScore: number;
  riskFlags: string[];
  reasoning: string;
}

// ==================== Review Types ====================

export interface AdminReview {
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

export interface AdminReviewQuery {
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

export interface AdminReviewDeleteRequest {
  reason: string;
}

// ==================== Dashboard Chart Types ====================

export interface UserActivityItem {
  dayOfWeek: number;
  hour: number;
  count: number;
}

export interface TopProductItem {
  productId: number;
  name: string;
  viewCount: number;
  price: number;
  mainImage: string | null;
  status: number;
  statusDesc: string;
}
