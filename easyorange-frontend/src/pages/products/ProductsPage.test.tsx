import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ProductsPage from './ProductsPage';

const mockUseProducts = vi.hoisted(() => vi.fn());
const mockUseCategories = vi.hoisted(() => vi.fn());
const mockUseFavoriteCheck = vi.hoisted(() => vi.fn());
const mockUseColumnCount = vi.hoisted(() => vi.fn());
const mockUseAuthStore = vi.hoisted(() =>
  vi.fn(() => ({ user: null, token: null, isAuthenticated: false })),
);
const mockNavigate = vi.hoisted(() => vi.fn());

const mockUseSemanticSearch = vi.hoisted(() => vi.fn(() => ({
  results: [],
  isSearching: false,
  isSemanticMode: false,
  total: 0,
  search: vi.fn(),
  toggleSemanticMode: vi.fn(),
  setResults: vi.fn(),
})));

vi.mock('@/hooks', () => ({
  useProducts: mockUseProducts,
  useCategories: mockUseCategories,
  useFavoriteCheck: mockUseFavoriteCheck,
  useColumnCount: mockUseColumnCount,
  useSemanticSearch: mockUseSemanticSearch,
}));

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

HTMLCanvasElement.prototype.toDataURL = vi
  .fn()
  .mockImplementation(() => 'data:image/png;base64,test');

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
  mockUseProducts.mockReturnValue({
    data: { records: [], total: 0 },
    isLoading: false,
  });
});

describe('ProductsPage', () => {
  it('renders loading skeleton when initially loading', () => {
    mockUseProducts.mockReturnValue({
      data: undefined,
      isLoading: true,
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
    expect(
      screen.getByText('尝试调整筛选条件或搜索其他关键词'),
    ).toBeInTheDocument();
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
    mockUseProducts.mockReturnValue({
      data: { records: [], total: 42 },
      isLoading: false,
    });

    renderPage();

    expect(screen.getByText('42 件商品')).toBeInTheDocument();
  });

  it('renders result count', () => {
    mockUseProducts.mockReturnValue({
      data: { records: [], total: 42 },
      isLoading: false,
    });

    renderPage();

    const countEl = screen.getByText('件商品');
    expect(countEl).toBeInTheDocument();
  });

  it('shows category filter chip when categoryId is in params', () => {
    mockUseProducts.mockReturnValue({
      data: { records: [], total: 0 },
      isLoading: false,
    });

    renderWithProviders(<ProductsPage />, {
      initialRoute: '/products?category=1',
    });

    const catElements = screen.getAllByText('电子产品');
    expect(catElements.length).toBeGreaterThanOrEqual(1);
  });

  it('clears category filter when X is clicked', async () => {
    mockUseProducts.mockReturnValue({
      data: { records: [], total: 0 },
      isLoading: false,
    });

    renderWithProviders(<ProductsPage />, {
      initialRoute: '/products?category=1',
    });

    const user = userEvent.setup();
    const clearBtn = document.querySelector('.results-category') as HTMLElement;
    expect(clearBtn).toBeInTheDocument();
    await user.click(clearBtn);
  });

  it('shows default sort as newest in SortDropdown trigger', () => {
    renderPage();

    // SortDropdown trigger shows "最新发布" by default
    const newestTexts = screen.getAllByText('最新发布');
    const triggerSpan = newestTexts.find(el => el.closest('button'));
    expect(triggerSpan).toBeTruthy();
  });

  it('opens SortDropdown panel and changes sort on selection', async () => {
    renderPage();

    const user = userEvent.setup();
    // Find the SortDropdown trigger button (contains "最新发布" span inside a button)
    const newestTexts = screen.getAllByText('最新发布');
    const triggerSpan = newestTexts.find(el => el.closest('button')) as HTMLElement;
    const trigger = triggerSpan?.closest('button') as HTMLElement;
    await user.click(trigger);

    // Panel should show all options
    const priceAscTexts = screen.getAllByText('价格从低到高');
    const priceAscOption = priceAscTexts.find(el => el.closest('.sort-dropdown-panel')) as HTMLElement;
    expect(priceAscOption).toBeInTheDocument();

    // Click price_asc option in the panel
    await user.click(priceAscOption);

    // Trigger should update to reflect new value
    expect(trigger).toHaveTextContent('价格从低到高');
  });
});
