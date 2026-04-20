/**
 * @fileoverview 用户 API 模块
 */

import { request } from './core/request.js';

export const userApi = {
    login(data: { account: string; password: string }) {
        return request<{ token: string; user: Record<string, unknown> }>('/auth/login', {
            method: 'POST',
            body: { ...data, clientType: 'web' }
        });
    },

    register(data: Record<string, unknown>) {
        return request('/users/register', {
            method: 'POST',
            body: data
        });
    },

    logout() {
        return request('/auth/logout', { method: 'POST' });
    },

    getInfo() {
        return request<Record<string, unknown>>('/users/info');
    },

    updateProfile(data: Record<string, unknown>) {
        return request('/users/info', {
            method: 'PUT',
            body: data
        });
    }
};
