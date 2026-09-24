import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { PaginationBar } from './PaginationBar';

describe('PaginationBar', () => {
    it('renders nothing when there is a single page', () => {
        const { container } = render(<PaginationBar pageNum={1} totalPages={1} onPageChange={vi.fn()} />);
        expect(container).toBeEmptyDOMElement();
    });

    it('renders every page when the total is small', () => {
        render(<PaginationBar pageNum={1} totalPages={5} onPageChange={vi.fn()} />);
        for (const p of [1, 2, 3, 4, 5]) {
            expect(screen.getByRole('button', { name: String(p) })).toBeInTheDocument();
        }
    });

    it('collapses distant pages into an ellipsis on large totals', () => {
        render(<PaginationBar pageNum={10} totalPages={50} onPageChange={vi.fn()} />);
        // 首尾 + 当前页附近
        expect(screen.getByRole('button', { name: '1' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: '50' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: '9' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: '10' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: '11' })).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: '25' })).not.toBeInTheDocument();
    });

    it('disables previous on the first page instead of only blocking pointer events', () => {
        render(<PaginationBar pageNum={1} totalPages={5} onPageChange={vi.fn()} />);
        expect(screen.getByRole('button', { name: '上一页' })).toBeDisabled();
    });

    it('disables next on the last page', () => {
        render(<PaginationBar pageNum={5} totalPages={5} onPageChange={vi.fn()} />);
        expect(screen.getByRole('button', { name: '下一页' })).toBeDisabled();
    });

    it('enables both controls in the middle', () => {
        render(<PaginationBar pageNum={3} totalPages={5} onPageChange={vi.fn()} />);
        expect(screen.getByRole('button', { name: '上一页' })).toBeEnabled();
        expect(screen.getByRole('button', { name: '下一页' })).toBeEnabled();
    });

    it('marks the current page as active', () => {
        render(<PaginationBar pageNum={3} totalPages={5} onPageChange={vi.fn()} />);
        expect(screen.getByRole('button', { name: '3' })).toHaveAttribute('aria-current', 'page');
    });

    it('reports the requested page on click', () => {
        const onPageChange = vi.fn();
        render(<PaginationBar pageNum={1} totalPages={5} onPageChange={onPageChange} />);
        fireEvent.click(screen.getByRole('button', { name: '3' }));
        expect(onPageChange).toHaveBeenCalledWith(3);
    });

    it('clamps an out-of-range current page for rendering', () => {
        render(<PaginationBar pageNum={99} totalPages={5} onPageChange={vi.fn()} />);
        // 钳到最后一页，末页高亮
        expect(screen.getByRole('button', { name: '5' })).toHaveAttribute('aria-current', 'page');
    });

    it('clamps a page number below 1', () => {
        render(<PaginationBar pageNum={-3} totalPages={5} onPageChange={vi.fn()} />);
        expect(screen.getByRole('button', { name: '1' })).toHaveAttribute('aria-current', 'page');
    });
});
