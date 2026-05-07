/**
 * @fileoverview 评论 API 模块
 */

import type { PageResult } from '@/types';
import { request } from './core/request';

export const reviewApi = {
    getList(productId: number, _params?: Record<string, unknown>) {
        return request<PageResult<Record<string, unknown>>>(`/products/${productId}/reviews`, {
            method: 'GET'
        });
    },

    create(productId: number, data: Record<string, unknown>) {
        return request(`/products/${productId}/reviews`, {
            method: 'POST',
            body: data
        });
    }
};
