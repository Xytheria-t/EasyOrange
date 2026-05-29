import { Suspense, lazy } from 'react';
import { Routes, Route } from 'react-router-dom';
import { AdminRouteGuard } from './components/AdminRouteGuard';
import { AdminLayout } from './layout';
import '@/admin/styles/admin.css';

const LoadingFallback = () => (
  <div className="flex items-center justify-center min-h-screen bg-gradient-to-br from-slate-50 to-slate-100">
    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-orange-500"></div>
  </div>
);

const DashboardPage = lazy(() => import('./pages/dashboard/DashboardPage'));
const UserManagePage = lazy(() => import('./pages/users/UserManagePage'));
const ProductReviewPage = lazy(() => import('./pages/products/ProductReviewPage'));
const OrderManagePage = lazy(() => import('./pages/orders/OrderManagePage'));
const ReportManagePage = lazy(() => import('./pages/reports/ReportManagePage'));
const StatsPage = lazy(() => import('./pages/stats/StatsPage'));
const CategoryManagePage = lazy(() => import('./pages/categories/CategoryManagePage'));
const ReviewManagePage = lazy(() => import('./pages/reviews/ReviewManagePage'));

export function AdminRoutes() {
  return (
    <Suspense fallback={<LoadingFallback />}>
      <Routes>
        <Route element={<AdminRouteGuard />}>
          <Route element={<AdminLayout />}>
            <Route index element={<DashboardPage />} />
            <Route path="users" element={<UserManagePage />} />
            <Route path="products" element={<ProductReviewPage />} />
            <Route path="orders" element={<OrderManagePage />} />
            <Route path="reports" element={<ReportManagePage />} />
            <Route path="categories" element={<CategoryManagePage />} />
            <Route path="reviews" element={<ReviewManagePage />} />
            <Route path="stats" element={<StatsPage />} />
          </Route>
        </Route>
      </Routes>
    </Suspense>
  );
}
