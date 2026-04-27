/**
 * @fileoverview 骨架屏组件 - 提供加载占位效果
 * @version 1.0.0
 */

/**
 * 列表项骨架屏配置选项
 */
export interface ListItemsOptions {
    hasAvatar?: boolean;
    lines?: number;
    hasAction?: boolean;
}

/**
 * 骨架屏创建选项
 */
export interface SkeletonCreateOptions extends ListItemsOptions {
    count?: number;
    rows?: number;
    cols?: number;
    fields?: number;
}

/**
 * 骨架屏类型
 */
export type SkeletonType = 'card' | 'list' | 'text' | 'table' | 'detail' | 'form';

/**
 * 骨架屏生成器
 * @namespace
 */
export const Skeleton = {
    /**
     * 创建商品卡片骨架屏
     * @param count - 数量
     * @returns DocumentFragment
     */
    createProductCards(count = 8): DocumentFragment {
        const fragment = document.createDocumentFragment();
        
        for (let i = 0; i < count; i++) {
            const skeleton = document.createElement('div');
            skeleton.className = 'skeleton-card';
            skeleton.innerHTML = `
                <div class="skeleton-image"></div>
                <div class="skeleton-content">
                    <div class="skeleton-line short"></div>
                    <div class="skeleton-line medium"></div>
                    <div class="skeleton-line"></div>
                </div>
            `;
            fragment.appendChild(skeleton);
        }
        
        return fragment;
    },
    
    /**
     * 创建列表项骨架屏
     * @param count - 数量
     * @param options - 配置选项
     * @returns DocumentFragment
     */
    createListItems(count = 5, options: ListItemsOptions = {}): DocumentFragment {
        const { 
            lines = 2,
            hasAction = true 
        } = options;
        
        const fragment = document.createDocumentFragment();
        
        for (let i = 0; i < count; i++) {
            const item = document.createElement('div');
            item.className = 'skeleton-list-item';

            let html = '<div class="skeleton-content">';
            for (let j = 0; j < lines; j++) {
                html += `<div class="skeleton-line ${j === 0 ? 'short' : ''}"></div>`;
            }
            html += '</div>';
            
            if (hasAction) {
                html += '<div class="skeleton-action"></div>';
            }
            
            item.innerHTML = html;
            fragment.appendChild(item);
        }
        
        return fragment;
    },
    
    /**
     * 创建文本骨架屏
     * @param lines - 行数
     * @returns DocumentFragment
     */
    createText(lines = 3): DocumentFragment {
        const fragment = document.createDocumentFragment();
        const container = document.createElement('div');
        container.className = 'skeleton-text';
        
        for (let i = 0; i < lines; i++) {
            const line = document.createElement('div');
            line.className = `skeleton-line ${i === lines - 1 ? 'short' : ''}`;
            container.appendChild(line);
        }
        
        fragment.appendChild(container);
        return fragment;
    },
    
    /**
     * 创建表格骨架屏
     * @param rows - 行数
     * @param cols - 列数
     * @returns DocumentFragment
     */
    createTable(rows = 5, cols = 4): DocumentFragment {
        const fragment = document.createDocumentFragment();
        const table = document.createElement('div');
        table.className = 'skeleton-table';
        
        // 表头
        let headerHtml = '<div class="skeleton-table-header">';
        for (let j = 0; j < cols; j++) {
            headerHtml += '<div class="skeleton-table-cell"></div>';
        }
        headerHtml += '</div>';
        
        // 表体
        let bodyHtml = '<div class="skeleton-table-body">';
        for (let i = 0; i < rows; i++) {
            bodyHtml += '<div class="skeleton-table-row">';
            for (let j = 0; j < cols; j++) {
                bodyHtml += '<div class="skeleton-table-cell"></div>';
            }
            bodyHtml += '</div>';
        }
        bodyHtml += '</div>';
        
        table.innerHTML = headerHtml + bodyHtml;
        fragment.appendChild(table);
        return fragment;
    },
    
    /**
     * 创建详情页骨架屏
     * @returns DocumentFragment
     */
    createDetail(): DocumentFragment {
        const fragment = document.createDocumentFragment();
        const container = document.createElement('div');
        container.className = 'skeleton-detail';
        
        container.innerHTML = `
            <div class="skeleton-detail-header">
                <div class="skeleton-detail-info">
                    <div class="skeleton-line short"></div>
                    <div class="skeleton-line medium"></div>
                </div>
            </div>
            <div class="skeleton-detail-body">
                <div class="skeleton-image large"></div>
                <div class="skeleton-content">
                    <div class="skeleton-line"></div>
                    <div class="skeleton-line"></div>
                    <div class="skeleton-line short"></div>
                </div>
            </div>
            <div class="skeleton-detail-actions">
                <div class="skeleton-button"></div>
                <div class="skeleton-button"></div>
            </div>
        `;
        
        fragment.appendChild(container);
        return fragment;
    },
    
    /**
     * 创建表单骨架屏
     * @param fields - 字段数
     * @returns DocumentFragment
     */
    createForm(fields = 4): DocumentFragment {
        const fragment = document.createDocumentFragment();
        const form = document.createElement('div');
        form.className = 'skeleton-form';
        
        let html = '';
        for (let i = 0; i < fields; i++) {
            html += `
                <div class="skeleton-form-field">
                    <div class="skeleton-label"></div>
                    <div class="skeleton-input"></div>
                </div>
            `;
        }
        html += '<div class="skeleton-button full"></div>';
        
        form.innerHTML = html;
        fragment.appendChild(form);
        return fragment;
    },
    
    /**
     * 显示骨架屏
     * @param target - 目标元素或选择器
     * @param skeleton - 骨架屏内容
     * @returns 移除骨架屏的函数
     */
    show(target: string | HTMLElement, skeleton: string | DocumentFragment): () => void {
        const element = typeof target === 'string'
            ? document.querySelector<HTMLElement>(target)
            : target;
        
        if (!element) {return () => {};}

        // 保存原始内容
        const originalContent = element.innerHTML;
        const originalPosition = element.style.position;
        
        // 设置相对定位
        element.style.position = 'relative';
        
        // 插入骨架屏
        if (typeof skeleton === 'string') {
            element.innerHTML = skeleton;
        } else {
            element.innerHTML = '';
            element.appendChild(skeleton);
        }
        
        // 添加加载类
        element.classList.add('skeleton-loading');
        
        // 返回移除函数
        return () => {
            element.classList.remove('skeleton-loading');
            element.style.position = originalPosition;
            element.innerHTML = originalContent;
        };
    },
    
    /**
     * 创建脉冲动画骨架屏
     * @param type - 类型
     * @param options - 配置选项
     * @returns HTMLElement
     */
    create(type: SkeletonType = 'card', options: SkeletonCreateOptions = {}): HTMLElement {
        const creators: Record<SkeletonType, () => DocumentFragment> = {
            card: () => this.createProductCards(1),
            list: () => this.createListItems(options.count || 5, options),
            text: () => this.createText(options.lines || 3),
            table: () => this.createTable(options.rows || 5, options.cols || 4),
            detail: () => this.createDetail(),
            form: () => this.createForm(options.fields || 4)
        };
        
        const creator = creators[type] || creators.card;
        const fragment = creator();
        
        // 包装在容器中
        const container = document.createElement('div');
        container.className = 'skeleton-container';
        container.appendChild(fragment);
        
        return container;
    }
};

