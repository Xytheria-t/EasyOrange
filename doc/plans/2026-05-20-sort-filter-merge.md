# 排序与筛选合并 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use subagent-driven-development or executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 消除 ProductsPage 中排序功能的重复入口，将排序从 ToolsPlaza 移到专有 SortDropdown 组件，分离排序（互斥单选）和筛选（模式切换）职责。

**Architecture:**
- 新增 `SortDropdown` 组件放入 `src/components/search/`（与 FacetFilter 同级）
- 修改 `ToolsPlaza` 去掉排序相关按钮，只保留模式筛选
- 修改 `ProductsPage` 用 `SortDropdown` 替换 `.view-options` 按钮组
- 更新现有测试以匹配新行为

**Tech Stack:** TypeScript, React, Tailwind, Vitest, Testing Library

---

### Task 1: 新建 SortDropdown 组件

**Files:**
- Create: `easyorange-frontend/src/components/search/SortDropdown.tsx`
- Test: 暂无单独测试（在 Task 3 ProductsPage 测试中覆盖）

- [x] **Step 1: 创建 SortDropdown 组件**

```typescript
// src/components/search/SortDropdown.tsx
import { useState, useRef, useEffect } from 'react';
import { ChevronDown } from 'lucide-react';

export type SortOption = 'newest' | 'price_asc' | 'price_desc' | 'popular';

interface SortDropdownProps {
  value: SortOption;
  onChange: (value: SortOption) => void;
}

const SORT_OPTIONS: { value: SortOption; label: string }[] = [
  { value: 'newest', label: '最新发布' },
  { value: 'price_asc', label: '价格从低到高' },
  { value: 'price_desc', label: '价格从高到低' },
  { value: 'popular', label: '最受欢迎' },
];

export default function SortDropdown({ value, onChange }: SortDropdownProps) {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const currentLabel = SORT_OPTIONS.find(o => o.value === value)?.label ?? '排序方式';

  return (
    <div ref={ref} className="sort-dropdown relative">
      <button
        type="button"
        onClick={() => setOpen(!open)}
        className="sort-dropdown-trigger"
      >
        <span>{currentLabel}</span>
        <ChevronDown size={14} className={`sort-dropdown-arrow ${open ? 'rotate-180' : ''}`} />
      </button>
      {open && (
        <div className="sort-dropdown-panel">
          {SORT_OPTIONS.map(option => (
            <button
              key={option.value}
              type="button"
              className={`sort-dropdown-item ${value === option.value ? 'active' : ''}`}
              onClick={() => {
                onChange(option.value);
                setOpen(false);
              }}
            >
              <span>{option.label}</span>
              {value === option.value && (
                <span className="sort-dropdown-check">✓</span>
              )}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
```

- [x] **Step 2: 提交**

```bash
git add easyorange-frontend/src/components/search/SortDropdown.tsx
git commit -m "feat: add SortDropdown component for single-select sorting"
```

---

### Task 2: 更新 ToolsPlaza — 去掉排序相关按钮

**Files:**
- Modify: `easyorange-frontend/src/components/product/ToolsPlaza.tsx`
- Modify: `easyorange-frontend/src/components/product/ToolsPlaza.test.tsx`

- [x] **Step 1: 更新 ToolsPlaza 组件**

去掉"最新发布"和"热门商品"按钮，`activeFilter` 类型缩减为只追踪 `'all' | 'ai' | 'discount'`：

移除 `tools-plaza` className 中的 `#quickFilters` id（id 不再需要）。

```typescript
// ToolsPlaza.tsx — 修改部分

const [activeFilter, setActiveFilter] = useState<'all' | 'ai' | 'discount'>('all');

const handleFilterClick = (filter: 'all' | 'ai' | 'discount') => {
  if (filter === 'ai') {
    setAiMode(!aiMode);
    setActiveFilter('ai');
    onFilterChange?.('ai');
    return;
  }
  setActiveFilter(filter);
  setAiMode(false);
  onFilterChange?.(filter);
};
```

删除 JSX 中 `new` 和 `hot` 按钮的 `<button>` 元素（保留全部、AI推荐、特价优惠）：

