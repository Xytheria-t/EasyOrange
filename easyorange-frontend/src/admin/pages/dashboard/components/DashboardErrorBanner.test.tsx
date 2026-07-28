import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { DashboardErrorBanner } from './DashboardErrorBanner';

describe('DashboardErrorBanner', () => {
    it('renders error banner with first error message', () => {
        render(<DashboardErrorBanner errors={[null, new Error('Network failure'), null]} />);

        expect(screen.getByText('数据加载失败')).toBeInTheDocument();
        expect(screen.getByText(/Network failure/)).toBeInTheDocument();
        expect(screen.getByText('刷新页面')).toBeInTheDocument();
    });

    it('shows generic message when no error details provided', () => {
        render(<DashboardErrorBanner errors={[null, null, null]} />);

        expect(screen.getByText('数据加载失败')).toBeInTheDocument();
        expect(screen.getByText('无法连接到服务器，请检查后端服务是否启动')).toBeInTheDocument();
    });

    it('reloads page when retry button clicked', () => {
        const reloadMock = vi.fn();
        Object.defineProperty(window, 'location', {
            value: { reload: reloadMock },
            writable: true,
        });

        render(<DashboardErrorBanner errors={[new Error('fail')]} />);
        screen.getByText('刷新页面').click();

        expect(reloadMock).toHaveBeenCalled();
    });
});
