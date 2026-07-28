import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { DashboardChartsSection } from './DashboardChartsSection';

vi.mock('../charts/ActivityHeatmap', () => ({ default: () => <div data-testid="activity-heatmap" /> }));
vi.mock('../charts/lazyCharts', () => ({
    LazyTrendChart: () => <div data-testid="trend-chart" />,
    LazyTopProductsChart: () => <div data-testid="top-products-chart" />,
}));

function renderWithRouter(ui: React.ReactNode) {
    return render(<MemoryRouter>{ui}</MemoryRouter>);
}

describe('DashboardChartsSection', () => {
    it('renders three chart sections', () => {
        renderWithRouter(<DashboardChartsSection trend={[]} heatmap={[]} topProducts={[]} />);

        expect(screen.getByText('趋势概览')).toBeInTheDocument();
        expect(screen.getByText('用户活跃时段')).toBeInTheDocument();
        expect(screen.getByText('Top 浏览量商品')).toBeInTheDocument();

        expect(screen.getByTestId('trend-chart')).toBeInTheDocument();
        expect(screen.getByTestId('activity-heatmap')).toBeInTheDocument();
        expect(screen.getByTestId('top-products-chart')).toBeInTheDocument();
    });

    it('links trend chart to stats page', () => {
        renderWithRouter(<DashboardChartsSection trend={[]} heatmap={[]} topProducts={[]} />);

        const detailLink = screen.getByText('详情').closest('a');
        expect(detailLink).toHaveAttribute('href', '/admin/stats');
    });
});
