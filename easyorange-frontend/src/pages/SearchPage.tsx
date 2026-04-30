import { useState, useRef, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Search, X, TrendingUp, ArrowLeft } from 'lucide-react';
import { useProductSearch, useSearchSuggestions, useHotKeywords } from '@/hooks';
import { ProductCard } from '@/components/sections/ProductCard';
import '@/styles/main.css';

export function SearchPage() {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const initialKeyword = searchParams.get('keyword') || '';
    const [keyword, setKeyword] = useState(initialKeyword);
    const [submittedKeyword, setSubmittedKeyword] = useState(initialKeyword);
    const [showSuggestions, setShowSuggestions] = useState(false);
    const inputRef = useRef<HTMLInputElement>(null);

    const { data: searchResult, isLoading: isSearching } = useProductSearch(submittedKeyword, {
        current: 1,
        size: 20,
    });
    const { data: suggestions } = useSearchSuggestions(keyword);
    const { data: hotKeywords } = useHotKeywords(10);

    const products = searchResult?.records ?? [];
    const total = searchResult?.total ?? 0;

    useEffect(() => {
        inputRef.current?.focus();
    }, []);

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        const trimmed = keyword.trim();
        if (trimmed) {
            setSubmittedKeyword(trimmed);
            setShowSuggestions(false);
        }
    };

    const handleSuggestionClick = (suggestion: string) => {
        setKeyword(suggestion);
        setSubmittedKeyword(suggestion);
        setShowSuggestions(false);
    };

    const handleHotKeywordClick = (kw: string) => {
        setKeyword(kw);
        setSubmittedKeyword(kw);
        setShowSuggestions(false);
    };

    const handleClear = () => {
        setKeyword('');
        setSubmittedKeyword('');
        inputRef.current?.focus();
    };

    const hasResults = submittedKeyword && products.length > 0;
    const noResults = submittedKeyword && !isSearching && products.length === 0;

    return (
        <div className="container py-4" style={{ maxWidth: 800 }}>
            <div className="flex items-center gap-3 mb-4">
                <button
                    className="btn btn-ghost"
                    style={{ padding: '0.4rem' }}
                    onClick={() => navigate(-1)}
                >
                    <ArrowLeft size={20} />
                </button>
                <form onSubmit={handleSubmit} className="flex-1 relative">
                    <div className="relative">
                        <Search size={18} className="absolute left-3 top-1/2 -translate-y-1/2" style={{ color: 'var(--text-tertiary)' }} />
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
                            placeholder="搜索商品..."
                            className="form-input"
                            style={{ paddingLeft: '2.5rem', paddingRight: '2.5rem' }}
                        />
                        {keyword && (
                            <button
                                type="button"
                                onClick={handleClear}
                                className="absolute right-3 top-1/2 -translate-y-1/2"
                                style={{ color: 'var(--text-tertiary)' }}
                            >
                                <X size={16} />
                            </button>
                        )}
                    </div>
                    {showSuggestions && suggestions && suggestions.length > 0 && !submittedKeyword && (
                        <div className="card-elevated mt-1 p-2" style={{ position: 'absolute', width: '100%', zIndex: 50 }}>
                            {suggestions.map(s => (
                                <button
                                    key={s}
                                    type="button"
                                    className="w-full text-left px-3 py-2 rounded hover:bg-[var(--bg-secondary)]"
                                    style={{ fontSize: '0.9rem' }}
                                    onMouseDown={() => handleSuggestionClick(s)}
                                >
                                    <Search size={14} className="inline mr-2" style={{ color: 'var(--text-tertiary)' }} />
                                    {s}
                                </button>
                            ))}
                        </div>
                    )}
                </form>
            </div>

            {!submittedKeyword && (
                <div className="mt-4">
                    {hotKeywords && hotKeywords.length > 0 && (
                        <div className="mb-6">
                            <h3 className="flex items-center gap-2 mb-3" style={{ fontSize: '0.95rem', fontWeight: 600 }}>
                                <TrendingUp size={16} style={{ color: 'var(--color-primary)' }} />
                                热门搜索
                            </h3>
                            <div className="flex flex-wrap gap-2">
                                {hotKeywords.map(item => (
                                    <button
                                        key={item.keyword}
                                        className="btn btn-ghost"
                                        style={{ fontSize: '0.85rem', padding: '0.3rem 0.8rem' }}
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

            {submittedKeyword && (
                <div className="mt-2">
                    <div className="flex items-center justify-between mb-4">
                        <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                            搜索 "<span style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{submittedKeyword}</span>"
                            共 <span style={{ fontWeight: 600 }}>{total}</span> 件商品
                        </p>
                    </div>

                    {isSearching && (
                        <div className="flex justify-center py-12">
                            <div className="loading-spinner-lg"></div>
                        </div>
                    )}

                    {hasResults && !isSearching && (
                        <div className="products-grid">
                            {products.map(product => (
                                <ProductCard key={product.id} product={product} onViewDetails={(id) => navigate(`/products/${id}`)} />
                            ))}
                        </div>
                    )}

                    {noResults && (
                        <div className="text-center py-12">
                            <div style={{ fontSize: '3rem', marginBottom: '1rem' }}>🔍</div>
                            <h3 style={{ fontSize: '1.1rem', marginBottom: '0.5rem' }}>未找到相关商品</h3>
                            <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
                                试试其他关键词或浏览热门商品
                            </p>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}
