import { create } from 'zustand';

interface Toast {
    id: string;
    type: 'success' | 'error' | 'info' | 'warning';
    message: string;
}

export interface UIState {
    toasts: Toast[];
    isLoading: boolean;
    loadingMessage: string;
    addToast: (toast: Omit<Toast, 'id'>) => void;
    removeToast: (id: string) => void;
    showLoading: (message?: string) => void;
    hideLoading: () => void;
}

let toastCounter = 0;

function generateToastId(): string {
    toastCounter += 1;
    return `toast-${Date.now()}-${toastCounter}`;
}

export const useUIStore = create<UIState>()(set => ({
    toasts: [],
    isLoading: false,
    loadingMessage: '',

    addToast: toast => {
        const id = generateToastId();
        set(state => ({
            toasts: [...state.toasts, { ...toast, id }],
        }));

        setTimeout(() => {
            set(state => ({
                toasts: state.toasts.filter(t => t.id !== id),
            }));
        }, 3000);
    },

    removeToast: id =>
        set(state => ({
            toasts: state.toasts.filter(t => t.id !== id),
        })),

    showLoading: (message = '加载中...') => set({ isLoading: true, loadingMessage: message }),

    hideLoading: () => set({ isLoading: false, loadingMessage: '' }),
}));
