/**
 * 管理端图表色板 —— 单一出口。
 *
 * 真实取值只在 `styles/admin.css` 的 `--admin-chart-*` / `--admin-gradient-*`，
 * 这里只做引用与派生。此前色值散在统计页、趋势图、动态列表三处各写一份，
 * 同一个「用户」在卡片、图例、圆点上是三个色。
 *
 * SVG 的 `fill` / `stroke` 接受 `var()`，所以图表不必把颜色拷进 JS 常量。
 */

/** 分类饼图 / 分布条：按顺序取用，超过 6 类要另配一组可区分的色。 */
export const CATEGORY_COLORS = [
    'var(--admin-chart-1)',
    'var(--admin-chart-2)',
    'var(--admin-chart-3)',
    'var(--admin-chart-4)',
    'var(--admin-chart-5)',
    'var(--admin-chart-6)',
] as const;

/** 趋势图三条序列。 */
export const TREND_SERIES_COLORS = {
    users: 'var(--admin-chart-1)',
    products: 'var(--admin-chart-3)',
    orders: 'var(--admin-chart-5)',
} as const;

/** 最近动态的类型色。 */
export const ACTIVITY_COLORS = {
    user: 'var(--admin-chart-1)',
    product: 'var(--admin-chart-3)',
    order: 'var(--admin-chart-5)',
} as const;

/** 统计卡的图标底色渐变。 */
export const STAT_CARD_GRADIENTS = [
    'var(--admin-gradient-1)',
    'var(--admin-gradient-2)',
    'var(--admin-gradient-3)',
    'var(--admin-gradient-4)',
] as const;

/** 订单摘要各项的圆点色。 */
export const ORDER_SUMMARY_COLORS = {
    today: 'var(--admin-chart-1)',
    toShip: 'var(--admin-chart-4)',
    toReceive: 'var(--admin-chart-5)',
    completed: 'var(--admin-chart-3)',
    revenue: 'var(--admin-chart-2)',
} as const;

/** 图表里的中性文字色：坐标轴、图例、提示。 */
export const CHART_TEXT = {
    axis: 'var(--admin-muted)',
    label: 'var(--admin-ink)',
    legend: 'var(--admin-muted)',
} as const;
