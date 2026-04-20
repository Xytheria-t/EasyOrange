/**
 * @fileoverview 用户资料 API 模块
 */

import { request } from './core/request.js';

export const profileApi = {
    getInfo() {
        return request<Record<string, unknown>>('/profile');
    },

    getProfile() {
        return request<Record<string, unknown>>('/profile');
    },

    getPreferences() {
        return request<Record<string, unknown>>('/profile/preferences');
    },

    getSecurity() {
        return request<Record<string, unknown>>('/profile/security');
    },

    getActivities(page?: number, limit?: number) {
        const params = new URLSearchParams();
        if (page) {params.set('page', String(page));}
        if (limit) {params.set('limit', String(limit));}
        const query = params.toString() ? `?${params.toString()}` : '';
        return request<Record<string, unknown>[]>(`/profile/activities${query}`);
    },

    exportData() {
        return request<Record<string, unknown>>('/profile/export');
    },

    changePassword(data: { currentPassword?: string; oldPassword?: string; newPassword: string; confirmPassword?: string }) {
        return request('/profile/change-password', {
            method: 'POST',
            body: data
        });
    },

    updateUserInfo(data: Record<string, unknown> | { username?: string; email?: string; phone?: string; realName?: string; studentId?: string; gender?: string }) {
        return request('/profile/user-info', {
            method: 'PUT',
            body: data as Record<string, unknown>
        });
    },

    updatePreferences(data: Record<string, unknown> | { [key: string]: unknown }) {
        return request('/profile/preferences', {
            method: 'PUT',
            body: data as Record<string, unknown>
        });
    },

    updateSecurity(data: Record<string, unknown> | { [key: string]: unknown }) {
        return request('/profile/security', {
            method: 'PUT',
            body: data as Record<string, unknown>
        });
    },

    updateStats(data: Record<string, unknown>) {
        return request('/profile/stats', {
            method: 'PUT',
            body: data
        });
    }
};
