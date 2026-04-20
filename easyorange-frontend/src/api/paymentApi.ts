/**
 * @fileoverview 支付 API 模块
 */

import { request } from './core/request.js';

export interface PaymentInfo {
    id: number;
    orderId: number;
    amount: number;
    expireTime: string;
}

export interface PaymentResponse {
    payUrl?: string;
    qrCodeUrl?: string;
    status: string;
}

export interface PaymentStatusResponse {
    status: string;
}

export const paymentApi = {
    getPaymentByOrder(orderId: number) {
        return request<PaymentInfo>(`/payment/orders/${orderId}`);
    },

    createPayment(orderId: number, method: string) {
        return request<PaymentResponse>('/payment/create', {
            method: 'POST',
            body: { orderId, paymentMethod: method }
        });
    },

    getPaymentStatus(orderId: number) {
        return request<PaymentStatusResponse>(`/payment/${orderId}/status`);
    },

    queryPaymentStatus(orderId: number) {
        return this.getPaymentStatus(orderId);
    }
};
