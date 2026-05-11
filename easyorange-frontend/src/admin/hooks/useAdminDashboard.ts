import { useQuery } from '@tanstack/react-query';
import { adminApi } from '../api/adminApi';
import type { DashboardStats, PendingItems, RecentUser, RecentProduct } from '../types/admin';

export const ADMIN_DASHBOARD_KEYS = {
  all: ['admin', 'dashboard'] as const,
  stats: () => [...ADMIN_DASHBOARD_KEYS.all, 'stats'] as const,
  pending: () => [...ADMIN_DASHBOARD_KEYS.all, 'pending'] as const,
  recentUsers: (limit?: number) => [...ADMIN_DASHBOARD_KEYS.all, 'recent-users', limit] as const,
  recentProducts: (limit?: number) => [...ADMIN_DASHBOARD_KEYS.all, 'recent-products', limit] as const,
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

export function usePendingItems() {
  return useQuery<PendingItems>({
    queryKey: ADMIN_DASHBOARD_KEYS.pending(),
    queryFn: async () => {
      const response = await adminApi.getPendingItems();
      return response.data;
    },
    staleTime: 30 * 1000,
    gcTime: 2 * 60 * 1000,
    retry: 1,
  });
}

export function useRecentUsers(limit = 5) {
  return useQuery<RecentUser[]>({
    queryKey: ADMIN_DASHBOARD_KEYS.recentUsers(limit),
    queryFn: async () => {
      const response = await adminApi.getRecentUsers(limit);
      return response.data ?? [];
    },
    staleTime: 60 * 1000,
    gcTime: 5 * 60 * 1000,
    retry: 1,
  });
}

export function useRecentProducts(limit = 5) {
  return useQuery<RecentProduct[]>({
    queryKey: ADMIN_DASHBOARD_KEYS.recentProducts(limit),
    queryFn: async () => {
      const response = await adminApi.getRecentProducts(limit);
      return response.data ?? [];
    },
    staleTime: 60 * 1000,
    gcTime: 5 * 60 * 1000,
    retry: 1,
  });
}
