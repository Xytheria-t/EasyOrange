# 排序与筛选合并设计

> 解决 ProductsPage 中 ToolsPlaza (#quickFilters) 和 Toolbar (.products-toolbar) 之间排序功能冲突的问题。

## 问题分析

ProductsPage 现有两个与控制排序相关的 UI 区域：

| 区域 | 位置 | 现有按钮 |
|------|------|---------|
| **ToolsPlaza** (`#quickFilters`) | 商品列表上方品牌区域 | AI推荐 · 全部 · **最新发布** · **热门商品** · 特价优惠 |
| **Toolbar** (`.products-toolbar`) | 商品列表顶部操作栏 | **最新发布** · **价格从低到高** · **价格从高到低** · **最受欢迎** |

**冲突原因：**
- "最新发布" 同时出现在两个区域
- "热门商品" (ToolsPlaza) ≈ "最受欢迎" (Toolbar) 但标签不同
- ToolsPlaza 的按钮被映射为排序值，和 Toolbar 的排序按钮操作相同功能
- 排序是**互斥单选**，但两个入口让用户误以为可以组合选择

## 设计目标

1. 消除排序功能的重复入口 — 每个排序选项只出现一次
2. 明确区分"排序"（互斥单选）和"筛选"（可组合模式）
3. 最小化 UI 占用，保持视觉一致性

## 方案：清理 + 合并

### ToolsPlaza 改造

去掉排序相关的按钮（最新发布、热门商品），只保留真正的模式/筛选按钮：

```
[AI推荐] [全部] [特价优惠]
```

| 按钮 | 语义 | 操作 |
|------|------|------|
| AI推荐 | AI 语义搜索模式 | 切换 `isSemanticMode`（已有逻辑） |
| 全部 | 重置所有筛选 | 清筛选 + 排序回 newest + 页码 1 |
| 特价优惠 | 折扣商品筛选 | 前端过滤 `originalPrice > price` 的商品 |

### Toolbar 排序改为 Dropdown

将 4 个并排按钮改为排序下拉选择器，明确"互斥单选"语义：

```
┌──────────────────────────────────────────────────────────┐
│  分类：电子产品              42 件商品                     │
│                              [🔍 筛选]                    │
│                        排序方式：[最新发布 ▾]             │
│                                   ├ 最新发布              │
│                                   ├ 价格从低到高          │
│                                   ├ 价格从高到低          │
│                                   └ 最受欢迎              │
└──────────────────────────────────────────────────────────┘
```

**后端支持的 sort 值**（来自 `ProductQueryRequest` 正则校验）：
`default | price_asc | price_desc | newest | view | popular`

前端目前使用 subset：`newest | price_asc | price_desc | popular`

Dropdown 默认显示当前选中项，选中后即时刷新列表并关闭面板。

### 数据流

```
ToolsPlaza (filter/mode)
  │
  ├─ 'all'      → resetAllProducts + params.sort='newest' + clear filters
  ├─ 'ai'       → toggleSemanticMode()
  └─ 'discount' → setActiveFilter('discount') + 前端过滤 originalPrice > price

ProductsPage.params
  │
  ├─ sort (来自 SortDropdown) → 'newest' | 'price_asc' | 'price_desc' | 'popular'
  ├─ pageNum, pageSize
  ├─ keyword, categoryId, priceMin, priceMax, conditions
  └─ activeFilter (前端状态) → 'all' | 'ai' | 'discount'
```

- ToolsPlaza 不再影响排序值 — 职责分离
- SortDropdown 作为排序的唯一入口
- 排序变化时 `resetAllProducts()` + `pageNum = 1`
- 特价优惠通过前端过滤实现，不依赖后端 API 参数

## 组件变更

### 新增：`SortDropdown`

```
src/components/search/
├── FacetFilter.tsx       (不变)
└── SortDropdown.tsx      (新增)
```

```typescript
interface SortDropdownProps {
  value: 'newest' | 'price_asc' | 'price_desc' | 'popular';
  onChange: (value: 'newest' | 'price_asc' | 'price_desc' | 'popular') => void;
}
```

- 触发按钮：当前选中文本 + 箭头 icon
- 下拉面板：选项列表，选中项高亮
- 点击选项后：`onChange` 回调 + 面板关闭
- 点击外部/按 Escape：面板关闭

### 修改：`ToolsPlaza`

- 移除 `new` 和 `hot` 按钮对应的 JSX
- `activeFilter` 状态类型缩减为 `'all' | 'ai' | 'discount'`
- `onFilterChange` 回调只传递这三种值

### 修改：`ProductsPage`

- `view-options` 区域 → `<SortDropdown>` 组件
- `handleFilterChange` 不再做 sort 映射，只处理模式切换
- 移除 `filterSortMap` 逻辑
- 新增 `handleSortChange` 方法，只更新 `params.sort`
- `sortOptions` 数组保留供 SortDropdown 使用

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `src/components/product/ToolsPlaza.tsx` | 修改 | 去掉最新发布/热门商品按钮 |
| `src/pages/products/ProductsPage.tsx` | 修改 | view-options → SortDropdown；handleFilterChange 语义更新 |
| `src/components/search/SortDropdown.tsx` | 新增 | 排序下拉组件 |
| `src/pages/products/products-premium.css` | 修改 | 新增 SortDropdown 样式 |

## 视觉设计要点

- **SortDropdown 触发按钮**：灰底圆角、与现有 `view-btn` 风格一致
- **下拉面板**：白底阴影、与 AdminSelect 类似但适配商品页设计语言
- **选项 icon**：用文本标签 + 当前选中圆点标记
- **ToolsPlaza**：去掉两个按钮后保持三按钮布局，视觉平衡
- **响应式**：移动端 Dropdown 宽度自适应，选项可触控
