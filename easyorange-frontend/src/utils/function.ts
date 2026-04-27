/**
 * @fileoverview 函数工具模块
 * @description 提供 debounce、throttle 等函数工具
 */

export interface DebounceOptions {
    immediate?: boolean;
}

export function debounce<T extends (...args: unknown[]) => unknown>(
    func: T,
    delay = 300,
    options: DebounceOptions = {}
): ((...args: Parameters<T>) => void) & { cancel: () => void } {
    const { immediate = false } = options;
    let timeoutId: ReturnType<typeof setTimeout> | null = null;

    const debounced = function (this: unknown, ...args: Parameters<T>) {
        const callNow = immediate && !timeoutId;
        if (timeoutId) {clearTimeout(timeoutId);}
        timeoutId = setTimeout(() => {
            timeoutId = null;
            if (!immediate) {func.apply(this, args);}
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
    limit = 300
): (...args: Parameters<T>) => void {
    let inThrottle = false;
    let lastArgs: Parameters<T> | null = null;

    return function (this: unknown, ...args: Parameters<T>) {
        if (!inThrottle) {
            func.apply(this, args);
            inThrottle = true;
            setTimeout(() => {
                inThrottle = false;
                if (lastArgs) {
                    (func as (...args: Parameters<T>) => void).apply(this, lastArgs);
                    lastArgs = null;
                }
            }, limit);
        } else {
            lastArgs = args;
        }
    };
}
