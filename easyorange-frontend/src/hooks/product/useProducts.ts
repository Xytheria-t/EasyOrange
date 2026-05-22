import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { productApi } from '@/api/productApi';
import type { ProductQueryParams, CreateProductRequest, UpdateProductRequest, PageResult, Product } from '@/types';
import { normalizeProduct } from '@/utils/product';

export const PRODUCT_KEYS = {
  all: ['products'] as const,
  lists: () => [...PRODUCT_KEYS.all, 'list'] as const,
  list: (params: ProductQueryParams) =>
    [...PRODUCT_KEYS.lists(),
      params.pageNum, params.pageSize, params.keyword,
      params.categoryId, params.sort, params.priceMin,
      params.priceMax, params.conditions,
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
        records: (data.records ?? []).map((r) => normalizeProduct(r as unknown as Record<string, unknown>)),
      };
    },
    staleTime: 2 * 60 * 1000,
  });
}

export function useProduct(id: string) {
  return useQuery({
    queryKey: PRODUCT_KEYS.detail(id),
    queryFn: async () => {
      const response = await productApi.getProductById(id);
      return normalizeProduct(response.data as unknown as Record<string, unknown>);
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
      return (response.data ?? []).map((r) => normalizeProduct(r as unknown as Record<string, unknown>));
    },
    enabled: !!productId,
    staleTime: 5 * 60 * 1000,
  });
}

export function useMyProducts(params: { pageNum?: number; pageSize?: number; status?: number } = {}) {
  return useQuery<PageResult<Product>>({
    queryKey: [...PRODUCT_KEYS.all, 'my', params.pageNum, params.pageSize, params.status],
    queryFn: async () => {
      const response = await productApi.getMyProducts(params);
      const data = response.data;
      return {
        ...data,
        records: (data.records ?? []).map((r) => normalizeProduct(r as unknown as Record<string, unknown>)),
      };
    },
    staleTime: 2 * 60 * 1000,
  });
}
