import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { EmptyState, ErrorState, LoadingState } from './StateDisplay';

describe('EmptyState', () => {
    it('renders title and description', () => {
        render(<EmptyState title="未找到相关商品" description="换个关键词试试" />);
        expect(screen.getByText('未找到相关商品')).toBeInTheDocument();
        expect(screen.getByText('换个关键词试试')).toBeInTheDocument();
    });

    it('omits description when not provided', () => {
        render(<EmptyState title="暂无数据" />);
        expect(screen.queryByText(/网络或服务/)).not.toBeInTheDocument();
    });

    it('renders action slot', () => {
        render(<EmptyState title="空" action={<button type="button">去发布</button>} />);
        expect(screen.getByRole('button', { name: '去发布' })).toBeInTheDocument();
    });
});

describe('ErrorState', () => {
    it('announces itself as an alert', () => {
        render(<ErrorState />);
        expect(screen.getByRole('alert')).toBeInTheDocument();
    });

    it('uses a default title distinct from empty state wording', () => {
        render(<ErrorState />);
        expect(screen.getByText('加载失败')).toBeInTheDocument();
    });

    it('renders retry button and calls onRetry', () => {
        const onRetry = vi.fn();
        render(<ErrorState onRetry={onRetry} />);
        fireEvent.click(screen.getByRole('button', { name: /重新加载/ }));
        expect(onRetry).toHaveBeenCalledTimes(1);
    });

    it('hides retry button when no handler given', () => {
        render(<ErrorState />);
        expect(screen.queryByRole('button')).not.toBeInTheDocument();
    });

    it('accepts custom title and retry label', () => {
        render(<ErrorState title="统计加载失败" onRetry={vi.fn()} retryLabel="刷新统计" />);
        expect(screen.getByText('统计加载失败')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /刷新统计/ })).toBeInTheDocument();
    });
});

describe('LoadingState', () => {
    it('marks itself busy and announces politely', () => {
        render(<LoadingState />);
        const status = screen.getByRole('status');
        expect(status).toHaveAttribute('aria-busy', 'true');
        expect(status).toHaveAttribute('aria-live', 'polite');
    });

    it('exposes a screen-reader label', () => {
        render(<LoadingState label="正在加载商品" />);
        expect(screen.getByText('正在加载商品')).toBeInTheDocument();
    });
});
