/**
 * @fileoverview 订单 API 模块
 */

import type { PageResult } from '../types/index.js';
import { request } from './core/request.js';

export interface OrderDetail {
    id: number;
    orderNo: string;
    productId: number;
    productTitle: string;
    productImage: string;
    price: number;
    quantity: number;
    totalAmount: number;
    status: string;
    paymentMethod: string | null;
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

export const orderApi = {
    getList(_params?: Record<string, unknown>) {
        return request<PageResult<Record<string, unknown>>>('/orders/list', {
            method: 'GET'
        });
    },

    getMyOrders(params?: Record<string, unknown>) {
        return request<PageResult<OrderDetail>>('/orders/my', {
            method: 'GET',
            params
        });
    },

    getSoldOrders(params?: Record<string, unknown>) {
        return request<PageResult<OrderDetail>>('/orders/sold', {
            method: 'GET',
            params
        });
    },

    getDetail(id: number) {
        return request<OrderDetail>(`/orders/${id}`);
    },

    cancel(id: number) {
        return request(`/orders/${id}/cancel`, {
            method: 'PUT'
        });
    },

    confirm(id: number) {
        return request(`/orders/${id}/confirm`, {
            method: 'PUT'
        });
    },

    receive(id: number) {
        return request(`/orders/${id}/receive`, {
            method: 'PUT'
        });
    },

    pay(id: number) {
        return request(`/orders/${id}/pay`, {
            method: 'PUT'
        });
    },

    ship(id: number) {
        return request(`/orders/${id}/ship`, {
            method: 'PUT'
        });
    }
};
