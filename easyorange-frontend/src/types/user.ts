export type UserStatus = 'NORMAL' | 'DISABLED' | 'BANNED';

export type Gender = 'MALE' | 'FEMALE' | 'UNKNOWN';

export interface User {
    id: number;
    username: string;
    email: string;
    phone: string | null;
    studentId: string;
    realName: string;
    gender: Gender;
    status: UserStatus;
    createTime: string;
    lastLoginTime: string | null;
}

export interface LoginRequest {
    account: string;
    password: string;
}

export interface RegisterRequest {
    username: string;
    password: string;
    email: string;
    phone?: string;
    studentId: string;
    realName: string;
}

export interface CampusLoginRequest {
    campusId: string;
    password: string;
}

export interface LoginResponse {
    token: string;
    user: UserInfo;
}

export interface UserInfo {
    id: number;
    username: string;
    email: string;
    phone: string | null;
    studentId: string;
    realName: string;
    gender: Gender;
    status: UserStatus;
    createTime: string;
    lastLoginTime: string | null;
}

export interface UpdateUserRequest {
    username?: string;
    email?: string;
    phone?: string;
    realName?: string;
    studentId?: string;
    gender?: Gender;
}

export interface UserStats {
    productCount: number;
    soldCount: number;
    boughtCount: number;
    favoriteCount: number;
    followerCount: number;
    followingCount: number;
    rating: number;
    reviewCount: number;
}
