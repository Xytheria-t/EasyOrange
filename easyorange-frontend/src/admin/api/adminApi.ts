import { request } from '@/api/core/request';
import type { PageResult, Result } from '@/types';
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
    BatchAuditResultResponse,
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

/**
 * 后端 `JacksonConfig` 给 `Long` 全局注册了 `ToStringSerializer`：DTO 里声明成包装类型
 * `Long` 的计数字段，线上到的是 `"24"` 而不是 `24`，但前端类型标注是 `number`。
 * 不收敛就会在算术处悄悄退化成字符串拼接——统计页分类占比恒显示 0% 就是这么来的
 * （`0 + "40" + "24"` 得到天文数字，再拿 40 去除，四舍五入后还是 0）。
 * 声明为数字的计数字段，一律在 API 层收敛。
 */
function coerceCounts<T>(row: T, keys: readonly (keyof T)[]): T {
    const next = { ...row };
    for (const key of keys) {
        if (next[key] != null) {
            next[key] = Number(next[key]) as T[keyof T];
        }
    }
    return next;
}

/** `PageResult.total` 是 `long`，同样是字符串；收敛后大数才有千位分隔。 */
function coercePageTotal<T>(res: Result<PageResult<T>>): Result<PageResult<T>> {
    return { ...res, data: { ...res.data, total: Number(res.data.total ?? 0) } };
}

export const adminApi = {
    async getDashboardStats() {
        const res = await request<DashboardStats>(`${ADMIN_API_PREFIX}/dashboard/stats`);
        return {
            ...res,
            data: coerceCounts(res.data, [
                'totalUsers',
                'todayNewUsers',
                'totalProducts',
                'pendingProducts',
                'totalOrders',
                'todayOrders',
            ]),
        };
    },

    async getTrend() {
        const res = await request<TrendItem[]>(`${ADMIN_API_PREFIX}/dashboard/trend`);
        return { ...res, data: (res.data ?? []).map(row => coerceCounts(row, ['users', 'products', 'orders'])) };
    },

    getActivity() {
        return request<ActivityItem[]>(`${ADMIN_API_PREFIX}/dashboard/activity`);
    },

    async getUsers(params: AdminUserQuery) {
        return coercePageTotal(
            await request<PageResult<AdminUser>>(`${ADMIN_API_PREFIX}/users`, {
                params: { ...params },
            })
        );
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

    async getProducts(params: AdminProductQuery) {
        return coercePageTotal(
            await request<PageResult<AdminProduct>>(`${ADMIN_API_PREFIX}/products`, {
                params: { ...params },
            })
        );
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
        // 后端是 @PostMapping：写成 PUT 会 405，且返回体是逐条结果而非 void
        return request<BatchAuditResultResponse>(`${ADMIN_API_PREFIX}/products/batch-audit`, {
            method: 'POST',
            body: data,
        });
    },

    getAuditLogs(id: string) {
        return request<AuditLogResponse[]>(`${ADMIN_API_PREFIX}/products/${id}/audit-logs`);
    },

    async getOrders(params: AdminOrderQuery) {
        return coercePageTotal(
            await request<PageResult<AdminOrder>>(`${ADMIN_API_PREFIX}/orders`, {
                params: { ...params },
            })
        );
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

    async getCategories() {
        // productCount 是 Long，不收敛会让统计页的占比算成字符串拼接
        const res = await request<CategoryResponse[]>(`${ADMIN_API_PREFIX}/categories`);
        return { ...res, data: (res.data ?? []).map(row => coerceCounts(row, ['productCount'])) };
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

    async getKnowledgeDocs(pageNum = 1, pageSize = 10) {
        return coercePageTotal(
            await request<PageResult<KnowledgeDoc>>(`${ADMIN_API_PREFIX}/knowledge`, {
                params: { pageNum, pageSize },
            })
        );
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
