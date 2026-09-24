import { useEffect, useState } from 'react';

function matchMediaSafe(query: string): MediaQueryList | null {
    if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
        return null;
    }
    return window.matchMedia(query);
}

/** 订阅一条媒体查询，随视口变化实时更新。SSR / 无 matchMedia 环境下退化为 false。 */
export function useMediaQuery(query: string): boolean {
    const [matches, setMatches] = useState(() => matchMediaSafe(query)?.matches ?? false);

    useEffect(() => {
        const mediaQuery = matchMediaSafe(query);
        if (!mediaQuery) {
            return;
        }
        setMatches(mediaQuery.matches);

        const handleChange = (event: MediaQueryListEvent) => setMatches(event.matches);
        mediaQuery.addEventListener('change', handleChange);
        return () => mediaQuery.removeEventListener('change', handleChange);
    }, [query]);

    return matches;
}
