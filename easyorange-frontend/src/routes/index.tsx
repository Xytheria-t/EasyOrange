import { lazy, Suspense } from 'react';
import { createBrowserRouter, createRoutesFromElements, Navigate, Route, useLocation } from 'react-router-dom';
import { Layout } from '@/components/layout/Layout';
import { getStoredToken } from '@/features/auth/session';

const LoadingFallback = () => (
  <div className="flex items-center justify-center min-h-screen">
    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-orange-500"></div>
  </div>
);

const HomePage = lazy(() => import('@/pages/HomePage'));
const ProductsPage = lazy(() => import('@/pages/ProductsPage').then(m => ({ default: m.ProductsPage })));
const ProductDetailPage = lazy(() => import('@/pages/ProductDetailPage').then(m => ({ default: m.ProductDetailPage })));
const ProfilePage = lazy(() => import('@/pages/ProfilePage').then(m => ({ default: m.ProfilePage })));
const FavoritesPage = lazy(() => import('@/pages/FavoritesPage').then(m => ({ default: m.FavoritesPage })));
const MessagesPage = lazy(() => import('@/pages/MessagesPage').then(m => ({ default: m.MessagesPage })));
const OrdersPage = lazy(() => import('@/pages/OrdersPage').then(m => ({ default: m.OrdersPage })));
const OrderDetailPage = lazy(() => import('@/pages/OrderDetailPage').then(m => ({ default: m.OrderDetailPage })));
const PaymentPage = lazy(() => import('@/pages/PaymentPage').then(m => ({ default: m.PaymentPage })));
const PaymentResultPage = lazy(() => import('@/pages/PaymentResultPage').then(m => ({ default: m.PaymentResultPage })));
const PublishPage = lazy(() => import('@/pages/PublishPage').then(m => ({ default: m.PublishPage })));
const SearchPage = lazy(() => import('@/pages/SearchPage').then(m => ({ default: m.SearchPage })));
const EditProductPage = lazy(() => import('@/pages/EditProductPage').then(m => ({ default: m.EditProductPage })));
const LoginPage = lazy(() => import('@/pages/LoginPage').then(m => ({ default: m.LoginPage })));
const ForgotPasswordPage = lazy(() => import('@/pages/ForgotPasswordPage').then(m => ({ default: m.ForgotPasswordPage })));
const NotFoundPage = lazy(() => import('@/pages/NotFoundPage').then(m => ({ default: m.NotFoundPage })));

const ProtectedRoute = ({ children }: { children: React.ReactNode }) => {
  const location = useLocation();
  const token = getStoredToken();
  return token ? <>{children}</> : <Navigate to={`/login?redirect=${encodeURIComponent(location.pathname)}`} replace />;
};

const withSuspense = (Component: React.LazyExoticComponent<React.ComponentType>) => (
  <Suspense fallback={<LoadingFallback />}>
    <Component />
  </Suspense>
);

export const router = createBrowserRouter(
  createRoutesFromElements(
    <Route path="/" element={<Layout />}>
      <Route index element={withSuspense(HomePage)} />
      <Route path="products" element={withSuspense(ProductsPage)} />
      <Route path="products/:id" element={withSuspense(ProductDetailPage)} />
      <Route path="search" element={withSuspense(SearchPage)} />
      <Route
        path="products/:id/edit"
        element={
          <ProtectedRoute>
            {withSuspense(EditProductPage)}
          </ProtectedRoute>
        }
      />
      <Route path="login" element={withSuspense(LoginPage)} />
      <Route path="forgot-password" element={withSuspense(ForgotPasswordPage)} />
      <Route
        path="profile"
        element={
          <ProtectedRoute>
            {withSuspense(ProfilePage)}
          </ProtectedRoute>
        }
      />
      <Route
        path="favorites"
        element={
          <ProtectedRoute>
            {withSuspense(FavoritesPage)}
          </ProtectedRoute>
        }
      />
      <Route
        path="messages"
        element={
          <ProtectedRoute>
            {withSuspense(MessagesPage)}
          </ProtectedRoute>
        }
      />
      <Route
        path="orders"
        element={
          <ProtectedRoute>
            {withSuspense(OrdersPage)}
          </ProtectedRoute>
        }
      />
      <Route
        path="orders/:id"
        element={
          <ProtectedRoute>
            {withSuspense(OrderDetailPage)}
          </ProtectedRoute>
        }
      />
      <Route
        path="payment"
        element={
          <ProtectedRoute>
            {withSuspense(PaymentPage)}
          </ProtectedRoute>
        }
      />
      <Route
        path="payment/result"
        element={
          <ProtectedRoute>
            {withSuspense(PaymentResultPage)}
          </ProtectedRoute>
        }
      />
      <Route
        path="publish"
        element={
          <ProtectedRoute>
            {withSuspense(PublishPage)}
          </ProtectedRoute>
        }
      />
      <Route path="*" element={withSuspense(NotFoundPage)} />
    </Route>
  )
);
