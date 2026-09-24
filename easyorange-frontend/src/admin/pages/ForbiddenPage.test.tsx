import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import ForbiddenPage from './ForbiddenPage';

describe('ForbiddenPage', () => {
    // ── Test 1: Renders 403 text ──
    it('renders 403 error code', () => {
        renderWithProviders(<ForbiddenPage />);
        expect(screen.getByText('403')).toBeInTheDocument();
    });

    // ── Test 2: Renders title ──
    it('renders "访问受限" title', () => {
        renderWithProviders(<ForbiddenPage />);
        expect(screen.getByText('访问受限')).toBeInTheDocument();
    });

    // ── Test 3: Renders description about no permission ──
    it('renders permission description with an actionable next step', () => {
        renderWithProviders(<ForbiddenPage />);
        expect(screen.getByText(/抱歉，您没有权限访问此页面/)).toBeInTheDocument();
        expect(screen.getByText(/请改用管理员账号登录/)).toBeInTheDocument();
    });

    // ── Test 4: Renders "返回主站" link ──
    it('renders "返回主站" link pointing to "/"', () => {
        renderWithProviders(<ForbiddenPage />);
        const link = screen.getByText('返回主站');
        expect(link).toBeInTheDocument();
        expect(link.closest('a')).toHaveAttribute('href', '/');
    });

    // ── Test 5: 权限被拒时必须给出第二条出路，而不是只让用户干等 ──
    it('offers a re-login path to "/login"', () => {
        renderWithProviders(<ForbiddenPage />);
        const link = screen.getByText('换个账号登录');
        expect(link.closest('a')).toHaveAttribute('href', '/login');
    });

    // ── Test 5: Renders shield icon (SVG) ──
    it('renders shield icon SVG', () => {
        renderWithProviders(<ForbiddenPage />);
        // The shield icon is an SVG with a shield path
        const svg = document.querySelector('svg');
        expect(svg).toBeInTheDocument();
    });

    // ── Test 6: Has background atmosphere divs ──
    it('renders within a full-height container', () => {
        const { container } = renderWithProviders(<ForbiddenPage />);
        const mainDiv = container.firstElementChild as HTMLElement;
        expect(mainDiv.style.minHeight).toBe('100vh');
    });
});
