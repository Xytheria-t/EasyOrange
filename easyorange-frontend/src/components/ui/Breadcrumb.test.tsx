import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import {
    Breadcrumb,
    BreadcrumbItem,
    BreadcrumbLink,
    BreadcrumbList,
    BreadcrumbPage,
    BreadcrumbSeparator,
} from './Breadcrumb';

function renderBreadcrumb() {
    return render(
        <MemoryRouter>
            <Breadcrumb>
                <BreadcrumbList>
                    <BreadcrumbItem>
                        <BreadcrumbLink to="/products">商城</BreadcrumbLink>
                    </BreadcrumbItem>
                    <BreadcrumbSeparator />
                    <BreadcrumbItem>
                        <BreadcrumbLink to="/orders">订单</BreadcrumbLink>
                    </BreadcrumbItem>
                    <BreadcrumbSeparator />
                    <BreadcrumbItem>
                        <BreadcrumbPage>订单详情</BreadcrumbPage>
                    </BreadcrumbItem>
                </BreadcrumbList>
            </Breadcrumb>
        </MemoryRouter>
    );
}

describe('Breadcrumb', () => {
    it('renders nav with accessible label', () => {
        renderBreadcrumb();
        expect(screen.getByRole('navigation', { name: '面包屑' })).toBeInTheDocument();
    });

    it('renders items as an ordered list', () => {
        const { container } = renderBreadcrumb();
        expect(container.querySelector('ol.bc-list')).toBeInTheDocument();
        expect(screen.getByText('商城').tagName).toBe('A');
    });

    it('marks the current page with aria-current', () => {
        renderBreadcrumb();
        expect(screen.getByText('订单详情')).toHaveAttribute('aria-current', 'page');
    });

    it('exposes separator to neither a11y tree nor tab order', () => {
        const { container } = renderBreadcrumb();
        const seps = container.querySelectorAll('li[aria-hidden="true"]');
        expect(seps).toHaveLength(2);
        expect(seps[0]).toHaveAttribute('role', 'presentation');
    });

    it('renders router links as anchors with target href', () => {
        renderBreadcrumb();
        // Link 的默认跳转由 MemoryRouter 承载，这里只验渲染产物是可点锚点
        const link = screen.getByText('商城');
        expect(link).toHaveAttribute('href', '/products');
        expect(link).toHaveClass('bc-link');
    });

    it('renders custom separator content', () => {
        render(
            <MemoryRouter>
                <Breadcrumb>
                    <BreadcrumbList>
                        <BreadcrumbItem>
                            <BreadcrumbLink to="/a">A</BreadcrumbLink>
                        </BreadcrumbItem>
                        <BreadcrumbSeparator>/</BreadcrumbSeparator>
                        <BreadcrumbItem>
                            <BreadcrumbPage>B</BreadcrumbPage>
                        </BreadcrumbItem>
                    </BreadcrumbList>
                </Breadcrumb>
            </MemoryRouter>
        );
        expect(screen.getByText('/')).toBeInTheDocument();
    });

    it('external links render as plain anchors', () => {
        render(
            <MemoryRouter>
                <Breadcrumb>
                    <BreadcrumbList>
                        <BreadcrumbItem>
                            <BreadcrumbLink asExternal href="https://example.com">
                                外链
                            </BreadcrumbLink>
                        </BreadcrumbItem>
                    </BreadcrumbList>
                </Breadcrumb>
            </MemoryRouter>
        );
        const anchor = screen.getByText('外链');
        expect(anchor.tagName).toBe('A');
        expect(anchor).toHaveAttribute('href', 'https://example.com');
    });
});
