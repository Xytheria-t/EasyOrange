/**
 * @fileoverview 商品列表页主控制器
 */

import '../../styles/main.css';
import '../../styles/floating-nav.css';
import '../../styles/products.css';
import api from '../../api/index.js';
import { toast, debounce } from '../../utils/index.js';
import { favoriteManager } from '../../managers/index.js';
import header from '../../components/Header.js';
import { FilterManager } from './FilterManager.js';
import { CompareManager } from './CompareManager.js';
import { BrowseHistoryManager } from './BrowseHistoryManager.js';
import { ProductCardRenderer } from './ProductCardRenderer.js';
import { ProductDetailModal } from './ProductDetailModal.js';
import { navigation } from '../../app/navigation.js';
import { generateMockProducts } from './mockData.js';
import type { Category } from '../../types';
import type { ProductListItem, QuickFilterOption, ViewMode } from './types.js';

const eventManager = {
    on: (element: EventTarget | null, event: string, handler: EventListener) => {
        if (element) {
            element.addEventListener(event, handler);
        }
    }
};

interface ProductsPageElements {
    searchInput: HTMLInputElement | null;
    searchClear: HTMLElement | null;
    searchBtn: HTMLElement | null;
    filterToggle: HTMLElement | null;
    filterBadge: HTMLElement | null;
    filterSidebar: HTMLElement | null;
    filterClose: HTMLElement | null;
    categoryFilter: HTMLElement | null;
    priceMin: HTMLInputElement | null;
    priceMax: HTMLInputElement | null;
    conditionFilter: HTMLElement | null;
    sortFilter: HTMLElement | null;
    resetFilter: HTMLElement | null;
    applyFilter: HTMLElement | null;
    resultsCount: HTMLElement | null;
    activeFilters: HTMLElement | null;
    activeFilterTags: HTMLElement | null;
    clearAllFilters: HTMLElement | null;
    productsGrid: HTMLElement | null;
    loadingMore: HTMLElement | null;
    noResults: HTMLElement | null;
    resetSearch: HTMLElement | null;
    loadMoreTrigger: HTMLElement | null;
    productDetailModal: HTMLElement | null;
    modalOverlay: HTMLElement | null;
    modalClose: HTMLElement | null;
    modalMainImage: HTMLImageElement | null;
    galleryPrev: HTMLElement | null;
    galleryNext: HTMLElement | null;
    galleryCounter: HTMLElement | null;
    galleryThumbnails: HTMLElement | null;
    modalCondition: HTMLElement | null;
    modalDiscount: HTMLElement | null;
    modalTitle: HTMLElement | null;
    modalPrice: HTMLElement | null;
    modalOriginalPrice: HTMLElement | null;
    modalLocation: HTMLElement | null;
    modalDate: HTMLElement | null;
    modalViews: HTMLElement | null;
    modalDescription: HTMLElement | null;
    modalSellerAvatar: HTMLImageElement | null;
    modalSellerName: HTMLElement | null;
    modalSellerStats: HTMLElement | null;
    modalContact: HTMLElement | null;
    modalFavorite: HTMLElement | null;
    modalShare: HTMLElement | null;
    modalPriceTrend: HTMLElement | null;
    priceTrendChart: HTMLElement | null;
    trendHighPrice: HTMLElement | null;
    trendLowPrice: HTMLElement | null;
    trendAvgPrice: HTMLElement | null;
    modalSimilar: HTMLElement | null;
    similarProducts: HTMLElement | null;
    userMenu: HTMLElement | null;
    userAvatarBtn: HTMLElement | null;
    userAvatarImg: HTMLImageElement | null;
    userName: HTMLElement | null;
    userDropdown: HTMLElement | null;
    logoutBtn: HTMLElement | null;
    loginBtn: HTMLElement | null;
    viewBtns: NodeListOf<HTMLElement>;
    presetBtns: NodeListOf<HTMLElement>;
    shareModal: HTMLElement | null;
    shareModalOverlay: HTMLElement | null;
    shareModalClose: HTMLElement | null;
    shareLinkInput: HTMLInputElement | null;
    copyLinkBtn: HTMLElement | null;
    shareOptions: NodeListOf<HTMLElement>;
    compareBar: HTMLElement | null;
    compareCount: HTMLElement | null;
    compareItems: HTMLElement | null;
    clearCompare: HTMLElement | null;
    startCompare: HTMLElement | null;
    compareModal: HTMLElement | null;
    compareModalOverlay: HTMLElement | null;
    compareModalClose: HTMLElement | null;
    compareModalBody: HTMLElement | null;
    quickFilters: HTMLElement | null;
    quickFilterBtns: NodeListOf<HTMLElement>;
    recentHistory: HTMLElement | null;
    historyItems: HTMLElement | null;
    clearHistory: HTMLElement | null;
    contactModal: HTMLElement | null;
    contactModalOverlay: HTMLElement | null;
    contactModalClose: HTMLElement | null;
    contactSellerAvatar: HTMLImageElement | null;
    contactSellerName: HTMLElement | null;
    contactProductName: HTMLElement | null;
    contactMethods: HTMLElement | null;
}

