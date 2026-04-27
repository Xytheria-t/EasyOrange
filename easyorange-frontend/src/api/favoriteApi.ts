/**
 * @fileoverview 收藏 API 模块
 */

import type { PageResult } from '../types/index.js';
import { request } from './core/request.js';

export const favoriteApi = {
    getList(_params?: Record<string, unknown>) {
        return request<PageResult<Record<string, unknown>>>('/favorites', {
            method: 'GET'
        });
    },

    add(productId: number) {
        return request(`/favorites/${productId}`, {
            method: 'POST'
        });
    },

    remove(productId: number) {
        return request(`/favorites/${productId}`, {
            method: 'DELETE'
        });
    },

    removeMany(ids: number[]) {
        return request('/favorites/batch', {
            method: 'DELETE',
            body: { ids }
        });
    }
};
