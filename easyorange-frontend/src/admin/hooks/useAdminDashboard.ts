import { useQuery } from '@tanstack/react-query';
import { adminApi } from '../api/adminApi';
import type { ActivityItem, DashboardStats, TrendItem } from '../types/admin';

export const ADMIN_DASHBOARD_KEYS = {
    all: ['admin', 'dashboard'] as const,
    stats: () => [...ADMIN_DASHBOARD_KEYS.all, 'stats'] as const,
    trend: () => [...ADMIN_DASHBOARD_KEYS.all, 'trend'] as const,
    activity: () => [...ADMIN_DASHBOARD_KEYS.all, 'activity'] as const,
};

export function useDashboardStats() {
    return useQuery<DashboardStats>({
        queryKey: ADMIN_DASHBOARD_KEYS.stats(),
        queryFn: async () => {
            const response = await adminApi.getDashboardStats();
            return response.data;
        },
        staleTime: 60 * 1000,
        gcTime: 5 * 60 * 1000,
        retry: 1,
    });
}

export function useTrend() {
    return useQuery<TrendItem[]>({
        queryKey: ADMIN_DASHBOARD_KEYS.trend(),
        queryFn: async () => {
            const response = await adminApi.getTrend();
            return response.data ?? [];
        },
        staleTime: 60 * 1000,
        gcTime: 5 * 60 * 1000,
        retry: 1,
    });
}

export function useRecentActivity() {
    return useQuery<ActivityItem[]>({
        queryKey: ADMIN_DASHBOARD_KEYS.activity(),
        queryFn: async () => {
            const response = await adminApi.getActivity();
            return response.data ?? [];
        },
        staleTime: 30 * 1000,
        gcTime: 2 * 60 * 1000,
        retry: 1,
    });
}
