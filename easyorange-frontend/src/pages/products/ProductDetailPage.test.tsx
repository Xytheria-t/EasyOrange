import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import type { Product, User } from '@/types';
import ProductDetailPage from './ProductDetailPage';

/** Helper to create a mock User with sensible defaults (overridable per test). */
function createMockUser(overrides: Partial<User> = {}): User {
    return {
        userId: 'currentUser',
        username: 'testuser',
        nickname: '测试用户',
        email: 'test@example.com',
        phone: null,
        studentId: null,
        realName: null,
        avatar: null,
        status: 1,
        userType: '01',
        createTime: '2026-01-01T00:00:00Z',
        updateTime: '2026-01-01T00:00:00Z',
        ...overrides,
    };
}

// ── Hoisted mock functions for per-test control ──
const mockUseProduct = vi.hoisted(() => vi.fn());
const mockUseSimilarProducts = vi.hoisted(() => vi.fn());
const mockUseCreateOrder = vi.hoisted(() =>
    vi.fn(() => ({ mutateAsync: vi.fn().mockResolvedValue('order1'), isPending: false }))
);
const mockUseAuthStore = vi.hoisted(() =>
    vi.fn(() => ({
        user: null as User | null,
        token: null as string | null,
        isAuthenticated: false as boolean,
    }))
);
const mockUseUIStore = vi.hoisted(() =>
    vi.fn((selector?: (s: { addToast: ReturnType<typeof vi.fn> }) => unknown) => {
        const state = { addToast: vi.fn() };
        return selector ? selector(state) : state;
    })
);
const mockNavigate = vi.hoisted(() => vi.fn());

// jsdom does not implement HTMLCanvasElement.toDataURL('image/webp')
HTMLCanvasElement.prototype.toDataURL = vi.fn().mockImplementation(() => 'data:image/png;base64,test');

// jsdom does not implement Element.scrollIntoView
Element.prototype.scrollIntoView = vi.fn();

// ── Module mocks ──
vi.mock('@/hooks', () => ({
    useProduct: mockUseProduct,
    useSimilarProducts: mockUseSimilarProducts,
    useCreateOrder: mockUseCreateOrder,
}));

vi.mock('@/store/authStore', () => ({
    useAuthStore: mockUseAuthStore,
}));

vi.mock('@/store/uiStore', () => ({
    useUIStore: mockUseUIStore,
}));

vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom');
    return { ...(actual as object), useNavigate: () => mockNavigate };
});

vi.mock('@/api/favoriteApi', () => ({
    favoriteApi: {
        check: vi.fn().mockResolvedValue({ data: false }),
        add: vi.fn().mockResolvedValue({}),
        remove: vi.fn().mockResolvedValue({}),
    },
}));

vi.mock('@/api/reviewApi', () => ({
    reviewApi: {
        getList: vi.fn().mockResolvedValue({
            data: { records: [], total: 0, pages: 0, current: 1, size: 10 },
        }),
        create: vi.fn().mockResolvedValue({}),
    },
}));

vi.mock('@/api/productApi', () => ({
    productApi: {
        incrementView: vi.fn().mockResolvedValue({}),
        submitForReview: vi.fn().mockResolvedValue({}),
    },
}));

vi.mock('@/api/messageApi', () => ({
    messageApi: {
        getConversation: vi.fn().mockResolvedValue({ data: [] }),
        sendMessage: vi.fn().mockResolvedValue({}),
    },
}));

// ── Helpers ──
function createMockProduct(overrides: Partial<Product> = {}): Product {
    return {
        id: '123',
        title: '测试商品名称',
        description: '这是一个测试商品的详细描述',
        price: 99.99,
        originalPrice: 150.0,
        categoryId: 1,
        categoryName: '电子产品',
        condition: 1,
        conditionLevel: 1,
        status: 'ONLINE',
        images: ['https://example.com/image.jpg'],
        location: '北京海淀',
        views: 100,
        favorites: 20,
        sellerId: 'seller1',
        sellerName: '资产方张三',
        sellerAvatar: null,
        sellerRating: 4.5,
        createTime: '2026-05-01T10:00:00Z',
        updateTime: '2026-05-01T10:00:00Z',
        ...overrides,
    };
}

