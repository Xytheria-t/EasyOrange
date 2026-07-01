import { HelpCircle, Sparkles, Target, TrendingUp } from 'lucide-react';
import { Button } from '@/components/ui/button';
import type { AiEnhancement } from '@/types/product';

interface AiSearchPanelProps {
    enhancement: AiEnhancement;
    onQuestionClick: (question: string) => void;
}

export function AiSearchPanel({ enhancement, onQuestionClick }: AiSearchPanelProps) {
    return (
        <div className="ai-search-panel">
            <div className="ai-panel-header">
                <Sparkles size={18} />
                <span>AI 智能分析</span>
            </div>

            {enhancement.intentExplanation && (
                <div className="ai-panel-section">
                    <div className="ai-section-title">
                        <Target size={14} />
                        <span>需求理解</span>
                    </div>
                    <p className="ai-section-content">{enhancement.intentExplanation}</p>
                </div>
            )}

            {enhancement.marketAnalysis && (
                <div className="ai-panel-section">
                    <div className="ai-section-title">
                        <TrendingUp size={14} />
                        <span>市场分析</span>
                    </div>
                    <p className="ai-section-content">{enhancement.marketAnalysis}</p>
                </div>
            )}

            {enhancement.suggestedQuestions?.length > 0 && (
                <div className="ai-panel-section">
                    <div className="ai-section-title">
                        <HelpCircle size={14} />
                        <span>猜你想问</span>
                    </div>
                    <div className="ai-questions">
                        {enhancement.suggestedQuestions.map((q, i) => (
                            <Button
                                key={i}
                                type="button"
                                variant="outline"
                                size="sm"
                                className="ai-question-btn"
                                onClick={() => onQuestionClick(q)}
                            >
                                {q}
                            </Button>
                        ))}
                    </div>
                </div>
            )}
        </div>
    );
}
