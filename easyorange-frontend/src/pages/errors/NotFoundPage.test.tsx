import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import NotFoundPage from './NotFoundPage';

// Mock useNavigate
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom');
    return {
        ...actual,
        useNavigate: () => mockNavigate,
    };
});

describe('NotFoundPage', () => {
    beforeEach(() => {
        mockNavigate.mockClear();
    });

    it('renders 404 text', () => {
        render(
            <MemoryRouter>
                <NotFoundPage />
            </MemoryRouter>
        );
        expect(screen.getByText('404')).toBeInTheDocument();
    });

    it('renders title 页面走丢了', () => {
        render(
            <MemoryRouter>
                <NotFoundPage />
            </MemoryRouter>
        );
        expect(screen.getByText('页面走丢了')).toBeInTheDocument();
    });

    it('renders back button that calls navigate(-1)', async () => {
        render(
            <MemoryRouter>
                <NotFoundPage />
            </MemoryRouter>
        );
        await userEvent.click(screen.getByText('返回上页'));
        expect(mockNavigate).toHaveBeenCalledWith(-1);
    });

    it('renders 回到首页 link', () => {
        render(
            <MemoryRouter>
                <NotFoundPage />
            </MemoryRouter>
        );
        expect(screen.getByText('回到首页')).toBeInTheDocument();
    });

    it('renders smart navigation suggestions', () => {
        render(
            <MemoryRouter>
                <NotFoundPage />
            </MemoryRouter>
        );
        expect(screen.getByText('浏览首页')).toBeInTheDocument();
        expect(screen.getByText('搜索商品')).toBeInTheDocument();
        expect(screen.getByText('热门商品')).toBeInTheDocument();
    });

    it('renders smart nav card title', () => {
        render(
            <MemoryRouter>
                <NotFoundPage />
            </MemoryRouter>
        );
        expect(screen.getByText('智能导航')).toBeInTheDocument();
    });
});
