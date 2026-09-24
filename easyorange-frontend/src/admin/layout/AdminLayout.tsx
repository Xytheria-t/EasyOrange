import { useState } from 'react';
import { Outlet } from 'react-router-dom';
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from '@/components/ui/sheet';
import { ToastContainer } from '@/components/ui/Toast';
import { useMediaQuery } from '@/hooks';
import { useAdminStore } from '../store';
import { AdminHeader } from './AdminHeader';
import { AdminSidebar } from './AdminSidebar';
import './admin-layout.css';

export function AdminLayout() {
    const { sidebarCollapsed } = useAdminStore();
    const isDesktop = useMediaQuery('(min-width: 769px)');
    const [mobileNavOpen, setMobileNavOpen] = useState(false);

    return (
        <div className="admin-root admin-layout">
            {isDesktop ? (
                <AdminSidebar />
            ) : (
                <Sheet open={mobileNavOpen} onOpenChange={setMobileNavOpen}>
                    <SheetContent side="left" className="admin-nav-drawer">
                        <SheetHeader className="sr-only">
                            <SheetTitle>管理后台导航</SheetTitle>
                            <SheetDescription>切换到各个管理页面</SheetDescription>
                        </SheetHeader>
                        <AdminSidebar className="admin-sidebar--drawer" onNavigate={() => setMobileNavOpen(false)} />
                    </SheetContent>
                </Sheet>
            )}
            <div className="admin-layout-main">
                <div className="admin-content-wrapper">
                    <AdminHeader onOpenMobileNav={() => setMobileNavOpen(true)} />
                    <main className={`admin-content ${sidebarCollapsed ? 'sidebar-collapsed' : ''}`}>
                        <Outlet />
                    </main>
                </div>
            </div>
            <ToastContainer />
        </div>
    );
}
