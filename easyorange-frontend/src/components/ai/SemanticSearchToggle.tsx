import { Search, Sparkles } from 'lucide-react';

interface SemanticSearchToggleProps {
  isActive: boolean;
  onToggle: () => void;
}

export function SemanticSearchToggle({ isActive, onToggle }: SemanticSearchToggleProps) {
  return (
    <button
      className={`semantic-toggle ${isActive ? 'active' : ''}`}
      onClick={onToggle}
      title={isActive ? '当前为语义搜索模式' : '当前为关键词搜索模式'}
    >
      {isActive ? <Sparkles size={14} /> : <Search size={14} />}
      <span>{isActive ? '语义搜索' : '关键词搜索'}</span>
    </button>
  );
}