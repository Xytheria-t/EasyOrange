/**
 * @fileoverview 草稿管理模块
 * @version 2.0.0
 */

import { storage } from '@/utils';

/** 草稿数据接口 */
export interface DraftData {
    categoryId?: number | null;
    name?: string;
    description?: string;
    price?: string;
    originalPrice?: string;
    stock?: string;
    conditionLevel?: number;
    location?: string;
    contactMethod?: string;
    savedAt?: string;
}

/**
 * 草稿管理器类
 * 负责草稿的保存、加载和清除
 */
export class DraftManager {
    private storageKey: string;

    /**
     * 创建草稿管理器实例
     * @param storageKey - 存储键名
     */
    constructor(storageKey = 'publish_draft') {
        this.storageKey = storageKey;
    }

    /**
     * 保存草稿
     * @param data - 草稿数据
     */
    save(data: DraftData): void {
        const draft: DraftData = {
            ...data,
            savedAt: new Date().toISOString()
        };
        storage.set(this.storageKey, draft);
    }

    /**
     * 加载草稿
     * @returns 草稿数据，如果不存在则返回 null
     */
    load(): DraftData | null {
        return storage.get<DraftData | null>(this.storageKey, null);
    }

    /**
     * 清除草稿
     */
    clear(): void {
        storage.remove(this.storageKey);
    }

    /**
     * 检查是否存在草稿
     * @returns 是否存在草稿
     */
    hasDraft(): boolean {
        return storage.has(this.storageKey);
    }
}

export default DraftManager;
