import { request } from './core/request';
import type { PageResult, PaymentMethod } from '@/types';

export interface PaymentInfo {
    id: string;
    paymentNo: string;
    orderId: string;
    amount: number;
    status: string;
    paymentMethod: PaymentMethod | null;
    expireTime: string;
    createTime: string;
}

export interface CreatePaymentRequest {
    orderId: string;
    paymentMethod: PaymentMethod;
}

export interface PaymentResponse {
    paymentId: string;
    paymentNo: string;
    payUrl?: string;
    qrCodeUrl?: string;
    status: string;
}

export interface PaymentStatusResponse {
    status: string;
    paymentMethod: PaymentMethod | null;
    payTime: string | null;
}

export const paymentApi = {
    createPayment(data: CreatePaymentRequest) {
        return request<PaymentResponse>('/payments', {
            method: 'POST',
            body: data
        });
    },

    getPaymentById(id: string) {
        return request<PaymentInfo>(`/payments/${id}`);
    },

    getMyPayments(params?: Record<string, unknown>) {
        return request<PageResult<PaymentInfo>>('/payments/my', {
            method: 'GET',
            params
        });
    },

    getPaymentByOrder(orderId: string) {
        return request<PaymentInfo>(`/payments/orders/${orderId}`);
    },

    getPaymentStatus(id: string) {
        return request<PaymentStatusResponse>(`/payments/${id}/status`);
    },

    refundPayment(id: string) {
        return request(`/payments/${id}/refund`, {
            method: 'POST'
        });
    },

    closePayment(id: string) {
        return request(`/payments/${id}/close`, {
            method: 'POST'
        });
    }
};
