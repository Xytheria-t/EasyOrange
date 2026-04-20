/**
 * @fileoverview 浏览历史管理器
 */

import { storage, toast, escapeHtml } from '../../utils/index.js';
import type { BrowseHistoryItem, ProductListItem } from './types.js';

const MAX_HISTORY = 10;
const DISPLAY_COUNT = 5;

export class BrowseHistoryManager {
    private items: BrowseHistoryItem[] = [];
    private elements: {
        recentHistory: HTMLElement | null;
        historyItems: HTMLElement | null;
        clearHistory: HTMLElement | null;
    };
    private onItemSelect?: (product: ProductListItem | undefined) => void;
    private allProducts?: ProductListItem[];

    constructor(
        elements: BrowseHistoryManager['elements'],
        onItemSelect?: (product: ProductListItem | undefined) => void
    ) {
        this.elements = elements;
        this.onItemSelect = onItemSelect;
        this.load();
    }

    private load(): void {
        const saved = storage.get<BrowseHistoryItem[]>('browseHistory');
        if (saved && Array.isArray(saved)) {this.items = saved;}
    }

    private save(): void {
        storage.set('browseHistory', this.items.slice(0, MAX_HISTORY));
    }

    add(product: ProductListItem): void {
        const idx = this.items.findIndex(i => i.id === product.id);
        if (idx > -1) {this.items.splice(idx, 1);}
        this.items.unshift({
            id: product.id, name: product.name, price: product.price,
            image: product.images?.[0] ?? null
        });
        if (this.items.length > MAX_HISTORY) {this.items = this.items.slice(0, MAX_HISTORY);}
        this.save();
    }

    clear(): void {
        this.items = [];
        this.save();
        this.render();
        toast.info('浏览记录已清空');
    }

    setProductSource(products: ProductListItem[]): void {
        this.allProducts = products;
    }

    render(): void {
        const { historyItems, recentHistory } = this.elements;
        if (!historyItems || !recentHistory) {return;}

        if (this.items.length === 0) {
            recentHistory.style.display = 'none';
            return;
        }
        recentHistory.style.display = 'block';

        let html = '';
        this.items.slice(0, DISPLAY_COUNT).forEach(item => {
            html += `<div class="history-item" data-id="${escapeHtml(String(item.id))}">` +
                `<img src="${escapeHtml(item.image || `https://picsum.photos/seed/${item.id}/100/100`)}" alt="${escapeHtml(item.name)}">` +
                '<div class="history-item-info">' +
                    `<div class="history-item-name">${escapeHtml(item.name)}</div>` +
                    `<div class="history-item-price">¥${escapeHtml(String(item.price))}</div>` +
                '</div></div>';
        });
        historyItems.innerHTML = html;

        historyItems.querySelectorAll<HTMLElement>('.history-item').forEach(item => {
            item.addEventListener('click', () => {
                const productId = parseInt(item.dataset.id || '0');
                const product = this.allProducts?.find(p => p.id === productId);
                this.onItemSelect?.(product);
            });
        });
    }
}
