/**
 * @fileoverview 用户 API 模块
 */

import { request } from './core/request';
import type { LoginRequest, RegisterRequest, LoginResponse, User } from '../types/index.js';

export const userApi = {
    login(data: LoginRequest) {
        return request<LoginResponse>('/auth/login', {
            method: 'POST',
            body: { ...data, clientType: 'web' }
        });
    },

    register(data: RegisterRequest) {
        return request<void>('/users/register', {
            method: 'POST',
            body: data
        });
    },

    logout() {
        return request<void>('/auth/logout', { method: 'POST' });
    },

    getCurrentUser() {
        return request<User>('/users/info');
    },

    updateProfile(data: Partial<Pick<User, 'username' | 'email' | 'phone' | 'realName' | 'studentId'>>) {
        return request<User>('/users/info', {
            method: 'PUT',
            body: data
        });
    }
};
