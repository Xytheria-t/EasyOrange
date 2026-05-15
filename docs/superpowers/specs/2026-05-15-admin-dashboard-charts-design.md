# 管理端 Dashboard 图表增强设计

## 概述

为管理端 Dashboard 新增三类图表：趋势图、用户活跃热力图、Top 浏览量商品。采用 Recharts 图表库实现交互式可视化。

## 技术选型

- **图表库**: Recharts ^2.x（纯 React 声明式图表，~150KB gzip）
- **后端增强**: 新增 2 个 API 端点 + 复用现有 `/dashboard/trend`

## 后端新增 API

### GET /api/admin/dashboard/user-activity-heatmap

返回用户操作按星期×小时的分布数据。

请求参数: 无（默认最近 30 天）

响应:
```json
[
  { "dayOfWeek": 1, "hour": 9, "count": 42 },
  { "dayOfWeek": 1, "hour": 10, "count": 38 },
  ...
]
```

数据来源: `eo_oper_log` 表，按 `DAYOFWEEK(oper_time)` 和 `HOUR(oper_time)` 分组统计，限制最近 30 天。

### GET /api/admin/dashboard/top-products?limit=10

返回浏览量最高的商品列表。

响应:
```json
[
  {
    "productId": 1,
    "name": "商品名",
    "viewCount": 1024,
    "price": 99.00,
    "mainImage": "url",
    "status": 1,
    "statusDesc": "已上架"
  }
]
```

数据来源: `eo_product` 表，按 `view_count` 降序排列，限制 `del_flag = 0`。

### 复用接口

- `GET /api/admin/dashboard/trend` — 已有，用于趋势折线图

## 后端新增 VO

```java
// UserActivityHeatmapVO.java
public record UserActivityHeatmapVO(Integer dayOfWeek, Integer hour, Long count) {}

// TopProductVO.java
@Builder @Data
public class TopProductVO {
    private Long productId;
    private String name;
    private Integer viewCount;
    private java.math.BigDecimal price;
    private String mainImage;
    private Integer status;
    private String statusDesc;
}
```

## 前端组件架构

### 新增依赖

```json
"recharts": "^2.15.0"
```

### 新增组件

| 组件 | 位置 | 用途 |
|------|------|------|
| `TrendChart.tsx` | `admin/pages/dashboard/charts/` | 趋势折线图（精简版 + 完整版） |
| `ActivityHeatmap.tsx` | `admin/pages/dashboard/charts/` | 用户活跃热力图 |
| `TopProductsChart.tsx` | `admin/pages/dashboard/charts/` | Top 浏览量商品柱状图 |

### 增强组件

| 组件 | 变更 |
|------|------|
| `DashboardPage.tsx` | 新增 3 列图表区域（在现有 2 列内容下方） |
| `StatsPage.tsx` | 替换 CSS 趋势柱图为 Recharts 交互式折线图 |

### 新增 Hook

```typescript
// useAdminDashboard.ts 中新增
export function useUserActivityHeatmap(): QueryResult<...>
export function useTopProducts(limit?: number): QueryResult<...>
```

### 新增 API

```typescript
// adminApi.ts 中新增
getUserActivityHeatmap(): request<UserActivityItem[]>
getTopProducts(limit?: number): request<TopProductItem[]>
```

### 新增 Type

```typescript
// admin.ts 中新增
interface UserActivityItem {
  dayOfWeek: number;
  hour: number;
  count: number;
}

interface TopProductItem {
  productId: number;
  name: string;
  viewCount: number;
  price: number;
  mainImage: string | null;
  status: number;
  statusDesc: string;
}
```

## Dashboard 首页布局变更

在当前 "最近注册用户 + 最近上架商品" 两列区域下方，新增三列图表区域：

```
┌──────────────────────┬──────────────────────┬──────────────────────┐
│    趋势概览 (折线)    │  用户活跃热力图       │  Top 浏览量商品      │
│                      │  (星期×小时)         │  (横向柱状图)        │
│  3 个月月趋势折线     │  自定义颜色单元格矩阵  │  水平条形 Top10      │
│  显示 users/products │  X轴: 星期一到日     │  显示名称+浏览量+价格 │
│  有 tooltip          │  Y轴: 0-23时        │                      │
└──────────────────────┴──────────────────────┴──────────────────────┘
```

### 设计风格

- 保持现有玻璃态 (Glassmorphism) 视觉语言
- 卡片背景: `rgba(255,255,255,0.65)` + `backdropFilter: blur(24px)`
- 颜色延续现有调色板: orange(#F97316), rose(#FB7185), purple(#C39BD3), gold(#FBBF24), emerald(#10B981)
- 文字字体: Playfair Display 标题 + LXGW WenKai 正文

## 实现步骤

1. 后端: 新增 `UserActivityHeatmapVO.java`, `TopProductVO.java`
2. 后端: 在 `AdminDashboardService` 新增 `getUserActivityHeatmap()`, `getTopProducts()`
3. 后端: 在 `AdminDashboardController` 新增对应端点
4. 前端: 安装 recharts 依赖
5. 前端: 新增类型定义 `UserActivityItem`, `TopProductItem`
6. 前端: 新增 API 方法
7. 前端: 新增 hook
8. 前端: 新增 3 个图表组件
9. 前端: 改造 DashboardPage 布局，集成图表区域
10. 前端: 改造 StatsPage 趋势图为 Recharts 交互版
11. 测试: 后端单元测试 + 前端编译验证

## 未涉及的范围

- 不修改现有权限/认证逻辑
- 不修改数据库表结构
- 不需要 Flyway 迁移
- 不涉及 WebSocket/实时推送

## 响应式处理

- ≥1280px: 3 列图表并排
- 900-1280px: 2 列（趋势图占 2 列，热力图和 Top 商品并排或堆叠）
- <900px: 全部单列堆叠
