import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { adminApi } from '../api/adminApi';
import type { AdminReport, AdminReportQuery, ReportHandleRequest, ReportStatsResponse } from '../types/admin';

export const ADMIN_REPORT_KEYS = {
    all: ['admin', 'reports'] as const,
    lists: () => [...ADMIN_REPORT_KEYS.all, 'list'] as const,
    list: (params: AdminReportQuery) =>
        [
            ...ADMIN_REPORT_KEYS.lists(),
            params.pageNum,
            params.pageSize,
            params.status,
            params.type,
            params.keyword,
            params.startTime,
            params.endTime,
        ] as const,
    details: () => [...ADMIN_REPORT_KEYS.all, 'detail'] as const,
    detail: (id: string) => [...ADMIN_REPORT_KEYS.details(), id] as const,
    stats: () => [...ADMIN_REPORT_KEYS.all, 'stats'] as const,
};

export function useAdminReports(params: AdminReportQuery) {
    return useQuery({
        queryKey: ADMIN_REPORT_KEYS.list(params),
        queryFn: async () => {
            const response = await adminApi.getReports(params);
            return response.data;
        },
        staleTime: 30 * 1000,
        gcTime: 2 * 60 * 1000,
        retry: 1,
    });
}

export function useAdminReportDetail(id: string) {
    return useQuery<AdminReport>({
        queryKey: ADMIN_REPORT_KEYS.detail(id),
        queryFn: async () => {
            const response = await adminApi.getReportById(id);
            return response.data;
        },
        enabled: !!id,
        staleTime: 60 * 1000,
        gcTime: 5 * 60 * 1000,
        retry: 1,
    });
}

export function useAdminReportStats() {
    return useQuery<ReportStatsResponse>({
        queryKey: ADMIN_REPORT_KEYS.stats(),
        queryFn: async () => {
            const response = await adminApi.getReportStats();
            return response.data;
        },
        staleTime: 30 * 1000,
        gcTime: 2 * 60 * 1000,
        retry: 1,
    });
}

export function useHandleReport() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ id, data }: { id: string; data: ReportHandleRequest }) => {
            const response = await adminApi.handleReport(id, data);
            return response.data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ADMIN_REPORT_KEYS.all });
        },
    });
}
