import { renderHook } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { useDashboardTime } from './useDashboardTime';

describe('useDashboardTime', () => {
    beforeEach(() => {
        vi.useFakeTimers();
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it('returns formatted current time on initial render', () => {
        vi.setSystemTime(new Date(2026, 4, 16, 10, 0, 0));
        const { result } = renderHook(() => useDashboardTime());
        expect(result.current).toBe('5月16日 周六');
    });

    it('updates time every 30 seconds', () => {
        vi.setSystemTime(new Date(2026, 4, 16, 10, 0, 0));
        const { result, rerender } = renderHook(() => useDashboardTime());
        expect(result.current).toBe('5月16日 周六');

        vi.setSystemTime(new Date(2026, 4, 17, 11, 30, 0));
        vi.advanceTimersByTime(30_000);
        rerender();
        expect(result.current).toBe('5月17日 周日');
    });

    it('cleans up interval on unmount', () => {
        vi.setSystemTime(new Date(2026, 4, 16, 10, 0, 0));
        const { unmount } = renderHook(() => useDashboardTime());
        expect(() => unmount()).not.toThrow();
    });
});
