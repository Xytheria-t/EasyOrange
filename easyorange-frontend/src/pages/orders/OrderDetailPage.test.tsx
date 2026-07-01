import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import type { OrderDetail } from '@/types';
import OrderDetailPage from './OrderDetailPage';

const mockUseOrderDetail = vi.hoisted(() => vi.fn());
const mockUseCancelOrder = vi.hoisted(() => vi.fn());
const mockUsePayOrder = vi.hoisted(() => vi.fn());
const mockUseReceiveOrder = vi.hoisted(() => vi.fn());
const mockUseRefundOrder = vi.hoisted(() => vi.fn());
const mockNavigate = vi.hoisted(() => vi.fn());
const mockUseUIStore = vi.hoisted(() =>
    vi.fn((selector?: (s: { addToast: ReturnType<typeof vi.fn> }) => unknown) => {
        const state = { addToast: vi.fn() };
        return selector ? selector(state) : state;
    })
);

vi.mock('@/hooks', () => ({
    useOrderDetail: mockUseOrderDetail,
    useCancelOrder: mockUseCancelOrder,
    usePayOrder: mockUsePayOrder,
    useReceiveOrder: mockUseReceiveOrder,
    useRefundOrder: mockUseRefundOrder,
}));

vi.mock('@/store', () => ({
    useUIStore: mockUseUIStore,
}));

vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom');
    return { ...(actual as object), useNavigate: () => mockNavigate, useParams: () => ({ id: 'order-123' }) };
});

function createMockOrderDetail(overrides: Partial<OrderDetail> = {}): OrderDetail {
    return {
        id: 'order-123',
        orderNo: 'ORD202605011001',
        buyerId: 'buyer1',
        buyerUsername: '认领方小明',
        sellerId: 'seller1',
        sellerUsername: '资产方张三',
        items: [
            {
                itemId: 'item1',
                productId: 'prod1',
                productName: '测试商品名称',
                productImage: 'https://example.com/image.jpg',
                unitPrice: 99.99,
                quantity: 1,
                subtotal: 99.99,
            },
        ],
        totalAmount: 99.99,
        singleItem: true,
        status: 0,
        statusDesc: '待付款',
        address: '北京市海淀区',
        phone: '13800138000',
        remark: '请尽快发货',
        createTime: '2026-05-01 10:00:00',
        updateTime: '2026-05-01 10:00:00',
        ...overrides,
    };
}

function renderPage() {
    return renderWithProviders(<OrderDetailPage />, { initialRoute: '/orders/order-123' });
}

beforeEach(() => {
    vi.clearAllMocks();
    mockUseOrderDetail.mockReturnValue({
        data: undefined,
        isLoading: false,
        isError: false,
        error: null,
        refetch: vi.fn(),
    });
    mockUseCancelOrder.mockReturnValue({ mutateAsync: vi.fn().mockResolvedValue(undefined), isPending: false });
    mockUsePayOrder.mockReturnValue({ mutateAsync: vi.fn().mockResolvedValue(undefined), isPending: false });
    mockUseReceiveOrder.mockReturnValue({ mutateAsync: vi.fn().mockResolvedValue(undefined), isPending: false });
    mockUseRefundOrder.mockReturnValue({ mutateAsync: vi.fn().mockResolvedValue(undefined), isPending: false });
    mockUseUIStore.mockImplementation((selector?: (s: { addToast: ReturnType<typeof vi.fn> }) => unknown) => {
        const state = { addToast: vi.fn() };
        return selector ? selector(state) : state;
    });
});

