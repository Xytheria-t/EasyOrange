import { useNavigate } from 'react-router-dom';
import { useAdminStore } from '../store';
import { useAuthStore } from '@/store';
import { Button } from '@/components/ui/button';

const PAGE_TITLES: Record<string, string> = {
  '/admin': '仪表盘',
  '/admin/users': '用户管理',
  '/admin/products': '商品审核',
  '/admin/orders': '订单管理',
  '/admin/reports': '举报处理',
  '/admin/stats': '数据统计',
};

export function AdminHeader() {
  const { sidebarCollapsed, toggleSidebar } = useAdminStore();
  const { user } = useAuthStore();
  const navigate = useNavigate();

  const pathname = window.location.pathname;
  const title = PAGE_TITLES[pathname] ?? '管理后台';

  const handleLogout = () => {
    navigate('/');
  };

  return (
    <header className={`admin-header ${sidebarCollapsed ? 'sidebar-collapsed' : ''}`}>
      <div className="header-left">
        <Button
          variant="ghost"
          size="icon"
          onClick={toggleSidebar}
          className={`collapse-btn ${sidebarCollapsed ? 'collapsed' : ''}`}
          title={sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'}
        >
          <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M11 19l-7-7 7-7M18 19l-7-7 7-7" />
          </svg>
        </Button>
        <span className="header-title">{title}</span>
      </div>

      <div className="header-right">
        <div
          className="header-user"
          role="button"
          tabIndex={0}
          onClick={handleLogout}
          onKeyDown={(e) => e.key === 'Enter' && handleLogout()}
          title="返回主站"
        >
          <div className="header-user-avatar">
            {user?.nickname?.charAt(0) || user?.username?.charAt(0) || 'A'}
          </div>
          <div className="header-user-info">
            <span className="header-user-name">{user?.nickname || user?.username || '管理员'}</span>
            <span className="header-user-role">超级管理员</span>
          </div>
        </div>
      </div>
    </header>
  );
}
