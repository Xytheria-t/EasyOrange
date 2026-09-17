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
        // 后端在 AI 不可用时返回 isApproved=false + AI_UNAVAILABLE：语义是「建议无效」而非「建议拒绝」，
        // 不能渲染成拒绝态、更不能给「采纳 AI 建议」按钮（那会变成一键误杀）
        const unavailable = result.riskFlags.includes('AI_UNAVAILABLE');
        return (
            <div className="ai-review-suggestion" style={{ marginTop: 12 }}>
                <div className="ai-review-header">
                    <Sparkles size={16} />
                    <span>AI 审核建议</span>
                </div>
                <div className="ai-review-result">
                    {unavailable ? (
                        <div className="ai-review-action pending">
                            <AlertTriangle size={16} />
                            <span>无法判定，请人工审核</span>
                        </div>
                    ) : (
                        <div className={`ai-review-action ${result.isApproved ? 'pass' : 'reject'}`}>
                            {result.isApproved ? <CheckCircle size={16} /> : <XCircle size={16} />}
                            <span>
                                {result.isApproved ? '建议通过' : '建议拒绝'}
                                <span style={{ marginLeft: 8, fontWeight: 400, opacity: 0.8 }}>
                                    置信度 {result.confidenceScore}%
                                </span>
                            </span>
                        </div>
                    )}
                    {result.riskFlags.length > 0 && (
                        <div className="ai-risk-flags">
                            {result.riskFlags.map(flag => (
                                <span key={flag} className="risk-flag">
                                    {getRiskIcon()}
                                    {flag === 'AI_UNAVAILABLE' ? 'AI 不可用' : flag}
                                </span>
                            ))}
                        </div>
                    )}
                    <div className="ai-reasoning" style={{ margin: '8px 0', lineHeight: 1.6 }}>
                        {result.reasoning}
                    </div>
                    {!unavailable && (
                        <Button
                            className="ai-apply-btn"
                            onClick={() => onApply(result.isApproved ? 'approve' : 'reject')}
                        >
                            <Sparkles size={14} />
                            采纳 AI 建议
                        </Button>
                    )}
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
