import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAdminGuard } from '../hooks/useAdminGuard';
import ForbiddenPage from '../pages/ForbiddenPage';

function LoadingSpinner() {
  return (
    <div className="flex items-center justify-center min-h-screen bg-gradient-to-br from-slate-50 to-slate-100">
      <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-orange-500"></div>
    </div>
  );
}

export function AdminRouteGuard() {
  const { isLoading, isAuthenticated, isAdmin } = useAdminGuard();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to={`/login?redirect=${encodeURIComponent(location.pathname)}`} replace />;
  }

  if (isLoading) {
    return <LoadingSpinner />;
  }

  if (!isAdmin) {
    return <ForbiddenPage />;
  }

  return <Outlet />;
}
