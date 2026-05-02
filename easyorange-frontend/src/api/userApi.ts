import { request } from './core/request';
import type {
    LoginRequest,
    RegisterRequest,
    LoginResponse,
    User
} from '../types/index.js';
import { useAuthStore } from '../store';

export const userApi = {
    login(data: LoginRequest) {
        return request<LoginResponse>('/auth/login', {
            method: 'POST',
            body: { ...data, clientType: 'web' }
        });
    },

    register(data: RegisterRequest) {
        return request<number>('/auth/register', {
            method: 'POST',
            body: data
        });
    },

    logout() {
        const refreshToken = useAuthStore.getState().refreshToken;
        const headers: Record<string, string> = {};
        if (refreshToken) {
            headers['X-Refresh-Token'] = `Bearer ${refreshToken}`;
        }
        return request<void>('/auth/logout', {
            method: 'POST',
            headers
        });
    },

    refreshToken(refreshToken: string) {
        return request<string>('/auth/refresh', {
            method: 'POST',
            body: { refreshToken }
        });
    },

    sendSmsCode(phone: string) {
        return request<void>('/auth/sms-code', {
            method: 'POST',
            params: { phone }
        });
    },

    forgotPassword(data: { phone: string; verifyCode: string; newPassword: string }) {
        return request<void>('/auth/password-reset', {
            method: 'POST',
            body: data
        });
    },

    getCurrentUser() {
        return request<User>('/users/me');
    },

    updateProfile(data: { nickname?: string; email?: string; phone?: string; gender?: number; realName?: string; studentId?: string }) {
        return request<User>('/users/me', {
            method: 'PUT',
            body: data
        });
    },

    changePassword(data: { oldPassword: string; newPassword: string }) {
        return request<void>('/users/me/password', {
            method: 'PUT',
            body: data
        });
    },

    uploadAvatar(file: File) {
        const formData = new FormData();
        formData.append('avatar', file);
        return request<User>('/users/avatar', {
            method: 'POST',
            body: formData,
            headers: {}
        });
    }
};
