/**
 * @fileoverview 筛选管理器 — filter state, URL sync, client-side filtering
 */

import { escapeHtml } from '../../utils/index.js';
import type {
    SortOption, ProductFilters, FilterTag,
    ProductListItem, QueryParams
} from './types.js';
import type { Category } from '../../types';

export class FilterManager {
    filters: ProductFilters = {
        keyword: '', categoryId: null, priceMin: null, priceMax: null,
        conditions: [], sort: 'newest', quickFilter: 'all'
    };

    private elements: {
        searchInput: HTMLInputElement | null;
        searchClear: HTMLElement | null;
        filterBadge: HTMLElement | null;
        filterSidebar: HTMLElement | null;
        categoryFilter: HTMLElement | null;
        priceMin: HTMLInputElement | null;
        priceMax: HTMLInputElement | null;
        conditionFilter: HTMLElement | null;
        sortFilter: HTMLElement | null;
        activeFilters: HTMLElement | null;
        activeFilterTags: HTMLElement | null;
        presetBtns: NodeListOf<HTMLElement>;
        quickFilterBtns: NodeListOf<HTMLElement>;
    };
    private categories: Category[] = [];
    private onApply?: () => void;
    private onReset?: () => void;

    constructor(
        elements: FilterManager['elements'],
        categories: Category[],
        onApply?: () => void,
        onReset?: () => void
    ) {
        this.elements = elements;
        this.categories = categories;
        this.onApply = onApply;
        this.onReset = onReset;
    }

    restoreFromUrl(): void {
        const params = new URLSearchParams(window.location.search);
        this.filters.keyword = params.get('q') || '';
        const catParam = params.get('cat');
        this.filters.categoryId = catParam ? parseInt(catParam) : null;
        const minParam = params.get('min');
        this.filters.priceMin = minParam ? parseFloat(minParam) : null;
        const maxParam = params.get('max');
        this.filters.priceMax = maxParam ? parseFloat(maxParam) : null;
        this.filters.sort = (params.get('sort') as SortOption) || 'newest';

        if (this.elements.searchInput) {
            this.elements.searchInput.value = this.filters.keyword;
            this.elements.searchClear?.classList.toggle('visible', this.filters.keyword.length > 0);
        }
        if (this.filters.priceMin && this.elements.priceMin) {
            this.elements.priceMin.value = String(this.filters.priceMin);
        }
        if (this.filters.priceMax && this.elements.priceMax) {
            this.elements.priceMax.value = String(this.filters.priceMax);
        }
        const sortRadio = this.elements.sortFilter?.querySelector<HTMLInputElement>(`input[value="${this.filters.sort}"]`);
        if (sortRadio) {sortRadio.checked = true;}
    }

    saveToUrl(): void {
        const params = new URLSearchParams();
        if (this.filters.keyword) {params.set('q', this.filters.keyword);}
        if (this.filters.categoryId) {params.set('cat', String(this.filters.categoryId));}
        if (this.filters.priceMin) {params.set('min', String(this.filters.priceMin));}
        if (this.filters.priceMax) {params.set('max', String(this.filters.priceMax));}
        if (this.filters.sort !== 'newest') {params.set('sort', this.filters.sort);}
        const newUrl = params.toString()
            ? `${window.location.pathname}?${params.toString()}`
            : window.location.pathname;
        history.replaceState(null, '', newUrl);
    }

    buildQueryParams(currentPage: number, pageSize: number): QueryParams {
        const params: QueryParams = { page: currentPage, size: pageSize };
        if (this.filters.keyword) {params.keyword = this.filters.keyword;}
        if (this.filters.categoryId) {params.categoryId = this.filters.categoryId;}
        if (this.filters.priceMin) {params.minPrice = this.filters.priceMin;}
        if (this.filters.priceMax) {params.maxPrice = this.filters.priceMax;}

        switch (this.filters.sort) {
            case 'price_asc': params.sortBy = 'price'; params.sortOrder = 'asc'; break;
            case 'price_desc': params.sortBy = 'price'; params.sortOrder = 'desc'; break;
            case 'popular': params.sortBy = 'viewCount'; params.sortOrder = 'desc'; break;
            default: params.sortBy = 'createTime'; params.sortOrder = 'desc';
        }
        return params;
    }

