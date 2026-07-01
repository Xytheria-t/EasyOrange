import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAdminGuard } from '../hooks/useAdminGuard';
import ForbiddenPage from '../pages/ForbiddenPage';

export function AdminRouteGuard() {
    const { isAuthenticated, isAdmin } = useAdminGuard();
    const location = useLocation();

    if (!isAuthenticated) {
        return <Navigate to={`/login?redirect=${encodeURIComponent(location.pathname)}`} replace />;
    }

    if (!isAdmin) {
        return <ForbiddenPage />;
    }

    return <Outlet />;
}
