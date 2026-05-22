import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { orderApi } from '@/api/orderApi';
import type { OrderQueryParams, CreateOrderRequest, Order, OrderDetail, PageResult } from '@/types';

const ORDER_KEYS = {
  all: ['orders'] as const,
  lists: () => [...ORDER_KEYS.all, 'list'] as const,
  myOrders: (params: OrderQueryParams) => [...ORDER_KEYS.all, 'my', params] as const,
  soldOrders: (params: OrderQueryParams) => [...ORDER_KEYS.all, 'sold', params] as const,
  details: () => [...ORDER_KEYS.all, 'detail'] as const,
  detail: (id: string) => [...ORDER_KEYS.details(), id] as const,
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

export function useOrderDetail(id: string) {
  return useQuery<OrderDetail>({
    queryKey: ORDER_KEYS.detail(id),
    queryFn: async () => {
      const response = await orderApi.getOrderDetail(id);
      return response.data;
    },
    enabled: !!id,
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
      queryClient.invalidateQueries({ queryKey: ORDER_KEYS.all });
    },
  });
}

export function useCancelOrder() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, reason }: { id: string; reason?: string }) => {
      await orderApi.cancelOrder(id, reason);
    },
    onMutate: async ({ id }) => {
      await queryClient.cancelQueries({ queryKey: ORDER_KEYS.all });

      const previousLists = queryClient.getQueriesData<{ records: Array<{ id: string; status: number }> }>({ queryKey: ORDER_KEYS.all });
      const previousDetail = queryClient.getQueryData(ORDER_KEYS.detail(id));

      queryClient.setQueriesData<{ records: Array<{ id: string; status: number }> }>(
        { queryKey: ORDER_KEYS.all },
        (oldData) => {
          if (!oldData?.records) {return oldData;}
          return {
            ...oldData,
            records: oldData.records.map((order) =>
              order.id === id ? { ...order, status: 4 } : order
            ),
          };
        }
      );

      queryClient.setQueryData(ORDER_KEYS.detail(id), (oldData: { status: number } | undefined) => {
        if (!oldData) {return oldData;}
        return { ...oldData, status: 4 };
      });

      return { previousLists, previousDetail };
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ORDER_KEYS.all });
    },
    onError: (_err, _vars, context) => {
      if (context?.previousLists) {
        for (const [queryKey, data] of context.previousLists) {
          queryClient.setQueryData(queryKey, data);
        }
      }
      if (context?.previousDetail) {
        queryClient.setQueryData(ORDER_KEYS.detail(_vars.id), context.previousDetail);
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ORDER_KEYS.all });
    },
  });
}

export function usePayOrder() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: string) => {
      await orderApi.payOrder(id);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ORDER_KEYS.all });
    },
  });
}

export function useShipOrder() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: string) => {
      await orderApi.shipOrder(id);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ORDER_KEYS.all });
    },
  });
}

export function useReceiveOrder() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: string) => {
      await orderApi.receiveOrder(id);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ORDER_KEYS.all });
    },
  });
}

export function useRefundOrder() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, reason }: { id: string; reason?: string }) => {
      await orderApi.refundOrder(id, reason);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ORDER_KEYS.all });
    },
  });
}
