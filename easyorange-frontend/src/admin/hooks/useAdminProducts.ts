import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { adminApi } from '../api/adminApi';
import type { AdminProduct, AdminProductQuery, UpdateStatusRequest } from '../types/admin';

export const ADMIN_PRODUCT_KEYS = {
  all: ['admin', 'products'] as const,
  lists: () => [...ADMIN_PRODUCT_KEYS.all, 'list'] as const,
  list: (params: AdminProductQuery) =>
    [...ADMIN_PRODUCT_KEYS.lists(),
      params.pageNum, params.pageSize, params.keyword,
      params.categoryId, params.status, params.sellerId,
      params.startTime, params.endTime,
    ] as const,
  details: () => [...ADMIN_PRODUCT_KEYS.all, 'detail'] as const,
  detail: (id: number) => [...ADMIN_PRODUCT_KEYS.details(), id] as const,
};

export function useAdminProducts(params: AdminProductQuery) {
  return useQuery({
    queryKey: ADMIN_PRODUCT_KEYS.list(params),
    queryFn: async () => {
      const response = await adminApi.getProducts(params);
      return response.data;
    },
    staleTime: 30 * 1000,
    gcTime: 2 * 60 * 1000,
    retry: 1,
  });
}

export function useAdminProductDetail(id: number) {
  return useQuery<AdminProduct>({
    queryKey: ADMIN_PRODUCT_KEYS.detail(id),
    queryFn: async () => {
      const response = await adminApi.getProductById(id);
      return response.data;
    },
    enabled: !!id,
    staleTime: 60 * 1000,
    gcTime: 5 * 60 * 1000,
    retry: 1,
  });
}

export function useUpdateProductStatus() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, data }: { id: number; data: UpdateStatusRequest }) => {
      const response = await adminApi.updateProductStatus(id, data);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ADMIN_PRODUCT_KEYS.lists() });
    },
  });
}
