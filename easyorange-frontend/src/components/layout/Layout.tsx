import { Outlet } from 'react-router-dom';
import BackgroundEffects from '@/components/sections/BackgroundEffects';
import Footer from '@/components/sections/Footer';
import { GlobalLoading } from '@/components/ui/Loading';
import ScrollProgressBar from '@/components/ui/ScrollProgressBar';
import { ToastContainer } from '@/components/ui/Toast';
import { Header } from './Header';

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
