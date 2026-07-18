import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { adminApi } from '../api/adminApi';
import type { AdminRating, AdminRatingDeleteRequest, AdminRatingQuery } from '../types/admin';

export const ADMIN_RATING_KEYS = {
    all: ['admin', 'reviews'] as const,
    lists: () => [...ADMIN_RATING_KEYS.all, 'list'] as const,
    list: (params: AdminRatingQuery) =>
        [
            ...ADMIN_RATING_KEYS.lists(),
            params.pageNum,
            params.pageSize,
            params.productId,
            params.userId,
            params.rating,
            params.status,
            params.keyword,
            params.startTime,
            params.endTime,
        ] as const,
    details: () => [...ADMIN_RATING_KEYS.all, 'detail'] as const,
    detail: (id: string) => [...ADMIN_RATING_KEYS.details(), id] as const,
};

export function useAdminRatings(params: AdminRatingQuery) {
    return useQuery({
        queryKey: ADMIN_RATING_KEYS.list(params),
        queryFn: async () => {
            const response = await adminApi.getReviews(params);
            return response.data;
        },
        staleTime: 30 * 1000,
        gcTime: 2 * 60 * 1000,
        retry: 1,
    });
}

export function useAdminRatingDetail(id: string) {
    return useQuery<AdminRating>({
        queryKey: ADMIN_RATING_KEYS.detail(id),
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

export function useDeleteRating() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ id, data }: { id: string; data: AdminRatingDeleteRequest }) => {
            const response = await adminApi.deleteReview(id, data);
            return response.data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ADMIN_RATING_KEYS.all });
        },
    });
}
