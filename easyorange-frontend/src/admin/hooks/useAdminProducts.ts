import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { PageResult } from '@/types';
import { adminApi } from '../api/adminApi';
import type { AdminProduct, AdminProductQuery, UpdateStatusRequest } from '../types/admin';

/**
 * 只暴露列表页真正消费的三个字段；`size` / `pages` 全仓无人使用，留在返回形状里
 * 只会让整包数据参与结构共享的深比较。
 *
 * 必须定义在模块作用域而非内联：query-core 对「select 引用未变 + data 未变」有
 * 复用快路径（queryObserver 的 #selectResult），内联会让每次渲染都重跑 select。
 */
const selectList = (data: PageResult<AdminProduct>) => ({
    records: data.records,
    total: data.total,
    current: data.current,
});

export const ADMIN_PRODUCT_KEYS = {
    all: ['admin', 'products'] as const,
    lists: () => [...ADMIN_PRODUCT_KEYS.all, 'list'] as const,
    list: (params: AdminProductQuery) =>
        [
            ...ADMIN_PRODUCT_KEYS.lists(),
            params.pageNum,
            params.pageSize,
            params.keyword,
            params.categoryId,
            params.status,
            params.sellerId,
            params.startTime,
            params.endTime,
        ] as const,
    details: () => [...ADMIN_PRODUCT_KEYS.all, 'detail'] as const,
    detail: (id: string) => [...ADMIN_PRODUCT_KEYS.details(), id] as const,
};

export function useAdminProducts(params: AdminProductQuery) {
    return useQuery({
        queryKey: ADMIN_PRODUCT_KEYS.list(params),
        queryFn: async () => {
            const response = await adminApi.getProducts(params);
            return response.data;
        },
        select: selectList,
        // v5 里 keepPreviousData 改名 placeholderData：翻页 / 换筛选时沿用上一页数据，
        // 否则表格会退回骨架屏再整页重排
        placeholderData: keepPreviousData,
        staleTime: 30 * 1000,
        gcTime: 2 * 60 * 1000,
        retry: 1,
    });
}

export function useAdminProductDetail(id: string) {
    return useQuery<AdminProduct>({
        queryKey: ADMIN_PRODUCT_KEYS.detail(id),
        queryFn: async () => {
            const response = await adminApi.getProductById(id);
            return response.data;
        },
        enabled: !!id,
        staleTime: 60 * 1000,
        // 审核抽屉每次打开都重新取：缓存 60 秒内也可能是审核前的旧状态，
        // 审核员看到过期价格再操作，代价是真金白银
        refetchOnMount: 'always',
        gcTime: 5 * 60 * 1000,
        retry: 1,
    });
}

export function useUpdateProductStatus() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ id, data }: { id: string; data: UpdateStatusRequest }) => {
            const response = await adminApi.updateProductStatus(id, data);
            return response.data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ADMIN_PRODUCT_KEYS.lists() });
        },
    });
}
