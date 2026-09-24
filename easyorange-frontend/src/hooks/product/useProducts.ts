import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo } from 'react';
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
    batch: (ids: string[]) => [...PRODUCT_KEYS.details(), 'batch', ...ids] as const,
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

/**
 * 批量取商品（AI 对话引用溯源用）。
 *
 * AI 侧的召回物（AssetHit）只有 id / 标题 / 价格 / 类目 / 成色描述，够不上
 * ProductCard 需要的完整 Product（图片、卖家、地点、原价、浏览量）。这里按
 * 召回出的 productId 走公开的 /products/batch 一次补全，而不是让后端把商品
 * 字段塞进 ES 文档 —— 那些字段变更频繁，索引侧多一份映射就要跟着重建一次。
 *
 * 返回顺序跟随入参：后端 batch 接口不保证顺序，AI 侧又按召回分排序，
 * 顺序错了卡片次序就会随机跳。
 */
export function useProductsByIds(ids: string[]) {
    // queryKey 需要稳定值：入参每次渲染都是新数组，直接进 key 会让缓存永不命中。
    // 依赖写 join 后的字符串而非 ids 本身 —— 同样的 id 集合重渲染时不再产出新 key
    const keySource = ids.join('|');
    const key = useMemo(() => [...new Set(keySource ? keySource.split('|') : [])].sort(), [keySource]);

    return useQuery({
        queryKey: PRODUCT_KEYS.batch(key),
        queryFn: async () => {
            const response = await productApi.getProductsByIds(key);
            const byId = new Map(response.data.map(raw => [raw.id, normalizeProduct(raw)]));
            return key.flatMap(id => {
                const product = byId.get(id);
                return product ? [product] : [];
            });
        },
        enabled: key.length > 0,
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

/** 上架 / 下架切换（我的发布卡片上的货架开关）——整棵 products 查询树失效，分组计数与详情一并刷新 */
export function useToggleProductShelf() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ id, online }: { id: string; online: boolean }) => {
            if (online) {
                await productApi.goOnline(id);
            } else {
                await productApi.goOffline(id);
            }
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: PRODUCT_KEYS.all });
        },
    });
}
