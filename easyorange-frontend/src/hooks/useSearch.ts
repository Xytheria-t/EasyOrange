import { useQuery } from '@tanstack/react-query';
import { productApi } from '@/api/productApi';
import type { ProductQueryParams, PageResult, Product } from '@/types';

export function useProductSearch(keyword: string, params?: Omit<ProductQueryParams, 'keyword'>) {
    return useQuery<PageResult<Product>>({
        queryKey: ['productSearch', keyword, params],
        queryFn: async () => {
            const response = await productApi.searchProducts(keyword, params?.current, params?.size);
            return response.data;
        },
        enabled: keyword.trim().length > 0,
        staleTime: 30 * 1000,
    });
}

export function useSearchSuggestions(keyword: string) {
    return useQuery<string[]>({
        queryKey: ['searchSuggestions', keyword],
        queryFn: async () => {
            const response = await productApi.getSearchSuggestions(keyword);
            return response.data ?? [];
        },
        enabled: keyword.trim().length >= 2,
        staleTime: 10 * 1000,
    });
}

export function useHotKeywords(limit = 10) {
    return useQuery<Array<{ keyword: string; searchCount: number }>>({
        queryKey: ['hotKeywords', limit],
        queryFn: async () => {
            const response = await productApi.getHotKeywords(limit);
            return response.data ?? [];
        },
        staleTime: 5 * 60 * 1000,
    });
}
