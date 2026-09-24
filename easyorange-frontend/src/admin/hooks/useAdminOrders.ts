import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { PageResult } from '@/types';
import { adminApi } from '../api/adminApi';
import type {
    AdminOrder,
    AdminOrderDetail,
    AdminOrderQuery,
    OrderInterventionRequest,
    OrderStatsResponse,
} from '../types/admin';

/** 同 useAdminProducts 的 selectList：只留列表页消费的 records / total / current */
const selectList = (data: PageResult<AdminOrder>) => ({
    records: data.records,
    total: data.total,
    current: data.current,
});

export const ADMIN_ORDER_KEYS = {
    all: ['admin', 'orders'] as const,
    lists: () => [...ADMIN_ORDER_KEYS.all, 'list'] as const,
    list: (params: AdminOrderQuery) =>
        [
            ...ADMIN_ORDER_KEYS.lists(),
            params.pageNum,
            params.pageSize,
            params.orderNo,
            params.buyerId,
            params.sellerId,
            params.status,
            params.paymentStatus,
            params.startTime,
            params.endTime,
        ] as const,
    details: () => [...ADMIN_ORDER_KEYS.all, 'detail'] as const,
    detail: (id: string) => [...ADMIN_ORDER_KEYS.details(), id] as const,
    stats: () => [...ADMIN_ORDER_KEYS.all, 'stats'] as const,
};

export function useAdminOrders(params: AdminOrderQuery) {
    return useQuery({
        queryKey: ADMIN_ORDER_KEYS.list(params),
        queryFn: async () => {
            const response = await adminApi.getOrders(params);
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

export function useAdminOrderDetail(id: string) {
    return useQuery<AdminOrderDetail>({
        queryKey: ADMIN_ORDER_KEYS.detail(id),
        queryFn: async () => {
            const response = await adminApi.getOrderById(id);
            return response.data;
        },
        enabled: !!id,
        staleTime: 60 * 1000,
        gcTime: 5 * 60 * 1000,
        retry: 1,
    });
}

export function useAdminOrderStats() {
    return useQuery<OrderStatsResponse>({
        queryKey: ADMIN_ORDER_KEYS.stats(),
        queryFn: async () => {
            const response = await adminApi.getOrderStats();
            return response.data;
        },
        staleTime: 30 * 1000,
        gcTime: 2 * 60 * 1000,
        retry: 1,
    });
}

export function useAdminCancelOrder() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ id, data }: { id: string; data: OrderInterventionRequest }) => {
            const response = await adminApi.cancelOrder(id, data);
            return response.data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ADMIN_ORDER_KEYS.all });
        },
    });
}

export function useForceCompleteOrder() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ id, data }: { id: string; data: OrderInterventionRequest }) => {
            const response = await adminApi.forceCompleteOrder(id, data);
            return response.data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ADMIN_ORDER_KEYS.all });
        },
    });
}

export function useAdminRefundOrder() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ id, data }: { id: string; data: OrderInterventionRequest }) => {
            const response = await adminApi.refundOrder(id, data);
            return response.data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ADMIN_ORDER_KEYS.all });
        },
    });
}
