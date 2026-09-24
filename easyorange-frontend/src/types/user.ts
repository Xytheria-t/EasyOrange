export type UserType = '00' | '01' | '02';

export interface User {
    userId: string;
    username: string;
    nickname?: string;
    email: string;
    phone: string | null;
    realName: string | null;
    avatar: string | null;
    /** 账号状态语义码（后端 UserProfileResponse/UserResponse 均为 String：NORMAL / DISABLED / LOCKED） */
    status: string;
    userType: UserType;
    createTime: string;
    updateTime: string;
}

export interface LoginRequest {
    account: string;
    password: string;
    loginMethod: 'password' | 'sms';
    clientType?: 'WEB';
    isRegister?: boolean;
}

export interface RegisterRequest {
    username: string;
    password: string;
    phone: string;
    verifyCode: string;
}

export interface LoginResponse {
    accessToken: string;
    user: User;
}

export interface TokenRefreshResult {
    accessToken: string;
}
