import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import CategoriesSection from './CategoriesSection';

// Mock hooks
const mockUseCategories = vi.fn();
vi.mock('@/hooks', () => ({
  useCategories: () => mockUseCategories(),
  useScrollReveal: vi.fn(),
}));

const mockCategories = [
  { id: '1', name: '电子数码', productCount: 15 },
  { id: '2', name: '书籍教材', productCount: 23 },
  { id: '3', name: '服饰鞋包', productCount: 0 },
  { id: '4', name: '生活用品', productCount: 8 },
  { id: '5', name: '运动健身', productCount: 12 },
  { id: '6', name: '虚拟物品', productCount: 5 },
];

describe('CategoriesSection', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUseCategories.mockReturnValue({ data: mockCategories });
  });

  it('renders section header', () => {
    render(<CategoriesSection />);
    expect(screen.getByText('探索分类')).toBeInTheDocument();
    expect(screen.getByText('发现你需要的')).toBeInTheDocument();
    expect(screen.getByText('精选热门品类，快速找到心仪好物')).toBeInTheDocument();
  });

  it('renders all category cards', () => {
    render(<CategoriesSection />);
    expect(screen.getByText('电子数码')).toBeInTheDocument();
    expect(screen.getByText('书籍教材')).toBeInTheDocument();
    expect(screen.getByText('服饰鞋包')).toBeInTheDocument();
    expect(screen.getByText('生活用品')).toBeInTheDocument();
    expect(screen.getByText('运动健身')).toBeInTheDocument();
    expect(screen.getByText('虚拟物品')).toBeInTheDocument();
  });

  it('renders product counts for categories with items', () => {
    render(<CategoriesSection />);
    expect(screen.getByText('15+ 件商品')).toBeInTheDocument();
    expect(screen.getByText('23+ 件商品')).toBeInTheDocument();
    expect(screen.getByText('8+ 件商品')).toBeInTheDocument();
  });

  it('renders "暂无商品" for categories with zero products', () => {
    render(<CategoriesSection />);
    expect(screen.getByText('暂无商品 件商品')).toBeInTheDocument();
  });

  it('renders "更多分类" card at the end', () => {
    render(<CategoriesSection />);
    expect(screen.getByText('更多分类')).toBeInTheDocument();
    expect(screen.getByText('探索全部分类')).toBeInTheDocument();
    expect(screen.getByText('查看全部 → 件商品')).toBeInTheDocument();
  });

  it('renders category descriptions', () => {
    render(<CategoriesSection />);
    expect(screen.getByText('手机电脑 · 数码配件 · 智能设备')).toBeInTheDocument();
    expect(screen.getByText('专业课本 · 考研资料 · 课外读物')).toBeInTheDocument();
    expect(screen.getByText('潮流服饰 · 品牌鞋履 · 箱包配饰')).toBeInTheDocument();
  });

  it('renders empty state when no categories', () => {
    mockUseCategories.mockReturnValue({ data: [] });
    render(<CategoriesSection />);
    expect(screen.queryByText('电子数码')).not.toBeInTheDocument();
    // "更多分类" should still appear since it's added separately
    expect(screen.getByText('更多分类')).toBeInTheDocument();
  });

  it('renders undefined data gracefully', () => {
    mockUseCategories.mockReturnValue({ data: undefined });
    render(<CategoriesSection />);
    expect(screen.getByText('更多分类')).toBeInTheDocument();
  });

  it('links categories to correct URLs', () => {
    render(<CategoriesSection />);
    const electronicLink = screen.getByText('电子数码').closest('a');
    expect(electronicLink).toHaveAttribute('href', '/products?category=1');
  });

  it('links "更多分类" to /products', () => {
    render(<CategoriesSection />);
    const moreLink = screen.getByText('更多分类').closest('a');
    expect(moreLink).toHaveAttribute('href', '/products');
  });
});
