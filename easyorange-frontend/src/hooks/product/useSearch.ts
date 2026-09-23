import { useQuery } from '@tanstack/react-query';
import { productApi } from '@/api/productApi';
import type { AiEnhancement, Product, ProductSearchParams, ProductSearchResult } from '@/types/product';
import { normalizeProduct } from '@/utils/product';

export interface UseProductSearchResult {
    products: Product[];
    total: number;
    facets: import('@/types/product').FacetBucket[];
    aiEnhancement?: AiEnhancement;
    /** 已开启增强但本次失败（尝试过才为 true，短关键词「不适用」不会误报） */
    aiEnhancementDegraded: boolean;
    isLoading: boolean;
    error: Error | null;
}

export function useProductSearch(params: ProductSearchParams = {}): UseProductSearchResult {
    const query = useQuery<ProductSearchResult>({
        queryKey: ['productSearch', params],
        queryFn: async () => {
            const response = await productApi.searchProducts(params);
            return response.data;
        },
        enabled: (params.keyword?.trim().length ?? 0) > 0,
        staleTime: 30 * 1000,
    });

    return {
        // 与商品列表链路同一归一（username→sellerName、images/mainImageUrl 兜底等）
        products: (query.data?.records ?? []).map(normalizeProduct),
        total: query.data?.total ?? 0,
        facets: query.data?.facets ?? [],
        aiEnhancement: query.data?.aiEnhancement,
        aiEnhancementDegraded: query.data?.aiEnhancementDegraded ?? false,
        isLoading: query.isLoading,
        error: query.error,
    };
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
