import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { adminApi } from '../api/adminApi';
import type { AdminOrderDetail, AdminOrderQuery, OrderInterventionRequest, OrderStatsResponse } from '../types/admin';

export const ADMIN_ORDER_KEYS = {
  all: ['admin', 'orders'] as const,
  lists: () => [...ADMIN_ORDER_KEYS.all, 'list'] as const,
  list: (params: AdminOrderQuery) =>
    [...ADMIN_ORDER_KEYS.lists(),
      params.pageNum, params.pageSize, params.orderNo,
      params.buyerId, params.sellerId, params.status,
      params.paymentStatus, params.startTime, params.endTime,
    ] as const,
  details: () => [...ADMIN_ORDER_KEYS.all, 'detail'] as const,
  detail: (id: number) => [...ADMIN_ORDER_KEYS.details(), id] as const,
  stats: () => [...ADMIN_ORDER_KEYS.all, 'stats'] as const,
};

export function useAdminOrders(params: AdminOrderQuery) {
  return useQuery({
    queryKey: ADMIN_ORDER_KEYS.list(params),
    queryFn: async () => {
      const response = await adminApi.getOrders(params);
      return response.data;
    },
    staleTime: 30 * 1000,
    gcTime: 2 * 60 * 1000,
    retry: 1,
  });
}

export function useAdminOrderDetail(id: number) {
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
    mutationFn: async ({ id, data }: { id: number; data: OrderInterventionRequest }) => {
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
    mutationFn: async ({ id, data }: { id: number; data: OrderInterventionRequest }) => {
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
    mutationFn: async ({ id, data }: { id: number; data: OrderInterventionRequest }) => {
      const response = await adminApi.refundOrder(id, data);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ADMIN_ORDER_KEYS.all });
    },
  });
}
