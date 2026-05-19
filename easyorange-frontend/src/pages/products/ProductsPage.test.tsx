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

  it('renders sort options and filter button', () => {
    renderPage();

    expect(screen.getAllByText('最新发布').length).toBeGreaterThanOrEqual(1);
    const sortButtons = document.querySelectorAll('.view-options .view-btn');
    expect(sortButtons.length).toBe(4);
    expect(sortButtons[0]).toHaveTextContent('最新发布');
    expect(sortButtons[1]).toHaveTextContent('价格从低到高');
    expect(sortButtons[2]).toHaveTextContent('价格从高到低');
    expect(sortButtons[3]).toHaveTextContent('最受欢迎');
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

  it('shows active sort class on the default sort button', () => {
    renderPage();

    const activeBtn = document.querySelector('.view-btn.active');
    expect(activeBtn).toBeInTheDocument();
    expect(activeBtn).toHaveTextContent('最新发布');
  });

  it('changes active sort when a sort option is clicked', async () => {
    renderPage();

    const user = userEvent.setup();
    const sortButtons = document.querySelectorAll('.view-options .view-btn');
    expect(sortButtons.length).toBe(4);

    const priceAscBtn = sortButtons[1];
    await user.click(priceAscBtn);

    const activeBtn = document.querySelector('.view-btn.active');
    expect(activeBtn).toHaveTextContent('价格从低到高');
  });
});
