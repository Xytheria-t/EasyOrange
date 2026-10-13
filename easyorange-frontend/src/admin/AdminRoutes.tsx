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

const KnowledgePage = lazy(() => import('./pages/knowledge/KnowledgePage'));
const RetrievalQualityPage = lazy(() => import('./pages/retrieval/RetrievalQualityPage'));

export function AdminRoutes() {
    return (
        <Suspense fallback={<LoadingFallback />}>
            <Routes>
                <Route element={<AdminRouteGuard />}>
                    <Route element={<AdminLayout />}>
                        {/* 管理端前端只留 AI 运维两个面：知识库管理 + 检索质量回看（ADR-0012 的
                            兑现位）。业务 CRUD（用户/商品审核/订单/分类/统计）由后端 REST +
                            Swagger 承载，前端重复一份只摊薄维护预算——其余未知路径回首页 */}
                        <Route index element={<RetrievalQualityPage />} />
                        <Route path="knowledge" element={<KnowledgePage />} />
                        <Route path="retrieval-quality" element={<RetrievalQualityPage />} />
                        {/* 后台内未知路径回数据统计，绝不给演示留白屏 */}
                        <Route path="*" element={<Navigate to="/admin" replace />} />
                    </Route>
                </Route>
            </Routes>
        </Suspense>
    );
}
