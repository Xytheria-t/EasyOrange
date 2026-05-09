import type { OrderStatus } from '@/types/order';

export const ORDER_STATUS_CODE: Record<number, OrderStatus> = {
    0: 'PENDING_PAYMENT',
    1: 'PAID',
    2: 'SHIPPED',
    3: 'COMPLETED',
    4: 'CANCELLED',
    5: 'REFUNDED',
};

export const ORDER_STATUS_LABEL: Record<OrderStatus, string> = {
    PENDING_PAYMENT: '待付款',
    PAID: '待发货',
    SHIPPED: '已发货',
    COMPLETED: '已完成',
    CANCELLED: '已取消',
    REFUNDED: '已退款',
};

export const getOrderStatusLabel = (status: number | OrderStatus): string => {
    if (typeof status === 'number') {
        const key = ORDER_STATUS_CODE[status];
        return key ? ORDER_STATUS_LABEL[key] : '未知状态';
    }
    return ORDER_STATUS_LABEL[status] ?? '未知状态';
};

export const getOrderStatusFromCode = (code: number): OrderStatus => {
    return ORDER_STATUS_CODE[code] ?? 'PENDING_PAYMENT';
};
