import { useState, useEffect } from 'react';

interface ColumnBreakpoint {
    maxWidth: number;
    columns: number;
}

const DEFAULT_BREAKPOINTS: ColumnBreakpoint[] = [
    { maxWidth: 480, columns: 2 },
    { maxWidth: 768, columns: 2 },
    { maxWidth: 1200, columns: 3 },
];

function getColumnCount(breakpoints: ColumnBreakpoint[] = DEFAULT_BREAKPOINTS): number {
    if (typeof window === 'undefined') {
        return 4;
    }
    const width = window.innerWidth;
    for (const bp of breakpoints) {
        if (width <= bp.maxWidth) {
            return bp.columns;
        }
    }
    return 4;
}

export function useColumnCount(breakpoints?: ColumnBreakpoint[]): number {
    const [columnCount, setColumnCount] = useState(() => getColumnCount(breakpoints));

    useEffect(() => {
        const handleResize = () => {
            setColumnCount(getColumnCount(breakpoints));
        };

        window.addEventListener('resize', handleResize);
        return () => window.removeEventListener('resize', handleResize);
    }, [breakpoints]);

    return columnCount;
}

export type { ColumnBreakpoint };