移除的 JSX 代码块：
```tsx
{/* ❌ 删除 — 最新发布按钮 */}
<button
  className={`plaza-tool ${activeFilter === 'new' && !aiMode ? 'active' : ''}`}
  onClick={() => handleFilterClick('new')}
>
  ...
  <span className="tool-label">最新发布</span>
  ...
</button>

{/* ❌ 删除 — 热门商品按钮 */}
<button
  className={`plaza-tool ${activeFilter === 'hot' && !aiMode ? 'active' : ''}`}
  onClick={() => handleFilterClick('hot')}
>
  ...
  <span className="tool-label">热门商品</span>
  ...
</button>
```

- [x] **Step 2: 更新 ToolsPlaza 测试**

```typescript
// ToolsPlaza.test.tsx — 修改
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { ToolsPlaza } from './ToolsPlaza';

describe('ToolsPlaza', () => {
  it('renders brand title', () => {
    render(<ToolsPlaza />);
    expect(screen.getByText('筛选工具')).toBeInTheDocument();
  });

  it('shows total count', () => {
    render(<ToolsPlaza total={42} />);
    expect(screen.getByText('42 件商品')).toBeInTheDocument();
  });

  it('shows zero total by default', () => {
    render(<ToolsPlaza />);
    expect(screen.getByText('0 件商品')).toBeInTheDocument();
  });

  it('renders all filter buttons', () => {
    render(<ToolsPlaza />);
    expect(screen.getByText('全部')).toBeInTheDocument();
    expect(screen.getByText('特价优惠')).toBeInTheDocument();
    expect(screen.queryByText('最新发布')).not.toBeInTheDocument();
    expect(screen.queryByText('热门商品')).not.toBeInTheDocument();
  });

  it('renders AI推荐 button', () => {
    render(<ToolsPlaza />);
    expect(screen.getByText('AI推荐')).toBeInTheDocument();
  });

  it('calls onFilterChange with "all" when clicking 全部', () => {
    const onFilterChange = vi.fn();
    render(<ToolsPlaza onFilterChange={onFilterChange} />);
    fireEvent.click(screen.getByText('全部'));
    expect(onFilterChange).toHaveBeenCalledWith('all');
  });

  it('calls onFilterChange with "ai" when clicking AI button', () => {
    const onFilterChange = vi.fn();
    render(<ToolsPlaza onFilterChange={onFilterChange} />);
    fireEvent.click(screen.getByText('AI推荐'));
    expect(onFilterChange).toHaveBeenCalledWith('ai');
  });

  it('shows AI hint when AI mode is active', () => {
    render(<ToolsPlaza />);
    fireEvent.click(screen.getByText('AI推荐'));
    expect(screen.getByText(/AI正在根据您的浏览习惯/)).toBeInTheDocument();
  });

  it('hides AI hint when clicking regular filter after AI mode', () => {
    render(<ToolsPlaza />);
    fireEvent.click(screen.getByText('AI推荐'));
    expect(screen.getByText(/AI正在根据您的浏览习惯/)).toBeInTheDocument();
    fireEvent.click(screen.getByText('全部'));
    expect(screen.queryByText(/AI正在根据您的浏览习惯/)).not.toBeInTheDocument();
  });

  it('sets 全部 active by default', () => {
    render(<ToolsPlaza />);
    const allBtn = screen.getByText('全部').closest('button');
    expect(allBtn?.className).toContain('active');
  });

  it('toggles AI mode on second click', () => {
    const onFilterChange = vi.fn();
    render(<ToolsPlaza onFilterChange={onFilterChange} />);
    fireEvent.click(screen.getByText('AI推荐'));
    expect(onFilterChange).toHaveBeenCalledWith('ai');
    fireEvent.click(screen.getByText('AI推荐'));
    expect(onFilterChange).toHaveBeenCalledTimes(2);
  });
});
```

- [x] **Step 3: 运行 ToolsPlaza 测试确认通过**

```bash
cd easyorange-frontend && npx vitest run src/components/product/ToolsPlaza.test.tsx
```

Expected: All tests PASS

- [x] **Step 4: 提交**

```bash
git add easyorange-frontend/src/components/product/ToolsPlaza.tsx \
       easyorange-frontend/src/components/product/ToolsPlaza.test.tsx
git commit -m "refactor: remove sort buttons from ToolsPlaza, keep only filter/mode toggles"
```

