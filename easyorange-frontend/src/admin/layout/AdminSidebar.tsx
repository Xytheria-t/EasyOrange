import { ClipboardCheck, LayoutDashboard, ReceiptText, Users } from 'lucide-react';
import type { CSSProperties } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useDashboardStats } from '../hooks';
import { useAdminStore } from '../store';

// 管理端前端：概览 + 业务管理（审核 / 用户 / 订单）+ AI 运维（知识库 / 检索质量）
const navItems = [
    {
        section: '概览',
        items: [
            {
                path: '/admin',
                label: '数据总览',
                // 首页路径是所有管理端路由的前缀，前缀匹配会让它在每一页都亮着
                exact: true,
                icon: <LayoutDashboard size={17} aria-hidden="true" />,
            },
        ],
    },
    {
        section: '业务管理',
        items: [
            {
                path: '/admin/products',
                label: '商品审核',
                icon: <ClipboardCheck size={17} aria-hidden="true" />,
            },
            {
                path: '/admin/users',
                label: '用户管理',
                icon: <Users size={17} aria-hidden="true" />,
            },
            {
                path: '/admin/orders',
                label: '订单管理',
                icon: <ReceiptText size={17} aria-hidden="true" />,
            },
        ],
    },
    {
        section: 'AI 运维',
        items: [
            {
                path: '/admin/knowledge',
                label: '知识库管理',
                icon: (
                    <svg
                        aria-hidden="true"
                        fill="none"
                        stroke="currentColor"
                        viewBox="0 0 24 24"
                        strokeWidth="1.8"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                    >
                        <path d="M4 19.5A2.5 2.5 0 016.5 17H20" />
                        <path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z" />
                    </svg>
                ),
            },
            {
                path: '/admin/retrieval-quality',
                label: '检索质量回看',
                icon: (
                    <svg
                        aria-hidden="true"
                        fill="none"
                        stroke="currentColor"
                        viewBox="0 0 24 24"
                        strokeWidth="1.8"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                    >
                        <path d="M3 3v18h18" />
                        <path d="M7 15l4-5 3 3 5-7" />
                    </svg>
                ),
            },
        ],
    },
];

interface AdminSidebarProps {
    /** 移动端抽屉里点击导航项后关闭抽屉 */
    onNavigate?: () => void;
    /** 抽屉内复用时覆盖定位方式 */
    className?: string;
}

/**
 * 运行环境徽标 —— 把 vite 的 MODE 翻成人话。
 *
 * 摆在侧边栏底部那段本来就空着的留白里。后台演示最常见的手滑是改着改着连错了栈，
 * 页面上又没有地方交代当前连的是哪套，出了问题只能回头翻 vite.config 的 proxy。
 * 色值走 admin.css 令牌（开发=琥珀 / 生产=青），不写字面量。
 */
const ENV_PRESETS: Record<string, { label: string; color: string }> = {
    development: { label: '开发环境', color: 'var(--admin-flow-ship)' },
    production: { label: '生产环境', color: 'var(--admin-flow-done)' },
    test: { label: '测试环境', color: 'var(--admin-chart-5)' },
};
const RUNTIME_ENV = ENV_PRESETS[import.meta.env.MODE] ?? {
    label: import.meta.env.MODE,
    color: 'var(--admin-faint)',
};

export function AdminSidebar({ onNavigate, className = '' }: AdminSidebarProps = {}) {
    const { sidebarCollapsed } = useAdminStore();
    const location = useLocation();

    // 待审核商品数直接标在导航项上：运营一进来就知道有多少件等着自己处理，
    // 不用先点进审核页才看到数字。与总览页共用同一个查询键，不会多打一次接口。
    const { data: dashboardStats } = useDashboardStats();
    const pendingReview = Number(dashboardStats?.pendingProducts ?? 0);

    // 首页（/admin）是所有管理端路由的前缀，前缀匹配会让它在每一页都亮着，
    // 这类项走 exact：只有路径完全相等才算选中
    const isActive = (path: string, exact = false) =>
        exact ? location.pathname === path : location.pathname.startsWith(path);

    // 抽屉内始终展开，不跟随桌面端的折叠态
    const collapsed = className.includes('admin-sidebar--drawer') ? false : sidebarCollapsed;

    return (
        <aside className={`admin-sidebar ${collapsed ? 'collapsed' : ''} ${className}`}>
            {/* Logo */}
            <div className="sidebar-logo">
                <div className="sidebar-logo-icon">
                    <svg aria-hidden="true" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor">
                        <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            d="M15.362 5.214A8.252 8.252 0 0112 21 8.25 8.25 0 016.038 7.048 8.287 8.287 0 009 9.6a8.983 8.983 0 013.361-6.867 8.21 8.21 0 003 2.48z"
                        />
                        <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            d="M12 18a3.75 3.75 0 00.495-7.467 5.99 5.99 0 00-1.925 3.546 5.974 5.974 0 01-2.133-1A3.75 3.75 0 0012 18z"
                        />
                    </svg>
                </div>
                <span className="sidebar-logo-text">易橙管理</span>
            </div>

            {/* Navigation */}
            <nav className="sidebar-nav">
                {navItems.map(section => (
                    <div key={section.section} className="nav-section">
                        {/* 条件渲染必须看 collapsed 而不是裸的 sidebarCollapsed：抽屉里
                            collapsed 被强制成 false（永远展开），若读 store，
                            桌面端收起过一次后，抽屉里的分组标题会整片消失。 */}
                        {!collapsed && <div className="nav-section-title">{section.section}</div>}
                        {section.items.map(item => (
                            <Link
                                key={item.path}
                                to={item.path}
                                className={`nav-item ${isActive(item.path, 'exact' in item ? item.exact : false) ? 'active' : ''}`}
                                title={collapsed ? item.label : undefined}
                                onClick={onNavigate}
                            >
                                <span className="nav-item-icon">{item.icon}</span>
                                <span className="nav-item-text">{item.label}</span>
                                {item.path === '/admin/products' && pendingReview > 0 ? (
                                    <span className="nav-item-count">{pendingReview}</span>
                                ) : null}
                            </Link>
                        ))}
                    </div>
                ))}
            </nav>

            {/* Footer */}
            <div className="sidebar-footer">
                <div
                    className="sidebar-env"
                    title={`当前构建模式：${import.meta.env.MODE}`}
                    style={{ '--env-color': RUNTIME_ENV.color } as CSSProperties}
                >
                    <span className="sidebar-env-dot" aria-hidden="true" />
                    <span className="sidebar-env-text">{RUNTIME_ENV.label}</span>
                </div>
                <Link
                    to="/"
                    className="sidebar-back-link"
                    title={collapsed ? '返回主站' : undefined}
                    onClick={onNavigate}
                >
                    <svg
                        aria-hidden="true"
                        fill="none"
                        stroke="currentColor"
                        viewBox="0 0 24 24"
                        strokeWidth="1.8"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                    >
                        <path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z" />
                        <polyline points="9 22 9 12 15 12 15 22" />
                    </svg>
                    <span>返回主站</span>
                </Link>
            </div>
        </aside>
    );
}
