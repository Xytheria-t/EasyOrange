import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { userApi } from '@/api/userApi';
import { logout as sessionLogout, setSession } from '@/features/auth/session';
import { useAuthStore } from '@/store';
import type { LoginRequest, RegisterRequest, User } from '@/types';

const AUTH_KEYS = {
    all: ['auth'] as const,
    user: () => [...AUTH_KEYS.all, 'user'] as const,
};

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

    return useMutation({
        mutationFn: async (data: LoginRequest) => {
            const response = await userApi.login(data);
            return response.data;
        },
        onSuccess: data => {
            if (data?.accessToken && data?.user) {
                setSession(data.accessToken, data.user as User);
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

    return async () => {
        await sessionLogout();
        queryClient.clear();
    };
}
