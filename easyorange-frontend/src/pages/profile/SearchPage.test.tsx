import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import type { Product } from '@/types';
import type { ProductSearchParams } from '@/types/product';
import SearchPage from './SearchPage';

function getLastSearchParams(): ProductSearchParams | undefined {
    const calls = mockUseProductSearch.mock.calls as unknown as Array<[ProductSearchParams]>;
    return calls.at(-1)?.[0];
}

const mockNavigate = vi.hoisted(() => vi.fn());
const mockUseProductSearch = vi.hoisted(() =>
    vi.fn(() => ({
        products: [] as Product[],
        total: 0 as number,
        facets: [] as Array<{ code: string; count: number }>,
        aiEnhancement: undefined,
        isLoading: false as boolean,
        error: null as Error | null,
    }))
);
const mockUseSearchSuggestions = vi.hoisted(() => vi.fn(() => ({ data: [] })));
const mockUseHotKeywords = vi.hoisted(() =>
    vi.fn(() => ({
        data: [
            { keyword: '手机', searchCount: 100 },
            { keyword: '电脑', searchCount: 80 },
            { keyword: '耳机', searchCount: 60 },
        ],
    }))
);
const mockUseCategories = vi.hoisted(() =>
    vi.fn(() => ({
        data: [
            { id: '1', name: '电子数码' },
            { id: '2', name: '书籍教材' },
        ],
    }))
);

vi.mock('@/hooks', () => ({
    useProductSearch: mockUseProductSearch,
    useSearchSuggestions: mockUseSearchSuggestions,
    useHotKeywords: mockUseHotKeywords,
    useCategories: mockUseCategories,
}));

vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom');
    return { ...(actual as object), useNavigate: () => mockNavigate };
});

vi.mock('@tanstack/react-virtual', () => ({
    useVirtualizer: vi.fn(({ count }) => {
        const items = Array.from({ length: count }, (_, i) => ({ index: i, start: i * 520, size: 520, key: i }));
        return { getVirtualItems: () => items, getTotalSize: () => count * 520 };
    }),
}));

vi.mock('@/components/product/ProductCard', () => ({
    ProductCard: ({ product }: { product: Pick<Product, 'title' | 'price'> }) => (
        <div data-testid="product-card">{product.title}</div>
    ),
}));

function renderPage() {
    return renderWithProviders(<SearchPage />);
}

beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    localStorage.setItem('eo_search_history', JSON.stringify(['手机', '电脑']));
});

describe('SearchPage', () => {
    it('renders search input and initial content', () => {
        renderPage();
        expect(screen.getByPlaceholderText('搜索你想要的商品...')).toBeInTheDocument();
        expect(screen.getByText('搜索')).toBeInTheDocument();
        expect(screen.getByText('热门搜索')).toBeInTheDocument();
        const phones = screen.getAllByText('手机');
        expect(phones.length).toBeGreaterThanOrEqual(1);
        const computers = screen.getAllByText('电脑');
        expect(computers.length).toBeGreaterThanOrEqual(1);
        expect(screen.getByText('耳机')).toBeInTheDocument();
    });

    it('renders categories', () => {
        renderPage();
        expect(screen.getByText('分类浏览')).toBeInTheDocument();
        expect(screen.getByText('电子数码')).toBeInTheDocument();
        expect(screen.getByText('书籍教材')).toBeInTheDocument();
    });

    it('renders search history from localStorage', () => {
        renderPage();
        expect(screen.getByText('最近搜索')).toBeInTheDocument();
        const phones2 = screen.getAllByText('手机');
        expect(phones2.length).toBeGreaterThanOrEqual(1);
        const computers2 = screen.getAllByText('电脑');
        expect(computers2.length).toBeGreaterThanOrEqual(1);
    });

    it('clears search history', async () => {
        renderPage();
        const user = userEvent.setup();
        await user.click(screen.getByText('清空'));
        expect(screen.queryByText('最近搜索')).not.toBeInTheDocument();
    });

    it('submits search keyword', async () => {
        const mockProducts: Product[] = [
            {
                id: 'p1',
                title: '测试手机',
                price: 1999,
                images: [],
                status: 'ONLINE',
                condition: 1,
                createTime: '2026-05-10T10:00:00Z',
                views: 100,
                categoryName: '电子数码',
                location: '北京',
                sellerName: '资产方',
                description: 'desc',
                originalPrice: null,
                categoryId: 1,
                conditionLevel: 1,
                favorites: 0,
                sellerId: 's1',
                sellerAvatar: null,
                sellerRating: 0,
                updateTime: '2026-05-10T10:00:00Z',
            },
        ];
        mockUseProductSearch.mockReturnValue({
            products: mockProducts,
            total: 1,
            facets: [],
            aiEnhancement: undefined,
            isLoading: false,
            error: null,
        });

        renderPage();
        const user = userEvent.setup();
        const input = screen.getByPlaceholderText('搜索你想要的商品...');
        await user.type(input, '手机');
        await user.click(screen.getByText('搜索'));

        expect(await screen.findByDisplayValue('手机')).toBeInTheDocument();
        expect(screen.getByText('搜索结果')).toBeInTheDocument();
        expect(screen.getByTestId('product-card')).toBeInTheDocument();
        expect(screen.getByText('测试手机')).toBeInTheDocument();
        expect(document.querySelector('.search-results-count')).toBeInTheDocument();
    });

    it('shows loading state during search', async () => {
        mockUseProductSearch.mockReturnValue({
            products: [],
            total: 0,
            facets: [],
            aiEnhancement: undefined,
            isLoading: true,
            error: null,
        });

        renderPage();
        const user = userEvent.setup();
        const input = screen.getByPlaceholderText('搜索你想要的商品...');
        await user.type(input, '手机');
        await user.click(screen.getByText('搜索'));

        expect(await screen.findByText('正在搜索中...')).toBeInTheDocument();
    });

    it('shows no results state', async () => {
        mockUseProductSearch.mockReturnValue({
            products: [],
            total: 0,
            facets: [],
            aiEnhancement: undefined,
            isLoading: false,
            error: null,
        });

        renderPage();
        const user = userEvent.setup();
        const input = screen.getByPlaceholderText('搜索你想要的商品...');
        await user.type(input, 'zzz');
        await user.click(screen.getByText('搜索'));

        expect(await screen.findByText('未找到相关商品')).toBeInTheDocument();
        expect(screen.getByText('试试其他关键词，或浏览下面的热门商品')).toBeInTheDocument();
    });

    it('reads keyword from URL on mount and triggers search', () => {
        renderWithProviders(<SearchPage />, { initialRoute: '/search?keyword=耳机' });

        const lastCall = getLastSearchParams();
        expect(lastCall).toBeDefined();
        expect(lastCall?.keyword).toBe('耳机');
        expect(screen.getByDisplayValue('耳机')).toBeInTheDocument();
    });

    it('writes submitted keyword to URL', async () => {
        renderPage();
        const user = userEvent.setup();
        const input = screen.getByPlaceholderText('搜索你想要的商品...');
        await user.type(input, '平板');
        await user.click(screen.getByText('搜索'));

        const lastCall = getLastSearchParams();
        expect(lastCall).toBeDefined();
        expect(lastCall?.keyword).toBe('平板');
    });

    it('toggles AI flag and passes aiEnhanced to search params', async () => {
        renderPage();
        const user = userEvent.setup();
        const aiButton = screen.getByTitle('开启AI智能搜索');
        await user.click(aiButton);

        const lastCall = getLastSearchParams();
        expect(lastCall).toBeDefined();
        expect(lastCall?.aiEnhanced).toBe(true);
    });
});
