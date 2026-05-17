import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ConfirmModal } from './ConfirmModal';

describe('ConfirmModal', () => {
  it('renders title and content', () => {
    render(
      <ConfirmModal
        open={true}
        title="确认删除"
        content="确定要删除吗？"
        onConfirm={() => {}}
        onCancel={() => {}}
      />,
    );
    expect(screen.getByText('确认删除')).toBeInTheDocument();
    expect(screen.getByText('确定要删除吗？')).toBeInTheDocument();
  });

  it('renders nothing when open is false', () => {
    const { container } = render(
      <ConfirmModal
        open={false}
        title="确认删除"
        content="确定要删除吗？"
        onConfirm={() => {}}
        onCancel={() => {}}
      />,
    );
    expect(container.innerHTML).toBe('');
  });

  it('calls onConfirm when confirm button clicked', async () => {
    const onConfirm = vi.fn();
    render(
      <ConfirmModal
        open={true}
        title="确认"
        content="确定？"
        onConfirm={onConfirm}
        onCancel={() => {}}
      />,
    );
    const confirmBtn = screen.getAllByRole('button').find(b => b.textContent === '确认');
    expect(confirmBtn).toBeDefined();
    await userEvent.click(confirmBtn!);
    expect(onConfirm).toHaveBeenCalledTimes(1);
  });

  it('calls onCancel when cancel button clicked', async () => {
    const onCancel = vi.fn();
    render(
      <ConfirmModal
        open={true}
        title="确认"
        content="确定？"
        onConfirm={() => {}}
        onCancel={onCancel}
      />,
    );
    await userEvent.click(screen.getByText('取消'));
    expect(onCancel).toHaveBeenCalledTimes(1);
  });

  it('calls onCancel when backdrop clicked', async () => {
    const onCancel = vi.fn();
    render(
      <ConfirmModal
        open={true}
        title="确认"
        content="确定？"
        onConfirm={() => {}}
        onCancel={onCancel}
      />,
    );
    await userEvent.click(screen.getByLabelText('关闭对话框'));
    expect(onCancel).toHaveBeenCalledTimes(1);
  });

  it('uses custom confirmText and cancelText', () => {
    render(
      <ConfirmModal
        open={true}
        title="确认"
        content="确定？"
        confirmText="Yes"
        cancelText="No"
        onConfirm={() => {}}
        onCancel={() => {}}
      />,
    );
    expect(screen.getByText('Yes')).toBeInTheDocument();
    expect(screen.getByText('No')).toBeInTheDocument();
  });

  it('has dialog role', () => {
    render(
      <ConfirmModal
        open={true}
        title="确认"
        content="确定？"
        onConfirm={() => {}}
        onCancel={() => {}}
      />,
    );
    expect(screen.getByRole('dialog')).toBeInTheDocument();
  });

  it('shows loading state', () => {
    render(
      <ConfirmModal
        open={true}
        title="确认"
        content="确定？"
        onConfirm={() => {}}
        onCancel={() => {}}
        loading={true}
      />,
    );
    expect(screen.getByText('处理中...')).toBeInTheDocument();
  });
});
