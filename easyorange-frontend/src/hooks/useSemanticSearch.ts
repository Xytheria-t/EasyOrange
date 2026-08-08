import { keepPreviousData, useQuery, useQueryClient } from '@tanstack/react-query';
import { useCallback, useRef, useState } from 'react';
import { aiApi } from '@/api/aiApi';
import type { Product } from '@/types';
import { normalizeProduct } from '@/utils/product';

interface UseSemanticSearchReturn {
    results: Product[];
    isSearching: boolean;
    isSemanticMode: boolean;
    total: number;
    error: string | null;
    hasSearched: boolean;
    search: (keyword: string, pageNum?: number, pageSize?: number) => Promise<void>;
    toggleSemanticMode: () => void;
}

export function useSemanticSearch(): UseSemanticSearchReturn {
    const queryClient = useQueryClient();
    const [keyword, setKeyword] = useState('');
    const [pageNum, setPageNum] = useState(1);
    const [pageSize, setPageSize] = useState(20);
    const [isSemanticMode, setIsSemanticMode] = useState(false);

    const keywordRef = useRef(keyword);
    keywordRef.current = keyword;
    const pageNumRef = useRef(pageNum);
    pageNumRef.current = pageNum;
    const pageSizeRef = useRef(pageSize);
    pageSizeRef.current = pageSize;

    const query = useQuery({
        queryKey: ['semantic-search', keyword, pageNum, pageSize],
        queryFn: async () => {
            const response = await aiApi.semanticSearch({ keyword, pageNum, pageSize });
            const rawRecords = response.data?.records ?? [];
            return {
                records: rawRecords.map(r => normalizeProduct(r)),
                total: response.data?.total ?? 0,
            };
        },
        enabled: isSemanticMode && !!keyword.trim(),
        placeholderData: keepPreviousData,
    });

    const search = useCallback(
        async (nextKeyword: string, nextPageNum = 1, nextPageSize = 20) => {
            const trimmed = nextKeyword.trim();
            if (!trimmed) {
                setKeyword('');
                setPageNum(1);
                return;
            }
            if (
                trimmed === keywordRef.current &&
                nextPageNum === pageNumRef.current &&
                nextPageSize === pageSizeRef.current
            ) {
                await queryClient.invalidateQueries({
                    queryKey: ['semantic-search', trimmed, nextPageNum, nextPageSize],
                    exact: true,
                });
                return;
            }
            setKeyword(trimmed);
            setPageNum(nextPageNum);
            setPageSize(nextPageSize);
        },
        [queryClient]
    );

    const toggleSemanticMode = useCallback(() => {
        setIsSemanticMode(prev => {
            if (prev) {
                setKeyword('');
                setPageNum(1);
            }
            return !prev;
        });
    }, []);

    return {
        results: query.data?.records ?? [],
        isSearching: query.isFetching,
        isSemanticMode,
        total: query.data?.total ?? 0,
        error: query.isError ? '语义搜索暂时不可用，请稍后重试或切换到关键词搜索' : null,
        hasSearched: query.isSuccess || query.isError,
        search,
        toggleSemanticMode,
    };
}
