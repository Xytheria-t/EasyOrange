/**
 * @fileoverview 统一弹窗管理模块
 * @description 提供模态框的统一管理，支持 open/close/closeAll 操作
 * @version 1.0.0
 */

interface ModalState {
    isOpen: boolean;
    openedAt?: number;
}

/**
 * 模态框管理器
 * 提供全局模态框状态的跟踪和管理
 */
class ModalManager {
    private modalStates: Map<string, ModalState>;
    private activeModals: Set<string>;
    private previousActiveElement: HTMLElement | null;

    constructor() {
        this.modalStates = new Map();
        this.activeModals = new Set();
        this.previousActiveElement = null;
    }

    /**
     * 获取模态框元素
     * @param modalId - 模态框 ID
     */
    private getModal(modalId: string): HTMLElement | null {
        return document.getElementById(modalId);
    }

    /**
     * 获取遮罩层元素
     */
    private getOverlay(): HTMLElement | null {
        return document.getElementById('modalOverlay');
    }

    /**
     * 打开模态框
     * @param modalId - 模态框 ID
     * @param options - 配置选项
     */
    open(modalId: string, options?: { closeOnOverlayClick?: boolean }): boolean {
        const modal = this.getModal(modalId);
        if (!modal) {
            console.warn(`ModalManager: Modal with id "${modalId}" not found`);
            return false;
        }

        // 记录当前激活元素（用于无障碍访问）
        this.previousActiveElement = document.activeElement as HTMLElement;

        // 显示遮罩层
        const overlay = this.getOverlay();
        if (overlay) {
            overlay.classList.add('active');
        }

        // 打开模态框
        modal.classList.add('active');
        this.modalStates.set(modalId, { isOpen: true, openedAt: Date.now() });
        this.activeModals.add(modalId);

        // 如果需要，点击遮罩层关闭
        if (options?.closeOnOverlayClick !== false) {
            const closeOnOverlay = () => {
                if (overlay && overlay.classList.contains('active')) {
                    this.close(modalId);
                }
            };

            overlay?.addEventListener('click', closeOnOverlay);
        }

        // 聚焦到模态框内部第一个可聚焦元素
        const focusableElements = modal.querySelectorAll<HTMLElement>(
            'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
        );
        if (focusableElements.length > 0) {
            focusableElements[0].focus();
        }

        return true;
    }

    /**
     * 关闭模态框
     * @param modalId - 模态框 ID
     */
    close(modalId: string): boolean {
        const modal = this.getModal(modalId);
        if (!modal) {
            console.warn(`ModalManager: Modal with id "${modalId}" not found`);
            return false;
        }

        // 隐藏遮罩层
        const overlay = this.getOverlay();
        if (overlay) {
            overlay.classList.remove('active');
        }

        // 关闭模态框
        modal.classList.remove('active');
        this.modalStates.set(modalId, { isOpen: false });
        this.activeModals.delete(modalId);

        // 恢复之前聚焦的元素
        if (this.previousActiveElement) {
            this.previousActiveElement.focus();
            this.previousActiveElement = null;
        }

        return true;
    }

    /**
     * 关闭所有模态框
     */
    closeAll(): void {
        // 关闭遮罩层
        const overlay = this.getOverlay();
        if (overlay) {
            overlay.classList.remove('active');
        }

        // 关闭所有打开的模态框
        this.activeModals.forEach((modalId) => {
            const modal = this.getModal(modalId);
            if (modal) {
                modal.classList.remove('active');
            }
            this.modalStates.set(modalId, { isOpen: false });
        });

        this.activeModals.clear();

        // 恢复之前聚焦的元素
        if (this.previousActiveElement) {
            this.previousActiveElement.focus();
            this.previousActiveElement = null;
        }
    }

    /**
     * 切换模态框状态
     * @param modalId - 模态框 ID
     */
    toggle(modalId: string): boolean {
        if (this.isOpen(modalId)) {
            return this.close(modalId);
        } else {
            return this.open(modalId);
        }
    }

    /**
     * 检查模态框是否打开
     * @param modalId - 模态框 ID
     */
    isOpen(modalId: string): boolean {
        return this.activeModals.has(modalId);
    }

    /**
     * 获取当前打开的模态框数量
     */
    getOpenCount(): number {
        return this.activeModals.size;
    }

    /**
     * 检查是否有模态框打开
     */
    hasOpenModals(): boolean {
        return this.activeModals.size > 0;
    }

    /**
     * 获取已打开的模态框 ID 列表
     */
    getOpenModals(): string[] {
        return Array.from(this.activeModals);
    }
}

// 导出单例
const modalManager = new ModalManager();

export { ModalManager, modalManager };
export default modalManager;
