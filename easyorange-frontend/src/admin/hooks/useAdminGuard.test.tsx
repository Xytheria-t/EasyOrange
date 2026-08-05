import { renderHook } from '@testing-library/react';
import { beforeEach, describe, expect, it } from 'vitest';
import { useAuthStore } from '@/store';
import type { User } from '@/types';
import { useAdminGuard } from './useAdminGuard';

const mockAdminUser = { userId: '1', userType: '00', username: 'admin' } as unknown as User;
const mockAdminUser02 = { userId: '2', userType: '02', username: 'admin02' } as unknown as User;
const mockRegularUser = { userId: '3', userType: '01', username: 'user' } as unknown as User;
const mockAdminUserWithCodeObject = {
    userId: '4',
    userType: { code: '00', description: '管理员' },
    username: 'admin3',
} as unknown as User;
const mockAdminUserWithCodeObject02 = {
    userId: '5',
    userType: { code: '02', description: '管理员' },
    username: 'admin4',
} as unknown as User;
const mockAdminUserWithValueObject = {
    userId: '6',
    userType: { value: '00', description: '管理员' },
    username: 'admin5',
} as unknown as User;
const mockAdminUserWithValueObject02 = {
    userId: '7',
    userType: { value: '02', description: '管理员' },
    username: 'admin6',
} as unknown as User;

beforeEach(() => {
    useAuthStore.setState({
        user: null,
        token: null,
    });
});

describe('useAdminGuard', () => {
    it('detects admin user with string userType 00', () => {
        useAuthStore.setState({
            token: 'admin-token',
            user: mockAdminUser,
        });

        const { result } = renderHook(() => useAdminGuard());

        expect(result.current.isAdmin).toBe(true);
        expect(result.current.isAuthenticated).toBe(true);
        expect(result.current.isLoading).toBe(false);
        expect(result.current.user).toEqual(mockAdminUser);
    });

    it('detects admin user with string userType 02', () => {
        useAuthStore.setState({
            token: 'admin-token',
            user: mockAdminUser02,
        });

        const { result } = renderHook(() => useAdminGuard());

        expect(result.current.isAdmin).toBe(true);
        expect(result.current.isAuthenticated).toBe(true);
    });

    it('detects non-admin user with string userType 01', () => {
        useAuthStore.setState({
            token: 'user-token',
            user: mockRegularUser,
        });

        const { result } = renderHook(() => useAdminGuard());

        expect(result.current.isAdmin).toBe(false);
        expect(result.current.isAuthenticated).toBe(true);
        expect(result.current.isLoading).toBe(false);
        expect(result.current.user).toEqual(mockRegularUser);
    });

    it('detects admin user with code object userType 00', () => {
        useAuthStore.setState({
            token: 'admin-token',
            user: mockAdminUserWithCodeObject,
        });

        const { result } = renderHook(() => useAdminGuard());

        expect(result.current.isAdmin).toBe(true);
        expect(result.current.isAuthenticated).toBe(true);
    });

    it('detects admin user with code object userType 02', () => {
        useAuthStore.setState({
            token: 'admin-token',
            user: mockAdminUserWithCodeObject02,
        });

        const { result } = renderHook(() => useAdminGuard());

        expect(result.current.isAdmin).toBe(true);
        expect(result.current.isAuthenticated).toBe(true);
    });

    it('detects admin user with value object userType 00', () => {
        useAuthStore.setState({
            token: 'admin-token',
            user: mockAdminUserWithValueObject,
        });

        const { result } = renderHook(() => useAdminGuard());

        expect(result.current.isAdmin).toBe(true);
        expect(result.current.isAuthenticated).toBe(true);
    });

    it('detects admin user with value object userType 02', () => {
        useAuthStore.setState({
            token: 'admin-token',
            user: mockAdminUserWithValueObject02,
        });

        const { result } = renderHook(() => useAdminGuard());

        expect(result.current.isAdmin).toBe(true);
        expect(result.current.isAuthenticated).toBe(true);
    });

    it('returns non-authenticated state when no token', () => {
        const { result } = renderHook(() => useAdminGuard());

        expect(result.current.isAdmin).toBe(false);
        expect(result.current.isAuthenticated).toBe(false);
        expect(result.current.isLoading).toBe(false);
        expect(result.current.user).toBeNull();
    });

    it('returns non-admin when user is null even with token', () => {
        useAuthStore.setState({
            token: 'some-token',
            user: null,
        });

        const { result } = renderHook(() => useAdminGuard());

        expect(result.current.isAdmin).toBe(false);
        expect(result.current.isAuthenticated).toBe(true);
    });
});
