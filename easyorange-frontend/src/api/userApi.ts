import { request } from './core/request';
import type {
    LoginRequest,
    RegisterRequest,
    LoginResponse,
    User
} from '../types/index.js';

export const userApi = {
    login(data: LoginRequest) {
        return request<LoginResponse>('/auth/login', {
            method: 'POST',
            body: { ...data, clientType: 'web' }
        });
    },

    register(data: RegisterRequest) {
        return request<void>('/auth/register', {
            method: 'POST',
            body: data
        });
    },

    logout() {
        return request<void>('/auth/logout', { method: 'POST' });
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

    updateProfile(data: { email?: string; phone?: string; gender?: number }) {
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
