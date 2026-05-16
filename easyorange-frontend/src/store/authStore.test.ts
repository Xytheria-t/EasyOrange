import { describe, it, expect, beforeEach } from 'vitest';
import { useAuthStore } from './authStore';

beforeEach(() => {
  useAuthStore.setState({
    user: null,
    token: null,
    refreshToken: null,
    isAuthenticated: false,
  });
});

describe('authStore', () => {
  describe('initial state', () => {
    it('starts with no user and not authenticated', () => {
      const state = useAuthStore.getState();
      expect(state.user).toBeNull();
      expect(state.token).toBeNull();
      expect(state.refreshToken).toBeNull();
      expect(state.isAuthenticated).toBe(false);
    });
  });

  describe('setUser', () => {
    it('updates user and sets isAuthenticated when user is provided', () => {
      const mockUser = { id: '1', username: 'test', nickname: 'Test' } as any;
      useAuthStore.getState().setUser(mockUser);
      const state = useAuthStore.getState();
      expect(state.user).toEqual(mockUser);
      expect(state.isAuthenticated).toBe(true);
    });

    it('clears user and sets isAuthenticated to false when null', () => {
      useAuthStore.getState().setUser(null);
      const state = useAuthStore.getState();
      expect(state.user).toBeNull();
      expect(state.isAuthenticated).toBe(false);
    });
  });

  describe('setToken', () => {
    it('updates token', () => {
      useAuthStore.getState().setToken('my-token');
      expect(useAuthStore.getState().token).toBe('my-token');
    });

    it('clears token when set to null', () => {
      useAuthStore.setState({ token: 'existing-token' });
      useAuthStore.getState().setToken(null);
      expect(useAuthStore.getState().token).toBeNull();
    });
  });

  describe('login', () => {
    it('sets user, token, refreshToken and isAuthenticated', () => {
      const mockUser = { id: '1', username: 'test' } as any;
      useAuthStore.getState().login(mockUser, 'access-token', 'refresh-token');
      const state = useAuthStore.getState();
      expect(state.user).toEqual(mockUser);
      expect(state.token).toBe('access-token');
      expect(state.refreshToken).toBe('refresh-token');
      expect(state.isAuthenticated).toBe(true);
    });
  });

  describe('logout', () => {
    it('clears all session data', () => {
      useAuthStore.getState().login({ id: '1' } as any, 'token', 'refresh');
      useAuthStore.getState().logout();
      const state = useAuthStore.getState();
      expect(state.user).toBeNull();
      expect(state.token).toBeNull();
      expect(state.refreshToken).toBeNull();
      expect(state.isAuthenticated).toBe(false);
    });
  });
});
