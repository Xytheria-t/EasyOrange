import { ChevronLeft, ChevronRight, Inbox } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { Pagination, PaginationContent, PaginationEllipsis, PaginationItem } from '@/components/ui/pagination';
import { cn } from '@/lib/utils';

export interface Column<T> {
    key: keyof T | string;
    title: string;
    render?: (value: unknown, record: T) => React.ReactNode;
}

export interface AdminTableSelection<T> {
    /** 已勾选行的 rowKey 字符串集合 */
    selectedKeys: ReadonlySet<string>;
    onChange: (keys: string[]) => void;
    /** 只让这些行可选（如仅「待审核」可批量审核）；不传则本页全可选 */
    isSelectable?: (record: T) => boolean;
    /** 复选框可访问名的主体，如「商品」→「选择商品 <名称>」 */
    noun: string;
    labelOf: (record: T) => string;
}

export interface AdminTableProps<T> {
    columns: Column<T>[];
    data: T[];
    rowKey: keyof T;
    loading?: boolean;
    /** 列表请求失败。传了 error 就渲染重试出口，不会退化成"暂无数据"。 */
    error?: Error | null;
    onRetry?: () => void;
    pagination?: {
        current: number;
        pageSize: number;
        total: number;
        onChange: (page: number) => void;
    };
    onRowClick?: (record: T) => void;
    /** 批量操作的行勾选。传了才出现勾选列。 */
    selection?: AdminTableSelection<T>;
    emptyText?: string;
}

/**
 * 页码序列：≤7 页全列，两侧溢出用省略号。
 *
 * 这里原先包了一层 `useMemo([pagination])`，但分页对象是调用方内联的字面量，
 * 引用每次渲染都变——那个 memo 从未命中。算几条页码是纳秒级，直接算即可。
 */
function buildPageNumbers(current: number, pageSize: number, total: number): (number | 'ellipsis')[] {
    const totalPages = Math.ceil(total / pageSize);
    const pages: (number | 'ellipsis')[] = [];
    if (totalPages <= 7) {
        for (let i = 1; i <= totalPages; i++) {
            pages.push(i);
        }
        return pages;
    }
    pages.push(1);
    if (current > 3) {
        pages.push('ellipsis');
    }
    for (let i = Math.max(2, current - 1); i <= Math.min(totalPages - 1, current + 1); i++) {
        pages.push(i);
    }
    if (current < totalPages - 2) {
        pages.push('ellipsis');
    }
    pages.push(totalPages);
    return pages;
}

/**
 * 数据表。
 *
 * 两处刻意的取舍：
 * 1. 不再内建排序。此前表头排序只作用于当前页数据，而分页是服务端的——点「注册时间」
 *    排的是这 10 条，不是全量，视觉上却像已经按时间排好，比没有排序更容易误导。
 *    要真排序得先让后端支持 sort 参数，届时从这里接。
 * 2. 不再套一层滚动容器。共享的 shadcn `Table` 自带 `overflow-auto + border + rounded`，
 *    外面又包 `.admin-table-scroll`，窄屏会出现双滚动条、卡片里套一层多余的边框圆角。
 *    现在只有 `.admin-table-scroll` 一个滚动容器，卡片本身就是唯一表面。
 */
