import { describe, it, expect, vi } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import { ProductCard } from './ProductCard';
import { createMockProduct } from '@/testUtils/factories';

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
  sellerName: '卖家小明',
  views: 50,
  favorites: 10,
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
    expect(screen.getByText('卖家小明')).toBeInTheDocument();
  });

  it('renders discount badge when originalPrice > price', () => {
    renderWithProviders(<ProductCard product={baseProduct} />);
    expect(screen.getByText(/-33%/)).toBeInTheDocument();
  });

  it('navigates to product detail on card click', async () => {
    renderWithProviders(<ProductCard product={baseProduct} />);
    const card = screen.getByLabelText('商品：测试商品标题');
    await userEvent.click(card);
    expect(mockNavigate).toHaveBeenCalledWith('/products/1');
  });

  it('calls onFavorite when favorite button is clicked', async () => {
    const onFavorite = vi.fn();
    renderWithProviders(
      <ProductCard product={baseProduct} onFavorite={onFavorite} />,
    );
    const favBtn = screen.getByLabelText('收藏');
    await userEvent.click(favBtn);
    expect(onFavorite).toHaveBeenCalledWith('1', true);
  });

  it('shows favorited state', () => {
    renderWithProviders(
      <ProductCard product={baseProduct} isFavorited={true} />,
    );
    expect(screen.getByLabelText('取消收藏')).toBeInTheDocument();
  });

  it('shows view count', () => {
    renderWithProviders(<ProductCard product={baseProduct} />);
    expect(screen.getByText(/50 浏览/)).toBeInTheDocument();
  });
});
