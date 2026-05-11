import { useMutation, useQueryClient } from '@tanstack/react-query';
import { adminApi } from '../api/adminApi';
import type { BatchAuditRequest, ProductAuditRequest } from '../types/admin';

export const ADMIN_AUDIT_KEYS = {
  all: ['admin', 'audit'] as const,
};

export function useAuditProduct() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, data }: { id: number; data: ProductAuditRequest }) => {
      const response = await adminApi.auditProduct(id, data);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'products'] });
    },
  });
}

export function useBatchAuditProducts() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: BatchAuditRequest) => {
      const response = await adminApi.batchAuditProducts(data);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'products'] });
    },
  });
}
