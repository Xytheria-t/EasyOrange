import {
    Pagination,
    PaginationContent,
    PaginationEllipsis,
    PaginationItem,
    PaginationLink,
    PaginationNext,
    PaginationPrevious,
} from '@/components/ui/pagination';
import { cn } from '@/lib/utils';

interface PaginationBarProps {
    pageNum: number;
    totalPages: number;
    onPageChange: (page: number) => void;
    className?: string;
}

/** 当前页附近保留的页码数量，两侧各留 1 页 + 首尾 + 省略号 */
const SIBLING_COUNT = 1;

type PageItem = number | 'ellipsis-start' | 'ellipsis-end';

function buildPageItems(current: number, totalPages: number): PageItem[] {
    if (totalPages <= 7) {
        return Array.from({ length: totalPages }, (_, i) => i + 1);
    }

    const pages = new Set<number>([1, totalPages]);
    for (let p = current - SIBLING_COUNT; p <= current + SIBLING_COUNT; p++) {
        if (p >= 1 && p <= totalPages) {
            pages.add(p);
        }
    }

    const sorted = [...pages].sort((a, b) => a - b);
    const items: PageItem[] = [];
    let prev = 0;
    for (const p of sorted) {
        if (prev && p - prev > 1) {
            // 省略号按位置命名，天然唯一，不需要索引做 key
            items.push(prev === 1 ? 'ellipsis-start' : 'ellipsis-end');
        }
        items.push(p);
        prev = p;
    }
    return items;
}

export function PaginationBar({ pageNum, totalPages, onPageChange, className }: PaginationBarProps) {
    if (totalPages <= 1) {
        return null;
    }

    // 越界时钳回有效范围，防止 URL 里的坏值渲染出"当前第 99 页"
    const safePage = Math.min(Math.max(pageNum, 1), totalPages);
    const isFirst = safePage <= 1;
    const isLast = safePage >= totalPages;

    return (
        <Pagination className={cn('w-auto', className)}>
            <PaginationContent>
                <PaginationItem>
                    {/* 用原生 disabled 而非 pointer-events-none：
                        后者只挡鼠标，键盘仍能聚焦并触发越界翻页 */}
                    <PaginationPrevious
                        disabled={isFirst}
                        onClick={() => onPageChange(safePage - 1)}
                        className={cn(isFirst && 'pointer-events-none opacity-40')}
                    />
                </PaginationItem>
                {buildPageItems(safePage, totalPages).map(item =>
                    typeof item === 'number' ? (
                        <PaginationItem key={item}>
                            <PaginationLink isActive={item === safePage} onClick={() => onPageChange(item)}>
                                {item}
                            </PaginationLink>
                        </PaginationItem>
                    ) : (
                        <PaginationItem key={item}>
                            <PaginationEllipsis />
                        </PaginationItem>
                    )
                )}
                <PaginationItem>
                    <PaginationNext
                        disabled={isLast}
                        onClick={() => onPageChange(safePage + 1)}
                        className={cn(isLast && 'pointer-events-none opacity-40')}
                    />
                </PaginationItem>
            </PaginationContent>
        </Pagination>
    );
}
