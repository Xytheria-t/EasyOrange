import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { QuickActionsPanel } from './QuickActionsPanel';

function renderWithRouter(ui: React.ReactNode) {
    return render(<MemoryRouter>{ui}</MemoryRouter>);
}

describe('QuickActionsPanel', () => {
    it('renders all 6 quick action links', () => {
        renderWithRouter(<QuickActionsPanel />);

        expect(screen.getByText('快捷操作')).toBeInTheDocument();

        const actions = ['商品审核', '处理举报', '订单管理', '用户管理', '分类管理', '数据统计'];
        for (const label of actions) {
            expect(screen.getByText(label)).toBeInTheDocument();
        }
    });

    it('links each action to the correct route', () => {
        renderWithRouter(<QuickActionsPanel />);

        const expectations: Record<string, string> = {
            商品审核: '/admin/products?status=0',
            处理举报: '/admin/reports',
            订单管理: '/admin/orders',
            用户管理: '/admin/users',
            分类管理: '/admin/categories',
            数据统计: '/admin/stats',
        };

        for (const [label, route] of Object.entries(expectations)) {
            expect(screen.getByText(label).closest('a')).toHaveAttribute('href', route);
        }
    });

    it('displays pending counts for actions that support them', () => {
        renderWithRouter(
            <QuickActionsPanel pendingItems={{ pendingProducts: 12, pendingReports: 4, pendingOrders: 8 }} />
        );

        expect(screen.getByText('12')).toBeInTheDocument();
        expect(screen.getByText('4')).toBeInTheDocument();
        expect(screen.getByText('8')).toBeInTheDocument();

        expect(screen.queryByText('0')).not.toBeInTheDocument();
    });

    it('does not render zero pending badges', () => {
        renderWithRouter(
            <QuickActionsPanel pendingItems={{ pendingProducts: 0, pendingReports: 0, pendingOrders: 0 }} />
        );

        expect(screen.queryByText('0')).not.toBeInTheDocument();
    });

    it('handles undefined pendingItems gracefully', () => {
        renderWithRouter(<QuickActionsPanel />);

        expect(screen.queryByText('0')).not.toBeInTheDocument();
        expect(screen.getAllByText(/商品审核|处理举报|订单管理|用户管理|分类管理|数据统计/)).toHaveLength(6);
    });
});
