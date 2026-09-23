import { useEffect, useState } from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { getStoredToken, restoreSession } from '@/features/auth/session';
import { useAdminGuard } from '../hooks/useAdminGuard';
import ForbiddenPage from '../pages/ForbiddenPage';

/**
 * 后台守卫 —— 与 ProtectedRoute 同款的会话恢复等待。
 * <p>
 * access token 只存内存：整页刷新（演示中 F5 后台页、地址栏深链直达）时 store 是空的，
 * 必须等 {@link restoreSession} 用 HttpOnly refresh cookie 恢复完会话（其内部会连带拉取
 * /users/me 装载 userType）再判定 —— 否则已登录管理员会在恢复完成前被误踢到登录页，
 * token 迟到也救不回来（守卫不订阅 store，与 ProtectedRoute 注释同款坑）。
 */
export function AdminRouteGuard() {
    const { isAuthenticated, isAdmin } = useAdminGuard();
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

    if (!isAuthenticated) {
        return <Navigate to={`/login?redirect=${encodeURIComponent(location.pathname)}`} replace />;
    }

    if (!isAdmin) {
        return <ForbiddenPage />;
    }

    return <Outlet />;
}
