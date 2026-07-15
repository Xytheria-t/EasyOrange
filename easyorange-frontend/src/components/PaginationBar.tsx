import {
    Pagination,
    PaginationContent,
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

export function PaginationBar({ pageNum, totalPages, onPageChange, className }: PaginationBarProps) {
    if (totalPages <= 1) {
        return null;
    }

    return (
        <Pagination className={cn('w-auto', className)}>
            <PaginationContent>
                <PaginationItem>
                    <PaginationPrevious
                        onClick={() => onPageChange(pageNum - 1)}
                        className={cn(pageNum <= 1 && 'pointer-events-none opacity-40')}
                    />
                </PaginationItem>
                {Array.from({ length: totalPages }, (_, i) => i + 1).map(p => (
                    <PaginationItem key={p}>
                        <PaginationLink isActive={p === pageNum} onClick={() => onPageChange(p)}>
                            {p}
                        </PaginationLink>
                    </PaginationItem>
                ))}
                <PaginationItem>
                    <PaginationNext
                        onClick={() => onPageChange(pageNum + 1)}
                        className={cn(pageNum >= totalPages && 'pointer-events-none opacity-40')}
                    />
                </PaginationItem>
            </PaginationContent>
        </Pagination>
    );
}
