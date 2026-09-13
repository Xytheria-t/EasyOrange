/**
 * @fileoverview 评论 API 模块
 */

import type { PageResult } from '@/types';
import { request } from './core/request';

export const reviewApi = {
    getReviews(productId: string) {
        return request<PageResult<Record<string, unknown>>>(`/products/${productId}/reviews`, {
            method: 'GET',
            skipAuth: true,
        });
    },

    createReview(productId: string, data: Record<string, unknown>) {
        return request(`/products/${productId}/reviews`, {
            method: 'POST',
            body: data,
        });
    },

    /** 当前用户能否评价该资产（有已完成订单且未评价过）—— 决定是否展示评价入口 */
    canReview(productId: string) {
        return request<boolean>(`/products/${productId}/reviews/eligibility`, {
            method: 'GET',
        });
    },
};
