import { AlertTriangle, CheckCircle, Loader2, Sparkles, XCircle } from 'lucide-react';
import type { AiReviewResult } from '@/api/aiApi';
import { Button } from '@/components/ui/button';
import './ai-components.css';

interface AiReviewSuggestionProps {
    result: AiReviewResult | null;
    isLoading: boolean;
    onGetSuggestion: () => void;
    onApply: (action: 'approve' | 'reject') => void;
}

function getRiskIcon() {
    return <AlertTriangle size={14} />;
}

export function AiReviewSuggestion({ result, isLoading, onGetSuggestion, onApply }: AiReviewSuggestionProps) {
    if (isLoading) {
        return (
            <div className="ai-review-suggestion" style={{ marginTop: 12 }}>
                <div className="ai-badge-loading">
                    <Loader2 size={16} className="ai-sparkle" style={{ animation: 'spin 0.8s linear infinite' }} />
                    AI 正在分析商品信息，请稍候...
                </div>
            </div>
        );
    }

    if (result) {
        return (
            <div className="ai-review-suggestion" style={{ marginTop: 12 }}>
                <div className="ai-review-header">
                    <Sparkles size={16} />
                    <span>AI 审核建议</span>
                </div>
                <div className="ai-review-result">
                    <div className={`ai-review-action ${result.isApproved ? 'pass' : 'reject'}`}>
                        {result.isApproved ? <CheckCircle size={16} /> : <XCircle size={16} />}
                        <span>
                            {result.isApproved ? '建议通过' : '建议拒绝'}
                            <span style={{ marginLeft: 8, fontWeight: 400, opacity: 0.8 }}>
                                置信度 {result.confidenceScore}%
                            </span>
                        </span>
                    </div>
                    {result.riskFlags.length > 0 && (
                        <div className="ai-risk-flags">
                            {result.riskFlags.map((flag, index) => (
                                <span key={index} className="risk-flag">
                                    {getRiskIcon()}
                                    {flag}
                                </span>
                            ))}
                        </div>
                    )}
                    <div className="ai-reasoning" style={{ margin: '8px 0', lineHeight: 1.6 }}>
                        {result.reasoning}
                    </div>
                    <Button className="ai-apply-btn" onClick={() => onApply(result.isApproved ? 'approve' : 'reject')}>
                        <Sparkles size={14} />
                        采纳 AI 建议
                    </Button>
                </div>
            </div>
        );
    }

    return (
        <div className="ai-review-suggestion" style={{ marginTop: 12 }}>
            <div className="ai-review-header">
                <Sparkles size={16} />
                <span>AI 审核建议</span>
            </div>
            <p style={{ fontSize: 13, color: '#713f12', margin: '0 0 12px', lineHeight: 1.5 }}>
                让 AI 分析商品信息，提供审核建议
            </p>
            <Button className="ai-review-trigger" onClick={onGetSuggestion}>
                <Sparkles size={14} />
                获取 AI 审核建议
            </Button>
        </div>
    );
}
