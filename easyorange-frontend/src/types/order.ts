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
    buyerId: number;
    buyerUsername: string;
    sellerId: number;
    sellerUsername: string;
    productId: number;
    productTitle: string;
    productImage: string;
    amount: number;
    status: number;
    statusDesc: string;
    address: string;
    phone: string;
    quantity: number;
    remark: string | null;
    createTime: string;
    updateTime: string;
}

export interface CreateOrderRequest {
    productId: number;
    quantity?: number;
    paymentMethod?: PaymentMethod;
    address?: string;
    phone?: string;
    remark?: string;
}

export interface OrderQueryParams {
    orderNo?: string;
    status?: number | OrderStatus;
    buyerId?: number;
    sellerId?: number;
    productId?: number;
    role?: 'buyer' | 'seller';
    pageNum?: number;
    pageSize?: number;
    current?: number;
    size?: number;
    sortField?: string;
    sortDirection?: 'asc' | 'desc';
}

export interface OrderDetail extends Order {
    product?: {
        id: number;
        title: string;
        images: string[];
        condition: string;
    };
}