---

### Task 3: 更新 ProductsPage — 接入 SortDropdown

**Files:**
- Modify: `easyorange-frontend/src/pages/products/ProductsPage.tsx`
- Modify: `easyorange-frontend/src/pages/products/ProductsPage.test.tsx`
- Modify: `easyorange-frontend/src/pages/products/products-premium.css`

- [x] **Step 1: 修改 ProductsPage 组件**

替换排序按钮区域为 SortDropdown，更新 `handleFilterChange` 语义：

```typescript
// ProductsPage.tsx — 修改顶部 imports
import SortDropdown from '@/components/search/SortDropdown';
import type { SortOption } from '@/components/search/SortDropdown';
```

删除现有的 `sortOptions` 常量（第 173-178 行）：
```typescript
// ❌ 删除
const sortOptions: { value: NonNullable<ProductQueryParams['sort']>; label: string }[] = [
  { value: 'newest', label: '最新发布' },
  { value: 'price_asc', label: '价格从低到高' },
  { value: 'price_desc', label: '价格从高到低' },
  { value: 'popular', label: '最受欢迎' },
];
```

删除现有 `handleSortChange`（第 180-183 行），替换为新版本：
```typescript
const handleSortChange = useCallback((sort: SortOption) => {
  resetAllProducts();
  setParams((prev) => ({ ...prev, sort, pageNum: 1 }));
}, [resetAllProducts]);
```

更新 `handleFilterChange` — 支持特价优惠前端过滤：
```typescript
const handleFilterChange = useCallback((filter: ToolsPlazaFilter) => {
  setActiveFilter(filter);
  if (filter === 'all') {
    resetAllProducts();
    setParams(prev => ({ ...prev, sort: 'newest', pageNum: 1 }));
  } else if (filter === 'discount') {
    resetAllProducts();
    setParams(prev => ({ ...prev, pageNum: 1 }));
  }
}, [resetAllProducts]);
```

新增 `displayProducts` 和 `displayTotal` 使用 useMemo 实现特价优惠前端过滤：
```typescript
const displayProducts = useMemo(() => {
  const baseProducts = isSemanticMode ? semanticResults : allProducts;
  if (activeFilter === 'discount') {
    return baseProducts.filter((p: Product) => 
      p.originalPrice !== null && 
      p.originalPrice !== undefined && 
      p.originalPrice > p.price
    );
  }
  return baseProducts;
}, [isSemanticMode, semanticResults, allProducts, activeFilter]);

const displayTotal = useMemo(() => {
  if (activeFilter === 'discount') {
    return displayProducts.length;
  }
  return total;
}, [activeFilter, displayProducts.length, total]);
```

替换 JSX 中的 `.view-options` 区域（第 306-316 行）：

原代码：
```tsx
<div className="view-options">
  {sortOptions.map((option) => (
    <button
      key={option.value}
      onClick={() => handleSortChange(option.value)}
      className={`view-btn ${params.sort === option.value ? 'active' : ''}`}
    >
      {option.label}
    </button>
  ))}
</div>
```

改为：
```tsx
<SortDropdown
  value={(params.sort ?? 'newest') as SortOption}
  onChange={handleSortChange}
/>
```

- [x] **Step 2: 更新 ProductsPage 测试**

