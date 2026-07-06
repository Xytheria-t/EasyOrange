import { Sparkles, Zap } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Button } from '@/components/ui/button';

export type ToolsPlazaFilter = 'all' | 'ai' | 'discount';

interface ToolsPlazaProps {
    onFilterChange?: (filter: ToolsPlazaFilter) => void;
    total?: number;
    activeFilter?: ToolsPlazaFilter;
}

export function ToolsPlaza({ onFilterChange, total = 0, activeFilter: externalActiveFilter }: ToolsPlazaProps) {
    const [internalActiveFilter, setInternalActiveFilter] = useState<ToolsPlazaFilter>('all');
    const [aiMode, setAiMode] = useState(false);

    const activeFilter = externalActiveFilter ?? internalActiveFilter;

    useEffect(() => {
        if (externalActiveFilter && externalActiveFilter !== 'ai') {
            setAiMode(false);
        }
    }, [externalActiveFilter]);

    const handleFilterClick = (filter: ToolsPlazaFilter) => {
        if (filter === 'ai') {
            const nextAiMode = !aiMode;
            setAiMode(nextAiMode);
            setInternalActiveFilter(nextAiMode ? 'ai' : 'all');
            if (nextAiMode) {
                onFilterChange?.('ai');
            } else {
                onFilterChange?.('all');
            }
            return;
        }
        setInternalActiveFilter(filter);
        setAiMode(false);
        onFilterChange?.(filter);
    };

    return (
        <div className="tools-plaza" id="quickFilters">
            <div className="plaza-header">
                <div className="plaza-brand">
                    <div className="brand-icon">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
                            <circle cx="12" cy="12" r="3" />
                            <path d="M12 1v6m0 6v6m11-7h-6m-6 0H1m15.5-6.5l-4.5 4.5m-4 4l-4.5 4.5m9-13l4.5 4.5m-4 4l4.5 4.5" />
                        </svg>
                    </div>
                    <div className="brand-text">
                        <span className="brand-title">筛选工具</span>
                        <span className="brand-subtitle">TOOLS PLAZA</span>
                    </div>
                </div>
                <div className="plaza-status">
                    <span className="status-dot" />
                    <span className="status-text">{total} 件商品</span>
                </div>
            </div>

            <div className="plaza-tools">
                <Button
                    type="button"
                    variant="ghost"
                    className={`plaza-tool plaza-ai-tool ${aiMode ? 'active' : ''}`}
                    onClick={() => handleFilterClick('ai')}
                >
                    <div className="tool-icon">
                        <Sparkles size={16} />
                    </div>
                    <span className="tool-label">AI推荐</span>
                    <div className="tool-badge ai-badge">
                        <Zap size={8} />
                        AI
                    </div>
                </Button>

                <div className="tool-divider" />

                <div className="tool-group">
                    <Button
                        type="button"
                        variant="ghost"
                        className={`plaza-tool ${activeFilter === 'all' && !aiMode ? 'active' : ''}`}
                        onClick={() => handleFilterClick('all')}
                    >
                        <div className="tool-icon">
                            <svg
                                viewBox="0 0 24 24"
                                fill="none"
                                stroke="currentColor"
                                strokeWidth="2"
                                aria-hidden="true"
                            >
                                <rect x="3" y="3" width="7" height="7" />
                                <rect x="14" y="3" width="7" height="7" />
                                <rect x="14" y="14" width="7" height="7" />
                                <rect x="3" y="14" width="7" height="7" />
                            </svg>
                        </div>
                        <span className="tool-label">全部</span>
                        <div className="tool-badge">ALL</div>
                    </Button>

                    <Button
                        type="button"
                        variant="ghost"
                        className={`plaza-tool ${activeFilter === 'discount' && !aiMode ? 'active' : ''}`}
                        onClick={() => handleFilterClick('discount')}
                    >
                        <div className="tool-icon">
                            <svg
                                viewBox="0 0 24 24"
                                fill="none"
                                stroke="currentColor"
                                strokeWidth="2"
                                aria-hidden="true"
                            >
                                <circle cx="9" cy="21" r="1" />
                                <circle cx="20" cy="21" r="1" />
                                <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" />
                            </svg>
                        </div>
                        <span className="tool-label">特价优惠</span>
                        <div className="tool-badge">SALE</div>
                    </Button>
                </div>
            </div>

            {aiMode && (
                <div className="plaza-ai-hint">
                    <Sparkles size={14} />
                    <span>AI正在根据您的浏览习惯为您推荐最合适的商品</span>
                </div>
            )}

            <div className="plaza-accent">
                <div className="accent-glow" />
                <div className="accent-line" />
            </div>
        </div>
    );
}