export class ProductsPage {
    private products: ProductListItem[] = [];
    private filteredProducts: ProductListItem[] = [];
    private categories: Category[] = [];
    private currentPage = 1;
    private pageSize = 12;
    private isLoading = false;
    private hasMore = true;
    private currentView: ViewMode = 'grid';

    private elements: ProductsPageElements;
    private filterManager!: FilterManager;
    private compareManager!: CompareManager;
    private historyManager!: BrowseHistoryManager;
    private renderer!: ProductCardRenderer;
    private detailModal!: ProductDetailModal;

    constructor() {
        this.elements = {} as ProductsPageElements;
        this.init();
    }

    private async init(): Promise<void> {
        this.cacheElements();
        this.initManagers();
        this.bindEvents();
        header.init();
        header.setActiveNav('products');
        this.filterManager.restoreFromUrl();
        await Promise.all([this.loadCategories(), this.loadProducts(false)]);
        this.initInfiniteScroll();
        this.historyManager.render();
    }

    private initManagers(): void {
        this.filterManager = new FilterManager({
            searchInput: this.elements.searchInput,
            searchClear: this.elements.searchClear,
            filterBadge: this.elements.filterBadge,
            filterSidebar: this.elements.filterSidebar,
            categoryFilter: this.elements.categoryFilter,
            priceMin: this.elements.priceMin,
            priceMax: this.elements.priceMax,
            conditionFilter: this.elements.conditionFilter,
            sortFilter: this.elements.sortFilter,
            activeFilters: this.elements.activeFilters,
            activeFilterTags: this.elements.activeFilterTags,
            presetBtns: this.elements.presetBtns,
            quickFilterBtns: this.elements.quickFilterBtns,
        }, this.categories, () => this.onFilterApply(), () => this.onFilterReset());

        this.compareManager = new CompareManager({
            compareBar: this.elements.compareBar,
            compareCount: this.elements.compareCount,
            compareItems: this.elements.compareItems,
            compareModal: this.elements.compareModal,
            compareModalOverlay: this.elements.compareModalOverlay,
            compareModalClose: this.elements.compareModalClose,
            compareModalBody: this.elements.compareModalBody,
            clearCompare: this.elements.clearCompare,
            startCompare: this.elements.startCompare,
        }, () => this.updateProductCardCompareState());

        this.historyManager = new BrowseHistoryManager({
            recentHistory: this.elements.recentHistory,
            historyItems: this.elements.historyItems,
            clearHistory: this.elements.clearHistory,
        }, (product) => { if (product) {this.detailModal.open(product);} });

        this.detailModal = new ProductDetailModal({
            productDetailModal: this.elements.productDetailModal,
            modalOverlay: this.elements.modalOverlay,
            modalClose: this.elements.modalClose,
            modalMainImage: this.elements.modalMainImage,
            galleryPrev: this.elements.galleryPrev,
            galleryNext: this.elements.galleryNext,
            galleryCounter: this.elements.galleryCounter,
            galleryThumbnails: this.elements.galleryThumbnails,
            modalCondition: this.elements.modalCondition,
            modalDiscount: this.elements.modalDiscount,
            modalTitle: this.elements.modalTitle,
            modalPrice: this.elements.modalPrice,
            modalOriginalPrice: this.elements.modalOriginalPrice,
            modalLocation: this.elements.modalLocation,
            modalDate: this.elements.modalDate,
            modalViews: this.elements.modalViews,
            modalDescription: this.elements.modalDescription,
            modalSellerAvatar: this.elements.modalSellerAvatar,
            modalSellerName: this.elements.modalSellerName,
            modalSellerStats: this.elements.modalSellerStats,
            modalContact: this.elements.modalContact,
            modalFavorite: this.elements.modalFavorite,
            modalShare: this.elements.modalShare,
            modalPriceTrend: this.elements.modalPriceTrend,
            priceTrendChart: this.elements.priceTrendChart,
            trendHighPrice: this.elements.trendHighPrice,
            trendLowPrice: this.elements.trendLowPrice,
            trendAvgPrice: this.elements.trendAvgPrice,
            modalSimilar: this.elements.modalSimilar,
            similarProducts: this.elements.similarProducts,
            shareModal: this.elements.shareModal,
            shareModalOverlay: this.elements.shareModalOverlay,
            shareModalClose: this.elements.shareModalClose,
            shareLinkInput: this.elements.shareLinkInput,
            copyLinkBtn: this.elements.copyLinkBtn,
            shareOptions: this.elements.shareOptions,
            contactModal: this.elements.contactModal,
            contactModalOverlay: this.elements.contactModalOverlay,
            contactModalClose: this.elements.contactModalClose,
            contactSellerAvatar: this.elements.contactSellerAvatar,
            contactSellerName: this.elements.contactSellerName,
            contactProductName: this.elements.contactProductName,
            contactMethods: this.elements.contactMethods,
            productsGrid: this.elements.productsGrid,
        });

        this.renderer = new ProductCardRenderer({
            onCardClick: (p) => this.detailModal.open(p),
            onCompareToggle: (p) => this.compareManager.toggle(p),
            onFavoriteToggle: (id, btn) => this.toggleProductFavorite(id, btn),
            isCompareSelected: (id) => this.compareManager.isSelected(id),
        });
    }

