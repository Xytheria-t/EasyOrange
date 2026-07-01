import { fireEvent, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import type { AdminOrderDetail } from '../../types/admin';
import { OrderDetailModal } from './OrderDetailModal';

// ─── Hook mocks ───
const mockUseAdminOrderDetail = vi.fn();

vi.mock('../../hooks', () => ({
    useAdminOrderDetail: (...args: unknown[]) => mockUseAdminOrderDetail(...args),
}));

// ─── Sample data ───
const sampleOrderDetail: AdminOrderDetail = {
    orderId: 1,
    orderNo: 'ORD20260516001',
    buyer: { userId: 1, nickname: '认领方A', avatar: null, phone: '13800138000' },
    seller: { userId: 2, nickname: '资产方B', avatar: null, phone: '13900139000' },
    items: [
        {
            itemId: 1,
            productId: 10,
            productName: '测试商品',
            productImage: '',
            unitPrice: 100.0,
            quantity: 1,
            subtotal: 100.0,
        },
    ],
    totalAmount: 100.0,
    singleItem: true,
    status: 0,
    statusDesc: '待付款',
    paymentStatus: 0,
    paymentNo: null,
    paidAmount: null,
    refundedAmount: null,
    shippingAddress: null,
    remark: null,
    cancelReason: null,
    createTime: '2026-05-16T10:00:00',
    payTime: null,
    updateTime: '2026-05-16T10:00:00',
    cancelTime: null,
};

function setupMocks(overrides: Partial<{ data: AdminOrderDetail | undefined; isLoading: boolean }> = {}) {
    const data = 'data' in overrides ? overrides.data : sampleOrderDetail;
    const isLoading = 'isLoading' in overrides ? overrides.isLoading : false;
    mockUseAdminOrderDetail.mockReturnValue({ data, isLoading });
}

describe('OrderDetailModal', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        setupMocks();
    });

    // ── Test 1: Does not render when closed ──
    it('does not render when open is false', () => {
        const { container } = renderWithProviders(<OrderDetailModal open={false} orderId={null} onClose={() => {}} />);
        // Should return null, nothing in container
        expect(container.textContent).toBe('');
    });

    // ── Test 2: Does not render when orderId is null even if open is true ──
    it('does not render when orderId is null', () => {
        const { container } = renderWithProviders(<OrderDetailModal open={true} orderId={null} onClose={() => {}} />);
        expect(container.textContent).toBe('');
    });

    // ── Test 3: Shows loading state ──
    it('shows loading spinner when loading', () => {
        setupMocks({ isLoading: true, data: undefined });
        renderWithProviders(<OrderDetailModal open={true} orderId={1} onClose={() => {}} />);

        expect(screen.getByText('加载中...')).toBeInTheDocument();
    });

    // ── Test 4: Renders order details correctly ──
    it('renders order details when open with data', () => {
        renderWithProviders(<OrderDetailModal open={true} orderId={1} onClose={() => {}} />);

        // Title
        expect(screen.getByText('订单详情')).toBeInTheDocument();

        // Order number
        expect(screen.getByText('ORD20260516001')).toBeInTheDocument();

        // Order amount
        expect(screen.getByText('¥100.00')).toBeInTheDocument();

        // Product info
        expect(screen.getByText('测试商品')).toBeInTheDocument();
        expect(screen.getByText('单价: ¥100.00')).toBeInTheDocument();

        // Buyer and seller
        expect(screen.getByText('认领方A')).toBeInTheDocument();
        expect(screen.getByText('资产方B')).toBeInTheDocument();

        // Payment status
        expect(screen.getByText('未支付')).toBeInTheDocument();
    });

    // ── Test 5: Shows "order not found" for missing data ──
    it('shows not found message when order is undefined', () => {
        setupMocks({ data: undefined, isLoading: false });
        renderWithProviders(<OrderDetailModal open={true} orderId={999} onClose={() => {}} />);

        expect(screen.getByText('订单不存在或已被删除')).toBeInTheDocument();
    });

    // ── Test 6: Close button calls onClose ──
    it('calls onClose when close button is clicked', () => {
        const onClose = vi.fn();
        renderWithProviders(<OrderDetailModal open={true} orderId={1} onClose={onClose} />);

        // The visible header close button shares the accessible name with the hidden dialog close button
        const [closeBtn] = screen.getAllByRole('button', { name: '关闭' });
        fireEvent.click(closeBtn);
        expect(onClose).toHaveBeenCalledTimes(1);
    });

    // ── Test 7: Escape key closes modal ──
    it('calls onClose when Escape key is pressed', () => {
        const onClose = vi.fn();
        renderWithProviders(<OrderDetailModal open={true} orderId={1} onClose={onClose} />);

        fireEvent.keyDown(document, { key: 'Escape' });
        expect(onClose).toHaveBeenCalledTimes(1);
    });

    // ── Test 8: Shows shipping address when present ──
    it('renders shipping address when available', () => {
        const orderWithAddress: AdminOrderDetail = {
            ...sampleOrderDetail,
            shippingAddress: {
                receiverName: '张三',
                phone: '13800138000',
                detailAddress: '北京市朝阳区xxx街道',
            },
        };
        setupMocks({ data: orderWithAddress });

        renderWithProviders(<OrderDetailModal open={true} orderId={1} onClose={() => {}} />);

        expect(screen.getByText('收货地址')).toBeInTheDocument();
        expect(screen.getByText(/张三/)).toBeInTheDocument();
        expect(screen.getByText(/北京市朝阳区xxx街道/)).toBeInTheDocument();
    });

    // ── Test 9: Shows remark when present ──
    it('renders remark when available', () => {
        const orderWithRemark: AdminOrderDetail = {
            ...sampleOrderDetail,
            remark: '这是一个备注',
        };
        setupMocks({ data: orderWithRemark });

        renderWithProviders(<OrderDetailModal open={true} orderId={1} onClose={() => {}} />);

        expect(screen.getByText('备注')).toBeInTheDocument();
        expect(screen.getByText('这是一个备注')).toBeInTheDocument();
    });

    // ── Test 10: Shows cancel reason when present ──
    it('renders cancel reason when available', () => {
        const orderWithCancel: AdminOrderDetail = {
            ...sampleOrderDetail,
            status: 4,
            cancelReason: '认领方申请取消',
        };
        setupMocks({ data: orderWithCancel });

        renderWithProviders(<OrderDetailModal open={true} orderId={1} onClose={() => {}} />);

        expect(screen.getByText('取消原因')).toBeInTheDocument();
        expect(screen.getByText('认领方申请取消')).toBeInTheDocument();
    });
});
