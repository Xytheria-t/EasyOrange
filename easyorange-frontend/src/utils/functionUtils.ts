/**
 * @fileoverview 函数工具模块
 * @description 提供 debounce、throttle 等函数工具
 */

export interface DebounceOptions {
    isImmediate?: boolean;
}

export function debounce<T extends (...args: unknown[]) => unknown>(
    func: T,
    delay = 300,
    options: DebounceOptions = {}
): ((...args: Parameters<T>) => void) & { cancel: () => void } {
    const { isImmediate = false } = options;
    let timeoutId: ReturnType<typeof setTimeout> | null = null;

    const debounced = function (this: unknown, ...args: Parameters<T>) {
        const callNow = isImmediate && !timeoutId;
        if (timeoutId) {clearTimeout(timeoutId);}
        timeoutId = setTimeout(() => {
            timeoutId = null;
            if (!isImmediate) {func.apply(this, args);}
        }, delay);
        if (callNow) {func.apply(this, args);}
    };

    debounced.cancel = () => {
        if (timeoutId) {
            clearTimeout(timeoutId);
            timeoutId = null;
        }
    };

    return debounced;
}

export function throttle<T extends (...args: unknown[]) => unknown>(
    func: T,
    _limit = 300
): ((...args: Parameters<T>) => void) & { cancel: () => void } {
    let rafId: number | null = null;
    let lastArgs: Parameters<T> | null = null;

    const throttled = function (this: unknown, ...args: Parameters<T>) {
        lastArgs = args;
        if (rafId === null) {
            rafId = requestAnimationFrame(() => {
                if (lastArgs !== null) {
                    func.apply(this, lastArgs);
                }
                rafId = null;
                lastArgs = null;
            });
        }
    };

    throttled.cancel = () => {
        if (rafId !== null) {
            cancelAnimationFrame(rafId);
            rafId = null;
        }
        lastArgs = null;
    };

    return throttled;
}