    private cacheElements(): void {
        const el = this.elements;
        el.searchInput = document.getElementById('searchInput') as HTMLInputElement | null;
        el.searchClear = document.getElementById('searchClear');
        el.searchBtn = document.getElementById('searchBtn');
        el.filterToggle = document.getElementById('filterToggle');
        el.filterBadge = document.getElementById('filterBadge');
        el.filterSidebar = document.getElementById('filterSidebar');
        el.filterClose = document.getElementById('filterClose');
        el.categoryFilter = document.getElementById('categoryFilter');
        el.priceMin = document.getElementById('priceMin') as HTMLInputElement | null;
        el.priceMax = document.getElementById('priceMax') as HTMLInputElement | null;
        el.conditionFilter = document.getElementById('conditionFilter');
        el.sortFilter = document.getElementById('sortFilter');
        el.resetFilter = document.getElementById('resetFilter');
        el.applyFilter = document.getElementById('applyFilter');
        el.resultsCount = document.getElementById('resultsCount');
        el.activeFilters = document.getElementById('activeFilters');
        el.activeFilterTags = document.getElementById('activeFilterTags');
        el.clearAllFilters = document.getElementById('clearAllFilters');
        el.productsGrid = document.getElementById('productsGrid');
        el.loadingMore = document.getElementById('loadingMore');
        el.noResults = document.getElementById('noResults');
        el.resetSearch = document.getElementById('resetSearch');
        el.loadMoreTrigger = document.getElementById('loadMoreTrigger');
        el.productDetailModal = document.getElementById('productDetailModal');
        el.modalOverlay = document.getElementById('modalOverlay');
        el.modalClose = document.getElementById('modalClose');
        el.modalMainImage = document.getElementById('modalMainImage') as HTMLImageElement | null;
        el.galleryPrev = document.getElementById('galleryPrev');
        el.galleryNext = document.getElementById('galleryNext');
        el.galleryCounter = document.getElementById('galleryCounter');
        el.galleryThumbnails = document.getElementById('galleryThumbnails');
        el.modalCondition = document.getElementById('modalCondition');
        el.modalDiscount = document.getElementById('modalDiscount');
        el.modalTitle = document.getElementById('modalTitle');
        el.modalPrice = document.getElementById('modalPrice');
        el.modalOriginalPrice = document.getElementById('modalOriginalPrice');
        el.modalLocation = document.getElementById('modalLocation');
        el.modalDate = document.getElementById('modalDate');
        el.modalViews = document.getElementById('modalViews');
        el.modalDescription = document.getElementById('modalDescription');
        el.modalSellerAvatar = document.getElementById('modalSellerAvatar') as HTMLImageElement | null;
        el.modalSellerName = document.getElementById('modalSellerName');
        el.modalSellerStats = document.getElementById('modalSellerStats');
        el.modalContact = document.getElementById('modalContact');
        el.modalFavorite = document.getElementById('modalFavorite');
        el.modalShare = document.getElementById('modalShare');
        el.modalPriceTrend = document.getElementById('modalPriceTrend');
        el.priceTrendChart = document.getElementById('priceTrendChart');
        el.trendHighPrice = document.getElementById('trendHighPrice');
        el.trendLowPrice = document.getElementById('trendLowPrice');
        el.trendAvgPrice = document.getElementById('trendAvgPrice');
        el.modalSimilar = document.getElementById('modalSimilar');
        el.similarProducts = document.getElementById('similarProducts');
        el.userMenu = document.getElementById('userMenu');
        el.userAvatarBtn = document.getElementById('userAvatarBtn');
        el.userAvatarImg = document.getElementById('userAvatarImg') as HTMLImageElement | null;
        el.userName = document.getElementById('userName');
        el.userDropdown = document.getElementById('userDropdown');
        el.logoutBtn = document.getElementById('logoutBtn');
        el.loginBtn = document.getElementById('loginBtn');
        el.shareModal = document.getElementById('shareModal');
        el.shareModalOverlay = document.getElementById('shareModalOverlay');
        el.shareModalClose = document.getElementById('shareModalClose');
        el.shareLinkInput = document.getElementById('shareLinkInput') as HTMLInputElement | null;
        el.copyLinkBtn = document.getElementById('copyLinkBtn');
        el.compareBar = document.getElementById('compareBar');
        el.compareCount = document.getElementById('compareCount');
        el.compareItems = document.getElementById('compareItems');
        el.clearCompare = document.getElementById('clearCompare');
        el.startCompare = document.getElementById('startCompare');
        el.compareModal = document.getElementById('compareModal');
        el.compareModalOverlay = document.getElementById('compareModalOverlay');
        el.compareModalClose = document.getElementById('compareModalClose');
        el.compareModalBody = document.getElementById('compareModalBody');
        el.quickFilters = document.getElementById('quickFilters');
        el.recentHistory = document.getElementById('recentHistory');
        el.historyItems = document.getElementById('historyItems');
        el.clearHistory = document.getElementById('clearHistory');
        el.contactModal = document.getElementById('contactModal');
        el.contactModalOverlay = document.getElementById('contactModalOverlay');
        el.contactModalClose = document.getElementById('contactModalClose');
        el.contactSellerAvatar = document.getElementById('contactSellerAvatar') as HTMLImageElement | null;
        el.contactSellerName = document.getElementById('contactSellerName');
        el.contactProductName = document.getElementById('contactProductName');
        el.contactMethods = document.getElementById('contactMethods');
        el.viewBtns = document.querySelectorAll<HTMLElement>('.view-btn');
        el.presetBtns = document.querySelectorAll<HTMLElement>('.preset-btn');
        el.shareOptions = document.querySelectorAll<HTMLElement>('.share-option');
        el.quickFilterBtns = document.querySelectorAll<HTMLElement>('.quick-filter-btn');
    }

