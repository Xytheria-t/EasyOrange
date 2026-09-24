import { renderHook } from '@testing-library/react';
import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import { useScrollReveal } from './useScrollReveal';

class MockIntersectionObserver {
    readonly root: Element | null = null;
    readonly rootMargin: string = '';
    readonly thresholds: ReadonlyArray<number> = [];
    observe = vi.fn();
    disconnect = vi.fn();
    unobserve = vi.fn();
    takeRecords = vi.fn();
    static instances: MockIntersectionObserver[] = [];
    constructor() {
        MockIntersectionObserver.instances.push(this);
    }
}

let originalIO: typeof IntersectionObserver;

beforeAll(() => {
    originalIO = globalThis.IntersectionObserver;
    globalThis.IntersectionObserver = MockIntersectionObserver as unknown as typeof IntersectionObserver;
});

afterAll(() => {
    globalThis.IntersectionObserver = originalIO;
});

beforeEach(() => {
    MockIntersectionObserver.instances = [];
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

        // 挂载后每个 reveal 元素都应被 observe（3 个元素 → 至少 3 次 observe）
        const observedCount = MockIntersectionObserver.instances.reduce((n, o) => n + o.observe.mock.calls.length, 0);
        expect(observedCount).toBeGreaterThanOrEqual(3);

        unmount();

        // 卸载后每个 observer 都应 disconnect
        for (const o of MockIntersectionObserver.instances) {
            expect(o.disconnect).toHaveBeenCalled();
        }
    });

    it('handles no reveal elements gracefully', () => {
        const { unmount } = renderHook(() => useScrollReveal());
        // 无 reveal 元素时不应崩溃；不强制创建 observer（实现可提前返回）
        expect(() => unmount()).not.toThrow();
    });
});
