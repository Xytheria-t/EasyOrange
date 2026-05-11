# Admin 模块补全 & Application 模块清理 设计文档

> 日期: 2026-05-11
> 状态: ✅ 已实施完成

## 问题背景

easy-orange 项目的后台管理（Admin）模块在开发中途停止，导致前后端 admin 模块对接不全。同时 application 模块可能残留旧的管理端代码。

## 现状分析

### 后端 Admin 模块 (`easyorange-admin`) — 代码完整

后端 6 个 Controller + Service + DTO 全套齐全：

| 功能域 | Controller | 端点数 | 状态 |
|--------|-----------|--------|------|
| 仪表板 | AdminDashboardController | 4 | ✅ |
| 用户管理 | AdminUserController | 7+ | ✅ |
| 商品管理 | AdminProductController | 4 | ✅ |
| 订单管理 | AdminOrderController | 6 | ✅ |
| 分类管理 | AdminCategoryController | 5+ | ✅ |
| 举报管理 | AdminReportController | 4+ | ✅ |
| 商品审核 | AdminProductAuditController | 2 | ✅ |

### 前端 Admin 模块 (实施前) — 严重不完整

**API 层** (`adminApi.ts`): 仅 10 个接口，缺失约 20+ 个
**Types 层** (`types/admin.ts`): 缺少 Order/Report/Category/Audit/用户高级操作 类型定义
**Hooks 层**: 仅 Dashboard/Products/Users 有 hook，缺少 Orders/Reports/Categories/Audit
**页面层**: OrderManagePage 和 ReportManagePage 使用硬编码 MOCK 数据

### Application 模块 — 需清理

- `PlatformStatsController` 的 `/api/stats/platform` 与 Admin Dashboard 统计功能重叠（保留为公开接口）
- AGENTS.md 描述了不存在的 adapter 目录文件名
- 父 POM `<modules>` 遗漏 `easyorange-admin` 注册 → 构建报错

## 实施方案：补全 + 清理 + 美化

### Part 1: Types 补全 (`types/admin.ts`) ✅

新增 ~20 个类型定义，与后端 DTO 完全对齐：
- 订单: `AdminOrder`, `AdminOrderDetail`, `AdminOrderQuery`, `OrderInterventionRequest`, `OrderStatsVO`
- 举报: `AdminReport`, `AdminReportDetail`, `AdminReportQuery`, `ReportHandleRequest`, `ReportStatsVO`
- 分类: `CategoryVO`, `CategoryTreeVO`, `CategoryCreateRequest`, `CategoryUpdateRequest`
- 审核与用户操作: `BatchAuditRequest`, `ProductAuditRequest`, `UserRoleRequest`, `ResetPasswordRequest`, `UserUnlockRequest`

### Part 2: API 层补全 (`api/adminApi.ts`) ✅

10 → **33 个** API 函数，与后端 6 个 Controller 完全对齐。

### Part 3: Hooks 补全 (`hooks/`) ✅

新建 4 个 hook 文件 + 更新 index.ts 导出：
- `useAdminOrders.ts` — 订单列表/详情/统计/取消/强制完成/退款
- `useAdminReports.ts` — 举报列表/详情/统计/处理
- `useAdminCategories.ts` — 分类 CRUD + 树形结构
- `useAdminProductAudit.ts` — 商品审核（单个+批量）

### Part 4: 页面改造 (Mock → Real API) ✅

- **OrderManagePage**: MOCK_ORDERS → useAdminOrders 真实 API + 分页/筛选/搜索
- **ReportManagePage**: MOCK_REPORTS → useAdminReports + useHandleReport 处理/驳回
- **StatsPage**: MOCK_CATEGORY → useAdminCategories 真实数据；Trend/Activity 标记 TODO

### Part 5: Application 模块清理 ✅

1. 父 POM `<modules>` 新增 `easyorange-admin` 注册
2. AGENTS.md 同步实际文件名和模块依赖

### Part 6: UI 美化与修复（超出原设计范围的额外工作）

#### 6a. Dashboard 仪表盘重设计 ✅

完全重写 DashboardPage，匹配主页 HeroSection 的橙粉弥散渐变视觉语言：
- 4 个动态浮动 Blob 背景 + Aurora 光晕 + 噪点纹理 + 网格 + 暗角
- Playfair Display 超大标题 + "管理员" 渐变文字 + 装饰下划线动画
- 6 个快捷操作药丸（商品审核/处理举报/订单管理/用户管理/分类管理/数据统计）
- 15+ keyframe 动画，支持 prefers-reduced-motion

#### 6b. StatCard 组件放大 ✅

数值字体 1.75rem → 2.35rem (+34%)，图标容器 48×48 → 58×58 (+21%)，内边距 +23%，圆角 20→24px。标题统一为 4 字 + whiteSpace nowrap。

#### 6c. 全局布局修复 ✅

所有 6 个管理页面的背景层从 `position: fixed` 改为 `position: absolute`，根容器增加 `minHeight: calc(100vh - 80px)`，内容层改为 flex column 布局。解决内容区裁切问题。

#### 6d. 侧边栏收窄 ✅

sidebar-width: 260px → 210px (-19%)，collapsed: 72px → 60px，所有内边距同步收紧。

#### 6e. AdminSelect 下拉菜单修复 ✅

handleClickOutside 增加 listRef 排除检查，解决 Portal 下拉选项点击即关闭的 bug。同时移除冲突的 overflow:hidden。

#### 6f. StatsPage 分类分布进度条修复 ✅

进度条宽度从绝对百分比改为相对最大值比例 (ratio = count/maxCount)，解决所有分类显示 0% 的问题。

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `src/admin/types/admin.ts` | 修改 | 新增 ~20 个类型定义 |
| `src/admin/api/adminApi.ts` | 修改 | 10 → 33 个 API 函数 |
| `src/admin/hooks/useAdminOrders.ts` | 新建 | 订单管理 hook |
| `src/admin/hooks/useAdminReports.ts` | 新建 | 举报管理 hook |
| `src/admin/hooks/useAdminCategories.ts` | 新建 | 分类管理 hook |
| `src/admin/hooks/useAdminProductAudit.ts` | 新建 | 商品审核 hook |
| `src/admin/hooks/index.ts` | 修改 | 导出新 hooks |
| `src/admin/pages/dashboard/DashboardPage.tsx` | 重写 | 主页风格仪表盘 |
| `src/admin/pages/dashboard/StatCard.tsx` | 修改 | 放大尺寸 |
| `src/admin/pages/orders/OrderManagePage.tsx` | 重写 | Mock → 真实 API + 布局修复 |
| `src/admin/pages/reports/ReportManagePage.tsx` | 重写 | Mock → 真实 API + 布局修复 |
| `src/admin/pages/stats/StatsPage.tsx` | 修改 | 分类真实数据 + 进度条修复 |
| `src/admin/components/AdminSelect.tsx` | 修改 | Portal 点击修复 + 滚动修复 |
| `src/admin/styles/admin.css` | 修改 | 侧边栏 210px |
| `.../backend/pom.xml` (父) | 修改 | 注册 easyorange-admin 模块 |
| `.../application/AGENTS.md` | 修改 | 同步实际结构 |

## 后续待办

- [ ] 后端新增 `/admin/dashboard/trend` 月度趋势 API（替换 StatsPage MOCK_TREND）
- [ ] 后端新增 `/admin/dashboard/activity` 最近动态 API（替换 StatsPage MOCK_RECENT_ACTIVITY）
- [ ] 各管理页面添加数据详情弹窗（订单详情、举报详情等）
