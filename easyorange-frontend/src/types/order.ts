export type OrderStatus = 'PENDING_PAYMENT' | 'PAID' | 'SHIPPED' | 'COMPLETED' | 'CANCELLED' | 'REFUNDED';

export type PaymentMethod = 'WECHAT' | 'ALIPAY' | 'CAMPUS_CARD' | 'CASH';

export interface OrderItemVO {
    itemId: string;
    productId: string;
    productName: string;
    productImage: string;
    unitPrice: number;
    quantity: number;
    subtotal: number;
}

export interface Order {
    id: string;
    orderNo: string;
    buyerId: string;
    buyerUsername: string;
    sellerId: string;
    sellerUsername: string;
    items: OrderItemVO[];
    totalAmount: number;
    singleItem: boolean;
    status: number;
    statusDesc: string;
    address: string;
    phone: string;
    remark: string | null;
    createTime: string;
    updateTime: string;
}

export interface CreateOrderRequest {
    items: { productId: string; quantity: number }[];
    address?: string;
    phone?: string;
    remark?: string;
}

export interface OrderQueryParams {
    orderNo?: string;
    status?: number | OrderStatus;
    buyerId?: string;
    sellerId?: string;
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
        id: string;
        title: string;
        images: string[];
        condition: string;
    };
}
