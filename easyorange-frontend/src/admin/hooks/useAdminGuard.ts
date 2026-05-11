import { useAuthStore } from '@/store';
import type { User } from '@/types';

interface AdminGuardState {
  isLoading: boolean;
  isAuthenticated: boolean;
  isAdmin: boolean;
  user: User | null;
}

export function useAdminGuard(): AdminGuardState {
  const { user, token, isAuthenticated } = useAuthStore();

  const isAdmin = !!token && !!user && user.userType === '00';

  return {
    isLoading: !token,
    isAuthenticated,
    isAdmin,
    user,
  };
}
