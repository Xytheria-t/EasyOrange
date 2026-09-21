import { request } from '@/api/core/request';
import type { PageResult } from '@/types';
import type {
    ActivityItem,
    AdminOrder,
    AdminOrderDetail,
    AdminOrderQuery,
    AdminProduct,
    AdminProductQuery,
    AdminUser,
    AdminUserQuery,
    AuditLogResponse,
    BatchAuditRequest,
    CategoryCreateRequest,
    CategoryResponse,
    CategoryTreeResponse,
    CategoryUpdateRequest,
    CreateKnowledgeDocRequest,
    DashboardStats,
    KnowledgeDoc,
    OrderInterventionRequest,
    OrderStatsResponse,
    ProductAuditRequest,
    ResetPasswordRequest,
    TrendItem,
    UpdateStatusRequest,
    UpdateUserStatusRequest,
    UserRoleRequest,
    UserUnlockRequest,
} from '../types/admin';

const ADMIN_API_PREFIX = '/admin';

export const adminApi = {
    getDashboardStats() {
        return request<DashboardStats>(`${ADMIN_API_PREFIX}/dashboard/stats`);
    },

    getTrend() {
        return request<TrendItem[]>(`${ADMIN_API_PREFIX}/dashboard/trend`);
    },

    getActivity() {
        return request<ActivityItem[]>(`${ADMIN_API_PREFIX}/dashboard/activity`);
    },

    getUsers(params: AdminUserQuery) {
        return request<PageResult<AdminUser>>(`${ADMIN_API_PREFIX}/users`, {
            params: { ...params },
        });
    },

    getUserById(id: string) {
        return request<AdminUser>(`${ADMIN_API_PREFIX}/users/${id}`);
    },

    updateUserStatus(id: string, data: UpdateUserStatusRequest) {
        return request<void>(`${ADMIN_API_PREFIX}/users/${id}/status`, {
            method: 'PUT',
            body: data,
        });
    },

    resetPassword(id: string, data: ResetPasswordRequest) {
        return request<void>(`${ADMIN_API_PREFIX}/users/${id}/reset-password`, {
            method: 'PUT',
            body: data,
        });
    },

    unlockUser(id: string, data: UserUnlockRequest) {
        return request<void>(`${ADMIN_API_PREFIX}/users/${id}/unlock`, {
            method: 'PUT',
            body: data,
        });
    },

    updateUserRole(id: string, data: UserRoleRequest) {
        return request<void>(`${ADMIN_API_PREFIX}/users/${id}/role`, {
            method: 'PUT',
            body: data,
        });
    },

    getProducts(params: AdminProductQuery) {
        return request<PageResult<AdminProduct>>(`${ADMIN_API_PREFIX}/products`, {
            params: { ...params },
        });
    },

    getProductById(id: string) {
        return request<AdminProduct>(`${ADMIN_API_PREFIX}/products/${id}`);
    },

    updateProductStatus(id: string, data: UpdateStatusRequest) {
        return request<void>(`${ADMIN_API_PREFIX}/products/${id}/status`, {
            method: 'PUT',
            body: data,
        });
    },

    auditProduct(id: string, data: ProductAuditRequest) {
        return request<void>(`${ADMIN_API_PREFIX}/products/${id}/audit`, {
            method: 'PUT',
            body: data,
        });
    },

    batchAuditProducts(data: BatchAuditRequest) {
        return request<void>(`${ADMIN_API_PREFIX}/products/batch-audit`, {
            method: 'PUT',
            body: data,
        });
    },

    getAuditLogs(id: string) {
        return request<AuditLogResponse[]>(`${ADMIN_API_PREFIX}/products/${id}/audit-logs`);
    },

    getOrders(params: AdminOrderQuery) {
        return request<PageResult<AdminOrder>>(`${ADMIN_API_PREFIX}/orders`, {
            params: { ...params },
        });
    },

    getOrderById(id: string) {
        return request<AdminOrderDetail>(`${ADMIN_API_PREFIX}/orders/${id}`);
    },

    getOrderStats() {
        return request<OrderStatsResponse>(`${ADMIN_API_PREFIX}/orders/stats`);
    },

    cancelOrder(id: string, data: OrderInterventionRequest) {
        return request<void>(`${ADMIN_API_PREFIX}/orders/${id}/cancel`, {
            method: 'PUT',
            body: data,
        });
    },

    forceCompleteOrder(id: string, data: OrderInterventionRequest) {
        return request<void>(`${ADMIN_API_PREFIX}/orders/${id}/force-complete`, {
            method: 'PUT',
            body: data,
        });
    },

    refundOrder(id: string, data: OrderInterventionRequest) {
        return request<void>(`${ADMIN_API_PREFIX}/orders/${id}/refund`, {
            method: 'PUT',
            body: data,
        });
    },

    getCategories() {
        return request<CategoryResponse[]>(`${ADMIN_API_PREFIX}/categories`);
    },

    getCategoryTree() {
        return request<CategoryTreeResponse[]>(`${ADMIN_API_PREFIX}/categories/tree`);
    },

    createCategory(data: CategoryCreateRequest) {
        return request<number>(`${ADMIN_API_PREFIX}/categories`, {
            method: 'POST',
            body: data,
        });
    },

    updateCategory(id: string, data: CategoryUpdateRequest) {
        return request<void>(`${ADMIN_API_PREFIX}/categories/${id}`, {
            method: 'PUT',
            body: data,
        });
    },

    updateCategoryStatus(id: string, status: number) {
        return request<void>(`${ADMIN_API_PREFIX}/categories/${id}/status`, {
            method: 'PUT',
            params: { status },
        });
    },

    deleteCategory(id: string) {
        return request<void>(`${ADMIN_API_PREFIX}/categories/${id}`, {
            method: 'DELETE',
        });
    },

    getKnowledgeDocs(pageNum = 1, pageSize = 10) {
        return request<PageResult<KnowledgeDoc>>(`${ADMIN_API_PREFIX}/knowledge`, {
            params: { pageNum, pageSize },
        });
    },

    createKnowledgeDoc(data: CreateKnowledgeDocRequest) {
        return request<string>(`${ADMIN_API_PREFIX}/knowledge`, {
            method: 'POST',
            body: data,
        });
    },

    deleteKnowledgeDoc(id: string) {
        return request<void>(`${ADMIN_API_PREFIX}/knowledge/${id}`, {
            method: 'DELETE',
        });
    },

    reindexKnowledge() {
        return request<number>(`${ADMIN_API_PREFIX}/knowledge/reindex`, {
            method: 'POST',
        });
    },
};
