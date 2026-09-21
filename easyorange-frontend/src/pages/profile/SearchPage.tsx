import {
    ArrowLeft,
    BookOpen,
    ChevronRight,
    Clock,
    Dumbbell,
    Gift,
    History,
    Home,
    PackageSearch,
    Search,
    ShoppingBag,
    Smartphone,
    Sparkles,
    Trash2,
    TrendingUp,
    X,
} from 'lucide-react';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { PaginationBar } from '@/components/PaginationBar';
import { ProductCard } from '@/components/product/ProductCard';
import '@/components/product/products-grid.css';
import { AiSearchPanel } from '@/components/search/AiSearchPanel';
import FacetFilter from '@/components/search/FacetFilter';
import SortDropdown, { type SortOption } from '@/components/search/SortDropdown';
import { Button } from '@/components/ui/button';
import { useCategories, useHotKeywords, useProductSearch, useSearchSuggestions } from '@/hooks';
import { useSearchUrlState } from '@/hooks/product/useSearchUrlState';
import type { ProductSearchParams } from '@/types/product';
import { debounce } from '@/utils';
import '@/styles/main.css';
import './search-header.css';
import './search-content.css';
import './search-ai.css';

const CATEGORY_ICON_MAP: Record<string, { icon: typeof Smartphone; color: string; bg: string }> = {
    电子数码: { icon: Smartphone, color: '#3B82F6', bg: '#EFF6FF' },
    书籍教材: { icon: BookOpen, color: '#10B981', bg: '#ECFDF5' },
    服饰鞋包: { icon: ShoppingBag, color: '#EC4899', bg: '#FDF2F8' },
    生活用品: { icon: Home, color: '#F59E0B', bg: '#FFFBEB' },
    运动健身: { icon: Dumbbell, color: '#EF4444', bg: '#FEF2F2' },
    虚拟物品: { icon: Gift, color: '#8B5CF6', bg: '#F5F3FF' },
};

const DEFAULT_CATEGORY_ICON = { icon: Gift, color: '#F97316', bg: '#FFF7ED' };

/** 每页条数与后端 PageRequest 上限（100）以内任意值；须与请求参数 pageSize 保持一致 */
const SEARCH_PAGE_SIZE = 20;

