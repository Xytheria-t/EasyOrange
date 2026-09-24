import { BookOpen, Plus, RefreshCw } from 'lucide-react';
import { useState } from 'react';
import { Button } from '@/components/ui/button';
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import { Textarea } from '@/components/ui/textarea';
import { AdminField } from '../../components/AdminControls';
import { AdminCard, AdminPage, AdminPageHeader } from '../../components/AdminPage';
import { AdminTable } from '../../components/AdminTable';
import { ConfirmModal } from '../../components/ConfirmModal';
import { useAdminKnowledgeDocs, useCreateKnowledgeDoc, useDeleteKnowledgeDoc, useReindexKnowledge } from '../../hooks';
import { notify } from '../../notify';
import type { KnowledgeDoc } from '../../types/admin';

const STATUS_LABEL: Record<string, { text: string; color: string }> = {
    PENDING: { text: '待索引', color: 'var(--status-warning)' },
    INDEXED: { text: '已索引', color: 'var(--status-success)' },
    FAILED: { text: '失败', color: 'var(--status-error)' },
};

const PAGE_SIZE = 10;

export default function KnowledgePage() {
    const [pageNum, setPageNum] = useState(1);
    const { data, isLoading, isError, error, refetch } = useAdminKnowledgeDocs(pageNum, PAGE_SIZE);
    const createMutation = useCreateKnowledgeDoc();
    const deleteMutation = useDeleteKnowledgeDoc();
    const reindexMutation = useReindexKnowledge();

    const [createOpen, setCreateOpen] = useState(false);
    const [title, setTitle] = useState('');
    const [content, setContent] = useState('');
    const [source, setSource] = useState('');
    const [deleteTarget, setDeleteTarget] = useState<KnowledgeDoc | null>(null);

    function handleCreate() {
        if (!title.trim() || !content.trim()) {
            return;
        }
        createMutation.mutate(
            { title: title.trim(), content: content.trim(), source: source.trim() || '运营' },
            {
                onSuccess: () => {
                    notify.success('文档已提交，正在分块并写入索引');
                    setCreateOpen(false);
                    setTitle('');
                    setContent('');
                    setSource('');
                },
                onError: e => notify.failure(e, '文档摄入失败，请重试'),
            }
        );
    }

    function handleReindex() {
        reindexMutation.mutate(undefined, {
            onSuccess: () => notify.success('补索引任务已提交'),
            onError: e => notify.failure(e, '补索引失败，请重试'),
        });
    }

    async function handleDelete() {
        if (!deleteTarget) {
            return;
        }
        try {
            await deleteMutation.mutateAsync(deleteTarget.id);
            notify.success(`已删除「${deleteTarget.title}」`);
            setDeleteTarget(null);
        } catch (e) {
            // 确认框保留，让用户看到后端拒绝的原因
            notify.failure(e, '删除失败，请重试');
        }
    }

    return (
        <AdminPage>
            <AdminPageHeader
                icon={<BookOpen size={17} />}
                title="知识库管理"
                description="RAG 文档摄入管线：新增文档自动分块 → Embedding → ES 索引，聊天引用溯源的数据源"
                actions={
                    <>
                        <Button
                            variant="outline"
                            onClick={handleReindex}
                            disabled={reindexMutation.isPending}
                            isLoading={reindexMutation.isPending}
                            loadingText="补索引中"
                        >
                            {!reindexMutation.isPending ? <RefreshCw size={15} aria-hidden="true" /> : null}
                            补索引
                        </Button>
                        <Button onClick={() => setCreateOpen(true)}>
                            <Plus size={15} aria-hidden="true" />
                            新增文档
                        </Button>
                    </>
                }
            />

            <AdminCard grow>
                <AdminTable<KnowledgeDoc>
                    columns={[
                        {
                            key: 'title',
                            title: '标题',
                            render: value => (
                                <span
                                    className="truncate block"
                                    style={{ fontWeight: 600, color: 'var(--admin-ink)', maxWidth: 280 }}
                                >
                                    {(value as string) || '（无标题）'}
                                </span>
                            ),
                        },
                        {
                            key: 'source',
                            title: '来源',
                            render: value => <span className="admin-muted">{(value as string) || '—'}</span>,
                        },
                        {
                            key: 'status',
                            title: '状态',
                            render: (_value, record) => {
                                // 此前未知状态回落到「待索引」，把后端新状态伪装成排队中
                                const status = STATUS_LABEL[String(record.status)];
                                const dotColor = status?.color ?? 'var(--status-default)';
                                return (
                                    <span
                                        style={{
                                            display: 'inline-flex',
                                            alignItems: 'center',
                                            gap: '0.35rem',
                                            padding: '0.22rem 0.6rem',
                                            borderRadius: 'var(--admin-radius-pill)',
                                            fontSize: '0.75rem',
                                            fontWeight: 600,
                                            color: dotColor,
                                            background: status
                                                ? `color-mix(in srgb, ${dotColor} 10%, transparent)`
                                                : 'var(--status-default-bg)',
                                        }}
                                    >
                                        <span className="admin-status-dot" style={{ background: dotColor }} />
                                        {status?.text ?? String(record.status ?? '未知')}
                                    </span>
                                );
                            },
                        },
                        {
                            key: 'chunkCount',
                            title: '分块数',
                            render: value => <span className="admin-muted">{Number(value ?? 0)} 块</span>,
                        },
                        {
                            key: 'createTime',
                            title: '创建时间',
                            render: value => <span className="admin-muted">{value as string}</span>,
                        },
                        {
                            key: 'actions',
                            title: '操作',
                            render: (_, record) => (
                                <Button
                                    variant="ghost"
                                    size="sm"
                                    onClick={() => setDeleteTarget(record)}
                                    className="h-auto min-h-0"
                                    style={{
                                        color: 'var(--admin-danger)',
                                        fontSize: '0.8rem',
                                        fontWeight: 600,
                                        padding: '0.32rem 0.7rem',
                                    }}
                                >
                                    删除
                                </Button>
                            ),
                        },
                    ]}
                    data={data?.records ?? []}
                    rowKey="id"
                    loading={isLoading}
                    error={isError ? error : null}
                    onRetry={() => refetch()}
                    pagination={{
                        current: data?.current ?? 1,
                        // 统一用客户端 pageSize：此前用 data.size，分页容量会随接口返回漂移
                        pageSize: PAGE_SIZE,
                        total: data?.total ?? 0,
                        onChange: setPageNum,
                    }}
                    emptyText="暂无知识库文档"
                />
            </AdminCard>

            <Dialog
                open={createOpen}
                onOpenChange={open => {
                    // 摄入中不允许关弹窗，否则用户看不到结果
                    if (!open && !createMutation.isPending) {
                        setCreateOpen(false);
                    }
                }}
            >
                <DialogContent className="max-w-lg">
                    <DialogHeader>
                        <DialogTitle>新增知识库文档</DialogTitle>
                        <DialogDescription>保存后系统自动分块、向量化并写入 ES 索引</DialogDescription>
                    </DialogHeader>
                    <div className="space-y-4">
                        <AdminField label="标题" required>
                            {props => (
                                <input
                                    {...props}
                                    type="text"
                                    value={title}
                                    onChange={e => setTitle(e.target.value)}
                                    placeholder="如：平台交易流程"
                                    disabled={createMutation.isPending}
                                    className="admin-input"
                                />
                            )}
                        </AdminField>
                        <AdminField label="来源" hint="留空默认记为「运营」">
                            {props => (
                                <input
                                    {...props}
                                    type="text"
                                    value={source}
                                    onChange={e => setSource(e.target.value)}
                                    placeholder="如：平台规则"
                                    disabled={createMutation.isPending}
                                    className="admin-input"
                                />
                            )}
                        </AdminField>
                        <AdminField label="正文（Markdown / 纯文本）" required>
                            {props => (
                                <Textarea
                                    {...props}
                                    value={content}
                                    onChange={e => setContent(e.target.value)}
                                    rows={8}
                                />
                            )}
                        </AdminField>
                    </div>
                    <DialogFooter>
                        <Button
                            variant="outline"
                            onClick={() => setCreateOpen(false)}
                            disabled={createMutation.isPending}
                        >
                            取消
                        </Button>
                        <Button
                            onClick={handleCreate}
                            disabled={!title.trim() || !content.trim() || createMutation.isPending}
                            isLoading={createMutation.isPending}
                            loadingText="摄入中"
                        >
                            保存并摄入
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>

            <ConfirmModal
                isOpen={deleteTarget !== null}
                title="删除知识库文档"
                content={`确认删除「${deleteTarget?.title ?? ''}」？将同步移除 ES 索引中的分块。`}
                confirmText="删除"
                isLoading={deleteMutation.isPending}
                onConfirm={handleDelete}
                onCancel={() => setDeleteTarget(null)}
            />
        </AdminPage>
    );
}