```typescript
// ProductsPage.test.tsx — 更新测试
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ProductsPage from './ProductsPage';

const mockUseProducts = vi.hoisted(() => vi.fn());
const mockUseCategories = vi.hoisted(() => vi.fn());
const mockUseFavoriteCheck = vi.hoisted(() => vi.fn());
const mockUseColumnCount = vi.hoisted(() => vi.fn());
const mockUseAuthStore = vi.hoisted(() =>
  vi.fn(() => ({ user: null, token: null, isAuthenticated: false })),
);
const mockNavigate = vi.hoisted(() => vi.fn());

const mockUseSemanticSearch = vi.hoisted(() => vi.fn(() => ({
  results: [],
  isSearching: false,
  isSemanticMode: false,
  total: 0,
  search: vi.fn(),
  toggleSemanticMode: vi.fn(),
  setResults: vi.fn(),
})));

vi.mock('@/hooks', () => ({
  useProducts: mockUseProducts,
  useCategories: mockUseCategories,
  useFavoriteCheck: mockUseFavoriteCheck,
  useColumnCount: mockUseColumnCount,
  useSemanticSearch: mockUseSemanticSearch,
}));

vi.mock('@/store/authStore', () => ({
  useAuthStore: mockUseAuthStore,
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...(actual as object),
    useNavigate: () => mockNavigate,
  };
});

vi.mock('@/api/favoriteApi', () => ({
  favoriteApi: {
    batchCheck: vi.fn().mockResolvedValue({ data: {} }),
    add: vi.fn().mockResolvedValue({}),
    remove: vi.fn().mockResolvedValue({}),
  },
}));

vi.mock('@/api/productApi', () => ({
  productApi: {
    getProducts: vi.fn().mockResolvedValue({ data: { records: [], total: 0 } }),
  },
}));

vi.mock('@tanstack/react-virtual', () => ({
  useVirtualizer: vi.fn(() => ({
    getVirtualItems: vi.fn(() => []),
    getTotalSize: vi.fn(() => 0),
  })),
}));

HTMLCanvasElement.prototype.toDataURL = vi
  .fn()
  .mockImplementation(() => 'data:image/png;base64,test');

function renderPage() {
  return renderWithProviders(<ProductsPage />, {
    initialRoute: '/products',
  });
}

beforeEach(() => {
  vi.clearAllMocks();
  HTMLCanvasElement.prototype.toDataURL = vi.fn(() => 'data:image/png;base64,test');
  mockUseColumnCount.mockReturnValue(4);
  mockUseFavoriteCheck.mockReturnValue({
    checkFavorites: vi.fn(),
    isFavorited: vi.fn(() => false),
    toggleFavorite: vi.fn(),
  });
  mockUseCategories.mockReturnValue({
    data: [
      { id: '1', name: '电子产品' },
      { id: '2', name: '服装鞋帽' },
    ],
    isLoading: false,
  });
  mockUseAuthStore.mockImplementation(() => ({
    user: null,
    token: null,
    isAuthenticated: false,
  }));
  mockUseProducts.mockReturnValue({
    data: { records: [], total: 0 },
    isLoading: false,
  });
});

describe('ProductsPage', () => {
  it('renders loading skeleton when initially loading', () => {
    mockUseProducts.mockReturnValue({
      data: undefined,
      isLoading: true,
    });

    renderPage();

    const skeletonLines = document.querySelectorAll('.skeleton-line');
    expect(skeletonLines.length).toBeGreaterThan(0);
    const loadingCards = document.querySelectorAll('.product-card-loading-premium');
    expect(loadingCards.length).toBe(10);
  });

  it('renders empty state when no products found', () => {
    renderPage();

    expect(screen.getByText('未找到相关商品')).toBeInTheDocument();
    expect(
      screen.getByText('尝试调整筛选条件或搜索其他关键词'),
    ).toBeInTheDocument();
  });

  it('renders SortDropdown and filter button', () => {
    renderPage();

    // SortDropdown 触发按钮显示当前排序（默认最新发布）
    expect(screen.getByText('最新发布')).toBeInTheDocument();
    // 筛选按钮
    expect(screen.getByText('筛选')).toBeInTheDocument();
  });

  it('renders ToolsPlaza with total count', () => {
    mockUseProducts.mockReturnValue({
      data: { records: [], total: 42 },
      isLoading: false,
    });

    renderPage();

    expect(screen.getByText('42 件商品')).toBeInTheDocument();
  });

  it('renders result count', () => {
    mockUseProducts.mockReturnValue({
      data: { records: [], total: 42 },
      isLoading: false,
    });

    renderPage();

    const countEl = screen.getByText('件商品');
    expect(countEl).toBeInTheDocument();
  });

  it('shows category filter chip when categoryId is in params', () => {
    mockUseProducts.mockReturnValue({
      data: { records: [], total: 0 },
      isLoading: false,
    });

    renderWithProviders(<ProductsPage />, {
      initialRoute: '/products?category=1',
    });

    const catElements = screen.getAllByText('电子产品');
    expect(catElements.length).toBeGreaterThanOrEqual(1);
  });

  it('clears category filter when X is clicked', async () => {
    mockUseProducts.mockReturnValue({
      data: { records: [], total: 0 },
      isLoading: false,
    });

    renderWithProviders(<ProductsPage />, {
      initialRoute: '/products?category=1',
    });

    const user = userEvent.setup();
    const clearBtn = document.querySelector('.results-category') as HTMLElement;
    expect(clearBtn).toBeInTheDocument();
    await user.click(clearBtn);
  });

  it('shows default sort as newest in SortDropdown', () => {
    renderPage();

    // SortDropdown trigger shows "最新发布" by default
    expect(screen.getByText('最新发布')).toBeInTheDocument();
  });

  it('opens SortDropdown panel and changes sort on selection', async () => {
    renderPage();

    const user = userEvent.setup();
    // Click the dropdown trigger
    const trigger = screen.getByText('最新发布').closest('button') as HTMLElement;
    await user.click(trigger);

    // Panel should show all options
    const priceAscOption = screen.getByText('价格从低到高');
    expect(priceAscOption).toBeInTheDocument();

    // Click price_asc option
    await user.click(priceAscOption);

    // Trigger should update to new value
    expect(trigger).toHaveTextContent('价格从低到高');
  });
});
```

