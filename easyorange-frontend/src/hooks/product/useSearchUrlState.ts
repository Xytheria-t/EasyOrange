import { useCallback, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import { applyListPartial, type ListUrlState, resetListParams, useListUrlState } from '@/hooks/ui/useListUrlState';

export interface SearchUrlState extends ListUrlState {
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

const AI_PARAM = 'ai';

/**
 * SearchPage 专用 URL 状态 hook。
 *
 * 在通用 {@link useListUrlState} 基础上扩展 `aiEnabled` 标志（语义搜索开关）。
 *
 * `aiEnabled` 通过独立的 `ai` 查询参数持久化（不进入 `filters` 序列化），
 * 这样切换语义搜索不会触发列表筛选 chip 的展示。
 *
 * 写入操作复用 useListUrlState 的 updateParams 以共享同一 setSearchParams 实例，
 * 避免多个 useSearchParams 写入在并发更新时产生竞争。读取操作直接使用
 * useSearchParams（只读不竞争）。
 *
 * `setState` 与 `reset` 将 list 状态与 aiEnabled 合并到单次 updateParams 调用，
 * 确保一次 React Router 导航即完成所有 URL 变更（避免双导航竞态）。
 */
export function useSearchUrlState(): SearchUrlState & SearchUrlStateSetters {
    const listState = useListUrlState();
    const [searchParams] = useSearchParams();

    const aiEnabled = useMemo(() => searchParams.get(AI_PARAM) === '1', [searchParams]);

    const setAiEnabled = useCallback(
        (enabled: boolean) => {
            listState.updateParams(params => {
                if (enabled) {
                    params.set(AI_PARAM, '1');
                } else {
                    params.delete(AI_PARAM);
                }
                return params;
            });
        },
        [listState]
    );

    const setState = useCallback(
        (partial: Partial<SearchUrlState>) => {
            listState.updateParams(params => {
                applyListPartial(params, partial);
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
        [listState]
    );

    const reset = useCallback(() => {
        listState.updateParams(params => {
            resetListParams(params);
            params.delete(AI_PARAM);
            return params;
        });
    }, [listState]);

    return {
        keyword: listState.keyword,
        filters: listState.filters,
        pageNum: listState.pageNum,
        aiEnabled,
        setKeyword: listState.setKeyword,
        setFilters: listState.setFilters,
        setFilterValue: listState.setFilterValue,
        setPageNum: listState.setPageNum,
        setAiEnabled,
        setState,
        reset,
    };
}
