/**
 * AdminTable 全量重渲染成本测量。
 *
 * 回答的问题：管理端列表一次「数据没变、只是父组件重渲染」要付多少代价？
 * 这是 React.memo / React Compiler 能省掉的那部分——若它本身就便宜，两者都不必上。
 *
 * 口径（与 doc/工程指标.md 性能节一致）：
 *   - 指标 = 一次多余重渲染的墙钟耗时中位数 + 单元格 render 调用数
 *   - 样本 = 每档行数预热 5 次后取 30 次中位数
 *   - 复现 = cd easyorange-frontend && npm run bench:render
 *
 * 边界：jsdom 无布局与绘制，测的是 React reconciliation + DOM 变更提交，
 * 不含浏览器 paint/layout。结论只能用于「重渲染本身贵不贵」，
 * 不能当作 INP / FCP 之类的用户侧指标。
 */
import { act, render } from '@testing-library/react';
import { Profiler, useState } from 'react';
import { describe, expect, it } from 'vitest';
import { AdminTable, type Column } from './AdminTable';

interface Row {
    id: string;
    name: string;
    price: number;
    status: string;
    createTime: string;
}

const WARMUP = 20;
const SAMPLES = 200;

/** 列渲染函数的调用次数 = 一次重渲染实际付出的单元格工作量 */
let cellRenders = 0;

function makeColumns(): Column<Row>[] {
    const bump = (node: React.ReactNode) => {
        cellRenders += 1;
        return node;
    };
    return [
        { key: 'name', title: '商品名称', render: v => bump(<span className="truncate">{v as string}</span>) },
        {
            key: 'price',
            title: '价格',
            render: v => bump(<span className="admin-price">¥{Number(v).toFixed(2)}</span>),
        },
        { key: 'status', title: '状态', render: v => bump(<span className="admin-muted">{v as string}</span>) },
        { key: 'createTime', title: '发布时间', render: v => bump(<span className="admin-muted">{v as string}</span>) },
    ];
}

function makeRows(count: number): Row[] {
    return Array.from({ length: count }, (_, i) => ({
        id: `p${i}`,
        name: `测试商品 ${i}`,
        price: 1000 + i,
        status: 'ONLINE',
        createTime: '2026-09-20T10:00:00Z',
    }));
}

/** 父组件持一个只用于「逼自己重渲染」的 state；data 引用始终不变 */
function Harness({ rows, onRender }: { rows: Row[]; onRender: (actualDuration: number) => void }) {
    const [, setTick] = useState(0);
    // 与 ProductReviewPage 等页面一致：columns 每次渲染重建，pagination 也是内联字面量
    const columns = makeColumns();
    return (
        <Profiler id="table" onRender={(_id, _phase, actualDuration) => onRender(actualDuration)}>
            <div data-testid="harness">
                <button type="button" onClick={() => setTick(t => t + 1)}>
                    rerender
                </button>
                <AdminTable
                    columns={columns}
                    data={rows}
                    rowKey="id"
                    pagination={{ current: 1, pageSize: 10, total: rows.length * 5, onChange: () => {} }}
                />
            </div>
        </Profiler>
    );
}

function median(values: number[]): number {
    const sorted = [...values].sort((a, b) => a - b);
    const mid = Math.floor(sorted.length / 2);
    return sorted.length % 2 === 0 ? (sorted[mid - 1] + sorted[mid]) / 2 : sorted[mid];
}

function measure(rowCount: number) {
    const rows = makeRows(rowCount);
    const samples: number[] = [];
    const { getByText, unmount } = render(<Harness rows={rows} onRender={d => samples.push(d)} />);
    const button = getByText('rerender');

    for (let i = 0; i < WARMUP; i++) {
        act(() => {
            button.click();
        });
    }

    // Profiler 首次挂载也会回调，丢弃预热阶段采到的
    samples.length = 0;
    cellRenders = 0;
    for (let i = 0; i < SAMPLES; i++) {
        act(() => {
            button.click();
        });
    }
    const cellsPerRender = cellRenders / SAMPLES;
    unmount();
    return { median: median(samples), min: Math.min(...samples), cellsPerRender };
}

describe('AdminTable 多余重渲染成本', () => {
    const results: Array<{ rows: number; median: number; min: number; cellsPerRender: number }> = [];

    for (const rowCount of [10, 20, 50]) {
        it(`${rowCount} 行：数据不变、仅父组件重渲染`, () => {
            const r = measure(rowCount);
            results.push({ rows: rowCount, ...r });
            // 单元格数 = 行数 × 列数，说明「无谓重渲染」确实在真实发生
            expect(r.cellsPerRender).toBe(rowCount * 4);
        });
    }

    it('汇总', () => {
        const frame = 1000 / 60;
        const lines = results.map(
            r =>
                `  ${String(r.rows).padStart(2)} 行 | 中位 ${r.median.toFixed(3)} ms | 最低 ${r.min.toFixed(3)} ms | ` +
                `单元格 ${r.cellsPerRender} 次 | 中位数下单帧(16.7ms) 内可发生 ${Math.floor(frame / r.median)} 次`
        );
        // biome-ignore lint/suspicious/noConsole: bench 需输出结果供人工读取
        console.log(
            `\n[AdminTable] 一次「数据未变」的重渲染（Profiler actualDuration，jsdom，n=${SAMPLES}）\n` +
                `${lines.join('\n')}\n`
        );
        expect(results.length).toBe(3);
    });
});
