import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { AdminTable } from '../../components/AdminTable';
import { ConfirmModal } from '../../components/ConfirmModal';
import { useAdminKnowledgeDocs, useCreateKnowledgeDoc, useDeleteKnowledgeDoc, useReindexKnowledge } from '../../hooks';
import type { KnowledgeDoc } from '../../types/admin';

const STATUS_LABEL: Record<KnowledgeDoc['status'], { text: string; tone: string }> = {
    PENDING: { text: '待索引', tone: 'bg-amber-100 text-amber-700' },
    INDEXED: { text: '已索引', tone: 'bg-emerald-100 text-emerald-700' },
    FAILED: { text: '失败', tone: 'bg-rose-100 text-rose-700' },
};

export default function KnowledgePage() {
    const [pageNum, setPageNum] = useState(1);
    const [pageSize] = useState(10);
    const { data, isLoading } = useAdminKnowledgeDocs(pageNum, pageSize);
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
                    setCreateOpen(false);
                    setTitle('');
                    setContent('');
                    setSource('');
                },
            }
        );
    }

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <div>
                    <h2 className="text-lg font-semibold">知识库管理</h2>
                    <p className="text-sm text-muted-foreground">
                        RAG 文档摄入管线：新增文档自动分块 → Embedding → ES 索引，聊天引用溯源的数据源
                    </p>
                </div>
                <div className="flex gap-2">
                    <Button
                        variant="outline"
                        onClick={() => reindexMutation.mutate()}
                        disabled={reindexMutation.isPending}
                    >
                        {reindexMutation.isPending ? '补索引中…' : '补索引'}
                    </Button>
                    <Button onClick={() => setCreateOpen(true)}>新增文档</Button>
                </div>
            </div>

            <AdminTable<KnowledgeDoc>
                columns={[
                    { key: 'title', title: '标题' },
                    { key: 'source', title: '来源' },
                    {
                        key: 'status',
                        title: '状态',
                        render: (_, record) => {
                            const status = STATUS_LABEL[record.status] ?? STATUS_LABEL.PENDING;
                            return (
                                <span
                                    className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${status.tone}`}
                                >
                                    {status.text}
                                </span>
                            );
                        },
                    },
                    { key: 'chunkCount', title: '分块数' },
                    { key: 'createTime', title: '创建时间' },
                    {
                        key: 'actions',
                        title: '操作',
                        render: (_, record) => (
                            <Button
                                variant="ghost"
                                size="sm"
                                className="text-rose-600"
                                onClick={() => setDeleteTarget(record)}
                            >
                                删除
                            </Button>
                        ),
                    },
                ]}
                data={data?.records ?? []}
                rowKey="id"
                loading={isLoading}
                pagination={{
                    current: data?.current ?? 1,
                    pageSize: data?.size ?? pageSize,
                    total: data?.total ?? 0,
                    onChange: setPageNum,
                }}
            />

            {createOpen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
                    <div className="w-full max-w-lg rounded-xl bg-white p-6 shadow-xl">
                        <h3 className="mb-4 text-base font-semibold">新增知识库文档</h3>
                        <div className="space-y-4">
                            <div className="space-y-1.5">
                                <Label htmlFor="kb-title">标题</Label>
                                <Input
                                    id="kb-title"
                                    value={title}
                                    onChange={e => setTitle(e.target.value)}
                                    placeholder="如：平台交易流程"
                                />
                            </div>
                            <div className="space-y-1.5">
                                <Label htmlFor="kb-source">来源</Label>
                                <Input
                                    id="kb-source"
                                    value={source}
                                    onChange={e => setSource(e.target.value)}
                                    placeholder="如：平台规则"
                                />
                            </div>
                            <div className="space-y-1.5">
                                <Label htmlFor="kb-content">正文（Markdown / 纯文本）</Label>
                                <Textarea
                                    id="kb-content"
                                    value={content}
                                    onChange={e => setContent(e.target.value)}
                                    rows={8}
                                    placeholder="输入文档正文，系统会自动分块并向量化…"
                                />
                            </div>
                        </div>
                        <div className="mt-5 flex justify-end gap-2">
                            <Button variant="outline" onClick={() => setCreateOpen(false)}>
                                取消
                            </Button>
                            <Button
                                onClick={handleCreate}
                                disabled={!title.trim() || !content.trim() || createMutation.isPending}
                            >
                                {createMutation.isPending ? '摄入中…' : '保存并摄入'}
                            </Button>
                        </div>
                    </div>
                </div>
            )}

            <ConfirmModal
                isOpen={deleteTarget !== null}
                title="删除知识库文档"
                content={`确认删除「${deleteTarget?.title ?? ''}」？将同步移除 ES 索引中的分块。`}
                confirmText="删除"
                onConfirm={() => {
                    if (deleteTarget) {
                        deleteMutation.mutate(deleteTarget.id);
                    }
                    setDeleteTarget(null);
                }}
                onCancel={() => setDeleteTarget(null)}
            />
        </div>
    );
}
