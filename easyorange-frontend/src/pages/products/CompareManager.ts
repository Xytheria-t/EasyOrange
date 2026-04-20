/**
 * @fileoverview 商品对比管理器
 */

import { storage, toast, escapeHtml } from '../../utils/index.js';
import type { CompareItem, ProductListItem } from './types.js';

export class CompareManager {
    private compareList: CompareItem[] = [];
    private elements: {
        compareBar: HTMLElement | null;
        compareCount: HTMLElement | null;
        compareItems: HTMLElement | null;
        compareModal: HTMLElement | null;
        compareModalOverlay: HTMLElement | null;
        compareModalClose: HTMLElement | null;
        compareModalBody: HTMLElement | null;
        clearCompare: HTMLElement | null;
        startCompare: HTMLElement | null;
    };
    private onStateChange?: () => void;

    constructor(elements: CompareManager['elements'], onStateChange?: () => void) {
        this.elements = elements;
        this.onStateChange = onStateChange;
        this.load();
    }

    private load(): void {
        const saved = storage.get<CompareItem[]>('compareList');
        if (saved && Array.isArray(saved)) {
            this.compareList = saved;
        }
        this.updateBar();
    }

    private save(): void {
        storage.set('compareList', this.compareList);
    }

    toggle(product: ProductListItem): void {
        const existingIndex = this.compareList.findIndex(p => p.id === product.id);
        if (existingIndex > -1) {
            this.compareList.splice(existingIndex, 1);
            toast.info('已从对比列表移除');
        } else {
            if (this.compareList.length >= 4) {
                toast.warning('最多只能对比4件商品');
                return;
            }
            this.compareList.push({
                id: product.id, name: product.name, price: product.price,
                originalPrice: product.originalPrice,
                image: product.images?.[0] ?? null,
                conditionName: product.conditionName, conditionIcon: product.conditionIcon,
                categoryName: product.categoryName, location: product.location,
                viewCount: product.viewCount
            });
            toast.success('已添加到对比列表');
        }
        this.save();
        this.updateBar();
        this.onStateChange?.();
    }

    remove(productId: number): void {
        const idx = this.compareList.findIndex(p => p.id === productId);
        if (idx > -1) {
            this.compareList.splice(idx, 1);
            this.save();
            this.updateBar();
            this.onStateChange?.();
        }
    }

    clear(): void {
        this.compareList = [];
        this.save();
        this.updateBar();
        this.onStateChange?.();
        toast.info('对比列表已清空');
    }

    isSelected(productId: number): boolean {
        return this.compareList.some(p => p.id === productId);
    }

    get list(): CompareItem[] { return this.compareList; }

    openModal(): void {
        if (this.compareList.length < 2) {
            toast.warning('请至少选择2件商品进行对比');
            return;
        }
        this.renderTable();
        this.elements.compareModal?.classList.add('open');
        document.body.style.overflow = 'hidden';
    }

    closeModal(): void {
        this.elements.compareModal?.classList.remove('open');
        document.body.style.overflow = '';
    }

    private updateBar(): void {
        const { compareBar, compareCount, compareItems } = this.elements;
        if (!compareBar) {return;}
        if (this.compareList.length === 0) {
            compareBar.classList.remove('visible');
            return;
        }
        compareBar.classList.add('visible');
        if (compareCount) {compareCount.textContent = String(this.compareList.length);}
        if (!compareItems) {return;}

        let html = '';
        this.compareList.forEach(item => {
            html += `<div class="compare-item" data-id="${escapeHtml(String(item.id))}">` +
                `<img src="${escapeHtml(item.image || `https://picsum.photos/seed/${item.id}/100/100`)}" alt="${escapeHtml(item.name)}">` +
                `<span class="compare-item-name">${escapeHtml(item.name)}</span>` +
                `<button class="compare-item-remove" data-id="${escapeHtml(String(item.id))}">` +
                    '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">' +
                        '<line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>' +
                    '</svg></button></div>';
        });
        compareItems.innerHTML = html;

        compareItems.querySelectorAll<HTMLElement>('.compare-item-remove').forEach(btn => {
            btn.addEventListener('click', (e: MouseEvent) => {
                e.stopPropagation();
                this.remove(parseInt(btn.dataset.id || '0'));
            });
        });
    }

    private renderTable(): void {
        const { compareModalBody } = this.elements;
        if (!compareModalBody) {return;}

        const minPrice = Math.min(...this.compareList.map(p => p.price));
        const fields: Array<{ key: keyof CompareItem; label: string }> = [
            { key: 'image', label: '商品' }, { key: 'price', label: '价格' },
            { key: 'conditionName', label: '成色' }, { key: 'categoryName', label: '分类' },
            { key: 'location', label: '交易地点' }, { key: 'viewCount', label: '浏览量' }
        ];

        let html = '<table class="compare-table"><thead><tr><th>属性</th>';
        this.compareList.forEach(product => {
            html += '<th class="product-header">' +
                `<img class="product-image" src="${escapeHtml(product.image || `https://picsum.photos/seed/${product.id}/200/200`)}" alt="${escapeHtml(product.name)}">` +
                `<div class="product-name">${escapeHtml(product.name)}</div></th>`;
        });
        html += '</tr></thead><tbody>';

        fields.forEach(field => {
            if (field.key === 'image') {return;}
            html += `<tr><th>${escapeHtml(field.label)}</th>`;
            this.compareList.forEach(product => {
                let value: string | number | null = product[field.key] as string | number | null;
                let isBest = false;
                if (field.key === 'price') { value = `¥${value}`; isBest = product.price === minPrice; }
                else if (field.key === 'viewCount') { value = `${value}次`; }
                html += `<td class="${isBest ? 'best-value' : ''}">${escapeHtml(String(value))}</td>`;
            });
            html += '</tr>';
        });
        html += '</tbody></table>';
        compareModalBody.innerHTML = html;
    }
}
