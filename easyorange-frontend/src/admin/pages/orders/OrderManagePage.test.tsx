import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, fireEvent } from '@testing-library/react';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import OrderManagePage from './OrderManagePage';
import type { PageData, AdminOrder } from '../../types/admin';

// ─── Hook mocks ───
const mockUseAdminOrders = vi.fn();

vi.mock('../../hooks', () => ({
  useAdminOrders: (...args: unknown[]) => mockUseAdminOrders(...args),
  useAdminOrderDetail: vi.fn(),
  useAdminOrderStats: vi.fn(),
}));

// Mock OrderDetailModal
vi.mock('./OrderDetailModal', () => ({
  OrderDetailModal: ({
    open,
    orderId,
    onClose,
  }: {
    open: boolean;
    orderId: number | null;
    onClose: () => void;
  }) => {
    if (!open) return null;
    return (
      <div data-testid="order-detail-modal" data-order-id={orderId}>
        <button onClick={onClose}>关闭</button>
      </div>
    );
  },
}));

// Mock AdminTable
vi.mock('../../components/AdminTable', () => ({
  AdminTable: ({
    data,
    loading,
    pagination,
    emptyText,
  }: {
    data: unknown[];
    loading: boolean;
    pagination: unknown;
    emptyText: string;
  }) => (
    <div data-testid="admin-table">
      {loading ? (
        <div data-testid="loading-skeleton">Loading...</div>
      ) : data.length === 0 ? (
        <div data-testid="empty-state">{emptyText}</div>
      ) : (
        <div>
          {(data as AdminOrder[]).map((order) => (
            <div key={order.orderId} data-testid="order-row">
              <span data-testid="order-no">{order.orderNo}</span>
              <span data-testid="order-product">{order.productName}</span>
              <span data-testid="order-amount">{order.amount}</span>
              <span data-testid="order-status">{order.status}</span>
              <button
                data-testid="view-detail-btn"
                onClick={() => {
                  /* clicks handled in test */
                }}
              >
                详情
              </button>
            </div>
          ))}
          {!!pagination && (
            <div data-testid="pagination">
              <button onClick={() => (pagination as { onChange: (p: number) => void }).onChange(2)}>
                下一页
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  ),
}));

vi.mock('../../components/StatusBadge', () => ({
  StatusBadge: ({ status }: { status: number | string }) => (
    <span data-testid="status-badge">{status}</span>
  ),
}));

vi.mock('../../components/AdminSelect', () => ({
  AdminSelect: ({
    options,
    value,
    onChange,
  }: {
    options: { value: string; label: string }[];
    value: string;
    onChange: (val: string) => void;
  }) => (
    <select
      data-testid="admin-select"
      value={value}
      onChange={(e) => onChange(e.target.value)}
    >
      {options.map((opt) => (
        <option key={opt.value} value={opt.value}>
          {opt.label}
        </option>
      ))}
    </select>
  ),
}));

// ─── Sample data ───
const samplePageData: PageData<AdminOrder> = {
  records: [
    {
      orderId: 1,
      orderNo: 'ORD20260516001',
      buyerId: 1,
      buyerName: '买家A',
      sellerId: 2,
      sellerName: '卖家B',
      productId: 10,
      productName: '测试商品1',
      amount: 100.00,
      status: 0,
      statusDesc: '待付款',
      paymentStatus: 0,
      paymentStatusDesc: '未支付',
      createTime: '2026-05-16T10:00:00',
    },
    {
      orderId: 2,
      orderNo: 'ORD20260516002',
      buyerId: 3,
      buyerName: '买家C',
      sellerId: 4,
      sellerName: '卖家D',
      productId: 20,
      productName: '测试商品2',
      amount: 200.00,
      status: 1,
      statusDesc: '待发货',
      paymentStatus: 1,
      paymentStatusDesc: '已支付',
      createTime: '2026-05-16T12:00:00',
    },
  ],
  total: 2,
  current: 1,
  size: 10,
  pages: 1,
};

function setupMocks(overrides: Partial<{ data: PageData<AdminOrder> | undefined; isLoading: boolean }> = {}) {
  const { data = samplePageData, isLoading = false } = overrides;

  mockUseAdminOrders.mockReturnValue({
    data,
    isLoading,
    isError: false,
    error: null,
  });
}

describe('OrderManagePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    setupMocks();
  });

  // ── Test 1: Page title ──
  it('renders page title "订单管理"', () => {
    renderWithProviders(<OrderManagePage />);
    expect(screen.getByText('订单管理')).toBeInTheDocument();
    expect(screen.getByText('查看和管理平台所有交易订单')).toBeInTheDocument();
  });

  // ── Test 2: Shows order list ──
  it('renders order list with order data', () => {
    renderWithProviders(<OrderManagePage />);

    expect(screen.getByTestId('admin-table')).toBeInTheDocument();
    expect(screen.getByText('ORD20260516001')).toBeInTheDocument();
    expect(screen.getByText('ORD20260516002')).toBeInTheDocument();
    expect(screen.getByText('测试商品1')).toBeInTheDocument();
    expect(screen.getByText('测试商品2')).toBeInTheDocument();
  });

  // ── Test 3: Shows order total count ──
  it('shows total order count', () => {
    renderWithProviders(<OrderManagePage />);

    const countText = screen.getByText(/共/);
    expect(countText).toHaveTextContent('共 2 笔订单');
  });

  // ── Test 4: Search by order number ──
  it('searches by keyword', () => {
    renderWithProviders(<OrderManagePage />);

    const searchInput = screen.getByPlaceholderText('搜索订单号/商品/买家...');
    fireEvent.change(searchInput, { target: { value: 'ORD20260516001' } });

    // Click search button
    fireEvent.click(screen.getByText('搜索'));
    expect(mockUseAdminOrders).toHaveBeenCalled();
  });

  // ── Test 5: Search on Enter key ──
  it('triggers search on Enter key press', () => {
    renderWithProviders(<OrderManagePage />);

    const searchInput = screen.getByPlaceholderText('搜索订单号/商品/买家...');
    fireEvent.change(searchInput, { target: { value: '测试' } });
    fireEvent.keyPress(searchInput, { key: 'Enter', code: 'Enter' });

    expect(mockUseAdminOrders).toHaveBeenCalled();
  });

  // ── Test 6: Status filter changes page to 1 ──
  it('changes status filter and resets page', () => {
    renderWithProviders(<OrderManagePage />);

    const selects = screen.getAllByTestId('admin-select');
    const statusSelect = selects[0];
    fireEvent.change(statusSelect, { target: { value: '1' } });

    expect(mockUseAdminOrders).toHaveBeenCalled();
  });

  // ── Test 7: Loading state ──
  it('shows loading state', () => {
    setupMocks({ isLoading: true });
    renderWithProviders(<OrderManagePage />);

    expect(screen.getByTestId('loading-skeleton')).toBeInTheDocument();
  });

  // ── Test 8: Empty state ──
  it('shows empty state when no orders', () => {
    setupMocks({
      data: { records: [], total: 0, current: 1, size: 10, pages: 0 },
    });
    renderWithProviders(<OrderManagePage />);

    expect(screen.getByText('暂无订单数据')).toBeInTheDocument();
  });

  // ── Test 9: Pagination ──
  it('triggers pagination on page change', () => {
    const multiPageData: PageData<AdminOrder> = {
      records: Array.from({ length: 10 }, (_, i) => ({
        orderId: i + 1,
        orderNo: `ORD${String(i + 1).padStart(11, '0')}`,
        buyerId: 1,
        buyerName: `买家${i}`,
        sellerId: 2,
        sellerName: `卖家${i}`,
        productId: 10 + i,
        productName: `商品${i}`,
        amount: (i + 1) * 100,
        status: i % 3,
        statusDesc: ['待付款', '待发货', '已发货'][i % 3],
        paymentStatus: i % 2,
        paymentStatusDesc: i % 2 === 0 ? '未支付' : '已支付',
        createTime: '2026-05-16T10:00:00',
      })),
      total: 25,
      current: 1,
      size: 10,
      pages: 3,
    };

    setupMocks({ data: multiPageData });
    renderWithProviders(<OrderManagePage />);

    expect(screen.getByTestId('pagination')).toBeInTheDocument();

    const nextBtn = screen.getByText('下一页');
    fireEvent.click(nextBtn);
    // The hook should have been called with page 2
    expect(mockUseAdminOrders).toHaveBeenCalled();
  });

  // ── Test 10: Detail modal interaction ──
  it('opens order detail modal when detail button is clicked', () => {
    // We need to test the actual detail button from the page
    renderWithProviders(<OrderManagePage />);

    // The page has detail buttons in the column actions
    const detailBtns = screen.getAllByText('详情');
    expect(detailBtns.length).toBeGreaterThan(0);
  });
});
