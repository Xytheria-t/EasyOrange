import { Outlet } from 'react-router-dom';
import { Header } from './Header';
import { ToastContainer } from '@/components/ui/Toast';
import { GlobalLoading } from '@/components/ui/Loading';
import ScrollProgressBar from '@/components/ui/ScrollProgressBar';

function MinimalLayout() {
  return (
    <div className="min-h-screen flex flex-col relative">
      <ScrollProgressBar />
      <Header />
      <main className="main-content flex-1 w-full" style={{ display: 'flex', flexDirection: 'column' }}>
        <Outlet />
      </main>
      <ToastContainer />
      <GlobalLoading />
    </div>
  );
}

export { MinimalLayout };
