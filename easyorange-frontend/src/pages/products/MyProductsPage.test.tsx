import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import type { Product } from '@/types';
import MyProductsPage from './MyProductsPage';

// ── Hoisted mock functions ──
const mockUseMyProducts = vi.hoisted(() => vi.fn());
const mockNavigate = vi.hoisted(() => vi.fn());

// ── Module mocks ──
vi.mock('@/hooks/product/useProducts', () => ({
    useMyProducts: mockUseMyProducts,
}));

vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom');
    return { ...(actual as object), useNavigate: () => mockNavigate };
});

// ── Helpers ──
function createMockProduct(overrides: Partial<Product> = {}): Product {
    return {
        id: '1',
        title: '测试商品',
        description: '测试描述',
        price: 100,
        originalPrice: 150,
        categoryId: 1,
        categoryName: '电子产品',
        condition: 1,
        conditionLevel: 1,
        status: 'ONLINE',
        images: ['https://example.com/img.jpg'],
        location: '北京',
        views: 50,
        favorites: 10,
        sellerId: 'user1',
        sellerName: '资产方小明',
        sellerAvatar: null,
        sellerRating: 4.5,
        createTime: '2026-05-16 10:00:00',
        updateTime: '2026-05-16 10:00:00',
        ...overrides,
    };
}

function renderPage() {
    return renderWithProviders(<MyProductsPage />, {
        initialRoute: '/my-products',
    });
}

beforeEach(() => {
    vi.clearAllMocks();
});

// ── Tests ──
describe('MyProductsPage', () => {
    it('renders the page title "我发布的商品"', () => {
        mockUseMyProducts.mockReturnValue({
            data: { records: [], total: 0, pages: 1, current: 1, size: 20 },
            isLoading: false,
            isError: false,
            refetch: vi.fn(),
        });

        renderPage();

        expect(screen.getByText('我发布的商品')).toBeInTheDocument();
        expect(screen.getByText('管理你发布的商品，追踪审核状态')).toBeInTheDocument();
    });

    it('shows loading state', () => {
        mockUseMyProducts.mockReturnValue({
            data: undefined,
            isLoading: true,
            isError: false,
            refetch: vi.fn(),
        });

        renderPage();

        expect(screen.getByText('正在加载商品...')).toBeInTheDocument();
    });

    it('shows error state with retry button', async () => {
        const mockRefetch = vi.fn();
        mockUseMyProducts.mockReturnValue({
            data: undefined,
            isLoading: false,
            isError: true,
            error: new Error('加载失败'),
            refetch: mockRefetch,
        });

        renderPage();

        expect(screen.getByText('加载失败，请稍后重试')).toBeInTheDocument();
        const retryBtn = screen.getByText('重新加载');
        expect(retryBtn).toBeInTheDocument();

        await userEvent.click(retryBtn);
        expect(mockRefetch).toHaveBeenCalled();
    });

    it('shows empty state with "还没有提交资产"', () => {
        mockUseMyProducts.mockReturnValue({
            data: { records: [], total: 0, pages: 1, current: 1, size: 20 },
            isLoading: false,
            isError: false,
            refetch: vi.fn(),
        });

        renderPage();

        expect(screen.getByText('还没有提交资产')).toBeInTheDocument();
        // Text is in a <p> with <br/>, so use regex match
        expect(screen.getByText(/开始托管你的第一件资产吧/)).toBeInTheDocument();
    });

    it('renders product list with status badges', () => {
        const products = [
            createMockProduct({ id: '1', title: '商品一', status: 'ONLINE' }),
            createMockProduct({ id: '2', title: '商品二', status: 'PENDING_REVIEW' }),
            createMockProduct({ id: '3', title: '商品三', status: 'SOLD' }),
        ];
        mockUseMyProducts.mockReturnValue({
            data: { records: products, total: 3, pages: 1, current: 1, size: 20 },
            isLoading: false,
            isError: false,
            refetch: vi.fn(),
        });

        renderPage();

        expect(screen.getByText('商品一')).toBeInTheDocument();
        expect(screen.getByText('商品二')).toBeInTheDocument();
        expect(screen.getByText('商品三')).toBeInTheDocument();
        // Status badges ("在售" appears in both tab and badge, so use getAllByText)
        const onSaleElements = screen.getAllByText('在售');
        expect(onSaleElements.length).toBeGreaterThanOrEqual(1);
        expect(screen.getByText('⏳ 审核中')).toBeInTheDocument();
        // "已售出" appears in both tab and badge, so use getAllByText
        const soldElements = screen.getAllByText('已售出');
        expect(soldElements.length).toBeGreaterThanOrEqual(1);
    });

    it('renders status filter tabs', () => {
        mockUseMyProducts.mockReturnValue({
            data: { records: [], total: 0, pages: 1, current: 1, size: 20 },
            isLoading: false,
            isError: false,
            refetch: vi.fn(),
        });

        renderPage();

        expect(screen.getByText('全部')).toBeInTheDocument();
        expect(screen.getByText('在售')).toBeInTheDocument();
        expect(screen.getByText('审核中')).toBeInTheDocument();
        expect(screen.getByText('草稿')).toBeInTheDocument();
        expect(screen.getByText('已驳回')).toBeInTheDocument();
        expect(screen.getByText('已下架')).toBeInTheDocument();
        expect(screen.getByText('已售出')).toBeInTheDocument();
    });

    it('navigates to edit page when edit button is clicked', async () => {
        const products = [createMockProduct({ id: '1', title: '可编辑商品' })];
        mockUseMyProducts.mockReturnValue({
            data: { records: products, total: 1, pages: 1, current: 1, size: 20 },
            isLoading: false,
            isError: false,
            refetch: vi.fn(),
        });

        renderPage();

        const user = userEvent.setup();
        const editBtn = screen.getByText('编辑');
        await user.click(editBtn);
        expect(mockNavigate).toHaveBeenCalledWith('/products/1/edit');
    });

    it('navigates to product detail when view button is clicked', async () => {
        const products = [createMockProduct({ id: '2', title: '可查看商品' })];
        mockUseMyProducts.mockReturnValue({
            data: { records: products, total: 1, pages: 1, current: 1, size: 20 },
            isLoading: false,
            isError: false,
            refetch: vi.fn(),
        });

        renderPage();

        // "查看" button uses <Button asChild><Link to={to}>, so check href instead of navigate mock
        const viewLink = screen.getByRole('link', { name: '查看' });
        expect(viewLink).toHaveAttribute('href', '/products/2');
    });

    it('navigates to publish page when "发布新商品" button is clicked', async () => {
        mockUseMyProducts.mockReturnValue({
            data: { records: [], total: 0, pages: 1, current: 1, size: 20 },
            isLoading: false,
            isError: false,
            refetch: vi.fn(),
        });

        renderPage();

        const user = userEvent.setup();
        const publishBtn = screen.getByText('发布新商品');
        await user.click(publishBtn);
        expect(mockNavigate).toHaveBeenCalledWith('/publish');
    });
});
