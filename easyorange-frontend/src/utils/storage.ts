/**
 * @fileoverview 本地存储工具模块
 * @description 提供安全的 localStorage 封装，支持过期时间
 * @version 1.0.0
 */

import type { StorageItem } from '../types';

/**
 * 本地存储工具类
 */
class StorageUtils {
    private _prefix: string;

    constructor(prefix = '') {
        this._prefix = prefix;
    }

    /**
     * 设置存储前缀
     */
    setPrefix(prefix: string): void {
        this._prefix = prefix;
    }

    /**
     * 获取完整的键名
     */
    private _getKey(key: string): string {
        return this._prefix ? `${this._prefix}_${key}` : key;
    }

    /**
     * 存储数据
     */
    set(key: string, value: unknown): void {
        try {
            localStorage.setItem(this._getKey(key), JSON.stringify(value));
        } catch (error) {
            // localStorage 写入失败静默处理，不影响核心功能
        }
    }

    /**
     * 存储带过期时间的数据
     */
    setEx<T>(key: string, value: T, ttl: number): void {
        const item: StorageItem<T> = {
            value,
            expireAt: Date.now() + ttl
        };
        this.set(key, item);
    }

    /**
     * 获取数据
     */
    get<T = unknown>(key: string, defaultValue: T | null = null): T {
        try {
            const item = localStorage.getItem(this._getKey(key));
            if (!item) {return defaultValue as T;}

            const parsed = JSON.parse(item);

            // 检查是否有过期时间
            if (parsed && typeof parsed === 'object' && 'expireAt' in parsed) {
                if (Date.now() > parsed.expireAt) {
                    this.remove(key);
                    return defaultValue as T;
                }
                return (parsed.value ?? defaultValue) as T;
            }

            return (parsed ?? defaultValue) as T;
        } catch (error) {
            // 解析失败返回默认值
            return defaultValue as T;
        }
    }

    /**
     * 删除数据
     */
    remove(key: string): void {
        try {
            localStorage.removeItem(this._getKey(key));
        } catch (error) {
            // 删除失败静默处理
        }
    }

    /**
     * 清空所有数据（仅清除带前缀的键）
     */
    clear(): void {
        try {
            if (this._prefix) {
                // 仅清除带前缀的键，避免误删同域下其他应用的存储
                const prefix = this._getKey('');
                const keysToRemove: string[] = [];
                for (let i = 0; i < localStorage.length; i++) {
                    const key = localStorage.key(i);
                    if (key && key.startsWith(prefix)) {
                        keysToRemove.push(key);
                    }
                }
                keysToRemove.forEach(key => localStorage.removeItem(key));
            } else {
                // 无前缀时清除所有数据，仅在开发模式下提示
                localStorage.clear();
            }
        } catch (error) {
            // 清空失败静默处理
        }
    }

    /**
     * 检查键是否存在
     */
    has(key: string): boolean {
        return localStorage.getItem(this._getKey(key)) !== null;
    }
}

// 导出单例
const storage = new StorageUtils();

export { StorageUtils, storage };
export default storage;
