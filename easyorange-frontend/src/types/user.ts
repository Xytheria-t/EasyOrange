export interface User {
    userId: number;
    username: string;
    nickname?: string;
    email: string;
    phone: string | null;
    studentId: string | null;
    realName: string | null;
    avatar: string | null;
    status: number;
    createTime: string;
    updateTime: string;
}

export interface LoginRequest {
    account: string;
    password: string;
    loginMethod?: 'password' | 'sms';
    clientType?: 'WEB';
    isRegister?: boolean;
}

export interface RegisterRequest {
    username: string;
    password: string;
    phone?: string;
    email?: string;
}

export interface LoginResponse {
    token: string;
    refreshToken: string;
    user: User;
}
