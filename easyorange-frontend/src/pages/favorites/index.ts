import '../../styles/main.css';
import '../../styles/floating-nav.css';
import '../../styles/favorites.css';
import api from '../../api/index.js';
import { storage, toast, escapeHtml, getConditionNameFromString, calculateDiscount } from '../../utils/index.js';
import { favoriteManager } from '../../managers/index.js';
import header from '../../components/Header.js';
import { navigation } from '../../app/navigation.js';
import type { Product } from '../../types';
import BasePage from '../BasePage.js';

export type { Product };

type SortType = 'newest' | 'price_asc' | 'price_desc' | 'name';

interface FavoritesPageElements {
    favoritesGrid: HTMLElement | null;
    favoritesCount: HTMLElement | null;
    searchInput: HTMLInputElement | null;
    searchClear: HTMLElement | null;
    sortSelect: HTMLSelectElement | null;
    selectAllBtn: HTMLElement | null;
    deleteSelectedBtn: HTMLElement | null;
    loadingState: HTMLElement | null;
    emptyState: HTMLElement | null;
    noResults: HTMLElement | null;
    clearSearch: HTMLElement | null;
    userMenu: HTMLElement | null;
    userAvatarBtn: HTMLElement | null;
    userAvatarImg: HTMLImageElement | null;
    userName: HTMLElement | null;
    userDropdown: HTMLElement | null;
    logoutBtn: HTMLElement | null;
    loginBtn: HTMLElement | null;
    notificationBtn: HTMLElement | null;
}

interface ApiResponse<T> {
    data: T;
    code?: number;
    message?: string;
}

interface ConditionMap {
    [key: number]: string;
}

class FavoritesPage extends BasePage<FavoritesPageElements> {
    private favorites: Product[];
    private filteredFavorites: Product[];
    private selectedItems: Set<number>;
    private currentSort: SortType;
    private searchKeyword: string;

    constructor() {
        super();
        this.favorites = [];
        this.filteredFavorites = [];
        this.selectedItems = new Set<number>();
        this.currentSort = 'newest';
        this.searchKeyword = '';
    }

    protected cacheElements(): void {
        this.elements.favoritesGrid = this.querySelector<HTMLElement>('#favoritesGrid');
        this.elements.favoritesCount = this.querySelector<HTMLElement>('#favoritesCount');
        this.elements.searchInput = this.querySelector<HTMLInputElement>('#searchInput');
        this.elements.searchClear = this.querySelector<HTMLElement>('#searchClear');
        this.elements.sortSelect = this.querySelector<HTMLSelectElement>('#sortSelect');
        this.elements.selectAllBtn = this.querySelector<HTMLElement>('#selectAllBtn');
        this.elements.deleteSelectedBtn = this.querySelector<HTMLElement>('#deleteSelectedBtn');
        this.elements.loadingState = this.querySelector<HTMLElement>('#loadingState');
        this.elements.emptyState = this.querySelector<HTMLElement>('#emptyState');
        this.elements.noResults = this.querySelector<HTMLElement>('#noResults');
        this.elements.clearSearch = this.querySelector<HTMLElement>('#clearSearch');
        this.elements.userMenu = this.querySelector<HTMLElement>('#userMenu');
        this.elements.userAvatarBtn = this.querySelector<HTMLElement>('#userAvatarBtn');
        this.elements.userAvatarImg = this.querySelector<HTMLImageElement>('#userAvatarImg');
        this.elements.userName = this.querySelector<HTMLElement>('#userName');
        this.elements.userDropdown = this.querySelector<HTMLElement>('#userDropdown');
        this.elements.logoutBtn = this.querySelector<HTMLElement>('#logoutBtn');
        this.elements.loginBtn = this.querySelector<HTMLElement>('#loginBtn');
        this.elements.notificationBtn = this.querySelector<HTMLElement>('#notificationBtn');
    }

