import { useEffect, useState } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { getStoredToken, restoreSession } from '@/features/auth/session';

/**
 * 需要登录的路由守卫。
 * <p>
 * access token 只存内存，整页刷新（刷新收藏页、从书签/新标签打开）时 store 是空的，
 * 必须等 restoreSession 用 HttpOnly refresh cookie 恢复完会话再判定，否则守卫会在
 * 恢复完成前把已登录用户直接踢到登录页（token 迟到也不会再救回来——守卫不订阅 store）。
 */
export function ProtectedRoute({ children }: { children: React.ReactNode }) {
    const location = useLocation();
    const [sessionChecked, setSessionChecked] = useState(() => !!getStoredToken());

    useEffect(() => {
        if (sessionChecked) {
            return;
        }
        let cancelled = false;
        restoreSession().finally(() => {
            if (!cancelled) {
                setSessionChecked(true);
            }
        });
        return () => {
            cancelled = true;
        };
    }, [sessionChecked]);

    if (!sessionChecked) {
        return (
            <div className="flex items-center justify-center min-h-screen">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-orange-500"></div>
            </div>
        );
    }

    return getStoredToken() ? (
        children
    ) : (
        <Navigate to={`/login?redirect=${encodeURIComponent(location.pathname)}`} replace />
    );
}
