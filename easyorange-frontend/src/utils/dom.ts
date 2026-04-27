/**
 * @fileoverview DOM 操作工具模块
 * @description 提供常用的 DOM 操作功能
 * @version 1.0.0
 */

import type { Unsubscribe } from '../types';

/**
 * DOM 操作工具类
 */
class DomUtils {
    /**
     * 获取单个元素
     */
    get<T extends HTMLElement = HTMLElement>(
        selector: string,
        context: Element | Document = document
    ): T | null {
        return (context?.querySelector(selector) ?? null) as T | null;
    }

    /**
     * 获取所有匹配的元素
     */
    getAll<T extends HTMLElement = HTMLElement>(
        selector: string,
        context: Element | Document = document
    ): T[] {
        return Array.from(context?.querySelectorAll(selector) ?? []) as T[];
    }

    /**
     * 添加事件监听器
     */
    on(
        element: Element | Window | Document | null,
        event: string,
        handler: EventListener,
        options?: AddEventListenerOptions
    ): Unsubscribe {
        if (!element) {return () => {};}
        element.addEventListener(event, handler, options);
        return () => element.removeEventListener(event, handler, options);
    }

    /**
     * 移除事件监听器
     */
    off(
        element: Element | Window | Document | null,
        event: string,
        handler: EventListener
    ): void {
        element?.removeEventListener(event, handler);
    }

    /**
     * 显示元素
     */
    show(element: HTMLElement | null): void {
        if (element) {
            element.style.display = '';
        }
    }

    /**
     * 隐藏元素
     */
    hide(element: HTMLElement | null): void {
        if (element) {
            element.style.display = 'none';
        }
    }

    /**
     * 切换类名
     */
    toggleClass(element: Element | null, className: string): boolean {
        if (!element) {return false;}
        return element.classList.toggle(className);
    }

    /**
     * 添加类名
     */
    addClass(element: Element | null, ...classNames: string[]): void {
        element?.classList.add(...classNames);
    }

    /**
     * 移除类名
     */
    removeClass(element: Element | null, ...classNames: string[]): void {
        element?.classList.remove(...classNames);
    }

    /**
     * 检查是否包含类名
     */
    hasClass(element: Element | null, className: string): boolean {
        return element?.classList.contains(className) ?? false;
    }

    /**
     * 设置属性
     */
    setAttrs(element: Element | null, attrs: Record<string, string>): void {
        if (!element) {return;}
        Object.entries(attrs).forEach(([key, value]) => {
            element.setAttribute(key, value);
        });
    }

    /**
     * 设置样式
     */
    setStyles(element: HTMLElement | null, styles: Partial<CSSStyleDeclaration>): void {
        if (!element) {return;}
        Object.assign(element.style, styles);
    }
}

// 导出单例
const dom = new DomUtils();

export { DomUtils, dom };
export default dom;
