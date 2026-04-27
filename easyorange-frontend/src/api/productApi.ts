/**
 * @fileoverview 商品 API 模块
 */

import type { PageResult, Category, Product, ProductQueryParams, CreateProductRequest, UpdateProductRequest } from '../types/index.js';
import { request } from './core/request';

export const productApi = {
    getProducts(params?: ProductQueryParams) {
        return request<PageResult<Product>>('/products', {
            method: 'GET',
            params: params as Record<string, unknown>
        });
    },

    getProductById(id: number) {
        return request<Product>(`/products/${id}`);
    },

    getProductDetail(id: number) {
        return request<Product>(`/products/${id}`);
    },

    createProduct(data: CreateProductRequest) {
        return request<Product>('/products', {
            method: 'POST',
            body: data
        });
    },

    updateProduct(id: number, data: UpdateProductRequest) {
        return request<Product>(`/products/${id}`, {
            method: 'PUT',
            body: data
        });
    },

    deleteProduct(id: number) {
        return request<void>(`/products/${id}`, {
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

    searchProducts(keyword: string) {
        return request<PageResult<Product>>('/products/search', {
            method: 'GET',
            params: { keyword }
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