function SearchPage() {
    const navigate = useNavigate();
    const {
        keyword: urlKeyword,
        filters,
        pageNum,
        aiEnabled,
        setFilterValue,
        setPageNum,
        setAiEnabled: setUrlAiEnabled,
        setState: setUrlState,
        reset,
    } = useSearchUrlState();
    const [keyword, setKeyword] = useState(urlKeyword);
    const [submittedKeyword, setSubmittedKeyword] = useState(urlKeyword);
    const [showSuggestions, setShowSuggestions] = useState(false);
    const inputRef = useRef<HTMLInputElement>(null);
    const [searchHistory, setSearchHistory] = useState<string[]>(() => {
        try {
            return JSON.parse(localStorage.getItem('eo_search_history') || '[]');
        } catch {
            return [];
        }
    });

    const [debouncedKeyword, setDebouncedKeyword] = useState(urlKeyword);

    const searchQueryParams: ProductSearchParams = useMemo(() => {
        const params: ProductSearchParams = {
            keyword: submittedKeyword,
            pageNum,
            pageSize: SEARCH_PAGE_SIZE,
        };
        if (filters.category) {
            params.categoryId = filters.category;
        }
        if (filters.condition) {
            params.conditionLevel = Number(filters.condition);
        }
        if (filters.price) {
            const [min, max] = filters.price.split('_');
            if (min) {
                params.minPrice = Number(min);
            }
            if (max) {
                params.maxPrice = Number(max);
            }
        }
        // 相关度排序是后端默认行为，不传 sortField
        if (filters.sort && filters.sort !== 'relevance') {
            params.sortField = filters.sort as ProductSearchParams['sortField'];
        }
        if (aiEnabled) {
            params.aiEnhanced = true;
        }
        return params;
    }, [submittedKeyword, pageNum, filters, aiEnabled]);

    const { products, total, facets, aiEnhancement, isLoading: isSearching } = useProductSearch(searchQueryParams);
    const { data: suggestions } = useSearchSuggestions(debouncedKeyword);
    const { data: hotKeywords } = useHotKeywords(10);
    const { data: categories } = useCategories();
    const handleFilterChange = useCallback(
        (key: string, value: string | null) => {
            setFilterValue(key, value);
        },
        [setFilterValue]
    );

    // 防抖处理搜索输入，避免频繁请求建议
    const debouncedSetKeyword = useMemo(
        () =>
            debounce((value: unknown) => {
                setDebouncedKeyword(value as string);
            }, 300),
        []
    );

    // 卸载时取消待执行的防抖定时器，避免组件卸载后回调 setState（触发 React 未处理异步错误）
    useEffect(() => () => debouncedSetKeyword.cancel(), [debouncedSetKeyword]);

    useEffect(() => {
        inputRef.current?.focus();
    }, []);

    // URL keyword 变化（首页热门词、头部搜索入口等外部导航）时本地状态跟随；
    // 自己提交的搜索 URL 与本地一致，此效应为 no-op
    useEffect(() => {
        if (urlKeyword !== submittedKeyword) {
            setKeyword(urlKeyword);
            setSubmittedKeyword(urlKeyword);
            setDebouncedKeyword(urlKeyword);
        }
    }, [urlKeyword, submittedKeyword]);

    const addToHistory = useCallback((kw: string) => {
        if (!kw.trim()) {
            return;
        }
        setSearchHistory(prev => {
            const filtered = prev.filter(h => h !== kw);
            const next = [kw, ...filtered].slice(0, 10);
            localStorage.setItem('eo_search_history', JSON.stringify(next));
            return next;
        });
    }, []);

    const removeFromHistory = useCallback((kw: string, e: React.MouseEvent) => {
        e.stopPropagation();
        setSearchHistory(prev => {
            const next = prev.filter(h => h !== kw);
            localStorage.setItem('eo_search_history', JSON.stringify(next));
            return next;
        });
    }, []);

    const clearHistory = useCallback(() => {
        setSearchHistory([]);
        localStorage.removeItem('eo_search_history');
    }, []);

    /** 发起一次新搜索：URL（keyword + 回到第一页）与本地状态一次对齐 */
    const runSearch = useCallback(
        (kw: string) => {
            const trimmed = kw.trim();
            if (!trimmed) {
                return;
            }
            setUrlState({ keyword: trimmed, pageNum: 1 });
            setKeyword(trimmed);
            setSubmittedKeyword(trimmed);
            setDebouncedKeyword(trimmed);
            setShowSuggestions(false);
            addToHistory(trimmed);
        },
        [setUrlState, addToHistory]
    );

    const handleSubmit = useCallback(
        (e: React.FormEvent) => {
            e.preventDefault();
            runSearch(keyword);
        },
        [keyword, runSearch]
    );

    const handleAiToggle = useCallback(() => {
        setUrlAiEnabled(!aiEnabled);
    }, [aiEnabled, setUrlAiEnabled]);

    const handleSuggestionClick = useCallback(
        (suggestion: string) => {
            runSearch(suggestion);
        },
        [runSearch]
    );

    const handleHotKeywordClick = useCallback(
        (kw: string) => {
            runSearch(kw);
        },
        [runSearch]
    );

    const handleCategoryClick = (categoryId: string) => {
        navigate(`/products?filters=category:${encodeURIComponent(categoryId)}`);
    };

    const handleClear = useCallback(() => {
        setKeyword('');
        setDebouncedKeyword('');
        setSubmittedKeyword('');
        reset();
        inputRef.current?.focus();
    }, [reset]);

    const handleKeywordChange = useCallback(
        (e: React.ChangeEvent<HTMLInputElement>) => {
            const value = e.target.value;
            setKeyword(value);
            setShowSuggestions(true);
            debouncedSetKeyword(value);
        },
        [debouncedSetKeyword]
    );

    const sortValue = (filters.sort as SortOption) || 'relevance';

    const handleSortChange = useCallback(
        (sort: SortOption) => {
            // 换排序回到第一页；relevance（后端默认）不占 filters 参数
            setFilterValue('sort', sort === 'relevance' ? null : sort);
        },
        [setFilterValue]
    );

    const hasResults = submittedKeyword && products.length > 0;
    const noResults = submittedKeyword && !isSearching && products.length === 0;

    // total 由后端 Long 序列化为字符串，参与运算前先归一为数字
    const totalPages = Math.max(1, Math.ceil(Number(total) / SEARCH_PAGE_SIZE));

    const handlePageChange = useCallback(
        (nextPage: number) => {
            setPageNum(nextPage);
            window.scrollTo({ top: 0, behavior: 'smooth' });
        },
        [setPageNum]
    );

    return (
        <div className="search-page-wrapper">
            {/* Animated Background */}
            <div className="search-page-bg">
                <div className="search-bg-orb search-bg-orb-1"></div>
                <div className="search-bg-orb search-bg-orb-2"></div>
                <div className="search-bg-orb search-bg-orb-3"></div>
                <div className="search-bg-mesh"></div>
            </div>

            <div className="search-page-content">
                {/* Search Header */}
                <div className="search-header-bar">
                    <Button variant="ghost" size="icon" className="search-back-btn" onClick={() => navigate(-1)}>
                        <ArrowLeft size={20} />
                    </Button>
                    <form onSubmit={handleSubmit} className="search-form-wrapper">
                        <div className="search-input-premium">
                            <Search size={18} className="search-input-icon" />
                            <input
                                ref={inputRef}
                                type="text"
                                value={keyword}
                                onChange={handleKeywordChange}
                                onFocus={() => setShowSuggestions(true)}
                                onBlur={() => setTimeout(() => setShowSuggestions(false), 200)}
                                placeholder="搜索你想要的商品..."
                                className="search-input-field"
                                aria-label="搜索商品"
                            />
                            {keyword && (
                                <Button
                                    type="button"
                                    variant="ghost"
                                    size="icon"
                                    onClick={handleClear}
                                    className="search-clear-btn"
                                >
                                    <X size={12} />
                                </Button>
                            )}
                            <Button
                                type="button"
                                variant="ghost"
                                size="icon"
                                className={`search-ai-btn ${aiEnabled ? 'ai-enabled' : ''}`}
                                title={aiEnabled ? '关闭AI智能搜索' : '开启AI智能搜索'}
                                onClick={handleAiToggle}
                            >
                                <Sparkles size={14} />
                            </Button>
                            <Button type="submit" className="search-submit-btn">
                                <Search size={14} />
                                <span>搜索</span>
                            </Button>
                        </div>
                        {showSuggestions && suggestions && suggestions.length > 0 && !submittedKeyword && (
                            <div className="search-suggestions-dropdown">
                                <div className="suggestions-header">
                                    <Sparkles size={12} />
                                    <span>AI智能建议</span>
                                </div>
                                {suggestions.map(s => (
                                    <Button
                                        key={s}
                                        type="button"
                                        variant="ghost"
                                        className="suggestion-item justify-start"
                                        onMouseDown={() => handleSuggestionClick(s)}
                                    >
                                        <Search size={14} className="suggestion-icon" />
                                        <span className="suggestion-text">{s}</span>
                                        <ChevronRight size={14} className="suggestion-arrow" />
                                    </Button>
                                ))}
                            </div>
                        )}
                    </form>
                </div>

                {/* Initial State - Rich Content */}
                {!submittedKeyword && (
                    <div className="search-initial-content">
                        {/* Top Row: History + Hot Keywords side by side */}
                        {(searchHistory.length > 0 || (hotKeywords && hotKeywords.length > 0)) && (
                            <div className="search-top-row">
                                {searchHistory.length > 0 && (
                                    <div className="search-top-card">
                                        <div className="search-top-card-header">
                                            <div className="search-top-card-icon">
                                                <History size={14} />
                                            </div>
                                            <h3 className="search-top-card-title">最近搜索</h3>
                                            <Button
                                                variant="ghost"
                                                size="sm"
                                                className="search-top-card-action"
                                                onClick={clearHistory}
                                            >
                                                <Trash2 size={12} />
                                                <span>清空</span>
                                            </Button>
                                        </div>
                                        <div className="search-history-tags">
                                            {searchHistory.map(item => (
                                                <div key={item} className="search-history-tag">
                                                    <Button
                                                        type="button"
                                                        variant="ghost"
                                                        size="sm"
                                                        className="search-history-tag-main font-normal px-3 py-1.5"
                                                        onClick={() => handleHotKeywordClick(item)}
                                                    >
                                                        <Clock size={10} />
                                                        <span>{item}</span>
                                                    </Button>
                                                    <Button
                                                        type="button"
                                                        variant="ghost"
                                                        size="icon"
                                                        className="search-history-remove"
                                                        onClick={e => removeFromHistory(item, e)}
                                                        aria-label={`删除搜索记录 ${item}`}
                                                    >
                                                        <X size={8} />
                                                    </Button>
                                                </div>
                                            ))}
                                        </div>
                                    </div>
                                )}

                                {hotKeywords && hotKeywords.length > 0 && (
                                    <div className="search-top-card">
                                        <div className="search-top-card-header">
                                            <div className="search-top-card-icon">
                                                <TrendingUp size={14} />
                                            </div>
                                            <h3 className="search-top-card-title">热门搜索</h3>
                                        </div>
                                        <div className="search-hot-tags">
                                            {hotKeywords.map((item, index) => (
                                                <Button
                                                    key={item.keyword}
                                                    type="button"
                                                    variant="ghost"
                                                    className={`search-hot-tag ${index < 3 ? 'search-hot-tag-highlight' : ''}`}
                                                    onClick={() => handleHotKeywordClick(item.keyword)}
                                                >
                                                    {index < 3 && (
                                                        <span className={`search-hot-tag-rank rank-${index + 1}`}>
                                                            {index + 1}
                                                        </span>
                                                    )}
                                                    <span>{item.keyword}</span>
                                                </Button>
                                            ))}
                                        </div>
                                    </div>
                                )}
                            </div>
                        )}

                        {/* Category Quick Access */}
                        <div className="search-categories-section">
                            <div className="search-section-header-compact">
                                <div className="search-section-icon-compact">
                                    <PackageSearch size={14} />
                                </div>
                                <h3 className="search-section-title-compact">分类浏览</h3>
                            </div>
                            <div className="search-categories-grid">
                                {categories?.map(cat => {
                                    const iconConfig = CATEGORY_ICON_MAP[cat.name] || DEFAULT_CATEGORY_ICON;
                                    const IconComponent = iconConfig.icon;
                                    return (
                                        <Button
                                            key={cat.id}
                                            type="button"
                                            variant="ghost"
                                            className="search-category-card whitespace-normal"
                                            onClick={() => handleCategoryClick(cat.id)}
                                            style={
                                                {
                                                    '--cat-color': iconConfig.color,
                                                    '--cat-bg': iconConfig.bg,
                                                } as React.CSSProperties
                                            }
                                        >
                                            <div className="search-category-icon">
                                                <IconComponent size={20} />
                                            </div>
                                            <span className="search-category-name">{cat.name}</span>
                                        </Button>
                                    );
                                })}
                            </div>
                        </div>
                    </div>
                )}

                {/* Search Results */}
                {submittedKeyword && (
                    <div className="search-results-section">
                        <div className="search-results-toolbar">
                            <div className="search-results-info">
                                <span className="search-results-badge">
                                    <Search size={12} />
                                    <span>搜索结果</span>
                                </span>
                                <h2 className="search-results-title">
                                    &ldquo;<span className="search-keyword-highlight">{submittedKeyword}</span>&rdquo;
                                </h2>
                                <p className="search-results-count">
                                    共 <span className="search-count-number">{total}</span> 件相关商品
                                </p>
                            </div>
                            <div className="search-results-actions">
                                <SortDropdown value={sortValue} onChange={handleSortChange} />
                            </div>
                        </div>

                        {facets.length > 0 && (
                            <FacetFilter facets={facets} filters={filters} onFilterChange={handleFilterChange} />
                        )}

                        {aiEnhancement && (
                            <AiSearchPanel enhancement={aiEnhancement} onQuestionClick={handleHotKeywordClick} />
                        )}

                        {isSearching && (
                            <div className="search-loading">
                                <div className="search-loading-spinner"></div>
                                <p className="search-loading-text">正在搜索中...</p>
                            </div>
                        )}

                        {hasResults && !isSearching && (
                            <div className="search-results-grid products-grid-premium">
                                {products.map((product, index) => (
                                    <ProductCard
                                        key={product.id}
                                        product={product}
                                        index={index}
                                        aiTags={aiEnhancement?.productTags[product.id]}
                                    />
                                ))}
                            </div>
                        )}

                        {hasResults && !isSearching && totalPages > 1 && (
                            <PaginationBar
                                pageNum={pageNum}
                                totalPages={totalPages}
                                onPageChange={handlePageChange}
                                className="search-pagination"
                            />
                        )}

                        {noResults && (
                            <div className="search-no-results">
                                <div className="search-no-results-icon">
                                    <div className="search-no-results-orb"></div>
                                    <PackageSearch size={40} />
                                </div>
                                <h3 className="search-no-results-title">未找到相关商品</h3>
                                <p className="search-no-results-desc">试试其他关键词，或浏览下面的热门商品</p>
                                {hotKeywords && hotKeywords.length > 0 && (
                                    <div className="search-no-results-hints">
                                        <span className="search-hint-label">试试搜索：</span>
                                        <div className="search-hint-tags">
                                            {hotKeywords.slice(0, 5).map(item => (
                                                <Button
                                                    key={item.keyword}
                                                    variant="outline"
                                                    size="sm"
                                                    className="search-hint-tag"
                                                    onClick={() => handleHotKeywordClick(item.keyword)}
                                                >
                                                    {item.keyword}
                                                </Button>
                                            ))}
                                        </div>
                                    </div>
                                )}
                            </div>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
}

export default SearchPage;
