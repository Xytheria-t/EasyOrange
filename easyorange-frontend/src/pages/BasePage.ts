/**
 * @fileoverview 页面基类
 * @description 提供统一的页面生命周期管理、事件管理和资源清理
 */

/**
 * 基础页面类
 * @template TElements - 页面 DOM 元素类型
 */
export abstract class BasePage<TElements = Record<string, HTMLElement | null>> {
    protected elements: TElements;
    protected initialized: boolean;
    private eventListeners: Array<() => void>;
    private abortControllers: Set<AbortController>;

    constructor() {
        this.elements = {} as TElements;
        this.initialized = false;
        this.eventListeners = [];
        this.abortControllers = new Set();
    }

    /**
     * 初始化页面
     */
    async init(): Promise<void> {
        if (this.initialized) {
            return;
        }

        this.cacheElements();
        this.bindEvents();
        await this.onInit();
        this.initialized = true;
    }

    /**
     * 缓存 DOM 元素（子类实现）
     */
    protected abstract cacheElements(): void;

    /**
     * 绑定事件监听器（子类实现）
     */
    protected abstract bindEvents(): void;

    /**
     * 初始化逻辑（子类实现）
     */
    protected abstract onInit(): Promise<void>;

    /**
     * 销毁页面，清理所有资源
     */
    destroy(): void {
        if (!this.initialized) {
            return;
        }

        // 清理所有事件监听器
        this.eventListeners.forEach(cleanup => {
            try {
                cleanup();
            } catch (error) {
                // 清理事件监听器失败静默处理
            }
        });
        this.eventListeners = [];

        // 中止所有进行中的请求
        this.abortControllers.forEach(controller => {
            try {
                controller.abort();
            } catch (error) {
                // 中止请求失败静默处理
            }
        });
        this.abortControllers.clear();

        // 调用子类的清理逻辑
        this.onDestroy();

        this.initialized = false;
        this.elements = {} as TElements;
    }

    /**
     * 页面销毁前的清理逻辑（子类可覆写）
     */
    protected onDestroy(): void {
        // 子类可以覆写以添加额外的清理逻辑
    }

    /**
     * 绑定事件监听器（自动清理）
     * @param target - 事件目标
     * @param event - 事件名称
     * @param handler - 事件处理函数
     * @param options - 事件选项
     */
    protected onEvent<T extends EventTarget>(
        target: T | null,
        event: string,
        handler: (e: Event) => void,
        options?: AddEventListenerOptions
    ): void {
        if (!target) {
            return;
        }

        target.addEventListener(event, handler, options);
        
        // 记录清理函数
        this.eventListeners.push(() => {
            target.removeEventListener(event, handler);
        });
    }

    /**
     * 绑定带 AbortController 的事件（用于 fetch 等）
     * @returns AbortController 实例
     */
    protected createAbortController(): AbortController {
        const controller = new AbortController();
        this.abortControllers.add(controller);
        return controller;
    }

    /**
     * 防抖函数
     * @param func - 原函数
     * @param wait - 等待时间（毫秒）
     */
    protected debounce<T extends (...args: unknown[]) => unknown>(
        func: T,
        wait: number
    ): (...args: Parameters<T>) => void {
        let timeout: ReturnType<typeof setTimeout> | null = null;
        
        return (...args: Parameters<T>) => {
            if (timeout) {
                clearTimeout(timeout);
            }
            timeout = setTimeout(() => {
                func(...args);
                timeout = null;
            }, wait);
        };
    }

    /**
     * 节流函数
     * @param func - 原函数
     * @param limit - 限制时间（毫秒）
     */
    protected throttle<T extends (...args: unknown[]) => unknown>(
        func: T,
        limit: number
    ): (...args: Parameters<T>) => void {
        let inThrottle = false;
        
        return (...args: Parameters<T>) => {
            if (!inThrottle) {
                func(...args);
                inThrottle = true;
                setTimeout(() => {
                    inThrottle = false;
                }, limit);
            }
        };
    }

    /**
     * 检查页面是否已初始化
     */
    isInitialized(): boolean {
        return this.initialized;
    }

    /**
     * 获取页面元素
     * @param key - 元素键名
     */
    getElement<K extends keyof TElements>(key: K): TElements[K] {
        return this.elements[key];
    }

    /**
     * 更新页面元素
     * @param key - 元素键名
     * @param element - 新元素
     */
    setElement<K extends keyof TElements>(key: K, element: TElements[K]): void {
        this.elements[key] = element;
    }

    /**
     * 安全查询单个 DOM 元素
     * @param selector - CSS 选择器
     * @returns 匹配的 DOM 元素，未找到返回 null
     */
    protected querySelector<T extends HTMLElement>(selector: string): T | null {
        return document.querySelector<T>(selector);
    }

    /**
     * 安全查询多个 DOM 元素
     * @param selector - CSS 选择器
     * @returns 匹配的 DOM 元素列表
     */
    protected querySelectorAll<T extends HTMLElement>(selector: string): NodeListOf<T> {
        return document.querySelectorAll<T>(selector);
    }

    /**
     * 安全执行操作，仅在元素非空时调用回调
     * @param el - DOM 元素
     * @param fn - 操作函数
     */
    protected safe<T>(el: T | null | undefined, fn: (el: T) => void): void {
        if (el != null) {
            fn(el);
        }
    }
}

export default BasePage;
