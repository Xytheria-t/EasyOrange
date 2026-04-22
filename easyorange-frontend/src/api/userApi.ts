/**
 * @fileoverview 用户 API 模块
 */

import { request } from './core/request.js';
import type { LoginResponse, UserBasicInfo } from '../types/user.js';

export const userApi = {
    login(data: { account: string; password: string }) {
        return request<LoginResponse>('/auth/login', {
            method: 'POST',
            body: { ...data, clientType: 'web' }
        });
    },

    register(data: { username: string; password: string }) {
        return request<void>('/users/register', {
            method: 'POST',
            body: data
        });
    },

    logout() {
        return request<void>('/auth/logout', { method: 'POST' });
    },

    getInfo() {
        return request<UserBasicInfo>('/users/info');
    },

    updateProfile(data: { username?: string; email?: string; phone?: string; realName?: string; studentId?: string }) {
        return request<UserBasicInfo>('/users/info', {
            method: 'PUT',
            body: data
        });
    }
};