describe('OrderDetailPage', () => {
    it('renders loading skeleton when order is loading', () => {
        mockUseOrderDetail.mockReturnValue({ data: undefined, isLoading: true });
        renderPage();
        expect(screen.getByText('加载订单详情...')).toBeInTheDocument();
    });

    it('renders not-found state when order is null', () => {
        mockUseOrderDetail.mockReturnValue({ data: undefined, isLoading: false, isError: true });
        renderPage();
        expect(screen.getByText('订单不存在或加载失败')).toBeInTheDocument();
        expect(screen.getByText('重新加载')).toBeInTheDocument();
        expect(screen.getByText('返回订单列表')).toBeInTheDocument();
    });

    it('renders order detail sections with correct content', () => {
        const order = createMockOrderDetail();
        mockUseOrderDetail.mockReturnValue({ data: order, isLoading: false, isError: false });
        renderPage();
        expect(screen.getByText('商品信息')).toBeInTheDocument();
        expect(screen.getByText('收货信息')).toBeInTheDocument();
        expect(screen.getByText('交易信息')).toBeInTheDocument();
        expect(screen.getByText('时间信息')).toBeInTheDocument();
        expect(screen.getByText('测试商品名称')).toBeInTheDocument();
        expect(screen.getAllByText('¥99.99').length).toBeGreaterThanOrEqual(1);
    });

    it('shows address and phone in shipping section', () => {
        const order = createMockOrderDetail({ address: '上海市浦东新区', phone: '13900139000' });
        mockUseOrderDetail.mockReturnValue({ data: order, isLoading: false, isError: false });
        renderPage();
        expect(screen.getByText('上海市浦东新区')).toBeInTheDocument();
        expect(screen.getByText('13900139000')).toBeInTheDocument();
    });

    it('shows buyer and seller usernames', () => {
        const order = createMockOrderDetail({ buyerUsername: '认领方王五', sellerUsername: '资产方赵六' });
        mockUseOrderDetail.mockReturnValue({ data: order, isLoading: false, isError: false });
        renderPage();
        expect(screen.getByText('认领方王五')).toBeInTheDocument();
        expect(screen.getByText('资产方赵六')).toBeInTheDocument();
    });

    it('shows remark when present', () => {
        const order = createMockOrderDetail({ remark: '请包装好' });
        mockUseOrderDetail.mockReturnValue({ data: order, isLoading: false, isError: false });
        renderPage();
        expect(screen.getByText('请包装好')).toBeInTheDocument();
    });

    it('does not show remark section when remark is null', () => {
        const order = createMockOrderDetail({ remark: null });
        mockUseOrderDetail.mockReturnValue({ data: order, isLoading: false, isError: false });
        renderPage();
        expect(screen.queryByText('备注')).not.toBeInTheDocument();
    });

    it('shows orderNo in time section', () => {
        const order = createMockOrderDetail({ orderNo: 'ORD-SPECIAL-001' });
        mockUseOrderDetail.mockReturnValue({ data: order, isLoading: false, isError: false });
        renderPage();
        expect(screen.getByText('ORD-SPECIAL-001')).toBeInTheDocument();
    });

    it('shows status hero with correct label for PENDING_PAYMENT', () => {
        const order = createMockOrderDetail({ status: 0 });
        mockUseOrderDetail.mockReturnValue({ data: order, isLoading: false, isError: false });
        renderPage();
        expect(screen.getByText('待付款')).toBeInTheDocument();
        expect(screen.getByText('请尽快完成支付，超时订单将自动取消')).toBeInTheDocument();
    });

    it('shows timeline for active orders', () => {
        const order = createMockOrderDetail({ status: 1 });
        mockUseOrderDetail.mockReturnValue({ data: order, isLoading: false, isError: false });
        renderPage();
        expect(screen.getByText('下单')).toBeInTheDocument();
        expect(screen.getByText('付款')).toBeInTheDocument();
        expect(screen.getByText('发货')).toBeInTheDocument();
        expect(screen.getByText('完成')).toBeInTheDocument();
    });

    it('does not show timeline for CANCELLED orders', () => {
        const order = createMockOrderDetail({ status: 4 });
        mockUseOrderDetail.mockReturnValue({ data: order, isLoading: false, isError: false });
        renderPage();
        expect(screen.queryByText('下单')).not.toBeInTheDocument();
        expect(screen.queryByText('付款')).not.toBeInTheDocument();
        expect(screen.queryByText('发货')).not.toBeInTheDocument();
        expect(screen.queryByText('完成')).not.toBeInTheDocument();
    });

    it('shows cancel and pay buttons for PENDING_PAYMENT', () => {
        const order = createMockOrderDetail({ status: 0 });
        mockUseOrderDetail.mockReturnValue({ data: order, isLoading: false, isError: false });
        renderPage();
        expect(screen.getByText('取消订单')).toBeInTheDocument();
        expect(screen.getByText('立即支付')).toBeInTheDocument();
    });

    it('shows refund button for PAID', () => {
        const order = createMockOrderDetail({ status: 1 });
        mockUseOrderDetail.mockReturnValue({ data: order, isLoading: false, isError: false });
        renderPage();
        expect(screen.getByText('申请退款')).toBeInTheDocument();
        expect(screen.queryByText('立即支付')).not.toBeInTheDocument();
    });

    it('shows receive button for SHIPPED', () => {
        const order = createMockOrderDetail({ status: 2 });
        mockUseOrderDetail.mockReturnValue({ data: order, isLoading: false, isError: false });
        renderPage();
        expect(screen.getByText('确认收货')).toBeInTheDocument();
    });

    it('does not show action buttons for COMPLETED', () => {
        const order = createMockOrderDetail({ status: 3 });
        mockUseOrderDetail.mockReturnValue({ data: order, isLoading: false, isError: false });
        renderPage();
        expect(screen.queryByText('取消订单')).not.toBeInTheDocument();
        expect(screen.queryByText('立即支付')).not.toBeInTheDocument();
        expect(screen.queryByText('确认收货')).not.toBeInTheDocument();
        expect(screen.queryByText('申请退款')).not.toBeInTheDocument();
    });

    it('calls cancelOrder when cancel button is clicked', async () => {
        const mockMutateAsync = vi.fn().mockResolvedValue(undefined);
        const addToast = vi.fn();
        mockUseCancelOrder.mockReturnValue({ mutateAsync: mockMutateAsync, isPending: false });
        mockUseUIStore.mockImplementation((sel?: (s: { addToast: typeof vi.fn }) => unknown) => {
            const s = { addToast };
            return sel ? sel(s) : s;
        });
        const order = createMockOrderDetail({ status: 0 });
        mockUseOrderDetail.mockReturnValue({ data: order, isLoading: false, isError: false });
        renderPage();
        const user = userEvent.setup();
        await user.click(screen.getByText('取消订单'));
        expect(mockMutateAsync).toHaveBeenCalledWith({ id: 'order-123' });
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
        const order = createMockOrderDetail({ status: 0 });
        mockUseOrderDetail.mockReturnValue({ data: order, isLoading: false, isError: false });
        renderPage();
        const user = userEvent.setup();
        await user.click(screen.getByText('立即支付'));
        expect(mockMutateAsync).toHaveBeenCalledWith('order-123');
        await waitFor(() => {
            expect(addToast).toHaveBeenCalledWith({ type: 'success', message: '支付请求已提交' });
        });
    });

    it('calls receiveOrder when receive button is clicked', async () => {
        const mockMutateAsync = vi.fn().mockResolvedValue(undefined);
        const addToast = vi.fn();
        mockUseReceiveOrder.mockReturnValue({ mutateAsync: mockMutateAsync, isPending: false });
        mockUseUIStore.mockImplementation((sel?: (s: { addToast: typeof vi.fn }) => unknown) => {
            const s = { addToast };
            return sel ? sel(s) : s;
        });
        const order = createMockOrderDetail({ status: 2 });
        mockUseOrderDetail.mockReturnValue({ data: order, isLoading: false, isError: false });
        renderPage();
        const user = userEvent.setup();
        await user.click(screen.getByText('确认收货'));
        expect(mockMutateAsync).toHaveBeenCalledWith('order-123');
        await waitFor(() => {
            expect(addToast).toHaveBeenCalledWith({ type: 'success', message: '已确认收货' });
        });
    });

    it('calls refundOrder when refund button is clicked', async () => {
        const mockMutateAsync = vi.fn().mockResolvedValue(undefined);
        const addToast = vi.fn();
        mockUseRefundOrder.mockReturnValue({ mutateAsync: mockMutateAsync, isPending: false });
        mockUseUIStore.mockImplementation((sel?: (s: { addToast: typeof vi.fn }) => unknown) => {
            const s = { addToast };
            return sel ? sel(s) : s;
        });
        const order = createMockOrderDetail({ status: 1 });
        mockUseOrderDetail.mockReturnValue({ data: order, isLoading: false, isError: false });
        renderPage();
        const user = userEvent.setup();
        await user.click(screen.getByText('申请退款'));
        expect(mockMutateAsync).toHaveBeenCalledWith({ id: 'order-123' });
        await waitFor(() => {
            expect(addToast).toHaveBeenCalledWith({ type: 'success', message: '退款申请已提交' });
        });
    });

    it('navigates back when back button is clicked', async () => {
        const order = createMockOrderDetail();
        mockUseOrderDetail.mockReturnValue({ data: order, isLoading: false, isError: false });
        renderPage();
        const user = userEvent.setup();
        const backBtn = screen.getByText('订单详情').previousElementSibling as HTMLElement;
        await user.click(backBtn);
        expect(mockNavigate).toHaveBeenCalledWith(-1);
    });

    it('shows quantity in product info', () => {
        const order = createMockOrderDetail({
            items: [
                {
                    itemId: 'i1',
                    productId: 'p1',
                    productName: '测试商品名称',
                    productImage: 'https://example.com/image.jpg',
                    unitPrice: 99.99,
                    quantity: 3,
                    subtotal: 299.97,
                },
            ],
            totalAmount: 299.97,
        });
        mockUseOrderDetail.mockReturnValue({ data: order, isLoading: false, isError: false });
        renderPage();
        expect(screen.getByText('×3')).toBeInTheDocument();
    });
});
