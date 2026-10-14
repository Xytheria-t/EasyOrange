/**
 * @fileoverview 商品 API 模块
 */

import type {
    Category,
    CreateProductRequest,
    PageResult,
    ProductQueryParams,
    ProductStatus,
    RawProduct,
    UpdateProductRequest,
} from '@/types';
import { request } from './core/request';

/**
 * 后端把 `long`/`Long` 全局序列化成字符串（JS 精度安全，见后端 `JacksonConfig`），
 * 所以线上 `total` 是 `"70"` 而不是 `70`。类型声明是 `number`，不收敛就是**类型谎言**：
 * 关系比较（`>=`）会隐式转型侥幸能用，但任何 `+` 会静默变成字符串拼接。
 * 口径与 adminApi 的 `coercePageTotal` 一致。
 */
function coercePageTotal<T extends { total: number }>(res: T): T {
    return { ...res, total: Number(res.total ?? 0) };
}

export const productApi = {
    async getProducts(params?: ProductQueryParams) {
        const res = await request<PageResult<RawProduct>>('/products', {
            method: 'GET',
            params: params as Record<string, unknown>,
            skipAuth: true,
        });
        return res.data ? { ...res, data: coercePageTotal(res.data) } : res;
    },

    getProductById(id: string) {
        return request<RawProduct>(`/products/${id}`);
    },

    createProduct(data: CreateProductRequest) {
        return request<string>('/products', {
            method: 'POST',
            body: data,
        });
    },

    updateProduct(id: string, data: UpdateProductRequest) {
        return request<string>(`/products/${id}`, {
            method: 'PUT',
            body: data,
        });
    },

    deleteProduct(id: string) {
        return request<void>(`/products/${id}`, {
            method: 'DELETE',
        });
    },

    goOnline(id: string) {
        return request<void>(`/products/${id}/online`, {
            method: 'PUT',
        });
    },

    goOffline(id: string) {
        return request<void>(`/products/${id}/offline`, {
            method: 'PUT',
        });
    },

    getCategories(parentId?: string) {
        return request<Category[]>('/products/categories', {
            method: 'GET',
            params: parentId != null ? { parentId } : undefined,
            skipAuth: true,
        });
    },

    getProductsByIds(ids: string[]) {
        return request<RawProduct[]>('/products/batch', {
            method: 'POST',
            body: ids,
            skipAuth: true,
        });
    },

    getSimilarProducts(id: string) {
        return request<RawProduct[]>(`/products/${id}/similar`, { skipAuth: true });
    },

    incrementView(id: string) {
        return request<void>(`/products/${id}/view`, {
            method: 'POST',
            skipAuth: true,
        });
    },

    submitForReview(id: string) {
        return request<void>(`/products/${id}/submit`, {
            method: 'PUT',
        });
    },

    async getMyProducts(params?: { pageNum?: number; pageSize?: number; status?: ProductStatus }) {
        const res = await request<PageResult<RawProduct>>('/products/my', {
            method: 'GET',
            params: params as Record<string, unknown>,
        });
        return res.data ? { ...res, data: coercePageTotal(res.data) } : res;
    },
};
