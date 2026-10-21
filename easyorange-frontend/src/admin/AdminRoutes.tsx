import { lazy, Suspense } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { AdminRouteGuard } from './components/AdminRouteGuard';
import { AdminLayout } from './layout';
import '@/admin/styles/admin.css';

const LoadingFallback = () => (
    <div className="flex items-center justify-center min-h-screen bg-gradient-to-br from-slate-50 to-slate-100">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-orange-500"></div>
    </div>
);

const OverviewPage = lazy(() => import('./pages/overview/OverviewPage'));
const ProductReviewPage = lazy(() => import('./pages/products/ProductReviewPage'));
const UserManagePage = lazy(() => import('./pages/users/UserManagePage'));
const OrderManagePage = lazy(() => import('./pages/orders/OrderManagePage'));
const KnowledgePage = lazy(() => import('./pages/knowledge/KnowledgePage'));
const RetrievalQualityPage = lazy(() => import('./pages/retrieval/RetrievalQualityPage'));

export function AdminRoutes() {
    return (
        <Suspense fallback={<LoadingFallback />}>
            <Routes>
                <Route element={<AdminRouteGuard />}>
                    <Route element={<AdminLayout />}>
                        {/* 数据总览是管理端首页：一屏看全用户 / 商品 / 订单 / 营收四个口径。
                            /admin/overview 是它的深链别名，缺它时该 URL 在内层 Routes 无匹配
                            → 整棵树渲染 null → 白屏（无链接指向时也兜住手输地址） */}
                        <Route index element={<OverviewPage />} />
                        <Route path="overview" element={<OverviewPage />} />
                        {/* 业务管理面：审核 / 用户风控 / 订单干预 */}
                        <Route path="products" element={<ProductReviewPage />} />
                        <Route path="users" element={<UserManagePage />} />
                        <Route path="orders" element={<OrderManagePage />} />
                        {/* AI 运维面：知识库管理 + 检索质量回看（ADR-0012 的兑现位） */}
                        <Route path="knowledge" element={<KnowledgePage />} />
                        <Route path="retrieval-quality" element={<RetrievalQualityPage />} />
                        {/* 后台内未知路径回总览，绝不给演示留白屏 */}
                        <Route path="*" element={<Navigate to="/admin" replace />} />
                    </Route>
                </Route>
            </Routes>
        </Suspense>
    );
}