    private bindEvents(): void {
        const el = this.elements;

        // Search
        eventManager.on(el.searchInput, 'input', debounce(() => {
            if (el.searchInput) {
                this.filterManager.keyword = el.searchInput.value.trim();
            }
            if (el.searchClear) {
                el.searchClear.classList.toggle('visible', this.filterManager.keyword.length > 0);
            }
        }, 300));

        eventManager.on(el.searchInput, 'keydown', ((e: Event) => {
            if ((e as KeyboardEvent).key === 'Enter') {this.handleSearch();}
        }) as EventListener);

        eventManager.on(el.searchClear, 'click', () => {
            if (el.searchInput) { el.searchInput.value = ''; }
            this.filterManager.keyword = '';
            if (el.searchClear) { el.searchClear.classList.remove('visible'); }
            this.handleSearch();
        });
        eventManager.on(el.searchBtn, 'click', () => this.handleSearch());

        // Filters
        eventManager.on(el.filterToggle, 'click', () => this.filterManager.toggleSidebar());
        eventManager.on(el.filterClose, 'click', () => this.filterManager.closeSidebar());
        eventManager.on(el.modalOverlay, 'click', () => this.detailModal.close());
        eventManager.on(el.modalClose, 'click', () => this.detailModal.close());
        eventManager.on(el.resetFilter, 'click', () => this.filterManager.reset());
        eventManager.on(el.applyFilter, 'click', () => this.filterManager.apply());
        eventManager.on(el.clearAllFilters, 'click', () => { this.filterManager.clearAll(); this.resetAndReload(); });
        eventManager.on(el.resetSearch, 'click', () => this.resetAndReload());

        // Gallery
        eventManager.on(el.galleryPrev, 'click', () => this.detailModal.navigateGallery(-1));
        eventManager.on(el.galleryNext, 'click', () => this.detailModal.navigateGallery(1));

        // Modal actions
        eventManager.on(el.modalContact, 'click', () => this.detailModal.openContactModal());
        eventManager.on(el.modalFavorite, 'click', () => this.detailModal.toggleFavorite());
        eventManager.on(el.modalShare, 'click', () => this.detailModal.openShareModal());

        if (el.modalSellerAvatar) {
            el.modalSellerAvatar.style.cursor = 'pointer';
            eventManager.on(el.modalSellerAvatar, 'click', () => {
                const product = this.detailModal.getCurrentProduct();
                if (product?.sellerId) {
                    navigation.go('profile', { query: { userId: product.sellerId } });
                } else {
                    toast.info('无法查看卖家信息');
                }
            });
        }

        // Share modal
        eventManager.on(el.shareModalOverlay, 'click', () => this.detailModal.closeShareModal());
        eventManager.on(el.shareModalClose, 'click', () => this.detailModal.closeShareModal());
        eventManager.on(el.copyLinkBtn, 'click', () => this.detailModal.copyShareLink());
        el.shareOptions.forEach(btn => {
            eventManager.on(btn, 'click', () => this.detailModal.handleShare(btn.dataset.platform || ''));
        });

        // Contact modal
        eventManager.on(el.contactModalOverlay, 'click', () => this.detailModal.closeContactModal());
        eventManager.on(el.contactModalClose, 'click', () => this.detailModal.closeContactModal());

        // Compare
        eventManager.on(el.clearCompare, 'click', () => this.compareManager.clear());
        eventManager.on(el.startCompare, 'click', () => this.compareManager.openModal());
        eventManager.on(el.compareModalOverlay, 'click', () => this.compareManager.closeModal());
        eventManager.on(el.compareModalClose, 'click', () => this.compareManager.closeModal());

        // Quick filters
        el.quickFilterBtns.forEach(btn => {
            eventManager.on(btn, 'click', () => {
                el.quickFilterBtns.forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
                const quickFilter = btn.dataset.filter as QuickFilterOption;
                this.filterManager.filters.quickFilter = quickFilter;
                this.resetAndReload();
            });
        });

        eventManager.on(el.clearHistory, 'click', () => this.historyManager.clear());

        // View toggle
        el.viewBtns.forEach(btn => {
            eventManager.on(btn, 'click', () => {
                el.viewBtns.forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
                this.currentView = btn.dataset.view as ViewMode;
                if (el.productsGrid) {
                    el.productsGrid.classList.toggle('list-view', this.currentView === 'list');
                }
            });
        });

        // Price presets
        el.presetBtns.forEach(btn => {
            eventManager.on(btn, 'click', () => {
                el.presetBtns.forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
                if (el.priceMin) { el.priceMin.value = btn.dataset.min || ''; }
                if (el.priceMax) { el.priceMax.value = btn.dataset.max || ''; }
            });
        });
        eventManager.on(el.priceMin, 'input', () => el.presetBtns.forEach(b => b.classList.remove('active')));
        eventManager.on(el.priceMax, 'input', () => el.presetBtns.forEach(b => b.classList.remove('active')));

        // Keyboard
        eventManager.on(document, 'keydown', ((e: Event) => {
            const keyEvent = e as KeyboardEvent;
            if (keyEvent.key === 'Escape') {
                if (this.detailModal.isOpen()) {this.detailModal.close();}
                else if (el.filterSidebar?.classList.contains('open')) {this.filterManager.closeSidebar();}
                else if (el.shareModal?.classList.contains('open')) {this.detailModal.closeShareModal();}
                else if (el.compareModal?.classList.contains('open')) {this.compareManager.closeModal();}
            }
            if (this.detailModal.isOpen()) {
                if (keyEvent.key === 'ArrowLeft') { keyEvent.preventDefault(); this.detailModal.navigateGallery(-1); }
                else if (keyEvent.key === 'ArrowRight') { keyEvent.preventDefault(); this.detailModal.navigateGallery(1); }
            }
        }) as EventListener);
    }

