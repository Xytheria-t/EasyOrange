import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { HttpResponse, http } from 'msw';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { server } from '@/testUtils/mocks/server';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import { FilterSidebar } from './FilterSidebar';

const mockCategories = [
    { id: '1', name: '电子产品', productCount: 10 },
    { id: '2', name: '书籍', productCount: 5 },
];

afterEach(() => {
    server.resetHandlers();
});

describe('FilterSidebar', () => {
    it('renders close button and overlay when open', () => {
        server.use(
            http.get('/api/products/categories', () => {
                return HttpResponse.json({
                    code: 'A0000',
                    message: 'success',
                    data: mockCategories,
                    timestamp: Date.now(),
                });
            })
        );

        renderWithProviders(<FilterSidebar isOpen={true} onClose={vi.fn()} />);

        // Both the overlay and close button share the label, but the overlay is a div
        const closeButtons = screen.getAllByLabelText('关闭筛选面板');
        expect(closeButtons.length).toBe(2);
        expect(screen.getByText('筛选条件')).toBeInTheDocument();
    });

    it('does not render overlay when closed', () => {
        const { container } = renderWithProviders(<FilterSidebar isOpen={false} onClose={vi.fn()} />);

        expect(container.querySelector('.filter-overlay')).toBeNull();
    });

    it('calls onClose when overlay is clicked', async () => {
        const onClose = vi.fn();

        server.use(
            http.get('/api/products/categories', () => {
                return HttpResponse.json({
                    code: 'A0000',
                    message: 'success',
                    data: mockCategories,
                    timestamp: Date.now(),
                });
            })
        );

        renderWithProviders(<FilterSidebar isOpen={true} onClose={onClose} />);

        // Click the overlay (the first element with the label)
        const overlays = screen.getAllByLabelText('关闭筛选面板');
        await userEvent.click(overlays[0]);

        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('resets filters when reset button is clicked', async () => {
        const onReset = vi.fn();

        server.use(
            http.get('/api/products/categories', () => {
                return HttpResponse.json({
                    code: 'A0000',
                    message: 'success',
                    data: mockCategories,
                    timestamp: Date.now(),
                });
            })
        );

        renderWithProviders(<FilterSidebar isOpen={true} onClose={vi.fn()} onResetFilters={onReset} />);

        screen.getByText('重置').click();
        expect(onReset).toHaveBeenCalledTimes(1);
    });

    it('applies filters with correct values on submit', async () => {
        const onApply = vi.fn();
        const onClose = vi.fn();

        server.use(
            http.get('/api/products/categories', () => {
                return HttpResponse.json({
                    code: 'A0000',
                    message: 'success',
                    data: mockCategories,
                    timestamp: Date.now(),
                });
            })
        );

        renderWithProviders(<FilterSidebar isOpen={true} onClose={onClose} onApplyFilters={onApply} />);

        // Submit with default values
        screen.getByText('应用筛选').click();

        expect(onApply).toHaveBeenCalledWith({
            categories: [],
            conditions: [],
            priceMin: undefined,
            priceMax: undefined,
        });
        expect(onClose).toHaveBeenCalled();
    });
});
