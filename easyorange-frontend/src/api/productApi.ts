/**
 * @fileoverview 商品 API 模块
 */

import type { PageResult, Category, Product } from '../types/index.js';
import { request } from './core/request.js';

export const productApi = {
    getList(params?: Record<string, unknown>) {
        return request<PageResult<Product>>('/products', {
            method: 'GET',
            params
        });
    },

    getProducts(params?: Record<string, unknown>) {
        return request<PageResult<Product>>('/products', {
            method: 'GET',
            params
        });
    },

    getDetail(id: number) {
        return request<Product>(`/products/${id}`);
    },

    getProductDetail(id: number) {
        return request<Product>(`/products/${id}`);
    },

    create(data: Record<string, unknown>) {
        return request('/products', {
            method: 'POST',
            body: data
        });
    },

    createProduct(data: Record<string, unknown> | { title: string; description: string; price: number; originalPrice?: number; condition?: string; location?: string; images?: string[]; categoryId?: number }) {
        return request('/products', {
            method: 'POST',
            body: data as Record<string, unknown>
        });
    },

    update(id: number, data: Record<string, unknown>) {
        return request(`/products/${id}`, {
            method: 'PUT',
            body: data
        });
    },

    delete(id: number) {
        return request(`/products/${id}`, {
            method: 'DELETE'
        });
    },

    getCategories() {
        return request<Category[]>('/products/categories');
    },

    getCategoryTree() {
        return request<Category[]>('/products/categories/tree');
    },

    getProductsByCategory(category: string | number) {
        return request<PageResult<Product>>(`/products/category/${category}`);
    },

    searchProducts(_keyword: string) {
        return request<PageResult<Product>>('/products/search', {
            method: 'GET'
        });
    },

    getProductsByIds(ids: number[]) {
        return request<Product[]>('/products/by-ids', {
            method: 'POST',
            body: { ids }
        });
    },

    getPriceHistory(id: number) {
        return request<{ date: string; price: number }[]>(`/products/${id}/price-history`);
    },

    getSimilarProducts(id: number) {
        return request<Product[]>(`/products/${id}/similar`);
    }
};