    private async loadCategories(): Promise<void> {
        try {
            const response = await api.product.getCategoryTree();
            if (response) {
                const data = (response as { data?: Category[] }).data ?? (response as unknown as Category[]);
                this.categories = Array.isArray(data) ? data : [];
            }
        } catch {
            this.categories = this.getDefaultCategories();
        }
        this.filterManager.renderCategoryFilters();
    }

    private getDefaultCategories(): Category[] {
        return [
            { id: 1, name: '图书教材', icon: '📚', parentId: null, productCount: 0 },
            { id: 2, name: '电子产品', icon: '💻', parentId: null, productCount: 0 },
            { id: 3, name: '服装鞋包', icon: '👔', parentId: null, productCount: 0 },
            { id: 4, name: '生活用品', icon: '🏠', parentId: null, productCount: 0 },
            { id: 5, name: '运动户外', icon: '⚽', parentId: null, productCount: 0 },
            { id: 6, name: '美妆护肤', icon: '💄', parentId: null, productCount: 0 },
            { id: 7, name: '交通工具', icon: '🚲', parentId: null, productCount: 0 },
            { id: 8, name: '其他', icon: '📦', parentId: null, productCount: 0 },
        ];
    }

    private async loadProducts(append: boolean): Promise<void> {
        if (this.isLoading) {return;}
        this.isLoading = true;

        if (!append) {
            if (this.elements.productsGrid) {
                this.renderer.renderSkeletonGrid(this.elements.productsGrid);
            }
        } else {
            if (this.elements.loadingMore) {
                this.elements.loadingMore.style.display = 'flex';
            }
        }

        try {
            const params = this.filterManager.buildQueryParams(this.currentPage, this.pageSize) as unknown as Record<string, unknown>;
            const response = await api.product.getProducts(params);
            const products = (response.data?.records || []) as unknown as ProductListItem[];

            if (append) {
                this.products = [...this.products, ...products];
            } else {
                this.products = products;
            }
            this.hasMore = products.length === this.pageSize;
            this.filteredProducts = this.filterManager.applyClientFilters(this.products);
            this.renderProducts(append);
            this.updateResultsCount();
            this.historyManager.setProductSource(this.products);
            this.detailModal.setProductSource(this.products);
        } catch {
            if (!append) {this.loadMockProducts();}
        } finally {
            this.isLoading = false;
            if (this.elements.loadingMore) {
                this.elements.loadingMore.style.display = 'none';
            }
        }
    }

