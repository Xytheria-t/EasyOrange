import { useQuery } from '@tanstack/react-query';
import { type PlatformStats, statsApi } from '@/api/statsApi';

export function usePlatformStats() {
    return useQuery<PlatformStats>({
        queryKey: ['platform', 'stats'],
        queryFn: async () => {
            const response = await statsApi.getPlatformStats();
            return response.data ?? { activeUsers: 0, onlineProducts: 0, completedOrders: 0 };
        },
        staleTime: 5 * 60 * 1000,
        gcTime: 10 * 60 * 1000,
        retry: 1,
    });
}
