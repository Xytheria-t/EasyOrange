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
            }
        });
    },

    addFavorite(productId: string) {
        return request(`/favorites/${productId}`, {
            method: 'POST'
        });
    },

    removeFavorite(productId: string) {
        return request(`/favorites/${productId}`, {
            method: 'DELETE'
        });
    },

    removeMany(ids: string[]) {
        return request('/favorites/batch', {
            method: 'DELETE',
            body: { ids }
        });
    },

    checkFavorite(productId: string) {
        return request<boolean>(`/favorites/check/${productId}`, {
            method: 'GET'
        });
    },

    batchCheck(productIds: string[]) {
        return request<Record<string, boolean>>('/favorites/batch-check', {
            method: 'POST',
            body: { productIds }
        });
    },

    getCount() {
        return request<number>('/favorites/count', {
            method: 'GET'
        });
    }
};
