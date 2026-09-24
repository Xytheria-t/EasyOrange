# easyorange-frontend — 前端约定与踩坑

> React + Vite + TypeScript + TanStack Query。入口地图与命令见 [README.md](./README.md)。
> **本文件只写「不按这个写就出 bug」的约定**——这些坑的共同特征是：代码看起来完全正常，但运行结果是错的或会无限循环。
> 全局硬约束（`Result<T>` / UUID v7 string ID / `Long → String`）见[根 AGENTS.md](../AGENTS.md)。

## 管理端渲染（`src/admin/`）

- **弹窗 / 抽屉 / 确认框必须走 Radix 的 `Dialog` / `Sheet`**（内部已 `Portal` 到 `document.body`）：`.admin-sidebar` / `.admin-header` 的 `backdrop-filter: blur()` 会创建新包含块，手搓的 `position: fixed` 定位基准会错乱
- **Portal 容器必须处理溢出**：`maxHeight: 'calc(100vh - 2rem)'` + `display: flex; flexDirection: column; overflow: hidden`，内容区 `flex: 1; overflowY: auto; minHeight: 0`
- **`AdminTable` 的 render 签名是 `(value, record)`**——第一个参数是单元格值，第二个才是整行。只写 `(record) => ...` 会拿到 `undefined`
- **所有 `<select>` 必须用 `AdminSelect` 组件**（Portal 渲染面板，解决 fixed 定位失效）；`handleClickOutside` 必须**同时排除触发按钮 ref 和列表 `listRef`**，否则点选项会立即关闭
- **视觉取值只有一个来源：`styles/admin.css`**。tsx / ts 里禁裸 `#hex`、裸 `rgb()/rgba()`、Tailwind 任意色值（`text-[#6E6862]`）；要令牌用 `var(--admin-*)` 或 Tailwind 变量简写 `text-(--admin-muted)`。`.githooks/check-admin-style-drift.py` 会在 pre-commit 拦（也会查未定义的 `--admin-*` 令牌，那类 bug 静默失效不报错）
- **`style={{}}` 只留给按数据算出来的值**（头像渐变、状态点颜色、计算位置）；静态视觉与断点都上提成 `.admin-*` 类——内联样式写不了媒体查询，断点留在 tsx 里等于没有响应式
- **页面布局走 `<AdminPage>` 三层结构**（根 / 背景层 / 内容层，背景层 `position: absolute` **禁 fixed**），不要在页面里重写这三层
- **状态标签只认 `StatusBadge` 的配置出口**：`statusVisual(type, status)` 取配色、`statusFilterOptions(type)` 派生筛选选项；页面不要再抄一份状态→标签映射
- **`admin/*` 路由必须在 `MinimalLayout` 外部独立渲染**，否则 C 端 Header 会出现在管理页

## 状态（Zustand）

- **store 只接受事件驱动写入**（STOMP 回调、用户操作回调），**禁止在 `useEffect` 内写 store**——spread 新引用 → 重渲染 → 无限循环
- **selector 里 `?? []` / `?? {}` 必须用模块级常量**，禁止内联：内联产生新引用触发无限循环，**React StrictMode 下会放大到 50 层**

## 性能与加载

- **体积 > 100KB 的第三方库必须懒加载**（当前只有 recharts 走这条）：① `manualChunks` 分独立 `vendor-*` chunk ② `React.lazy` + `Suspense` 包装。参考 `src/admin/pages/stats/charts/lazyCharts.tsx`
- **共享组件（如 `ProductCard`）的样式 CSS 必须在组件文件自身 import**，禁止只靠页面级导入——`React.lazy` 懒加载时页面级 CSS 不随组件 chunk 加载

## 数据边界

- **`Long → String` 收敛**：后端全局把 `long`/`Long` 序列化成字符串（JS 精度安全，见后端 `JacksonConfig`），计数字段线上是 `"3"` 而非 `3`（含 `PageResult.total`）。**前端 API 层声明为 `number` 的计数字段必须 `Number(...)` 收敛**（参考 `paymentApi` / `adminApi` 里的计数字段处理），否则算术会变字符串拼接

## 交互

- **`scrollIntoView` 防误触发**：用 ref 记录上一次状态（如历史长度），仅在数据真正新增时滚动；禁止在仅依赖 props/state 的 effect 里无条件调用
