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
        const ut = userType as Record<string, unknown>;
        if (ut.code === '00' || ut.code === '02') {
            return true;
        }
        if (ut.value === '00' || ut.value === '02') {
            return true;
        }
    }

    return false;
}

export function useAdminGuard(): AdminGuardState {
    const { user, token } = useAuthStore();
    const isAuthenticated = !!token;
    const isAdmin = isAuthenticated && checkIsAdmin(user);

    return {
        isLoading: false,
        isAuthenticated,
        isAdmin,
        user,
    };
}
