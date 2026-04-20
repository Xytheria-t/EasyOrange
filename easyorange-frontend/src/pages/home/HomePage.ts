/**
 * @fileoverview 首页主模块
 * @description 整合认证、搜索、商品展示和动画模块
 */

import { productApi } from '../../api/index.js';
import { dom, toast, createEmptyState, appendChildren } from '../../utils/index.js';
import ProductCard from '../../components/ProductCard.js';
import header from '../../components/Header.js';
import type { Product } from '../../types/index.js';
import authManager from './auth.js';
import searchManager from './search.js';
import animationManager from './animations.js';

/** 首页 DOM 元素 */
interface HomePageElements {
    categoryCards: HTMLElement[];
    mobileMenuBtn: HTMLElement | null;
}

/**
 * 首页管理器
 */
export class HomePage {
    private elements: HomePageElements;
    private initialized: boolean;
    private currentCategory: string | null;

    constructor() {
        this.elements = {
            categoryCards: [],
            mobileMenuBtn: null
        };
        this.currentCategory = null;
        this.initialized = false;
    }

    /**
     * 初始化首页
     */
    async init(): Promise<void> {
        if (this.initialized) {return;}

        this.cacheElements();
        this.bindEvents();
        
        // 初始化各个子模块
        authManager.init();
        searchManager.init();
        animationManager.init();
        
        // 初始化 Header（会动态注入 #loginBtn）
        header.init();
        header.setActiveNav('home');
        
        // Header 初始化后再绑定登录按钮事件
        authManager.bindLoginButtons();

        // 加载数据
        await this.loadCategories();
        await this.fetchProducts();

        this.initialized = true;
    }

    /**
     * 缓存 DOM 元素
     */
    private cacheElements(): void {
        this.elements = {
            categoryCards: dom.getAll('.category-card'),
            mobileMenuBtn: dom.get('.mobile-menu-btn')
        };
    }

    /**
     * 绑定事件监听器
     */
    private bindEvents(): void {
        const { categoryCards, mobileMenuBtn } = this.elements;

        // 分类卡片
        categoryCards.forEach(card => {
            card.addEventListener('click', (e) => this.handleCategoryClick(e));
        });

        // 移动端菜单
        if (mobileMenuBtn) {
            mobileMenuBtn.addEventListener('click', () => this.toggleMobileMenu());
        }

        // 键盘 ESC 关闭模态框
        window.addEventListener('keydown', (e) => {
            const descriptionModal = document.getElementById('descriptionModal');
            if (descriptionModal?.classList.contains('modal-active') && e.key === 'Escape') {
                this.hideDescriptionModal();
            }
        });
    }

    /**
     * 处理分类点击
     */
    private handleCategoryClick(e: Event): void {
        const categoryCard = e.currentTarget as HTMLElement;
        const categoryName = categoryCard.querySelector('.card-title')?.textContent ||
            categoryCard.querySelector('h4')?.textContent || '';

        if (this.currentCategory === categoryName) {
            // 取消选中
            toast.info('显示所有推荐商品...');
            this.elements.categoryCards.forEach(card => card.classList.remove('active'));
            this.currentCategory = null;
            this.fetchProducts();
        } else {
            // 选中分类
            toast.info(`正在加载${categoryName}商品...`);
            this.elements.categoryCards.forEach(card => card.classList.remove('active'));
            categoryCard.classList.add('active');
            this.currentCategory = categoryName;
            this.fetchProductsByCategory(categoryName);
        }
    }

    /**
     * 获取商品列表
     */
    private async fetchProducts(params?: { sort?: string }): Promise<void> {
        const queryParams: Record<string, unknown> = {
            page: 1,
            size: 20,
            ...params
        };
        await this.loadProducts(
            () => productApi.getProducts(queryParams),
            '加载商品失败，请稍后重试'
        );
    }

    private async fetchProductsByCategory(category: string): Promise<void> {
        await this.loadProducts(
            () => productApi.getProductsByCategory(category),
            '加载分类商品失败，请稍后重试'
        );
    }

    private async loadProducts(
        loader: () => Promise<{ data?: { records?: Product[] } }>,
        errorMessage: string
    ): Promise<void> {
        try {
            const response = await loader();
            const products = response.data?.records || [];
            this.renderProducts(products);
        } catch (error) {
            toast.error(errorMessage);
        }
    }

    /**
     * 加载分类列表
     * 注意：当前分类在 HTML 中硬编码，支持通过 API 动态加载
     * 如需动态加载，调用 categoryApi.getCategoryTree()
     */
    private async loadCategories(): Promise<void> {
        // 分类在 HTML 中通过 Thymeleaf 渲染，客户端无需加载
        // 如需客户端动态加载，可实现：
        // const response = await categoryApi.getCategoryTree();
        // this.renderCategories(response.data);
    }

    /**
     * 渲染商品列表
     */
    private renderProducts(products: Product[]): void {
        const productsGrid = document.getElementById('productsGrid');
        
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
        const cards = products.map(product => ProductCard.create(product));
        productsGrid.innerHTML = '';
        appendChildren(productsGrid, cards);
    }

    /**
     * 切换移动端菜单
     */
    private toggleMobileMenu(): void {
        const nav = document.querySelector('.navbar-nav');
        if (nav) {
            nav.classList.toggle('mobile-open');
            this.elements.mobileMenuBtn?.classList.toggle('active');
        }
    }

    /**
     * 隐藏商品描述模态框
     */
    private hideDescriptionModal(): void {
        const modal = document.getElementById('descriptionModal');
        if (modal) {
            modal.classList.remove('modal-active');
        }
    }

    /**
     * 销毁模块，清理资源
     * 通过调用子模块的 destroy 方法来清理事件监听器
     */
    destroy(): void {
        this.initialized = false;
        this.currentCategory = null;
        this.elements = {
            categoryCards: [],
            mobileMenuBtn: null
        };

        authManager.destroy();
        searchManager.destroy();
        animationManager.destroy();
    }
}

// 导出单例
export const homePage = new HomePage();
export default homePage;
