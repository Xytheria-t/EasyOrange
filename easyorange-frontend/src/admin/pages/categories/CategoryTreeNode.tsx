import { Button } from '@/components/ui/button';
import type { CategoryTreeResponse } from '../../types/admin';

const levelLabels = ['', '一级', '二级', '三级'];

interface CategoryTreeNodeProps {
    node: CategoryTreeResponse;
    depth: number;
    expandedIds: Set<number>;
    onToggleExpand: (id: number) => void;
    onEdit: (node: CategoryTreeResponse) => void;
    onToggleStatus: (id: number, currentStatus: number) => void;
    onDelete: (node: CategoryTreeResponse) => void;
    isUpdatingStatus: boolean;
}

export function CategoryTreeNode({
    node,
    depth,
    expandedIds,
    onToggleExpand,
    onEdit,
    onToggleStatus,
    onDelete,
    isUpdatingStatus,
}: CategoryTreeNodeProps) {
    const hasChildren = node.children && node.children.length > 0;
    const isExpanded = expandedIds.has(Number(node.categoryId));
    const isEnabled = node.status === 1;
    const paddingLeft = 16 + depth * 28;

    return (
        <div key={node.categoryId}>
            {/* Node row */}
            <div
                className="category-tree-node-row"
                style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '0.5rem',
                    padding: '0.6rem 1rem 0.6rem 0',
                    paddingLeft,
                    borderBottom: '1px solid rgba(229,224,219,0.35)',
                    color: 'inherit',
                    font: 'inherit',
                    width: '100%',
                    textAlign: 'left',
                }}
            >
                {/* Expand/collapse */}
                <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    onClick={() => onToggleExpand(Number(node.categoryId))}
                    style={{
                        width: 20,
                        height: 20,
                        flexShrink: 0,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        border: 'none',
                        background: 'transparent',
                        color: hasChildren ? '#9B9590' : 'transparent',
                        transition: 'transform 0.2s ease',
                        transform: isExpanded ? 'rotate(90deg)' : 'rotate(0deg)',
                        visibility: hasChildren ? 'visible' : 'hidden',
                    }}
                    aria-label={isExpanded ? '折叠' : '展开'}
                >
                    <svg
                        aria-hidden="true"
                        width="12"
                        height="12"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="2.5"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                    >
                        <polyline points="9 18 15 12 9 6" />
                    </svg>
                </Button>

                {/* Level badge */}
                <span
                    style={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        minWidth: 32,
                        height: 20,
                        borderRadius: 6,
                        fontSize: '0.7rem',
                        fontWeight: 600,
                        color: depth === 0 ? '#EA580C' : depth === 1 ? '#2563EB' : '#7C3AED',
                        background:
                            depth === 0
                                ? 'rgba(249,115,22,0.08)'
                                : depth === 1
                                  ? 'rgba(37,99,235,0.08)'
                                  : 'rgba(124,58,237,0.08)',
                        flexShrink: 0,
                        whiteSpace: 'nowrap',
                    }}
                >
                    {levelLabels[depth] || `L${depth + 1}`}
                </span>

                {/* Name */}
                <span
                    style={{
                        flex: 1,
                        minWidth: 0,
                        fontSize: '0.89rem',
                        fontWeight: 600,
                        color: '#2A2520',
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap',
                    }}
                >
                    {node.name}
                </span>

                {/* Sort order */}
                {node.sortOrder > 0 && (
                    <span
                        style={{
                            fontSize: '0.76rem',
                            color: '#B5AEA8',
                            flexShrink: 0,
                            marginRight: '0.25rem',
                        }}
                    >
                        排序 {node.sortOrder}
                    </span>
                )}

                {/* Status */}
                <span
                    style={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: '0.3rem',
                        padding: '0.22rem 0.6rem',
                        borderRadius: 20,
                        fontSize: '0.75rem',
                        fontWeight: 600,
                        color: isEnabled ? '#059669' : '#9B9590',
                        background: isEnabled ? 'rgba(5,150,105,0.08)' : 'rgba(155,149,144,0.1)',
                        flexShrink: 0,
                        whiteSpace: 'nowrap',
                    }}
                >
                    <span
                        style={{
                            width: 6,
                            height: 6,
                            borderRadius: '50%',
                            background: isEnabled ? '#10B981' : '#D6CEC5',
                        }}
                    />
                    {isEnabled ? '启用' : '禁用'}
                </span>

                {/* Actions */}
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.3rem', flexShrink: 0 }}>
                    {/* Toggle status */}
                    <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        title={isEnabled ? '禁用' : '启用'}
                        onClick={() => onToggleStatus(Number(node.categoryId), node.status)}
                        disabled={isUpdatingStatus}
                        style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            width: 30,
                            height: 30,
                            borderRadius: 8,
                            border: 'none',
                            background: 'transparent',
                            color: isEnabled ? '#F59E0B' : '#10B981',
                            transition: 'all 0.15s ease',
                            opacity: isUpdatingStatus ? 0.5 : 1,
                        }}
                        onMouseEnter={e => {
                            e.currentTarget.style.background = 'rgba(42,37,32,0.05)';
                        }}
                        onMouseLeave={e => {
                            e.currentTarget.style.background = 'transparent';
                        }}
                    >
                        {isEnabled ? (
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
                                <path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24" />
                                <line x1="1" y1="1" x2="23" y2="23" />
                            </svg>
                        ) : (
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
                                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                                <circle cx="12" cy="12" r="3" />
                            </svg>
                        )}
                    </Button>

                    {/* Edit */}
                    <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        title="编辑"
                        onClick={() => onEdit(node)}
                        style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            width: 30,
                            height: 30,
                            borderRadius: 8,
                            border: 'none',
                            background: 'transparent',
                            color: '#6B6460',
                            transition: 'all 0.15s ease',
                        }}
                        onMouseEnter={e => {
                            e.currentTarget.style.background = 'rgba(249,115,22,0.08)';
                            e.currentTarget.style.color = '#EA580C';
                        }}
                        onMouseLeave={e => {
                            e.currentTarget.style.background = 'transparent';
                            e.currentTarget.style.color = '#6B6460';
                        }}
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
                            <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7" />
                            <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z" />
                        </svg>
                    </Button>

                    {/* Delete */}
                    <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        title="删除"
                        onClick={() => onDelete(node)}
                        style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            width: 30,
                            height: 30,
                            borderRadius: 8,
                            border: 'none',
                            background: 'transparent',
                            color: '#6B6460',
                            transition: 'all 0.15s ease',
                        }}
                        onMouseEnter={e => {
                            e.currentTarget.style.background = 'rgba(244,63,94,0.08)';
                            e.currentTarget.style.color = '#E11D48';
                        }}
                        onMouseLeave={e => {
                            e.currentTarget.style.background = 'transparent';
                            e.currentTarget.style.color = '#6B6460';
                        }}
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
                            <polyline points="3 6 5 6 21 6" />
                            <path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2" />
                        </svg>
                    </Button>
                </div>
            </div>

            {/* Children */}
            {hasChildren && isExpanded && (
                <div>
                    {node.children.map((child: CategoryTreeResponse) => (
                        <CategoryTreeNode
                            key={child.categoryId}
                            node={child}
                            depth={depth + 1}
                            expandedIds={expandedIds}
                            onToggleExpand={onToggleExpand}
                            onEdit={onEdit}
                            onToggleStatus={onToggleStatus}
                            onDelete={onDelete}
                            isUpdatingStatus={isUpdatingStatus}
                        />
                    ))}
                </div>
            )}
        </div>
    );
}
