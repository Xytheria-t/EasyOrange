/**
 * 管理端图表色板 —— 单一出口。
 *
 * 真实取值只在 `styles/admin.css` 的 `--admin-chart-*` / `--admin-flow-*`，
 * 这里只做引用与派生。此前色值散在统计页、趋势图、动态列表三处各写一份，
 * 同一个「用户」在卡片、图例、圆点上是三个色。
 *
 * 两套色板不能混用，这是编码语义的区别：
 * - `TREND_SERIES` / `ACTIVITY_COLORS` 编码**身份**：三条线是三个不同的东西，
 *   颜色要彼此可区分。
 * - `CATEGORY_RANKS` 编码**量级**：分类分布是同一条分布的前几名，
 *   不是六类不同事物，用单色相顺序色阶（色深随排名递减）。
 *   此前这里用的是分类色板，六根彩虹条并排会让人误读成六个并列的类别。
 *
 * SVG 的 `fill` / `stroke` 接受 `var()`，所以图表不必把颜色拷进 JS 常量。
 */

/** 趋势图三条序列。数值越大档位越靠后，色板与 `ACTIVITY_COLORS` 保持同源。 */
export const TREND_SERIES = [
    { key: 'users', label: '用户', color: 'var(--admin-chart-1)' },
    { key: 'products', label: '商品', color: 'var(--admin-chart-3)' },
    { key: 'orders', label: '订单', color: 'var(--admin-chart-5)' },
] as const;

export const TREND_SERIES_COLORS = {
    users: 'var(--admin-chart-1)',
    products: 'var(--admin-chart-3)',
    orders: 'var(--admin-chart-5)',
} as const;

/** 分类分布条：单色相顺序色阶，色深 = 排名。超出 `RANK_VISIBLE_COUNT` 的分类
 *  合并成「其他」一行，百分比才不会和总数对不上。 */
export const CATEGORY_RANKS = [
    'var(--admin-chart-seq-1)',
    'var(--admin-chart-seq-2)',
    'var(--admin-chart-seq-3)',
    'var(--admin-chart-seq-4)',
    'var(--admin-chart-seq-5)',
    'var(--admin-chart-seq-6)',
] as const;

/** 分布条最多展示的名次。 */
export const RANK_VISIBLE_COUNT = CATEGORY_RANKS.length;

/** 最近动态的类型色。与趋势图同源，同一个「用户」在两处是同一个橙。 */
export const ACTIVITY_COLORS = {
    user: 'var(--admin-chart-1)',
    product: 'var(--admin-chart-3)',
    order: 'var(--admin-chart-5)',
} as const;

/** 订单摘要：这几个是同一批单子的不同阶段，用推进程度表示，而不是给四个不相干的色。 */
export const ORDER_SUMMARY_COLORS = {
    today: 'var(--admin-chart-1)',
    toShip: 'var(--admin-flow-ship)',
    toReceive: 'var(--admin-flow-transit)',
    completed: 'var(--admin-flow-done)',
    revenue: 'var(--admin-accent-deep)',
} as const;

/** 图表里的中性文字色：坐标轴、图例、提示。 */
export const CHART_TEXT = {
    axis: 'var(--admin-muted)',
    label: 'var(--admin-ink)',
    legend: 'var(--admin-muted)',
} as const;
