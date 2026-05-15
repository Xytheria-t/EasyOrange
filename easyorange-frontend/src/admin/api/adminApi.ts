import { request } from '@/api/core/request';
import type {
  ActivityItem,
  DashboardStats,
  PendingItems,
  RecentUser,
  RecentProduct,
  AdminUser,
  AdminProduct,
  AdminUserQuery,
  AdminProductQuery,
  TrendItem,
  TopProductItem,
  UpdateStatusRequest,
  PageData,
  AdminOrder,
  AdminOrderDetail,
  AdminOrderQuery,
  OrderInterventionRequest,
  OrderStatsVO,
  AdminReport,
  AdminReportQuery,
  ReportHandleRequest,
  ReportStatsVO,
  CategoryVO,
  CategoryTreeVO,
  CategoryCreateRequest,
  CategoryUpdateRequest,
  BatchAuditRequest,
  ProductAuditRequest,
  AuditLogVO,
  UserRoleRequest,
  ResetPasswordRequest,
  UserUnlockRequest,
  AdminReview,
  AdminReviewQuery,
  AdminReviewDeleteRequest,
  UserActivityItem,
} from '../types/admin';

const ADMIN_API_PREFIX = '/admin';

export const adminApi = {
  getDashboardStats() {
    return request<DashboardStats>(`${ADMIN_API_PREFIX}/dashboard/stats`);
  },

  getPendingItems() {
    return request<PendingItems>(`${ADMIN_API_PREFIX}/dashboard/pending`);
  },

  getRecentUsers(limit = 5) {
    return request<RecentUser[]>(`${ADMIN_API_PREFIX}/dashboard/recent-users`, {
      params: { limit }
    });
  },

  getRecentProducts(limit = 5) {
    return request<RecentProduct[]>(`${ADMIN_API_PREFIX}/dashboard/recent-products`, {
      params: { limit }
    });
  },

  getTrend() {
    return request<TrendItem[]>(`${ADMIN_API_PREFIX}/dashboard/trend`);
  },

  getActivity() {
    return request<ActivityItem[]>(`${ADMIN_API_PREFIX}/dashboard/activity`);
  },

  getUserActivityHeatmap() {
    return request<UserActivityItem[]>(`${ADMIN_API_PREFIX}/dashboard/user-activity-heatmap`);
  },

  getTopProducts(limit = 10) {
    return request<TopProductItem[]>(`${ADMIN_API_PREFIX}/dashboard/top-products`, {
      params: { limit }
    });
  },

  getUsers(params: AdminUserQuery) {
    return request<PageData<AdminUser>>(`${ADMIN_API_PREFIX}/users`, {
      params: { ...params }
    });
  },

  getUserById(id: number) {
    return request<AdminUser>(`${ADMIN_API_PREFIX}/users/${id}`);
  },

  updateUserStatus(id: number, data: UpdateStatusRequest) {
    return request<void>(`${ADMIN_API_PREFIX}/users/${id}/status`, {
      method: 'PUT',
      body: data
    });
  },

  resetPassword(id: number, data: ResetPasswordRequest) {
    return request<void>(`${ADMIN_API_PREFIX}/users/${id}/reset-password`, {
      method: 'PUT',
      body: data
    });
  },

  unlockUser(id: number, data: UserUnlockRequest) {
    return request<void>(`${ADMIN_API_PREFIX}/users/${id}/unlock`, {
      method: 'PUT',
      body: data
    });
  },

  updateUserRole(id: number, data: UserRoleRequest) {
    return request<void>(`${ADMIN_API_PREFIX}/users/${id}/role`, {
      method: 'PUT',
      body: data
    });
  },

  getProducts(params: AdminProductQuery) {
    return request<PageData<AdminProduct>>(`${ADMIN_API_PREFIX}/products`, {
      params: { ...params }
    });
  },

  getProductById(id: number) {
    return request<AdminProduct>(`${ADMIN_API_PREFIX}/products/${id}`);
  },

  updateProductStatus(id: number, data: UpdateStatusRequest) {
    return request<void>(`${ADMIN_API_PREFIX}/products/${id}/status`, {
      method: 'PUT',
      body: data
    });
  },

  auditProduct(id: number, data: ProductAuditRequest) {
    return request<void>(`${ADMIN_API_PREFIX}/products/${id}/audit`, {
      method: 'PUT',
      body: data
    });
  },

  batchAuditProducts(data: BatchAuditRequest) {
    return request<void>(`${ADMIN_API_PREFIX}/products/batch-audit`, {
      method: 'PUT',
      body: data
    });
  },

  getAuditLogs(id: number) {
    return request<AuditLogVO[]>(`${ADMIN_API_PREFIX}/products/${id}/audit-logs`);
  },

  getOrders(params: AdminOrderQuery) {
    return request<PageData<AdminOrder>>(`${ADMIN_API_PREFIX}/orders`, {
      params: { ...params }
    });
  },

  getOrderById(id: number) {
    return request<AdminOrderDetail>(`${ADMIN_API_PREFIX}/orders/${id}`);
  },

  getOrderStats() {
    return request<OrderStatsVO>(`${ADMIN_API_PREFIX}/orders/stats`);
  },

  cancelOrder(id: number, data: OrderInterventionRequest) {
    return request<void>(`${ADMIN_API_PREFIX}/orders/${id}/cancel`, {
      method: 'PUT',
      body: data
    });
  },

  forceCompleteOrder(id: number, data: OrderInterventionRequest) {
    return request<void>(`${ADMIN_API_PREFIX}/orders/${id}/force-complete`, {
      method: 'PUT',
      body: data
    });
  },

  refundOrder(id: number, data: OrderInterventionRequest) {
    return request<void>(`${ADMIN_API_PREFIX}/orders/${id}/refund`, {
      method: 'PUT',
      body: data
    });
  },

  getCategories() {
    return request<CategoryVO[]>(`${ADMIN_API_PREFIX}/categories`);
  },

  getCategoryTree() {
    return request<CategoryTreeVO[]>(`${ADMIN_API_PREFIX}/categories/tree`);
  },

  createCategory(data: CategoryCreateRequest) {
    return request<number>(`${ADMIN_API_PREFIX}/categories`, {
      method: 'POST',
      body: data
    });
  },

  updateCategory(id: number, data: CategoryUpdateRequest) {
    return request<void>(`${ADMIN_API_PREFIX}/categories/${id}`, {
      method: 'PUT',
      body: data
    });
  },

  updateCategoryStatus(id: number, status: number) {
    return request<void>(`${ADMIN_API_PREFIX}/categories/${id}/status`, {
      method: 'PUT',
      params: { status }
    });
  },

  deleteCategory(id: number) {
    return request<void>(`${ADMIN_API_PREFIX}/categories/${id}`, {
      method: 'DELETE'
    });
  },

  getReports(params: AdminReportQuery) {
    return request<PageData<AdminReport>>(`${ADMIN_API_PREFIX}/reports`, {
      params: { ...params }
    });
  },

  getReportById(id: number) {
    return request<AdminReport>(`${ADMIN_API_PREFIX}/reports/${id}`);
  },

  getReportStats() {
    return request<ReportStatsVO>(`${ADMIN_API_PREFIX}/reports/stats`);
  },

  handleReport(id: number, data: ReportHandleRequest) {
    return request<void>(`${ADMIN_API_PREFIX}/reports/${id}/handle`, {
      method: 'PUT',
      body: data
    });
  },

  // ==================== Review Management ====================

  getReviews(params: AdminReviewQuery) {
    return request<PageData<AdminReview>>(`${ADMIN_API_PREFIX}/reviews`, {
      params: { ...params }
    });
  },

  getReviewById(id: string) {
    return request<AdminReview>(`${ADMIN_API_PREFIX}/reviews/${id}`);
  },

  deleteReview(id: string, data: AdminReviewDeleteRequest) {
    return request<void>(`${ADMIN_API_PREFIX}/reviews/${id}`, {
      method: 'DELETE',
      body: data
    });
  },
};
