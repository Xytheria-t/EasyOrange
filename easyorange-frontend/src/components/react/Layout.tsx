import { Outlet } from 'react-router-dom';
import { Header } from './Header';
import { ToastContainer } from '../ui/Toast';
import { GlobalLoading } from '../ui/Loading';

function Layout() {
  return (
    <div className="min-h-screen bg-gray-50">
      <Header />
      <main className="mx-auto max-w-7xl overflow-hidden pb-6">
        <Outlet />
      </main>
      <footer className="mt-12 border-t bg-white py-8">
        <div className="mx-auto max-w-7xl px-4 text-center text-sm text-gray-500">
          <p>© 2026 EasyOrange · 校园二手交易平台</p>
        </div>
      </footer>
      <ToastContainer />
      <GlobalLoading />
    </div>
  );
}

export { Layout };
