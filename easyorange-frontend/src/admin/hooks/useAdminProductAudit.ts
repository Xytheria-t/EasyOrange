import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { adminApi } from '../api/adminApi';
import type { BatchAuditRequest, ProductAuditRequest, AuditLogVO } from '../types/admin';

export const ADMIN_AUDIT_KEYS = {
  all: ['admin', 'audit'] as const,
  logs: (id: number) => ['admin', 'audit-logs', id] as const,
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

export function useAuditLogs(productId: number | null) {
  return useQuery({
    queryKey: ADMIN_AUDIT_KEYS.logs(productId ?? 0),
    queryFn: async (): Promise<{ data: AuditLogVO[] }> => {
      if (!productId) {return { data: [] };}
      return adminApi.getAuditLogs(productId);
    },
    enabled: productId != null && productId > 0,
    select: (res): AuditLogVO[] => res.data,
  });
}
