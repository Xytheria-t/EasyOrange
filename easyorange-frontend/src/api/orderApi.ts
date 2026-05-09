import type { PageResult, OrderQueryParams, CreateOrderRequest, OrderDetail } from '@/types';
import { request } from './core/request';

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

    getDetail(id: string) {
        return request<OrderDetail>(`/orders/${id}`);
    },

    cancel(id: string, reason?: string) {
        return request(`/orders/${id}/cancel`, {
            method: 'PUT',
            params: reason ? { reason } as Record<string, unknown> : undefined
        });
    },

    receive(id: string) {
        return request(`/orders/${id}/receive`, {
            method: 'PUT'
        });
    },

    pay(id: string) {
        return request(`/orders/${id}/pay`, {
            method: 'PUT'
        });
    },

    ship(id: string) {
        return request(`/orders/${id}/ship`, {
            method: 'PUT'
        });
    },

    refund(id: string, reason?: string) {
        return request(`/orders/${id}/refund`, {
            method: 'PUT',
            params: reason ? { reason } as Record<string, unknown> : undefined
        });
    }
};
