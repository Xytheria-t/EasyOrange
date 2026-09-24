import { act, renderHook } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { useMediaQuery } from './useMediaQuery';

type ChangeHandler = (event: MediaQueryListEvent) => void;

function mockMatchMedia(initialMatches: boolean) {
    let handler: ChangeHandler | undefined;
    const mediaQueryList = {
        matches: initialMatches,
        media: '',
        addEventListener: vi.fn((_type: string, cb: ChangeHandler) => {
            handler = cb;
        }),
        removeEventListener: vi.fn(),
    };

    Object.defineProperty(window, 'matchMedia', {
        value: vi.fn(() => mediaQueryList),
        configurable: true,
        writable: true,
    });

    return {
        mediaQueryList,
        emit: (matches: boolean) => handler?.({ matches } as MediaQueryListEvent),
    };
}

describe('useMediaQuery', () => {
    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('returns the initial match result', () => {
        mockMatchMedia(true);
        const { result } = renderHook(() => useMediaQuery('(max-width: 768px)'));
        expect(result.current).toBe(true);
    });

    it('returns false when the query does not match', () => {
        mockMatchMedia(false);
        const { result } = renderHook(() => useMediaQuery('(max-width: 768px)'));
        expect(result.current).toBe(false);
    });

    it('updates when the media query changes', () => {
        const { emit } = mockMatchMedia(false);
        const { result } = renderHook(() => useMediaQuery('(max-width: 768px)'));
        expect(result.current).toBe(false);

        act(() => emit(true));

        expect(result.current).toBe(true);
    });

    it('removes the listener on unmount', () => {
        const { mediaQueryList } = mockMatchMedia(false);
        const { unmount } = renderHook(() => useMediaQuery('(max-width: 768px)'));

        unmount();

        expect(mediaQueryList.removeEventListener).toHaveBeenCalled();
    });
});

describe('useMediaQuery in jsdom without matchMedia mock', () => {
    beforeEach(() => {
        Object.defineProperty(window, 'matchMedia', {
            value: undefined,
            configurable: true,
            writable: true,
        });
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('returns false when matchMedia is unavailable', () => {
        const { result } = renderHook(() => useMediaQuery('(max-width: 768px)'));
        expect(result.current).toBe(false);
    });
});
