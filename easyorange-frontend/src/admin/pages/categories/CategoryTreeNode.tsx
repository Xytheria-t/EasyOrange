import { ChevronRight, Eye, EyeOff, Pencil, Trash2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import type { CategoryTreeResponse } from '../../types/admin';

// 索引按 depth 取：depth=0 是一级。
// 此前写成 ['', '一级', ...] 再用 levelLabels[depth]，一级落到 L1、二级显示「一级」，整体错位一级。
const LEVEL_LABELS = ['一级', '二级', '三级', '更深'];

const LEVEL_TONES = [
    { color: 'var(--admin-accent)', background: 'color-mix(in srgb, var(--admin-accent-bright) 8%, transparent)' },
    { color: 'var(--status-info)', background: 'color-mix(in srgb, var(--status-info) 8%, transparent)' },
    { color: 'var(--plum-600)', background: 'color-mix(in srgb, var(--plum-600) 8%, transparent)' },
];

interface CategoryTreeNodeProps {
    node: CategoryTreeResponse;
    depth: number;
    expandedIds: Set<string>;
    onToggleExpand: (id: string) => void;
    onEdit: (node: CategoryTreeResponse) => void;
    onToggleStatus: (id: string, currentStatus: number) => void;
    onDelete: (node: CategoryTreeResponse) => void;
    /** 正在切换状态的分类 id。全局 pending 会让整棵树一起变灰，改为按行反馈。 */
    updatingStatusId: string | null;
}

export function CategoryTreeNode({
    node,
    depth,
    expandedIds,
    onToggleExpand,
    onEdit,
    onToggleStatus,
    onDelete,
    updatingStatusId,
}: CategoryTreeNodeProps) {
    const hasChildren = Boolean(node.children?.length);
    const isExpanded = expandedIds.has(node.categoryId);
    const isEnabled = node.status === 1;
    const isRowUpdating = updatingStatusId === node.categoryId;
    const tone = LEVEL_TONES[Math.min(depth, LEVEL_TONES.length - 1)];

    return (
        <div>
            <div className="category-tree-node-row admin-tree-row" style={{ paddingLeft: 8 + depth * 26 }}>
                <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    onClick={() => onToggleExpand(node.categoryId)}
                    className="admin-icon-button"
                    style={{
                        width: 22,
                        height: 22,
                        transform: isExpanded ? 'rotate(90deg)' : 'none',
                        transition: 'transform 180ms var(--ease-out)',
                        visibility: hasChildren ? 'visible' : 'hidden',
                    }}
                    aria-label={isExpanded ? `折叠分类 ${node.name}` : `展开分类 ${node.name}`}
                    aria-expanded={hasChildren ? isExpanded : undefined}
                >
                    <ChevronRight size={13} aria-hidden="true" />
                </Button>

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
                        color: tone.color,
                        background: tone.background,
                        flexShrink: 0,
                        whiteSpace: 'nowrap',
                    }}
                >
                    {LEVEL_LABELS[depth] ?? `L${depth + 1}`}
                </span>

                <span
                    className="admin-value"
                    style={{ flex: 1, minWidth: 0, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}
                >
                    {node.name}
                </span>

                {node.sortOrder > 0 ? (
                    <span className="admin-muted" style={{ flexShrink: 0 }}>
                        排序 {node.sortOrder}
                    </span>
                ) : null}

                {/* 状态：圆点 + 文字双编码，不靠颜色单独传达 */}
                <span
                    style={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: '0.35rem',
                        padding: '0.22rem 0.6rem',
                        borderRadius: 'var(--admin-radius-pill)',
                        fontSize: '0.75rem',
                        fontWeight: 600,
                        color: isEnabled ? 'var(--status-success)' : 'var(--status-default)',
                        background: isEnabled ? 'var(--status-success-bg)' : 'var(--status-default-bg)',
                        flexShrink: 0,
                        whiteSpace: 'nowrap',
                    }}
                >
                    <span
                        className="admin-status-dot"
                        style={{ background: isEnabled ? 'var(--status-success-dot)' : 'var(--status-default-dot)' }}
                    />
                    {isRowUpdating ? '更新中' : isEnabled ? '启用' : '禁用'}
                </span>

                <div style={{ display: 'flex', alignItems: 'center', gap: '0.15rem', flexShrink: 0 }}>
                    <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        onClick={() => onToggleStatus(node.categoryId, node.status)}
                        disabled={isRowUpdating}
                        className="admin-icon-button h-auto min-h-0"
                        style={{ color: isEnabled ? 'var(--warning)' : 'var(--status-success)' }}
                        aria-label={isEnabled ? `禁用分类 ${node.name}` : `启用分类 ${node.name}`}
                        title={isEnabled ? '禁用' : '启用'}
                    >
                        {isEnabled ? <EyeOff size={15} aria-hidden="true" /> : <Eye size={15} aria-hidden="true" />}
                    </Button>

                    <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        onClick={() => onEdit(node)}
                        className="h-auto min-h-0 admin-icon-button"
                        admin-icon-button
                        aria-label={`编辑分类 ${node.name}`}
                        title="编辑"
                    >
                        <Pencil size={15} aria-hidden="true" />
                    </Button>

                    <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        onClick={() => onDelete(node)}
                        className="admin-icon-button h-auto min-h-0"
                        style={{ color: 'var(--admin-danger)' }}
                        aria-label={`删除分类 ${node.name}`}
                        title="删除"
                    >
                        <Trash2 size={15} aria-hidden="true" />
                    </Button>
                </div>
            </div>

            {hasChildren && isExpanded ? (
                <div>
                    {node.children?.map(child => (
                        <CategoryTreeNode
                            key={child.categoryId}
                            node={child}
                            depth={depth + 1}
                            expandedIds={expandedIds}
                            onToggleExpand={onToggleExpand}
                            onEdit={onEdit}
                            onToggleStatus={onToggleStatus}
                            onDelete={onDelete}
                            updatingStatusId={updatingStatusId}
                        />
                    ))}
                </div>
            ) : null}
        </div>
    );
}
