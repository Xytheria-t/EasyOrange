import { Link } from 'react-router-dom';
import type { RecentUser } from '@/admin/types/admin';
import { formatRelativeTime } from '@/utils';
import {
    emptyStateBox,
    emptyStateIcon,
    emptyStateSubtitle,
    emptyStateTitle,
    linkStyle,
    listColumn,
    listItemStyle,
    metaText,
    nameText,
    sectionCard,
    sectionHeader,
    sectionIconWrap,
    sectionTitle,
    timeBadge,
} from '../lib/dashboardStyles';

interface DashboardRecentUsersProps {
    users?: RecentUser[];
    isLoading: boolean;
}

const AVATAR_GRADIENTS = [
    'linear-gradient(135deg, #F97316, #FB923C)',
    'linear-gradient(135deg, #FB7185, #C39BD3)',
    'linear-gradient(135deg, #34D399, #10B981)',
    'linear-gradient(135deg, #FBBF24, #F97316)',
    'linear-gradient(135deg, #C39BD3, #D8B4FE)',
] as const;

export function DashboardRecentUsers({ users, isLoading }: DashboardRecentUsersProps) {
    return (
        <section style={sectionCard}>
            <div style={sectionHeader}>
                <h2 style={sectionTitle}>
                    <span
                        style={sectionIconWrap(
                            'linear-gradient(135deg, rgba(249,115,22,0.1), rgba(249,115,22,0.03))',
                            '#F97316'
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
                            <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" />
                            <circle cx="9" cy="7" r="4" />
                            <path d="M23 21v-2a4 4 0 00-3-3.87" />
                            <path d="M16 3.13a4 4 0 010 7.75" />
                        </svg>
                    </span>
                    最近注册用户
                </h2>
                <Link
                    to="/admin/users"
                    style={{ ...linkStyle, color: '#F97316' }}
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
                                            width: 88,
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
                ) : users && users.length > 0 ? (
                    <ul
                        style={{
                            listStyle: 'none',
                            display: 'flex',
                            flexDirection: 'column',
                            margin: 0,
                            padding: 0,
                        }}
                    >
                        {users.map((user, idx) => (
                            <li
                                key={user.userId}
                                style={{
                                    ...listItemStyle,
                                    borderBottom: idx < users.length - 1 ? '1px solid rgba(229,224,219,0.28)' : 'none',
                                }}
                                onMouseEnter={e => {
                                    e.currentTarget.style.paddingLeft = '0.85rem';
                                    e.currentTarget.style.background = 'rgba(249,115,22,0.025)';
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
                                            display: 'flex',
                                            alignItems: 'center',
                                            justifyContent: 'center',
                                            fontSize: '0.85rem',
                                            fontWeight: 700,
                                            color: '#fff',
                                            fontFamily: "'Playfair Display', serif",
                                            background: AVATAR_GRADIENTS[idx % AVATAR_GRADIENTS.length],
                                            flexShrink: 0,
                                            boxShadow: '0 2px 8px rgba(0,0,0,0.06)',
                                        }}
                                    >
                                        {(user.nickname || user.username || '?').charAt(0).toUpperCase()}
                                    </span>
                                    <div style={{ minWidth: 0 }}>
                                        <span style={nameText}>{user.nickname || user.username}</span>
                                        <span style={metaText}>@{user.username}</span>
                                    </div>
                                </div>
                                <span style={timeBadge}>{formatRelativeTime(user.createTime ?? '')}</span>
                            </li>
                        ))}
                    </ul>
                ) : (
                    <div style={emptyStateBox}>
                        <div
                            style={emptyStateIcon(
                                'linear-gradient(135deg, rgba(249,115,22,0.06), rgba(195,155,211,0.04))'
                            )}
                        >
                            {'\u{1F4ED}'}
                        </div>
                        <div style={emptyStateTitle}>暂无用户记录</div>
                        <div style={emptyStateSubtitle}>当前暂无新注册用户</div>
                    </div>
                )}
            </div>
        </section>
    );
}
