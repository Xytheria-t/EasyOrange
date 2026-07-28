import { act, renderHook } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { useSearchUrlState } from './useSearchUrlState';

const createWrapper = (initialEntries?: string[]) =>
    function Wrapper({ children }: { children: React.ReactNode }) {
        return <MemoryRouter initialEntries={initialEntries}>{children}</MemoryRouter>;
    };

describe('useSearchUrlState', () => {
    it('reads initial state from empty URL', () => {
        const { result } = renderHook(() => useSearchUrlState(), {
            wrapper: createWrapper(['/search']),
        });

        expect(result.current.keyword).toBe('');
        expect(result.current.filters).toEqual({});
        expect(result.current.pageNum).toBe(1);
        expect(result.current.aiEnabled).toBe(false);
    });

    it('parses keyword from URL', () => {
        const { result } = renderHook(() => useSearchUrlState(), {
            wrapper: createWrapper(['/search?keyword=手机']),
        });

        expect(result.current.keyword).toBe('手机');
    });

    it('parses filters from URL', () => {
        const { result } = renderHook(() => useSearchUrlState(), {
            wrapper: createWrapper(['/search?filters=category:1,condition:9']),
        });

        expect(result.current.filters).toEqual({ category: '1', condition: '9' });
    });

    it('decodes URL-encoded filter values', () => {
        const { result } = renderHook(() => useSearchUrlState(), {
            wrapper: createWrapper(['/search?filters=tag:%E6%89%8B%E6%9C%BA']),
        });

        expect(result.current.filters).toEqual({ tag: '手机' });
    });

    it('parses page number and clamps to at least 1', () => {
        const { result } = renderHook(() => useSearchUrlState(), {
            wrapper: createWrapper(['/search?page=3&page=-2']),
        });

        expect(result.current.pageNum).toBe(3);
    });

    it('parses AI enabled flag', () => {
        const { result } = renderHook(() => useSearchUrlState(), {
            wrapper: createWrapper(['/search?ai=1']),
        });

        expect(result.current.aiEnabled).toBe(true);
    });

    it('sets keyword in URL', () => {
        const { result } = renderHook(() => useSearchUrlState(), {
            wrapper: createWrapper(['/search']),
        });

        act(() => {
            result.current.setKeyword('电脑');
        });

        expect(result.current.keyword).toBe('电脑');
    });

    it('removes empty keyword from URL', () => {
        const { result } = renderHook(() => useSearchUrlState(), {
            wrapper: createWrapper(['/search?keyword=手机']),
        });

        act(() => {
            result.current.setKeyword('');
        });

        expect(result.current.keyword).toBe('');
    });

    it('sets filters and resets page to 1', () => {
        const { result } = renderHook(() => useSearchUrlState(), {
            wrapper: createWrapper(['/search?page=3']),
        });

        act(() => {
            result.current.setFilters({ category: '1', price: '0_100' });
        });

        expect(result.current.filters).toEqual({ category: '1', price: '0_100' });
        expect(result.current.pageNum).toBe(1);
    });

    it('sets a single filter value and resets page to 1', () => {
        const { result } = renderHook(() => useSearchUrlState(), {
            wrapper: createWrapper(['/search']),
        });

        act(() => {
            result.current.setFilterValue('category', '2');
        });

        expect(result.current.filters).toEqual({ category: '2' });
        expect(result.current.pageNum).toBe(1);
    });

    it('removes a single filter value when null is passed', () => {
        const { result } = renderHook(() => useSearchUrlState(), {
            wrapper: createWrapper(['/search?filters=category:1,condition:9']),
        });

        act(() => {
            result.current.setFilterValue('category', null);
        });

        expect(result.current.filters).toEqual({ condition: '9' });
    });

    it('sets page number', () => {
        const { result } = renderHook(() => useSearchUrlState(), {
            wrapper: createWrapper(['/search']),
        });

        act(() => {
            result.current.setPageNum(5);
        });

        expect(result.current.pageNum).toBe(5);
    });

    it('clamps page number to at least 1', () => {
        const { result } = renderHook(() => useSearchUrlState(), {
            wrapper: createWrapper(['/search']),
        });

        act(() => {
            result.current.setPageNum(-3);
        });

        expect(result.current.pageNum).toBe(1);
    });

    it('toggles AI enabled flag', () => {
        const { result } = renderHook(() => useSearchUrlState(), {
            wrapper: createWrapper(['/search']),
        });

        act(() => {
            result.current.setAiEnabled(true);
        });

        expect(result.current.aiEnabled).toBe(true);

        act(() => {
            result.current.setAiEnabled(false);
        });

        expect(result.current.aiEnabled).toBe(false);
    });

    it('sets partial state in one update', () => {
        const { result } = renderHook(() => useSearchUrlState(), {
            wrapper: createWrapper(['/search']),
        });

        act(() => {
            result.current.setState({ keyword: '耳机', pageNum: 2, aiEnabled: true });
        });

        expect(result.current.keyword).toBe('耳机');
        expect(result.current.pageNum).toBe(2);
        expect(result.current.aiEnabled).toBe(true);
    });

    it('resets all state', () => {
        const { result } = renderHook(() => useSearchUrlState(), {
            wrapper: createWrapper(['/search?keyword=手机&filters=category:1&page=2&ai=1']),
        });

        act(() => {
            result.current.reset();
        });

        expect(result.current.keyword).toBe('');
        expect(result.current.filters).toEqual({});
        expect(result.current.pageNum).toBe(1);
        expect(result.current.aiEnabled).toBe(false);
    });
});
