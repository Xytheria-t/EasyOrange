import type { PageResult, OrderQueryParams, CreateOrderRequest, OrderDetail } from '../types/index.js';
import { request } from './core/request.js';

export const orderApi = {
    createOrder(data: CreateOrderRequest) {
        return request<OrderDetail>('/orders', {
            method: 'POST',
            body: data
        });
    },

    getMyOrders(params?: OrderQueryParams) {
        return request<PageResult<OrderDetail>>('/orders/my', {
            method: 'GET',
            params: params as Record<string, unknown>
        });
    },

    getSoldOrders(params?: OrderQueryParams) {
        return request<PageResult<OrderDetail>>('/orders/sold', {
            method: 'GET',
            params: params as Record<string, unknown>
        });
    },

    getDetail(id: number) {
        return request<OrderDetail>(`/orders/${id}`);
    },

    cancel(id: number, reason?: string) {
        return request(`/orders/${id}/cancel`, {
            method: 'PUT',
            params: reason ? { reason } as Record<string, unknown> : undefined
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
    },

    refund(id: number, reason?: string) {
        return request(`/orders/${id}/refund`, {
            method: 'PUT',
            params: reason ? { reason } as Record<string, unknown> : undefined
        });
    }
};
