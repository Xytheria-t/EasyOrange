import { request } from '@/api/core/request';
import type {
    DashboardStats,
    PendingItems,
    RecentUser,
    RecentProduct,
    AdminUser,
    AdminProduct,
    AdminUserQuery,
    AdminProductQuery,
    UpdateStatusRequest,
    PageResponse,
    ActionResponse
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

    getUsers(params: AdminUserQuery) {
        return request<PageResponse<AdminUser>['data']>(`${ADMIN_API_PREFIX}/users`, {
            params: { ...params }
        });
    },

    getUserById(id: string) {
        return request<AdminUser>(`${ADMIN_API_PREFIX}/users/${id}`);
    },

    updateUserStatus(id: string, data: UpdateStatusRequest) {
        return request<ActionResponse['data']>(`${ADMIN_API_PREFIX}/users/${id}/status`, {
            method: 'PUT',
            body: data
        });
    },

    getProducts(params: AdminProductQuery) {
        return request<PageResponse<AdminProduct>['data']>(`${ADMIN_API_PREFIX}/products`, {
            params: { ...params }
        });
    },

    getProductById(id: string) {
        return request<AdminProduct>(`${ADMIN_API_PREFIX}/products/${id}`);
    },

    updateProductStatus(id: string, data: UpdateStatusRequest) {
        return request<ActionResponse['data']>(`${ADMIN_API_PREFIX}/products/${id}/status`, {
            method: 'PUT',
            body: data
        });
    }
};
