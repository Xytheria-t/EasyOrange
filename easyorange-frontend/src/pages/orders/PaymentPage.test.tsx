import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { OrderDetail } from '@/types';
import PaymentPage from './PaymentPage';

const mockUseOrderDetail = vi.hoisted(() => vi.fn());
const mockUsePaymentStatus = vi.hoisted(() => vi.fn());
const mockUseCreatePayment = vi.hoisted(() => vi.fn());
const mockNavigate = vi.hoisted(() => vi.fn());
const mockUseUIStore = vi.hoisted(() =>
  vi.fn((selector?: (s: { addToast: ReturnType<typeof vi.fn> }) => unknown) => {
    const state = { addToast: vi.fn() };
    return selector ? selector(state) : state;
  }),
);

vi.mock('@/hooks', () => ({
  useOrderDetail: mockUseOrderDetail,
  usePaymentStatus: mockUsePaymentStatus,
  useCreatePayment: mockUseCreatePayment,
}));

vi.mock('@/store/uiStore', () => ({
  useUIStore: mockUseUIStore,
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...(actual as object), useNavigate: () => mockNavigate };
});

function createMockOrder(overrides: Partial<OrderDetail> = {}): OrderDetail {
  return {
    id: 'order-123',
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

function renderPage() {
  return renderWithProviders(<PaymentPage />, { initialRoute: '/payment?orderId=order-123' });
}

beforeEach(() => {
  vi.clearAllMocks();
  mockUseOrderDetail.mockReturnValue({ data: undefined, isLoading: false, isError: false });
  mockUsePaymentStatus.mockReturnValue({ data: undefined, isLoading: false });
  mockUseCreatePayment.mockReturnValue({ mutateAsync: vi.fn().mockResolvedValue({ paymentId: 'payment-1' }), isPending: false });
  mockUseUIStore.mockImplementation(
    (selector?: (s: { addToast: ReturnType<typeof vi.fn> }) => unknown) => {
      const state = { addToast: vi.fn() };
      return selector ? selector(state) : state;
    },
  );
});

describe('PaymentPage', () => {
  it('shows invalid order state when no orderId', () => {
    renderWithProviders(<PaymentPage />, { initialRoute: '/payment' });
    expect(screen.getByText('无效的订单')).toBeInTheDocument();
    expect(screen.getByText('请从订单页面重新发起支付')).toBeInTheDocument();
    expect(screen.getByText('返回订单')).toBeInTheDocument();
  });

  it('shows loading state while fetching order', () => {
    mockUseOrderDetail.mockReturnValue({ data: undefined, isLoading: true });
    renderPage();
    expect(screen.getByText('加载订单信息...')).toBeInTheDocument();
  });

  it('renders payment page with order info', () => {
    const order = createMockOrder({ items: [{ itemId: 'i1', productId: 'p1', productName: '测试手机', productImage: '', unitPrice: 1999.00, quantity: 2, subtotal: 3998.00 }], totalAmount: 1999.00 });
    mockUseOrderDetail.mockReturnValue({ data: order, isLoading: false });
    renderPage();
    expect(screen.getByText('收银台')).toBeInTheDocument();
    const prices = screen.getAllByText('¥1999.00');
    expect(prices.length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('测试手机')).toBeInTheDocument();
    expect(screen.getByText('×2')).toBeInTheDocument();
  });

  it('renders all payment method options', () => {
    const order = createMockOrder();
    mockUseOrderDetail.mockReturnValue({ data: order, isLoading: false });
    renderPage();
    expect(screen.getByText('微信支付')).toBeInTheDocument();
    expect(screen.getByText('支付宝')).toBeInTheDocument();
    expect(screen.getByText('托管钱包')).toBeInTheDocument();
    expect(screen.getByText('推荐使用')).toBeInTheDocument();
    expect(screen.getByText('安全便捷')).toBeInTheDocument();
    expect(screen.getByText('AI 托管专属')).toBeInTheDocument();
  });

  it('shows security note', () => {
    const order = createMockOrder();
    mockUseOrderDetail.mockReturnValue({ data: order, isLoading: false });
    renderPage();
    expect(screen.getByText('支付环境安全，请放心支付')).toBeInTheDocument();
  });

  it('selects WECHAT by default', () => {
    const order = createMockOrder();
    mockUseOrderDetail.mockReturnValue({ data: order, isLoading: false });
    renderPage();
    const wechatBtn = screen.getByText('微信支付').closest('button');
    expect(wechatBtn?.className).toContain('payment-method-selected');
  });

  it('allows switching payment method', async () => {
    const order = createMockOrder();
    mockUseOrderDetail.mockReturnValue({ data: order, isLoading: false });
    renderPage();
    const user = userEvent.setup();
    await user.click(screen.getByText('支付宝'));
    const alipayBtn = screen.getByText('支付宝').closest('button');
    expect(alipayBtn?.className).toContain('payment-method-selected');
    const wechatBtn = screen.getByText('微信支付').closest('button');
    expect(wechatBtn?.className).not.toContain('payment-method-selected');
  });

  it('shows "立即支付" on submit button by default', () => {
    const order = createMockOrder();
    mockUseOrderDetail.mockReturnValue({ data: order, isLoading: false });
    renderPage();
    expect(screen.getByText('立即支付')).toBeInTheDocument();
  });

  it('calls createPayment and shows paying state after submit', async () => {
    const mockMutateAsync = vi.fn().mockResolvedValue({ paymentId: 'payment-1' });
    const addToast = vi.fn();
    mockUseCreatePayment.mockReturnValue({ mutateAsync: mockMutateAsync, isPending: false });
    mockUseUIStore.mockImplementation((sel?: (s: { addToast: typeof vi.fn }) => unknown) => {
      const s = { addToast };
      return sel ? sel(s) : s;
    });
    mockUsePaymentStatus.mockReturnValue({ data: { status: 'PROCESSING' }, isLoading: false });
    const order = createMockOrder();
    mockUseOrderDetail.mockReturnValue({ data: order, isLoading: false });
    renderPage();
    const user = userEvent.setup();
    await user.click(screen.getByText('立即支付'));
    expect(mockMutateAsync).toHaveBeenCalledWith({ orderId: 'order-123', paymentMethod: 'WECHAT' });
    await waitFor(() => {
      expect(addToast).toHaveBeenCalledWith({ type: 'info', message: '支付处理中，请等待...' });
    });
    await waitFor(() => {
      expect(screen.getByText('支付处理中...')).toBeInTheDocument();
    });
  });

  it('navigates to success page when payment succeeds via status polling', () => {
    const order = createMockOrder();
    mockUseOrderDetail.mockReturnValue({ data: order, isLoading: false });
    mockUsePaymentStatus.mockReturnValue({ data: { status: 'SUCCESS' }, isLoading: false });
    renderPage();
    expect(mockNavigate).toHaveBeenCalledWith('/payment/result?status=success&orderId=order-123', { replace: true });
  });

  it('navigates to failed page when payment fails via status polling', () => {
    const order = createMockOrder();
    mockUseOrderDetail.mockReturnValue({ data: order, isLoading: false });
    mockUsePaymentStatus.mockReturnValue({ data: { status: 'FAILED' }, isLoading: false });
    renderPage();
    expect(mockNavigate).toHaveBeenCalledWith('/payment/result?status=failed&orderId=order-123', { replace: true });
  });

  it('shows error toast when createPayment fails', async () => {
    const mockMutateAsync = vi.fn().mockRejectedValue(new Error('Payment error'));
    const addToast = vi.fn();
    mockUseCreatePayment.mockReturnValue({ mutateAsync: mockMutateAsync, isPending: false });
    mockUseUIStore.mockImplementation((sel?: (s: { addToast: typeof vi.fn }) => unknown) => {
      const s = { addToast };
      return sel ? sel(s) : s;
    });
    const order = createMockOrder();
    mockUseOrderDetail.mockReturnValue({ data: order, isLoading: false });
    renderPage();
    const user = userEvent.setup();
    await user.click(screen.getByText('立即支付'));
    await waitFor(() => {
      expect(addToast).toHaveBeenCalledWith({ type: 'error', message: '创建支付失败，请重试' });
    });
  });

  it('navigates back when back button is clicked', async () => {
    const order = createMockOrder();
    mockUseOrderDetail.mockReturnValue({ data: order, isLoading: false });
    renderPage();
    const user = userEvent.setup();
    const backBtn = screen.getByText('收银台').previousElementSibling as HTMLElement;
    await user.click(backBtn);
    expect(mockNavigate).toHaveBeenCalledWith(-1);
  });
});
