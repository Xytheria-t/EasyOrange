import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { productApi } from '@/api/productApi';
import type {
    CreateProductRequest,
    PageResult,
    Product,
    ProductQueryParams,
    ProductStatus,
    UpdateProductRequest,
} from '@/types';
import { normalizeProduct } from '@/utils/product';

export const PRODUCT_KEYS = {
    all: ['products'] as const,
    lists: () => [...PRODUCT_KEYS.all, 'list'] as const,
    list: (params: ProductQueryParams) =>
        [
            ...PRODUCT_KEYS.lists(),
            params.pageNum,
            params.pageSize,
            params.keyword,
            params.categoryId,
            params.sort,
            params.priceMin,
            params.priceMax,
            params.conditions,
            params.hasDiscount,
        ] as const,
    infinite: (params: Omit<ProductQueryParams, 'pageNum'>) =>
        [
            ...PRODUCT_KEYS.lists(),
            'infinite',
            params.pageSize,
            params.keyword,
            params.categoryId,
            params.sort,
            params.priceMin,
            params.priceMax,
            params.conditions,
            params.hasDiscount,
        ] as const,
    details: () => [...PRODUCT_KEYS.all, 'detail'] as const,
    detail: (id: string) => [...PRODUCT_KEYS.details(), id] as const,
};

export function useProducts(params: ProductQueryParams = {}) {
    return useQuery<PageResult<Product>>({
        queryKey: PRODUCT_KEYS.list(params),
        queryFn: async () => {
            const response = await productApi.getProducts(params);
            const data = response.data;
            return {
                ...data,
                records: (data.records ?? []).map(r => normalizeProduct(r)),
            };
        },
        staleTime: 2 * 60 * 1000,
    });
}

export function useInfiniteProducts(params: Omit<ProductQueryParams, 'pageNum'> = {}) {
    const pageSize = params.pageSize ?? 20;

    return useInfiniteQuery<
        PageResult<Product>,
        Error,
        { pages: PageResult<Product>[]; pageParams: number[] },
        readonly unknown[],
        number
    >({
        queryKey: PRODUCT_KEYS.infinite(params),
        queryFn: async ({ pageParam }) => {
            const response = await productApi.getProducts({ ...params, pageNum: pageParam, pageSize });
            const data = response.data;
            return {
                ...data,
                records: (data.records ?? []).map(r => normalizeProduct(r)),
            };
        },
        initialPageParam: 1,
        getNextPageParam: lastPage => {
            const total = lastPage.total ?? 0;
            const currentCount = lastPage.records?.length ?? 0;
            const currentPage = lastPage.current ?? 1;

            // 如果当前页没有数据或已加载全部数据，返回 null 表示没有更多页
            if (currentCount === 0 || currentPage * pageSize >= total) {
                return null;
            }
            return currentPage + 1;
        },
        staleTime: 2 * 60 * 1000,
        maxPages: 5, // 限制缓存的页数，避免内存无限增长
    });
}

export function useProduct(id: string) {
    return useQuery({
        queryKey: PRODUCT_KEYS.detail(id),
        queryFn: async () => {
            const response = await productApi.getProductById(id);
            return normalizeProduct(response.data);
        },
        enabled: !!id,
        staleTime: 5 * 60 * 1000,
    });
}

export function useCreateProduct() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (data: CreateProductRequest) => {
            const response = await productApi.createProduct(data);
            return response.data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: PRODUCT_KEYS.lists() });
        },
    });
}

export function useUpdateProduct(id: string) {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (data: UpdateProductRequest) => {
            const response = await productApi.updateProduct(id, data);
            return response.data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: PRODUCT_KEYS.detail(id) });
            queryClient.invalidateQueries({ queryKey: PRODUCT_KEYS.lists() });
        },
    });
}

export function useDeleteProduct() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (id: string) => {
            await productApi.deleteProduct(id);
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: PRODUCT_KEYS.lists() });
        },
    });
}

export function useCategories() {
    return useQuery({
        queryKey: ['categories'],
        queryFn: async () => {
            const response = await productApi.getCategories();
            return response.data ?? [];
        },
        staleTime: 10 * 60 * 1000,
    });
}

export function useSimilarProducts(productId: string) {
    return useQuery({
        queryKey: ['similar-products', productId],
        queryFn: async () => {
            const response = await productApi.getSimilarProducts(productId);
            return (response.data ?? []).map(r => normalizeProduct(r));
        },
        enabled: !!productId,
        staleTime: 5 * 60 * 1000,
    });
}

export function useMyProducts(params: { pageNum?: number; pageSize?: number; status?: ProductStatus } = {}) {
    return useQuery<PageResult<Product>>({
        queryKey: [...PRODUCT_KEYS.all, 'my', params.pageNum, params.pageSize, params.status],
        queryFn: async () => {
            const response = await productApi.getMyProducts(params);
            const data = response.data;
            return {
                ...data,
                records: (data.records ?? []).map(r => normalizeProduct(r)),
            };
        },
        staleTime: 2 * 60 * 1000,
    });
}
