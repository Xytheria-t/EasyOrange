import { request } from './core/request';

export interface PlatformStats {
    activeUsers: number;
    onlineProducts: number;
    completedOrders: number;
}

export const statsApi = {
    getPlatformStats() {
        return request<PlatformStats>('/stats/platform', {
            method: 'GET',
            skipAuth: true,
            cache: true,
        });
    },
};
