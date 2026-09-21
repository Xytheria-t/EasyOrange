import { lazy, Suspense } from 'react';
import { Route, Routes } from 'react-router-dom';
import { AdminRouteGuard } from './components/AdminRouteGuard';
import { AdminLayout } from './layout';
import '@/admin/styles/admin.css';

const LoadingFallback = () => (
    <div className="flex items-center justify-center min-h-screen bg-gradient-to-br from-slate-50 to-slate-100">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-orange-500"></div>
    </div>
);

const UserManagePage = lazy(() => import('./pages/users/UserManagePage'));
const ProductReviewPage = lazy(() => import('./pages/products/ProductReviewPage'));
const OrderManagePage = lazy(() => import('./pages/orders/OrderManagePage'));
const StatsPage = lazy(() => import('./pages/stats/StatsPage'));
const CategoryManagePage = lazy(() => import('./pages/categories/CategoryManagePage'));
const KnowledgePage = lazy(() => import('./pages/knowledge/KnowledgePage'));

export function AdminRoutes() {
    return (
        <Suspense fallback={<LoadingFallback />}>
            <Routes>
                <Route element={<AdminRouteGuard />}>
                    <Route element={<AdminLayout />}>
                        <Route index element={<StatsPage />} />
                        <Route path="users" element={<UserManagePage />} />
                        <Route path="products" element={<ProductReviewPage />} />
                        <Route path="orders" element={<OrderManagePage />} />
                        <Route path="categories" element={<CategoryManagePage />} />
                        <Route path="knowledge" element={<KnowledgePage />} />
                    </Route>
                </Route>
            </Routes>
        </Suspense>
    );
}
