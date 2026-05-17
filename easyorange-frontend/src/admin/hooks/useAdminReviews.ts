import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { adminApi } from '../api/adminApi';
import type { AdminReview, AdminReviewQuery, AdminReviewDeleteRequest } from '../types/admin';

export const ADMIN_REVIEW_KEYS = {
  all: ['admin', 'reviews'] as const,
  lists: () => [...ADMIN_REVIEW_KEYS.all, 'list'] as const,
  list: (params: AdminReviewQuery) => [...ADMIN_REVIEW_KEYS.lists(), params] as const,
  details: () => [...ADMIN_REVIEW_KEYS.all, 'detail'] as const,
  detail: (id: string) => [...ADMIN_REVIEW_KEYS.details(), id] as const,
};

export function useAdminReviews(params: AdminReviewQuery) {
  return useQuery({
    queryKey: ADMIN_REVIEW_KEYS.list(params),
    queryFn: async () => {
      const response = await adminApi.getReviews(params);
      return response.data;
    },
    staleTime: 30 * 1000,
    gcTime: 2 * 60 * 1000,
    retry: 1,
  });
}

export function useAdminReviewDetail(id: string) {
  return useQuery<AdminReview>({
    queryKey: ADMIN_REVIEW_KEYS.detail(id),
    queryFn: async () => {
      const response = await adminApi.getReviewById(id);
      return response.data;
    },
    enabled: !!id,
    staleTime: 60 * 1000,
    gcTime: 5 * 60 * 1000,
    retry: 1,
  });
}

export function useDeleteReview() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, data }: { id: string; data: AdminReviewDeleteRequest }) => {
      const response = await adminApi.deleteReview(id, data);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ADMIN_REVIEW_KEYS.all });
    },
  });
}
