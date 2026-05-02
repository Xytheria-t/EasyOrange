import { useState, useRef, useEffect, useCallback } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import {
    Search, X, TrendingUp, ArrowLeft, Clock, Sparkles, PackageSearch,
    Zap, ShoppingBag, Smartphone, BookOpen, Home, Gift, Gamepad2,
    Palette, Dumbbell, Flame, Star, ChevronRight, History, Trash2
} from 'lucide-react';
import { useProductSearch, useSearchSuggestions, useHotKeywords } from '@/hooks';
import { ProductCard } from '@/components/sections/ProductCard';
import '@/styles/main.css';
import './SearchPage.css';

const CATEGORY_ICONS = [
    { name: '数码电子', icon: Smartphone, color: '#3B82F6', bg: '#EFF6FF' },
    { name: '服饰穿搭', icon: ShoppingBag, color: '#EC4899', bg: '#FDF2F8' },
    { name: '图书文具', icon: BookOpen, color: '#10B981', bg: '#ECFDF5' },
    { name: '家居生活', icon: Home, color: '#F59E0B', bg: '#FFFBEB' },
    { name: '美妆护肤', icon: Palette, color: '#8B5CF6', bg: '#F5F3FF' },
    { name: '运动户外', icon: Dumbbell, color: '#EF4444', bg: '#FEF2F2' },
    { name: '游戏娱乐', icon: Gamepad2, color: '#6366F1', bg: '#EEF2FF' },
    { name: '礼品周边', icon: Gift, color: '#F97316', bg: '#FFF7ED' },
];

const TRENDING_TOPICS = [
    { title: '春季新品', subtitle: '焕新季', desc: '发现最新潮流单品', color: '#F97316', icon: Flame },
    { title: '限时特惠', subtitle: '超值购', desc: '精选商品低至5折', color: '#EC4899', icon: Zap },
    { title: '品质生活', subtitle: '精选集', desc: '提升生活幸福感', color: '#8B5CF6', icon: Star },
];

