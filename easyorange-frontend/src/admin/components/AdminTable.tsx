import { ChevronLeft, ChevronRight } from 'lucide-react';
import { useMemo, useState } from 'react';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui';
import { Button } from '@/components/ui/button';
import {
    Pagination,
    PaginationContent,
    PaginationEllipsis,
    PaginationItem,
} from '@/components/ui/pagination';
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
            <span className="ml-1 inline-flex flex-col leading-none">
                <svg
                    aria-hidden="true"
                    className="mb-[-2px] h-2.5 w-2.5 transition-colors duration-150"
                    style={{
                        color: isActive && sortState.direction === 'asc' ? '#F97316' : '#D6CEC5',
                    }}
                    fill="currentColor"
                    viewBox="0 0 24 24"
                >
                    <path d="M7 14l5-5 5 5z" />
                </svg>
                <svg
                    aria-hidden="true"
                    className="h-2.5 w-2.5 transition-colors duration-150"
                    style={{
                        color: isActive && sortState.direction === 'desc' ? '#F97316' : '#D6CEC5',
                    }}
                    fill="currentColor"
                    viewBox="0 0 24 24"
                >
                    <path d="M7 10l5 5 5-5z" />
                </svg>
            </span>
        );
    };

    const headerBaseClass =
        'text-left py-[0.85rem] px-5 text-[0.68rem] font-semibold text-[#9B9590] uppercase tracking-[0.06em] ' +
        'bg-[linear-gradient(180deg,rgba(249,115,22,0.04)_0%,transparent_100%)] border-b border-[#EDE8E3] whitespace-nowrap';

    const cellBaseClass =
        'py-[0.85rem] px-5 text-[0.87rem] text-[#4A4540] border-b border-[#F5F2EE] align-middle transition-colors duration-150';

    const pageButtonClass =
        'min-w-[34px] h-[34px] inline-flex items-center justify-center border-[1.5px] border-transparent ' +
        'rounded-[10px] text-[0.81rem] font-semibold text-[#8B857E] bg-transparent transition-all duration-150 px-2';

    const isFirstColumn = (_column: Column<T>, idx: number) => idx === 0;
    const isLastColumn = (_column: Column<T>, idx: number) => idx === columns.length - 1;

    return (
        <>
            <Table>
                <TableHeader>
                    <TableRow className="border-0 hover:bg-transparent">
                        {columns.map((column, idx) => (
                            <TableHead
                                key={String(column.key)}
                                className={cn(
                                    headerBaseClass,
                                    isFirstColumn(column, idx) && 'rounded-tl-[20px]',
                                    isLastColumn(column, idx) && 'rounded-tr-[20px]',
                                    column.sortable && 'cursor-pointer select-none'
                                )}
                                onClick={() => column.sortable && handleSort(String(column.key))}
                            >
                                <span className="inline-flex items-center">
                                    {column.title}
                                    {column.sortable && renderSortIcon(String(column.key))}
                                </span>
                            </TableHead>
                        ))}
                    </TableRow>
                </TableHeader>
                <TableBody>
                    {loading ? (
                        <TableRow className="border-0 hover:bg-transparent">
                            <TableCell colSpan={columns.length} className={cn(cellBaseClass, 'text-center')}>
                                <div className="flex flex-col items-center gap-[0.7rem] py-12 px-4">
                                    <div className="h-7 w-7 rounded-full border-[2.5px] border-[#E5E0DB] border-t-[#F97316] animate-spin" />
                                    <span className="text-[0.87rem] text-[#9B9590]">加载中...</span>
                                </div>
                            </TableCell>
                        </TableRow>
                    ) : sortedData.length === 0 ? (
                        <TableRow className="border-0 hover:bg-transparent">
                            <TableCell colSpan={columns.length} className={cn(cellBaseClass, 'text-center')}>
                                <div className="py-14 px-6 text-center">
                                    <div className="mb-[0.65rem] text-[2.2rem] opacity-45">📭</div>
                                    <div
                                        className="text-[0.98rem] font-semibold text-[#8B857E] mb-1"
                                        style={{ fontFamily: "'Playfair Display', serif" }}
                                    >
                                        {emptyText}
                                    </div>
                                    <div className="text-[0.84rem] text-[#B5AEA8]">暂无相关数据</div>
                                </div>
                            </TableCell>
                        </TableRow>
                    ) : (
                        sortedData.map(record => (
                            <TableRow
                                key={String(record[rowKey])}
                                className={cn(
                                    'border-b border-[#F5F2EE] transition-colors duration-150',
                                    onRowClick && 'cursor-pointer',
                                    'hover:bg-[rgba(249,115,22,0.025)]'
                                )}
                                onClick={() => onRowClick?.(record)}
                            >
                                {columns.map(column => {
                                    const cellValue = getValue(record, column.key);
                                    return (
                                        <TableCell key={String(column.key)} className={cellBaseClass}>
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

            {pagination && totalPages > 1 && (
                <div className="relative flex items-center justify-between px-5 py-[0.9rem] border-t border-[#EDE8E3]">
                    <div
                        className="absolute top-0 left-5 right-5 h-px"
                        style={{
                            background: 'linear-gradient(90deg, transparent, rgba(249,115,22,0.08), transparent)',
                        }}
                    />

                    <div className="text-[0.81rem] text-[#B5AEA8]">
                        共 <strong className="text-[#4A4540] font-semibold">{pagination.total.toLocaleString()}</strong>{' '}
                        条记录， 第 <strong className="text-[#4A4540] font-semibold">{pagination.current}</strong> /{' '}
                        <strong className="text-[#4A4540] font-semibold">{totalPages}</strong> 页
                    </div>

                    <Pagination className="w-auto">
                        <PaginationContent>
                            <PaginationItem>
                                <Button
                                    variant="ghost"
                                    size="icon"
                                    disabled={pagination.current <= 1}
                                    onClick={() => pagination.onChange(pagination.current - 1)}
                                    className={cn(pageButtonClass, pagination.current <= 1 && 'opacity-40 cursor-not-allowed')}
                                >
                                    <ChevronLeft className="h-3.5 w-3.5" />
                                </Button>
                            </PaginationItem>

                            {pageNumbers.map((page, index) => {
                                if (page === 'ellipsis') {
                                    return (
                                    <PaginationItem
                                        // biome-ignore lint/suspicious/noArrayIndexKey: stable list
                                        key={`e-${index}`}
                                    >
                                        <PaginationEllipsis className="text-[0.81rem] text-[#B5AEA8]" />
                                    </PaginationItem>
                                    );
                                }
                                return (
                                    <PaginationItem key={page}>
                                        <Button
                                            variant="ghost"
                                            size="icon"
                                            onClick={() => pagination.onChange(page)}
                                            className={cn(
                                                pageButtonClass,
                                                page === pagination.current &&
                                                    'text-white bg-[linear-gradient(135deg,#F97316,#EA580C)] shadow-[0_2px_8px_rgba(249,115,22,0.28)]'
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
                                    className={cn(pageButtonClass, pagination.current >= totalPages && 'opacity-40 cursor-not-allowed')}
                                >
                                    <ChevronRight className="h-3.5 w-3.5" />
                                </Button>
                            </PaginationItem>
                        </PaginationContent>
                    </Pagination>
                </div>
            )}
        </>
    );
}
