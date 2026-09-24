import { ChevronLeft, ChevronRight, Inbox } from 'lucide-react';
import { useMemo, useState } from 'react';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui';
import { Button } from '@/components/ui/button';
import { Pagination, PaginationContent, PaginationEllipsis, PaginationItem } from '@/components/ui/pagination';
import { cn } from '@/lib/utils';

export interface Column<T> {
    key: keyof T | string;
    title: string;
    render?: (value: unknown, record: T) => React.ReactNode;
    sortable?: boolean;
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
    emptyText?: string;
}

type SortDirection = 'asc' | 'desc' | null;

interface SortState {
    key: string | null;
    direction: SortDirection;
}

export function AdminTable<T extends object>({
    columns,
    data,
    rowKey,
    loading = false,
    error = null,
    onRetry,
    pagination,
    onRowClick,
    emptyText = '暂无数据',
}: AdminTableProps<T>) {
    const [sortState, setSortState] = useState<SortState>({ key: null, direction: null });

    const handleSort = (columnKey: string) => {
        setSortState(prev => {
            if (prev.key !== columnKey) {
                return { key: columnKey, direction: 'asc' };
            }
            if (prev.direction === 'asc') {
                return { key: columnKey, direction: 'desc' };
            }
            return { key: null, direction: null };
        });
    };

    const sortedData = useMemo(() => {
        if (!sortState.key || !sortState.direction) {
            return data;
        }
        return [...data].sort((a, b) => {
            const aValue = a[sortState.key as keyof T];
            const bValue = b[sortState.key as keyof T];
            if (aValue === bValue) {
                return 0;
            }
            if (aValue == null) {
                return 1;
            }
            if (bValue == null) {
                return -1;
            }
            const comparison = aValue < bValue ? -1 : 1;
            return sortState.direction === 'asc' ? comparison : -comparison;
        });
    }, [data, sortState]);

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

    const totalPages = pagination ? Math.ceil(pagination.total / pagination.pageSize) : 0;

    const pageNumbers = useMemo(() => {
        if (!pagination) {
            return [];
        }
        const pages: (number | 'ellipsis')[] = [];
        const { current, pageSize, total } = pagination;
        const totalP = Math.ceil(total / pageSize);
        if (totalP <= 7) {
            for (let i = 1; i <= totalP; i++) {
                pages.push(i);
            }
        } else {
            pages.push(1);
            if (current > 3) {
                pages.push('ellipsis');
            }
            for (let i = Math.max(2, current - 1); i <= Math.min(totalP - 1, current + 1); i++) {
                pages.push(i);
            }
            if (current < totalP - 2) {
                pages.push('ellipsis');
            }
            pages.push(totalP);
        }
        return pages;
    }, [pagination]);

    const renderSortIcon = (columnKey: string) => {
        const isActive = sortState.key === columnKey;
        return (
            <span className="admin-sort-icon">
                <svg
                    aria-hidden="true"
                    className={cn(
                        'mb-[-2px] h-2.5 w-2.5',
                        isActive && sortState.direction === 'asc' ? 'admin-sort-icon--active' : 'admin-sort-icon--idle'
                    )}
                    fill="currentColor"
                    viewBox="0 0 24 24"
                >
                    <path d="M7 14l5-5 5 5z" />
                </svg>
                <svg
                    aria-hidden="true"
                    className={cn(
                        'h-2.5 w-2.5',
                        isActive && sortState.direction === 'desc' ? 'admin-sort-icon--active' : 'admin-sort-icon--idle'
                    )}
                    fill="currentColor"
                    viewBox="0 0 24 24"
                >
                    <path d="M7 10l5 5 5-5z" />
                </svg>
            </span>
        );
    };

    const isFirstColumn = (_column: Column<T>, idx: number) => idx === 0;
    const isLastColumn = (_column: Column<T>, idx: number) => idx === columns.length - 1;

    const ariaSortFor = (columnKey: string) => {
        if (sortState.key !== columnKey || !sortState.direction) {
            return 'none' as const;
        }
        return sortState.direction === 'asc' ? ('ascending' as const) : ('descending' as const);
    };

    return (
        <>
            {/* 窄屏横向滚动：列宽由内容撑开，不把订单号/金额压成两行 */}
            <div className="admin-table-scroll">
                <Table>
                    <TableHeader>
                        <TableRow className="border-0 hover:bg-transparent">
                            {columns.map((column, idx) => (
                                <TableHead
                                    key={String(column.key)}
                                    className={cn(
                                        'admin-table-head',
                                        isFirstColumn(column, idx) && 'admin-table-head--first',
                                        isLastColumn(column, idx) && 'admin-table-head--last',
                                        column.sortable && 'admin-table-head--sortable'
                                    )}
                                    // aria-sort 挂在 th 上，读屏才能播报当前排序方向
                                    aria-sort={column.sortable ? ariaSortFor(String(column.key)) : undefined}
                                >
                                    {column.sortable ? (
                                        <button
                                            type="button"
                                            onClick={() => handleSort(String(column.key))}
                                            className="admin-table-head-sort focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-(--admin-accent-bright) focus-visible:ring-inset"
                                        >
                                            {column.title}
                                            {renderSortIcon(String(column.key))}
                                        </button>
                                    ) : (
                                        <span className="inline-flex items-center">{column.title}</span>
                                    )}
                                </TableHead>
                            ))}
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {loading ? (
                            <TableRow className="border-0 hover:bg-transparent">
                                <TableCell colSpan={columns.length} className="admin-table-cell">
                                    <div
                                        className="admin-table-state admin-table-state--loading"
                                        role="status"
                                        aria-live="polite"
                                        aria-busy="true"
                                    >
                                        <div className="admin-spinner animate-spin" />
                                        <span className="admin-table-state-message">加载中...</span>
                                    </div>
                                </TableCell>
                            </TableRow>
                        ) : error ? (
                            <TableRow className="border-0 hover:bg-transparent">
                                <TableCell colSpan={columns.length} className="admin-table-cell">
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
                                </TableCell>
                            </TableRow>
                        ) : sortedData.length === 0 ? (
                            <TableRow className="border-0 hover:bg-transparent">
                                <TableCell colSpan={columns.length} className="admin-table-cell">
                                    <div className="admin-table-state">
                                        <Inbox className="mx-auto mb-[0.65rem] h-9 w-9 opacity-40" aria-hidden="true" />
                                        {/* 只渲染 emptyText：再补一句「暂无相关数据」会和页面自带的空态文案重复 */}
                                        <div className="admin-table-state-title">{emptyText}</div>
                                    </div>
                                </TableCell>
                            </TableRow>
                        ) : (
                            sortedData.map(record => (
                                <TableRow
                                    key={String(record[rowKey])}
                                    className={cn(
                                        'admin-table-row',
                                        // 可点击行补键盘可达：Enter / Space 等同点击
                                        onRowClick && 'admin-table-row--clickable focus-visible:outline-none'
                                    )}
                                    tabIndex={onRowClick ? 0 : undefined}
                                    role={onRowClick ? 'button' : undefined}
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
                                    {columns.map(column => {
                                        const cellValue = getValue(record, column.key);
                                        return (
                                            <TableCell key={String(column.key)} className="admin-table-cell">
                                                {column.render
                                                    ? column.render(cellValue, record)
                                                    : (cellValue as React.ReactNode)}
                                            </TableCell>
                                        );
                                    })}
                                </TableRow>
                            ))
                        )}
                    </TableBody>
                </Table>
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
