import { Info, Loader2, Sparkles, TrendingUp } from 'lucide-react';
import type { PricingSuggestion } from '@/api/aiApi';
import { Button } from '@/components/ui/button';
import './ai-components.css';

interface AiPricingBadgeProps {
    suggestion: PricingSuggestion;
    onApply: (price: number) => void;
    isLoading?: boolean;
}

export function AiPricingBadge({ suggestion, onApply, isLoading }: AiPricingBadgeProps) {
    if (isLoading) {
        return (
            <div className="ai-badge-loading">
                <Loader2 size={16} className="animate-spin" />
                <span>AI 正在分析市场行情...</span>
            </div>
        );
    }

    return (
        <div className="ai-badge">
            <div className="ai-badge-header">
                <Sparkles size={16} className="ai-icon" />
                <span className="ai-badge-title">AI 智能定价建议</span>
            </div>
            <div className="ai-badge-body">
                <div className="price-suggestions">
                    <div className="price-item">
                        <span className="price-label">建议定价</span>
                        <span className="price-value">¥{suggestion.suggestedPrice.toFixed(2)}</span>
                    </div>
                    <div className="price-range">
                        <span className="price-label">合理区间</span>
                        <span className="price-value highlight">
                            ¥{suggestion.minPrice.toFixed(2)} - ¥{suggestion.maxPrice.toFixed(2)}
                        </span>
                    </div>
                </div>
                <div className="ai-reasoning">
                    <TrendingUp size={14} />
                    <span>{suggestion.reasoning}</span>
                </div>
                <div className="ai-market">
                    <Info size={14} />
                    <span>{suggestion.marketContext}</span>
                </div>
                <Button className="ai-apply-btn" onClick={() => onApply(suggestion.suggestedPrice)}>
                    采纳此定价
                </Button>
            </div>
        </div>
    );
}