    applyClientFilters(products: ProductListItem[]): ProductListItem[] {
        let filtered = products.slice();

        if (this.filters.keyword) {
            const keyword = this.filters.keyword.toLowerCase();
            filtered = filtered.filter(p =>
                (p.name?.toLowerCase().includes(keyword)) ||
                (p.description?.toLowerCase().includes(keyword)) ||
                (p.categoryName?.toLowerCase().includes(keyword))
            );
        }
        if (this.filters.categoryId) {
            filtered = filtered.filter(p => p.categoryId === this.filters.categoryId);
        }
        if (this.filters.priceMin) {
            const minPrice = this.filters.priceMin;
            filtered = filtered.filter(p => p.price >= minPrice);
        }
        if (this.filters.priceMax) {
            const maxPrice = this.filters.priceMax;
            filtered = filtered.filter(p => p.price <= maxPrice);
        }
        if (this.filters.conditions.length > 0) {
            filtered = filtered.filter(p => this.filters.conditions.includes(p.conditionLevel));
        }

        switch (this.filters.quickFilter) {
            case 'hot': filtered = filtered.filter(p => (p.viewCount || 0) > 200); break;
            case 'discount': filtered = filtered.filter(p => p.originalPrice && p.originalPrice > p.price); break;
            case 'newArrival': filtered = filtered.filter(p => p.conditionLevel === 1); break;
        }

        switch (this.filters.sort) {
            case 'price_asc': filtered.sort((a, b) => a.price - b.price); break;
            case 'price_desc': filtered.sort((a, b) => b.price - a.price); break;
            case 'popular': filtered.sort((a, b) => (b.viewCount || 0) - (a.viewCount || 0)); break;
            default: filtered.sort((a, b) => new Date(b.createTime).getTime() - new Date(a.createTime).getTime());
        }

        return filtered;
    }

    apply(): void {
        if (this.elements.priceMin) {
            this.filters.priceMin = this.elements.priceMin.value ? parseFloat(this.elements.priceMin.value) : null;
        }
        if (this.elements.priceMax) {
            this.filters.priceMax = this.elements.priceMax.value ? parseFloat(this.elements.priceMax.value) : null;
        }
        this.filters.conditions = [];
        this.elements.conditionFilter?.querySelectorAll<HTMLInputElement>('input:checked')
            .forEach(input => this.filters.conditions.push(parseInt(input.value)));

        const sortRadio = this.elements.sortFilter?.querySelector<HTMLInputElement>('input:checked');
        this.filters.sort = sortRadio ? sortRadio.value as SortOption : 'newest';

        this.saveToUrl();
        this.updateActiveFilterTags();
        this.closeSidebar();
        this.onApply?.();
    }

    reset(): void {
        this.filters = {
            keyword: this.filters.keyword, categoryId: null,
            priceMin: null, priceMax: null, conditions: [],
            sort: 'newest', quickFilter: 'all'
        };
        this.elements.categoryFilter?.querySelectorAll<HTMLElement>('.filter-option')
            .forEach(b => b.classList.remove('active'));
        if (this.elements.priceMin) {this.elements.priceMin.value = '';}
        if (this.elements.priceMax) {this.elements.priceMax.value = '';}
        this.elements.presetBtns?.forEach(b => b.classList.remove('active'));
        this.elements.conditionFilter?.querySelectorAll<HTMLInputElement>('input')
            .forEach(input => input.checked = false);
        const newestRadio = this.elements.sortFilter?.querySelector<HTMLInputElement>('input[value="newest"]');
        if (newestRadio) {newestRadio.checked = true;}
        this.onReset?.();
    }

