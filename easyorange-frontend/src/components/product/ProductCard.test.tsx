import { fireEvent, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { createMockProduct } from '@/testUtils/factories';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import { ProductCard } from './ProductCard';

// Mock Image component to avoid canvas API dependency in jsdom
vi.mock('@/components/ui/Image', () => ({
    Image: ({ src, alt, className, style, ...props }: React.ImgHTMLAttributes<HTMLImageElement>) => (
        <img src={src} alt={alt} className={className} style={style} data-mocked="true" {...props} />
    ),
    preloadImage: vi.fn(),
    preloadImages: vi.fn(),
    clearImageCache: vi.fn(),
    buildThumbnailUrl: vi.fn(),
    buildResponsiveUrl: vi.fn(),
    buildViewUrl: vi.fn(),
}));

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom');
    return {
        ...actual,
        useNavigate: () => mockNavigate,
    };
});

const baseProduct = createMockProduct({
    id: '1',
    title: '测试商品标题',
    price: 100,
    originalPrice: 150,
    condition: 1,
    location: '北京',
    sellerName: '资产方小明',
    views: 50,
    images: ['https://example.com/image.jpg'],
    categoryName: '电子产品',
});

describe('ProductCard', () => {
    it('renders product title', () => {
        renderWithProviders(<ProductCard product={baseProduct} />);
        expect(screen.getByText('测试商品标题')).toBeInTheDocument();
    });

    it('renders seller name', () => {
        renderWithProviders(<ProductCard product={baseProduct} />);
        expect(screen.getByText('资产方小明')).toBeInTheDocument();
    });

    it('renders discount badge when originalPrice > price', () => {
        renderWithProviders(<ProductCard product={baseProduct} />);
        expect(screen.getByText(/-33%/)).toBeInTheDocument();
    });

    it('renders the title as a link to the product detail', () => {
        renderWithProviders(<ProductCard product={baseProduct} />);
        const link = screen.getByRole('link', { name: '测试商品标题' });
        expect(link).toHaveAttribute('href', '/products/1');
    });

    it('does not nest interactive elements inside a button role', () => {
        const { container } = renderWithProviders(<ProductCard product={baseProduct} />);
        const card = container.querySelector('.product-card-premium');
        expect(card).not.toHaveAttribute('role', 'button');
        expect(card).not.toHaveAttribute('tabindex');
    });

    it('shows view count', () => {
        renderWithProviders(<ProductCard product={baseProduct} />);
        expect(screen.getByText(/50 浏览/)).toBeInTheDocument();
    });

    describe('variant="compact"', () => {
        const twoImageProduct = createMockProduct({
            ...baseProduct,
            images: ['https://example.com/1.jpg', 'https://example.com/2.jpg'],
            description: '一段商品描述',
        });

        it('根元素带 compact modifier', () => {
            const { container } = renderWithProviders(<ProductCard product={baseProduct} variant="compact" />);
            expect(container.querySelector('.product-card-premium--compact')).toBeInTheDocument();
        });

        it('保留标题链接与价格', () => {
            const { container } = renderWithProviders(<ProductCard product={baseProduct} variant="compact" />);
            const link = screen.getByRole('link', { name: '测试商品标题' });
            expect(link).toHaveAttribute('href', '/products/1');
            expect(container.querySelector('.price-current-premium')).toHaveTextContent('¥100');
            expect(container.querySelector('.price-original-premium')).toHaveTextContent('¥150');
        });

        it('不渲染第二张图、徽标、操作按钮、图片价格标签、描述、浏览量', () => {
            const { container } = renderWithProviders(<ProductCard product={twoImageProduct} variant="compact" />);
            expect(container.querySelector('.secondary-img')).toBeNull();
            expect(container.querySelector('.product-badges-premium')).toBeNull();
            expect(container.querySelector('.product-actions-premium')).toBeNull();
            expect(container.querySelector('.product-image-price-tag')).toBeNull();
            expect(container.querySelector('.product-desc-expand')).toBeNull();
            expect(container.querySelector('.product-stat-row-premium')).toBeNull();
            expect(container.querySelector('.product-card-shimmer')).toBeNull();
        });

        it('不绑定 3D 倾斜的鼠标事件', () => {
            const { container } = renderWithProviders(<ProductCard product={baseProduct} variant="compact" />);
            const figure = container.querySelector('.product-image-premium');
            expect(figure?.getAttribute('onmousemove')).toBeNull();
            fireEvent.mouseMove(figure as Element, { clientX: 10, clientY: 10 });
            expect(container.querySelector('.product-card-premium')).not.toHaveAttribute('data-tilt-active');
        });
    });
});
