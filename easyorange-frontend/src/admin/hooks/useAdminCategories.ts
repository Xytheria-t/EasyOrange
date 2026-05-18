import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { adminApi } from '../api/adminApi';
import type { CategoryResponse, CategoryTreeResponse, CategoryCreateRequest, CategoryUpdateRequest } from '../types/admin';

export const ADMIN_CATEGORY_KEYS = {
  all: ['admin', 'categories'] as const,
  lists: () => [...ADMIN_CATEGORY_KEYS.all, 'list'] as const,
  tree: () => [...ADMIN_CATEGORY_KEYS.all, 'tree'] as const,
};

export function useAdminCategories() {
  return useQuery<CategoryResponse[]>({
    queryKey: ADMIN_CATEGORY_KEYS.lists(),
    queryFn: async () => {
      const response = await adminApi.getCategories();
      return response.data;
    },
    staleTime: 60 * 1000,
    gcTime: 5 * 60 * 1000,
    retry: 1,
  });
}

export function useAdminCategoryTree() {
  return useQuery<CategoryTreeResponse[]>({
    queryKey: ADMIN_CATEGORY_KEYS.tree(),
    queryFn: async () => {
      const response = await adminApi.getCategoryTree();
      return response.data;
    },
    staleTime: 60 * 1000,
    gcTime: 5 * 60 * 1000,
    retry: 1,
  });
}

export function useCreateCategory() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: CategoryCreateRequest) => {
      const response = await adminApi.createCategory(data);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ADMIN_CATEGORY_KEYS.all });
    },
  });
}

export function useUpdateCategory() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, data }: { id: number; data: CategoryUpdateRequest }) => {
      const response = await adminApi.updateCategory(id, data);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ADMIN_CATEGORY_KEYS.all });
    },
  });
}

export function useUpdateCategoryStatus() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, status }: { id: number; status: number }) => {
      const response = await adminApi.updateCategoryStatus(id, status);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ADMIN_CATEGORY_KEYS.all });
    },
  });
}

export function useDeleteCategory() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: number) => {
      const response = await adminApi.deleteCategory(id);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ADMIN_CATEGORY_KEYS.all });
    },
  });
}
