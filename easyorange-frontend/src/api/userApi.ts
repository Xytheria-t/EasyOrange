import type { LoginRequest, LoginResponse, RegisterRequest, User } from '@/types';
import { request } from './core/request';

export const userApi = {
    login(data: LoginRequest) {
        return request<LoginResponse>('/auth/login', {
            method: 'POST',
            body: { identifier: data.account, password: data.password, clientType: 'web' },
            skipAuth: true,
        });
    },

    register(data: RegisterRequest) {
        return request<number>('/auth/register', {
            method: 'POST',
            body: data,
            skipAuth: true,
        });
    },

    refreshToken(refreshToken: string) {
        return request<string>('/auth/refresh', {
            method: 'POST',
            body: { refreshToken },
        });
    },

    sendSmsCode(phone: string) {
        return request<void>('/auth/sms-code', {
            method: 'POST',
            params: { phone },
        });
    },

    forgotPassword(data: { phone: string; verifyCode: string; newPassword: string }) {
        return request<void>('/auth/password/reset', {
            method: 'POST',
            body: data,
        });
    },

    getCurrentUser() {
        return request<User>('/users/me');
    },

    updateProfile(data: {
        nickname?: string;
        email?: string;
        phone?: string;
        gender?: number;
        realName?: string;
        studentId?: string;
    }) {
        return request<User>('/users/me', {
            method: 'PUT',
            body: data,
        });
    },

    changePassword(data: { verifyCode: string; newPassword: string }) {
        return request<void>('/auth/password/change', {
            method: 'PUT',
            body: data,
        });
    },

    uploadAvatar(file: File) {
        const formData = new FormData();
        formData.append('avatar', file);
        return request<User>('/users/avatar', {
            method: 'POST',
            body: formData,
            headers: {},
        });
    },
};
