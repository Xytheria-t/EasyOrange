import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { productApi } from '@/api/productApi';
import { generateMockProducts } from '@/data/mockProducts';
import type { ProductQueryParams, CreateProductRequest, UpdateProductRequest, PageResult, Product } from '@/types';

const PRODUCT_KEYS = {
  all: ['products'] as const,
  lists: () => [...PRODUCT_KEYS.all, 'list'] as const,
  list: (params: ProductQueryParams) => [...PRODUCT_KEYS.lists(), params] as const,
  details: () => [...PRODUCT_KEYS.all, 'detail'] as const,
  detail: (id: number) => [...PRODUCT_KEYS.details(), id] as const,
};

export function useProducts(params: ProductQueryParams = {}) {
  return useQuery<PageResult<Product>>({
    queryKey: PRODUCT_KEYS.list(params),
    queryFn: async () => {
      try {
        const response = await productApi.getProducts(params);
        return response.data;
      } catch (error) {
        // 后端不可用时返回模拟数据
        console.warn('后端服务不可用，使用模拟数据', error);
        const mockItems = generateMockProducts(20);
        return {
          records: mockItems as Product[],
          total: mockItems.length,
          current: params.current ?? 1,
          size: params.size ?? 20,
          pages: 1,
        };
      }
    },
    staleTime: 2 * 60 * 1000,
  });
}

export function useProduct(id: number) {
  return useQuery({
    queryKey: PRODUCT_KEYS.detail(id),
    queryFn: async () => {
      const response = await productApi.getProductById(id);
      return response.data;
    },
    enabled: id > 0,
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

export function useUpdateProduct(id: number) {
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
    mutationFn: async (id: number) => {
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
