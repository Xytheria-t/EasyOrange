/**
 * @fileoverview 分类选择器模块
 * @version 2.0.0
 */

import type { Category } from '@/types';

/** 分类选择器选项 */
export interface CategorySelectorOptions {
    onSelect?: (category: Category) => void;
}

/** 分类选择器元素接口 */
export interface CategorySelectorElements {
    level1: HTMLElement | null;
    level2: HTMLElement | null;
    level3: HTMLElement | null;
    level1Options: HTMLElement | null;
    level2Options: HTMLElement | null;
    level3Options: HTMLElement | null;
    selectedCategoryId: HTMLInputElement | null;
    categoryError: HTMLElement | null;
    selectedCategoryDisplay: HTMLElement | null;
}

/** API 接口 */
interface ApiInterface {
    product: {
        getCategoryTree: () => Promise<Category[] | { data: Category[] }>;
    };
}

/**
 * 分类选择器类
 * 负责三级分类的选择和展示
 */
export class CategorySelector {
    private categories: Category[];
    private selectedCategory: Category | null;
    private onSelect: (category: Category) => void;
    private elements: CategorySelectorElements;

    constructor(_container: HTMLElement | null, options: CategorySelectorOptions = {}) {
        this.categories = [];
        this.selectedCategory = null;
        this.onSelect = options.onSelect || (() => {});
        this.elements = {
            level1: null,
            level2: null,
            level3: null,
            level1Options: null,
            level2Options: null,
            level3Options: null,
            selectedCategoryId: null,
            categoryError: null,
            selectedCategoryDisplay: null
        };
        this.initElements();
    }

    /**
     * 初始化元素引用
     */
    private initElements(): void {
        this.elements = {
            level1: document.getElementById('level1'),
            level2: document.getElementById('level2'),
            level3: document.getElementById('level3'),
            level1Options: document.getElementById('level1Options'),
            level2Options: document.getElementById('level2Options'),
            level3Options: document.getElementById('level3Options'),
            selectedCategoryId: document.getElementById('selectedCategoryId') as HTMLInputElement | null,
            categoryError: document.getElementById('categoryError'),
            selectedCategoryDisplay: document.getElementById('selectedCategoryDisplay')
        };
    }

    /**
     * 加载分类数据
     * @param api - API 接口
     * @param retryCount - 重试次数
     * @returns 是否加载成功
     */
    async loadCategories(api: ApiInterface, retryCount = 0): Promise<boolean> {
        const maxRetries = 3;
        const retryDelay = 1000;

        try {
            this.showLoading(true);

            const response = await api.product.getCategoryTree();

            if (response) {
                this.categories = (response as { data?: Category[] }).data || response as Category[];
                this.renderLevel(1, this.categories);
                this.showLoading(false);
                return true;
            } else {
                throw new Error('分类数据格式错误');
            }
        } catch {
            if (retryCount < maxRetries) {
                await new Promise(resolve => setTimeout(resolve, retryDelay));
                return this.loadCategories(api, retryCount + 1);
            }

            this.showLoading(false);
            this.showError('加载分类失败，请检查网络连接后刷新页面');
            return false;
        }
    }

    /**
     * 显示/隐藏加载状态
     * @param show - 是否显示
     */
    private showLoading(show: boolean): void {
        const level1Options = this.elements.level1Options;
        if (!level1Options) {return;}

        if (show) {
            level1Options.innerHTML = '';
            const loadingDiv = document.createElement('div');
            loadingDiv.className = 'cascade-loading';

            const spinnerDiv = document.createElement('div');
            spinnerDiv.className = 'loading-spinner';

            const span = document.createElement('span');
            span.textContent = '正在加载分类...';

            loadingDiv.appendChild(spinnerDiv);
            loadingDiv.appendChild(span);
            level1Options.appendChild(loadingDiv);
        }
    }

    /**
     * 显示错误信息
     * @param message - 错误信息
     */
    private showError(message: string): void {
        const level1Options = this.elements.level1Options;
        if (!level1Options) {return;}

        level1Options.innerHTML = '';
        const errorDiv = document.createElement('div');
        errorDiv.className = 'cascade-error';

        const iconSpan = document.createElement('span');
        iconSpan.className = 'error-icon';
        iconSpan.textContent = '⚠️';

        const messageSpan = document.createElement('span');
        messageSpan.textContent = message;

        const retryBtn = document.createElement('button');
        retryBtn.className = 'retry-btn';
        retryBtn.textContent = '刷新页面';
        retryBtn.onclick = () => location.reload();

        errorDiv.appendChild(iconSpan);
        errorDiv.appendChild(messageSpan);
        errorDiv.appendChild(retryBtn);
        level1Options.appendChild(errorDiv);
    }