    protected bindEvents(): void {
        const { elements } = this;

        this.onEvent(elements.searchInput, 'input', this.debounce(() => {
            const val = elements.searchInput?.value.trim().toLowerCase() ?? '';
            this.searchKeyword = val;
            this.safe(elements.searchClear, (el) => {
                el.classList.toggle('visible', this.searchKeyword.length > 0);
            });
            this.filterAndRender();
        }, 300));

        this.onEvent(elements.searchClear, 'click', () => {
            this.safe(elements.searchInput, (el) => { el.value = ''; });
            this.searchKeyword = '';
            this.safe(elements.searchClear, (el) => { el.classList.remove('visible'); });
            this.filterAndRender();
        });

        this.onEvent(elements.sortSelect, 'change', () => {
            this.currentSort = (elements.sortSelect as HTMLSelectElement).value as SortType;
            this.filterAndRender();
        });

        this.onEvent(elements.selectAllBtn, 'click', () => { this.toggleSelectAll(); });
        this.onEvent(elements.deleteSelectedBtn, 'click', () => { this.deleteSelected(); });

        this.onEvent(elements.clearSearch, 'click', () => {
            this.safe(elements.searchInput, (el) => { el.value = ''; });
            this.searchKeyword = '';
            this.safe(elements.searchClear, (el) => { el.classList.remove('visible'); });
            this.filterAndRender();
        });
    }

    protected async onInit(): Promise<void> {
        if (!this.checkLoginStatus()) { return; }
        header.init();
        header.setActiveNav('favorites');
        await this.loadFavorites();
    }

    private checkLoginStatus(): boolean {
        const token = storage.get<string>('token');
        if (!token) {
            toast.warning('请先登录后查看收藏');
            setTimeout(() => { navigation.go('home', { query: { redirect: '/favorites.html' } }); }, 2000);
            return false;
        }
        return true;
    }

    private async loadFavorites(): Promise<void> {
        this.showLoading(true);
        try {
            const savedIds = Array.from(favoriteManager.getAll());
            if (!savedIds || savedIds.length === 0) {
                this.favorites = [];
                this.showEmpty(true);
                this.updateCount();
                return;
            }

            const response = await api.product.getProductsByIds(savedIds);
            const data = response.data ?? null;
            this.favorites = (data || []).filter((product): product is Product => product != null);
            this.filterAndRender();
            this.updateCount();
        } catch {
            toast.error('加载收藏失败');
            this.showEmpty(true);
        } finally {
            this.showLoading(false);
        }
    }

    private filterAndRender(): void {
        this.filteredFavorites = this.favorites.filter((item): boolean => {
            if (!this.searchKeyword) { return true; }
            const title = item.title ?? '';
            const desc = item.description ?? '';
            return title.toLowerCase().includes(this.searchKeyword) ||
                   desc.toLowerCase().includes(this.searchKeyword);
        });
        this.sortFavorites();
        this.renderFavorites();
        this.updateCount();
    }

    private sortFavorites(): void {
        switch (this.currentSort) {
            case 'newest':
                this.filteredFavorites.sort((a, b) =>
                    new Date(b.createTime || 0).getTime() - new Date(a.createTime || 0).getTime());
                break;
            case 'price_asc':
                this.filteredFavorites.sort((a, b) => (a.price || 0) - (b.price || 0));
                break;
            case 'price_desc':
                this.filteredFavorites.sort((a, b) => (b.price || 0) - (a.price || 0));
                break;
            case 'name':
                this.filteredFavorites.sort((a, b) => (a.title || '').localeCompare(b.title || ''));
                break;
        }
    }

    private renderFavorites(): void {
        const { elements } = this;
        if (this.searchKeyword && this.filteredFavorites.length === 0) {
            this.showNoResults(true);
            this.safe(elements.favoritesGrid, (el) => { el.innerHTML = ''; });
            return;
        }

        this.showNoResults(false);
        this.showEmpty(this.filteredFavorites.length === 0);

        if (this.filteredFavorites.length === 0) {
            this.safe(elements.favoritesGrid, (el) => { el.innerHTML = ''; });
            return;
        }

        const fragment = document.createDocumentFragment();
        this.filteredFavorites.forEach((item, index) => {
            fragment.appendChild(this.createFavoriteCard(item, index));
        });

        this.safe(elements.favoritesGrid, (el) => {
            el.innerHTML = '';
            el.appendChild(fragment);
        });
    }

