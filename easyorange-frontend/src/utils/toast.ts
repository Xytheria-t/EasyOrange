/**
 * @fileoverview Toast 消息提示工具模块
 * @description 提供全局消息提示功能
 * @version 1.0.0
 */

import type { ToastType } from '../types';

/**
 * Toast 消息提示类
 */
class ToastUtils {
    private _element: HTMLElement | null = null;
    private _timeoutId: ReturnType<typeof setTimeout> | null = null;
    private _isShowing = false;

    /**
     * 获取或创建 Toast 元素
     */
    private _getElement(): HTMLElement | null {
        if (!this._element) {
            this._element = document.getElementById('toast');
        }
        return this._element;
    }

    /**
     * 显示消息
     */
    show(message: string, type: ToastType = 'info', duration = 3000): void {
        const element = this._getElement();
        if (!element) {return;}

        if (this._timeoutId) {
            clearTimeout(this._timeoutId);
            this._timeoutId = null;
        }

        element.textContent = message;
        element.className = `toast active toast-${type}`;

        this._isShowing = true;
        this._timeoutId = setTimeout(() => {
            element.classList.remove('active');
            this._isShowing = false;
            this._timeoutId = null;
        }, duration);
    }

    /**
     * 显示成功消息
     */
    success(message: string, duration?: number): void {
        this.show(message, 'success', duration);
    }

    /**
     * 显示错误消息
     */
    error(message: string, duration?: number): void {
        this.show(message, 'error', duration);
    }

    /**
     * 显示信息消息
     */
    info(message: string, duration?: number): void {
        this.show(message, 'info', duration);
    }

    /**
     * 显示警告消息
     */
    warning(message: string, duration?: number): void {
        this.show(message, 'warning', duration);
    }

    /**
     * 隐藏消息
     */
    hide(): void {
        const element = this._getElement();
        if (element && this._isShowing) {
            element.classList.remove('active');
            if (this._timeoutId) {
                clearTimeout(this._timeoutId);
                this._timeoutId = null;
            }
            this._isShowing = false;
        }
    }
}

// 导出单例
const toast = new ToastUtils();

export { ToastUtils, toast };
export default toast;
