/**
 * 模态框组件
 */

/**
 * Modal 实例映射类型
 */
type ModalInstances = Map<string, Modal>;

// 模态框组件
class Modal {
    // 静态实例缓存，防止重复创建
    static instances: ModalInstances = new Map();
    
    /** 当前聚焦的元素 */
    static _previousFocus: HTMLElement | null = null;
    
    /** 当前打开的模态框选择器 */
    static _currentModal: string | null = null;

    /** 模态框元素 */
    modal: HTMLElement;
    /** 遮罩层元素 */
    overlay: HTMLElement | null;
    /** 关闭按钮元素 */
    closeBtn: HTMLElement | null;
    /** 内容区域元素 */
    content: HTMLElement | null;
    /** 可聚焦元素列表 */
    private _focusableElements: NodeListOf<Element> | null = null;
    /** 第一个可聚焦元素 */
    private _firstFocusable: HTMLElement | null = null;
    /** 最后一个可聚焦元素 */
    private _lastFocusable: HTMLElement | null = null;

    /**
     * 构造函数
     * @param modalElement 
     */
    constructor(modalElement: HTMLElement) {
        this.modal = modalElement;
        // overlay 现在和 modal 是平级元素，通过 ID 查找
        this.overlay = document.getElementById('modalOverlay');
        this.closeBtn = modalElement.querySelector('.modal-close');
        this.content = modalElement.querySelector('.modal-content');
        this._focusableElements = null;
        this._firstFocusable = null;
        this._lastFocusable = null;
        
        this.bindEvents();
    }

    /**
     * 静态方法：创建模态框实例
     * @param selector 
     * @returns Modal实例或null
     */
    static create(selector: string): Modal | null {
        // 检查是否已存在实例
        if (Modal.instances.has(selector)) {
            return Modal.instances.get(selector) as Modal;
        }

        const modalElement = document.querySelector(selector);
        if (modalElement) {
            const modal = new Modal(modalElement as HTMLElement);
            Modal.instances.set(selector, modal);
            return modal;
        }
        return null;
    }

    /**
     * 静态方法：显示指定模态框
     * @param selector 
     */
    static show(selector: string): void {
        const modal = Modal.create(selector);
        if (modal) {
            modal.show();
        }
    }

    /**
     * 静态方法：隐藏指定模态框
     * @param selector 
     */
    static hide(selector: string): void {
        const modal = Modal.create(selector);
        if (modal) {
            modal.hide();
        }
    }
    
    /**
     * 静态方法：关闭当前打开的模态框
     */
    static closeCurrent(): void {
        if (Modal._currentModal) {
            const instance = Modal.instances.get(Modal._currentModal);
            if (instance) {
                instance.hide();
            }
        }
    }

    /**
     * 绑定事件
     */
    bindEvents(): void {
        // 关闭按钮点击
        if (this.closeBtn) {
            this.closeBtn.addEventListener('click', (e) => {
                e.stopPropagation();
                this.hide();
            });
        }

        // 点击模态框外部关闭
        if (this.overlay) {
            this.overlay.addEventListener('click', (e) => {
                if (e.target === this.overlay) {
                    e.stopPropagation();
                    this.hide();
                }
            });
        }
        
        // 键盘事件处理
        this.modal.addEventListener('keydown', (e) => this._handleKeydown(e));
    }
    
    /**
     * 获取可聚焦元素
     * @private
     */
    private _updateFocusableElements(): void {
        const focusableSelectors = [
            'button:not([disabled])',
            'input:not([disabled])',
            'select:not([disabled])',
            'textarea:not([disabled])',
            'a[href]',
            '[tabindex]:not([tabindex="-1"])'
        ].join(', ');
        
        this._focusableElements = this.modal.querySelectorAll(focusableSelectors);
        this._firstFocusable = this._focusableElements[0] as HTMLElement | null;
        this._lastFocusable = this._focusableElements[this._focusableElements.length - 1] as HTMLElement | null;
    }
    