    private createFavoriteCard(item: Product, index: number): HTMLElement {
        const card = document.createElement('div');
        card.className = 'favorite-card';
        card.dataset.id = String(item.id);
        card.style.animationDelay = `${index * 50}ms`;

        const isSelected = this.selectedItems.has(item.id);
        if (isSelected) { card.classList.add('selected'); }

        const imageUrl = item.images?.[0] ?? 'https://via.placeholder.com/300x200?text=No+Image';
        const conditionText = getConditionNameFromString(item.condition);
        const discount = calculateDiscount(item.price, item.originalPrice ?? undefined);

        card.innerHTML =
            `<div class="card-checkbox">` +
                `<input type="checkbox" id="fav-${item.id}" ${isSelected ? 'checked' : ''}>` +
                `<label for="fav-${item.id}"></label>` +
            `</div>` +
            `<div class="card-image">` +
                `<img src="${escapeHtml(imageUrl)}" alt="${escapeHtml(item.title)}" loading="lazy">${
                discount ? `<span class="discount-badge">${escapeHtml(String(discount))}折</span>` : ''
                }<span class="condition-badge">${escapeHtml(conditionText)}</span>` +
            `</div>` +
            `<div class="card-content">` +
                `<h3 class="card-title">${escapeHtml(item.title)}</h3>` +
                `<p class="card-description">${escapeHtml((item.description || '').substring(0, 60) + (item.description && item.description.length > 60 ? '...' : ''))}</p>` +
                `<div class="card-price">` +
                    `<span class="current-price">¥${(item.price || 0).toFixed(2)}</span>${
                    item.originalPrice && item.originalPrice > item.price ? `<span class="original-price">¥${item.originalPrice.toFixed(2)}</span>` : ''
                }</div>` +
                `<div class="card-meta">` +
                    `<span class="seller">` +
                        `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">` +
                            `<path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>` +
                            `<circle cx="12" cy="7" r="4"/>` +
                        `</svg>` +
                        `${escapeHtml(item.sellerName || '匿名卖家')}` +
                    `</span>` +
                    `<span class="location">` +
                        `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">` +
                            `<path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/>` +
                            `<circle cx="12" cy="10" r="3"/>` +
                        `</svg>` +
                        `${escapeHtml(item.location || '未知')}` +
                    `</span>` +
                `</div>` +
            `</div>` +
            `<div class="card-actions">` +
                `<button class="action-btn view-btn" data-id="${item.id}">` +
                    `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">` +
                        `<path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>` +
                        `<circle cx="12" cy="12" r="3"/>` +
                    `</svg>` +
                    `查看` +
                `</button>` +
                `<button class="action-btn remove-btn" data-id="${item.id}">` +
                    `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">` +
                        `<polyline points="3 6 5 6 21 6"/>` +
                        `<path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>` +
                    `</svg>` +
                    `移除` +
                `</button>` +
            `</div>`;

        const checkbox = card.querySelector('input[type="checkbox"]') as HTMLInputElement;
        checkbox.addEventListener('change', () => { this.toggleSelect(item.id); });

        const viewBtn = card.querySelector('.view-btn') as HTMLElement;
        viewBtn.addEventListener('click', () => {
            navigation.updateQuery({ product: item.id }, 'push');
        });

        const removeBtn = card.querySelector('.remove-btn') as HTMLElement;
        removeBtn.addEventListener('click', () => { this.removeFavorite(item.id); });

        const cardImage = card.querySelector('.card-image') as HTMLElement;
        cardImage.addEventListener('click', () => {
            navigation.updateQuery({ product: item.id }, 'push');
        });
        cardImage.style.cursor = 'pointer';

        return card;
    }

    private toggleSelect(id: number): void {
        if (this.selectedItems.has(id)) {
            this.selectedItems.delete(id);
        } else {
            this.selectedItems.add(id);
        }
        this.updateSelectUI();
    }

