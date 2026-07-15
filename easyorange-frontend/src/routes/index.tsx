import { lazy, Suspense } from 'react';
import { createBrowserRouter, createRoutesFromElements, Navigate, Route, useLocation } from 'react-router-dom';
import { Layout } from '@/components/layout/Layout';
import { MinimalLayout } from '@/components/layout/MinimalLayout';
import { PageMeta } from '@/components/seo/PageMeta';
import { getStoredToken } from '@/features/auth/session';

const LoadingFallback = () => (
    <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-orange-500"></div>
    </div>
);

const HomePage = lazy(() => import('@/pages/home/HomePage'));
const ProductsPage = lazy(() => import('@/pages/products/ProductsPage'));
const ProductDetailPage = lazy(() => import('@/pages/products/ProductDetailPage'));
const ProfilePage = lazy(() => import('@/pages/profile/ProfilePage'));
const FavoritesPage = lazy(() => import('@/pages/favorites/FavoritesPage'));
const MessagesPage = lazy(() => import('@/pages/messages/MessagesPage'));
const ChatWindowPage = lazy(() => import('@/pages/messages/ChatWindowPage'));
const OrdersPage = lazy(() => import('@/pages/orders/OrdersPage'));
const OrderDetailPage = lazy(() => import('@/pages/orders/OrderDetailPage'));
const PaymentPage = lazy(() => import('@/pages/orders/PaymentPage'));
const PaymentResultPage = lazy(() => import('@/pages/orders/PaymentResultPage'));
const PublishPage = lazy(() => import('@/pages/products/PublishPage'));
const MyProductsPage = lazy(() => import('@/pages/products/MyProductsPage'));
const SearchPage = lazy(() => import('@/pages/profile/SearchPage'));
const EditProductPage = lazy(() => import('@/pages/products/EditProductPage'));
const NotificationsPage = lazy(() => import('@/pages/notifications/NotificationsPage'));
const CreditPage = lazy(() => import('@/pages/profile/CreditPage'));
const LoginPage = lazy(() => import('@/pages/auth/LoginPage'));
const ForgotPasswordPage = lazy(() => import('@/pages/auth/ForgotPasswordPage'));
const NotFoundPage = lazy(() => import('@/pages/errors/NotFoundPage'));
const AdminRoutes = lazy(() => import('@/admin/AdminRoutes').then(m => ({ default: m.AdminRoutes })));

const ProtectedRoute = ({ children }: { children: React.ReactNode }) => {
    const location = useLocation();
    const token = getStoredToken();
    return token ? children : <Navigate to={`/login?redirect=${encodeURIComponent(location.pathname)}`} replace />;
};

interface RouteMeta {
    title: string;
    description?: string;
}

const withSuspense = (Component: React.LazyExoticComponent<React.ComponentType>, meta?: RouteMeta) => (
    <Suspense fallback={<LoadingFallback />}>
        {meta && <PageMeta title={meta.title} description={meta.description} />}
        <Component />
    </Suspense>
);

// Route title/description definitions
const R = {
    home: { title: '首页', description: 'EasyOrange - C2C 资产流转平台' },
    products: { title: '商品', description: '浏览全部商品' },
    productDetail: { title: '商品详情', description: '查看商品详细信息' },
    search: { title: '搜索', description: '搜索商品' },
    login: { title: '登录', description: '登录 EasyOrange' },
    forgotPwd: { title: '忘记密码' },
    profile: { title: '个人中心' },
    favorites: { title: '我的收藏' },
    messages: { title: '消息' },
    chat: { title: '聊天' },
    orders: { title: '我的订单' },
    orderDetail: { title: '订单详情' },
    payment: { title: '收银台' },
    paymentResult: { title: '支付结果' },
    publish: { title: '提交资产' },
    myProducts: { title: '我的商品' },
    editProduct: { title: '编辑商品' },
    notifications: { title: '通知中心' },
    credit: { title: '我的信用' },
    notFound: { title: '404' },
    admin: { title: '管理后台', description: 'EasyOrange 管理控制台' },
} as const;

export const router = createBrowserRouter(
    createRoutesFromElements(
        <>
            <Route path="/" element={<Layout />}>
                <Route index element={withSuspense(HomePage, R.home)} />
            </Route>
            <Route path="/" element={<MinimalLayout />}>
                <Route path="products" element={withSuspense(ProductsPage, R.products)} />
                <Route path="products/:id" element={withSuspense(ProductDetailPage, R.productDetail)} />
                <Route path="search" element={withSuspense(SearchPage, R.search)} />
                <Route
                    path="products/:id/edit"
                    element={<ProtectedRoute>{withSuspense(EditProductPage, R.editProduct)}</ProtectedRoute>}
                />
                <Route path="login" element={withSuspense(LoginPage, R.login)} />
                <Route path="forgot-password" element={withSuspense(ForgotPasswordPage, R.forgotPwd)} />
                <Route
                    path="profile"
                    element={<ProtectedRoute>{withSuspense(ProfilePage, R.profile)}</ProtectedRoute>}
                />
                <Route
                    path="favorites"
                    element={<ProtectedRoute>{withSuspense(FavoritesPage, R.favorites)}</ProtectedRoute>}
                />
                <Route
                    path="messages"
                    element={<ProtectedRoute>{withSuspense(MessagesPage, R.messages)}</ProtectedRoute>}
                />
                <Route
                    path="messages/:targetUserId"
                    element={<ProtectedRoute>{withSuspense(ChatWindowPage, R.chat)}</ProtectedRoute>}
                />
                <Route path="orders" element={<ProtectedRoute>{withSuspense(OrdersPage, R.orders)}</ProtectedRoute>} />
                <Route
                    path="orders/:id"
                    element={<ProtectedRoute>{withSuspense(OrderDetailPage, R.orderDetail)}</ProtectedRoute>}
                />
                <Route
                    path="payment"
                    element={<ProtectedRoute>{withSuspense(PaymentPage, R.payment)}</ProtectedRoute>}
                />
                <Route
                    path="payment/result"
                    element={<ProtectedRoute>{withSuspense(PaymentResultPage, R.paymentResult)}</ProtectedRoute>}
                />
                <Route
                    path="publish"
                    element={<ProtectedRoute>{withSuspense(PublishPage, R.publish)}</ProtectedRoute>}
                />
                <Route
                    path="my-products"
                    element={<ProtectedRoute>{withSuspense(MyProductsPage, R.myProducts)}</ProtectedRoute>}
                />
                <Route
                    path="notifications"
                    element={<ProtectedRoute>{withSuspense(NotificationsPage, R.notifications)}</ProtectedRoute>}
                />
                <Route path="credit" element={<ProtectedRoute>{withSuspense(CreditPage, R.credit)}</ProtectedRoute>} />
                <Route path="*" element={withSuspense(NotFoundPage, R.notFound)} />
            </Route>
            <Route path="admin/*" element={withSuspense(AdminRoutes, R.admin)} />
        </>
    )
);
