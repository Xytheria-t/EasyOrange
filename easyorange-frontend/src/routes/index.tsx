import { lazy, Suspense } from 'react';
import { createBrowserRouter, createRoutesFromElements, Navigate, Route, useLocation } from 'react-router-dom';
import { Layout } from '@/components/layout/Layout';
import { MinimalLayout } from '@/components/layout/MinimalLayout';
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
const OrdersPage = lazy(() => import('@/pages/orders/OrdersPage'));
const OrderDetailPage = lazy(() => import('@/pages/orders/OrderDetailPage'));
const PaymentPage = lazy(() => import('@/pages/orders/PaymentPage'));
const PaymentResultPage = lazy(() => import('@/pages/orders/PaymentResultPage'));
const PublishPage = lazy(() => import('@/pages/products/PublishPage'));
const SearchPage = lazy(() => import('@/pages/profile/SearchPage'));
const EditProductPage = lazy(() => import('@/pages/products/EditProductPage'));
const LoginPage = lazy(() => import('@/pages/auth/LoginPage'));
const ForgotPasswordPage = lazy(() => import('@/pages/auth/ForgotPasswordPage'));
const NotFoundPage = lazy(() => import('@/pages/errors/NotFoundPage'));

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
    <>
      <Route path="/" element={<Layout />}>
        <Route index element={withSuspense(HomePage)} />
      </Route>
      <Route path="/" element={<MinimalLayout />}>
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
    </>
  )
);
