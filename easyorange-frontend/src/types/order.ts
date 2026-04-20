export type OrderStatus =
    | 'PENDING_PAYMENT'
    | 'PAID'
    | 'SHIPPED'
    | 'COMPLETED'
    | 'CANCELLED'
    | 'REFUNDED';

export type PaymentMethod = 'WECHAT' | 'ALIPAY' | 'CAMPUS_CARD' | 'CASH';

export interface Order {
    id: number;
    orderNo: string;
    productId: number;
    productTitle: string;
    productImage: string;
    price: number;
    quantity: number;
    totalAmount: number;
    status: OrderStatus;
    paymentMethod: PaymentMethod | null;
    buyerId: number;
    buyerName: string;
    buyerAvatar: string | null;
    sellerId: number;
    sellerName: string;
    sellerAvatar: string | null;
    createTime: string;
    payTime: string | null;
    shipTime: string | null;
    completeTime: string | null;
    cancelTime: string | null;
    cancelReason: string | null;
}

export interface OrderDetail extends Order {
    product: {
        id: number;
        title: string;
        images: string[];
        condition: string;
    };
    address: OrderAddress | null;
    logistics: OrderLogistics | null;
}

export interface OrderAddress {
    receiverName: string;
    receiverPhone: string;
    province: string;
    city: string;
    district: string;
    detail: string;
}

export interface OrderLogistics {
    company: string;
    trackingNo: string;
    status: string;
    traces: LogisticsTrace[];
}

export interface LogisticsTrace {
    time: string;
    content: string;
}

export interface CreateOrderRequest {
    productId: number;
    address: string;
    phone: string;
    remark?: string;
}

export interface OrderQueryParams {
    status?: OrderStatus;
    role?: 'buyer' | 'seller';
    current?: number;
    size?: number;
}

export interface OrderActionRequest {
    orderId: number;
    action: 'cancel' | 'confirm' | 'ship' | 'complete';
    reason?: string;
    trackingNo?: string;
    logisticsCompany?: string;
}
