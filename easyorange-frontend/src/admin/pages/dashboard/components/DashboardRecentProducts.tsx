import { Link } from 'react-router-dom';
import type { RecentProduct } from '@/admin/types/admin';
import { formatRelativeTime } from '@/utils';
import {
    emptyStateBox,
    emptyStateIcon,
    emptyStateSubtitle,
    emptyStateTitle,
    linkStyle,
    listColumn,
    listItemStyle,
    nameText,
    sectionCard,
    sectionHeader,
    sectionIconWrap,
    sectionTitle,
    timeBadge,
} from '../lib/dashboardStyles';

interface DashboardRecentProductsProps {
    products?: RecentProduct[];
    isLoading: boolean;
}

const PRODUCT_ICONS: Record<string, string> = {
    default: '\u{1F4E6}',
    教材: '\u{1F4DA}',
    电子: '\u{1F4BB}',
    手机: '\u{1F4F1}',
    电脑: '\u{1F4BB}',
    自行车: '\u{1F6B4}',
    衣服: '\u{1F455}',
    鞋: '\u{1F45F}',
    书: '\u{1F4D6}',
};

function getProductIcon(name: string): string {
    for (const key of Object.keys(PRODUCT_ICONS)) {
        if (key !== 'default' && name.includes(key)) {
            return PRODUCT_ICONS[key];
        }
    }
    return PRODUCT_ICONS.default;
}

export function DashboardRecentProducts({ products, isLoading }: DashboardRecentProductsProps) {
    return (
        <section style={sectionCard}>
            <div style={sectionHeader}>
                <h2 style={sectionTitle}>
                    <span
                        style={sectionIconWrap(
                            'linear-gradient(135deg, rgba(195,155,211,0.12), rgba(195,155,211,0.03))',
                            '#C39BD3'
                        )}
                    >
                        <svg
                            aria-hidden="true"
                            width="15"
                            height="15"
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            strokeWidth="2"
                            strokeLinecap="round"
                            strokeLinejoin="round"
                        >
                            <path d="M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 003 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0021 16z" />
                            <polyline points="3.27 6.96 12 12.01 20.73 6.96" />
                            <line x1="12" y1="22.08" x2="12" y2="12" />
                        </svg>
                    </span>
                    最近上架商品
                </h2>
                <Link
                    to="/admin/products"
                    style={{ ...linkStyle, color: '#C39BD3' }}
                    onMouseEnter={e => {
                        e.currentTarget.style.gap = '0.5rem';
                    }}
                    onMouseLeave={e => {
                        e.currentTarget.style.gap = '0.25rem';
                    }}
                >
                    查看全部
                    <svg
                        aria-hidden="true"
                        width="13"
                        height="13"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="2.5"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                    >
                        <path d="M5 12h14M12 5l7 7-7 7" />
                    </svg>
                </Link>
            </div>
            <div style={{ padding: '1.15rem 1.5rem' }}>
                {isLoading ? (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '0.8rem' }}>
                        {Array.from({ length: 3 }).map((_, i) => (
                            <div
                                // biome-ignore lint/suspicious/noArrayIndexKey: stable skeleton list
                                key={i}
                                style={{
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'space-between',
                                }}
                            >
                                <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                                    <div
                                        style={{
                                            width: 38,
                                            height: 38,
                                            borderRadius: 12,
                                            background: 'rgba(229,224,219,0.35)',
                                        }}
                                    />
                                    <div
                                        style={{
                                            width: 104,
                                            height: 14,
                                            background: 'rgba(229,224,219,0.3)',
                                            borderRadius: 6,
                                        }}
                                    />
                                </div>
                                <div
                                    style={{
                                        width: 66,
                                        height: 14,
                                        background: 'rgba(229,224,219,0.25)',
                                        borderRadius: 6,
                                    }}
                                />
                            </div>
                        ))}
                    </div>
                ) : products && products.length > 0 ? (
                    <ul
                        style={{
                            listStyle: 'none',
                            display: 'flex',
                            flexDirection: 'column',
                            margin: 0,
                            padding: 0,
                        }}
                    >
                        {products.map((product, idx) => (
                            <li
                                key={product.productId}
                                style={{
                                    ...listItemStyle,
                                    borderBottom:
                                        idx < products.length - 1 ? '1px solid rgba(229,224,219,0.28)' : 'none',
                                }}
                                onMouseEnter={e => {
                                    e.currentTarget.style.paddingLeft = '0.85rem';
                                    e.currentTarget.style.background = 'rgba(195,155,211,0.025)';
                                }}
                                onMouseLeave={e => {
                                    e.currentTarget.style.paddingLeft = '0.4rem';
                                    e.currentTarget.style.background = 'transparent';
                                }}
                            >
                                <div style={listColumn}>
                                    <span
                                        style={{
                                            width: 38,
                                            height: 38,
                                            borderRadius: 12,
                                            background:
                                                'linear-gradient(135deg, rgba(249,115,22,0.06), rgba(195,155,211,0.05))',
                                            display: 'flex',
                                            alignItems: 'center',
                                            justifyContent: 'center',
                                            fontSize: '1.1rem',
                                            flexShrink: 0,
                                            boxShadow: '0 1px 4px rgba(0,0,0,0.03)',
                                        }}
                                    >
                                        {getProductIcon(product.name)}
                                    </span>
                                    <div style={{ minWidth: 0 }}>
                                        <span style={nameText}>{product.name}</span>
                                        {product.price != null && (
                                            <span
                                                style={{
                                                    fontSize: '0.72rem',
                                                    color: '#F97316',
                                                    fontWeight: 600,
                                                    fontFamily: "'Cormorant Garamond', 'Playfair Display', serif",
                                                }}
                                            >
                                                ¥{Number(product.price).toLocaleString('zh-CN')}
                                            </span>
                                        )}
                                    </div>
                                </div>
                                <span style={timeBadge}>{formatRelativeTime(product.createTime ?? '')}</span>
                            </li>
                        ))}
                    </ul>
                ) : (
                    <div style={emptyStateBox}>
                        <div
                            style={emptyStateIcon(
                                'linear-gradient(135deg, rgba(195,155,211,0.07), rgba(249,115,22,0.04))'
                            )}
                        >
                            {'\u{1F4E6}'}
                        </div>
                        <div style={emptyStateTitle}>暂无商品记录</div>
                        <div style={emptyStateSubtitle}>当前暂无新上架商品</div>
                    </div>
                )}
            </div>
        </section>
    );
}
