import { useState, useCallback } from 'react';
import { aiApi } from '@/api/aiApi';
import { normalizeProduct } from '@/utils/product';
import type { Product } from '@/types';

interface UseSemanticSearchReturn {
  results: Product[];
  isSearching: boolean;
  isSemanticMode: boolean;
  total: number;
  search: (keyword: string, pageNum?: number, pageSize?: number) => Promise<void>;
  toggleSemanticMode: () => void;
  setResults: (results: Product[]) => void;
}

export function useSemanticSearch(): UseSemanticSearchReturn {
  const [results, setResults] = useState<Product[]>([]);
  const [total, setTotal] = useState(0);
  const [isSearching, setIsSearching] = useState(false);
  const [isSemanticMode, setIsSemanticMode] = useState(false);

  const search = useCallback(async (keyword: string, pageNum = 1, pageSize = 20) => {
    if (!keyword.trim()) {
      setResults([]);
      setTotal(0);
      return;
    }

    setIsSearching(true);
    try {
      const response = await aiApi.semanticSearch({ keyword, pageNum, pageSize });
      const rawRecords = response.data?.records ?? [];
      const normalized = rawRecords.map((r) =>
        normalizeProduct(r as Record<string, unknown>)
      );
      setResults(normalized);
      setTotal(response.data?.total ?? 0);
    } catch {
      setResults([]);
      setTotal(0);
    } finally {
      setIsSearching(false);
    }
  }, []);

  const toggleSemanticMode = useCallback(() => {
    setIsSemanticMode((prev) => !prev);
  }, []);

  return {
    results,
    isSearching,
    isSemanticMode,
    total,
    search,
    toggleSemanticMode,
    setResults,
  };
}