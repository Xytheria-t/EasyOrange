/**
 * @fileoverview 首页搜索模块
 * @description 处理商品搜索、过滤和排序逻辑
 */

import { productApi } from '../../api/index.js';
import { toast, dom, createEmptyState, appendChildren } from '../../utils/index.js';
import type { Product, PageParams } from '../../types/index.js';
import ProductCard from '../../components/ProductCard.js';

/** 搜索参数 */
export interface SearchParams {
    keyword?: string;
    categoryId?: number;
    sort?: 'newest' | 'popular' | 'price_asc' | 'price_desc';
    filter?: 'all' | 'new' | 'hot' | 'discount';
}

/** 搜索模块 DOM 元素 */
interface SearchElements {
    searchInput: HTMLInputElement | null;
    searchBtn: HTMLElement | null;
    filterTabs: HTMLElement[];
    tagBtns: HTMLElement[];
    productsGrid: HTMLElement | null;
}

/**
 * 搜索管理器
 */
export class SearchManager {
    private elements: SearchElements;
    private initialized: boolean;

    constructor() {
        this.elements = {
            searchInput: null,
            searchBtn: null,
            filterTabs: [],
            tagBtns: [],
            productsGrid: null
        };
        this.initialized = false;
    }

    /**
     * 初始化搜索模块
     */
    init(): void {
        if (this.initialized) {return;}

        this.cacheElements();
        this.bindEvents();
        this.initialized = true;
    }

    /**
     * 缓存 DOM 元素
     */
    private cacheElements(): void {
        this.elements = {
            searchInput: dom.get('.search-wrapper .search-input') as HTMLInputElement | null,
            searchBtn: dom.get('.search-btn'),
            filterTabs: dom.getAll('.filter-tab'),
            tagBtns: dom.getAll('.tag-btn'),
            productsGrid: dom.get('#productsGrid')
        };
    }

    /**
     * 绑定事件监听器
     */
    private bindEvents(): void {
        const { searchBtn, searchInput, filterTabs, tagBtns } = this.elements;

        if (searchBtn) {
            searchBtn.addEventListener('click', () => this.handleSearch());
        }

        if (searchInput) {
            searchInput.addEventListener('keydown', (e) => {
                if (e.key === 'Enter') {
                    this.handleSearch();
                }
            });
        }

        // 过滤标签
        filterTabs.forEach(tab => {
            tab.addEventListener('click', (e) => this.handleFilterTabClick(e));
        });

        // 标签按钮
        tagBtns.forEach(btn => {
            btn.addEventListener('click', (e) => this.handleTagClick(e));
        });
    }

    /**
     * 处理搜索
     */
    private handleSearch(): void {
        const { searchInput } = this.elements;
        const keyword = searchInput?.value.trim() || '';

        if (keyword) {
            toast.success(`搜索：${keyword}`);
            this.searchProducts(keyword);
        } else {
            toast.error('请输入搜索关键词');
        }
    }

    /**
     * 搜索商品
     */
    private async searchProducts(keyword: string): Promise<void> {
        try {
            const response = await productApi.searchProducts(keyword);
            const products = response.data?.records || [];
            this.renderProducts(products);
        } catch {
            toast.error('搜索失败，请稍后重试');
        }
    }

    /**
     * 处理过滤标签点击
     */
    private handleFilterTabClick(e: Event): void {
        const tab = e.currentTarget as HTMLElement;
        const filter = tab.dataset.filter || '';

        // 更新激活状态
        this.elements.filterTabs.forEach(t => t.classList.remove('active'));
        tab.classList.add('active');

        // 根据过滤条件获取商品
        switch (filter) {
            case 'all':
                this.fetchProducts();
                break;
            case 'new':
                this.fetchProducts({ sort: 'newest' });
                break;
            case 'hot':
                this.fetchProducts({ sort: 'popular' });
                break;
            case 'discount':
                this.fetchProducts({ sort: 'price_asc' });
                break;
            default:
                this.fetchProducts();
        }
    }

    /**
     * 处理标签点击
     */
    private handleTagClick(e: Event): void {
        const tag = e.currentTarget as HTMLElement;
        const keyword = tag.textContent?.trim() || '';

        if (this.elements.searchInput) {
            this.elements.searchInput.value = keyword;
        }
        this.handleSearch();
    }

    /**
     * 获取商品
     */
    private async fetchProducts(params?: SearchParams): Promise<void> {
        try {
            let response;
            if (params?.sort) {
                const queryParams: Record<string, unknown> = {
                    current: 1,
                    size: 20,
                    sort: params.sort
                };
                response = await productApi.getProducts(queryParams as Record<string, unknown> & PageParams);
            } else {
                response = await productApi.getProducts({ current: 1, size: 20 });
            }
            const products = response.data?.records || [];
            this.renderProducts(products);
        } catch {
            toast.error('加载商品失败，请稍后重试');
        }
    }

    /**
     * 渲染商品列表
     */
    private renderProducts(products: Product[]): void {
        const { productsGrid } = this.elements;
        
        if (!productsGrid) {return;}

        if (products.length === 0) {
            // 使用 DOM 工具创建空状态
            const emptyState = createEmptyState('暂无商品', {
                icon: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="10"/>
                    <line x1="12" y1="8" x2="12" y2="12"/>
                    <line x1="12" y1="16" x2="12.01" y2="16"/>
                </svg>`
            });
            productsGrid.innerHTML = '';
            productsGrid.appendChild(emptyState);
            return;
        }

        // 使用 DocumentFragment 批量添加商品卡片
        const cards = products.map(product => this.createProductCard(product));
        productsGrid.innerHTML = '';
        appendChildren(productsGrid, cards);
    }

    /**
     * 创建商品卡片
     */
    private createProductCard(product: Product): HTMLElement {
        return ProductCard.create(product);
    }

    /**
     * 销毁模块，清理资源
     */
    destroy(): void {
        this.initialized = false;
        this.elements = {
            searchInput: null,
            searchBtn: null,
            filterTabs: [],
            tagBtns: [],
            productsGrid: null
        };
    }
}

export default new SearchManager();
