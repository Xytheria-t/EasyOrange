import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { AdminOrderDetail } from '../../types/admin';
import { OrderInterventionBar } from './OrderInterventionBar';

const mockCancel = vi.fn();
const mockForceComplete = vi.fn();
const mockRefund = vi.fn();

vi.mock('../../hooks', () => ({
    useAdminCancelOrder: () => ({ mutateAsync: mockCancel, isPending: false }),
    useForceCompleteOrder: () => ({ mutateAsync: mockForceComplete, isPending: false }),
    useAdminRefundOrder: () => ({ mutateAsync: mockRefund, isPending: false }),
}));

const notifySuccess = vi.fn();
const notifyError = vi.fn();
vi.mock('../../notify', () => ({
    notify: {
        success: (m: string) => notifySuccess(m),
        error: (m: string) => notifyError(m),
        failure: (_e: unknown, fallback: string) => notifyError(fallback),
    },
}));

function order(overrides: Partial<AdminOrderDetail> = {}): AdminOrderDetail {
    return {
        orderId: 'o1',
        orderNo: 'ORD1',
        buyer: { userId: '1', nickname: '买家', avatar: null, phone: null },
        seller: { userId: '2', nickname: '卖家', avatar: null, phone: null },
        items: [],
        totalAmount: 100,
        status: 'PENDING_PAYMENT',
        statusDesc: '待付款',
        paymentStatus: 'UNPAID',
        paymentNo: null,
        paidAmount: null,
        refundedAmount: null,
        shippingAddress: null,
        remark: null,
        cancelReason: null,
        createTime: null,
        payTime: null,
        updateTime: null,
        cancelTime: null,
        refundReason: null,
        refundTime: null,
        ...overrides,
    };
}

describe('OrderInterventionBar', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('待付款订单给出取消与强制完成，不给退款', () => {
        render(<OrderInterventionBar order={order()} />);

        expect(screen.getByText('取消订单')).toBeInTheDocument();
        expect(screen.getByText('强制完成')).toBeInTheDocument();
        expect(screen.queryByText('退款')).not.toBeInTheDocument();
    });

    it('已付款已发货订单给退款，不给取消', () => {
        render(<OrderInterventionBar order={order({ status: 'SHIPPED', paymentStatus: 'PAID' })} />);

        expect(screen.getByText('退款')).toBeInTheDocument();
        expect(screen.queryByText('取消订单')).not.toBeInTheDocument();
        expect(screen.queryByText('强制完成')).not.toBeInTheDocument();
    });

    it('未支付时不给退款（对齐后端退款守卫）', () => {
        render(<OrderInterventionBar order={order({ status: 'PAID', paymentStatus: 'UNPAID' })} />);

        expect(screen.queryByText('退款')).not.toBeInTheDocument();
    });

    it('终态订单不渲染任何干预入口', () => {
        render(<OrderInterventionBar order={order({ status: 'COMPLETED', paymentStatus: 'PAID' })} />);

        expect(screen.queryByText('取消订单')).not.toBeInTheDocument();
        expect(screen.queryByText('强制完成')).not.toBeInTheDocument();
        expect(screen.queryByText('退款')).not.toBeInTheDocument();
    });

    it('填原因后确认才调 mutation', async () => {
        mockCancel.mockResolvedValue(undefined);
        render(<OrderInterventionBar order={order()} />);

        fireEvent.click(screen.getByText('取消订单'));
        // 未填原因时点确认不该发请求
        fireEvent.click(screen.getByText('确认'));
        expect(mockCancel).not.toHaveBeenCalled();

        fireEvent.change(screen.getByPlaceholderText('请填写原因'), { target: { value: '买家放弃' } });
        fireEvent.click(screen.getByText('确认'));

        await waitFor(() => {
            expect(mockCancel).toHaveBeenCalledWith({ id: 'o1', data: { reason: '买家放弃' } });
        });
    });
});
