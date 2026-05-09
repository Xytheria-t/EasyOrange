/**
 * @fileoverview 商品 API 模块
 */

import type { PageResult, Category, Product, ProductQueryParams, CreateProductRequest, UpdateProductRequest } from '@/types';
import { request } from './core/request';

export const productApi = {
    getProducts(params?: ProductQueryParams) {
        return request<PageResult<Product>>('/products', {
            method: 'GET',
            params: params as Record<string, unknown>,
            skipAuth: true
        });
    },

    getProductById(id: string) {
        return request<Product>(`/products/${id}`);
    },

    createProduct(data: CreateProductRequest) {
        return request<string>('/products', {
            method: 'POST',
            body: data
        });
    },

    updateProduct(id: string, data: UpdateProductRequest) {
        return request<string>(`/products/${id}`, {
            method: 'PUT',
            body: data
        });
    },

    deleteProduct(id: string) {
        return request<void>(`/products/${id}`, {
            method: 'DELETE'
        });
    },

    putOnline(id: string) {
        return request<void>(`/products/${id}/online`, {
            method: 'PUT'
        });
    },

    takeOffline(id: string) {
        return request<void>(`/products/${id}/offline`, {
            method: 'PUT'
        });
    },

    getCategories(parentId?: string) {
        return request<Category[]>('/products/categories', {
            method: 'GET',
            params: parentId != null ? { parentId } : undefined,
            skipAuth: true
        });
    },

    getProductsByCategory(category: string | number) {
        return request<PageResult<Product>>(`/products/category/${category}`, { skipAuth: true });
    },

    searchProducts(keyword: string, pageNum?: number, pageSize?: number) {
        return request<PageResult<Product>>('/products/search', {
            method: 'GET',
            params: { keyword, pageNum, pageSize } as Record<string, unknown>,
            skipAuth: true
        });
    },

    getSearchSuggestions(keyword: string) {
        return request<string[]>('/products/search/suggestions', {
            method: 'GET',
            params: { keyword },
            skipAuth: true
        });
    },

    getHotKeywords(limit?: number) {
        return request<Array<{ keyword: string; searchCount: number }>>('/products/search/hot', {
            method: 'GET',
            params: limit != null ? { limit } : undefined,
            skipAuth: true
        });
    },

    getProductsByIds(ids: string[]) {
        return request<Product[]>('/products/batch', {
            method: 'POST',
            body: ids,
            skipAuth: true
        });
    },

    getSimilarProducts(id: string) {
        return request<Product[]>(`/products/${id}/similar`, { skipAuth: true });
    },

    incrementView(id: string) {
        return request<void>(`/products/${id}/view`, {
            method: 'POST',
            skipAuth: true
        });
    }
};
