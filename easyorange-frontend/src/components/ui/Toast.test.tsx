import { describe, it, expect, beforeEach } from 'vitest';
import { screen } from '@testing-library/react';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import { ToastContainer } from './Toast';
import { useUIStore } from '@/store';

beforeEach(() => {
  useUIStore.setState({ toasts: [], isLoading: false, loadingMessage: '' });
});

describe('ToastContainer', () => {
  it('renders nothing when no toasts exist', () => {
    const { container } = renderWithProviders(<ToastContainer />);
    expect(container.firstChild).toBeNull();
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

  it('removes toast when close button clicked', () => {
    useUIStore.setState({
      toasts: [{ id: '1', type: 'info', message: '可关闭' }],
    });
    renderWithProviders(<ToastContainer />);

    const closeBtn = screen.getByLabelText('关闭通知');
    closeBtn.click();

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
