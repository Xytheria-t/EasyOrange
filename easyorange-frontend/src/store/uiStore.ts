import { create } from 'zustand';

interface Toast {
    id: string;
    type: 'success' | 'error' | 'info' | 'warning';
    message: string;
}

interface ProfileSetup {
    open: boolean;
    username: string;
}

export interface UIState {
    toasts: Toast[];
    addToast: (toast: Omit<Toast, 'id'>) => void;
    removeToast: (id: string) => void;
    /** 注册后的完善信息弹窗 — 全局态，注册页跳转卸载后弹窗仍存活 */
    profileSetup: ProfileSetup;
    openProfileSetup: (username: string) => void;
    closeProfileSetup: () => void;
}

let toastCounter = 0;

function generateToastId(): string {
    toastCounter += 1;
    return `toast-${Date.now()}-${toastCounter}`;
}

export const useUIStore = create<UIState>()(set => ({
    toasts: [],

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

    profileSetup: { open: false, username: '' },

    openProfileSetup: username => set({ profileSetup: { open: true, username } }),

    closeProfileSetup: () => set({ profileSetup: { open: false, username: '' } }),
}));
