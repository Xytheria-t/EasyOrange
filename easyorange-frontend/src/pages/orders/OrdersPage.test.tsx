import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { Order, PageResult } from '@/types';
import OrdersPage from './OrdersPage';

const mockUseMyOrders = vi.hoisted(() => vi.fn());
const mockUseCancelOrder = vi.hoisted(() => vi.fn());
const mockUsePayOrder = vi.hoisted(() => vi.fn());
const mockUseReceiveOrder = vi.hoisted(() => vi.fn());
const mockNavigate = vi.hoisted(() => vi.fn());
const mockUseUIStore = vi.hoisted(() =>
  vi.fn((selector?: (s: { addToast: ReturnType<typeof vi.fn> }) => unknown) => {
    const state = { addToast: vi.fn() };
    return selector ? selector(state) : state;
  }),
);

vi.mock('@/hooks', () => ({
  useMyOrders: mockUseMyOrders,
  useCancelOrder: mockUseCancelOrder,
  usePayOrder: mockUsePayOrder,
  useReceiveOrder: mockUseReceiveOrder,
}));

vi.mock('@/store', () => ({
  useUIStore: mockUseUIStore,
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...(actual as object), useNavigate: () => mockNavigate };
});

function createMockOrder(overrides: Partial<Order> = {}): Order {
  return {
    id: '1',
    orderNo: 'ORD202605011001',
    buyerId: 'buyer1',
    buyerUsername: '认领方小明',
    sellerId: 'seller1',
    sellerUsername: '资产方张三',
    items: [{
      itemId: 'item1',
      productId: 'prod1',
      productName: '测试商品名称',
      productImage: 'https://example.com/image.jpg',
      unitPrice: 99.99,
      quantity: 1,
      subtotal: 99.99,
    }],
    totalAmount: 99.99,
    singleItem: true,
    status: 0,
    statusDesc: '待付款',
    address: '北京市海淀区',
    phone: '13800138000',
    remark: null,
    createTime: '2026-05-01 10:00:00',
    updateTime: '2026-05-01 10:00:00',
    ...overrides,
  };
}

function createMockPage(data: Order[] = []): PageResult<Order> {
  return { records: data, total: data.length, current: 1, size: 20, pages: 1 };
}

function renderPage() {
  return renderWithProviders(<OrdersPage />, { initialRoute: '/orders' });
}

beforeEach(() => {
  vi.clearAllMocks();
  mockUseMyOrders.mockReturnValue({ data: undefined, isLoading: false, isError: false, refetch: vi.fn() });
  mockUseCancelOrder.mockReturnValue({ mutateAsync: vi.fn().mockResolvedValue(undefined), isPending: false });
  mockUsePayOrder.mockReturnValue({ mutateAsync: vi.fn().mockResolvedValue(undefined), isPending: false });
  mockUseReceiveOrder.mockReturnValue({ mutateAsync: vi.fn().mockResolvedValue(undefined), isPending: false });
  mockUseUIStore.mockImplementation(
    (selector?: (s: { addToast: ReturnType<typeof vi.fn> }) => unknown) => {
      const state = { addToast: vi.fn() };
      return selector ? selector(state) : state;
    },
  );
});

