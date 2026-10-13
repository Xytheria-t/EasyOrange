import { Outlet } from 'react-router-dom';
import BackgroundEffects from '@/components/sections/BackgroundEffects';
import Footer from '@/components/sections/Footer';
import ScrollProgressBar from '@/components/ui/ScrollProgressBar';
import { ToastContainer } from '@/components/ui/Toast';
import { useChatUnreadSocket } from '@/hooks/useChatUnreadSocket';
import { Header } from './Header';

function Layout() {
    // 顶栏未读角标的 WS 触发器：登录态常驻，不随页面卸载丢连接
    useChatUnreadSocket();

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
        </div>
    );
}

export { Layout };
