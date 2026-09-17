/**
 * @fileoverview 收藏 API 模块
 */

import type { Favorite, PageResult } from '@/types';
import { request } from './core/request';

export const favoriteApi = {
    getFavorites(params?: { pageNum?: number; pageSize?: number }) {
        return request<PageResult<Favorite>>('/favorites', {
            method: 'GET',
            params: {
                pageNum: params?.pageNum ?? 1,
                pageSize: params?.pageSize ?? 20,
            },
        });
    },

    addFavorite(productId: string) {
        return request(`/favorites/${productId}`, {
            method: 'POST',
        });
    },

    removeFavorite(productId: string) {
        return request(`/favorites/${productId}`, {
            method: 'DELETE',
        });
    },

    removeMany(ids: string[]) {
        return request('/favorites/batch', {
            method: 'DELETE',
            body: { ids },
        });
    },

    checkFavorite(productId: string) {
        return request<boolean>(`/favorites/check/${productId}`, {
            method: 'GET',
        });
    },

    batchCheck(productIds: string[]) {
        return request<Record<string, boolean>>('/favorites/batch-check', {
            method: 'POST',
            body: { productIds },
        });
    },

    /**
     * 收藏数量。
     * 后端全局 Long → String（前端 JS 精度安全策略），计数在线上是 `"3"` 而非 `3`：
     * 在这里收敛成 number，调用方拿到的就是真正能参与运算的数字。
     */
    async getCount(): Promise<number> {
        const response = await request<string | number>('/favorites/count', {
            method: 'GET',
        });
        return Number(response.data ?? 0);
    },
};
