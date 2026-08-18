import { Outlet } from 'react-router-dom';
import { ToastContainer } from '@/components/ui/Toast';
import { useAdminStore } from '../store';
import { AdminHeader } from './AdminHeader';
import { AdminSidebar } from './AdminSidebar';
import './admin-layout.css';

export function AdminLayout() {
    const { sidebarCollapsed } = useAdminStore();

    return (
        <div className="admin-root admin-layout">
            <AdminSidebar />
            <div className="admin-layout-main">
                <div className="admin-content-wrapper">
                    <AdminHeader />
                    <main className={`admin-content ${sidebarCollapsed ? 'sidebar-collapsed' : ''}`}>
                        <Outlet />
                    </main>
                </div>
            </div>
            <ToastContainer />
        </div>
    );
}