export function AdminTable<T extends object>({
    columns,
    data,
    rowKey,
    loading = false,
    error = null,
    onRetry,
    pagination,
    onRowClick,
    selection,
    emptyText = '暂无数据',
}: AdminTableProps<T>) {
    const getValue = (record: T, key: keyof T | string): unknown => {
        if (typeof key === 'string' && key.includes('.')) {
            let value: unknown = record;
            for (const k of key.split('.')) {
                value = (value as Record<string, unknown>)?.[k];
            }
            return value;
        }
        return record[key as keyof T];
    };

    const keyOf = (record: T): string => String(record[rowKey]);

    const selectableRecords = selection
        ? data.filter(record => (selection.isSelectable ? selection.isSelectable(record) : true))
        : [];
    const selectedOnPage = selectableRecords.filter(record => selection?.selectedKeys.has(keyOf(record)));
    const allSelected = selectableRecords.length > 0 && selectedOnPage.length === selectableRecords.length;
    const someSelected = selectedOnPage.length > 0 && !allSelected;

    /** 本页全选只增删本页可选行的 key，跨页已选的不会被清掉。 */
    const toggleAllOnPage = () => {
        if (!selection) {
            return;
        }
        const next = new Set(selection.selectedKeys);
        if (allSelected) {
            for (const record of selectableRecords) {
                next.delete(keyOf(record));
            }
        } else {
            for (const record of selectableRecords) {
                next.add(keyOf(record));
            }
        }
        selection.onChange([...next]);
    };

    const toggleRow = (record: T) => {
        if (!selection) {
            return;
        }
        const key = keyOf(record);
        const next = new Set(selection.selectedKeys);
        if (next.has(key)) {
            next.delete(key);
        } else {
            next.add(key);
        }
        selection.onChange([...next]);
    };

    const totalPages = pagination ? Math.ceil(pagination.total / pagination.pageSize) : 0;
    const pageNumbers = pagination ? buildPageNumbers(pagination.current, pagination.pageSize, pagination.total) : [];
    const columnCount = columns.length + (selection ? 1 : 0);

    return (
        <>
            {/* 窄屏横向滚动：列宽由内容撑开，不把订单号/金额压成两行 */}
            <div className="admin-table-scroll">
                <table className="admin-table">
                    <thead>
                        <tr>
                            {selection ? (
                                <th className="admin-table-head admin-table-head--check" scope="col">
                                    <Checkbox
                                        checked={allSelected ? true : someSelected ? 'indeterminate' : false}
                                        onCheckedChange={toggleAllOnPage}
                                        disabled={selectableRecords.length === 0}
                                        aria-label={`全选本页可操作的${selection.noun}`}
                                        className="admin-checkbox"
                                    />
                                </th>
                            ) : null}
                            {columns.map(column => (
                                <th key={String(column.key)} className="admin-table-head" scope="col">
                                    {column.title}
                                </th>
                            ))}
                        </tr>
                    </thead>
                    <tbody>
                        {loading ? (
                            <tr>
                                <td colSpan={columnCount} className="admin-table-cell">
                                    {/* 骨架行：占住与真实行相同的行高，数据到达时不跳版 */}
                                    <div className="admin-skeleton" role="status" aria-busy="true">
                                        <span className="sr-only">加载中</span>
                                        {Array.from({ length: 6 }, (_, i) => (
                                            // biome-ignore lint/suspicious/noArrayIndexKey: 静态占位，无状态
                                            <div key={i} className="admin-skeleton-row" aria-hidden="true">
                                                <span className="admin-skeleton-bar" style={{ width: '28%' }} />
                                                <span className="admin-skeleton-bar" style={{ width: '18%' }} />
                                                <span className="admin-skeleton-bar" style={{ width: '14%' }} />
                                            </div>
                                        ))}
                                    </div>
                                </td>
                            </tr>
                        ) : error ? (
                            <tr>
                                <td colSpan={columnCount} className="admin-table-cell">
                                    <div className="admin-table-state admin-table-state--error" role="alert">
                                        <div className="admin-table-state-title admin-table-state-title--error">
                                            数据加载失败
                                        </div>
                                        <div className="admin-table-state-message">
                                            {error.message || '服务暂时不可用，请稍后重试。'}
                                        </div>
                                        {onRetry ? (
                                            <Button variant="outline" size="sm" className="mt-2" onClick={onRetry}>
                                                重新加载
                                            </Button>
                                        ) : null}
                                    </div>
                                </td>
                            </tr>
                        ) : data.length === 0 ? (
                            <tr>
                                <td colSpan={columnCount} className="admin-table-cell">
                                    <div className="admin-table-state">
                                        <Inbox className="mx-auto mb-[0.65rem] h-9 w-9 opacity-40" aria-hidden="true" />
                                        {/* 只渲染 emptyText：再补一句「暂无相关数据」会和页面自带的空态文案重复 */}
                                        <div className="admin-table-state-title">{emptyText}</div>
                                    </div>
                                </td>
                            </tr>
                        ) : (
                            data.map(record => {
                                const selectable = selection ? (selection.isSelectable?.(record) ?? true) : false;
                                return (
                                    <tr
                                        key={keyOf(record)}
                                        className={cn(
                                            'admin-table-row',
                                            onRowClick && 'admin-table-row--clickable',
                                            selection?.selectedKeys.has(keyOf(record)) && 'admin-table-row--selected'
                                        )}
                                        tabIndex={onRowClick ? 0 : undefined}
                                        aria-selected={
                                            selection ? selection.selectedKeys.has(keyOf(record)) : undefined
                                        }
                                        onClick={() => onRowClick?.(record)}
                                        onKeyDown={e => {
                                            if (!onRowClick) {
                                                return;
                                            }
                                            if (e.key === 'Enter' || e.key === ' ') {
                                                e.preventDefault();
                                                onRowClick(record);
                                            }
                                        }}
                                    >
                                        {selection ? (
                                            <td className="admin-table-cell admin-table-cell--check">
                                                <Checkbox
                                                    checked={selection.selectedKeys.has(keyOf(record))}
                                                    onCheckedChange={() => toggleRow(record)}
                                                    disabled={!selectable}
                                                    onClick={e => e.stopPropagation()}
                                                    aria-label={
                                                        selectable
                                                            ? `选择${selection.noun} ${selection.labelOf(record)}`
                                                            : `${selection.noun} ${selection.labelOf(record)} 当前不可操作`
                                                    }
                                                    className="admin-checkbox"
                                                />
                                            </td>
                                        ) : null}
                                        {columns.map(column => (
                                            <td key={String(column.key)} className="admin-table-cell">
                                                {column.render
                                                    ? column.render(getValue(record, column.key), record)
                                                    : (getValue(record, column.key) as React.ReactNode)}
                                            </td>
                                        ))}
                                    </tr>
                                );
                            })
                        )}
                    </tbody>
                </table>
            </div>

            {pagination && totalPages > 1 && (
                <nav className="admin-pagination" aria-label="分页导航">
                    <div className="admin-pagination-summary">
                        共 <strong>{pagination.total.toLocaleString()}</strong> 条记录， 第{' '}
                        <strong>{pagination.current}</strong> / <strong>{totalPages}</strong> 页
                    </div>

                    <Pagination className="w-auto">
                        <PaginationContent>
                            <PaginationItem>
                                <Button
                                    variant="ghost"
                                    size="icon"
                                    disabled={pagination.current <= 1}
                                    onClick={() => pagination.onChange(pagination.current - 1)}
                                    aria-label="上一页"
                                    title="上一页"
                                    className="admin-page-btn"
                                >
                                    <ChevronLeft className="h-3.5 w-3.5" aria-hidden="true" />
                                </Button>
                            </PaginationItem>

                            {pageNumbers.map((page, index) => {
                                if (page === 'ellipsis') {
                                    return (
                                        <PaginationItem
                                            // biome-ignore lint/suspicious/noArrayIndexKey: stable list
                                            key={`e-${index}`}
                                        >
                                            <PaginationEllipsis className="admin-page-ellipsis" />
                                        </PaginationItem>
                                    );
                                }
                                return (
                                    <PaginationItem key={page}>
                                        <Button
                                            variant="ghost"
                                            size="icon"
                                            onClick={() => pagination.onChange(page)}
                                            aria-label={`第 ${page} 页`}
                                            aria-current={page === pagination.current ? 'page' : undefined}
                                            className={cn(
                                                'admin-page-btn',
                                                page === pagination.current && 'admin-page-btn--active'
                                            )}
                                        >
                                            {page}
                                        </Button>
                                    </PaginationItem>
                                );
                            })}

                            <PaginationItem>
                                <Button
                                    variant="ghost"
                                    size="icon"
                                    disabled={pagination.current >= totalPages}
                                    onClick={() => pagination.onChange(pagination.current + 1)}
                                    aria-label="下一页"
                                    title="下一页"
                                    className="admin-page-btn"
                                >
                                    <ChevronRight className="h-3.5 w-3.5" aria-hidden="true" />
                                </Button>
                            </PaginationItem>
                        </PaginationContent>
                    </Pagination>
                </nav>
            )}
        </>
    );
}
