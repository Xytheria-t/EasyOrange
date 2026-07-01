import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it } from 'vitest';
import { useUIStore } from '@/store';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import { ToastContainer } from './Toast';

beforeEach(() => {
    useUIStore.setState({ toasts: [], isLoading: false, loadingMessage: '' });
});

describe('ToastContainer', () => {
    it('renders no toast messages when no toasts exist', () => {
        renderWithProviders(<ToastContainer />);
        expect(screen.queryByRole('status')).not.toBeInTheDocument();
    });

    it('renders toasts from store', () => {
        useUIStore.setState({
            toasts: [
                { id: '1', type: 'success', message: '操作成功' },
                { id: '2', type: 'error', message: '操作失败' },
            ],
        });
        renderWithProviders(<ToastContainer />);
        expect(screen.getByText('操作成功')).toBeInTheDocument();
        expect(screen.getByText('操作失败')).toBeInTheDocument();
    });

    it('renders close buttons', () => {
        useUIStore.setState({
            toasts: [{ id: '1', type: 'info', message: '提示消息' }],
        });
        renderWithProviders(<ToastContainer />);
        expect(screen.getByLabelText('关闭通知')).toBeInTheDocument();
    });

    it('removes toast when close button clicked', async () => {
        useUIStore.setState({
            toasts: [{ id: '1', type: 'info', message: '可关闭' }],
        });
        renderWithProviders(<ToastContainer />);

        const closeBtn = screen.getByLabelText('关闭通知');
        await userEvent.click(closeBtn);

        const state = useUIStore.getState();
        expect(state.toasts).toHaveLength(0);
    });

    it('applies correct CSS class based on type', () => {
        useUIStore.setState({
            toasts: [
                { id: '1', type: 'success', message: '成功' },
                { id: '2', type: 'error', message: '失败' },
            ],
        });
        const { container } = renderWithProviders(<ToastContainer />);
        expect(container.querySelector('.toast-success')).toBeInTheDocument();
        expect(container.querySelector('.toast-error')).toBeInTheDocument();
    });
});
