import { BarChart3, RefreshCw } from 'lucide-react';
import { useState } from 'react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { formatDate } from '@/utils/format';
import { AdminListCard, AdminListCount, AdminPage, AdminPageHeader } from '../../components/AdminPage';
import { AdminSelect } from '../../components/AdminSelect';
import { AdminTable } from '../../components/AdminTable';
import { useAdminRetrievalEvalCases, useAdminRetrievalEvalRuns } from '../../hooks';
import type { RetrievalEvalLine } from '../../types/admin';

const PAGE_SIZE = 10;

const LINE_OPTIONS: { value: RetrievalEvalLine; label: string }[] = [
    { value: 'KNOWLEDGE', label: '知识库检索（真实 embedding）' },
    { value: 'ASSET', label: '找货检索（合成向量语料）' },
];

export default function RetrievalQualityPage() {
    const [line, setLine] = useState<RetrievalEvalLine>('KNOWLEDGE');
    const [selectedRunId, setSelectedRunId] = useState<string | null>(null);
    const [casePage, setCasePage] = useState(1);

    const runsQuery = useAdminRetrievalEvalRuns(line);
    const runs = runsQuery.data ?? [];
    const casesQuery = useAdminRetrievalEvalCases(selectedRunId, casePage, PAGE_SIZE);

    function handleLineChange(next: string) {
        setLine(next as RetrievalEvalLine);
        // 评测线换了，旧批次的明细与新线不是同一批数据，选中态要跟着清掉
        setSelectedRunId(null);
        setCasePage(1);
    }

    const selectedRun = runs.find(r => r.runId === selectedRunId);

    return (
        <AdminPage>
            <AdminPageHeader
                icon={<BarChart3 className="admin-header-icon" aria-hidden="true" />}
                title="检索质量回看"
                description="按评测批次回看 hit@5 / MRR 趋势，并下钻到用例级明细 —— 调分块、topK、RRF 的 k 之后拿历史批次对比，而不是靠手感"
                actions={
                    <Button
                        variant="outline"
                        size="sm"
                        onClick={() => {
                            runsQuery.refetch();
                            casesQuery.refetch();
                        }}
                        disabled={runsQuery.isFetching}
                    >
                        <RefreshCw className="admin-btn-icon" aria-hidden="true" />
                        {runsQuery.isFetching ? '刷新中' : '刷新'}
                    </Button>
                }
            />

            <AdminListCard
                title="评测批次"
                icon={<BarChart3 aria-hidden="true" />}
                count={
                    <>
                        <AdminListCount prefix="共" count={String(runs.length)} suffix="批" />
                        <span className="admin-muted">
                            hit@5 与 MRR
                            只统计融合腿（生产路径），单腿属消融日志不落表；两条评测线语料空间不同，需分列比较
                        </span>
                    </>
                }
                toolbar={<AdminSelect options={LINE_OPTIONS} value={line} onChange={handleLineChange} minWidth={240} />}
            >
                <AdminTable
                    columns={[
                        {
                            key: 'runId',
                            title: '批次',
                            render: value => (
                                <span className="admin-cell-strong admin-cell-clamp" title={String(value)}>
                                    {String(value).slice(0, 8)}
                                </span>
                            ),
                        },
                        { key: 'lineLabel', title: '评测线' },
                        {
                            key: 'caseCount',
                            title: '用例数',
                            render: value => <span className="admin-muted">{String(value)}</span>,
                        },
                        {
                            key: 'hitRatePct',
                            title: 'hit@5',
                            render: value => <span className="admin-cell-strong">{formatPct(Number(value))}</span>,
                        },
                        {
                            key: 'mrr',
                            title: 'MRR',
                            render: value => <span className="admin-muted">{Number(value).toFixed(4)}</span>,
                        },
                        {
                            key: 'createdAt',
                            title: '采样时间',
                            render: value => (
                                <span className="admin-muted">
                                    {value ? formatDate(String(value), 'datetime') : '—'}
                                </span>
                            ),
                        },
                    ]}
                    data={runs}
                    rowKey="runId"
                    loading={runsQuery.isLoading}
                    error={runsQuery.isError ? runsQuery.error : null}
                    onRetry={() => runsQuery.refetch()}
                    onRowClick={record => {
                        setSelectedRunId(record.runId);
                        setCasePage(1);
                    }}
                    emptyText="暂无评测批次 —— 跑一次金标准检索评测（ai-eval 工作流）后这里才有数据"
                />
            </AdminListCard>

            <AdminListCard
                title="用例明细"
                icon={<BarChart3 aria-hidden="true" />}
                count={
                    selectedRun ? (
                        <span className="admin-muted">
                            批次 {selectedRun.runId.slice(0, 8)} · hit@5 {formatPct(selectedRun.hitRatePct)} · MRR{' '}
                            {selectedRun.mrr.toFixed(4)} · 未命中排前
                        </span>
                    ) : undefined
                }
            >
                <AdminTable
                    columns={[
                        { key: 'caseId', title: '用例' },
                        {
                            key: 'queryText',
                            title: '查询',
                            render: value => (
                                <span className="admin-cell-clamp" title={String(value ?? '')}>
                                    {value ? String(value) : '—'}
                                </span>
                            ),
                        },
                        {
                            key: 'goldDocIds',
                            title: '期望命中',
                            render: value => <span className="admin-muted">{value ? String(value) : '—'}</span>,
                        },
                        {
                            key: 'hitAt5',
                            title: '结果',
                            render: value =>
                                value ? (
                                    <Badge variant="success">命中</Badge>
                                ) : (
                                    <Badge variant="destructive">未命中</Badge>
                                ),
                        },
                        {
                            key: 'hitRank',
                            title: '命中位次',
                            render: value => <span className="admin-muted">{value ? `第 ${value} 位` : '—'}</span>,
                        },
                    ]}
                    data={casesQuery.data?.records ?? []}
                    rowKey="caseId"
                    loading={casesQuery.isLoading}
                    error={casesQuery.isError ? casesQuery.error : null}
                    onRetry={() => casesQuery.refetch()}
                    pagination={
                        selectedRun
                            ? {
                                  current: casesQuery.data?.current ?? 1,
                                  pageSize: PAGE_SIZE,
                                  total: casesQuery.data?.total ?? 0,
                                  onChange: setCasePage,
                              }
                            : undefined
                    }
                    emptyText={selectedRun ? '该批次没有采样记录' : '先在上方选择一个批次查看用例级明细'}
                />
            </AdminListCard>
        </AdminPage>
    );
}

/** hit@5 按百分数展示一位小数；后端已下发 hitRatePct，这里只做取整口径。 */
function formatPct(pct: number): string {
    return `${pct.toFixed(1)}%`;
}
