import { ArrowDownUp, FolderTree, Plus } from 'lucide-react';
import { useCallback, useMemo, useState } from 'react';
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui';
import { Button } from '@/components/ui/button';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import { AdminField, AdminFilterField, AdminSearchInput } from '../../components/AdminControls';
import {
    AdminErrorBanner,
    AdminListCard,
    AdminListCount,
    AdminPage,
    AdminPageHeader,
} from '../../components/AdminPage';
import { AdminSelect } from '../../components/AdminSelect';
import { ConfirmModal } from '../../components/ConfirmModal';
import {
    useAdminCategoryTree,
    useCreateCategory,
    useDeleteCategory,
    useUpdateCategory,
    useUpdateCategoryStatus,
} from '../../hooks';
import { notify } from '../../notify';
import type { CategoryCreateRequest, CategoryResponse, CategoryUpdateRequest } from '../../types/admin';
import { CategoryTreeNode } from './CategoryTreeNode';

type SortField = 'name' | 'sortOrder';
type SortDir = 'asc' | 'desc';

const STATUS_FILTER_OPTIONS = [
    { value: '', label: '全部状态' },
    { value: '1', label: '启用' },
    { value: '0', label: '禁用' },
];

const SORT_FIELD_OPTIONS = [
    { value: 'sortOrder', label: '按排序值' },
    { value: 'name', label: '按名称' },
];

/** 压平分类树，同时记下每个节点的父级 id。 */
function flatten(nodes: CategoryResponse[]) {
    const flat: CategoryResponse[] = [];
    const parentIdById = new Map<string, string | null>();

    const walk = (list: CategoryResponse[], parentId: string | null) => {
        for (const node of list) {
            flat.push(node);
            parentIdById.set(node.id, parentId);
            if (node.children?.length) {
                walk(node.children, node.id);
            }
        }
    };

    walk(nodes, null);
    return { flat, parentIdById };
}