    /**
     * 处理键盘事件
     * @private
     * @param e 
     */
    private _handleKeydown(e: KeyboardEvent): void {
        // ESC键关闭
        if (e.key === 'Escape') {
            e.preventDefault();
            this.hide();
            return;
        }
        
        // Tab键焦点陷阱
        if (e.key === 'Tab') {
            this._updateFocusableElements();
            
            if (e.shiftKey) {
                // Shift + Tab
                if (document.activeElement === this._firstFocusable) {
                    e.preventDefault();
                    this._lastFocusable?.focus();
                }
            } else {
                // Tab
                if (document.activeElement === this._lastFocusable) {
                    e.preventDefault();
                    this._firstFocusable?.focus();
                }
            }
        }
    }

    /**
     * 显示模态框
     */
    show(): void {
        if (this.modal) {
            // 保存当前焦点
            Modal._previousFocus = document.activeElement as HTMLElement;
            Modal._currentModal = this.modal.id ? `#${this.modal.id}` : null;
            
            // 添加 modal-active 类，触发 CSS 过渡动画
            this.modal.classList.add('modal-active');
            
            // 同时显示遮罩层
            if (this.overlay) {
                this.overlay.classList.add('active');
            }
            
            // 设置 ARIA 属性
            this.modal.setAttribute('aria-hidden', 'false');
            this.modal.setAttribute('aria-modal', 'true');
            
            // 防止背景滚动
            document.body.style.overflow = 'hidden';
            document.body.classList.add('modal-open');
            
            // 更新可聚焦元素并设置焦点
            requestAnimationFrame(() => {
                this._updateFocusableElements();
                
                // 尝试聚焦到关闭按钮或第一个可聚焦元素
                const focusTarget = this.closeBtn || this._firstFocusable;
                if (focusTarget) {
                    focusTarget.focus();
                }
            });
        }
    }

    /**
     * 隐藏模态框
     */
    hide(): void {
        if (this.modal) {
            // 移除 modal-active 类，触发 CSS 过渡动画
            this.modal.classList.remove('modal-active');
            
            // 同时隐藏遮罩层
            if (this.overlay) {
                this.overlay.classList.remove('active');
            }
            
            // 设置 ARIA 属性
            this.modal.setAttribute('aria-hidden', 'true');
            this.modal.removeAttribute('aria-modal');
            
            // 清除当前模态框引用
            if (Modal._currentModal === `#${this.modal.id}`) {
                Modal._currentModal = null;
            }
            
            // 延迟恢复背景滚动，等待过渡动画完成
            setTimeout(() => {
                document.body.style.overflow = '';
                document.body.classList.remove('modal-open');
                
                // 恢复之前的焦点
                if (Modal._previousFocus && typeof Modal._previousFocus.focus === 'function') {
                    Modal._previousFocus.focus();
                }
                Modal._previousFocus = null;
            }, 300);
        }
    }

    /**
     * 切换模态框显示/隐藏
     */
    toggle(): void {
        if (this.modal) {
            if (this.modal.classList.contains('modal-active')) {
                this.hide();
            } else {
                this.show();
            }
        }
    }
    
    /**
     * 设置模态框内容
     * @param content - HTML内容
     */
    setContent(content: string): void {
        if (this.content) {
            this.content.innerHTML = content;
        }
    }
    
    /**
     * 设置模态框标题
     * @param title 
     */
    setTitle(title: string): void {
        const titleElement = this.modal.querySelector('.modal-title');
        if (titleElement) {
            titleElement.textContent = title;
        }
    }
    
    /**
     * 销毁模态框实例
     */
    destroy(): void {
        if (this.modal.classList.contains('modal-active')) {
            this.hide();
        }
        
        // 移除事件监听器
        if (this.closeBtn) {
            this.closeBtn.replaceWith(this.closeBtn.cloneNode(true));
        }
        if (this.overlay) {
            this.overlay.replaceWith(this.overlay.cloneNode(true));
        }
        
        // 从实例缓存中移除
        const selector = `#${this.modal.id}`;
        Modal.instances.delete(selector);
    }
}

export default Modal;
