import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import SearchPage from './SearchPage';

const mockNavigate = vi.hoisted(() => vi.fn());
const mockUseProductSearch = vi.hoisted(() =>
  vi.fn(() => ({ data: undefined, isLoading: false, isError: false, error: null })),
);
const mockUseSearchSuggestions = vi.hoisted(() => vi.fn(() => ({ data: [] })));
const mockUseHotKeywords = vi.hoisted(() =>
  vi.fn(() => ({ data: [{ keyword: '手机', searchCount: 100 }, { keyword: '电脑', searchCount: 80 }, { keyword: '耳机', searchCount: 60 }] })),
);
const mockUseCategories = vi.hoisted(() =>
  vi.fn(() => ({ data: [{ id: '1', name: '电子数码' }, { id: '2', name: '书籍教材' }] })),
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
  ProductCard: ({ product }) => <div data-testid="product-card">{product.title}</div>,
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
    const mockProducts = [
      { id: 'p1', title: '测试手机', price: 1999, images: [], status: 'ONLINE', condition: 1, createTime: '2026-05-10T10:00:00Z', views: 100, categoryName: '电子数码', location: '北京', sellerName: '卖家', description: 'desc' },
    ];
    mockUseProductSearch.mockReturnValue({
      data: { records: mockProducts, total: 1, size: 20, current: 1, pages: 1 },
      isLoading: false,
      isError: false,
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
      data: undefined,
      isLoading: true,
      isError: false,
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
      data: { records: [], total: 0, size: 20, current: 1, pages: 0 },
      isLoading: false,
      isError: false,
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
});
