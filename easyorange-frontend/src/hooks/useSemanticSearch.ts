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
    setResults: (results: Product[]) => void;
    clearError: () => void;
}

export function useSemanticSearch(): UseSemanticSearchReturn {
    const [results, setResults] = useState<Product[]>([]);
    const [total, setTotal] = useState(0);
    const [isSearching, setIsSearching] = useState(false);
    const [isSemanticMode, setIsSemanticMode] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [hasSearched, setHasSearched] = useState(false);
    const abortRef = useRef<AbortController | null>(null);

    const search = useCallback(async (keyword: string, pageNum = 1, pageSize = 20) => {
        if (!keyword.trim()) {
            setResults([]);
            setTotal(0);
            setHasSearched(false);
            setError(null);
            return;
        }

        if (abortRef.current) {
            abortRef.current.abort();
        }
        abortRef.current = new AbortController();

        setIsSearching(true);
        setError(null);
        setHasSearched(false);

        try {
            const response = await aiApi.semanticSearch({ keyword, pageNum, pageSize });
            const rawRecords = response.data?.records ?? [];
            const normalized = rawRecords.map(r => normalizeProduct(r));
            setResults(normalized);
            setTotal(response.data?.total ?? 0);
            setHasSearched(true);
        } catch (err: unknown) {
            if (err instanceof Error && err.name === 'AbortError') {
                return;
            }
            setResults([]);
            setTotal(0);
            setHasSearched(true);
            setError('语义搜索暂时不可用，请稍后重试或切换到关键词搜索');
        } finally {
            setIsSearching(false);
        }
    }, []);

    const toggleSemanticMode = useCallback(() => {
        setIsSemanticMode(prev => {
            if (prev) {
                setResults([]);
                setTotal(0);
                setError(null);
                setHasSearched(false);
            }
            return !prev;
        });
    }, []);

    const clearError = useCallback(() => {
        setError(null);
    }, []);

    return {
        results,
        isSearching,
        isSemanticMode,
        total,
        error,
        hasSearched,
        search,
        toggleSemanticMode,
        setResults,
        clearError,
    };
}
