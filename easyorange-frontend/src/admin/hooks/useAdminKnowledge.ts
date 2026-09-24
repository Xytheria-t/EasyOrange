import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { adminApi } from '../api/adminApi';
import type { CreateKnowledgeDocRequest } from '../types/admin';

export const ADMIN_KNOWLEDGE_KEYS = {
    all: ['admin', 'knowledge'] as const,
    list: (pageNum: number, pageSize: number) => [...ADMIN_KNOWLEDGE_KEYS.all, 'list', pageNum, pageSize] as const,
};

export function useAdminKnowledgeDocs(pageNum: number, pageSize: number) {
    return useQuery({
        queryKey: ADMIN_KNOWLEDGE_KEYS.list(pageNum, pageSize),
        queryFn: async () => {
            const response = await adminApi.getKnowledgeDocs(pageNum, pageSize);
            return response.data;
        },
        // v5 里 keepPreviousData 改名 placeholderData：翻页 / 换筛选时沿用上一页数据，
        // 否则表格会退回骨架屏再整页重排
        placeholderData: keepPreviousData,
        staleTime: 60 * 1000,
        gcTime: 5 * 60 * 1000,
        retry: 1,
    });
}

export function useCreateKnowledgeDoc() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (data: CreateKnowledgeDocRequest) => {
            const response = await adminApi.createKnowledgeDoc(data);
            return response.data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ADMIN_KNOWLEDGE_KEYS.all });
        },
    });
}

export function useDeleteKnowledgeDoc() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (id: string) => {
            const response = await adminApi.deleteKnowledgeDoc(id);
            return response.data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ADMIN_KNOWLEDGE_KEYS.all });
        },
    });
}

export function useReindexKnowledge() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async () => {
            const response = await adminApi.reindexKnowledge();
            return response.data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ADMIN_KNOWLEDGE_KEYS.all });
        },
    });
}
