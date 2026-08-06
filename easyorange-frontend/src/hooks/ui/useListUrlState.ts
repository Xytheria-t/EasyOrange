import { useCallback, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';

/**
 * 通用列表页 URL 状态 hook。
 *
 * 将 keyword / filters / pageNum 三个查询参数同步到 URL，支持分享与刷新保留。
 * filters 以 `key:value,key:value` 形式序列化到单个 `filters` 参数中。
 */
export interface ListUrlState {
    keyword: string;
    filters: Record<string, string>;
    pageNum: number;
}

export interface ListUrlStateSetters {
    setKeyword: (keyword: string) => void;
    setFilters: (filters: Record<string, string>) => void;
    setFilterValue: (key: string, value: string | null) => void;
    setPageNum: (pageNum: number) => void;
    setState: (partial: Partial<ListUrlState>) => void;
    reset: () => void;
    /** 底层 URL 参数更新器，供组合 hook 复用同一 setSearchParams 实例 */
    updateParams: (updater: (params: URLSearchParams) => URLSearchParams) => void;
}

const FILTERS_PARAM = 'filters';
const PAGE_PARAM = 'page';
const KEYWORD_PARAM = 'keyword';

export function serializeFilters(filters: Record<string, string>): string {
    return Object.entries(filters)
        .filter(([, value]) => value !== undefined && value !== null && value !== '')
        .map(([key, value]) => `${key}:${encodeURIComponent(value)}`)
        .join(',');
}

export function parseFilters(raw: string | null): Record<string, string> {
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

/**
 * 将列表状态的部分更新应用到 URL 参数（keyword/filters/pageNum）。
 * 供 {@link useListUrlState} 与组合 hook（如 useSearchUrlState）复用同一分支逻辑，
 * 避免各自复制一份 URL 更新代码。
 */
export function applyListPartial(params: URLSearchParams, partial: Partial<ListUrlState>): URLSearchParams {
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
    return params;
}

/** 清空列表状态相关的全部 URL 参数（keyword/filters/pageNum）。 */
export function resetListParams(params: URLSearchParams): URLSearchParams {
    params.delete(KEYWORD_PARAM);
    params.delete(FILTERS_PARAM);
    params.delete(PAGE_PARAM);
    return params;
}

export function useListUrlState(): ListUrlState & ListUrlStateSetters {
    const [searchParams, setSearchParams] = useSearchParams();

    const state = useMemo<ListUrlState>(() => {
        const keyword = searchParams.get(KEYWORD_PARAM) || '';
        const filters = parseFilters(searchParams.get(FILTERS_PARAM));
        const pageNum = Math.max(1, parseInt(searchParams.get(PAGE_PARAM) || '1', 10) || 1);
        return { keyword, filters, pageNum };
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

    const setState = useCallback(
        (partial: Partial<ListUrlState>) => {
            updateParams(params => applyListPartial(params, partial));
        },
        [updateParams]
    );

    const reset = useCallback(() => {
        updateParams(resetListParams);
    }, [updateParams]);

    return {
        ...state,
        setKeyword,
        setFilters,
        setFilterValue,
        setPageNum,
        setState,
        reset,
        updateParams,
    };
}
