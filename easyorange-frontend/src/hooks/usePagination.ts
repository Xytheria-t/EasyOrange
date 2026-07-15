import { useCallback, useEffect, useState } from 'react';

interface UsePaginationConfig {
    pageSize?: number;
    resetDeps?: unknown[];
}

interface UsePaginationReturn {
    pageNum: number;
    pageSize: number;
    toNext: () => void;
    toPrev: () => void;
    goTo: (page: number) => void;
    totalPages: (total: number) => number;
    setPageNum: React.Dispatch<React.SetStateAction<number>>;
}

export function usePagination(config?: UsePaginationConfig): UsePaginationReturn {
    const [pageNum, setPageNum] = useState(1);
    const pageSize = config?.pageSize ?? 10;

    useEffect(() => {
        setPageNum(1);
        // biome-ignore lint/correctness/useExhaustiveDependencies: dynamic reset deps from consumer
    }, config?.resetDeps ?? []);

    const toNext = useCallback(() => setPageNum(p => p + 1), []);
    const toPrev = useCallback(() => setPageNum(p => Math.max(1, p - 1)), []);
    const goTo = useCallback((page: number) => setPageNum(page), []);

    const totalPages = useCallback((total: number) => Math.max(1, Math.ceil(total / pageSize)), [pageSize]);

    return { pageNum, pageSize, toNext, toPrev, goTo, totalPages, setPageNum };
}
