import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { adminApi } from '../api/adminApi';
import type { AdminUser, AdminUserQuery, UpdateStatusRequest } from '../types/admin';

export const ADMIN_USER_KEYS = {
    all: ['admin', 'users'] as const,
    lists: () => [...ADMIN_USER_KEYS.all, 'list'] as const,
    list: (params: AdminUserQuery) =>
        [
            ...ADMIN_USER_KEYS.lists(),
            params.pageNum,
            params.pageSize,
            params.keyword,
            params.userType,
            params.status,
            params.startTime,
            params.endTime,
        ] as const,
    details: () => [...ADMIN_USER_KEYS.all, 'detail'] as const,
    detail: (id: number) => [...ADMIN_USER_KEYS.details(), id] as const,
};

export function useAdminUsers(params: AdminUserQuery) {
    return useQuery({
        queryKey: ADMIN_USER_KEYS.list(params),
        queryFn: async () => {
            const response = await adminApi.getUsers(params);
            return response.data;
        },
        staleTime: 30 * 1000,
        gcTime: 2 * 60 * 1000,
        retry: 1,
    });
}

export function useAdminUserDetail(id: number) {
    return useQuery<AdminUser>({
        queryKey: ADMIN_USER_KEYS.detail(id),
        queryFn: async () => {
            const response = await adminApi.getUserById(id);
            return response.data;
        },
        enabled: !!id,
        staleTime: 60 * 1000,
        gcTime: 5 * 60 * 1000,
        retry: 1,
    });
}

export function useUpdateUserStatus() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ id, data }: { id: number; data: UpdateStatusRequest }) => {
            const response = await adminApi.updateUserStatus(id, data);
            return response.data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ADMIN_USER_KEYS.lists() });
        },
    });
}
