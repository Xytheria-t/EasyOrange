import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { PageResult } from '@/types';
import { adminApi } from '../api/adminApi';
import type { AdminUser, AdminUserQuery, UpdateUserStatusRequest } from '../types/admin';

/** 同 useAdminProducts 的 selectList：只留列表页消费的 records / total / current */
const selectList = (data: PageResult<AdminUser>) => ({
    records: data.records,
    total: data.total,
    current: data.current,
});

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
    detail: (id: string) => [...ADMIN_USER_KEYS.details(), id] as const,
};

export function useAdminUsers(params: AdminUserQuery) {
    return useQuery({
        queryKey: ADMIN_USER_KEYS.list(params),
        queryFn: async () => {
            const response = await adminApi.getUsers(params);
            return response.data;
        },
        select: selectList,
        // v5 里 keepPreviousData 改名 placeholderData：翻页 / 换筛选时沿用上一页数据，
        // 否则表格会退回骨架屏再整页重排
        placeholderData: keepPreviousData,
        staleTime: 30 * 1000,
        gcTime: 2 * 60 * 1000,
        retry: 1,
    });
}

export function useAdminUserDetail(id: string) {
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
        mutationFn: async ({ id, data }: { id: string; data: UpdateUserStatusRequest }) => {
            const response = await adminApi.updateUserStatus(id, data);
            return response.data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ADMIN_USER_KEYS.lists() });
        },
    });
}
