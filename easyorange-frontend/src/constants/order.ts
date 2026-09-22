import type { OrderStatus } from '@/types/order';

export const ORDER_STATUS_LABEL: Record<OrderStatus, string> = {
    PENDING_PAYMENT: '待付款',
    PAID: '待发货',
    SHIPPED: '已发货',
    COMPLETED: '已完成',
    CANCELLED: '已取消',
    REFUNDED: '已退款',
};

export const getOrderStatusLabel = (status: OrderStatus): string => ORDER_STATUS_LABEL[status] ?? '未知状态';
