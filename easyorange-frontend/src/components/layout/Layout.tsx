import { Outlet } from 'react-router-dom';
import { Header } from './Header';
import Footer from '@/components/sections/Footer';
import { ToastContainer } from '@/components/ui/Toast';
import { GlobalLoading } from '@/components/ui/Loading';
import BackgroundEffects from '@/components/sections/BackgroundEffects';
import ScrollProgressBar from '@/components/ui/ScrollProgressBar';

function Layout() {
  return (
    <div className="min-h-screen flex flex-col relative">
      <ScrollProgressBar />
      <BackgroundEffects />
      <Header />
      <main className="main-content flex-1 w-full">
        <Outlet />
      </main>
      <Footer />
      <ToastContainer />
      <GlobalLoading />
    </div>
  );
}

export { Layout };
