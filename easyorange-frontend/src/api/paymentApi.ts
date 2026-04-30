import { request } from './core/request.js';
import type { PaymentMethod } from '../types/index.js';

export interface PaymentInfo {
    id: number;
    paymentNo: string;
    orderId: number;
    amount: number;
    status: string;
    paymentMethod: PaymentMethod | null;
    expireTime: string;
    createTime: string;
}

export interface CreatePaymentRequest {
    orderId: number;
    paymentMethod: PaymentMethod;
}

export interface PaymentResponse {
    paymentId: number;
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

    getPaymentById(id: number) {
        return request<PaymentInfo>(`/payments/${id}`);
    },

    getMyPayments(params?: Record<string, unknown>) {
        return request<PaymentInfo[]>('/payments/my', {
            method: 'GET',
            params
        });
    },

    getPaymentByOrder(orderId: number) {
        return request<PaymentInfo>(`/payments/orders/${orderId}`);
    },

    getPaymentStatus(id: number) {
        return request<PaymentStatusResponse>(`/payments/${id}/status`);
    },

    refund(id: number) {
        return request(`/payments/${id}/refund`, {
            method: 'POST'
        });
    },

    close(id: number) {
        return request(`/payments/${id}/close`, {
            method: 'POST'
        });
    }
};