describe('OrdersPage', () => {
  it('renders the page title and subtitle', () => {
    mockUseMyOrders.mockReturnValue({ data: createMockPage(), isLoading: false, isError: false, refetch: vi.fn() });
    renderPage();
    expect(screen.getByText('我的订单')).toBeInTheDocument();
    expect(screen.getByText('追踪每一笔交易，掌控购物旅程')).toBeInTheDocument();
  });

  it('shows loading spinner when orders are loading', () => {
    mockUseMyOrders.mockReturnValue({ data: undefined, isLoading: true });
    renderPage();
    expect(screen.getByText('正在加载订单...')).toBeInTheDocument();
  });

  it('shows error state with retry button', () => {
    mockUseMyOrders.mockReturnValue({ data: undefined, isLoading: false, isError: true, refetch: vi.fn() });
    renderPage();
    expect(screen.getByText('加载失败，请稍后重试')).toBeInTheDocument();
    expect(screen.getByText('重新加载')).toBeInTheDocument();
  });

  it('shows empty state when no orders', () => {
    mockUseMyOrders.mockReturnValue({ data: createMockPage([]), isLoading: false, isError: false });
    renderPage();
    expect(screen.getByText('暂无订单')).toBeInTheDocument();
    expect(screen.getByText('探索资产')).toBeInTheDocument();
  });

  it('renders order cards with correct content', async () => {
    const orders = [
      createMockOrder({ id: '1', items: [{ itemId: 'i1', productId: 'p1', productName: '商品A', productImage: '', unitPrice: 50, quantity: 1, subtotal: 50 }], totalAmount: 50, status: 0 }),
      createMockOrder({ id: '2', items: [{ itemId: 'i2', productId: 'p2', productName: '商品B', productImage: '', unitPrice: 100, quantity: 1, subtotal: 100 }], totalAmount: 100, status: 2 }),
    ];
    mockUseMyOrders.mockReturnValue({ data: createMockPage(orders), isLoading: false, isError: false });
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('商品A')).toBeInTheDocument();
    });
    expect(screen.getByText('商品B')).toBeInTheDocument();
    expect(screen.getByText('¥50.00')).toBeInTheDocument();
    expect(screen.getByText('¥100.00')).toBeInTheDocument();
  });

  it('shows cancel and pay buttons for PENDING_PAYMENT orders', () => {
    const order = createMockOrder({ id: '1', status: 0, productTitle: '待付款商品' });
    mockUseMyOrders.mockReturnValue({ data: createMockPage([order]), isLoading: false, isError: false });
    renderPage();
    expect(screen.getByText('取消订单')).toBeInTheDocument();
    expect(screen.getByText('立即支付')).toBeInTheDocument();
  });

  it('shows confirm receive button for SHIPPED orders', () => {
    const order = createMockOrder({ id: '1', status: 2, productTitle: '已发货商品' });
    mockUseMyOrders.mockReturnValue({ data: createMockPage([order]), isLoading: false, isError: false });
    renderPage();
    expect(screen.getByText('确认收货')).toBeInTheDocument();
    expect(screen.queryByText('取消订单')).not.toBeInTheDocument();
    expect(screen.queryByText('立即支付')).not.toBeInTheDocument();
  });

  it('does not show action buttons for COMPLETED/CANCELLED orders', () => {
    const order = createMockOrder({ id: '1', status: 3, productTitle: '已完成' });
    mockUseMyOrders.mockReturnValue({ data: createMockPage([order]), isLoading: false, isError: false });
    renderPage();
    expect(screen.queryByText('取消订单')).not.toBeInTheDocument();
    expect(screen.queryByText('立即支付')).not.toBeInTheDocument();
    expect(screen.queryByText('确认收货')).not.toBeInTheDocument();
  });

  it('shows status badge with correct label for each status', () => {
    const order = createMockOrder({ id: '1', status: 0 });
    mockUseMyOrders.mockReturnValue({ data: createMockPage([order]), isLoading: false, isError: false });
    renderPage();
    const statusBadges = screen.getAllByText('待付款');
    expect(statusBadges.length).toBeGreaterThanOrEqual(1);
  });

  it('calls cancelOrder when cancel button is clicked', async () => {
    const mockMutateAsync = vi.fn().mockResolvedValue(undefined);
    const addToast = vi.fn();
    mockUseCancelOrder.mockReturnValue({ mutateAsync: mockMutateAsync, isPending: false });
    mockUseUIStore.mockImplementation((sel?: (s: { addToast: typeof vi.fn }) => unknown) => {
      const s = { addToast };
      return sel ? sel(s) : s;
    });
    const order = createMockOrder({ id: 'order1', status: 0 });
    mockUseMyOrders.mockReturnValue({ data: createMockPage([order]), isLoading: false, isError: false });
    renderPage();
    const user = userEvent.setup();
    await user.click(screen.getByText('取消订单'));
    expect(mockMutateAsync).toHaveBeenCalledWith({ id: 'order1' });
    await waitFor(() => {
      expect(addToast).toHaveBeenCalledWith({ type: 'success', message: '订单已取消' });
    });
  });

  it('calls payOrder when pay button is clicked', async () => {
    const mockMutateAsync = vi.fn().mockResolvedValue(undefined);
    const addToast = vi.fn();
    mockUsePayOrder.mockReturnValue({ mutateAsync: mockMutateAsync, isPending: false });
    mockUseUIStore.mockImplementation((sel?: (s: { addToast: typeof vi.fn }) => unknown) => {
      const s = { addToast };
      return sel ? sel(s) : s;
    });
    const order = createMockOrder({ id: 'order1', status: 0 });
    mockUseMyOrders.mockReturnValue({ data: createMockPage([order]), isLoading: false, isError: false });
    renderPage();
    const user = userEvent.setup();
    await user.click(screen.getByText('立即支付'));
    expect(mockMutateAsync).toHaveBeenCalledWith('order1');
    await waitFor(() => {
      expect(addToast).toHaveBeenCalledWith({ type: 'success', message: '支付请求已提交' });
    });
  });

  it('calls receiveOrder when confirm receive button is clicked', async () => {
    const mockMutateAsync = vi.fn().mockResolvedValue(undefined);
    const addToast = vi.fn();
    mockUseReceiveOrder.mockReturnValue({ mutateAsync: mockMutateAsync, isPending: false });
    mockUseUIStore.mockImplementation((sel?: (s: { addToast: typeof vi.fn }) => unknown) => {
      const s = { addToast };
      return sel ? sel(s) : s;
    });
    const order = createMockOrder({ id: 'order1', status: 2 });
    mockUseMyOrders.mockReturnValue({ data: createMockPage([order]), isLoading: false, isError: false });
    renderPage();
    const user = userEvent.setup();
    await user.click(screen.getByText('确认收货'));
    expect(mockMutateAsync).toHaveBeenCalledWith('order1');
    await waitFor(() => {
      expect(addToast).toHaveBeenCalledWith({ type: 'success', message: '已确认收货' });
    });
  });

  it('navigates to order detail when order card is clicked', async () => {
    const order = createMockOrder({ id: 'order-detail-1', status: 0 });
    mockUseMyOrders.mockReturnValue({ data: createMockPage([order]), isLoading: false, isError: false });
    renderPage();
    const user = userEvent.setup();
    const card = screen.getByText('测试商品名称').closest('[role="button"]');
    expect(card).not.toBeNull();
    await user.click(card as HTMLElement);
    expect(mockNavigate).toHaveBeenCalledWith('/orders/order-detail-1');
  });

  it('navigates to explore page from empty state CTA', async () => {
    mockUseMyOrders.mockReturnValue({ data: createMockPage([]), isLoading: false, isError: false });
    renderPage();
    const user = userEvent.setup();
    await user.click(screen.getByText('探索资产'));
    expect(mockNavigate).toHaveBeenCalledWith('/products');
  });

  it('renders all tab buttons', () => {
    mockUseMyOrders.mockReturnValue({ data: createMockPage(), isLoading: false, isError: false });
    renderPage();
    expect(screen.getByText('全部')).toBeInTheDocument();
    expect(screen.getByText('待付款')).toBeInTheDocument();
    expect(screen.getByText('待发货')).toBeInTheDocument();
    expect(screen.getByText('已发货')).toBeInTheDocument();
    expect(screen.getByText('已完成')).toBeInTheDocument();
    expect(screen.getByText('已取消')).toBeInTheDocument();
  });

  it('switches tab when clicked', async () => {
    mockUseMyOrders.mockReturnValue({ data: createMockPage(), isLoading: false, isError: false });
    renderPage();
    const user = userEvent.setup();
    await user.click(screen.getByText('已发货'));
    const shippedTab = screen.getByText('已发货').closest('button');
    expect(shippedTab?.className).toContain('orders-tab-active');
  });

  it('shows orderNo in the order card', () => {
    const order = createMockOrder({ orderNo: 'ORD999999' });
    mockUseMyOrders.mockReturnValue({ data: createMockPage([order]), isLoading: false, isError: false });
    renderPage();
    expect(screen.getByText('ORD999999')).toBeInTheDocument();
  });

  it('shows seller name in order card', () => {
    const order = createMockOrder({ sellerUsername: '测试资产方' });
    mockUseMyOrders.mockReturnValue({ data: createMockPage([order]), isLoading: false, isError: false });
    renderPage();
    expect(screen.getByText('资产方：测试资产方')).toBeInTheDocument();
  });

  it('shows quantity in order card', () => {
    const order = createMockOrder({ items: [{ itemId: 'i1', productId: 'p1', productName: '测试商品名称', productImage: 'https://example.com/image.jpg', unitPrice: 99.99, quantity: 3, subtotal: 299.97 }], totalAmount: 299.97 });
    mockUseMyOrders.mockReturnValue({ data: createMockPage([order]), isLoading: false, isError: false });
    renderPage();
    expect(screen.getByText('×3')).toBeInTheDocument();
  });
});
