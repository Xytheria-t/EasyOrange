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

    it('renders the two AI-ops nav items only', () => {
        render(<AdminSidebar />);
        expect(screen.getByText('知识库管理')).toBeInTheDocument();
        expect(screen.getByText('检索质量回看')).toBeInTheDocument();
        // 业务 CRUD 导航已裁（走后端 REST），不应再出现
        expect(screen.queryByText('用户管理')).not.toBeInTheDocument();
        expect(screen.queryByText('商品审核')).not.toBeInTheDocument();
        expect(screen.queryByText('订单管理')).not.toBeInTheDocument();
        expect(screen.queryByText('数据统计')).not.toBeInTheDocument();
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