- [x] **Step 3: 添加 SortDropdown CSS 到 products-premium.css**

在 products-premium.css 末尾添加样式：

```css
/* SortDropdown Component */
.sort-dropdown {
  position: relative;
  display: inline-block;
}

.sort-dropdown-trigger {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  color: #475569;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.sort-dropdown-trigger:hover {
  background: #f1f5f9;
  border-color: #cbd5e1;
}

.sort-dropdown-arrow {
  transition: transform 0.2s ease;
  color: #94a3b8;
}

.sort-dropdown-arrow.rotate-180 {
  transform: rotate(180deg);
}

.sort-dropdown-panel {
  position: absolute;
  top: calc(100% + 4px);
  right: 0;
  min-width: 160px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08), 0 1px 4px rgba(0, 0, 0, 0.04);
  z-index: 50;
  padding: 4px;
  animation: sortDropdownFadeIn 0.15s ease;
}

@keyframes sortDropdownFadeIn {
  from {
    opacity: 0;
    transform: translateY(-4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.sort-dropdown-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 8px 12px;
  border: none;
  background: transparent;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s ease;
  text-align: left;
}

.sort-dropdown-item:hover {
  background: #f1f5f9;
  color: #f97316;
}

.sort-dropdown-item.active {
  background: #fff7ed;
  color: #f97316;
}

.sort-dropdown-check {
  font-size: 12px;
  color: #f97316;
}
```

- [x] **Step 4: 运行 ProductsPage 测试确认通过**

```bash
cd easyorange-frontend && npx vitest run src/pages/products/ProductsPage.test.tsx
```

Expected: All tests PASS

- [x] **Step 5: 提交**

```bash
git add easyorange-frontend/src/pages/products/ProductsPage.tsx \
       easyorange-frontend/src/pages/products/ProductsPage.test.tsx \
       easyorange-frontend/src/pages/products/products-premium.css
git commit -m "refactor: replace sort buttons with SortDropdown in ProductsPage toolbar"
```

---

### Task 4: 全面验证

- [x] **Step 1: 运行所有前端测试**

```bash
cd easyorange-frontend && npm test
```

Expected: All tests pass (no regressions)

- [x] **Step 2: 确认无 TypeScript 错误**

```bash
cd easyorange-frontend && npx tsc --noEmit
```

Expected: No errors

- [x] **Step 3: 最终提交**

```bash
git add -A
git commit -m "feat: merge sort and filter - SortDropdown replaces duplicate sort buttons, ToolsPlaza simplified"
```

---

## 验证清单

- [x] ToolsPlaza 不再显示"最新发布"和"热门商品"
- [x] Toolbar 显示 SortDropdown 而非 4 个并排按钮
- [x] 点击 SortDropdown 触发按钮显示选项面板
- [x] 选择排序选项后触发 onChange，更新商品列表
- [x] 点击面板外关闭 SortDropdown
- [x] 特价优惠按钮通过前端过滤 `originalPrice > price` 实现
- [x] 全部测试通过
- [x] 无 TypeScript 错误