    private loadMockProducts(): void {
        this.products = generateMockProducts(24);
        this.hasMore = false;
        this.filteredProducts = this.filterManager.applyClientFilters(this.products);
        this.renderProducts(false);
        this.updateResultsCount();
    }

    private renderProducts(append: boolean): void {
        const grid = this.elements.productsGrid;
        if (!grid) {return;}

        if (this.filteredProducts.length === 0 && !append) {
            grid.innerHTML = '';
            if (this.elements.noResults) { this.elements.noResults.style.display = 'flex'; }
            return;
        }
        if (this.elements.noResults) { this.elements.noResults.style.display = 'none'; }

        if (!append) {grid.innerHTML = '';}
        const fragment = document.createDocumentFragment();
        const startIdx = append ? grid.children.length : 0;
        this.filteredProducts.slice(startIdx).forEach((product, i) => {
            fragment.appendChild(this.renderer.create(product, i));
        });
        grid.appendChild(fragment);
    }

    private resetAndReload(): void {
        this.currentPage = 1;
        this.products = [];
        this.loadProducts(false);
    }

    private onFilterApply(): void {
        this.resetAndReload();
    }

    private onFilterReset(): void {
        this.filterManager.updateActiveFilterTags();
    }

    private handleSearch(): void {
        this.filterManager.keyword = this.elements.searchInput?.value.trim() || '';
        this.filterManager.saveToUrl();
        this.resetAndReload();
    }

