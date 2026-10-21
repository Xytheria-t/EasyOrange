import { render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AdminSidebar } from './AdminSidebar';

const mockLocation = { pathname: '/admin' };
vi.mock('react-router-dom', () => ({
    Link: ({ children, to, ...props }: { children: React.ReactNode; to: string; [key: string]: unknown }) => (
        <a href={to} {...props}>
            {children}
        </a>
    ),
    useLocation: () => mockLocation,
}));

const mockAdminStore = vi.fn();
vi.mock('../store', () => ({
    useAdminStore: () => mockAdminStore(),
}));

describe('AdminSidebar', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        mockLocation.pathname = '/admin';
        mockAdminStore.mockReturnValue({
            sidebarCollapsed: false,
        });
    });

    it('renders logo text', () => {
        render(<AdminSidebar />);
        expect(screen.getByText('易橙管理')).toBeInTheDocument();
    });

    it('renders the AI-ops section', () => {
        render(<AdminSidebar />);
        expect(screen.getByText('AI 运维')).toBeInTheDocument();
    });

    it('renders the overview and business nav items', () => {
        render(<AdminSidebar />);
        expect(screen.getByText('数据总览')).toBeInTheDocument();
        expect(screen.getByText('知识库管理')).toBeInTheDocument();
        expect(screen.getByText('检索质量回看')).toBeInTheDocument();
        expect(screen.getByText('用户管理')).toBeInTheDocument();
        expect(screen.getByText('商品审核')).toBeInTheDocument();
        expect(screen.getByText('订单管理')).toBeInTheDocument();
    });

    it('marks only 数据总览 as active on /admin', () => {
        render(<AdminSidebar />);
        // /admin 是所有管理端路由的前缀，首页项走 exact，否则每一页都亮着
        expect(screen.getByText('数据总览').closest('a')).toHaveClass('active');
        expect(screen.getByText('商品审核').closest('a')).not.toHaveClass('active');
    });

    it('renders back to main site link', () => {
        render(<AdminSidebar />);
        expect(screen.getByText('返回主站')).toBeInTheDocument();
    });

    it('marks 检索质量回看 as active when on /admin/retrieval-quality', () => {
        mockLocation.pathname = '/admin/retrieval-quality';
        render(<AdminSidebar />);
        const retrievalLink = screen.getByText('检索质量回看').closest('a');
        expect(retrievalLink).toHaveClass('active');
    });

    it('marks nested route as active for both parent and child', () => {
        mockLocation.pathname = '/admin/retrieval-quality/runs';
        render(<AdminSidebar />);
        // 前缀匹配：/admin/retrieval-quality/runs 命中 /admin/retrieval-quality
        const retrievalLink = screen.getByText('检索质量回看').closest('a');
        expect(retrievalLink).toHaveClass('active');
    });

    it('does not collapse sections when sidebarCollapsed is false', () => {
        render(<AdminSidebar />);
        expect(screen.getByText('AI 运维')).toBeVisible();
    });

    it('hides section titles when sidebarCollapsed is true', () => {
        mockAdminStore.mockReturnValue({
            sidebarCollapsed: true,
        });
        render(<AdminSidebar />);
        expect(screen.queryByText('AI 运维')).not.toBeInTheDocument();
    });

    it('applies collapsed class when sidebarCollapsed is true', () => {
        mockAdminStore.mockReturnValue({
            sidebarCollapsed: true,
        });
        const { container } = render(<AdminSidebar />);
        expect(container.querySelector('.admin-sidebar')).toHaveClass('collapsed');
    });
});
