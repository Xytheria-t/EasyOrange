interface ModalState {
    isOpen: boolean;
    openedAt?: number;
}

class ModalManager {
    private modalStates: Map<string, ModalState>;
    private activeModals: Set<string>;
    private previousActiveElement: HTMLElement | null;

    constructor() {
        this.modalStates = new Map();
        this.activeModals = new Set();
        this.previousActiveElement = null;
    }

    private getModal(modalId: string): HTMLElement | null {
        return document.getElementById(modalId);
    }

    private getOverlay(): HTMLElement | null {
        return document.getElementById('modalOverlay');
    }

    open(modalId: string, options?: { closeOnOverlayClick?: boolean }): boolean {
        const modal = this.getModal(modalId);
        if (!modal) {
            return false;
        }

        this.previousActiveElement = document.activeElement as HTMLElement;

        const overlay = this.getOverlay();
        if (overlay) {
            overlay.classList.add('active');
        }

        modal.classList.add('active');
        this.modalStates.set(modalId, { isOpen: true, openedAt: Date.now() });
        this.activeModals.add(modalId);

        if (options?.closeOnOverlayClick !== false) {
            const closeOnOverlay = () => {
                if (overlay && overlay.classList.contains('active')) {
                    this.close(modalId);
                }
            };

            overlay?.addEventListener('click', closeOnOverlay);
        }

        const focusableElements = modal.querySelectorAll<HTMLElement>(
            'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
        );
        if (focusableElements.length > 0) {
            focusableElements[0].focus();
        }

        return true;
    }

    close(modalId: string): boolean {
        const modal = this.getModal(modalId);
        if (!modal) {
            return false;
        }

        const overlay = this.getOverlay();
        if (overlay) {
            overlay.classList.remove('active');
        }

        modal.classList.remove('active');
        this.modalStates.set(modalId, { isOpen: false });
        this.activeModals.delete(modalId);

        if (this.previousActiveElement) {
            this.previousActiveElement.focus();
            this.previousActiveElement = null;
        }

        return true;
    }

    closeAll(): void {
        const overlay = this.getOverlay();
        if (overlay) {
            overlay.classList.remove('active');
        }

        this.activeModals.forEach((modalId) => {
            const modal = this.getModal(modalId);
            if (modal) {
                modal.classList.remove('active');
            }
            this.modalStates.set(modalId, { isOpen: false });
        });

        this.activeModals.clear();

        if (this.previousActiveElement) {
            this.previousActiveElement.focus();
            this.previousActiveElement = null;
        }
    }

    toggle(modalId: string): boolean {
        if (this.isOpen(modalId)) {
            return this.close(modalId);
        } else {
            return this.open(modalId);
        }
    }

    isOpen(modalId: string): boolean {
        return this.activeModals.has(modalId);
    }

    getOpenCount(): number {
        return this.activeModals.size;
    }

    hasOpenModals(): boolean {
        return this.activeModals.size > 0;
    }

    getOpenModals(): string[] {
        return Array.from(this.activeModals);
    }
}

const modalManager = new ModalManager();

export { ModalManager, modalManager };
export default modalManager;
