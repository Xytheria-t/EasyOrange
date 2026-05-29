import { describe, it, expect, beforeEach } from 'vitest';
import { renderHook } from '@testing-library/react';
import { useAuthStore } from '@/store';
import { useAdminGuard } from './useAdminGuard';
import type { User } from '@/types';

const mockAdminUser = { userId: '1', userType: '00', username: 'admin' } as unknown as User;
const mockAdminUser02 = { userId: '2', userType: '02', username: 'admin02' } as unknown as User;
const mockRegularUser = { userId: '3', userType: '01', username: 'user' } as unknown as User;
const mockAdminUserWithCodeObject = { userId: '4', userType: { code: '00', description: '管理员' }, username: 'admin3' } as unknown as User;
const mockAdminUserWithCodeObject02 = { userId: '5', userType: { code: '02', description: '管理员' }, username: 'admin4' } as unknown as User;
const mockAdminUserWithValueObject = { userId: '6', userType: { value: '00', description: '管理员' }, username: 'admin5' } as unknown as User;
const mockAdminUserWithValueObject02 = { userId: '7', userType: { value: '02', description: '管理员' }, username: 'admin6' } as unknown as User;

beforeEach(() => {
  useAuthStore.setState({
    user: null,
    token: null,
    refreshToken: null,
    isAuthenticated: false,
  });
});

describe('useAdminGuard', () => {
  it('detects admin user with string userType 00', () => {
    useAuthStore.setState({
      token: 'admin-token',
      isAuthenticated: true,
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
      isAuthenticated: true,
      user: mockAdminUser02,
    });

    const { result } = renderHook(() => useAdminGuard());

    expect(result.current.isAdmin).toBe(true);
    expect(result.current.isAuthenticated).toBe(true);
  });

  it('detects non-admin user with string userType 01', () => {
    useAuthStore.setState({
      token: 'user-token',
      isAuthenticated: true,
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
      isAuthenticated: true,
      user: mockAdminUserWithCodeObject,
    });

    const { result } = renderHook(() => useAdminGuard());

    expect(result.current.isAdmin).toBe(true);
    expect(result.current.isAuthenticated).toBe(true);
  });

  it('detects admin user with code object userType 02', () => {
    useAuthStore.setState({
      token: 'admin-token',
      isAuthenticated: true,
      user: mockAdminUserWithCodeObject02,
    });

    const { result } = renderHook(() => useAdminGuard());

    expect(result.current.isAdmin).toBe(true);
    expect(result.current.isAuthenticated).toBe(true);
  });

  it('detects admin user with value object userType 00', () => {
    useAuthStore.setState({
      token: 'admin-token',
      isAuthenticated: true,
      user: mockAdminUserWithValueObject,
    });

    const { result } = renderHook(() => useAdminGuard());

    expect(result.current.isAdmin).toBe(true);
    expect(result.current.isAuthenticated).toBe(true);
  });

  it('detects admin user with value object userType 02', () => {
    useAuthStore.setState({
      token: 'admin-token',
      isAuthenticated: true,
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
      isAuthenticated: true,
      user: null,
    });

    const { result } = renderHook(() => useAdminGuard());

    expect(result.current.isAdmin).toBe(false);
    expect(result.current.isAuthenticated).toBe(true);
  });
});
