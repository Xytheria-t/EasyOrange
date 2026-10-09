import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { CopyButton } from './CopyButton';

describe('CopyButton', () => {
    it('copies value via clipboard API and reports success', async () => {
        const writeText = vi.fn().mockResolvedValue(undefined);
        Object.assign(navigator, { clipboard: { writeText } });
        const onCopy = vi.fn();
        render(<CopyButton value="hello" onCopy={onCopy} />);

        fireEvent.click(screen.getByRole('button'));

        await waitFor(() => expect(onCopy).toHaveBeenCalledWith('hello', true));
        expect(writeText).toHaveBeenCalledWith('hello');
    });

    it('accepts a function for deferred value', async () => {
        const writeText = vi.fn().mockResolvedValue(undefined);
        Object.assign(navigator, { clipboard: { writeText } });
        render(<CopyButton value={() => 'lazy'} />);

        fireEvent.click(screen.getByRole('button'));
        await waitFor(() => expect(writeText).toHaveBeenCalledWith('lazy'));
    });

    it('announces copied state and reverts after reset delay', async () => {
        vi.useFakeTimers();
        try {
            Object.assign(navigator, {
                clipboard: { writeText: vi.fn().mockResolvedValue(undefined) },
            });
            render(<CopyButton value="x" />);
            const button = screen.getByRole('button');

            expect(button).toHaveAttribute('aria-label', '复制');
            // act 的异步回调会把 clipboard 的 Promise 链与随后的 setState 一起冲完
            await act(async () => {
                fireEvent.click(button);
            });

            expect(button).toHaveAttribute('aria-label', '已复制');
            expect(button).toHaveAttribute('data-copied', 'true');
            expect(screen.getByRole('status')).toHaveTextContent('已复制');

            await act(async () => {
                await vi.advanceTimersByTimeAsync(2000);
            });
            expect(button).toHaveAttribute('aria-label', '复制');
        } finally {
            vi.useRealTimers();
        }
    });

    it('falls back to execCommand when clipboard API rejects', async () => {
        const execCommand = vi.fn().mockReturnValue(true);
        Object.assign(navigator, {
            clipboard: { writeText: vi.fn().mockRejectedValue(new Error('denied')) },
        });
        const docWithExec = document as unknown as { execCommand?: (id: string) => boolean };
        docWithExec.execCommand = execCommand;
        try {
            const onCopy = vi.fn();
            render(<CopyButton value="fallback" onCopy={onCopy} />);

            fireEvent.click(screen.getByRole('button'));

            await waitFor(() => expect(onCopy).toHaveBeenCalledWith('fallback', true));
            expect(execCommand).toHaveBeenCalledWith('copy');
        } finally {
            docWithExec.execCommand = undefined;
        }
    });

    it('reports failure when both clipboard paths fail', async () => {
        Object.assign(navigator, { clipboard: { writeText: vi.fn().mockRejectedValue(new Error('no')) } });
        const onCopy = vi.fn();
        render(<CopyButton value="nope" onCopy={onCopy} />);

        fireEvent.click(screen.getByRole('button'));

        await waitFor(() => expect(onCopy).toHaveBeenCalledWith('nope', false));
        expect(screen.getByRole('button')).toHaveAttribute('aria-label', '复制');
    });

    it('renders label mode with swap text', () => {
        Object.assign(navigator, { clipboard: { writeText: vi.fn() } });
        render(<CopyButton value="x" label="复制链接" size="sm" />);
        const button = screen.getByRole('button');
        expect(button).toHaveTextContent('复制链接');
        // 有文案时无障碍名交给文案，aria-label 不再插手
        expect(button).not.toHaveAttribute('aria-label');
    });

    it('clears pending reset timer on unmount', async () => {
        vi.useFakeTimers();
        try {
            Object.assign(navigator, {
                clipboard: { writeText: vi.fn().mockResolvedValue(undefined) },
            });
            const { unmount } = render(<CopyButton value="x" />);
            await act(async () => {
                fireEvent.click(screen.getByRole('button'));
            });
            unmount();
            // 卸载后再推进定时器不应抛「setState on unmounted」
            await act(async () => {
                await vi.advanceTimersByTimeAsync(2000);
            });
        } finally {
            vi.useRealTimers();
        }
    });
});
