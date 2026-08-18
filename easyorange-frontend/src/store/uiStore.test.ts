import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { useUIStore } from './uiStore';

beforeEach(() => {
    useUIStore.setState({
        toasts: [],
        isLoading: false,
        loadingMessage: '',
    });
});

afterEach(() => {
    vi.restoreAllMocks();
});

describe('uiStore', () => {
    describe('initial state', () => {
        it('starts with empty toasts and not loading', () => {
            const state = useUIStore.getState();
            expect(state.toasts).toEqual([]);
            expect(state.isLoading).toBe(false);
            expect(state.loadingMessage).toBe('');
        });
    });

    describe('addToast', () => {
        it('adds a toast with generated id', () => {
            useUIStore.getState().addToast({ type: 'success', message: '操作成功' });
            const state = useUIStore.getState();
            expect(state.toasts).toHaveLength(1);
            expect(state.toasts[0].type).toBe('success');
            expect(state.toasts[0].message).toBe('操作成功');
            expect(state.toasts[0].id).toBeDefined();
        });

        it('auto-removes toast after timeout', () => {
            vi.useFakeTimers();
            useUIStore.getState().addToast({ type: 'info', message: '临时消息' });
            expect(useUIStore.getState().toasts).toHaveLength(1);

            vi.advanceTimersByTime(3000);
            expect(useUIStore.getState().toasts).toHaveLength(0);
            vi.useRealTimers();
        });
    });

    describe('removeToast', () => {
        it('removes a toast by id', () => {
            useUIStore.setState({
                toasts: [{ id: 'toast-1', type: 'info', message: 'test' }],
            });
            useUIStore.getState().removeToast('toast-1');
            expect(useUIStore.getState().toasts).toHaveLength(0);
        });

        it('does nothing for non-existent id', () => {
            useUIStore.setState({
                toasts: [{ id: 'toast-1', type: 'info', message: 'test' }],
            });
            useUIStore.getState().removeToast('toast-999');
            expect(useUIStore.getState().toasts).toHaveLength(1);
        });
    });
});
