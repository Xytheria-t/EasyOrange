/**
 * @fileoverview 存储工具函数
 */

export interface StorageItem<T = unknown> {
    value: T;
    expireAt?: number;
}

const storage = {
    get<T = unknown>(key: string, defaultValue: T | null = null): T | null {
        const item = localStorage.getItem(key);
        if (!item) {return defaultValue;}
        
        try {
            const parsed = JSON.parse(item) as StorageItem<T>;
            if (parsed.expireAt && Date.now() > parsed.expireAt) {
                localStorage.removeItem(key);
                return defaultValue;
            }
            return parsed.value;
        } catch {
            return defaultValue;
        }
    },

    has(key: string): boolean {
        return localStorage.getItem(key) !== null;
    },

    set<T>(key: string, value: T, ttlMs?: number): void {
        const item: StorageItem<T> = {
            value,
            expireAt: ttlMs ? Date.now() + ttlMs : undefined
        };
        localStorage.setItem(key, JSON.stringify(item));
    },

    remove(key: string): void {
        localStorage.removeItem(key);
    },

    clear(): void {
        localStorage.clear();
    }
};

const StorageUtils = {
    get: storage.get,
    has: storage.has,
    set: storage.set,
    remove: storage.remove,
    clear: storage.clear
};

export { storage, StorageUtils };
