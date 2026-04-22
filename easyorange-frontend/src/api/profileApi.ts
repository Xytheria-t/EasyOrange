/**
 * @fileoverview 用户资料 API 模块
 */

import { request } from './core/request.js';

export const profileApi = {
    getProfile() {
        return request<Record<string, unknown>>('/users/info');
    },

    getPreferences() {
        return request<Record<string, unknown>>('/users/info');
    },

    getSecurity() {
        return request<Record<string, unknown>>('/users/info');
    },

    getActivities(page?: number, limit?: number) {
        const params = new URLSearchParams();
        if (page) {params.set('page', String(page));}
        if (limit) {params.set('limit', String(limit));}
        const query = params.toString() ? `?${params.toString()}` : '';
        return request<Record<string, unknown>[]>(`/users/info${query}`);
    },

    exportData() {
        return request<Record<string, unknown>>('/users/info');
    },

    changePassword(data: { currentPassword?: string; oldPassword?: string; newPassword: string; confirmPassword?: string }) {
        return request('/users/password', {
            method: 'PUT',
            body: data
        });
    },

    updateUserInfo(data: Record<string, unknown> | { username?: string; email?: string; phone?: string; realName?: string; studentId?: string }) {
        return request('/users/info', {
            method: 'PUT',
            body: data as Record<string, unknown>
        });
    },

    updatePreferences(data: Record<string, unknown>) {
        return request('/users/info', {
            method: 'PUT',
            body: data
        });
    },

    updateSecurity(data: Record<string, unknown>) {
        return request('/users/info', {
            method: 'PUT',
            body: data
        });
    }
};
