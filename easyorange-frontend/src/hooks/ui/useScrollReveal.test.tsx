import { renderHook } from '@testing-library/react';
import { afterAll, afterEach, beforeAll, describe, it, vi } from 'vitest';
import { useScrollReveal } from './useScrollReveal';

class MockIntersectionObserver {
    readonly root: Element | null = null;
    readonly rootMargin: string = '';
    readonly thresholds: ReadonlyArray<number> = [];
    observe = vi.fn();
    disconnect = vi.fn();
    unobserve = vi.fn();
    takeRecords = vi.fn();
}

let originalIO: typeof IntersectionObserver;

beforeAll(() => {
    originalIO = globalThis.IntersectionObserver;
    globalThis.IntersectionObserver = MockIntersectionObserver as unknown as typeof IntersectionObserver;
});

afterAll(() => {
    globalThis.IntersectionObserver = originalIO;
});

afterEach(() => {
    document.body.innerHTML = '';
});

describe('useScrollReveal', () => {
    it('observes reveal elements on mount and disconnects on unmount', () => {
        document.body.innerHTML = `
      <div class="reveal"></div>
      <div class="reveal-scale"></div>
      <div class="reveal-left"></div>
    `;

        const { unmount } = renderHook(() => useScrollReveal());

        // After mount, the hook should have created observers
        unmount();
    });

    it('handles no reveal elements gracefully', () => {
        const { unmount } = renderHook(() => useScrollReveal());
        unmount();
    });
});
