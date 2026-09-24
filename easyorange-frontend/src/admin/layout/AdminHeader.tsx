import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { useMediaQuery } from '@/hooks';
import { useAuthStore } from '@/store';
import { useAdminStore } from '../store';

const PAGE_TITLES: Record<string, string> = {
    '/admin': '数据统计',
    '/admin/users': '用户管理',
    '/admin/products': '商品审核',
    '/admin/orders': '订单管理',
    '/admin/categories': '分类管理',
    '/admin/knowledge': '知识库管理',
};

interface AdminHeaderProps {
    onOpenMobileNav: () => void;
}

export function AdminHeader({ onOpenMobileNav }: AdminHeaderProps) {
    const { sidebarCollapsed, toggleSidebar } = useAdminStore();
    const { user } = useAuthStore();
    const navigate = useNavigate();
    const isDesktop = useMediaQuery('(min-width: 769px)');

    const pathname = window.location.pathname;
    const title = PAGE_TITLES[pathname] ?? '管理后台';

    const handleLogout = () => {
        navigate('/');
    };

    return (
        <header className={`admin-header ${sidebarCollapsed && isDesktop ? 'sidebar-collapsed' : ''}`}>
            <div className="header-left">
                <Button
                    variant="ghost"
                    size="icon"
                    onClick={isDesktop ? toggleSidebar : onOpenMobileNav}
                    className={`collapse-btn ${sidebarCollapsed && isDesktop ? 'collapsed' : ''}`}
                    title={isDesktop ? (sidebarCollapsed ? '展开侧边栏' : '收起侧边栏') : '打开导航菜单'}
                    aria-label={isDesktop ? (sidebarCollapsed ? '展开侧边栏' : '收起侧边栏') : '打开导航菜单'}
                >
                    {isDesktop ? (
                        <svg
                            aria-hidden="true"
                            fill="none"
                            stroke="currentColor"
                            viewBox="0 0 24 24"
                            strokeWidth="2"
                            strokeLinecap="round"
                            strokeLinejoin="round"
                        >
                            <path d="M11 19l-7-7 7-7M18 19l-7-7 7-7" />
                        </svg>
                    ) : (
                        <svg
                            aria-hidden="true"
                            fill="none"
                            stroke="currentColor"
                            viewBox="0 0 24 24"
                            strokeWidth="2"
                            strokeLinecap="round"
                        >
                            <path d="M3 6h18M3 12h18M3 18h18" />
                        </svg>
                    )}
                </Button>
                <span className="header-title">{title}</span>
            </div>

            <div className="header-right">
                <Button variant="ghost" className="header-user" onClick={handleLogout} title="返回主站">
                    <div className="header-user-avatar">
                        {user?.nickname?.charAt(0) || user?.username?.charAt(0) || 'A'}
                    </div>
                    <div className="header-user-info">
                        <span className="header-user-name">{user?.nickname || user?.username || '管理员'}</span>
                        <span className="header-user-role">超级管理员</span>
                    </div>
                </Button>
            </div>
        </header>
    );
}
