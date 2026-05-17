import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, fireEvent } from '@testing-library/react';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import { ProductManageDetailModal } from './ProductManageDetailModal';
import type { AdminProduct } from '../../types/admin';

// ─── Hook mocks ───
const mockUseAdminProductDetail = vi.fn();
const mockUseUpdateProductStatus = vi.fn();

vi.mock('../../hooks/useAdminProducts', () => ({
  useAdminProductDetail: (...args: unknown[]) => mockUseAdminProductDetail(...args),
  useUpdateProductStatus: (...args: unknown[]) => mockUseUpdateProductStatus(...args),
  ADMIN_PRODUCT_KEYS: { all: ['admin', 'products'] },
}));

// ─── Sample product ───
const onlineProduct: AdminProduct = {
  productId: 1,
  name: '测试商品-上架中',
  description: '这是一个已上架的商品描述',
  price: 100,
  originalPrice: 150,
  stock: 10,
  status: 1,
  statusDesc: '上架',
  conditionLevel: 9,
  location: '北京',
  contactMethod: '微信',
  images: ['https://example.com/img1.jpg', 'https://example.com/img2.jpg'],
  mainImage: null,
  categoryId: 1,
  categoryName: '电子产品',
  sellerId: 1,
  sellerName: '卖家A',
  sellerAvatar: null,
  viewCount: 100,
  createTime: '2026-05-16T10:00:00',
  updateTime: '2026-05-16T11:00:00',
};

const offlineProduct: AdminProduct = {
  productId: 2,
  name: '测试商品-已下架',
  description: null,
  price: 200,
  originalPrice: null,
  stock: 5,
  status: 3,
  statusDesc: '下架',
  conditionLevel: 8,
  location: null,
  contactMethod: null,
  images: [],
  mainImage: null,
  categoryId: 2,
  categoryName: '图书教材',
  sellerId: 2,
  sellerName: '卖家B',
  sellerAvatar: null,
  viewCount: 50,
  createTime: '2026-05-15T10:00:00',
  updateTime: '2026-05-15T12:00:00',
};

function setupDefaultMocks() {
  mockUseAdminProductDetail.mockReturnValue({
    data: onlineProduct,
    isLoading: false,
    refetch: vi.fn(),
  });

  mockUseUpdateProductStatus.mockReturnValue({
    mutateAsync: vi.fn().mockResolvedValue({}),
    isPending: false,
  });
}