export function SearchPage() {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const initialKeyword = searchParams.get('keyword') || '';
    const [keyword, setKeyword] = useState(initialKeyword);
    const [submittedKeyword, setSubmittedKeyword] = useState(initialKeyword);
    const [showSuggestions, setShowSuggestions] = useState(false);
    const inputRef = useRef<HTMLInputElement>(null);
    const [mousePos, setMousePos] = useState({ x: 0, y: 0 });
    const [searchHistory, setSearchHistory] = useState<string[]>(() => {
        try {
            return JSON.parse(localStorage.getItem('eo_search_history') || '[]');
        } catch {
            return [];
        }
    });

    const { data: searchResult, isLoading: isSearching } = useProductSearch(submittedKeyword, {
        pageNum: 1,
        pageSize: 20,
    });
    const { data: suggestions } = useSearchSuggestions(keyword);
    const { data: hotKeywords } = useHotKeywords(10);

    const products = searchResult?.records ?? [];
    const total = searchResult?.total ?? 0;

    useEffect(() => {
        inputRef.current?.focus();
    }, []);

    const addToHistory = useCallback((kw: string) => {
        if (!kw.trim()) return;
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

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        const trimmed = keyword.trim();
        if (trimmed) {
            setSubmittedKeyword(trimmed);
            setShowSuggestions(false);
            addToHistory(trimmed);
        }
    };

    const handleSuggestionClick = (suggestion: string) => {
        setKeyword(suggestion);
        setSubmittedKeyword(suggestion);
        setShowSuggestions(false);
        addToHistory(suggestion);
    };

    const handleHotKeywordClick = (kw: string) => {
        setKeyword(kw);
        setSubmittedKeyword(kw);
        setShowSuggestions(false);
        addToHistory(kw);
    };

    const handleClear = () => {
        setKeyword('');
        setSubmittedKeyword('');
        inputRef.current?.focus();
    };

    const handleMouseMove = (e: React.MouseEvent) => {
        const rect = (e.currentTarget as HTMLElement).getBoundingClientRect();
        setMousePos({
            x: ((e.clientX - rect.left) / rect.width) * 100,
            y: ((e.clientY - rect.top) / rect.height) * 100,
        });
    };

    const hasResults = submittedKeyword && products.length > 0;
    const noResults = submittedKeyword && !isSearching && products.length === 0;

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
                    <button className="search-back-btn" onClick={() => navigate(-1)}>
                        <ArrowLeft size={20} />
                    </button>
                    <form onSubmit={handleSubmit} className="search-form-wrapper">
                        <div className="search-input-premium">
                            <Search size={18} className="search-input-icon" />
                            <input
                                ref={inputRef}
                                type="text"
                                value={keyword}
                                onChange={e => {
                                    setKeyword(e.target.value);
                                    setShowSuggestions(true);
                                }}
                                onFocus={() => setShowSuggestions(true)}
                                onBlur={() => setTimeout(() => setShowSuggestions(false), 200)}
                                placeholder="搜索你想要的商品..."
                                className="search-input-field"
                            />
                            {keyword && (
                                <button type="button" onClick={handleClear} className="search-clear-btn">
                                    <X size={12} />
                                </button>
                            )}
                            <button type="submit" className="search-submit-btn">
                                <Search size={14} />
                                <span>搜索</span>
                            </button>
                        </div>
                        {showSuggestions && suggestions && suggestions.length > 0 && !submittedKeyword && (
                            <div className="search-suggestions-dropdown">
                                <div className="suggestions-header">
                                    <Sparkles size={12} />
                                    <span>搜索建议</span>
                                </div>
                                {suggestions.map(s => (
                                    <button
                                        key={s}
                                        type="button"
                                        className="suggestion-item"
                                        onMouseDown={() => handleSuggestionClick(s)}
                                    >
                                        <Search size={14} className="suggestion-icon" />
                                        <span className="suggestion-text">{s}</span>
                                        <ChevronRight size={14} className="suggestion-arrow" />
                                    </button>
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
                                            <button className="search-top-card-action" onClick={clearHistory}>
                                                <Trash2 size={12} />
                                                <span>清空</span>
                                            </button>
                                        </div>
                                        <div className="search-history-tags">
                                            {searchHistory.map(item => (
                                                <button
                                                    key={item}
                                                    className="search-history-tag"
                                                    onClick={() => handleHotKeywordClick(item)}
                                                >
                                                    <Clock size={10} />
                                                    <span>{item}</span>
                                                    <span
                                                        className="search-history-remove"
                                                        onClick={(e) => removeFromHistory(item, e)}
                                                    >
                                                        <X size={8} />
                                                    </span>
                                                </button>
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
                                                <button
                                                    key={item.keyword}
                                                    className={`search-hot-tag ${index < 3 ? 'search-hot-tag-highlight' : ''}`}
                                                    onClick={() => handleHotKeywordClick(item.keyword)}
                                                >
                                                    {index < 3 && (
                                                        <span className={`search-hot-tag-rank rank-${index + 1}`}>
                                                            {index + 1}
                                                        </span>
                                                    )}
                                                    <span>{item.keyword}</span>
                                                </button>
                                            ))}
                                        </div>
                                    </div>
                                )}
                            </div>
                        )}

                        {/* Category Quick Access - 8 columns compact grid */}
                        <div className="search-categories-section">
                            <div className="search-section-header-compact">
                                <div className="search-section-icon-compact">
                                    <PackageSearch size={14} />
                                </div>
                                <h3 className="search-section-title-compact">分类浏览</h3>
                            </div>
                            <div className="search-categories-grid">
                                {CATEGORY_ICONS.map(cat => (
                                    <button
                                        key={cat.name}
                                        className="search-category-card"
                                        onClick={() => handleHotKeywordClick(cat.name)}
                                        style={{ '--cat-color': cat.color, '--cat-bg': cat.bg } as React.CSSProperties}
                                    >
                                        <div className="search-category-icon">
                                            <cat.icon size={20} />
                                        </div>
                                        <span className="search-category-name">{cat.name}</span>
                                    </button>
                                ))}
                            </div>
                        </div>

                        {/* Trending Topics Cards */}
                        <div className="search-trending-section">
                            <div className="search-section-header-compact">
                                <div className="search-section-icon-compact">
                                    <Flame size={14} />
                                </div>
                                <h3 className="search-section-title-compact">发现好物</h3>
                            </div>
                            <div className="search-trending-cards">
                                {TRENDING_TOPICS.map((topic) => (
                                    <div
                                        key={topic.title}
                                        className="search-trending-card"
                                        style={{ '--topic-color': topic.color } as React.CSSProperties}
                                        onMouseMove={handleMouseMove}
                                    >
                                        <div
                                            className="trending-card-glow"
                                            style={{
                                                background: `radial-gradient(circle at ${mousePos.x}% ${mousePos.y}%, ${topic.color}18 0%, transparent 50%)`,
                                            }}
                                        />
                                        <div className="trending-card-content">
                                            <div className="trending-card-badge" style={{ color: topic.color }}>
                                                <topic.icon size={12} />
                                                <span>{topic.subtitle}</span>
                                            </div>
                                            <h4 className="trending-card-title">{topic.title}</h4>
                                            <p className="trending-card-desc">{topic.desc}</p>
                                        </div>
                                        <div className="trending-card-shine" />
                                    </div>
                                ))}
                            </div>
                        </div>

                        {/* Tips Card */}
                        <div className="search-tips-section">
                            <div className="search-tips-card">
                                <div className="search-tips-icon">
                                    <Sparkles size={18} />
                                </div>
                                <div className="search-tips-content">
                                    <h4 className="search-tips-title">搜索小技巧</h4>
                                    <ul className="search-tips-list">
                                        <li>输入关键词即可搜索商品标题和描述</li>
                                        <li>使用空格分隔多个关键词进行精确搜索</li>
                                        <li>浏览热门商品发现更多好物</li>
                                    </ul>
                                </div>
                            </div>
                        </div>
                    </div>
                )}

                {/* Search Results */}
                {submittedKeyword && (
                    <div className="search-results-section">
                        <div className="search-results-header">
                            <div className="search-results-badge">
                                <Search size={12} />
                                <span>搜索结果</span>
                            </div>
                            <h2 className="search-results-title">
                                搜索 &ldquo;<span className="search-keyword-highlight">{submittedKeyword}</span>&rdquo;
                            </h2>
                            <p className="search-results-count">
                                共找到 <span className="search-count-number">{total}</span> 件商品
                            </p>
                        </div>

                        {isSearching && (
                            <div className="search-loading">
                                <div className="search-loading-spinner"></div>
                                <p className="search-loading-text">正在搜索中...</p>
                            </div>
                        )}

                        {hasResults && !isSearching && (
                            <div className="products-grid">
                                {products.map(product => (
                                    <ProductCard
                                        key={product.id}
                                        product={product}
                                        onViewDetails={(id) => navigate(`/products/${id}`)}
                                    />
                                ))}
                            </div>
                        )}

                        {noResults && (
                            <div className="search-no-results">
                                <div className="search-no-results-icon">
                                    <div className="search-no-results-orb"></div>
                                    <PackageSearch size={40} />
                                </div>
                                <h3 className="search-no-results-title">未找到相关商品</h3>
                                <p className="search-no-results-desc">
                                    试试其他关键词，或浏览下面的热门商品
                                </p>
                                {hotKeywords && hotKeywords.length > 0 && (
                                    <div className="search-no-results-hints">
                                        <span className="search-hint-label">试试搜索：</span>
                                        <div className="search-hint-tags">
                                            {hotKeywords.slice(0, 5).map(item => (
                                                <button
                                                    key={item.keyword}
                                                    className="search-hint-tag"
                                                    onClick={() => handleHotKeywordClick(item.keyword)}
                                                >
                                                    {item.keyword}
                                                </button>
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
