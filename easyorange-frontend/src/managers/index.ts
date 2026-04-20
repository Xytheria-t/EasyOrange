/**
 * @fileoverview 状态管理器 - 精简版
 * @description 收藏管理器
 */

// ============ 收藏管理器 ============
export type FavoriteChangeHandler = (productId: number, isFavorited: boolean) => void;

class FavoriteManager {
    private _ids: Set<number>;
    private _listeners: Map<string, FavoriteChangeHandler>;

    constructor() {
        this._ids = new Set<number>();
        this._listeners = new Map<string, FavoriteChangeHandler>();
        this._loadFromStorage();
    }

    private _loadFromStorage(): void {
        try {
            const raw = localStorage.getItem('favorites');
            if (raw) {
                const ids = JSON.parse(raw) as unknown[];
                if (Array.isArray(ids)) {
                    this._ids = new Set(ids.filter((id): id is number => typeof id === 'number'));
                }
            }
        } catch {
            this._ids = new Set<number>();
        }
    }

    private _saveToStorage(): void {
        try {
            localStorage.setItem('favorites', JSON.stringify(Array.from(this._ids)));
        } catch {
            // 静默处理
        }
    }

    private _notify(productId: number, isFavorited: boolean): void {
        this._listeners.forEach((handler) => {
            handler(productId, isFavorited);
        });
    }

    has(productId: number): boolean {
        return this._ids.has(productId);
    }

    getAll(): ReadonlySet<number> {
        return this._ids;
    }

    get size(): number {
        return this._ids.size;
    }

    add(productId: number): void {
        if (this._ids.has(productId)) {return;}
        this._ids.add(productId);
        this._saveToStorage();
        this._notify(productId, true);
    }

    remove(productId: number): void {
        if (!this._ids.has(productId)) {return;}
        this._ids.delete(productId);
        this._saveToStorage();
        this._notify(productId, false);
    }

    removeMany(productIds: number[]): void {
        productIds.forEach(id => {
            this._ids.delete(id);
            this._notify(id, false);
        });
        this._saveToStorage();
    }

    toggle(productId: number): boolean {
        if (this._ids.has(productId)) {
            this.remove(productId);
            return false;
        }
        this.add(productId);
        return true;
    }

    onChange(handler: FavoriteChangeHandler): () => void {
        const id = `listener_${Date.now()}_${Math.random().toString(36).slice(2, 9)}`;
        this._listeners.set(id, handler);
        return () => { this._listeners.delete(id); };
    }

    syncFromApi(productIds: number[]): void {
        this._ids = new Set(productIds);
        this._saveToStorage();
    }

    reset(): void {
        this._ids.clear();
        this._listeners.clear();
        this._loadFromStorage();
    }
}

export const favoriteManager = new FavoriteManager();
