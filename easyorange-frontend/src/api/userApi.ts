/**
 * @fileoverview 用户 API 模块
 */

import { request } from './core/request';
import type { 
    LoginRequest, 
    RegisterRequest, 
    PhoneRegisterRequest, 
    EmailRegisterRequest,
    PhoneLoginRequest, 
    EmailLoginRequest, 
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

    // 用户名密码登录
    loginWithPassword(data: { username: string; password: string }) {
        return request<LoginResponse>('/auth/login', {
            method: 'POST',
            body: { ...data, clientType: 'web' }
        });
    },

    // 手机号验证码登录（规划中）
    loginWithPhone(data: PhoneLoginRequest) {
        return request<LoginResponse>('/auth/login/phone', {
            method: 'POST',
            body: { ...data, clientType: 'web' }
        });
    },

    // 邮箱密码登录（规划中）
    loginWithEmail(data: EmailLoginRequest) {
        return request<LoginResponse>('/auth/login/email', {
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

    // 手机号注册（规划中）
    registerWithPhone(data: PhoneRegisterRequest) {
        return request<void>('/users/register/phone', {
            method: 'POST',
            body: data
        });
    },

    // 邮箱注册（规划中）
    registerWithEmail(data: EmailRegisterRequest) {
        return request<void>('/users/register/email', {
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
