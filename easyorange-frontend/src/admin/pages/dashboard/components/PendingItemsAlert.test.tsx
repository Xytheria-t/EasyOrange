import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { PendingItemsAlert } from './PendingItemsAlert';

function renderWithRouter(ui: React.ReactNode) {
    return render(<MemoryRouter>{ui}</MemoryRouter>);
}

describe('PendingItemsAlert', () => {
    it('renders nothing when all pending counts are zero', () => {
        renderWithRouter(
            <PendingItemsAlert pendingItems={{ pendingProducts: 0, pendingOrders: 0 }} isLoading={false} />
        );

        expect(screen.queryByText('待处理事项')).not.toBeInTheDocument();
    });

    it('renders nothing when pendingItems is undefined', () => {
        renderWithRouter(<PendingItemsAlert isLoading={false} />);

        expect(screen.queryByText('待处理事项')).not.toBeInTheDocument();
    });

    it('shows total pending badge and all sections when counts exist', () => {
        renderWithRouter(
            <PendingItemsAlert pendingItems={{ pendingProducts: 10, pendingOrders: 5 }} isLoading={false} />
        );

        expect(screen.getByText('待处理事项')).toBeInTheDocument();
        expect(screen.getByText('15')).toBeInTheDocument();

        expect(screen.getByText('10')).toBeInTheDocument();
        expect(screen.getByText(/个商品待审核/)).toBeInTheDocument();

        expect(screen.getByText('5')).toBeInTheDocument();
        expect(screen.getByText(/笔待处理订单/)).toBeInTheDocument();

        const links = screen.getAllByText('立即处理');
        expect(links).toHaveLength(2);
        expect(links[0].closest('a')).toHaveAttribute('href', '/admin/products?status=0');
        expect(links[1].closest('a')).toHaveAttribute('href', '/admin/orders');
    });

    it('renders only the sections with non-zero counts', () => {
        renderWithRouter(
            <PendingItemsAlert pendingItems={{ pendingProducts: 0, pendingOrders: 7 }} isLoading={false} />
        );

        expect(screen.getAllByText('7')).toHaveLength(2);
        expect(screen.getByText(/笔待处理订单/)).toBeInTheDocument();

        expect(screen.queryByText(/个商品待审核/)).not.toBeInTheDocument();

        expect(screen.getAllByText('立即处理')).toHaveLength(1);
    });

    it('renders loading skeletons when isLoading is true', () => {
        renderWithRouter(<PendingItemsAlert pendingItems={{ pendingProducts: 1, pendingOrders: 1 }} isLoading />);

        expect(screen.getByText('待处理事项')).toBeInTheDocument();
        expect(screen.queryByText('立即处理')).not.toBeInTheDocument();

        const skeletonBars = screen
            .getAllByRole('generic')
            .filter(el => el instanceof HTMLElement && el.style.background === 'rgba(229, 224, 219, 0.35)');
        expect(skeletonBars.length).toBeGreaterThanOrEqual(3);
    });
});
