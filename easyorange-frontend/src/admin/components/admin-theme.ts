import type { CSSProperties } from 'react';

/**
 * 管理端设计令牌的唯一出口。
 *
 * 页面样式仍按项目约定内联 `style={{}}`，但只能引用这里的 `var(--admin-*)` / `var(--status-*)`，
 * 不再散落裸 hex 与裸圆角。断点行为见 `admin.css` 的 `.admin-*` 类，两者职责不重叠。
 * 令牌定义在 `styles/admin.css` 的 `:root`。
 */

/** 页面三层结构：根容器 / 背景层 / 内容层。 */
export const pageRoot: CSSProperties = {
    position: 'relative',
    minHeight: 'calc(100vh - var(--admin-header-height))',
    isolation: 'isolate',
};

export const pageBackdrop: CSSProperties = {
    position: 'absolute',
    inset: 0,
    borderRadius: 'var(--admin-radius-card)',
    background: 'var(--gradient-mesh-1), var(--gradient-mesh-2), var(--gradient-mesh-3)',
    pointerEvents: 'none',
    zIndex: 0,
};

export const pageContent: CSSProperties = {
    position: 'relative',
    zIndex: 1,
    display: 'flex',
    flexDirection: 'column',
    flex: 1,
    minWidth: 0,
};

/** 玻璃内容表面。表格卡用 `grow` 让它吃掉剩余高度。 */
export function card(grow = false): CSSProperties {
    return {
        background: 'var(--admin-surface-bg)',
        backdropFilter: 'var(--admin-blur)',
        WebkitBackdropFilter: 'var(--admin-blur)',
        border: '1px solid var(--admin-surface-border)',
        borderRadius: 'var(--admin-radius-card)',
        boxShadow: 'var(--admin-surface-shadow)',
        // 卡片要承载绝对定位的装饰层，并裁掉子元素（直角表头 / 分页条）的溢出
        position: 'relative',
        overflow: 'hidden',
        ...(grow ? { flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' } : null),
    };
}

/** 更轻的次级表面（信息格、内嵌块），与主卡拉开层次但不换一套参数。 */
export function insetSurface(): CSSProperties {
    return {
        background: 'rgba(255,255,255,0.6)',
        border: '1px solid rgba(229,224,219,0.4)',
        borderRadius: 'var(--admin-radius-control)',
    };
}

/* ---------------- 文字 ---------------- */

export const titleText: CSSProperties = {
    fontFamily: 'var(--admin-font-title)',
    fontSize: '1.5rem',
    fontWeight: 700,
    color: 'var(--admin-ink)',
    letterSpacing: '-0.02em',
    lineHeight: 1.25,
    margin: 0,
};

export const subtitleText: CSSProperties = {
    fontSize: '0.84rem',
    color: 'var(--admin-muted)',
    lineHeight: 1.6,
    margin: 0,
};

export const sectionTitleText: CSSProperties = {
    fontFamily: 'var(--admin-font-title)',
    fontSize: '1.02rem',
    fontWeight: 700,
    color: 'var(--admin-ink)',
    margin: 0,
};

export const valueText: CSSProperties = {
    fontSize: '0.87rem',
    fontWeight: 600,
    color: 'var(--admin-ink)',
    margin: 0,
};

export const mutedText: CSSProperties = {
    fontSize: '0.82rem',
    color: 'var(--admin-muted)',
    margin: 0,
};

export const labelText: CSSProperties = {
    fontSize: '0.73rem',
    fontWeight: 600,
    color: 'var(--admin-muted)',
    margin: 0,
};

export const monoText: CSSProperties = {
    fontFamily: 'var(--admin-font-mono)',
    fontSize: '0.8rem',
    color: 'var(--admin-ink-soft)',
};

export const accentNumberText: CSSProperties = {
    fontFamily: 'var(--admin-font-price)',
    fontSize: '1.9rem',
    fontWeight: 700,
    color: 'var(--admin-ink)',
    lineHeight: 1.1,
};

export const priceText: CSSProperties = {
    fontFamily: 'var(--admin-font-price)',
    fontSize: '1rem',
    fontWeight: 700,
    color: 'var(--admin-accent)',
};

/* ---------------- 控件 ----------------
   主/次/危险按钮统一走共享 `Button`：它的 default 变体用 primary-700 而不是
   primary-500，白字对比度才跨过 WCAG AA，此前各页手写 #F97316 渐变白字只有 2.8:1。
   这里只保留没有共享组件可复用的形态。 */

/** 行内文字操作（如「详情」）。对比度取 --admin-accent，不用浅橙字。 */
export function linkButton(): CSSProperties {
    return {
        background: 'var(--admin-accent-soft)',
        color: 'var(--admin-accent)',
        border: '1px solid var(--admin-accent-soft-border)',
        borderRadius: 'var(--admin-radius-control)',
        padding: '0.32rem 0.7rem',
        fontSize: '0.8rem',
        fontWeight: 600,
        cursor: 'pointer',
    };
}

/** 32px 见方的图标按钮；无障碍名由调用方用 aria-label 给出，不只给 title。 */
export function iconButton(color = 'var(--admin-muted)'): CSSProperties {
    return {
        width: 32,
        height: 32,
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'transparent',
        color,
        border: '1px solid transparent',
        borderRadius: 10,
        cursor: 'pointer',
        transition: 'background 150ms var(--ease-out), color 150ms var(--ease-out)',
    };
}

export function textInput(): CSSProperties {
    return {
        width: '100%',
        background: '#fff',
        border: 'var(--admin-control-border)',
        borderRadius: 'var(--admin-radius-control)',
        padding: '0.68rem 1rem',
        fontSize: '0.87rem',
        color: 'var(--admin-ink)',
        outline: 'none',
        transition: 'border-color 150ms var(--ease-out), box-shadow 150ms var(--ease-out)',
    };
}

export function errorBannerStyle(): CSSProperties {
    return {
        display: 'flex',
        alignItems: 'flex-start',
        gap: '0.7rem',
        background: 'var(--admin-danger-bg)',
        border: '1px solid var(--admin-danger-border)',
        borderRadius: 'var(--admin-radius-control)',
        padding: '0.9rem 1.1rem',
    };
}

/** 状态点：形状 + 文字双编码，不靠颜色单独传达状态。 */
export function statusDot(color: string): CSSProperties {
    return {
        width: 7,
        height: 7,
        borderRadius: 'var(--admin-radius-pill)',
        background: color,
        flexShrink: 0,
    };
}