    clearAll(): void {
        this.filters = {
            keyword: '', categoryId: null, priceMin: null, priceMax: null,
            conditions: [], sort: 'newest', quickFilter: 'all'
        };
        if (this.elements.searchInput) {this.elements.searchInput.value = '';}
        this.elements.searchClear?.classList.remove('visible');
        this.reset();
        this.updateActiveFilterTags();
        this.elements.quickFilterBtns?.forEach(b => b.classList.remove('active'));
        this.elements.quickFilterBtns?.[0]?.classList.add('active');
    }

    remove(key: string): void {
        switch (key) {
            case 'keyword':
                this.filters.keyword = '';
                if (this.elements.searchInput) {this.elements.searchInput.value = '';}
                this.elements.searchClear?.classList.remove('visible');
                break;
            case 'categoryId':
                this.filters.categoryId = null;
                this.elements.categoryFilter?.querySelectorAll<HTMLElement>('.filter-option')
                    .forEach(b => b.classList.remove('active'));
                break;
            case 'price':
                this.filters.priceMin = null;
                this.filters.priceMax = null;
                if (this.elements.priceMin) {this.elements.priceMin.value = '';}
                if (this.elements.priceMax) {this.elements.priceMax.value = '';}
                this.elements.presetBtns?.forEach(b => b.classList.remove('active'));
                break;
        }
    }

    toggleSidebar(): void {
        this.elements.filterSidebar?.classList.toggle('open');
    }

    closeSidebar(): void {
        this.elements.filterSidebar?.classList.remove('open');
    }

    renderCategoryFilters(): void {
        if (!this.elements.categoryFilter) {return;}
        let html = '';
        this.categories.forEach(category => {
            html += `<button class="filter-option" data-id="${escapeHtml(String(category.id))}">` +
                `<span>${escapeHtml(category.icon || '📁')}</span>` +
                `<span>${escapeHtml(category.name)}</span></button>`;
        });
        this.elements.categoryFilter.innerHTML = html;
    }

    updateActiveFilterTags(): void {
        const tags: FilterTag[] = [];
        let activeCount = 0;

        if (this.filters.keyword) {
            tags.push({ label: `搜索: ${this.filters.keyword}`, key: 'keyword' });
            activeCount++;
        }
        if (this.filters.categoryId) {
            const category = this.categories.find(c => c.id === this.filters.categoryId);
            if (category) { tags.push({ label: category.name, key: 'categoryId' }); activeCount++; }
        }
        if (this.filters.priceMin || this.filters.priceMax) {
            tags.push({ label: `¥${this.filters.priceMin || 0} - ¥${this.filters.priceMax || '∞'}`, key: 'price' });
            activeCount++;
        }
        if (this.filters.conditions.length > 0) {activeCount += this.filters.conditions.length;}

        const { activeFilters, filterBadge, activeFilterTags } = this.elements;
        if (activeCount > 0) {
            if (activeFilters) { activeFilters.style.display = 'flex'; }
            if (filterBadge) {
                filterBadge.style.display = 'flex';
                filterBadge.textContent = String(activeCount);
            }

            let tagsHtml = '';
            tags.forEach(tag => {
                tagsHtml += `<span class="filter-tag">${tag.label}` +
                    `<button data-key="${tag.key}" aria-label="移除筛选">` +
                        '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">' +
                            '<line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>' +
                        '</svg></button></span>';
            });
            if (activeFilterTags) {
                activeFilterTags.innerHTML = tagsHtml;
                activeFilterTags.querySelectorAll<HTMLElement>('button').forEach(btn => {
                    btn.addEventListener('click', () => {
                        this.remove(btn.dataset.key || '');
                    });
                });
            }
        } else {
            if (activeFilters) { activeFilters.style.display = 'none'; }
            if (filterBadge) { filterBadge.style.display = 'none'; }
        }
    }

    get keyword(): string { return this.filters.keyword; }
    set keyword(v: string) { this.filters.keyword = v; }
}
