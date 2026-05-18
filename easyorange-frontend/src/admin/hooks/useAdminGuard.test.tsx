import { describe, it, expect, beforeEach } from 'vitest';
import { renderHook } from '@testing-library/react';
import { useAuthStore } from '@/store';
import { useAdminGuard } from './useAdminGuard';
import type { User } from '@/types';

const mockAdminUser = { id: '1', userType: '00', username: 'admin' } as unknown as User;
const mockRegularUser = { id: '2', userType: '01', username: 'user' } as unknown as User;

beforeEach(() => {
  useAuthStore.setState({
    user: null,
    token: null,
    refreshToken: null,
    isAuthenticated: false,
  });
});

describe('useAdminGuard', () => {
  it('detects admin user', () => {
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

  it('detects non-admin user', () => {
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

  it('returns non-authenticated state when no token', () => {
    const { result } = renderHook(() => useAdminGuard());

    expect(result.current.isAdmin).toBe(false);
    expect(result.current.isAuthenticated).toBe(false);
    expect(result.current.isLoading).toBe(true);
    expect(result.current.user).toBeNull();
  });
});