describe('ProductManageDetailModal', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    setupDefaultMocks();
  });

  // ── Test 1: Does not render when open=false ──
  it('does not render when open is false', () => {
    const { container } = renderWithProviders(
      <ProductManageDetailModal
        open={false}
        productId={1}
        onClose={vi.fn()}
        onSuccess={vi.fn()}
      />,
    );
    // Modal returns null when open=false
    expect(container.innerHTML).toBe('');
  });

  // ── Test 2: Does not render when productId is null ──
  it('does not render when productId is null', () => {
    const { container } = renderWithProviders(
      <ProductManageDetailModal
        open={true}
        productId={null}
        onClose={vi.fn()}
        onSuccess={vi.fn()}
      />,
    );
    expect(container.innerHTML).toBe('');
  });

  // ── Test 3: Renders product detail when open=true with productId ──
  it('renders product detail modal when open is true and productId is set', () => {
    renderWithProviders(
      <ProductManageDetailModal
        open={true}
        productId={1}
        onClose={vi.fn()}
        onSuccess={vi.fn()}
      />,
    );
    expect(screen.getByText('商品详情')).toBeInTheDocument();
    expect(screen.getByText('测试商品-上架中')).toBeInTheDocument();
  });

  // ── Test 4: Shows loading state ──
  it('shows loading state while fetching product', () => {
    mockUseAdminProductDetail.mockReturnValue({
      data: undefined,
      isLoading: true,
      refetch: vi.fn(),
    });

    renderWithProviders(
      <ProductManageDetailModal
        open={true}
        productId={1}
        onClose={vi.fn()}
        onSuccess={vi.fn()}
      />,
    );
    expect(screen.getByText('加载中...')).toBeInTheDocument();
  });

  // ── Test 5: Shows "商品不存在" when product is null ──
  it('shows "商品不存在或已被删除" when product data is undefined after load', () => {
    mockUseAdminProductDetail.mockReturnValue({
      data: undefined,
      isLoading: false,
      refetch: vi.fn(),
    });

    renderWithProviders(
      <ProductManageDetailModal
        open={true}
        productId={999}
        onClose={vi.fn()}
        onSuccess={vi.fn()}
      />,
    );
    expect(
      screen.getByText('商品不存在或已被删除'),
    ).toBeInTheDocument();
  });

  // ── Test 6: Renders product info fields ──
  it('renders all product info fields', () => {
    renderWithProviders(
      <ProductManageDetailModal
        open={true}
        productId={1}
        onClose={vi.fn()}
        onSuccess={vi.fn()}
      />,
    );
    expect(screen.getByText('¥100.00')).toBeInTheDocument();
    expect(screen.getByText('9成新')).toBeInTheDocument();
    expect(screen.getByText('电子产品')).toBeInTheDocument();
    expect(screen.getByText('卖家A')).toBeInTheDocument();
    expect(screen.getByText('北京')).toBeInTheDocument();
  });

  // ── Test 7: "关闭" button calls onClose ──
  it('calls onClose when close button is clicked', () => {
    const onClose = vi.fn();
    renderWithProviders(
      <ProductManageDetailModal
        open={true}
        productId={1}
        onClose={onClose}
        onSuccess={vi.fn()}
      />,
    );
    const closeButton = screen.getByText('关闭');
    fireEvent.click(closeButton);
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  // ── Test 8: Shows "下架" action button for online product ──
  it('renders "下架" action button when product status is online (1)', () => {
    renderWithProviders(
      <ProductManageDetailModal
        open={true}
        productId={1}
        onClose={vi.fn()}
        onSuccess={vi.fn()}
      />,
    );
    expect(screen.getByText('下架')).toBeInTheDocument();
  });

  // ── Test 9: Shows "上架" action button for offline product ──
  it('renders "上架" action button when product status is offline (3)', () => {
    mockUseAdminProductDetail.mockReturnValue({
      data: offlineProduct,
      isLoading: false,
      refetch: vi.fn(),
    });

    renderWithProviders(
      <ProductManageDetailModal
        open={true}
        productId={2}
        onClose={vi.fn()}
        onSuccess={vi.fn()}
      />,
    );
    expect(screen.getByText('上架')).toBeInTheDocument();
  });

  // ── Test 10: No status action for sold product ──
  it('does not render action buttons when product status is sold (2)', () => {
    const soldProduct: AdminProduct = {
      ...onlineProduct,
      productId: 3,
      status: 2,
      statusDesc: '已售',
    };
    mockUseAdminProductDetail.mockReturnValue({
      data: soldProduct,
      isLoading: false,
      refetch: vi.fn(),
    });

    renderWithProviders(
      <ProductManageDetailModal
        open={true}
        productId={3}
        onClose={vi.fn()}
        onSuccess={vi.fn()}
      />,
    );
    // The footer with action buttons is only rendered when actions.length > 0
    // Sold products have no available actions
    expect(screen.queryByText('下架')).not.toBeInTheDocument();
    expect(screen.queryByText('上架')).not.toBeInTheDocument();
  });

  // ── Test 11: Clicking action button calls mutateAsync ──
  it('calls mutateAsync with correct params when action button is clicked', () => {
    const mutateAsync = vi.fn().mockResolvedValue({});
    mockUseUpdateProductStatus.mockReturnValue({
      mutateAsync,
      isPending: false,
    });

    renderWithProviders(
      <ProductManageDetailModal
        open={true}
        productId={1}
        onClose={vi.fn()}
        onSuccess={vi.fn()}
      />,
    );
    const offlineButton = screen.getByText('下架');
    fireEvent.click(offlineButton);

    expect(mutateAsync).toHaveBeenCalledWith({
      id: 1,
      data: { status: 3 },
    });
  });

  // ── Test 12: Action button disabled while mutation pending ──
  it('disables action buttons when mutation is pending', () => {
    mockUseUpdateProductStatus.mockReturnValue({
      mutateAsync: vi.fn(),
      isPending: true,
    });

    renderWithProviders(
      <ProductManageDetailModal
        open={true}
        productId={1}
        onClose={vi.fn()}
        onSuccess={vi.fn()}
      />,
    );
    const offlineButton = screen.getByText('下架');
    expect(offlineButton).toBeDisabled();
  });

  // ── Test 13: Escape key calls onClose ──
  it('calls onClose when Escape key is pressed', () => {
    const onClose = vi.fn();
    renderWithProviders(
      <ProductManageDetailModal
        open={true}
        productId={1}
        onClose={onClose}
        onSuccess={vi.fn()}
      />,
    );
    fireEvent.keyDown(document, { key: 'Escape' });
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  // ── Test 14: Renders original price when it exists ──
  it('renders original price strikethrough when originalPrice > price', () => {
    renderWithProviders(
      <ProductManageDetailModal
        open={true}
        productId={1}
        onClose={vi.fn()}
        onSuccess={vi.fn()}
      />,
    );
    expect(screen.getByText('¥150.00')).toBeInTheDocument();
  });

  // ── Test 15: Renders description when present ──
  it('renders product description section', () => {
    renderWithProviders(
      <ProductManageDetailModal
        open={true}
        productId={1}
        onClose={vi.fn()}
        onSuccess={vi.fn()}
      />,
    );
    expect(screen.getByText('商品描述')).toBeInTheDocument();
    expect(
      screen.getByText('这是一个已上架的商品描述'),
    ).toBeInTheDocument();
  });

  // ── Test 16: Renders current status display ──
  it('displays current product status', () => {
    renderWithProviders(
      <ProductManageDetailModal
        open={true}
        productId={1}
        onClose={vi.fn()}
        onSuccess={vi.fn()}
      />,
    );
    expect(screen.getByText('当前状态：')).toBeInTheDocument();
    expect(screen.getByText('上架')).toBeInTheDocument();
  });
});
