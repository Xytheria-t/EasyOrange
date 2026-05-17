import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import FavoritesPage from './FavoritesPage';

const mockNavigate = vi.hoisted(() => vi.fn());
const mockAddToast = vi.hoisted(() => vi.fn());
const mockGetList = vi.hoisted(() => vi.fn());
const mockRemove = vi.hoisted(() => vi.fn());
const mockRemoveMany = vi.hoisted(() => vi.fn());

vi.mock('@/api/favoriteApi', () => ({
  favoriteApi: { getList: mockGetList, remove: mockRemove, removeMany: mockRemoveMany },
}));

vi.mock('@/store/uiStore', () => ({
  useUIStore: vi.fn((sel) => {
    const s = { addToast: mockAddToast };
    return sel ? sel(s) : s;
  }),
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...(actual as object), useNavigate: () => mockNavigate };
});

vi.mock('@/components/ui/Image', () => ({
  Image: ({ alt, ...props }) => <img alt={alt || ''} {...props} />,
}));

function createMockPage(recordsCount = 1, totalOverride?: number) {
  const total = totalOverride ?? recordsCount;
  const records = Array.from({ length: recordsCount }, (_, i) => ({
    id: `fav${i}`,
    productId: `prod${i}`,
    createTime: '2026-05-15T10:00:00Z',
    product: {
      id: `prod${i}`,
      sellerId: 'seller1',
      username: '卖家张三',
      userAvatar: null,
      categoryId: 1,
      categoryName: '电子产品',
      title: `测试商品${i}`,
      description: '描述',
      price: 99.99,
      originalPrice: 150,
      stock: 1,
      status: 1,
      statusDesc: null,
      views: 250,
      condition: 1,
      conditionDesc: null,
      location: '北京海淀',
      contactMethod: null,
      images: ['https://example.com/img.jpg'],
      mainImageUrl: null,
      createTime: '2026-05-10T10:00:00Z',
      updateTime: '2026-05-10T10:00:00Z',
    },
  }));
  return { records, total, size: 20, current: 1, pages: Math.ceil(total / 20) };
}

function renderPage() {
  return renderWithProviders(<FavoritesPage />);
}

beforeEach(() => {
  vi.clearAllMocks();
});

describe('FavoritesPage', () => {
  it('renders loading state', () => {
    mockGetList.mockReturnValue(new Promise(() => {}));
    renderPage();
    expect(screen.getByText('正在加载你的收藏...')).toBeInTheDocument();
  });

  it('renders error state', async () => {
    mockGetList.mockRejectedValue(new Error('fail'));
    renderPage();
    expect(await screen.findByText('加载失败')).toBeInTheDocument();
    expect(screen.getByText('重新加载')).toBeInTheDocument();
  });

  it('renders empty state', async () => {
    mockGetList.mockResolvedValue({ data: createMockPage(0) });
    renderPage();
    expect(await screen.findByText('收藏夹空空如也')).toBeInTheDocument();
    expect(screen.getByText('探索好物')).toBeInTheDocument();
  });

  it('renders favorites list with items', async () => {
    mockGetList.mockResolvedValue({ data: createMockPage(1) });
    renderPage();
    expect(await screen.findByText('测试商品0')).toBeInTheDocument();
    expect(screen.getByText('电子产品')).toBeInTheDocument();
    expect(screen.getByText('¥99.99')).toBeInTheDocument();
    expect(screen.getByText('¥150')).toBeInTheDocument();
    expect(screen.getByText('1')).toBeInTheDocument(); // count badge
  });

  it('selects item and batch removes', async () => {
    mockGetList.mockResolvedValue({ data: createMockPage(2) });
    mockRemoveMany.mockResolvedValue({});
    renderPage();
    await screen.findByText('测试商品0');

    const user = userEvent.setup();
    const checkboxes = screen.getAllByLabelText('选择此收藏项');
    await user.click(checkboxes[0]);

    expect(screen.getByText('删除选中 (1)')).toBeInTheDocument();
    await user.click(screen.getByText('删除选中 (1)'));
    expect(mockRemoveMany).toHaveBeenCalledWith(['fav0']);
  });

  it('removes single item via heart button', async () => {
    mockGetList.mockResolvedValue({ data: createMockPage(1) });
    mockRemove.mockResolvedValue({});
    renderPage();
    await screen.findByText('测试商品0');

    const user = userEvent.setup();
    const heartBtn = document.querySelector('.fav-card-heart') as HTMLElement;
    await user.click(heartBtn);
    expect(mockRemove).toHaveBeenCalledWith('prod0');
  });

  it('shows pagination for multi-page results', async () => {
    mockGetList.mockResolvedValue({ data: createMockPage(20, 30) });
    renderPage();
    expect(await screen.findByText('1 / 2')).toBeInTheDocument();
    expect(screen.getByText('下一页')).not.toBeDisabled();
    expect(screen.getByText('上一页')).toBeDisabled();
  });
});
