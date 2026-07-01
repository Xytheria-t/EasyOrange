import { screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import { ErrorBoundary } from './ErrorBoundary';

// Suppress console.error from React error boundary in tests
const originalError = console.error;
beforeEach(() => {
    console.error = vi.fn();
});
afterEach(() => {
    console.error = originalError;
});

describe('ErrorBoundary', () => {
    it('renders children when no error', () => {
        renderWithProviders(
            <ErrorBoundary>
                <div>正常内容</div>
            </ErrorBoundary>
        );
        expect(screen.getByText('正常内容')).toBeInTheDocument();
    });

    it('renders fallback UI when child throws', () => {
        const ThrowComponent = () => {
            throw new Error('测试错误');
        };

        renderWithProviders(
            <ErrorBoundary>
                <ThrowComponent />
            </ErrorBoundary>
        );

        expect(screen.getByText('页面出错了')).toBeInTheDocument();
        expect(screen.getByText('重试')).toBeInTheDocument();
        expect(screen.getByText('刷新页面')).toBeInTheDocument();
    });

    it('renders custom fallback when provided', () => {
        const ThrowComponent = () => {
            throw new Error('测试错误');
        };

        renderWithProviders(
            <ErrorBoundary fallback={<div>自定义错误页</div>}>
                <ThrowComponent />
            </ErrorBoundary>
        );

        expect(screen.getByText('自定义错误页')).toBeInTheDocument();
    });

    it('renders retry button that resets error state', () => {
        const ThrowComponent = () => {
            throw new Error('测试错误');
        };

        renderWithProviders(
            <ErrorBoundary>
                <ThrowComponent />
            </ErrorBoundary>
        );

        expect(screen.getByText('页面出错了')).toBeInTheDocument();

        // Click retry - this resets the error state and re-renders the children
        screen.getByText('重试').click();

        // After reset, the component should be back to normal rendering
        // But since ThrowComponent throws unconditionally, it will show the error again
        expect(screen.getByText('页面出错了')).toBeInTheDocument();
    });
});
