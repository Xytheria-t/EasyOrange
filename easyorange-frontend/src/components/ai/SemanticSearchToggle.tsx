import { Info, Search, Sparkles } from 'lucide-react';
import { Button } from '@/components/ui/button';

interface SemanticSearchToggleProps {
    isActive: boolean;
    onToggle: () => void;
}

export function SemanticSearchToggle({ isActive, onToggle }: SemanticSearchToggleProps) {
    return (
        <div className="semantic-toggle-wrapper">
            <Button
                type="button"
                variant="outline"
                className={`semantic-toggle ${isActive ? 'active' : ''}`}
                onClick={onToggle}
            >
                <span className="semantic-toggle-icon">{isActive ? <Sparkles size={14} /> : <Search size={14} />}</span>
                <span className="semantic-toggle-label">{isActive ? '语义搜索' : '关键词搜索'}</span>
                {isActive && <span className="semantic-toggle-dot" />}
            </Button>
            <div className="semantic-toggle-tooltip">
                <Info size={12} />
                <span>{isActive ? 'AI理解搜索意图，匹配更精准' : '切换为AI语义理解搜索'}</span>
            </div>
        </div>
    );
}
