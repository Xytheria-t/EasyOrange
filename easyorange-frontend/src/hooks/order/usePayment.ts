import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { paymentApi, type CreatePaymentRequest } from '@/api/paymentApi';

const PAYMENT_KEYS = {
  all: ['payments'] as const,
  details: () => [...PAYMENT_KEYS.all, 'detail'] as const,
  detail: (id: number) => [...PAYMENT_KEYS.details(), id] as const,
  byOrder: (orderId: number) => [...PAYMENT_KEYS.all, 'order', orderId] as const,
  status: (id: number) => [...PAYMENT_KEYS.all, 'status', id] as const,
};

export function usePayment(id: number) {
  return useQuery({
    queryKey: PAYMENT_KEYS.detail(id),
    queryFn: async () => {
      const response = await paymentApi.getPaymentById(id);
      return response.data;
    },
    enabled: id > 0,
    staleTime: 10 * 1000,
  });
}

export function usePaymentByOrder(orderId: number) {
  return useQuery({
    queryKey: PAYMENT_KEYS.byOrder(orderId),
    queryFn: async () => {
      const response = await paymentApi.getPaymentByOrder(orderId);
      return response.data;
    },
    enabled: orderId > 0,
    staleTime: 10 * 1000,
  });
}

export function usePaymentStatus(id: number, enabled = true) {
  return useQuery({
    queryKey: PAYMENT_KEYS.status(id),
    queryFn: async () => {
      const response = await paymentApi.getPaymentStatus(id);
      return response.data;
    },
    enabled: enabled && id > 0,
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      if (status === 'SUCCESS' || status === 'FAILED' || status === 'CLOSED' || status === 'REFUNDED') {
        return false;
      }
      return 3000;
    },
    staleTime: 0,
  });
}

export function useCreatePayment() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: CreatePaymentRequest) => {
      const response = await paymentApi.createPayment(data);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: PAYMENT_KEYS.all });
    },
  });
}

export function useRefundPayment() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: number) => {
      await paymentApi.refund(id);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: PAYMENT_KEYS.all });
    },
  });
}

export function useClosePayment() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: number) => {
      await paymentApi.close(id);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: PAYMENT_KEYS.all });
    },
  });
}
