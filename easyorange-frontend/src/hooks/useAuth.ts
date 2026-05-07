import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { userApi } from '@/api/userApi';
import { useAuthStore } from '@/store';
import { setSession, clearSession, type AuthSessionUser } from '@/features/auth/session';
import type { LoginRequest, RegisterRequest } from '@/types';

const AUTH_KEYS = {
  all: ['auth'] as const,
  user: () => [...AUTH_KEYS.all, 'user'] as const,
};

const TOKEN_EXPIRES_IN_MINUTES = 30;

export function useCurrentUser() {
  const { token, setUser } = useAuthStore();

  return useQuery({
    queryKey: AUTH_KEYS.user(),
    queryFn: async () => {
      const response = await userApi.getCurrentUser();
      const user = response.data;
      setUser(user);
      return user;
    },
    enabled: !!token,
    retry: false,
    staleTime: 5 * 60 * 1000,
    throwOnError: false,
  });
}

export function useLogin() {
  const queryClient = useQueryClient();
  const { login } = useAuthStore();

  return useMutation({
    mutationFn: async (data: LoginRequest) => {
      const response = await userApi.login(data);
      return response.data;
    },
    onSuccess: (data) => {
      if (data.token && data.user) {
        login(data.user, data.token, data.refreshToken);
        const expiresAt = Date.now() + TOKEN_EXPIRES_IN_MINUTES * 60 * 1000;
        setSession(data.token, data.user as unknown as AuthSessionUser, expiresAt, data.refreshToken);
        queryClient.setQueryData(AUTH_KEYS.user(), data.user);
      }
    },
  });
}

export function useRegister() {
  return useMutation({
    mutationFn: async (data: RegisterRequest) => {
      const response = await userApi.register(data);
      return response.data;
    },
  });
}

export function useLogout() {
  const queryClient = useQueryClient();
  const { logout: clearAuth } = useAuthStore();

  return useMutation({
    mutationFn: async () => {
      await userApi.logout();
    },
    onSuccess: () => {
      clearAuth();
      clearSession('logout');
      queryClient.clear();
    },
  });
}
