import { act, renderHook } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { useListUrlState } from './useListUrlState';

const createWrapper = (initialEntries?: string[]) =>
    function Wrapper({ children }: { children: React.ReactNode }) {
        return <MemoryRouter initialEntries={initialEntries}>{children}</MemoryRouter>;
    };

describe('useListUrlState', () => {
    it('reads initial state from empty URL', () => {
        const { result } = renderHook(() => useListUrlState(), {
            wrapper: createWrapper(['/list']),
        });

        expect(result.current.keyword).toBe('');
        expect(result.current.filters).toEqual({});
        expect(result.current.pageNum).toBe(1);
    });

    it('parses keyword from URL', () => {
        const { result } = renderHook(() => useListUrlState(), {
            wrapper: createWrapper(['/list?keyword=手机']),
        });

        expect(result.current.keyword).toBe('手机');
    });

    it('parses filters from URL', () => {
        const { result } = renderHook(() => useListUrlState(), {
            wrapper: createWrapper(['/list?filters=category:1,condition:9']),
        });

        expect(result.current.filters).toEqual({ category: '1', condition: '9' });
    });

    it('decodes URL-encoded filter values', () => {
        const { result } = renderHook(() => useListUrlState(), {
            wrapper: createWrapper(['/list?filters=tag:%E6%89%8B%E6%9C%BA']),
        });

        expect(result.current.filters).toEqual({ tag: '手机' });
    });

    it('parses page number and clamps to at least 1', () => {
        const { result } = renderHook(() => useListUrlState(), {
            wrapper: createWrapper(['/list?page=3&page=-2']),
        });

        expect(result.current.pageNum).toBe(3);
    });

    it('sets keyword in URL', () => {
        const { result } = renderHook(() => useListUrlState(), {
            wrapper: createWrapper(['/list']),
        });

        act(() => {
            result.current.setKeyword('电脑');
        });

        expect(result.current.keyword).toBe('电脑');
    });

    it('removes empty keyword from URL', () => {
        const { result } = renderHook(() => useListUrlState(), {
            wrapper: createWrapper(['/list?keyword=手机']),
        });

        act(() => {
            result.current.setKeyword('');
        });

        expect(result.current.keyword).toBe('');
    });

    it('sets filters and resets page to 1', () => {
        const { result } = renderHook(() => useListUrlState(), {
            wrapper: createWrapper(['/list?page=3']),
        });

        act(() => {
            result.current.setFilters({ category: '1', priceMin: '0' });
        });

        expect(result.current.filters).toEqual({ category: '1', priceMin: '0' });
        expect(result.current.pageNum).toBe(1);
    });

    it('sets a single filter value and resets page to 1', () => {
        const { result } = renderHook(() => useListUrlState(), {
            wrapper: createWrapper(['/list?page=5']),
        });

        act(() => {
            result.current.setFilterValue('status', 'PAID');
        });

        expect(result.current.filters).toEqual({ status: 'PAID' });
        expect(result.current.pageNum).toBe(1);
    });

    it('removes a single filter value when null is passed', () => {
        const { result } = renderHook(() => useListUrlState(), {
            wrapper: createWrapper(['/list?filters=category:1,condition:9']),
        });

        act(() => {
            result.current.setFilterValue('category', null);
        });

        expect(result.current.filters).toEqual({ condition: '9' });
    });

    it('sets page number', () => {
        const { result } = renderHook(() => useListUrlState(), {
            wrapper: createWrapper(['/list']),
        });

        act(() => {
            result.current.setPageNum(5);
        });

        expect(result.current.pageNum).toBe(5);
    });

    it('clamps page number to at least 1', () => {
        const { result } = renderHook(() => useListUrlState(), {
            wrapper: createWrapper(['/list']),
        });

        act(() => {
            result.current.setPageNum(-3);
        });

        expect(result.current.pageNum).toBe(1);
    });

    it('sets partial state in one update without touching unspecified fields', () => {
        const { result } = renderHook(() => useListUrlState(), {
            wrapper: createWrapper(['/list?keyword=耳机&page=2']),
        });

        act(() => {
            result.current.setState({ filters: { category: '3' } });
        });

        expect(result.current.keyword).toBe('耳机');
        expect(result.current.filters).toEqual({ category: '3' });
        expect(result.current.pageNum).toBe(2);
    });

    it('resets all list state', () => {
        const { result } = renderHook(() => useListUrlState(), {
            wrapper: createWrapper(['/list?keyword=手机&filters=category:1&page=2']),
        });

        act(() => {
            result.current.reset();
        });

        expect(result.current.keyword).toBe('');
        expect(result.current.filters).toEqual({});
        expect(result.current.pageNum).toBe(1);
    });
});
