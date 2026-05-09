import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { orderApi } from '@/api/orderApi';
import type { OrderQueryParams, CreateOrderRequest, Order, OrderDetail, PageResult } from '@/types';

const ORDER_KEYS = {
  all: ['orders'] as const,
  lists: () => [...ORDER_KEYS.all, 'list'] as const,
  myOrders: (params: OrderQueryParams) => [...ORDER_KEYS.all, 'my', params] as const,
  soldOrders: (params: OrderQueryParams) => [...ORDER_KEYS.all, 'sold', params] as const,
  details: () => [...ORDER_KEYS.all, 'detail'] as const,
  detail: (id: number) => [...ORDER_KEYS.details(), id] as const,
};

export function useMyOrders(params: OrderQueryParams = {}) {
  return useQuery<PageResult<Order>>({
    queryKey: ORDER_KEYS.myOrders(params),
    queryFn: async () => {
      const response = await orderApi.getMyOrders(params);
      return response.data;
    },
    staleTime: 30 * 1000,
  });
}

export function useSoldOrders(params: OrderQueryParams = {}) {
  return useQuery<PageResult<Order>>({
    queryKey: ORDER_KEYS.soldOrders(params),
    queryFn: async () => {
      const response = await orderApi.getSoldOrders(params);
      return response.data;
    },
    staleTime: 30 * 1000,
  });
}

export function useOrderDetail(id: number) {
  return useQuery<OrderDetail>({
    queryKey: ORDER_KEYS.detail(id),
    queryFn: async () => {
      const response = await orderApi.getDetail(id);
      return response.data;
    },
    enabled: id > 0,
    staleTime: 60 * 1000,
  });
}

export function useCreateOrder() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: CreateOrderRequest) => {
      const response = await orderApi.createOrder(data);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ORDER_KEYS.lists() });
    },
  });
}

export function useCancelOrder() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, reason }: { id: number; reason?: string }) => {
      await orderApi.cancel(id, reason);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ORDER_KEYS.lists() });
      queryClient.invalidateQueries({ queryKey: ORDER_KEYS.details() });
    },
  });
}

export function usePayOrder() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: number) => {
      await orderApi.pay(id);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ORDER_KEYS.lists() });
      queryClient.invalidateQueries({ queryKey: ORDER_KEYS.details() });
    },
  });
}

export function useShipOrder() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: number) => {
      await orderApi.ship(id);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ORDER_KEYS.lists() });
      queryClient.invalidateQueries({ queryKey: ORDER_KEYS.details() });
    },
  });
}

export function useReceiveOrder() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: number) => {
      await orderApi.receive(id);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ORDER_KEYS.lists() });
      queryClient.invalidateQueries({ queryKey: ORDER_KEYS.details() });
    },
  });
}

export function useRefundOrder() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, reason }: { id: number; reason?: string }) => {
      await orderApi.refund(id, reason);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ORDER_KEYS.lists() });
      queryClient.invalidateQueries({ queryKey: ORDER_KEYS.details() });
    },
  });
}