/**
 * Spinner 尺寸类型
 */
export type SpinnerSize = 'small' | 'medium' | 'large';

/**
 * Spinner 位置类型
 */
export type SpinnerPosition = 'center' | 'top' | 'bottom';

/**
 * Spinner 显示选项
 */
export interface SpinnerOptions {
    size?: SpinnerSize;
    text?: string;
    overlay?: boolean;
    position?: SpinnerPosition;
}

/**
 * 加载指示器
 * @namespace
 */
export const Spinner = {
    /**
     * 创建加载指示器
     * @param size - 尺寸：small, medium, large
     * @returns HTMLElement
     */
    create(size: SpinnerSize = 'medium'): HTMLElement {
        const spinner = document.createElement('div');
        spinner.className = `spinner spinner-${size}`;
        spinner.setAttribute('role', 'status');
        spinner.setAttribute('aria-label', '加载中');
        
        spinner.innerHTML = `
            <svg class="spinner-svg" viewBox="0 0 50 50">
                <circle class="spinner-circle" cx="25" cy="25" r="20" fill="none" stroke-width="4"></circle>
            </svg>
            <span class="spinner-text sr-only">加载中...</span>
        `;
        
        return spinner;
    },
    
    /**
     * 显示加载指示器
     * @param target - 目标元素
     * @param options - 配置选项
     * @returns 隐藏函数
     */
    show(target: string | HTMLElement, options: SpinnerOptions = {}): () => void {
        const element = typeof target === 'string' 
            ? document.querySelector(target) 
            : target;
        
        if (!element) {return () => {};}
        
        const { 
            size = 'medium',
            text = '',
            overlay = true,
            position = 'center'
        } = options;
        
        // 创建容器
        const container = document.createElement('div');
        container.className = `spinner-container spinner-${position}`;
        
        if (overlay) {
            container.classList.add('spinner-overlay');
        }
        
        // 添加指示器
        container.appendChild(this.create(size));
        
        // 添加文本
        if (text) {
            const textEl = document.createElement('div');
            textEl.className = 'spinner-text';
            textEl.textContent = text;
            container.appendChild(textEl);
        }
        
        element.appendChild(container);
        element.classList.add('loading');
        
        return () => {
            container.remove();
            element.classList.remove('loading');
        };
    },
    
    /**
     * 显示全局加载指示器
     * @param text - 加载文本
     * @returns 隐藏函数
     */
    showGlobal(text = '加载中...'): () => void {
        const overlay = document.createElement('div');
        overlay.id = 'global-spinner';
        overlay.className = 'spinner-global-overlay';
        
        const container = document.createElement('div');
        container.className = 'spinner-global-content';
        
        container.appendChild(this.create('large'));
        
        if (text) {
            const textEl = document.createElement('div');
            textEl.className = 'spinner-global-text';
            textEl.textContent = text;
            container.appendChild(textEl);
        }
        
        overlay.appendChild(container);
        document.body.appendChild(overlay);
        document.body.style.overflow = 'hidden';
        
        return () => {
            overlay.remove();
            document.body.style.overflow = '';
        };
    }
};

export default Skeleton;
