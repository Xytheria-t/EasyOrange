import { useCallback, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';

export interface SearchUrlState {
    keyword: string;
    filters: Record<string, string>;
    pageNum: number;
    aiEnabled: boolean;
}

export interface SearchUrlStateSetters {
    setKeyword: (keyword: string) => void;
    setFilters: (filters: Record<string, string>) => void;
    setFilterValue: (key: string, value: string | null) => void;
    setPageNum: (pageNum: number) => void;
    setAiEnabled: (enabled: boolean) => void;
    setState: (partial: Partial<SearchUrlState>) => void;
    reset: () => void;
}

const FILTERS_PARAM = 'filters';
const PAGE_PARAM = 'page';
const AI_PARAM = 'ai';
const KEYWORD_PARAM = 'keyword';

function serializeFilters(filters: Record<string, string>): string {
    return Object.entries(filters)
        .filter(([, value]) => value !== undefined && value !== null && value !== '')
        .map(([key, value]) => `${key}:${encodeURIComponent(value)}`)
        .join(',');
}

function parseFilters(raw: string | null): Record<string, string> {
    if (!raw) {
        return {};
    }
    const result: Record<string, string> = {};
    raw.split(',').forEach(segment => {
        const separatorIndex = segment.indexOf(':');
        if (separatorIndex <= 0) {
            return;
        }
        const key = segment.slice(0, separatorIndex);
        const value = segment.slice(separatorIndex + 1);
        if (key) {
            result[key] = decodeURIComponent(value);
        }
    });
    return result;
}

export function useSearchUrlState(): SearchUrlState & SearchUrlStateSetters {
    const [searchParams, setSearchParams] = useSearchParams();

    const state = useMemo<SearchUrlState>(() => {
        const keyword = searchParams.get(KEYWORD_PARAM) || '';
        const filters = parseFilters(searchParams.get(FILTERS_PARAM));
        const pageNum = Math.max(1, parseInt(searchParams.get(PAGE_PARAM) || '1', 10) || 1);
        const aiEnabled = searchParams.get(AI_PARAM) === '1';
        return { keyword, filters, pageNum, aiEnabled };
    }, [searchParams]);

    const updateParams = useCallback(
        (updater: (params: URLSearchParams) => URLSearchParams) => {
            setSearchParams(prev => {
                const next = new URLSearchParams(prev);
                return updater(next);
            });
        },
        [setSearchParams]
    );

    const setKeyword = useCallback(
        (keyword: string) => {
            updateParams(params => {
                if (keyword) {
                    params.set(KEYWORD_PARAM, keyword);
                } else {
                    params.delete(KEYWORD_PARAM);
                }
                return params;
            });
        },
        [updateParams]
    );

    const setFilters = useCallback(
        (filters: Record<string, string>) => {
            updateParams(params => {
                const serialized = serializeFilters(filters);
                if (serialized) {
                    params.set(FILTERS_PARAM, serialized);
                } else {
                    params.delete(FILTERS_PARAM);
                }
                params.set(PAGE_PARAM, '1');
                return params;
            });
        },
        [updateParams]
    );

    const setFilterValue = useCallback(
        (key: string, value: string | null) => {
            updateParams(params => {
                const current = parseFilters(params.get(FILTERS_PARAM));
                const next = { ...current };
                if (value === null || value === undefined || value === '') {
                    delete next[key];
                } else {
                    next[key] = value;
                }
                const serialized = serializeFilters(next);
                if (serialized) {
                    params.set(FILTERS_PARAM, serialized);
                } else {
                    params.delete(FILTERS_PARAM);
                }
                params.set(PAGE_PARAM, '1');
                return params;
            });
        },
        [updateParams]
    );

    const setPageNum = useCallback(
        (pageNum: number) => {
            updateParams(params => {
                const normalized = Math.max(1, pageNum);
                params.set(PAGE_PARAM, String(normalized));
                return params;
            });
        },
        [updateParams]
    );

    const setAiEnabled = useCallback(
        (enabled: boolean) => {
            updateParams(params => {
                if (enabled) {
                    params.set(AI_PARAM, '1');
                } else {
                    params.delete(AI_PARAM);
                }
                return params;
            });
        },
        [updateParams]
    );

    const setState = useCallback(
        (partial: Partial<SearchUrlState>) => {
            updateParams(params => {
                if ('keyword' in partial) {
                    const keyword = partial.keyword ?? '';
                    if (keyword) {
                        params.set(KEYWORD_PARAM, keyword);
                    } else {
                        params.delete(KEYWORD_PARAM);
                    }
                }
                if ('filters' in partial) {
                    const serialized = serializeFilters(partial.filters ?? {});
                    if (serialized) {
                        params.set(FILTERS_PARAM, serialized);
                    } else {
                        params.delete(FILTERS_PARAM);
                    }
                }
                if ('pageNum' in partial) {
                    params.set(PAGE_PARAM, String(Math.max(1, partial.pageNum ?? 1)));
                }
                if ('aiEnabled' in partial) {
                    if (partial.aiEnabled) {
                        params.set(AI_PARAM, '1');
                    } else {
                        params.delete(AI_PARAM);
                    }
                }
                return params;
            });
        },
        [updateParams]
    );

    const reset = useCallback(() => {
        updateParams(params => {
            params.delete(KEYWORD_PARAM);
            params.delete(FILTERS_PARAM);
            params.delete(PAGE_PARAM);
            params.delete(AI_PARAM);
            return params;
        });
    }, [updateParams]);

    return {
        ...state,
        setKeyword,
        setFilters,
        setFilterValue,
        setPageNum,
        setAiEnabled,
        setState,
        reset,
    };
}