    private initInfiniteScroll(): void {
        const observer = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting && this.hasMore && !this.isLoading) {
                    this.currentPage++;
                    this.loadProducts(true);
                }
            });
        }, { threshold: 0.1 });
        if (this.elements.loadMoreTrigger) {observer.observe(this.elements.loadMoreTrigger);}
    }

    private updateResultsCount(): void {
        if (this.elements.resultsCount) {
            this.elements.resultsCount.textContent = String(this.filteredProducts.length);
        }
    }

    private updateProductCardCompareState(): void {
        if (!this.elements.productsGrid) {return;}
        this.elements.productsGrid.querySelectorAll<HTMLElement>('.product-card').forEach(card => {
            const productId = parseInt(card.dataset.productId || '0');
            card.classList.toggle('compare-selected', this.compareManager.isSelected(productId));
        });
    }

    private toggleProductFavorite(productId: number, btn: HTMLElement): void {
        const isFavorited = favoriteManager.toggle(productId);
        btn.classList.toggle('favorited', isFavorited);
        btn.innerHTML = `<svg viewBox="0 0 24 24" fill="${isFavorited ? 'currentColor' : 'none'}" stroke="currentColor" stroke-width="2">` +
            '<path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>';
        toast.show(isFavorited ? '已添加到收藏' : '已取消收藏', isFavorited ? 'success' : 'info');
    }

    public destroy(): void {
        // Clean up event listeners
    }
}

let productsPageInstance: ProductsPage | null = null;
document.addEventListener('DOMContentLoaded', () => {
    productsPageInstance = new ProductsPage();
    window.addEventListener('beforeunload', () => {
        if (productsPageInstance) { productsPageInstance.destroy(); productsPageInstance = null; }
    });
});
