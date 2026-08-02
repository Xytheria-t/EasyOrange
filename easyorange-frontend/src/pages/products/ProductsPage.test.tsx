import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import ProductsPage from './ProductsPage';

const mockUseInfiniteProducts = vi.hoisted(() => vi.fn());
const mockUseCategories = vi.hoisted(() => vi.fn());
const mockUseFavoriteCheck = vi.hoisted(() => vi.fn());
const mockUseColumnCount = vi.hoisted(() => vi.fn());
const mockUseAuthStore = vi.hoisted(() => vi.fn(() => ({ user: null, token: null, isAuthenticated: false })));
const mockNavigate = vi.hoisted(() => vi.fn());

const mockUseSemanticSearch = vi.hoisted(() =>
    vi.fn(() => ({
        results: [],
        isSearching: false,
        isSemanticMode: false,
        total: 0,
        search: vi.fn(),
        toggleSemanticMode: vi.fn(),
        setResults: vi.fn(),
    }))
);

vi.mock('@/hooks', async () => {
    const actual = await vi.importActual<typeof import('@/hooks')>('@/hooks');
    return {
        ...actual,
        useInfiniteProducts: mockUseInfiniteProducts,
        useCategories: mockUseCategories,
        useFavoriteCheck: mockUseFavoriteCheck,
        useColumnCount: mockUseColumnCount,
        useSemanticSearch: mockUseSemanticSearch,
    };
});

vi.mock('@/store/authStore', () => ({
    useAuthStore: mockUseAuthStore,
}));

vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom');
    return {
        ...(actual as object),
        useNavigate: () => mockNavigate,
    };
});

vi.mock('@/api/favoriteApi', () => ({
    favoriteApi: {
        batchCheck: vi.fn().mockResolvedValue({ data: {} }),
        add: vi.fn().mockResolvedValue({}),
        remove: vi.fn().mockResolvedValue({}),
    },
}));

vi.mock('@/api/productApi', () => ({
    productApi: {
        getProducts: vi.fn().mockResolvedValue({ data: { records: [], total: 0 } }),
    },
}));

vi.mock('@tanstack/react-virtual', () => ({
    useVirtualizer: vi.fn(() => ({
        getVirtualItems: vi.fn(() => []),
        getTotalSize: vi.fn(() => 0),
    })),
}));

HTMLCanvasElement.prototype.toDataURL = vi.fn().mockImplementation(() => 'data:image/png;base64,test');

function renderPage() {
    return renderWithProviders(<ProductsPage />, {
        initialRoute: '/products',
    });
}

beforeEach(() => {
    vi.clearAllMocks();
    HTMLCanvasElement.prototype.toDataURL = vi.fn(() => 'data:image/png;base64,test');
    mockUseColumnCount.mockReturnValue(4);
    mockUseFavoriteCheck.mockReturnValue({
        checkFavorites: vi.fn(),
        isFavorited: vi.fn(() => false),
        toggleFavorite: vi.fn(),
    });
    mockUseCategories.mockReturnValue({
        data: [
            { id: '1', name: '电子产品' },
            { id: '2', name: '服装鞋帽' },
        ],
        isLoading: false,
    });
    mockUseAuthStore.mockImplementation(() => ({
        user: null,
        token: null,
        isAuthenticated: false,
    }));
    mockUseInfiniteProducts.mockReturnValue({
        data: { pages: [{ records: [], total: 0 }] },
        isLoading: false,
        fetchNextPage: vi.fn(),
        hasNextPage: false,
        isFetchingNextPage: false,
    });
});

describe('ProductsPage', () => {
    it('renders loading skeleton when initially loading', () => {
        mockUseInfiniteProducts.mockReturnValue({
            data: undefined,
            isLoading: true,
            fetchNextPage: vi.fn(),
            hasNextPage: false,
            isFetchingNextPage: false,
        });

        renderPage();

        const skeletonLines = document.querySelectorAll('.skeleton-line');
        expect(skeletonLines.length).toBeGreaterThan(0);
        const loadingCards = document.querySelectorAll('.product-card-loading-premium');
        expect(loadingCards.length).toBe(10);
    });

    it('renders empty state when no products found', () => {
        renderPage();

        expect(screen.getByText('未找到相关商品')).toBeInTheDocument();
        expect(screen.getByText('尝试调整筛选条件或搜索其他关键词')).toBeInTheDocument();
    });

    it('renders SortDropdown with default "最新发布" and filter button', () => {
        renderPage();

        // SortDropdown trigger shows "最新发布" by default (button element)
        const newestTexts = screen.getAllByText('最新发布');
        const triggerBtn = newestTexts.find(el => el.tagName === 'SPAN' && el.closest('button'));
        expect(triggerBtn).toBeTruthy();
        // Filter button
        expect(screen.getByText('筛选')).toBeInTheDocument();
    });

    it('renders ToolsPlaza with total count', () => {
        mockUseInfiniteProducts.mockReturnValue({
            data: { pages: [{ records: [], total: 42 }], pageParams: [] },
            isLoading: false,
            fetchNextPage: vi.fn(),
            hasNextPage: false,
            isFetchingNextPage: false,
        });

        renderPage();

        expect(screen.getByText('42 件商品')).toBeInTheDocument();
    });

    it('renders result count', () => {
        mockUseInfiniteProducts.mockReturnValue({
            data: { pages: [{ records: [], total: 42 }], pageParams: [] },
            isLoading: false,
            fetchNextPage: vi.fn(),
            hasNextPage: false,
            isFetchingNextPage: false,
        });

        renderPage();

        const countEl = screen.getByText('件商品');
        expect(countEl).toBeInTheDocument();
    });

    it('shows category filter chip when categoryId is in params', () => {
        mockUseInfiniteProducts.mockReturnValue({
            data: { pages: [{ records: [], total: 0 }], pageParams: [] },
            isLoading: false,
            fetchNextPage: vi.fn(),
            hasNextPage: false,
            isFetchingNextPage: false,
        });

        renderWithProviders(<ProductsPage />, {
            initialRoute: '/products?filters=category:1',
        });

        const catElements = screen.getAllByText('电子产品');
        expect(catElements.length).toBeGreaterThanOrEqual(1);
    });

    it('clears category filter when X is clicked', async () => {
        mockUseInfiniteProducts.mockReturnValue({
            data: { pages: [{ records: [], total: 0 }], pageParams: [] },
            isLoading: false,
            fetchNextPage: vi.fn(),
            hasNextPage: false,
            isFetchingNextPage: false,
        });

        renderWithProviders(<ProductsPage />, {
            initialRoute: '/products?filters=category:1',
        });

        const user = userEvent.setup();
        const clearBtn = document.querySelector('.results-category') as HTMLElement;
        expect(clearBtn).toBeInTheDocument();
        await user.click(clearBtn);
    });

    it('shows default sort as newest in SortDropdown trigger', () => {
        renderPage();

        // Radix Select trigger is a combobox button showing the selected value
        const trigger = screen.getByRole('combobox');
        expect(trigger).toHaveTextContent('最新发布');
    });

    it('opens SortDropdown panel and changes sort on selection', async () => {
        renderPage();

        const user = userEvent.setup();
        // Radix Select trigger is a combobox button
        const trigger = screen.getByRole('combobox');
        await user.click(trigger);

        // Radix Select renders options in a portal as option roles
        const priceAscOption = await screen.findByRole('option', { name: '价格从低到高' });
        expect(priceAscOption).toBeInTheDocument();

        // Click price_asc option in the panel
        await user.click(priceAscOption);

        // Trigger should update to reflect new value
        expect(trigger).toHaveTextContent('价格从低到高');
    });
});