export default function CategoryManagePage() {
    const { data: treeData, isLoading, isError, error, refetch } = useAdminCategoryTree();
    const createMutation = useCreateCategory();
    const updateMutation = useUpdateCategory();
    const updateStatusMutation = useUpdateCategoryStatus();
    const deleteMutation = useDeleteCategory();

    const [statusFilter, setStatusFilter] = useState('');
    const [searchInput, setSearchInput] = useState('');
    const [sortField, setSortField] = useState<SortField>('sortOrder');
    const [sortDir, setSortDir] = useState<SortDir>('asc');
    const [expandedIds, setExpandedIds] = useState<Set<string>>(new Set());
    const [updatingStatusId, setUpdatingStatusId] = useState<string | null>(null);

    // Create modal
    const [createOpen, setCreateOpen] = useState(false);
    const [createName, setCreateName] = useState('');
    const [createParentId, setCreateParentId] = useState<string | undefined>(undefined);
    const [createSortOrder, setCreateSortOrder] = useState(0);

    // Edit modal
    const [editOpen, setEditOpen] = useState(false);
    const [editId, setEditId] = useState<string | null>(null);
    const [editName, setEditName] = useState('');
    const [editParentId, setEditParentId] = useState<string | undefined>(undefined);
    const [editSortOrder, setEditSortOrder] = useState(0);
    const [editStatus, setEditStatus] = useState(1);

    // Delete confirm
    const [deleteTarget, setDeleteTarget] = useState<CategoryResponse | null>(null);

    const toggleExpand = useCallback((id: string) => {
        setExpandedIds(prev => {
            const next = new Set(prev);
            if (next.has(id)) {
                next.delete(id);
            } else {
                next.add(id);
            }
            return next;
        });
    }, []);

    const { flat: allCategories, parentIdById } = useMemo(() => flatten(treeData ?? []), [treeData]);

    const filteredTree = useMemo(() => {
        if (!treeData) {
            return [];
        }

        const filterNode = (node: CategoryResponse): CategoryResponse | null => {
            const matchStatus = !statusFilter || node.status === Number(statusFilter);
            const matchSearch = !searchInput || node.name.toLowerCase().includes(searchInput.toLowerCase());
            const selfMatch = matchStatus && matchSearch;

            const filteredChildren =
                node.children?.map(filterNode).filter((n): n is CategoryResponse => n !== null) ?? [];

            if (!selfMatch && filteredChildren.length === 0) {
                return null;
            }
            return { ...node, children: filteredChildren };
        };

        const sortNodes = (nodes: CategoryResponse[]): CategoryResponse[] =>
            [...nodes]
                .sort((a, b) => {
                    const cmp = sortField === 'name' ? a.name.localeCompare(b.name) : a.sortOrder - b.sortOrder;
                    return sortDir === 'asc' ? cmp : -cmp;
                })
                .map(n => ({ ...n, children: n.children ? sortNodes(n.children) : [] }));

        return sortNodes(treeData.map(filterNode).filter((n): n is CategoryResponse => n !== null));
    }, [treeData, statusFilter, searchInput, sortField, sortDir]);

    const filteredCount = useMemo(() => {
        let count = 0;
        const walk = (nodes: CategoryResponse[]) => {
            for (const node of nodes) {
                count += 1;
                if (node.children?.length) {
                    walk(node.children);
                }
            }
        };
        walk(filteredTree);
        return count;
    }, [filteredTree]);

    const handleCreate = useCallback(async () => {
        if (!createName.trim()) {
            return;
        }
        const data: CategoryCreateRequest = { name: createName.trim(), sortOrder: createSortOrder };
        if (createParentId) {
            data.parentId = createParentId;
        }
        try {
            await createMutation.mutateAsync(data);
            notify.success(`已创建分类「${createName.trim()}」`);
            setCreateOpen(false);
            setCreateName('');
            setCreateParentId(undefined);
            setCreateSortOrder(0);
        } catch (e) {
            // 不关弹窗：输入还在，用户改个名字就能重试
            notify.failure(e, '创建分类失败，请稍后重试');
        }
    }, [createName, createParentId, createSortOrder, createMutation]);

    const handleEdit = useCallback(async () => {
        if (!editId || !editName.trim()) {
            return;
        }
        const data: CategoryUpdateRequest = {
            name: editName.trim(),
            sortOrder: editSortOrder,
            status: editStatus,
            parentId: editParentId,
        };
        try {
            await updateMutation.mutateAsync({ id: editId, data });
            notify.success(`已更新分类「${editName.trim()}」`);
            setEditOpen(false);
            setEditId(null);
        } catch (e) {
            notify.failure(e, '保存分类失败，请稍后重试');
        }
    }, [editId, editName, editParentId, editSortOrder, editStatus, updateMutation]);

    const handleToggleStatus = useCallback(
        async (id: string, currentStatus: number) => {
            setUpdatingStatusId(id);
            try {
                await updateStatusMutation.mutateAsync({ id, status: currentStatus === 1 ? 0 : 1 });
            } catch (e) {
                notify.failure(e, '状态更新失败，请稍后重试');
            } finally {
                setUpdatingStatusId(null);
            }
        },
        [updateStatusMutation]
    );

    const handleDelete = useCallback(async () => {
        if (!deleteTarget) {
            return;
        }
        try {
            await deleteMutation.mutateAsync(deleteTarget.id);
            notify.success(`已删除分类「${deleteTarget.name}」`);
            setDeleteTarget(null);
        } catch (e) {
            // 保留确认框，让用户看到后端拒绝的原因
            notify.failure(e, '删除分类失败，请稍后重试');
        }
    }, [deleteTarget, deleteMutation]);

    const openEdit = useCallback(
        (node: CategoryResponse) => {
            setEditId(node.id);
            setEditName(node.name);
            setEditSortOrder(node.sortOrder);
            setEditStatus(node.status);
            // 此前固定写 undefined：编辑任何子分类都会被当成一级分类保存
            setEditParentId(parentIdById.get(node.id) ?? undefined);
            setEditOpen(true);
        },
        [parentIdById]
    );

    const createParentOptions = useMemo(
        () => [
            { value: '', label: '无（一级分类）' },
            ...allCategories.map(cat => ({
                value: String(cat.id),
                label: `${'— '.repeat(cat.level || 0)}${cat.name}`,
            })),
        ],
        [allCategories]
    );

    // 编辑时排除自己与自己的后代，否则能把父级挂到子级下面形成环
    const editParentOptions = useMemo(() => {
        if (!editId) {
            return createParentOptions;
        }
        const excluded = new Set<string>([editId]);
        let grew = true;
        while (grew) {
            grew = false;
            for (const [childId, parentId] of parentIdById) {
                if (parentId && excluded.has(parentId) && !excluded.has(childId)) {
                    excluded.add(childId);
                    grew = true;
                }
            }
        }
        return createParentOptions.filter(opt => !excluded.has(opt.value));
    }, [createParentOptions, editId, parentIdById]);

    const hasFilter = Boolean(searchInput || statusFilter);

    return (
        <AdminPage>
            <AdminErrorBanner
                message={isError ? error?.message || '无法连接到服务器，请检查后端服务是否启动' : null}
                onRetry={() => refetch()}
                retrying={isLoading}
            />

            <AdminPageHeader
                icon={<FolderTree size={17} />}
                title="分类管理"
                description="管理商品分类结构，支持添加、编辑、删除操作"
                actions={
                    <Button type="button" onClick={() => setCreateOpen(true)}>
                        <Plus size={16} aria-hidden="true" />
                        添加分类
                    </Button>
                }
            />

            <AdminListCard
                title="分类结构"
                icon={<FolderTree size={17} />}
                // 筛选后报总数会让人以为筛选没生效；请求失败时报「共 0 个」同样会被当成真的没数据
                count={
                    isError ? undefined : hasFilter ? (
                        <AdminListCount
                            prefix="筛选出"
                            count={filteredCount}
                            suffix={`/ ${allCategories.length} 个分类`}
                        />
                    ) : (
                        <AdminListCount prefix="共" count={allCategories.length.toLocaleString()} suffix="个分类" />
                    )
                }
                toolbar={
                    <>
                        <AdminSearchInput
                            value={searchInput}
                            onChange={setSearchInput}
                            placeholder="搜索分类名称"
                            loading={isLoading}
                            submitOnEnterOnly
                        />
                        <AdminFilterField
                            label="状态"
                            options={STATUS_FILTER_OPTIONS}
                            value={statusFilter}
                            onChange={setStatusFilter}
                            minWidth={120}
                        />
                        <AdminFilterField
                            label="排序"
                            options={SORT_FIELD_OPTIONS}
                            value={sortField}
                            onChange={val => setSortField(val as SortField)}
                            minWidth={130}
                        />
                        <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            onClick={() => setSortDir(d => (d === 'asc' ? 'desc' : 'asc'))}
                            className="self-end"
                            aria-label={sortDir === 'asc' ? '当前升序，切换为降序' : '当前降序，切换为升序'}
                            title={sortDir === 'asc' ? '升序' : '降序'}
                        >
                            <ArrowDownUp
                                size={14}
                                aria-hidden="true"
                                style={{ transform: sortDir === 'desc' ? 'rotate(180deg)' : 'none' }}
                            />
                            {sortDir === 'asc' ? '升序' : '降序'}
                        </Button>
                    </>
                }
            >
                <div className="admin-tree-scroll">
                    {isLoading ? (
                        <div className="admin-tree-state" role="status" aria-busy="true">
                            <div className="admin-spinner animate-spin" />
                            <p className="admin-muted">加载分类数据…</p>
                        </div>
                    ) : isError ? null : filteredTree.length === 0 ? (
                        <div className="admin-tree-state">
                            <span className="admin-tree-state-icon" aria-hidden="true">
                                <FolderTree size={28} />
                            </span>
                            <div className="admin-tree-state-title">暂无分类数据</div>
                            <p className="admin-muted">
                                {hasFilter ? '尝试调整筛选条件' : '点击右上角「添加分类」创建第一个分类'}
                            </p>
                        </div>
                    ) : (
                        <div>
                            {filteredTree.map(node => (
                                <CategoryTreeNode
                                    key={node.id}
                                    node={node}
                                    depth={0}
                                    expandedIds={expandedIds}
                                    onToggleExpand={toggleExpand}
                                    onEdit={openEdit}
                                    onToggleStatus={handleToggleStatus}
                                    onDelete={setDeleteTarget}
                                    updatingStatusId={updatingStatusId}
                                />
                            ))}
                        </div>
                    )}
                </div>
            </AdminListCard>

            {/* ===== Create Modal ===== */}
            <Dialog
                open={createOpen}
                onOpenChange={open => {
                    if (!open && !createMutation.isPending) {
                        setCreateOpen(false);
                    }
                }}
            >
                <DialogContent className="sm:max-w-[480px] gap-0 p-0 overflow-hidden rounded-3xl">
                    <DialogHeader className="p-6 pb-0">
                        <DialogTitle className="flex items-center gap-2">
                            <span aria-hidden="true" className="admin-dialog-icon">
                                <Plus size={14} />
                            </span>
                            添加分类
                        </DialogTitle>
                    </DialogHeader>

                    <div className="p-6 flex flex-col gap-4">
                        <AdminField label="分类名称" required>
                            {props => (
                                <input
                                    {...props}
                                    type="text"
                                    value={createName}
                                    onChange={e => setCreateName(e.target.value)}
                                    placeholder="请输入分类名称"
                                    maxLength={20}
                                    disabled={createMutation.isPending}
                                    className="admin-input"
                                />
                            )}
                        </AdminField>

                        <AdminField label="父级分类" hint="不选则为一级分类，最多支持三级分类">
                            {props => (
                                <AdminSelect
                                    id={props.id}
                                    aria-describedby={props['aria-describedby']}
                                    aria-invalid={props['aria-invalid']}
                                    options={createParentOptions}
                                    value={createParentId ?? ''}
                                    onChange={val => setCreateParentId(val || undefined)}
                                    disabled={createMutation.isPending}
                                />
                            )}
                        </AdminField>

                        <AdminField label="排序值" hint="数值越小越靠前">
                            {props => (
                                <input
                                    {...props}
                                    type="number"
                                    value={createSortOrder}
                                    onChange={e => setCreateSortOrder(Number(e.target.value))}
                                    min={0}
                                    max={9999}
                                    disabled={createMutation.isPending}
                                    className="admin-input"
                                />
                            )}
                        </AdminField>
                    </div>

                    <DialogFooter className="flex-row justify-end gap-2.5 p-4 border-t border-border/40">
                        <Button
                            type="button"
                            variant="outline"
                            onClick={() => setCreateOpen(false)}
                            disabled={createMutation.isPending}
                        >
                            取消
                        </Button>
                        <Button
                            type="button"
                            onClick={handleCreate}
                            disabled={createMutation.isPending || !createName.trim()}
                            isLoading={createMutation.isPending}
                            loadingText="创建中"
                        >
                            确认创建
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>

            {/* ===== Edit Modal ===== */}
            <Dialog
                open={editOpen && !!editId}
                onOpenChange={open => {
                    if (!open && !updateMutation.isPending) {
                        setEditOpen(false);
                    }
                }}
            >
                <DialogContent className="sm:max-w-[480px] gap-0 p-0 overflow-hidden rounded-3xl">
                    <DialogHeader className="p-6 pb-0">
                        <DialogTitle>编辑分类</DialogTitle>
                    </DialogHeader>

                    <div className="p-6 flex flex-col gap-4">
                        <AdminField label="分类名称" required>
                            {props => (
                                <input
                                    {...props}
                                    type="text"
                                    value={editName}
                                    onChange={e => setEditName(e.target.value)}
                                    placeholder="请输入分类名称"
                                    maxLength={20}
                                    disabled={updateMutation.isPending}
                                    className="admin-input"
                                />
                            )}
                        </AdminField>

                        <AdminField label="父级分类" hint="已排除自身及其下级，避免形成循环">
                            {props => (
                                <AdminSelect
                                    id={props.id}
                                    aria-describedby={props['aria-describedby']}
                                    aria-invalid={props['aria-invalid']}
                                    options={editParentOptions}
                                    value={editParentId ?? ''}
                                    onChange={val => setEditParentId(val || undefined)}
                                    disabled={updateMutation.isPending}
                                />
                            )}
                        </AdminField>

                        <AdminField label="排序值" hint="数值越小越靠前">
                            {props => (
                                <input
                                    {...props}
                                    type="number"
                                    value={editSortOrder}
                                    onChange={e => setEditSortOrder(Number(e.target.value))}
                                    min={0}
                                    max={9999}
                                    disabled={updateMutation.isPending}
                                    className="admin-input"
                                />
                            )}
                        </AdminField>

                        <div>
                            <p className="admin-label mb-[0.4rem]">状态</p>
                            <RadioGroup
                                value={String(editStatus)}
                                onValueChange={value => setEditStatus(Number(value))}
                                className="flex gap-3"
                            >
                                <label
                                    htmlFor="status-enabled"
                                    className={`admin-choice ${editStatus === 1 ? 'admin-choice--on-success' : ''}`}
                                >
                                    <RadioGroupItem value="1" id="status-enabled" />
                                    <span className="admin-choice-text">启用</span>
                                </label>
                                <label
                                    htmlFor="status-disabled"
                                    className={`admin-choice ${editStatus === 0 ? 'admin-choice--on-error' : ''}`}
                                >
                                    <RadioGroupItem value="0" id="status-disabled" />
                                    <span className="admin-choice-text">禁用</span>
                                </label>
                            </RadioGroup>
                        </div>
                    </div>

                    <DialogFooter className="flex-row justify-end gap-2.5 p-4 border-t border-border/40">
                        <Button
                            type="button"
                            variant="outline"
                            onClick={() => setEditOpen(false)}
                            disabled={updateMutation.isPending}
                        >
                            取消
                        </Button>
                        <Button
                            type="button"
                            onClick={handleEdit}
                            disabled={updateMutation.isPending || !editName.trim()}
                            isLoading={updateMutation.isPending}
                            loadingText="保存中"
                        >
                            保存修改
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>

            <ConfirmModal
                isOpen={deleteTarget !== null}
                title="删除分类"
                content={
                    deleteTarget
                        ? `确定要删除分类「${deleteTarget.name}」吗？如果该分类下有子分类或关联商品，将无法删除。`
                        : ''
                }
                confirmText="删除"
                cancelText="取消"
                variant="danger"
                isLoading={deleteMutation.isPending}
                onConfirm={handleDelete}
                onCancel={() => setDeleteTarget(null)}
            />
        </AdminPage>
    );
}
