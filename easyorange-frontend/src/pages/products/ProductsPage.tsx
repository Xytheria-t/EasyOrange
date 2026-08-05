import { useVirtualizer } from '@tanstack/react-virtual';
import { Search, X } from 'lucide-react';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { SemanticSearchToggle } from '@/components/ai/SemanticSearchToggle';
import { FilterSidebar, type FilterState } from '@/components/product/FilterSidebar';
import { ProductCard } from '@/components/product/ProductCard';
import { ToolsPlaza, type ToolsPlazaFilter } from '@/components/product/ToolsPlaza';

import SortDropdown, { type SortOption } from '@/components/search/SortDropdown';
import { Input } from '@/components/ui';
import { Button } from '@/components/ui/button';
import { preloadImages } from '@/components/ui/Image';
import {
    useCategories,
    useColumnCount,
    useFavoriteCheck,
    useInfiniteProducts,
    useListUrlState,
    useSemanticSearch,
} from '@/hooks';
import { useAuthStore } from '@/store/authStore';
import type { Product } from '@/types';
import './products-list.css';

const SORT_OPTIONS = ['newest', 'price_asc', 'price_desc', 'popular'] as const;
type ProductSort = (typeof SORT_OPTIONS)[number];

function ProductsPage() {
    const { token } = useAuthStore();
    const navigate = useNavigate();
    const { checkFavorites, isFavorited, toggleFavorite } = useFavoriteCheck();
    const {
        keyword: urlKeyword,
        filters,
        setKeyword: setUrlKeyword,
        setState: setUrlState,
        reset: resetUrl,
    } = useListUrlState();

    const [isFilterOpen, setIsFilterOpen] = useState(false);
    const [searchQuery, setSearchQuery] = useState(urlKeyword);
    const [semanticPage, setSemanticPage] = useState(1);

    const queryParams = useMemo<{
        pageSize: number;
        keyword?: string;
        categoryId?: string;
        sort?: ProductSort;
        priceMin?: number;
        priceMax?: number;
        conditions?: number[];
        hasDiscount?: boolean;
    }>(() => {
        const sortValue = filters.sort;
        const sort: ProductSort =
            sortValue && SORT_OPTIONS.includes(sortValue as ProductSort) ? (sortValue as ProductSort) : 'newest';
        return {
            pageSize: 20,
            keyword: urlKeyword || undefined,
            categoryId: filters.category || undefined,
            sort,
            priceMin: filters.priceMin ? Number(filters.priceMin) : undefined,
            priceMax: filters.priceMax ? Number(filters.priceMax) : undefined,
            conditions: filters.conditions ? filters.conditions.split(',').map(Number) : undefined,
            hasDiscount: filters.hasDiscount === '1' || undefined,
        };
    }, [urlKeyword, filters]);

    const activeFilter: ToolsPlazaFilter = queryParams.hasDiscount ? 'discount' : 'all';

    useEffect(() => {
        setSearchQuery(urlKeyword || '');
    }, [urlKeyword]);

    const {
        data: infiniteData,
        isLoading,
        fetchNextPage,
        hasNextPage,
        isFetchingNextPage,
    } = useInfiniteProducts(queryParams);

    const {
        results: semanticResults,
        isSearching: isSemanticSearching,
        isSemanticMode,
        total: semanticTotal,
        error: semanticError,
        search: semanticSearch,
        toggleSemanticMode,
    } = useSemanticSearch();

    const sentinelRef = useRef<HTMLDivElement>(null);

    // 从 TanStack Query 的无限查询数据中提取所有产品
    const allProducts = useMemo(() => {
        if (isSemanticMode) {
            return semanticResults;
        }
        if (!infiniteData?.pages) {
            return [];
        }
        // 合并所有页面的产品数据
        return infiniteData.pages.flatMap(page => page.records ?? []);
    }, [infiniteData, isSemanticMode, semanticResults]);

    // 获取总数（从第一页获取）
    const total = isSemanticMode ? semanticTotal : (infiniteData?.pages?.[0]?.total ?? 0);
    const isSearchLoading = isSemanticMode ? isSemanticSearching : isLoading;

    // 语义搜索的分页逻辑
    useEffect(() => {
        if (isSemanticMode && queryParams.keyword) {
            semanticSearch(queryParams.keyword, semanticPage, queryParams.pageSize);
        }
    }, [isSemanticMode, queryParams.keyword, semanticPage, queryParams.pageSize, semanticSearch]);

    // 预加载图片
    useEffect(() => {
        if (allProducts.length > 0) {
            const upcomingImages = allProducts
                .slice(0, 6)
                .map((p: Product) => p.images?.[0])
                .filter(Boolean) as string[];
            preloadImages(upcomingImages, { width: 300, format: 'webp', quality: 75 }).catch(() => {});
        }
    }, [allProducts]);

    // 检查收藏状态
    useEffect(() => {
        if (allProducts.length > 0 && token) {
            checkFavorites(allProducts.map(p => p.id));
        }
    }, [allProducts, token, checkFavorites]);

    // Intersection Observer 处理无限滚动
    useEffect(() => {
        const sentinel = sentinelRef.current;
        if (!sentinel || isSemanticMode) {
            return;
        }

        const observer = new IntersectionObserver(
            ([entry]) => {
                if (entry.isIntersecting && !isFetchingNextPage && hasNextPage) {
                    fetchNextPage();
                }
            },
            { rootMargin: '1200px' }
        );

        observer.observe(sentinel);
        return () => observer.disconnect();
    }, [isSemanticMode, isFetchingNextPage, hasNextPage, fetchNextPage]);

    // 语义搜索的无限滚动
    useEffect(() => {
        const sentinel = sentinelRef.current;
        if (!sentinel || !isSemanticMode) {
            return;
        }

        const observer = new IntersectionObserver(
            ([entry]) => {
                if (entry.isIntersecting && !isSemanticSearching && semanticResults.length < semanticTotal) {
                    setSemanticPage(prev => prev + 1);
                }
            },
            { rootMargin: '1200px' }
        );

        observer.observe(sentinel);
        return () => observer.disconnect();
    }, [isSemanticMode, isSemanticSearching, semanticResults.length, semanticTotal]);

    const { data: categories } = useCategories();
    const currentCategory = useMemo(() => {
        if (!queryParams.categoryId || !categories) {
            return null;
        }
        return categories.find(c => c.id === queryParams.categoryId) || null;
    }, [queryParams.categoryId, categories]);

    const COLUMN_COUNT = useColumnCount();

    const rows = useMemo(() => {
        const result: (Product | null)[][] = [];
        for (let i = 0; i < allProducts.length; i += COLUMN_COUNT) {
            result.push(allProducts.slice(i, i + COLUMN_COUNT));
        }
        // 添加加载占位符
        if ((isFetchingNextPage || isSemanticSearching) && allProducts.length > 0) {
            result.push(Array(COLUMN_COUNT).fill(null));
            result.push(Array(COLUMN_COUNT).fill(null));
        }
        return result;
    }, [allProducts, COLUMN_COUNT, isFetchingNextPage, isSemanticSearching]);

    const rowVirtualizer = useVirtualizer({
        count: rows.length,
        getScrollElement: () =>
            (typeof window !== 'undefined' ? window.document.documentElement : null) as HTMLElement | null,
        estimateSize: () => 520,
        overscan: 8,
    });

    const handleSortChange = useCallback(
        (sort: SortOption) => {
            setSemanticPage(1);
            setUrlState({ filters: { ...filters, sort } });
        },
        [filters, setUrlState]
    );

    const handleFilterChange = useCallback(
        (filter: ToolsPlazaFilter) => {
            setSemanticPage(1);
            if (filter === 'all') {
                const { hasDiscount, ...next } = filters;
                next.sort = 'newest';
                setUrlState({ filters: next });
            } else if (filter === 'discount') {
                setUrlState({ filters: { ...filters, hasDiscount: '1' } });
            }
        },
        [filters, setUrlState]
    );

    const handleApplyFilters = useCallback(
        (filterState: FilterState) => {
            setSemanticPage(1);
            const next: Record<string, string> = {};
            if (filters.sort) {
                next.sort = filters.sort;
            }
            if (filterState.categories.length === 1) {
                next.category = filterState.categories[0];
            }
            if (filterState.priceMin) {
                next.priceMin = String(filterState.priceMin);
            }
            if (filterState.priceMax) {
                next.priceMax = String(filterState.priceMax);
            }
            if (filterState.conditions.length > 0) {
                next.conditions = filterState.conditions.join(',');
            }
            setUrlState({ filters: next });
            setIsFilterOpen(false);
        },
        [filters, setUrlState]
    );

    const handleResetFilters = useCallback(() => {
        setSemanticPage(1);
        resetUrl();
    }, [resetUrl]);

    const handleClearCategory = useCallback(() => {
        setSemanticPage(1);
        const { category, hasDiscount, ...next } = filters;
        setUrlState({ filters: next });
    }, [filters, setUrlState]);

    const handleSearchSubmit = useCallback(
        (e: React.FormEvent) => {
            e.preventDefault();
            const trimmed = searchQuery.trim();
            if (trimmed === urlKeyword) {
                return;
            }
            setSemanticPage(1);
            setUrlKeyword(trimmed);
        },
        [searchQuery, urlKeyword, setUrlKeyword]
    );

    const handleSearchClear = useCallback(() => {
        setSearchQuery('');
        setSemanticPage(1);
        setUrlKeyword('');
    }, [setUrlKeyword]);

    const handleFavorite = useCallback(
        async (productId: string, shouldFavorite: boolean) => {
            if (!token) {
                navigate('/login');
                return;
            }
            toggleFavorite(productId, shouldFavorite);
        },
        [token, navigate, toggleFavorite]
    );

    if (isSearchLoading && allProducts.length === 0) {
        return (
            <div className="products-page-wrapper">
                <div className="products-container">
                    <div className="products-toolbar">
                        <div className="results-info">
                            <div className="skeleton-line" style={{ width: 80, height: 20 }} />
                        </div>
                    </div>
                    <div className="products-grid-premium">
                        {[...Array(10)].map((_, i) => (
                            <div
                                // biome-ignore lint/suspicious/noArrayIndexKey: stable loading skeleton
                                key={i}
                                className="product-card-loading-premium"
                            >
                                <div className="product-image-loading-premium" />
                                <div className="product-info-loading">
                                    <div className="loading-line short" />
                                    <div className="loading-line" />
                                    <div className="loading-line" />
                                    <div className="loading-line short" />
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="products-page-wrapper">
            <FilterSidebar
                isOpen={isFilterOpen}
                onClose={() => setIsFilterOpen(false)}
                onApplyFilters={handleApplyFilters}
                onResetFilters={handleResetFilters}
            />

            <div className="products-container">
                <ToolsPlaza onFilterChange={handleFilterChange} total={total} activeFilter={activeFilter} />

                <div className="products-toolbar">
                    <div className="results-info">
                        {currentCategory && (
                            <Button
                                variant="ghost"
                                size="sm"
                                className="results-category"
                                onClick={handleClearCategory}
                            >
                                <span className="category-label">分类：</span>
                                <span className="category-name">{currentCategory.name}</span>
                                <X size={12} className="category-clear" />
                            </Button>
                        )}
                        <span className="results-count">{total}</span>
                        <span className="results-text"> 件商品</span>
                    </div>

                    <form className="search-bar" onSubmit={handleSearchSubmit}>
                        <Search size={16} className="search-bar-icon" />
                        <Input
                            type="text"
                            className="search-bar-input"
                            placeholder="搜索托管商品..."
                            value={searchQuery}
                            onChange={e => setSearchQuery(e.target.value)}
                        />
                        {searchQuery && (
                            <Button
                                type="button"
                                variant="ghost"
                                size="icon"
                                className="search-bar-clear"
                                onClick={handleSearchClear}
                                aria-label="清除搜索"
                            >
                                <X size={14} />
                            </Button>
                        )}
                    </form>

                    <div className="toolbar-actions">
                        <SemanticSearchToggle isActive={isSemanticMode} onToggle={toggleSemanticMode} />
                        <Button variant="outline" className="filter-toggle-btn" onClick={() => setIsFilterOpen(true)}>
                            <svg
                                aria-hidden="true"
                                viewBox="0 0 24 24"
                                fill="none"
                                stroke="currentColor"
                                strokeWidth="2"
                            >
                                <line x1="4" y1="21" x2="4" y2="14" />
                                <line x1="4" y1="10" x2="4" y2="3" />
                                <line x1="12" y1="21" x2="12" y2="12" />
                                <line x1="12" y1="8" x2="12" y2="3" />
                                <line x1="20" y1="21" x2="20" y2="16" />
                                <line x1="20" y1="12" x2="20" y2="3" />
                            </svg>
                            <span>筛选</span>
                        </Button>

                        <SortDropdown
                            value={(queryParams.sort ?? 'newest') as SortOption}
                            onChange={handleSortChange}
                        />
                    </div>
                </div>

                <div style={{ height: `${rowVirtualizer.getTotalSize()}px`, width: '100%', position: 'relative' }}>
                    {rowVirtualizer.getVirtualItems().map(virtualRow => {
                        const row = rows[virtualRow.index];
                        if (!row) {
                            return null;
                        }
                        return (
                            <div
                                key={virtualRow.index}
                                style={{
                                    position: 'absolute',
                                    top: 0,
                                    left: 0,
                                    width: '100%',
                                    height: virtualRow.size,
                                    transform: `translateY(${virtualRow.start}px)`,
                                    display: 'grid',
                                    gridTemplateColumns: `repeat(${COLUMN_COUNT}, 1fr)`,
                                    gap: '1.75rem',
                                    padding: '0 0 1.75rem 0',
                                }}
                            >
                                {row.map((item: Product | null, colIndex: number) =>
                                    item === null ? (
                                        <div
                                            // biome-ignore lint/suspicious/noArrayIndexKey: stable buffer row
                                            key={`buffer-${virtualRow.index}-${colIndex}`}
                                            className="product-card-loading-premium"
                                        >
                                            <div className="product-image-loading-premium" />
                                            <div className="product-info-loading">
                                                <div className="loading-line short" />
                                                <div className="loading-line" />
                                                <div className="loading-line short" />
                                            </div>
                                        </div>
                                    ) : (
                                        <ProductCard
                                            key={item.id}
                                            product={item}
                                            index={virtualRow.index * COLUMN_COUNT + colIndex}
                                            isFavorited={isFavorited(item.id)}
                                            onFavorite={handleFavorite}
                                        />
                                    )
                                )}
                            </div>
                        );
                    })}
                </div>

                {allProducts.length > 0 && <div ref={sentinelRef} className="scroll-sentinel" />}

                {!isSearchLoading && allProducts.length === 0 && (
                    <div className="no-results-premium">
                        <div className={`no-results-icon-premium ${semanticError ? 'error' : ''}`}>
                            {semanticError ? (
                                <svg
                                    aria-hidden="true"
                                    viewBox="0 0 24 24"
                                    fill="none"
                                    stroke="currentColor"
                                    strokeWidth="1.5"
                                >
                                    <circle cx="12" cy="12" r="10" />
                                    <line x1="12" y1="8" x2="12" y2="12" />
                                    <line x1="12" y1="16" x2="12.01" y2="16" />
                                </svg>
                            ) : (
                                <svg
                                    aria-hidden="true"
                                    viewBox="0 0 24 24"
                                    fill="none"
                                    stroke="currentColor"
                                    strokeWidth="1.5"
                                >
                                    <circle cx="11" cy="11" r="8" />
                                    <path d="M21 21l-4.35-4.35" />
                                    <path d="M8 8l6 6M14 8l-6 6" />
                                </svg>
                            )}
                        </div>
                        {semanticError ? (
                            <>
                                <h3>语义搜索暂不可用</h3>
                                <p className="no-results-error">{semanticError}</p>
                                <div className="no-results-actions">
                                    <Button
                                        className="semantic-retry-btn"
                                        onClick={() => queryParams.keyword && semanticSearch(queryParams.keyword)}
                                    >
                                        重试
                                    </Button>
                                    <Button
                                        variant="outline"
                                        className="semantic-fallback-btn"
                                        onClick={toggleSemanticMode}
                                    >
                                        切换到关键词搜索
                                    </Button>
                                </div>
                            </>
                        ) : isSemanticMode ? (
                            <>
                                <h3>未找到相关商品</h3>
                                <p>语义搜索未匹配到结果，试试其他关键词</p>
                                <div className="no-results-actions">
                                    <Button
                                        variant="outline"
                                        className="semantic-fallback-btn"
                                        onClick={toggleSemanticMode}
                                    >
                                        切换到关键词搜索
                                    </Button>
                                </div>
                            </>
                        ) : (
                            <>
                                <h3>未找到相关商品</h3>
                                <p>尝试调整筛选条件或搜索其他关键词</p>
                            </>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
}

export default ProductsPage;