    /**
     * 渲染分类级别
     * @param level - 级别（1-3）
     * @param categories - 分类数据
     */
    private renderLevel(level: 1 | 2 | 3, categories: Category[]): void {
        const optionsContainer = this.elements[`level${level}Options` as keyof CategorySelectorElements] as HTMLElement | null;
        if (!optionsContainer) {return;}

        optionsContainer.innerHTML = '';

        categories.forEach(category => {
            const option = document.createElement('div');
            option.className = 'cascade-option';
            option.dataset.id = String(category.id);
            option.dataset.level = String(level);

            const iconSpan = document.createElement('span');
            iconSpan.className = 'cascade-icon';
            iconSpan.textContent = category.icon || '📁';

            const nameSpan = document.createElement('span');
            nameSpan.className = 'cascade-name';
            nameSpan.textContent = category.name;

            option.appendChild(iconSpan);
            option.appendChild(nameSpan);

            option.addEventListener('click', () => this.selectCategory(level, category));

            optionsContainer.appendChild(option);
        });

        const levelElement = this.elements[`level${level}` as keyof CategorySelectorElements] as HTMLElement | null;
        if (levelElement) {
            levelElement.classList.remove('hidden');
        }
    }

    /**
     * 选择分类
     * @param level - 级别
     * @param category - 分类数据
     */
    private selectCategory(level: 1 | 2 | 3, category: Category): void {
        const optionsContainer = this.elements[`level${level}Options` as keyof CategorySelectorElements] as HTMLElement | null;
        const options = optionsContainer?.querySelectorAll('.cascade-option');

        options?.forEach(opt => opt.classList.remove('selected'));
        const selectedOption = optionsContainer?.querySelector(`[data-id="${category.id}"]`);
        if (selectedOption) {
            selectedOption.classList.add('selected');
        }

        // 清除下级分类
        for (let i = level + 1; i <= 3; i++) {
            const levelElement = this.elements[`level${i}` as keyof CategorySelectorElements] as HTMLElement | null;
            if (levelElement) {
                levelElement.classList.add('hidden');
            }
            const optionsEl = this.elements[`level${i}Options` as keyof CategorySelectorElements] as HTMLElement | null;
            if (optionsEl) {
                optionsEl.innerHTML = '';
            }
        }

        if (category.children && category.children.length > 0) {
            this.renderLevel((level + 1) as 2 | 3, category.children);
            this.selectedCategory = null;
            if (this.elements.selectedCategoryId) {
                this.elements.selectedCategoryId.value = '';
            }
        } else {
            this.selectedCategory = category;
            if (this.elements.selectedCategoryId) {
                this.elements.selectedCategoryId.value = String(category.id);
            }
            this.updateSelectedCategoryDisplay(category);
            this.onSelect(category);
        }
    }

    /**
     * 更新选中分类显示
     * @param category - 分类数据
     */
    private updateSelectedCategoryDisplay(category: Category): void {
        const categoryPath = this.getCategoryPath(category.id);
        const displayElement = this.elements.selectedCategoryDisplay;
        if (displayElement && categoryPath) {
            const span = displayElement.querySelector('span');
            if (span) {
                span.textContent = categoryPath;
            }
            displayElement.classList.add('active');
        }
    }

    /**
     * 获取分类路径
     * @param categoryId - 分类ID
     * @param categories - 分类数据
     * @param path - 当前路径
     * @returns 分类路径字符串
     */
    private getCategoryPath(
        categoryId: string,
        categories: Category[] = this.categories,
        path = ''
    ): string | null {
        for (const category of categories) {
            const currentPath = path ? `${path} > ${category.name}` : category.name;
            if (category.id === categoryId) {
                return currentPath;
            }
            if (category.children && category.children.length > 0) {
                const found = this.getCategoryPath(categoryId, category.children, currentPath);
                if (found) {return found;}
            }
        }
        return null;
    }

    /**
     * 获取选中的分类
     * @returns 选中的分类，未选中返回null
     */
    getSelected(): Category | null {
        return this.selectedCategory;
    }

    /**
     * 验证分类选择
     * @returns 验证是否通过
     */
    validate(): boolean {
        if (!this.selectedCategory) {
            const errorElement = this.elements.categoryError;
            if (errorElement) {
                errorElement.textContent = '请选择商品分类';
                errorElement.style.display = 'block';
            }
            return false;
        }
        const errorElement = this.elements.categoryError;
        if (errorElement) {
            errorElement.textContent = '';
            errorElement.style.display = 'none';
        }
        return true;
    }
}

export default CategorySelector;