function renderPage() {
    return renderWithProviders(<ProductDetailPage />, {
        initialRoute: '/products/123',
    });
}

beforeEach(() => {
    vi.clearAllMocks();
    // jsdom doesn't support canvas.toDataURL('image/webp') — mock it to prevent Image component errors
    HTMLCanvasElement.prototype.toDataURL = vi.fn(() => 'data:image/png;base64,test');
    // Default mock implementations for all hoisted mocks (per-test overrides work via mockReturnValue)
    mockUseProduct.mockReturnValue({
        data: undefined,
        isLoading: false,
        isError: false,
        error: null,
    });
    mockUseSimilarProducts.mockReturnValue({ data: [], isLoading: false });
    mockUseCreateOrder.mockReturnValue({
        mutateAsync: vi.fn().mockResolvedValue('order1'),
        isPending: false,
    });
    mockUseAuthStore.mockImplementation(() => ({
        user: null,
        token: null,
        isAuthenticated: false,
    }));
    mockUseUIStore.mockImplementation((selector?: (s: { addToast: ReturnType<typeof vi.fn> }) => unknown) => {
        const state = { addToast: vi.fn() };
        return selector ? selector(state) : state;
    });
});

// ── Tests ──
describe('ProductDetailPage', () => {
    it('renders loading skeleton when product is loading', () => {
        mockUseProduct.mockReturnValue({ data: undefined, isLoading: true });
        mockUseSimilarProducts.mockReturnValue({ data: [], isLoading: false });

        renderPage();

        expect(screen.getByText('加载商品详情...')).toBeInTheDocument();
    });

    it('renders "商品不存在" when product is null', () => {
        mockUseProduct.mockReturnValue({ data: undefined, isLoading: false });
        mockUseSimilarProducts.mockReturnValue({ data: [], isLoading: false });

        renderPage();

        expect(screen.getByText('商品不存在')).toBeInTheDocument();
        expect(screen.getByText('该商品可能已下架或被删除')).toBeInTheDocument();
    });

    it('renders product info including name, price, description, category, condition', () => {
        const product = createMockProduct();
        mockUseProduct.mockReturnValue({ data: product, isLoading: false });
        mockUseSimilarProducts.mockReturnValue({ data: [], isLoading: false });
        mockUseAuthStore.mockReturnValue({
            user: createMockUser({ userId: 'currentUser' }),
            token: 'mock-token',
            isAuthenticated: true,
        });

        renderPage();

        // Title appears in h1 (also in breadcrumb, so use getAllByText)
        const titleElements = screen.getAllByText('测试商品名称');
        expect(titleElements.length).toBeGreaterThanOrEqual(1);
        // Price (¥99.99)
        expect(screen.getByText('¥99.99')).toBeInTheDocument();
        // Description
        expect(screen.getByText('这是一个测试商品的详细描述')).toBeInTheDocument();
        // Category (appears in breadcrumb and category chip)
        const categoryElements = screen.getAllByText('电子产品');
        expect(categoryElements.length).toBeGreaterThanOrEqual(1);
        // Seller
        expect(screen.getByText('资产方张三')).toBeInTheDocument();
    });

    it('shows "在售" status badge for ONLINE product', () => {
        const product = createMockProduct({ status: 'ONLINE' });
        mockUseProduct.mockReturnValue({ data: product, isLoading: false });
        mockUseSimilarProducts.mockReturnValue({ data: [], isLoading: false });
        mockUseAuthStore.mockReturnValue({
            user: createMockUser({ userId: 'currentUser' }),
            token: 'mock-token',
            isAuthenticated: true,
        });

        renderPage();

        expect(screen.getByText('在售')).toBeInTheDocument();
    });

    it('shows "已下架" badge when product is OFFLINE', () => {
        const product = createMockProduct({ status: 'OFFLINE' });
        mockUseProduct.mockReturnValue({ data: product, isLoading: false });
        mockUseSimilarProducts.mockReturnValue({ data: [], isLoading: false });
        mockUseAuthStore.mockReturnValue({
            user: createMockUser({ userId: 'currentUser' }),
            token: 'mock-token',
            isAuthenticated: true,
        });

        renderPage();

        expect(screen.getByText('已下架')).toBeInTheDocument();
    });

    it('shows pending review status for PENDING_REVIEW product', () => {
        const product = createMockProduct({ status: 'PENDING_REVIEW' });
        mockUseProduct.mockReturnValue({ data: product, isLoading: false });
        mockUseSimilarProducts.mockReturnValue({ data: [], isLoading: false });
        mockUseAuthStore.mockReturnValue({
            user: createMockUser({ userId: 'currentUser' }),
            token: 'mock-token',
            isAuthenticated: true,
        });

        renderPage();

        expect(screen.getByText('⏳ 审核中')).toBeInTheDocument();
    });

    it('shows sold overlay and disabled buy button for SOLD product', () => {
        const product = createMockProduct({ status: 'SOLD' });
        mockUseProduct.mockReturnValue({ data: product, isLoading: false });
        mockUseSimilarProducts.mockReturnValue({ data: [], isLoading: false });
        mockUseAuthStore.mockReturnValue({
            user: createMockUser({ userId: 'currentUser' }),
            token: 'mock-token',
            isAuthenticated: true,
        });

        renderPage();

        // Sold badge on image overlay
        const soldElements = screen.getAllByText('已售出');
        expect(soldElements.length).toBeGreaterThanOrEqual(2); // overlay badge + status chip + button
        // Buy button is disabled and shows "已售出"
        const buyBtn = screen.getByRole('button', { name: /已售出/i });
        expect(buyBtn).toBeDisabled();
    });

    it('shows edit button and no contact button when user is the owner', () => {
        const product = createMockProduct({ sellerId: 'currentUser' });
        mockUseProduct.mockReturnValue({ data: product, isLoading: false });
        mockUseSimilarProducts.mockReturnValue({ data: [], isLoading: false });
        mockUseAuthStore.mockReturnValue({
            user: createMockUser({ userId: 'currentUser' }),
            token: 'mock-token',
            isAuthenticated: true,
        });

        renderPage();

        // Owner sees "编辑商品" button
        expect(screen.getByText('编辑商品')).toBeInTheDocument();
        // Owner does NOT see "联系资产方" in the action buttons area
        expect(screen.queryByText('联系资产方')).not.toBeInTheDocument();
    });

    it('shows "修改并重新提交" button for owner when product is REJECTED', () => {
        const product = createMockProduct({
            sellerId: 'currentUser',
            status: 'REJECTED',
        });
        mockUseProduct.mockReturnValue({ data: product, isLoading: false });
        mockUseSimilarProducts.mockReturnValue({ data: [], isLoading: false });
        mockUseAuthStore.mockReturnValue({
            user: createMockUser({ userId: 'currentUser' }),
            token: 'mock-token',
            isAuthenticated: true,
        });

        renderPage();

        expect(screen.getByText('修改并重新提交')).toBeInTheDocument();
    });

    it('shows contact seller button when user is not the owner', () => {
        const product = createMockProduct({ sellerId: 'seller1' });
        mockUseProduct.mockReturnValue({ data: product, isLoading: false });
        mockUseSimilarProducts.mockReturnValue({ data: [], isLoading: false });
        mockUseAuthStore.mockReturnValue({
            user: createMockUser({ userId: 'otherUser' }),
            token: 'mock-token',
            isAuthenticated: true,
        });

        renderPage();

        // Buyer sees "联系资产方" section
        const contactBtns = screen.getAllByText('联系资产方');
        expect(contactBtns.length).toBeGreaterThanOrEqual(1);
    });

    it('navigates to login when favorite is clicked without a token', async () => {
        const product = createMockProduct({ sellerId: 'seller1' });
        mockUseProduct.mockReturnValue({ data: product, isLoading: false });
        mockUseAuthStore.mockReturnValue({ user: null, token: null, isAuthenticated: false });

        const { container } = renderPage();
        const user = userEvent.setup();
        // The heart favorite toggle button (first .pdp-action-fab, has no accessible name)
        const heartBtn = container.querySelector('.pdp-action-fab') as HTMLElement;
        expect(heartBtn).not.toBeNull();
        await user.click(heartBtn);
        expect(mockNavigate).toHaveBeenCalledWith(expect.stringContaining('/login?redirect='));
    });

    it('navigates to messages when contact seller is clicked while logged in', async () => {
        const product = createMockProduct({ sellerId: 'seller1' });
        mockUseProduct.mockReturnValue({ data: product, isLoading: false });
        mockUseSimilarProducts.mockReturnValue({ data: [], isLoading: false });
        mockUseAuthStore.mockReturnValue({
            user: createMockUser({ userId: 'otherUser' }),
            token: 'mock-token',
            isAuthenticated: true,
        });

        renderPage();

        const user = userEvent.setup();
        // Click the "联系" button in the seller detail row
        const contactBtn = screen.getByRole('button', { name: '联系' });
        await user.click(contactBtn);
        expect(mockNavigate).toHaveBeenCalledWith('/messages/seller1');
    });

    it('navigates back when "返回" button is clicked', async () => {
        const product = createMockProduct();
        mockUseProduct.mockReturnValue({ data: product, isLoading: false });
        mockUseSimilarProducts.mockReturnValue({ data: [], isLoading: false });
        mockUseAuthStore.mockReturnValue({
            user: createMockUser({ userId: 'currentUser' }),
            token: 'mock-token',
            isAuthenticated: true,
        });

        renderPage();

        const user = userEvent.setup();
        const backBtn = screen.getByText('返回');
        await user.click(backBtn);
        expect(mockNavigate).toHaveBeenCalledWith(-1);
    });

    it('shows "暂无评价" when there are no reviews', () => {
        const product = createMockProduct();
        mockUseProduct.mockReturnValue({ data: product, isLoading: false });
        mockUseSimilarProducts.mockReturnValue({ data: [], isLoading: false });
        mockUseAuthStore.mockReturnValue({
            user: createMockUser({ userId: 'currentUser' }),
            token: 'mock-token',
            isAuthenticated: true,
        });

        renderPage();

        expect(screen.getByText('暂无评价')).toBeInTheDocument();
    });

    it('shows similar products section when similar products exist', () => {
        const product = createMockProduct();
        const similarProduct = createMockProduct({
            id: '456',
            title: '相似商品',
            price: 80,
        });
        mockUseProduct.mockReturnValue({ data: product, isLoading: false });
        mockUseSimilarProducts.mockReturnValue({
            data: [similarProduct],
            isLoading: false,
        });
        mockUseAuthStore.mockReturnValue({
            user: createMockUser({ userId: 'currentUser' }),
            token: 'mock-token',
            isAuthenticated: true,
        });

        renderPage();

        expect(screen.getByText('相似商品')).toBeInTheDocument();
        expect(screen.getByText('¥80')).toBeInTheDocument();
        expect(screen.getByText('查看更多相似商品')).toBeInTheDocument();
    });

    it('shows "暂无相似商品推荐" when similar products is empty', () => {
        const product = createMockProduct();
        mockUseProduct.mockReturnValue({ data: product, isLoading: false });
        mockUseSimilarProducts.mockReturnValue({ data: [], isLoading: false });
        mockUseAuthStore.mockReturnValue({
            user: createMockUser({ userId: 'currentUser' }),
            token: 'mock-token',
            isAuthenticated: true,
        });

        renderPage();

        expect(screen.getByText('暂无相似商品推荐')).toBeInTheDocument();
    });
});
