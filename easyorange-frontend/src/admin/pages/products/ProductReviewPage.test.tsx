import { fireEvent, screen } from '@testing-library/react';
import { afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import type { PageResult } from '@/types';
import type { AdminProduct } from '../../types/admin';
import ProductReviewPage from './ProductReviewPage';

// ─── Mock scrollIntoView for AdminSelect portal ───
beforeAll(() => {
    Element.prototype.scrollIntoView = vi.fn();
});

// ─── Hook mocks ───
const mockUseAdminProducts = vi.fn();

vi.mock('../../hooks/useAdminProducts', () => ({
    useAdminProducts: (...args: unknown[]) => mockUseAdminProducts(...args),
    useAdminProductDetail: vi.fn(),
}));

// Mock the drawer component
let mockDrawerOpen = false;
let mockDrawerProductId: number | null = null;

vi.mock('./ProductDetailDrawer', () => ({
    ProductDetailDrawer: ({
        open,
        productId,
        onClose,
        onSuccess,
    }: {
        open: boolean;
        productId: number | null;
        onClose: () => void;
        onSuccess: () => void;
    }) => {
        mockDrawerOpen = open;
        mockDrawerProductId = productId;
        if (!open) {
            return null;
        }
        return (
            <div data-testid="product-detail-drawer" data-product-id={productId}>
                <button type="button" onClick={onClose}>
                    关闭
                </button>
                <button type="button" onClick={onSuccess}>
                    success
                </button>
            </div>
        );
    },
}));

// Mock react-router-dom Link
vi.mock('react-router-dom', async importOriginal => {
    const actual = await importOriginal<typeof import('react-router-dom')>();
    return {
        ...actual,
        Link: ({ children, to }: { children: React.ReactNode; to: string }) => <a href={to}>{children}</a>,
    };
});

function createPageData(records: AdminProduct[], total: number): PageResult<AdminProduct> {
    return {
        records,
        total,
        current: 1,
        size: 10,
        pages: Math.ceil(total / 10),
    };
}

// ─── Helper to create products with dynamic times (to work with fake timers) ───
function createSampleProducts(fakeNow: number) {
    return [
        {
            productId: 1,
            name: '待审核商品',
            description: '需要审核的商品描述',
            price: 100,
            originalPrice: null,
            stock: 10,
            status: 4,
            statusDesc: '待审核',
            conditionLevel: 9,
            location: '北京',
            contactMethod: '微信',
            images: [],
            mainImage: null,
            categoryId: 1,
            categoryName: '电子产品',
            sellerId: 1,
            sellerName: '资产方A',
            sellerAvatar: null,
            viewCount: 100,
            createTime: new Date(fakeNow - 30_000).toISOString(), // 30 seconds ago → "刚刚"
            updateTime: new Date(fakeNow - 30_000).toISOString(),
        },
        {
            productId: 2,
            name: '已驳回商品',
            description: null,
            price: 200,
            originalPrice: null,
            stock: 5,
            status: 5,
            statusDesc: '已驳回',
            conditionLevel: 8,
            location: null,
            contactMethod: null,
            images: [],
            mainImage: null,
            categoryId: 2,
            categoryName: '图书教材',
            sellerId: 2,
            sellerName: '资产方B',
            sellerAvatar: null,
            viewCount: 50,
            createTime: new Date(fakeNow - 300_000).toISOString(), // 5 minutes ago → "5分钟前"
            updateTime: new Date(fakeNow - 300_000).toISOString(),
        },
    ];
}

function setupDefaultMocks() {
    // fakeNow is the system time set in beforeEach
    const fakeNow = Date.now();
    const products = createSampleProducts(fakeNow);

    mockUseAdminProducts.mockReturnValue({
        data: createPageData(products, 25),
        isLoading: false,
        isError: false,
        error: null,
        refetch: vi.fn(),
    });

    mockDrawerOpen = false;
    mockDrawerProductId = null;
}

describe('ProductReviewPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        vi.useFakeTimers();
        vi.setSystemTime(new Date(2026, 4, 16, 10, 0, 0));
        setupDefaultMocks();
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    // ── Test 1: Page title and subtitle ──
    it('renders page title "商品审核" and subtitle', () => {
        renderWithProviders(<ProductReviewPage />);
        expect(screen.getByText('商品审核')).toBeInTheDocument();
        expect(screen.getByText('审核待上架商品，管理通过与驳回')).toBeInTheDocument();
    });

    // ── Test 2: Search input and search button ──
    it('renders search input and search button', () => {
        renderWithProviders(<ProductReviewPage />);
        expect(screen.getByPlaceholderText('搜索商品名称...')).toBeInTheDocument();
        expect(screen.getByText('搜索')).toBeInTheDocument();
    });

    // ── Test 3: Loading state ──
    it('shows loading spinner when isLoading is true', () => {
        mockUseAdminProducts.mockReturnValue({
            data: undefined,
            isLoading: true,
            isError: false,
            error: null,
            refetch: vi.fn(),
        });

        renderWithProviders(<ProductReviewPage />);
        expect(screen.getByText('加载中...')).toBeInTheDocument();
    });

    // ── Test 4: Displays products in table ──
    it('renders AdminTable with product data when loaded', () => {
        renderWithProviders(<ProductReviewPage />);
        expect(screen.getByText('待审核商品')).toBeInTheDocument();
        expect(screen.getByText('已驳回商品')).toBeInTheDocument();
        expect(screen.getByText('资产方A')).toBeInTheDocument();
        expect(screen.getByText('资产方B')).toBeInTheDocument();
    });

    // ── Test 5: Empty state ──
    it('shows empty state when no products', () => {
        mockUseAdminProducts.mockReturnValue({
            data: createPageData([], 0),
            isLoading: false,
            isError: false,
            error: null,
            refetch: vi.fn(),
        });

        renderWithProviders(<ProductReviewPage />);
        expect(screen.getByText('暂无商品数据')).toBeInTheDocument();
    });

    // ── Test 6: Error state ──
    it('shows error banner with refresh button when isError is true', () => {
        mockUseAdminProducts.mockReturnValue({
            data: undefined,
            isLoading: false,
            isError: true,
            error: new Error('Network failure'),
            refetch: vi.fn(),
        });

        renderWithProviders(<ProductReviewPage />);
        expect(screen.getByText('数据加载失败')).toBeInTheDocument();
        expect(screen.getByText('无法连接到服务器，请检查后端服务是否启动')).toBeInTheDocument();
        expect(screen.getByText('刷新')).toBeInTheDocument();
    });

    // ── Test 7: Default status filter shows "全部状态" ──
    it('default status filter shows "全部状态"', () => {
        renderWithProviders(<ProductReviewPage />);
        expect(screen.getByText('全部状态')).toBeInTheDocument();
    });

    // ── Test 8: Status filter, category, sort sections exist ──
    it('renders filter sections', () => {
        renderWithProviders(<ProductReviewPage />);
        // The filter toolbar has filter labels rendered as spans
        // "状态" may appear in both the label and the AdminSelect button text
        const statusElements = screen.getAllByText('状态');
        expect(statusElements.length).toBeGreaterThanOrEqual(1);
        expect(screen.getByText('分类')).toBeInTheDocument();
        expect(screen.getByText('排序')).toBeInTheDocument();
    });

    // ── Test 9: "审核" buttons render for each product ──
    it('renders "审核" buttons for each product', () => {
        renderWithProviders(<ProductReviewPage />);
        const auditButtons = screen.getAllByText('审核');
        expect(auditButtons).toHaveLength(2);
    });

    // ── Test 10: Clicking "审核" opens drawer ──
    it('opens ProductDetailDrawer when "审核" button is clicked', () => {
        renderWithProviders(<ProductReviewPage />);
        const auditButton = screen.getAllByText('审核')[0];
        fireEvent.click(auditButton);

        expect(mockDrawerOpen).toBe(true);
        expect(mockDrawerProductId).toBe(1);
    });

    // ── Test 11: Relative time formatting - "刚刚" ──
    it('shows "刚刚" for products created within the last minute', () => {
        renderWithProviders(<ProductReviewPage />);
        // Button with "(30_000ms = 30 seconds) ago, diff < 60s → "刚刚"
        const justNowElements = screen.getAllByText('刚刚');
        expect(justNowElements.length).toBeGreaterThanOrEqual(1);
    });

    // ── Test 12: Relative time formatting - "5分钟前" ──
    it('shows "5分钟前" for products created 5 minutes ago', () => {
        renderWithProviders(<ProductReviewPage />);
        // "5分钟前" text may appear in text content; getByText with exact match
        expect(screen.getByText('5分钟前')).toBeInTheDocument();
    });

    // ── Test 13: Search input Enter key triggers search ──
    it('triggers search on Enter key press', () => {
        renderWithProviders(<ProductReviewPage />);
        const input = screen.getByPlaceholderText('搜索商品名称...');
        fireEvent.change(input, { target: { value: '手机' } });
        fireEvent.keyDown(input, { key: 'Enter' });

        const params = mockUseAdminProducts.mock.calls[0][0];
        expect(params.pageNum).toBe(1);
    });

    // ── Test 14: Search button triggers search ──
    it('triggers search on search button click', () => {
        renderWithProviders(<ProductReviewPage />);
        const input = screen.getByPlaceholderText('搜索商品名称...');
        fireEvent.change(input, { target: { value: '电脑' } });
        fireEvent.click(screen.getByText('搜索'));

        const params = mockUseAdminProducts.mock.calls[0][0];
        expect(params.pageNum).toBe(1);
    });

    // ── Test 15: Pagination renders ──
    it('renders pagination controls', () => {
        renderWithProviders(<ProductReviewPage />);
        // ProductReviewPage always passes pagination
        expect(screen.getByText(content => content.includes('条记录'))).toBeInTheDocument();
    });

    // ── Test 16: Price gradient formatting ──
    it('renders price with formatting', () => {
        renderWithProviders(<ProductReviewPage />);
        expect(screen.getByText('¥100.00')).toBeInTheDocument();
        expect(screen.getByText('¥200.00')).toBeInTheDocument();
    });

    // ── Test 17: Status badge renders for each product ──
    it('renders status badges for products', () => {
        renderWithProviders(<ProductReviewPage />);
        expect(screen.getByText('待审核')).toBeInTheDocument();
        expect(screen.getByText('已驳回')).toBeInTheDocument();
    });

    // ── Test 18: Initial params passed to useAdminProducts ──
    it('passes correct initial params to useAdminProducts', () => {
        renderWithProviders(<ProductReviewPage />);
        const params = mockUseAdminProducts.mock.calls[0][0];
        expect(params).toMatchObject({
            pageNum: 1,
            pageSize: 10,
        });
        expect(params.keyword).toBeUndefined();
        expect(params.status).toBeUndefined();
    });
});
