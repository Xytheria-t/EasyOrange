import { useUIStore } from '@/store';

/**
 * 管理端操作反馈。
 *
 * 收敛前 5 个页面的 `mutateAsync` 都没有 catch：请求失败时按钮恢复原状、
 * 控制台报未处理的 Promise，界面上看不出发生了什么。
 */
function messageOf(error: unknown): string | null {
    if (!error) {
        return null;
    }
    if (typeof error === 'string') {
        return error;
    }
    if (error instanceof Error) {
        return error.message;
    }
    if (typeof error === 'object' && 'message' in error) {
        const msg = (error as { message?: unknown }).message;
        return typeof msg === 'string' ? msg : null;
    }
    return null;
}

export const notify = {
    success(message: string) {
        useUIStore.getState().addToast({ type: 'success', message });
    },
    error(message: string) {
        useUIStore.getState().addToast({ type: 'error', message });
    },
    /** 变更失败统一出口：后端有 message 就透传，没有则给可执行的下一步。 */
    failure(error: unknown, fallback: string) {
        const message = messageOf(error);
        useUIStore.getState().addToast({ type: 'error', message: message || fallback });
    },
};
