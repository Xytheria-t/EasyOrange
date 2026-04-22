/**
 * @fileoverview 统一事件管理模块
 * @description 提供全局事件绑定的统一管理，支持 bind/unbind/clearAll 操作
 * @version 1.0.0
 */

type EventHandler = (event: Event) => void;

interface ListenerEntry {
    element: EventTarget;
    event: string;
    handler: EventHandler;
    options?: AddEventListenerOptions;
}

/**
 * 事件管理器
 * 提供全局事件绑定的跟踪和清理功能
 */
class EventManager {
    private listeners: Map<string, ListenerEntry>;
    private counter: number;

    constructor() {
        this.listeners = new Map();
        this.counter = 0;
    }

    /**
     * 生成唯一 key
     */
    private generateKey(): string {
        return `listener_${++this.counter}_${Date.now()}`;
    }

    /**
     * 绑定事件监听器
     * @param element - 目标元素
     * @param event - 事件名称
     * @param handler - 事件处理函数
     * @param options - 事件选项
     * @returns 取消绑定的函数
     */
    on(
        element: EventTarget | null,
        event: string,
        handler: EventHandler,
        options?: AddEventListenerOptions
    ): () => void {
        if (!element) {
            return () => {};
        }

        const key = this.generateKey();
        const entry: ListenerEntry = {
            element,
            event,
            handler,
            options
        };

        this.listeners.set(key, entry);
        element.addEventListener(event, handler, options);

        return () => {
            this.off(key);
        };
    }

    /**
     * 取消绑定事件监听器（通过 key）
     * @param key - 监听器 key
     */
    off(key: string): void {
        const entry = this.listeners.get(key);
        if (!entry) {return;}

        const { element, event, handler, options } = entry;
        element.removeEventListener(event, handler, options);
        this.listeners.delete(key);
    }

    /**
     * 取消绑定事件监听器（通过元素和事件名）
     * @param element - 目标元素
     * @param event - 事件名称
     */
    offByElement(element: EventTarget, event: string): void {
        const keysToRemove: string[] = [];

        this.listeners.forEach((entry, key) => {
            if (entry.element === element && entry.event === event) {
                entry.element.removeEventListener(entry.event, entry.handler, entry.options);
                keysToRemove.push(key);
            }
        });

        keysToRemove.forEach(key => this.listeners.delete(key));
    }

    /**
     * 清空所有事件监听器
     */
    clearAll(): void {
        this.listeners.forEach((entry) => {
            entry.element.removeEventListener(entry.event, entry.handler, entry.options);
        });
        this.listeners.clear();
    }

    /**
     * 获取当前监听器数量
     */
    getListenerCount(): number {
        return this.listeners.size;
    }
}

// 导出单例
const eventManager = new EventManager();

export { EventManager, eventManager };
export default eventManager;
