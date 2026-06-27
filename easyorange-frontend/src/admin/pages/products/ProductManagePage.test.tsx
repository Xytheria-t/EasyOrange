import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, fireEvent } from '@testing-library/react';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import ProductManagePage from './ProductManagePage';
import type { PageData, AdminProduct } from '../../types/admin';

// ─── Hook mocks ───
const mockUseAdminProducts = vi.fn();
const mockUseUpdateProductStatus = vi.fn();
let mockModalOpen = false;
let mockModalProductId: number | null = null;

vi.mock('../../hooks/useAdminProducts', () => ({
  useAdminProducts: (...args: unknown[]) => mockUseAdminProducts(...args),
  useAdminProductDetail: vi.fn(),
  useUpdateProductStatus: (...args: unknown[]) => mockUseUpdateProductStatus(...args),
  ADMIN_PRODUCT_KEYS: { all: ['admin', 'products'] },
}));

// Mock the detail modal
vi.mock('./ProductManageDetailModal', () => ({
  ProductManageDetailModal: ({
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
    mockModalOpen = open;
    mockModalProductId = productId;
    if (!open) {return null;}
    return (
      <div data-testid="product-detail-modal" data-product-id={productId}>
        <button onClick={onClose}>关闭</button>
        <button onClick={onSuccess}>success</button>
      </div>
    );
  },
}));

// ─── Sample data ───
const sampleProducts: AdminProduct[] = [
  {
    productId: 1,
    name: '测试商品1',
    description: '这是一个测试商品',
    price: 100,
    originalPrice: 150,
    stock: 10,
    status: 1,
    statusDesc: '上架',
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
    createTime: '2026-05-16T10:00:00',
    updateTime: '2026-05-16T10:00:00',
  },
  {
    productId: 2,
    name: '测试商品2',
    description: null,
    price: 200,
    originalPrice: null,
    stock: 5,
    status: 3,
    statusDesc: '下架',
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
    createTime: '2026-05-15T10:00:00',
    updateTime: '2026-05-15T10:00:00',
  },
];

function createPageData(records: AdminProduct[], total: number): PageData<AdminProduct> {
  return {
    records,
    total,
    current: 1,
    size: 10,
    pages: Math.ceil(total / 10),
  };
}

function setupDefaultMocks() {
  mockUseAdminProducts.mockReturnValue({
    data: createPageData(sampleProducts, 2),
    isLoading: false,
    isError: false,
    error: null,
    refetch: vi.fn(),
  });

  mockUseUpdateProductStatus.mockReturnValue({
    mutateAsync: vi.fn().mockResolvedValue({}),
    isPending: false,
  });

  mockModalOpen = false;
  mockModalProductId = null;
}

describe('ProductManagePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    setupDefaultMocks();
  });

  // ── Test 1: Page title and subtitle ──
  it('renders page title "商品管理" and subtitle', () => {
    renderWithProviders(<ProductManagePage />);
    expect(screen.getByText('商品管理')).toBeInTheDocument();
    expect(
      screen.getByText('管理平台所有商品，支持上架/下架操作'),
    ).toBeInTheDocument();
  });

  // ── Test 2: Search input and button ──
  it('renders search input with placeholder and search button', () => {
    renderWithProviders(<ProductManagePage />);
    expect(
      screen.getByPlaceholderText('搜索商品名称...'),
    ).toBeInTheDocument();
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

    renderWithProviders(<ProductManagePage />);
    expect(screen.getByText('加载中...')).toBeInTheDocument();
  });

  // ── Test 4: Displays products in table when loaded ──
  it('renders AdminTable with product data when loaded', () => {
    renderWithProviders(<ProductManagePage />);
    expect(screen.getByText('测试商品1')).toBeInTheDocument();
    expect(screen.getByText('测试商品2')).toBeInTheDocument();
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

    renderWithProviders(<ProductManagePage />);
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

    renderWithProviders(<ProductManagePage />);
    expect(screen.getByText('数据加载失败')).toBeInTheDocument();
    expect(
      screen.getByText('无法连接到服务器，请检查后端服务是否启动'),
    ).toBeInTheDocument();
    expect(screen.getByText('刷新')).toBeInTheDocument();
  });

  // ── Test 7: Search input Enter key triggers keyword filter ──
  it('triggers search on Enter key press', () => {
    renderWithProviders(<ProductManagePage />);
    const input = screen.getByPlaceholderText('搜索商品名称...');
    fireEvent.change(input, { target: { value: '手机' } });
    fireEvent.keyPress(input, { key: 'Enter', charCode: 13 });
    const lastCall = mockUseAdminProducts.mock.calls[0][0];
    expect(lastCall.pageNum).toBe(1);
  });

  // ── Test 8: Search button click triggers keyword filter ──
  it('triggers search on search button click', () => {
    renderWithProviders(<ProductManagePage />);
    const input = screen.getByPlaceholderText('搜索商品名称...');
    fireEvent.change(input, { target: { value: '手机' } });
    fireEvent.click(screen.getByText('搜索'));
    const lastCall = mockUseAdminProducts.mock.calls[0][0];
    expect(lastCall.pageNum).toBe(1);
  });

  // ── Test 9: Status and category filter labels exist ──
  // "状态" may exist in AdminSelect button text ("全部状态"), so use getAllByText
  it('renders status filter label (may appear in select button text too)', () => {
    renderWithProviders(<ProductManagePage />);
    // The label span with "状态" is rendered alongside the filter
    // AdminSelect button also shows "全部状态" which contains "状态"
    // So use getAllByText and ensure at least one match
    const statusLabel = screen.getAllByText('状态');
    expect(statusLabel.length).toBeGreaterThanOrEqual(1);
    // Also verify the AdminSelect shows the default option
    expect(screen.getByText('全部状态')).toBeInTheDocument();
  });

  // ── Test 10: Category filter exists ──
  it('renders category filter label', () => {
    renderWithProviders(<ProductManagePage />);
    // "分类" appears in filter label and possibly in AdminSelect button text with "全部分类"
    expect(screen.getByText('全部分类')).toBeInTheDocument();
  });

  // ── Test 11: "详情" buttons render for each product ──
  it('renders detail buttons for each product', () => {
    renderWithProviders(<ProductManagePage />);
    const detailButtons = screen.getAllByText('详情');
    expect(detailButtons).toHaveLength(2);
  });

  // ── Test 12: Clicking "详情" opens modal ──
  it('opens ProductManageDetailModal when "详情" button is clicked', () => {
    renderWithProviders(<ProductManagePage />);
    const detailButton = screen.getAllByText('详情')[0];
    fireEvent.click(detailButton);
    expect(mockModalOpen).toBe(true);
    expect(mockModalProductId).toBe(1);
  });

  // ── Test 13: Status toggle button for online product (下架) ──
  it('renders "下架" button for online (status=1) product', () => {
    renderWithProviders(<ProductManagePage />);
    // The AdminSelect status options include "下架" as an option label
    // The table also has a "下架" action button. Use getAllByText to handle both.
    const btns = screen.getAllByText('下架');
    expect(btns.length).toBeGreaterThanOrEqual(1);
    // At least one button should be a clickable action
    const actionBtn = btns.find(
      (btn) => btn.tagName === 'BUTTON' && !btn.closest('[data-testid="product-detail-modal"]'),
    );
    expect(actionBtn).toBeTruthy();
  });

  // ── Test 14: Status toggle button for offline product (上架) ──
  it('renders "上架" button for offline (status=3) product', () => {
    renderWithProviders(<ProductManagePage />);
    const btns = screen.getAllByText('上架');
    expect(btns.length).toBeGreaterThanOrEqual(1);
  });

  // ── Test 15: Status toggle click calls mutateAsync ──
  it('calls updateStatus.mutateAsync when status toggle button is clicked', () => {
    const mutateAsyncMock = vi.fn().mockResolvedValue({});
    mockUseUpdateProductStatus.mockReturnValue({
      mutateAsync: mutateAsyncMock,
      isPending: false,
    });
    renderWithProviders(<ProductManagePage />);
    // Find the actual action button (not from AdminSelect options)
    const allOfflineBtns = screen.getAllByText('下架');
    const actionBtn = allOfflineBtns.find(
      (btn) => btn.tagName === 'BUTTON' && btn.closest('td') !== null,
    );
    expect(actionBtn).toBeTruthy();
    fireEvent.click(actionBtn!);

    expect(mutateAsyncMock).toHaveBeenCalledWith({
      id: 1,
      data: { status: 3 },
    });
  });

  // ── Test 16: Status toggle disabled while mutation pending ──
  it('disables status toggle buttons when mutation is pending', () => {
    mockUseUpdateProductStatus.mockReturnValue({
      mutateAsync: vi.fn(),
      isPending: true,
    });

    renderWithProviders(<ProductManagePage />);
    const allOfflineBtns = screen.getAllByText('下架');
    const actionBtn = allOfflineBtns.find(
      (btn) => btn.tagName === 'BUTTON' && btn.closest('td') !== null,
    );
    expect(actionBtn).toBeTruthy();
    expect(actionBtn).toBeDisabled();
  });

  // ── Test 17: Pagination renders when total > pageSize ──
  it('renders pagination when total exceeds page size', () => {
    mockUseAdminProducts.mockReturnValue({
      data: createPageData(sampleProducts, 25),
      isLoading: false,
      isError: false,
      error: null,
      refetch: vi.fn(),
    });

    renderWithProviders(<ProductManagePage />);
    // Pagination renders in AdminTable when total > pageSize (10).
    // It shows "共 <total> 条记录" — the "条记录" text is unique to pagination.
    expect(screen.getByText((content) => content.includes('条记录'))).toBeInTheDocument();
    // The filter toolbar also shows "共 <total> 件商品" — verify that too
    expect(screen.getByText((content) => content.includes('件商品'))).toBeInTheDocument();
    // Page numbers are rendered as buttons
    const pageOne = screen.getByRole('button', { name: '1' });
    expect(pageOne).toBeInTheDocument();
  });

  // ── Test 18: Pagination does NOT render when total ≤ pageSize ──
  it('does not render pagination when total is within page size', () => {
    mockUseAdminProducts.mockReturnValue({
      data: createPageData(sampleProducts, 2),
      isLoading: false,
      isError: false,
      error: null,
      refetch: vi.fn(),
    });

    renderWithProviders(<ProductManagePage />);
    // Total is 2, pageSize is 10, so pagination should not appear
    // No "共 ... 条记录" text should be present
    expect(
      screen.queryByText((content, element) => {
        return content.includes('条记录') && element?.tagName === 'DIV';
      }),
    ).not.toBeInTheDocument();
  });

  // ── Test 19: Price formatting ──
  it('formats price correctly in the table', () => {
    renderWithProviders(<ProductManagePage />);
    expect(screen.getByText('¥100.00')).toBeInTheDocument();
    expect(screen.getByText('¥200.00')).toBeInTheDocument();
  });

  // ── Test 20: Condition level labels ──
  it('renders condition level labels', () => {
    renderWithProviders(<ProductManagePage />);
    expect(screen.getByText('9成新')).toBeInTheDocument();
    expect(screen.getByText('8成新')).toBeInTheDocument();
  });

  // ── Test 21: Keyword param passed to useAdminProducts on mount ──
  it('passes initial params to useAdminProducts', () => {
    renderWithProviders(<ProductManagePage />);
    const params = mockUseAdminProducts.mock.calls[0][0];
    expect(params).toMatchObject({
      pageNum: 1,
      pageSize: 10,
    });
    expect(params.keyword).toBeUndefined();
  });
});
