import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { DashboardStatsGrid } from './DashboardStatsGrid';

describe('DashboardStatsGrid', () => {
    it('renders 5 stat cards with correct values', () => {
        render(
            <DashboardStatsGrid
                stats={{
                    totalUsers: 1000,
                    todayNewUsers: 25,
                    totalProducts: 500,
                    pendingProducts: 10,
                    totalOrders: 300,
                }}
                isLoading={false}
            />
        );

        expect(screen.getByText('用户总数')).toBeInTheDocument();
        expect(screen.getByText('今日新增')).toBeInTheDocument();
        expect(screen.getByText('商品总数')).toBeInTheDocument();
        expect(screen.getByText('待审商品')).toBeInTheDocument();
        expect(screen.getByText('订单总量')).toBeInTheDocument();

        expect(screen.getByText('1,000')).toBeInTheDocument();
        expect(screen.getByText('25')).toBeInTheDocument();
        expect(screen.getByText('500')).toBeInTheDocument();
        expect(screen.getByText('10')).toBeInTheDocument();
        expect(screen.getByText('300')).toBeInTheDocument();
    });

    it('renders loading skeletons when isLoading is true', () => {
        render(<DashboardStatsGrid isLoading />);

        // 5 skeleton blocks should render
        const skeletons = screen.getAllByRole('generic').filter(el => el.className === '');
        expect(skeletons.length).toBeGreaterThanOrEqual(5);
    });

    it('falls back to 0 when stats are undefined', () => {
        render(<DashboardStatsGrid isLoading={false} />);

        expect(screen.getAllByText('0').length).toBe(5);
    });
});
