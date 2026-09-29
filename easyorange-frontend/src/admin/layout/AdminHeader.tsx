import { useIsFetching, useQueryClient } from '@tanstack/react-query';
import { LogOut, RefreshCw } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { useLogout, useMediaQuery } from '@/hooks';
import { useAuthStore } from '@/store';
import { useUIStore } from '@/store/uiStore';
import { userTypeLabel } from '../components/StatusBadge';
import { useAdminStore } from '../store';

interface AdminHeaderProps {
    onOpenMobileNav: () => void;
}

export function AdminHeader({ onOpenMobileNav }: AdminHeaderProps) {
    const { sidebarCollapsed, toggleSidebar } = useAdminStore();
    const { user } = useAuthStore();
    const addToast = useUIStore(s => s.addToast);
    const logout = useLogout();
    const navigate = useNavigate();
    const isDesktop = useMediaQuery('(min-width: 769px)');
    const queryClient = useQueryClient();
    // 管理端所有查询键都以 ['admin'] 开头（见各 useAdminXxx 的 KEYS 出口），
    // 一个前缀就能刷全站，不必让每页各自暴露 refetch
    const isFetching = useIsFetching({ queryKey: ['admin'] }) > 0;

    const handleBackToSite = () => {
        navigate('/');
    };

    /** 与主站 Header 的登出同源（sessionLogout + 清查询缓存）；落登录页 —— 会话刚结束，下一步只能是重新认证 */
    const handleLogout = async () => {
        await logout();
        addToast({ type: 'success', message: '已退出登录' });
        navigate('/login');
    };

    const handleRefresh = () => {
        void queryClient.invalidateQueries({ queryKey: ['admin'] });
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
            </div>

            <div className="header-right">
                {/* 手动刷新：管理端读的全是实时值，页面却从不交代数据是什么时候的；
                    顶栏此前除折叠钮和用户块外整条空着，这里把刷新补上。 */}
                <Button
                    variant="ghost"
                    className="header-refresh"
                    onClick={handleRefresh}
                    disabled={isFetching}
                    data-busy={isFetching}
                    title="刷新全部管理端数据"
                    aria-label={isFetching ? '正在刷新数据' : '刷新全部管理端数据'}
                >
                    <span className="header-refresh-icon">
                        <RefreshCw size={14} aria-hidden="true" />
                    </span>
                    {isFetching ? '刷新中' : '刷新数据'}
                </Button>
                {/* 两个动作位分开：用户区块只回主站（不结束会话），登出是独立的显式动作 ——
                    此前一个函数名叫 logout 的按钮只做 navigate('/')，语义割裂，极易误用。
                    角色按真实 userType 显示；此前写死「超级管理员」，管理员（02）看了也是超管 */}
                <Button variant="ghost" className="header-user" onClick={handleBackToSite} title="返回主站">
                    <div className="header-user-avatar">
                        {user?.nickname?.charAt(0) || user?.username?.charAt(0) || 'A'}
                    </div>
                    <div className="header-user-info">
                        <span className="header-user-name">{user?.nickname || user?.username || '管理员'}</span>
                        <span className="header-user-role">{userTypeLabel(user?.userType)}</span>
                    </div>
                </Button>
                <Button
                    variant="ghost"
                    size="icon"
                    className="admin-icon-button"
                    onClick={() => void handleLogout()}
                    title="退出登录"
                    aria-label="退出登录"
                >
                    <LogOut size={16} aria-hidden="true" />
                </Button>
            </div>
        </header>
    );
}
