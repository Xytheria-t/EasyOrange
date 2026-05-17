import { useQuery } from '@tanstack/react-query';
import { productApi } from '@/api/productApi';
import type { ProductSearchParams, ProductSearchResult } from '@/types';

export function useProductSearch(params: ProductSearchParams = {}) {
    return useQuery<ProductSearchResult>({
        queryKey: ['productSearch', params],
        queryFn: async () => {
            const response = await productApi.searchProducts(params);
            return response.data;
        },
        enabled: (params.keyword?.trim().length ?? 0) > 0,
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