    private toggleSelectAll(): void {
        const { elements } = this;
        if (this.selectedItems.size === this.filteredFavorites.length) {
            this.selectedItems.clear();
            this.safe(elements.selectAllBtn, (el) => {
                el.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">' +
                    '<polyline points="9 11 12 14 22 4"/>' +
                    '<path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/>' +
                    '</svg>全选';
            });
        } else {
            this.filteredFavorites.forEach((item) => { this.selectedItems.add(item.id); });
            this.safe(elements.selectAllBtn, (el) => {
                el.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">' +
                    '<rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>' +
                    '</svg>取消全选';
            });
        }
        this.updateSelectUI();
        this.updateCardSelections();
    }

    private updateSelectUI(): void {
        const { elements } = this;
        const hasSelection = this.selectedItems.size > 0;
        this.safe(elements.deleteSelectedBtn, (el) => {
            el.style.display = hasSelection ? 'inline-flex' : 'none';
        });

        const isAllSelected = this.selectedItems.size === this.filteredFavorites.length && this.filteredFavorites.length > 0;
        this.safe(elements.selectAllBtn, (el) => {
            el.innerHTML = isAllSelected
                ? '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">' +
                    '<rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>' +
                    '</svg>取消全选'
                : '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">' +
                    '<polyline points="9 11 12 14 22 4"/>' +
                    '<path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/>' +
                    '</svg>全选';
        });
    }

    private updateCardSelections(): void {
        const { elements } = this;
        this.safe(elements.favoritesGrid, (grid) => {
            const cards = grid.querySelectorAll('.favorite-card');
            cards.forEach((card) => {
                const cardEl = card as HTMLElement;
                const id = parseInt(cardEl.dataset.id || '0', 10);
                const checkbox = cardEl.querySelector('input[type="checkbox"]') as HTMLInputElement;
                if (this.selectedItems.has(id)) {
                    cardEl.classList.add('selected');
                    checkbox.checked = true;
                } else {
                    cardEl.classList.remove('selected');
                    checkbox.checked = false;
                }
            });
        });
    }

    private removeFavorite(id: number): void {
        favoriteManager.remove(id);
        this.favorites = this.favorites.filter((item) => item.id !== id);
        this.selectedItems.delete(id);
        this.filterAndRender();
        this.updateSelectUI();
        toast.success('已从收藏中移除');
    }

    private deleteSelected(): void {
        if (this.selectedItems.size === 0) {
            toast.warning('请先选择要删除的商品');
            return;
        }
        const count = this.selectedItems.size;
        favoriteManager.removeMany(Array.from(this.selectedItems));
        this.favorites = this.favorites.filter((item) => !this.selectedItems.has(item.id));
        this.selectedItems.clear();
        this.filterAndRender();
        this.updateSelectUI();
        toast.success(`已删除 ${count} 件商品`);
    }

    private updateCount(): void {
        const { elements } = this;
        const count = this.filteredFavorites.length;
        this.safe(elements.favoritesCount, (el) => {
            const countNumber = el.querySelector('.count-number');
            if (countNumber) { countNumber.textContent = String(count); }
        });
    }

    private showLoading(show: boolean): void {
        const { elements } = this;
        this.safe(elements.loadingState, (el) => { el.style.display = show ? 'flex' : 'none'; });
        this.safe(elements.favoritesGrid, (el) => { el.style.display = show ? 'none' : 'grid'; });
    }

    private showEmpty(show: boolean): void {
        this.safe(this.elements.emptyState, (el) => {
            el.style.display = show && !this.searchKeyword ? 'flex' : 'none';
        });
    }

    private showNoResults(show: boolean): void {
        this.safe(this.elements.noResults, (el) => { el.style.display = show ? 'flex' : 'none'; });
    }
}

let favoritesPageInstance: FavoritesPage | null = null;
document.addEventListener('DOMContentLoaded', () => {
    favoritesPageInstance = new FavoritesPage();
    favoritesPageInstance.init().catch(() => {
        // 初始化失败时静默处理
    });
});

export { FavoritesPage };
export type { FavoritesPageElements, SortType, ApiResponse, ConditionMap };
export default FavoritesPage;