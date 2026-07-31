import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { DashboardRecentProducts } from './DashboardRecentProducts';

function renderWithRouter(ui: React.ReactNode) {
    return render(<MemoryRouter>{ui}</MemoryRouter>);
}

describe('DashboardRecentProducts', () => {
    beforeEach(() => {
        vi.useFakeTimers();
        vi.setSystemTime(new Date(2026, 4, 16, 10, 0, 0));
    });

    it('renders product list with icons and prices', () => {
        renderWithRouter(
            <DashboardRecentProducts
                products={[
                    {
                        productId: '1',
                        name: '高等数学教材',
                        price: 45,
                        mainImage: null,
                        status: 'ONLINE',
                        statusDesc: '上架',
                        sellerId: null,
                        sellerName: null,
                        categoryName: null,
                        viewCount: null,
                        createTime: new Date(Date.now() - 120_000).toISOString(),
                    },
                    {
                        productId: '2',
                        name: 'iPhone 手机',
                        price: 3000,
                        mainImage: null,
                        status: 'ONLINE',
                        statusDesc: '上架',
                        sellerId: null,
                        sellerName: null,
                        categoryName: null,
                        viewCount: null,
                        createTime: new Date(Date.now() - 300_000).toISOString(),
                    },
                ]}
                isLoading={false}
            />
        );

        expect(screen.getByText('最近上架商品')).toBeInTheDocument();
        expect(screen.getByText('高等数学教材')).toBeInTheDocument();
        expect(screen.getByText('iPhone 手机')).toBeInTheDocument();
        expect(screen.getByText('¥45')).toBeInTheDocument();
        expect(screen.getByText('¥3,000')).toBeInTheDocument();
        expect(screen.getByText('\u{1F4DA}')).toBeInTheDocument();
        expect(screen.getByText('\u{1F4F1}')).toBeInTheDocument();

        const viewAllLink = screen.getByText('查看全部').closest('a');
        expect(viewAllLink).toHaveAttribute('href', '/admin/products');
    });

    it('renders empty state when no products', () => {
        renderWithRouter(<DashboardRecentProducts products={[]} isLoading={false} />);

        expect(screen.getByText('暂无商品记录')).toBeInTheDocument();
        expect(screen.getByText('当前暂无新上架商品')).toBeInTheDocument();
    });

    it('renders skeleton placeholders when loading', () => {
        renderWithRouter(<DashboardRecentProducts isLoading />);

        expect(screen.getByText('最近上架商品')).toBeInTheDocument();
        expect(screen.queryByText('高等数学教材')).not.toBeInTheDocument();
    });
});
