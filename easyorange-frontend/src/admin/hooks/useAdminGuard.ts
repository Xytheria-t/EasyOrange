import { useAuthStore } from '@/store';
import type { User } from '@/types';

interface AdminGuardState {
  isLoading: boolean;
  isAuthenticated: boolean;
  isAdmin: boolean;
  user: User | null;
}

function checkIsAdmin(user: User | null): boolean {
  if (!user) {
    return false;
  }

  const userType = user.userType;

  if (typeof userType === 'string') {
    return userType === '00' || userType === '02';
  }

  if (typeof userType === 'object' && userType !== null) {
    if ('code' in userType && (userType.code === '00' || userType.code === '02')) {
      return true;
    }
    if ('value' in userType && (userType.value === '00' || userType.value === '02')) {
      return true;
    }
  }

  return false;
}

export function useAdminGuard(): AdminGuardState {
  const { user, token, isAuthenticated } = useAuthStore();

  const isAdmin = !!token && checkIsAdmin(user);

  return {
    isLoading: !token,
    isAuthenticated,
    isAdmin,
    user,
  };
}
