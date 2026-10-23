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

/**
 * 登录表单输入 — 不是线上请求体：{@link userApi.login} 会映射成
 * `{ identifier, password, clientType: 'web' }`，短信登录走独立端点的 `verifyCode`。
 */
export interface LoginFormInput {
    account: string;
    /** 密码登录时是密码；短信登录时是验证码（字段名沿用表单历史，勿直传 /auth/login） */
    password: string;
    loginMethod: 'password' | 'sms';
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
